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
import android.os.SystemClock

data class HelperSnapshot(
    val packageName: String? = null,
    val uid: Int? = null,
    val detail: String = "Helper noch nicht geprüft"
) {
    val installed: Boolean get() = packageName != null
    /** A PackageManager result after install proves that shared system UID has been accepted. */
    val acceptedSystemUid: Boolean get() = uid == Process.SYSTEM_UID
}

object HelperInspector {
    val acceptedPackages = listOf(
        "ctrl.mietze.veyracore.helper",
        "ctrl.veyracore.helper",
        "dev.rushiranpise.rmgnext.helper"
    )

    fun check(context: Context): HelperSnapshot {
        for (name in acceptedPackages) {
            val application = try {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(name, 0)
            } catch (_: PackageManager.NameNotFoundException) { continue }
            val uid = application.uid
            return HelperSnapshot(name, uid,
                if (uid == Process.SYSTEM_UID)
                    "PackageManager bestätigt System-UID 1000 für $name"
                else "Helper installiert, aber UID $uid statt System-UID 1000")
        }
        return HelperSnapshot(detail = "Kein freigegebener Veyra- oder RMG-Helper installiert")
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
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, enabled).apply()

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    fun isShown(context: Context): Boolean = runCatching {
        (context.getSystemService(NotificationManager::class.java))
            .activeNotifications.any { it.id == ID }
    }.getOrDefault(false)

    /** Only start from a foreground Activity (or a permitted boot broadcast). Never show before UID1000 proof. */
    fun sync(context: Context): Boolean {
        if (!enabled(context) || !HelperInspector.check(context).acceptedSystemUid) {
            runCatching { context.stopService(Intent(context, VeyraHelperNotificationService::class.java)) }
            return false
        }
        if (!hasPermission(context)) return false
        return runCatching {
            val intent = Intent(context, VeyraHelperNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent)
            else context.startService(intent)
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
            NotificationChannel(
                HelperNotificationControl.CHANNEL, "Veyra Helper Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Permanenter lokaler Status eines bestätigten Veyra System-Helpers"
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
            .setContentText("Helper bestätigt. Root (UID 0) wird separat geprüft.")
            .setContentIntent(open)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(HelperNotificationControl.ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
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
        // A userspace soft reboot does not reset SystemClock.elapsedRealtime: we do not claim it did.
        val prefs = context.getSharedPreferences(HelperNotificationControl.PREFS, Context.MODE_PRIVATE)
        prefs.edit().putLong("last_boot_observed_elapsed", SystemClock.elapsedRealtime()).apply()
        runCatching { HelperNotificationControl.sync(context) }
    }
}