package ctrl.veyracore

import android.content.Context
import android.os.Build
import android.system.Os
import android.system.OsConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

enum class Verification { UNKNOWN, VERIFIED, NOT_VERIFIED, ERROR }
enum class Severity { INFO, SUCCESS, WARNING, ERROR }

enum class UidClass {
    ROOT,
    SYSTEM,
    ADB_SHELL,
    APP,
    UNKNOWN;

    val label: String
        get() = when (this) {
            ROOT -> "UID 0 · root"
            SYSTEM -> "UID 1000 · system"
            ADB_SHELL -> "UID 2000 · adb shell"
            APP -> "App / anderer UID"
            UNKNOWN -> "UID unbekannt"
        }
}

enum class RuntimeStatus {
    UNKNOWN,
    CONNECTED,
    ABSENT,
    ERROR
}

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val android: String,
    val kernel: String,
    val fingerprint: String,
    val firmwareId: String = fingerprint,
    val abi: String = "unknown",
    val pageSize: Int? = null,
    val kmi: String = "unknown",
    val securityPatch: String = "unknown",
    val product: String = "unknown"
) {
    val profileSummary: String
        get() = listOf(
            "${manufacturer.ifBlank { "Unbekannt" }} ${model.ifBlank { "Unbekannt" }}",
            "Firmware ${firmwareId.ifBlank { "unbekannt" }}",
            "Kernel ${kernel.ifBlank { "unbekannt" }}"
        ).joinToString(" · ")

    companion object {
        fun current(): DeviceInfo {
            val kernel = System.getProperty("os.version").orEmpty().ifBlank { "Unbekannt" }
            val fingerprint = Build.FINGERPRINT.orEmpty().ifBlank { "Unbekannt" }
            val firmwareId = listOf(Build.ID, Build.VERSION.INCREMENTAL)
                .map { it.orEmpty() }
                .filter { it.isNotBlank() }
                .joinToString("/")
                .ifBlank { fingerprint }
            val pageSize = runCatching { Os.sysconf(OsConstants._SC_PAGESIZE).toInt() }.getOrNull()
            val kmi = Regex("android\\d+-\\d+\\.\\d+").find(kernel)?.value ?: "unknown"
            return DeviceInfo(
                manufacturer = Build.MANUFACTURER.orEmpty().ifBlank { "Unbekannt" },
                model = Build.MODEL.orEmpty().ifBlank { "Unbekannt" },
                android = "Android ${Build.VERSION.RELEASE.orEmpty().ifBlank { "?" }} (API ${Build.VERSION.SDK_INT})",
                kernel = kernel,
                fingerprint = fingerprint,
                firmwareId = firmwareId,
                abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty().ifBlank { "unknown" },
                pageSize = pageSize,
                kmi = kmi,
                securityPatch = Build.VERSION.SECURITY_PATCH.orEmpty().ifBlank { "unknown" },
                product = Build.PRODUCT.orEmpty().ifBlank { "unknown" }
            )
        }
    }
}

data class RootProbe(
    val state: Verification = Verification.UNKNOWN,
    val detail: String = "Noch nicht geprüft",
    val uid: Int? = null,
    val uidClass: UidClass = UidClass.UNKNOWN,
    val identity: String? = null,
    val selinux: String? = null,
    val runtime: RuntimeStatus = RuntimeStatus.UNKNOWN,
    val runtimeDetail: String = "Noch nicht geprüft"
) {
    val isUid0: Boolean
        get() = state == Verification.VERIFIED && uid == 0 && uidClass == UidClass.ROOT
}

data class AppLog(val epochMs: Long, val severity: Severity, val event: String, val detail: String)
data class ShellResult(val exitCode: Int?, val output: String, val timedOut: Boolean = false, val error: String? = null)
data class RootModule(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val disabled: Boolean,
    val markedForRemoval: Boolean
)

data class ModuleInstallerAvailability(val available: Boolean, val detail: String)

/** A distinct capability state used by the provider registry and UI. */
enum class ProviderStatus {
    UNAVAILABLE,
    SUPPORTED,
    BLOCKED,
    REQUIRES_EXTERNAL_RUNTIME;

