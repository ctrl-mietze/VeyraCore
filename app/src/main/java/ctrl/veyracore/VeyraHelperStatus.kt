package ctrl.veyracore

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Process
import java.security.MessageDigest

/** Current package-manager evidence, not a claim to have injected an OS certificate. */
data class HelperSnapshot(
    val packageName: String? = null,
    val uid: Int? = null,
    val detail: String = "Helper noch nicht geprüft",
    val versionName: String? = null,
    val signerSha256: String? = null,
    val checkedAt: Long = 0L
) {
    val installed: Boolean get() = packageName != null
    val acceptedSystemUid: Boolean get() = installed && uid == Process.SYSTEM_UID

    val summary: String get() = when {
        acceptedSystemUid -> "System-UID 1000 vom PackageManager bestätigt"
        installed -> "Helper installiert, jedoch nicht als System-UID"
        else -> "Helper nicht installiert"
    }
}

/** Preferred exact-known package IDs. A random application is never treated as trusted helper. */
object HelperInspector {
    val acceptedPackages = listOf(
        "ctrl.mietze.veyracore.helper",
        "ctrl.veyracore.helper",
        "dev.rushiranpise.rmgnext.helper"
    )

    fun check(context: Context): HelperSnapshot {
        val pm = context.packageManager
        val discovered = acceptedPackages.mapNotNull { name ->
            try {
                val pkg = if (Build.VERSION.SDK_INT >= 33)
                    pm.getPackageInfo(name, PackageManager.PackageInfoFlags.of(
                        PackageManager.GET_SIGNING_CERTIFICATES.toLong()
                    ))
                else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(name, PackageManager.GET_SIGNING_CERTIFICATES)
                }
                val app = pkg.applicationInfo ?: return@mapNotNull null
                val uid = app.uid
                val cert = pkg.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
                val signature = cert?.let {
                    MessageDigest.getInstance("SHA-256").digest(it)
                        .joinToString("") { part -> "%02x".format(part.toInt() and 0xff) }
                }
                HelperSnapshot(
                    packageName = name,
                    uid = uid,
                    versionName = pkg.versionName,
                    signerSha256 = signature,
                    checkedAt = System.currentTimeMillis(),
                    detail = if (uid == Process.SYSTEM_UID)
                        "UID 1000: $name · Signatur-Fingerprint erfasst"
                    else "Installiert: $name, UID $uid (nicht android.uid.system)"
                )
            } catch (_: PackageManager.NameNotFoundException) {
                null
            } catch (e: Exception) {
                HelperSnapshot(packageName = name,
                    detail = "Paketinformation nicht vollständig lesbar: ${e.javaClass.simpleName}")
            }
        }
        // Crucial: do not stop after the first installed but non-system package.
        return discovered.firstOrNull { it.acceptedSystemUid }
            ?: discovered.firstOrNull()
            ?: HelperSnapshot(detail = "Kein kompatibler Veyra/RMG-Helper gefunden")
    }
}

object HelperNotificationControl {
    const val CHANNEL = "veyra_core_helper"
    const val ID = 1102
    const val PREFS = "veyracore_helper"
    private const val ENABLED = "notifications_enabled"

    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, true)

    fun updatePreference(context: Context, enabled: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(ENABLED, enabled).apply()

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    fun isShown(context: Context): Boolean = runCatching {
        context.getSystemService(NotificationManager::class.java)
            .activeNotifications.any { it.id == ID }
    }.getOrDefault(false)

    /** Call from a foreground Activity or a permitted OS boot context. */
    fun sync(context: Context): Boolean {
        if (!enabled(context) || !HelperInspector.check(context).acceptedSystemUid) {
            runCatching {
                context.stopService(Intent(context, VeyraHelperNotificationService::class.java))
            }
            return false
        }
        if (!hasPermission(context)) return false
        return runCatching {
            val i = Intent(context, VeyraHelperNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i)
            else context.startService(i)
            true
        }.getOrDefault(false)
    }
}

class VeyraHelperNotificationService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val helper = HelperInspector.check(this)
        if (!helper.acceptedSystemUid || !HelperNotificationControl.enabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(HelperNotificationControl.CHANNEL,
                "Veyra Helper", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Aktueller Status des installierten System-UID-Helpers"
                setShowBadge(false)
            }
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = Notification.Builder(this, HelperNotificationControl.CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Veyra Helper · System-UID 1000")
            .setContentText("Helper ${helper.versionName ?: ""} erkannt · Root UID0 separat prüfen")
            .setContentIntent(open)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 34)
                startForeground(HelperNotificationControl.ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else {
                @Suppress("DEPRECATION")
                startForeground(HelperNotificationControl.ID, notification)
            }
        } catch (_: RuntimeException) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }
}

class VeyraHelperBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // Boot delivery never claims a kernel exploit or a UID0 grant.
        runCatching { HelperNotificationControl.sync(context) }
    }
}