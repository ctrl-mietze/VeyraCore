package ctrl.veyracore

import android.content.Context
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Integrated, READ-ONLY profile engine derived from Veyra Root's SupportManifest evidence rules.
 *
 * A matching model/KMI does not grant root. Exploit files are not loaded, staged or run here.
 */
enum class ProfileEvidence(val rank: Int, val label: String) {
    NO_MATCH(0, "Kein Treffer"),
    SERIES_CANDIDATE(1, "Kernel-Serie · Analyse"),
    MODEL_KERNEL_CANDIDATE(2, "Modell + Kernel · Kandidat"),
    EXACT_KERNEL(3, "Vollständiger Kernel · Profiltreffer"),
    EXACT_BUILD(4, "Modell + Kernel + Build · Profiltreffer")
}

data class ArtifactDescriptor(
    val size: Long?,
    val sha256: String?,
    val digestDeclared: Boolean
)

data class InternalRootProfile(
    val id: String,
    val name: String,
    val source: String,
    val models: Set<String>,
    val devices: Set<String>,
    val buildIds: Set<String>,
    val kernels: Set<String>,
    val flavor: String,
    val exploitEvidence: ArtifactDescriptor?,
    val runtimeEvidence: ArtifactDescriptor?
) {
    val wildcardModel: Boolean get() = "*" in models

    /** Exact evidence grading, intentionally not a promise that native code will work. */
    fun evidence(device: DeviceInfo): ProfileEvidence {
        val model = device.model.trim()
        val codename = device.codename.trim()
        val exactModel = models.any { it.equals(model, true) } ||
            devices.any { it.equals(codename, true) }
        val wildcard = models.any { it == "*" } ||
            models.any { it.endsWith("*") && model.startsWith(it.dropLast(1), true) }
        if (!exactModel && !wildcard) return ProfileEvidence.NO_MATCH

        val fullKernel = device.kernel.trim()
        val three = KernelVersion.extract(fullKernel)
        val exactRelease = kernels.any { it == fullKernel }
        val matchedNumeric = kernels.any { it == three }
        val series = kernels.any { series ->
            Regex("^\\d+\\.\\d+$").matches(series) &&
                fullKernel.startsWith("${series}.")
        }
        if (!exactRelease && !matchedNumeric && !series) return ProfileEvidence.NO_MATCH

        val build = device.buildId.trim()
        val buildMatched = buildIds.isEmpty() ||
            buildIds.any { value ->
                value.equals(build, true) ||
                    build.startsWith(value, true) ||
                    device.fingerprint.contains(value, true)
            }
        if (!buildMatched) return ProfileEvidence.NO_MATCH

        if (!exactModel) return ProfileEvidence.SERIES_CANDIDATE
        if (exactRelease && buildIds.isNotEmpty()) return ProfileEvidence.EXACT_BUILD
        if (exactRelease) return ProfileEvidence.EXACT_KERNEL
        return ProfileEvidence.MODEL_KERNEL_CANDIDATE
    }

    /** Requires more than a matching kernel version to be considered fully build-verified. */
    fun buildVerified(device: DeviceInfo): Boolean = evidence(device) == ProfileEvidence.EXACT_BUILD
}

data class InternalCatalogResult(
    val profiles: List<InternalRootProfile>,
    val sourceName: String,
    val manifestSha256: String,
    val note: String
)

object InternalRootProfileParser {
    private val validId = Regex("[A-Za-z0-9._-]{1,140}")
    private val validDigest = Regex("[0-9a-f]{64}")

    fun parse(source: String, bytes: ByteArray): InternalCatalogResult {
        require(bytes.size in 2..1_000_000) { "Manifestgröße nicht unterstützt" }
        val root = JSONObject(String(bytes, Charsets.UTF_8))
        require(root.optInt("schemaVersion", -1) == 3) { "Nur targets-v3 wird unterstützt" }
        val arr = root.getJSONArray("payloads")
        require(arr.length() <= 2048) { "Zu viele Profile" }
        val profiles = (0 until arr.length()).mapNotNull { index ->
            val entry = arr.optJSONObject(index) ?: return@mapNotNull null
            val id = entry.optString("payloadId")
            if (!validId.matches(id)) return@mapNotNull null

            fun strings(key: String, max: Int = 96): Set<String> {
                val a = entry.optJSONArray(key) ?: return emptySet()
                require(a.length() <= 1024) { "Zu viele $key" }
                return (0 until minOf(a.length(), max)).mapNotNull { x ->
                    a.optString(x).trim().takeIf { it.isNotBlank() && it.length <= 180 }
                }.toSet()
            }

            val models = strings("models")
            val exactKernel = entry.optJSONObject("exactMatch")
                ?.optString("kernelRelease")?.trim()?.takeIf { it.isNotEmpty() }
            // The original Veyra Root SupportManifest folds exactMatch.kernelRelease
            // into the candidates before grading. A three-part KMI is not the same
            // thing as this complete, firmware-specific kernel string.
            val kernels = strings("kernelVersions") + listOfNotNull(exactKernel)
            if (models.isEmpty() || kernels.isEmpty()) return@mapNotNull null

            fun artifact(key: String): ArtifactDescriptor? {
                val e = entry.optJSONObject(key) ?: return null
                val size = e.optLong("size", -1).takeIf { it >= 0 }
                val hash = e.optString("sha256").lowercase().takeIf { it.isNotBlank() }
                require(hash == null || validDigest.matches(hash)) { "Ungültiger $key Hash" }
                return ArtifactDescriptor(size, hash, hash != null)
            }
            InternalRootProfile(
                id = id,
                name = entry.optString("displayName", id).take(180),
                source = source,
                models = models,
                devices = strings("devices"),
                buildIds = strings("buildIds"),
                kernels = kernels,
                flavor = entry.optString("flavor", "kernelsu").take(64),
                exploitEvidence = artifact("exploit"),
                runtimeEvidence = artifact("kernelsu")
            )
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return InternalCatalogResult(
            profiles = profiles.distinctBy { it.id },
            sourceName = source,
            manifestSha256 = digest,
            note = "Interne Datenprüfung · kein Kernel-Exploit geladen"
        )
    }
}


/** Bundles Veyra Root's own DF and Pixel profile DATA, not its native code assets. */
class VeyraInternalCatalog(private val context: Context) {
    private fun parseAsset(assetName: String, sourceName: String): Result<InternalCatalogResult> =
        runCatching {
            val content = context.assets.open(assetName).use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(4096)
                while (true) {
                    val n = input.read(buf)
                    if (n == -1) break
                    require(out.size() + n <= 1_000_000)
                    out.write(buf, 0, n)
                }
                out.toByteArray()
            }
            InternalRootProfileParser.parse(sourceName, content)
        }

    fun loadLocal(): Result<InternalCatalogResult> =
        parseAsset("veyra-profiles/dfroot-targets-v3.json", "Veyra Root · DF local")

    fun loadPixel(): Result<InternalCatalogResult> =
        parseAsset("veyra-profiles/pixel-targets-v3.json", "Veyra Root · Pixel local")

    fun loadAll(): List<Result<InternalCatalogResult>> = listOf(loadLocal(), loadPixel())
}