    val label: String
        get() = when (this) {
            UNAVAILABLE -> "Nicht verfügbar"
            SUPPORTED -> "Unterstützt"
            BLOCKED -> "Gesperrt"
            REQUIRES_EXTERNAL_RUNTIME -> "Externe Runtime nötig"
        }
}

data class ProviderProvenance(
    val sourceName: String,
    val sourceUrl: String,
    val basis: String,
    val importedArtifacts: Boolean = false,
    val reviewedOn: String = "2026-11-01"
)

data class ProviderEvaluation(
    val methodId: String,
    val displayName: String,
    val status: ProviderStatus,
    val detail: String,
    val exactProfileMatched: Boolean,
    val provenance: ProviderProvenance
)

data class ProviderPreparation(
    val status: ProviderStatus,
    val started: Boolean,
    val detail: String
)

/**
 * Boundary for separately reviewed root integrations. A provider may report state and prepare a
 * verification plan, but this interface does not permit an exploit download or a destructive run.
 */
interface RootProvider {
    val methodId: String
    val displayName: String
    val provenance: ProviderProvenance
    val supportedProfiles: List<SupportedProfile>
    fun evaluate(device: DeviceInfo, proof: RootProbe): ProviderEvaluation
    suspend fun prepare(device: DeviceInfo): ProviderPreparation
}

class RootProviderRegistry private constructor(val providers: List<RootProvider>) {
    val supportedProfiles: List<SupportedProfile> = providers.flatMap { it.supportedProfiles }.distinct()

    fun evaluateAll(device: DeviceInfo, proof: RootProbe): List<ProviderEvaluation> =
        providers.map { it.evaluate(device, proof) }

    fun provider(methodId: String): RootProvider? = providers.firstOrNull { it.methodId == methodId }

    companion object {
        fun default(): RootProviderRegistry = RootProviderRegistry(
            providers = listOf(
                CatalogProvider(
                    methodId = "ghostlock",
                    displayName = "GhostLock",
                    provenance = ProviderProvenance(
                        sourceName = "GhostLock-Galaxy research",
                        sourceUrl = "https://github.com/wxxsfxyzm/GhostLock-Galaxy",
                        basis = "Upstream research and exact kernel tables only; no payload or exploit code imported."
                    ),
                    statusWithoutProfile = ProviderStatus.UNAVAILABLE,
                    detailWithoutProfile = "Kein exaktes Veyra-Profil und keine integrierte, testbare Provider-Implementierung."
                ),
                CatalogProvider(
                    methodId = "dirtyfrag",
                    displayName = "DirtyFrag",
                    provenance = ProviderProvenance(
                        sourceName = "DirtyFrag upstream research",
                        sourceUrl = "https://github.com/mitschud/DirtyFrag",
                        basis = "Dokumentation konsultiert; keine Payloads, Kernelmodule, Offsettabellen oder CVE-Logik importiert."
                    ),
                    statusWithoutProfile = ProviderStatus.BLOCKED,
                    detailWithoutProfile = "Absichtlich gesperrt: Firmware, ABI, Seitengröße, KMI und sicherer Lauf sind nicht Veyra-seitig verifiziert."
                ),
                CatalogProvider(
                    methodId = "vx-cert-reroot",
                    displayName = "VX Cert / Re-Root",
                    provenance = ProviderProvenance(
                        sourceName = "VeyraCore legacy backend architecture",
                        sourceUrl = "https://github.com/ctrl-mietze/VeyraCore",
                        basis = "Legacy app_process/helper architecture reviewed read-only; no helper, certificate mutation or installer imported."
                    ),
                    statusWithoutProfile = ProviderStatus.REQUIRES_EXTERNAL_RUNTIME,
                    detailWithoutProfile = "Benötigt die frühere externe Helper-/Runtime-Kette; diese unabhängige App liefert sie nicht aus."
                ),
                AuthorizedSuProvider()
            )
        )
    }
}

