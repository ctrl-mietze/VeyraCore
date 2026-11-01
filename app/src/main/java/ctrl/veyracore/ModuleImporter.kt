package ctrl.veyracore

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

data class ZipSummary(val count: Int, val unpackedBytes: Long, val containsModuleProp: Boolean)

/** Decompress-and-check input; do not extract or execute ZIP contents. */
object ZipSafety {
    const val MAX_ZIP_BYTES = 128L * 1024 * 1024
    private const val MAX_UNPACKED_BYTES = 512L * 1024 * 1024
    private const val MAX_ENTRIES = 5_000

    fun inspect(stream: InputStream): ZipSummary {
        var count = 0
        var total = 0L
        var moduleProp = false
        val seen = HashSet<String>()
        ZipInputStream(stream.buffered()).use { zip ->
            val buf = ByteArray(16 * 1024)
            while (true) {
                val entry = zip.nextEntry ?: break
                count++
                require(count <= MAX_ENTRIES) { "ZIP enthält zu viele Einträge" }
                val name = entry.name
                val normal = name.replace('\\', '/')
                require(normal.isNotBlank() && !normal.startsWith('/') && !normal.contains('\u0000')
                    && !normal.startsWith("./") && !normal.contains(':')
                    && normal.split('/').none { it == ".." || it == "." }) { "Unsicherer ZIP-Pfad" }
                require(seen.add(normal)) { "Doppelter ZIP-Eintrag" }
                if (normal == "module.prop") moduleProp = true
                while (true) {
                    val n = zip.read(buf)
                    if (n < 0) break
                    total += n
                    require(total <= MAX_UNPACKED_BYTES) { "ZIP-Entpackgröße überschritten" }
                }
                zip.closeEntry()
            }
        }
        require(count > 0 && moduleProp) { "Keine gültige Modul-ZIP (module.prop fehlt im Wurzelverzeichnis)" }
        return ZipSummary(count, total, moduleProp)
    }
}

data class StagedModule(val file: File, val summary: ZipSummary)
class ModuleImporter(private val context: Context, private val shell: SafeShell) {
    suspend fun stage(uri: Uri): Result<StagedModule> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "module-import").apply { mkdirs() }
            val file = File(dir, "module-${UUID.randomUUID()}.zip")
            try {
                val stream = context.contentResolver.openInputStream(uri) ?: error("Datei kann nicht geöffnet werden")
                stream.use { input ->
                    file.outputStream().buffered().use { out ->
                        var total = 0L
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            total += n
                            require(total <= ZipSafety.MAX_ZIP_BYTES) { "ZIP-Datei größer als 128 MiB" }
                            out.write(buffer, 0, n)
                        }
                    }
                }
                val summary = file.inputStream().use { ZipSafety.inspect(it) }
                StagedModule(file, summary)
            } catch (e: Exception) {
                file.delete()
                throw e
            }
        }
    }

    suspend fun install(staged: StagedModule): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(staged.file.isFile && staged.file.parentFile?.name == "module-import") { "Ungültige Zwischendatei" }
            val root = shell.probe()
            require(root.state == Verification.VERIFIED) { "UID 0 ist nicht mehr verifiziert" }
            // Only call a checked existing ksud. No custom arbitrary shell command from the ZIP.
            val candidate = shell.su(
                "if [ -x /data/adb/ksu/bin/ksud ]; then echo /data/adb/ksu/bin/ksud; else command -v ksud || true; fi", 4
            )
            val tool = candidate.output.lineSequence().map { it.trim() }.firstOrNull {
                it.startsWith('/') && it.endsWith("/ksud") && it.matches(Regex("[A-Za-z0-9/_.-]+"))
            } ?: error("Kein kompatibler ksud-Modulinstaller nachgewiesen")
            val safePath = staged.file.absolutePath
            require(safePath.matches(Regex("[A-Za-z0-9/_.-]+"))) { "Unsicherer Cachepfad" }
            val result = shell.su("$tool module install '$safePath'", 90)
            require(result.exitCode == 0 && !result.timedOut) {
                result.error ?: result.output.ifBlank { "Modul-Installation fehlgeschlagen" }
            }
            result.output.ifBlank { "Modul an kompatible Runtime übergeben" }.takeLast(1200)
        }
    }
}
