package ctrl.veyracore

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class PayloadProfile(
    val id: String,
    val label: String,
    val source: String,
    val models: List<String>,
    val kernels: List<String>,
    val fingerprints: List<String> = emptyList()
) {
    fun matches(device: DeviceInfo): Boolean {
        val kernel = KernelVersion.extract(device.kernel) ?: return false
        return models.any { it.equals(device.model, ignoreCase = true) } &&
            kernels.any { it == kernel } &&
            (fingerprints.isEmpty() || fingerprints.contains(device.fingerprint))
    }
}

/** Numeric KMI release, e.g. 6.6.98 from 6.6.98-android15-8-... */
object KernelVersion {
    fun extract(release: String): String? =
        Regex("^(\\d+)\\.(\\d+)\\.(\\d+)").find(release.trim())?.value
}

data class ProfileFeedResult(
    val profiles: List<PayloadProfile>,
    val message: String,
    val fromCache: Boolean,
    val internalProfiles: List<InternalRootProfile> = emptyList()
)

/** Read-only metadata. Deliberately never downloads or executes exploit / daemon artifacts. */
class PayloadFeeds(private val context: Context) {
    private val prefs = context.getSharedPreferences("root_profile_feeds", Context.MODE_PRIVATE)

    private data class Source(val key: String, val url: String)
    private val sources = listOf(
        Source("RMG Next",
            "https://raw.githubusercontent.com/rushiranpise/Root-My-Galaxy-Payloads/main/support/targets-v3.json"),
        Source("Root My Galaxy",
            "https://raw.githubusercontent.com/BuSung-dev/Root-My-Galaxy-Payloads/main/support/targets-v3.json")
    )

    suspend fun refresh(): ProfileFeedResult = withContext(Dispatchers.IO) {
        val found = mutableListOf<PayloadProfile>()
        val internal = mutableListOf<InternalRootProfile>()
        val errors = mutableListOf<String>()
        var cacheCount = 0
        for (source in sources) {
            val latest = runCatching { fetchBounded(source.url) }.getOrElse {
                errors += "${source.key}: ${it.javaClass.simpleName}"
                null
            }
            val raw = latest ?: prefs.getString("raw_${source.key}", null)?.also { cacheCount++ }
            if (raw == null) continue
            try {
                found += parse(source.key, raw)
                internal += InternalRootProfileParser.parse(
                    source.key, raw.toByteArray(Charsets.UTF_8)
                ).profiles
                if (latest != null) {
                    prefs.edit().putString("raw_${source.key}", raw)
                        .putLong("timestamp_${source.key}", System.currentTimeMillis()).apply()
                }
            } catch (e: Exception) {
                errors += "${source.key}: ungültiger Katalog"
            }
        }
        val merged = found.distinctBy { Triple(it.source, it.id, it.models.joinToString()) }
        val detail = "${merged.size} Profile aus ${sources.size} Feeds" +
            (if (cacheCount > 0) " · ${cacheCount} Cache-Quelle(n)" else "") +
            (if (errors.isEmpty()) "" else " · " + errors.joinToString("; "))
        ProfileFeedResult(merged, detail, cacheCount > 0,
            internal.distinctBy { it.source + ":" + it.id })
    }

    private fun fetchBounded(source: String): String {
        require(source.startsWith("https://raw.githubusercontent.com/"))
        val connection = URL(source).openConnection() as HttpURLConnection
        connection.connectTimeout = 7_000
        connection.readTimeout = 8_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/json")
        try {
            require(connection.responseCode == 200) { "HTTP ${connection.responseCode}" }
            val result = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val bytes = ByteArray(16_384)
                while (true) {
                    val n = input.read(bytes)
                    if (n == -1) break
                    require(result.size() + n <= 1_000_000) { "Feed zu groß" }
                    result.write(bytes, 0, n)
                }
            }
            return result.toString(Charsets.UTF_8.name())
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        /** Pure parse/validation. A published match is not a guarantee of exploit success. */
        fun parse(name: String, json: String): List<PayloadProfile> {
            val root = JSONObject(json)
            require(root.optInt("schemaVersion", -1) == 3) { "Unbekanntes Feed-Schema" }
            val payloads = root.getJSONArray("payloads")
            require(payloads.length() <= 2_048) { "Zu viele Einträge" }
            return (0 until payloads.length()).mapNotNull { index ->
                val entry = payloads.optJSONObject(index) ?: return@mapNotNull null
                val id = entry.optString("payloadId").take(140)
                if (id.isBlank() || !Regex("[A-Za-z0-9._-]+").matches(id)) return@mapNotNull null
                val modelValues = entry.optJSONArray("models") ?: return@mapNotNull null
                val kernelValues = entry.optJSONArray("kernelVersions") ?: return@mapNotNull null
                val models = (0 until modelValues.length()).map { modelValues.optString(it) }
                    .filter { it.isNotBlank() }.take(80)
                val kernels = (0 until kernelValues.length()).map { kernelValues.optString(it) }
                    .filter { Regex("\\d+\\.\\d+\\.\\d+").matches(it) }.take(16)
                if (models.isEmpty() || kernels.isEmpty()) return@mapNotNull null
                val fp = entry.optJSONArray("fingerprints")
                val fingerprints = if (fp == null) emptyList() else
                    (0 until fp.length()).map { fp.optString(it) }.filter { it.isNotBlank() }.take(64)
                PayloadProfile(
                    id = id,
                    label = entry.optString("displayName", id).take(180),
                    source = name,
                    models = models,
                    kernels = kernels,
                    fingerprints = fingerprints
                )
            }
        }

        fun matching(profiles: List<PayloadProfile>, device: DeviceInfo): List<PayloadProfile> =
            profiles.filter { it.matches(device) }.sortedWith(
                compareBy<PayloadProfile> { if (it.source == "RMG Next") 0 else 1 }
                    .thenBy { it.label }
            )
    }
}