private class CatalogProvider(
    override val methodId: String,
    override val displayName: String,
    override val provenance: ProviderProvenance,
    private val statusWithoutProfile: ProviderStatus,
    private val detailWithoutProfile: String,
    override val supportedProfiles: List<SupportedProfile> = emptyList()
) : RootProvider {
    override fun evaluate(device: DeviceInfo, proof: RootProbe): ProviderEvaluation {
        val exact = supportedProfiles.any { ProfileMatcher.matchesExact(it, device) }
        val detail = if (exact) {
            "Ein vollständiges Metadatenprofil passt, aber es gibt keinen integrierten und auf diesem Gerät verifizierten Provider."
        } else {
            detailWithoutProfile
        }
        return ProviderEvaluation(methodId, displayName, statusWithoutProfile, detail, exact, provenance)
    }

    override suspend fun prepare(device: DeviceInfo): ProviderPreparation = ProviderPreparation(
        status = statusWithoutProfile,
        started = false,
        detail = "Nur Verifikationsplan gespeichert. Es werden keine Dateien geladen und kein Root-Lauf gestartet."
    )
}

private class AuthorizedSuProvider : RootProvider {
    override val methodId: String = "authorized-su"
    override val displayName: String = "Bereits autorisiertes su"
    override val supportedProfiles: List<SupportedProfile> = emptyList()
    override val provenance = ProviderProvenance(
        sourceName = "VeyraCore SafeShell",
        sourceUrl = "https://github.com/ctrl-mietze/VeyraCore",
        basis = "Bounded su -c id proof plus fixed module marker commands; no provider source imported.",
        importedArtifacts = false
    )

    override fun evaluate(device: DeviceInfo, proof: RootProbe): ProviderEvaluation = ProviderEvaluation(
        methodId = methodId,
        displayName = displayName,
        status = if (proof.isUid0) ProviderStatus.SUPPORTED else ProviderStatus.UNAVAILABLE,
        detail = if (proof.isUid0) {
            "UID 0 wurde mit su -c id in dieser Sitzung nachgewiesen."
        } else {
            "Erfordert einen aktuellen, echten UID-0-Nachweis durch su -c id."
        },
        exactProfileMatched = proof.isUid0,
        provenance = provenance
    )

    override suspend fun prepare(device: DeviceInfo): ProviderPreparation = ProviderPreparation(
        status = ProviderStatus.SUPPORTED,
        started = false,
        detail = "Der vorhandene su-Zugang wird nur für die ausdrücklich unterstützten Diagnose- und Markeraktionen verwendet."
    )
}

/** Exact device profile. Nullable fields are optional only for the compatibility helper below. */
data class SupportedProfile(
    val manufacturer: String,
    val model: String,
    val fingerprint: String,
    val kernel: String,
    val firmwareId: String? = null,
    val abi: String? = null,
    val pageSize: Int? = null,
    val kmi: String? = null
) {
    val isComplete: Boolean
        get() = manufacturer.isUsable() && model.isUsable() && fingerprint.isUsable() && kernel.isUsable() &&
            firmwareId.isUsable() && abi.isUsable() && pageSize != null && pageSize > 0 && kmi.isUsable()

    private fun String?.isUsable(): Boolean = !isNullOrBlank() && this != "unknown" && this != "Unbekannt"
}

object ProfileMatcher {
    /** Optional fields act as constraints when present; this keeps old callers source-compatible. */
    fun matches(profile: SupportedProfile, info: DeviceInfo): Boolean =
        profile.manufacturer.equals(info.manufacturer, ignoreCase = true) &&
            profile.model.equals(info.model, ignoreCase = true) &&
            profile.fingerprint == info.fingerprint &&
            profile.kernel == info.kernel &&
            (profile.firmwareId == null || profile.firmwareId == info.firmwareId) &&
            (profile.abi == null || profile.abi == info.abi) &&
            (profile.pageSize == null || profile.pageSize == info.pageSize) &&
            (profile.kmi == null || profile.kmi == info.kmi)

    /** Provider compatibility requires every identity dimension, including firmware and hardware ABI. */
    fun matchesExact(profile: SupportedProfile, info: DeviceInfo): Boolean = profile.isComplete && matches(profile, info)
}

object RootIdentity {
    private val uidAtStart = Regex(
        "^uid=(\\d+)(?:\\([^)]*\\))?(?:\\s|$).*",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )

    /** Parses only the leading uid field; gid=0 or a later uid=0 token can never pass. */
    fun parseUid(identity: String): Int? = uidAtStart.matchEntire(identity.trim())?.groupValues?.get(1)?.toIntOrNull()

