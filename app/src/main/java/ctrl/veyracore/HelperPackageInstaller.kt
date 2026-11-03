package ctrl.veyracore

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Own helper APK update flow: byte verification, signer validation, explicit system installer.
 * Does NOT alter android.uid.system signature history or bypass PackageManager.
 */
data class StagedHelperApk(
    val file: File,
    val packageName: String,
    val versionName: String,
    val signature: String,
    val length: Long
)

object HelperSigner {
    fun hash(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

class HelperPackageInstaller(private val context: Context) {
    companion object {
        const val MAX_APK_BYTES = 32L * 1024 * 1024
        const val MIME = "application/vnd.android.package-archive"
    }
    suspend fun stage(uri: Uri): Result<StagedHelperApk> = withContext(Dispatchers.IO) {
        runCatching {
            require(uri.scheme == "content") { "Nur die Android-Dokumentauswahl ist erlaubt" }
            val dir = File(context.cacheDir, "helper-apk").apply { mkdirs() }
            val outFile = File(dir, "helper-update.apk")
            try {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: error("Datei kann nicht gelesen werden")
                stream.use { input ->
                    outFile.outputStream().buffered().use { output ->
                        val chunk = ByteArray(16 * 1024)
                        var total = 0L
                        while (true) {
                            val n = input.read(chunk)
                            if (n < 0) break
                            total += n
                            require(total <= MAX_APK_BYTES) { "Helper-APK zu groß" }
                            output.write(chunk, 0, n)
                        }
                    }
                }
                require(outFile.length() in 1..MAX_APK_BYTES)
                val pm = context.packageManager
                val archive = if (Build.VERSION.SDK_INT >= 33) {
                    pm.getPackageArchiveInfo(outFile.path,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageArchiveInfo(outFile.path, PackageManager.GET_SIGNING_CERTIFICATES)
                } ?: error("Keine gültige Android-APK")
                val name = archive.packageName
                require(name in HelperInspector.acceptedPackages) {
                    "Keine freigegebene Veyra/RMG-Helper-Paketkennung: $name"
                }
                val installed = if (Build.VERSION.SDK_INT >= 33) {
                    pm.getPackageInfo(name,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(name, PackageManager.GET_SIGNING_CERTIFICATES)
                }
                val actualUid = installed.applicationInfo?.uid
                require(actualUid == android.os.Process.SYSTEM_UID) {
                    "Für diese Helper-Identität wurde kein bestehender UID1000-Installationsnachweis gefunden"
                }
                val apkCert = archive.signingInfo?.apkContentsSigners?.firstOrNull()
                    ?: error("APK enthält keinen auslesbaren Signaturnachweis")
                val installedCert = installed.signingInfo?.apkContentsSigners?.firstOrNull()
                    ?: error("Vorhandener Helper ohne auslesbare Signatur")
                val apkHash = HelperSigner.hash(apkCert.toByteArray())
                require(apkHash == HelperSigner.hash(installedCert.toByteArray())) {
                    "APK-Signatur weicht vom installierten Helper ab: Android würde das Update ablehnen"
                }
                require(archive.longVersionCode >= installed.longVersionCode) {
                    "APK-Version ist älter als der installierte Helper"
                }
                StagedHelperApk(outFile, name, archive.versionName ?: "Unbekannt",
                    apkHash, outFile.length())
            } catch (e: Exception) {
                outFile.delete()
                throw e
            }
        }
    }

    fun requestInstall(activityContext: Context, staged: StagedHelperApk): Result<Unit> = runCatching {
        require(staged.file.isFile && staged.file.parentFile?.name == "helper-apk")
        require(staged.packageName in HelperInspector.acceptedPackages)
        require(staged.file.length() == staged.length)
        val uri = FileProvider.getUriForFile(
            activityContext, "${activityContext.packageName}.fileprovider", staged.file
        )
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            clipData = android.content.ClipData.newRawUri("Veyra Helper update", uri)
        }
        require(install.resolveActivity(activityContext.packageManager) != null) {
            "Kein Android-APK-Installer verfügbar"
        }
        activityContext.startActivity(install)
    }
}