    fun classify(uid: Int?): UidClass = when (uid) {
        0 -> UidClass.ROOT
        1000 -> UidClass.SYSTEM
        2000 -> UidClass.ADB_SHELL
        null -> UidClass.UNKNOWN
        else -> UidClass.APP
    }
}

object ModuleId {
    private val safe = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")
    fun isSafe(value: String): Boolean = safe.matches(value) && value !in setOf(".", "..")
}

data class ModuleZipPreview(
    val displayName: String,
    val archiveBytes: Int,
    val entryCount: Int,
    val entries: List<String>,
    val moduleId: String,
    val moduleName: String,
    val moduleVersion: String,
    val moduleAuthor: String,
    val moduleDescription: String
)

/** Bounded, text-only module ZIP inspection. It never extracts, sources, or executes archive files. */
object ModuleZipVerifier {
    const val MAX_ARCHIVE_BYTES = 64 * 1024 * 1024
    private const val MAX_UNCOMPRESSED_BYTES = 128 * 1024 * 1024L
    private const val MAX_MODULE_PROP_BYTES = 64 * 1024
    private const val MAX_ENTRIES = 2048
    private const val EOCD_SIGNATURE = 0x06054b50L
    private const val CENTRAL_SIGNATURE = 0x02014b50L

    fun inspect(input: InputStream, displayName: String): Result<ModuleZipPreview> = runCatching {
        inspectBytes(readBounded(input, MAX_ARCHIVE_BYTES), displayName).getOrThrow()
    }

    fun inspectBytes(bytes: ByteArray, displayName: String): Result<ModuleZipPreview> = runCatching {
        require(bytes.isNotEmpty()) { "ZIP ist leer" }
        require(bytes.size <= MAX_ARCHIVE_BYTES) { "ZIP überschreitet 64 MiB" }
        require(!centralDirectoryHasSymlink(bytes)) { "ZIP enthält einen symbolischen Link" }

        val names = LinkedHashSet<String>()
        var entryCount = 0
        var uncompressedBytes = 0L
        var modulePropText: String? = null
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount += 1
                require(entryCount <= MAX_ENTRIES) { "ZIP enthält zu viele Einträge" }
                validateEntryPath(entry.name)
                require(names.add(entry.name)) { "Doppelter ZIP-Pfad: ${entry.name}" }
                if (!entry.isDirectory) {
                    val limit = if (entry.name == "module.prop") MAX_MODULE_PROP_BYTES.toLong() else MAX_UNCOMPRESSED_BYTES
                    val content = readEntryBounded(zip, limit)
                    uncompressedBytes += content.size
                    require(uncompressedBytes <= MAX_UNCOMPRESSED_BYTES) { "ZIP ist unkomprimiert zu groß" }
                    if (entry.name == "module.prop") {
                        modulePropText = decodeModuleProp(content)
                    }
                }
            }
        }
        val prop = modulePropText ?: error("module.prop fehlt im ZIP-Root")
        val values = parseModuleProp(prop)
        val id = values["id"].orEmpty()
        require(ModuleId.isSafe(id)) { "Ungültige Modul-ID in module.prop" }
        val name = values["name"].orEmpty().trim()
        require(name.isNotBlank()) { "module.prop benötigt name" }
        ModuleZipPreview(
            displayName = displayName,
            archiveBytes = bytes.size,
            entryCount = entryCount,
            entries = names.toList(),
            moduleId = id,
            moduleName = name,
            moduleVersion = values["version"].orEmpty().ifBlank { "unbekannt" },
            moduleAuthor = values["author"].orEmpty().ifBlank { "unbekannt" },
            moduleDescription = values["description"].orEmpty()
        )
    }

    fun parseModuleProp(text: String): Map<String, String> = buildMap {
        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trimEnd('\r')
            if (line.isBlank() || line.startsWith("#")) return@forEach
            val separator = line.indexOf('=')
            if (separator <= 0) return@forEach
            val key = line.substring(0, separator).trim()
            val value = line.substring(separator + 1).trim()
            if (key.matches(Regex("[A-Za-z0-9._-]+"))) put(key, value)
        }
    }

    fun validateEntryPath(name: String) {
        require(name.isNotBlank()) { "Leerer ZIP-Pfad" }
        require(name.length <= 255) { "ZIP-Pfad ist zu lang" }
        require(!name.contains('\\')) { "Backslash im ZIP-Pfad" }
        require(!name.startsWith('/')) { "Absoluter ZIP-Pfad" }
        require(!Regex("^[A-Za-z]:").containsMatchIn(name)) { "Laufwerkspfad im ZIP" }
        // ZIP directories conventionally end in '/'; preserve that valid form,
        // but reject empty, repeated, dot and parent segments.
        val normalized = if (name.endsWith('/')) name.dropLast(1) else name
        require(normalized.isNotEmpty()) { "Leerer ZIP-Verzeichnispfad" }
        val parts = normalized.split('/')
        require(parts.none { it.isBlank() || it == "." || it == ".." }) { "Unsicherer ZIP-Pfad: $name" }
    }

    /** Exposed for focused unit tests and kept separate from archive traversal. */
    fun isUnixSymlink(mode: Int): Boolean = mode and 0xF000 == 0xA000

    private fun decodeModuleProp(bytes: ByteArray): String {
        require(bytes.none { it == 0.toByte() }) { "module.prop enthält NUL-Zeichen" }
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun readEntryBounded(input: InputStream, maxBytes: Long): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= maxBytes) { "ZIP-Eintrag überschreitet Sicherheitslimit" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun readBounded(input: InputStream, maxBytes: Int): ByteArray {
        input.use {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val count = it.read(buffer)
                if (count < 0) break
                total += count
                require(total <= maxBytes) { "ZIP überschreitet 64 MiB" }
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        }
    }

    private fun centralDirectoryHasSymlink(bytes: ByteArray): Boolean {
        val eocd = findEndOfCentralDirectory(bytes)
        require(eocd >= 0) { "ZIP-Ende fehlt" }
        val centralSize = readUInt32(bytes, eocd + 12)
        val centralOffset = readUInt32(bytes, eocd + 16)
        require(centralSize != 0xFFFFFFFFL && centralOffset != 0xFFFFFFFFL) { "ZIP64 ist nicht akzeptiert" }
        val end = centralOffset + centralSize
        require(centralOffset <= bytes.size && end <= bytes.size && end >= centralOffset) { "ZIP-Central-Directory ungültig" }
        var cursor = centralOffset.toInt()
        val endInt = end.toInt()
        while (cursor < endInt) {
            require(cursor + 46 <= endInt && readUInt32(bytes, cursor) == CENTRAL_SIGNATURE) { "ZIP-Central-Directory beschädigt" }
            val madeBy = readUInt16(bytes, cursor + 4)
            val nameLength = readUInt16(bytes, cursor + 28)
            val extraLength = readUInt16(bytes, cursor + 30)
            val commentLength = readUInt16(bytes, cursor + 32)
            val externalAttributes = readUInt32(bytes, cursor + 38)
            val unixMode = (externalAttributes ushr 16).toInt()
            if ((madeBy ushr 8) == 3 && isUnixSymlink(unixMode)) return true
            cursor += 46 + nameLength + extraLength + commentLength
        }
        require(cursor == endInt) { "ZIP-Central-Directory endet unerwartet" }
        return false
    }

    private fun findEndOfCentralDirectory(bytes: ByteArray): Int {
        val start = (bytes.size - 65_557).coerceAtLeast(0)
        for (index in bytes.size - 22 downTo start) {
            if (index >= 0 && readUInt32(bytes, index) == EOCD_SIGNATURE) return index
        }
        return -1
    }

    private fun readUInt16(bytes: ByteArray, offset: Int): Int {
        require(offset >= 0 && offset + 2 <= bytes.size) { "ZIP-Lesefehler" }
        return (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun readUInt32(bytes: ByteArray, offset: Int): Long {
        require(offset >= 0 && offset + 4 <= bytes.size) { "ZIP-Lesefehler" }
        return (bytes[offset].toLong() and 0xFF) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 3].toLong() and 0xFF) shl 24)
    }
}

class SafeShell {
    suspend fun su(command: String, timeoutSeconds: Long = 6): ShellResult = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
            val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return@withContext ShellResult(null, "", timedOut = true, error = "Zeitlimit nach ${timeoutSeconds}s")
            }
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText().take(24_000) }
            ShellResult(process.exitValue(), output.trim())
        } catch (e: Exception) {
            ShellResult(null, "", error = e.message ?: e.javaClass.simpleName)
        }
    }

    suspend fun probe(): RootProbe {
        val identity = su("id", 6)
        val uid = RootIdentity.parseUid(identity.output)
        val uidClass = RootIdentity.classify(uid)
        val root = identity.exitCode == 0 && uid == 0
        if (!root) {
            return RootProbe(
                state = if (identity.error != null || identity.timedOut) Verification.ERROR else Verification.NOT_VERIFIED,
                detail = identity.error ?: if (identity.timedOut) "su-Prüfung zeitlich begrenzt abgebrochen" else identity.output.ifBlank { "Kein UID-0-Nachweis" },
                uid = uid,
                uidClass = uidClass,
                identity = identity.output.ifBlank { null }
            )
        }
        val security = su("getenforce 2>/dev/null || echo unknown", 3)
        val daemon = su("if [ -S /dev/socket/veyra_loadd ]; then echo connected; else echo absent; fi", 3)
        val runtime = when {
            daemon.exitCode != 0 -> RuntimeStatus.ERROR
            daemon.output.trim() == "connected" -> RuntimeStatus.CONNECTED
            daemon.output.trim() == "absent" -> RuntimeStatus.ABSENT
            else -> RuntimeStatus.UNKNOWN
        }
        return RootProbe(
            state = Verification.VERIFIED,
            detail = identity.output,
            uid = uid,
            uidClass = uidClass,
            identity = identity.output,
            selinux = security.output.ifBlank { "unknown" },
            runtime = runtime,
            runtimeDetail = daemon.output.ifBlank { daemon.error ?: "Keine Laufzeitantwort" }
        )
    }

    suspend fun modules(): Result<List<RootModule>> = runCatching {
        val result = su(
            "for d in /data/adb/modules/*; do [ -d \"\$d\" ] || continue; id=\${d##*/}; case \"\$id\" in *[!A-Za-z0-9._-]*|'') continue;; esac; p=\"\$d/module.prop\"; readprop() { [ -r \"\$p\" ] && sed -n \"s/^\$1=//p\" \"\$p\" | head -n 1 | tr '\\036\\037\\n' '   '; }; name=\$(readprop name); version=\$(readprop version); author=\$(readprop author); description=\$(readprop description); [ -f \"\$d/disable\" ] && disabled=1 || disabled=0; [ -f \"\$d/remove\" ] && removed=1 || removed=0; printf '%s\\037%s\\037%s\\037%s\\037%s\\037%s\\037%s\\036' \"\$id\" \"\${name:-\$id}\" \"\${version:-unknown}\" \"\${author:-unknown}\" \"\${description:-}\" \"\$disabled\" \"\$removed\"; done",
            8
        )
        require(result.exitCode == 0) { result.error ?: result.output.ifBlank { "Modulpfad nicht zugreifbar" } }
        result.output.split('\u001e').filter { it.isNotBlank() }.mapNotNull { row ->
            val fields = row.split('\u001f')
            if (fields.size == 7 && ModuleId.isSafe(fields[0])) {
                RootModule(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5] == "1", fields[6] == "1")
            } else null
        }
    }

    suspend fun moduleInstallerAvailability(): ModuleInstallerAvailability {
        val result = su("command -v ksud >/dev/null 2>&1 && ksud module install --help 2>&1", 5)
        val supportsInstall = result.exitCode == 0 && Regex("(?i)module\\s+install").containsMatchIn(result.output)
        return if (supportsInstall) {
            ModuleInstallerAvailability(true, "ksud module install wurde über die autorisierte su-Runtime verifiziert.")
        } else {
            ModuleInstallerAvailability(false, "ksud module install ist in der aktuellen Runtime nicht nachgewiesen; ZIP-Installation bleibt deaktiviert.")
        }
    }

    suspend fun installModuleZip(cacheFile: File): ShellResult {
        val canonical = cacheFile.canonicalFile
        require(canonical.isFile) { "Temporäre ZIP-Datei fehlt" }
        return su("ksud module install ${shellQuote(canonical.path)}", 20)
    }

    suspend fun setModuleDisabled(id: String, disabled: Boolean): Result<Unit> = guardedModuleCommand(id, "disable", disabled)

    suspend fun markModuleRemoval(id: String): Result<Unit> = guardedModuleCommand(id, "remove", true)

    private suspend fun guardedModuleCommand(id: String, marker: String, create: Boolean): Result<Unit> = runCatching {
        require(ModuleId.isSafe(id)) { "Ungültige Modul-ID" }
        val command = if (create) "touch /data/adb/modules/$id/$marker" else "rm -f /data/adb/modules/$id/$marker"
        val result = su(command, 5)
        require(result.exitCode == 0) { result.error ?: result.output.ifBlank { "Shell-Befehl fehlgeschlagen" } }
    }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
}

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("veyracore", Context.MODE_PRIVATE)

    var berry: Boolean
        get() = prefs.getBoolean("berry", false)
        set(value) { prefs.edit().putBoolean("berry", value).apply() }

    var selectedMethodId: String?
        get() = prefs.getString("selected_method", null)
        set(value) { prefs.edit().putString("selected_method", value).apply() }

    fun append(log: AppLog) {
        val encoded = Base64.getEncoder().encodeToString(
            "${log.epochMs}|${log.severity}|${log.event}|${log.detail}".toByteArray(StandardCharsets.UTF_8)
        )
        val all = (prefs.getString("logs", "") ?: "").split(',').filter { it.isNotBlank() }.takeLast(199) + encoded
        prefs.edit().putString("logs", all.joinToString(",")).apply()
    }

    fun logs(): List<AppLog> = (prefs.getString("logs", "") ?: "").split(',').mapNotNull { entry ->
        runCatching { String(Base64.getDecoder().decode(entry), StandardCharsets.UTF_8).split('|', limit = 4) }
            .getOrNull()
            ?.let { parts ->
                if (parts.size == 4) runCatching { AppLog(parts[0].toLong(), Severity.valueOf(parts[1]), parts[2], parts[3]) }.getOrNull() else null
            }
    }.sortedByDescending { it.epochMs }

    fun clearLogs() = prefs.edit().remove("logs").apply()
}

data class PortResearchEntry(
    val name: String,
    val repositoryUrl: String,
    val documentedScope: String,
    val veyraStatus: String = "Research only · not integrated · no Veyra device compatibility claim"
)

object PortResearchCatalog {
    val entries: List<PortResearchEntry> = listOf(
        PortResearchEntry("Root My Galaxy Next", "https://github.com/rushiranpise/Root-My-Galaxy-Next", "Samsung Galaxy installer with explicitly documented model/kernel combinations."),
        PortResearchEntry("DirtyFrag", "https://github.com/mitschud/DirtyFrag", "Samsung and other documented temporary-root research around CVE-2026-43284 and KMI-specific artifacts."),
        PortResearchEntry("KernelSU", "https://github.com/tiann/KernelSU", "KernelSU upstream interoperability and module/runtime reference; no KernelSU source is imported here."),
        PortResearchEntry("GhostSam", "https://github.com/snothin/GhostSam", "Samsung GhostLock/DirtyFrag research and device-specific payload architecture."),
        PortResearchEntry("GhostLock-Galaxy", "https://github.com/wxxsfxyzm/GhostLock-Galaxy", "Galaxy Z Fold6 exact kernel profiles with shell/Shizuku execution requirements."),
        PortResearchEntry("ghostlock-a17", "https://github.com/mobilehackinglab/ghostlock-a17", "Galaxy A17 SM-A175F Android 16/GKI 6.12 exact-target research."),
        PortResearchEntry("Root My Pixel", "https://github.com/alex193a/Root-My-Pixel", "Pixel temporary-root app and exact build/payload research."),
        PortResearchEntry("GhostLock-oneplus", "https://github.com/JoinChang/ghostlock-oneplus", "OnePlus/OPPO/Xiaomi GhostLock research with verified kernel/build tables."),
        PortResearchEntry("Root My Device", "https://github.com/tqmane/Root-My-Device", "Exact-build Nothing Phone (3a) and OnePlus Pad 3 projects; separate app sets, not a universal port."),
        PortResearchEntry("Ghostlock-5.10", "https://github.com/CrackerCat/GhostLock-5.10", "Amazon Fire 5.10 device-specific GhostLock research."),
        PortResearchEntry("RootMyVivo", "https://github.com/zenyxx-xd/RootMyVivo", "vivo/iQOO temporary-root app and live payload-catalog research.")
    )
}
