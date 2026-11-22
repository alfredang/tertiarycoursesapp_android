package com.tertiaryinfotech.tertiarycourses

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as SystemSettings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Device-only preferences (nothing secret): appearance cache and the reminders switch. */
class Settings(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Last known account theme, so launch doesn't flash the wrong appearance. */
    var theme: String
        get() = prefs.getString("theme", "system") ?: "system"
        set(value) = prefs.edit { putString("theme", value) }

    var remindersOn: Boolean
        get() = prefs.getBoolean("reminders", true)
        set(value) = prefs.edit { putBoolean("reminders", value) }

    /** The role this device shows ("View as" on the Account page); empty = the account's first role. */
    var viewRole: String
        get() = prefs.getString("view_role", "") ?: ""
        set(value) = prefs.edit { if (value.isEmpty()) remove("view_role") else putString("view_role", value) }

    /** The role picked on the sign-in screen ("learner" or "trainer"); null when signed in before it was asked. */
    var loginRole: String?
        get() = prefs.getString("login_role", null)
        set(value) = prefs.edit { if (value == null) remove("login_role") else putString("login_role", value) }

    /** "Remember my email" on the sign-in screen (on by default) and the email it keeps. */
    var rememberEmail: Boolean
        get() = prefs.getBoolean("remember_email", true)
        set(value) = prefs.edit { putBoolean("remember_email", value); if (!value) remove("remembered_email") }

    var rememberedEmail: String
        get() = prefs.getString("remembered_email", "") ?: ""
        set(value) = prefs.edit { putString("remembered_email", value) }

    /** The notification permission prompt is shown once, after the first class load. */
    var askedNotifications: Boolean
        get() = prefs.getBoolean("asked_notifications", false)
        set(value) = prefs.edit { putBoolean("asked_notifications", value) }

    companion object {
        const val FILE = "settings"
    }
}

data class Reminder(val id: Int, val at: Long, val title: String, val text: String) {
    fun toJson(): JSONObject = JSONObject().put("id", id).put("at", at).put("title", title).put("text", text)

    companion object {
        fun from(j: JSONObject) = Reminder(j.optInt("id"), j.optLong("at"), j.optString("title"), j.optString("text"))
    }
}

/**
 * Class reminders as LOCAL notifications: built on the device from the class list, with no
 * push service and no server. For every upcoming or current class (attending or teaching):
 * 3 days before the first day at 09:00, the evening before each class day at 18:00, and on
 * each class day one hour before the start (08:00 when the class has no start time).
 * All times are in the site's time zone. Alarms are inexact (no exact-alarm permission).
 */
object Reminders {
    const val CHANNEL = "class_reminders"
    const val ACTION_FIRE = "com.tertiaryinfotech.tertiarycourses.REMINDER"
    private const val STORE = "reminders"
    private const val MAX = 60

    /** [showRole]: the trainer view, where a reminder says whether the class is taught or attended. */
    fun build(classes: List<ClassItem>, showRole: Boolean = false, now: Long = System.currentTimeMillis()): List<Reminder> {
        val zone = try { ZoneId.of(Site.timeZone) } catch (e: Exception) { ZoneId.systemDefault() }
        fun millis(d: LocalDate, t: LocalTime) = d.atTime(t).atZone(zone).toInstant().toEpochMilli()
        val out = ArrayList<Reminder>()
        for (c in classes) {
            if (c.runId <= 0 || c.isPendingPayment || c.bucket == "past") continue
            val days = c.classDays.ifEmpty { listOf(c.startDate) }.mapNotNull { Fmt.date(it) }.distinct().sorted()
            val first = days.firstOrNull() ?: continue
            val verb = if (showRole) (if (c.isTeaching) "Teaching" else "Attending") else "Class"
            val start = c.startTime.split(":").mapNotNull { it.toIntOrNull() }
                .takeIf { it.size >= 2 }?.let { LocalTime.of(it[0].coerceIn(0, 23), it[1].coerceIn(0, 59)) }
            val time = Fmt.times(c.startTime, c.endTime)
            val place = when {
                c.mode == "Live Online" -> "Live online"
                else -> listOf(c.room, c.venue).filter { it.isNotEmpty() }.joinToString(", ")
            }
            val details = listOf(time, place).filter { it.isNotEmpty() }.joinToString(" · ")
            // "Class tomorrow: <title>" with the time and place below, the same wording as iOS.
            fun add(kind: String, day: LocalDate, at: Long, lead: String) {
                if (at <= now) return
                out += Reminder("${c.role}-${c.runId}-$kind-$day".hashCode(), at, "$verb $lead: ${c.title}", details.ifEmpty { Site.siteName })
            }
            add("3d", first, millis(first.minusDays(3), LocalTime.of(9, 0)), "in 3 days")
            days.forEach { d ->
                add("eve", d, millis(d.minusDays(1), LocalTime.of(18, 0)), "tomorrow")
                if (start != null && start.hour >= 1) add("day", d, millis(d, start.minusHours(1)), "in 1 hour")
                else add("day", d, millis(d, LocalTime.of(8, 0)), "today")
            }
        }
        return out.sortedBy { it.at }.take(MAX)
    }

    /** Replaces every scheduled reminder with [list] and remembers it for the next boot. */
    fun schedule(context: Context, list: List<Reminder>) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        val alarms = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        stored(prefs).forEach { alarms.cancel(pending(app, it)) }
        prefs.edit { putString("list", JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()) }
        arm(app, list)
    }

    /** After a reboot, an app update or a clock change: alarms are gone, so set them again. */
    fun rearm(context: Context) {
        val app = context.applicationContext
        arm(app, stored(app.getSharedPreferences(STORE, Context.MODE_PRIVATE)))
    }

    private fun arm(app: Context, list: List<Reminder>) {
        val alarms = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        list.filter { it.at > now }.forEach {
            try {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it.at, pending(app, it))
            } catch (e: Exception) {
                // Never let a scheduling failure break a refresh.
            }
        }
    }

    private fun stored(prefs: SharedPreferences): List<Reminder> = try {
        JSONArray(prefs.getString("list", "[]")).objects().map(Reminder::from)
    } catch (e: Exception) {
        emptyList()
    }

    private fun pending(app: Context, r: Reminder): PendingIntent {
        val intent = Intent(app, ReminderReceiver::class.java).setAction(ACTION_FIRE)
            .setData(Uri.parse("tclms://reminder/${r.id}"))
            .putExtra("id", r.id).putExtra("title", r.title).putExtra("text", r.text)
        return PendingIntent.getBroadcast(app, r.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun canNotify(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun openSystemSettings(context: Context) {
        val intent = Intent(SystemSettings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(SystemSettings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(intent) } catch (e: Exception) { }
    }

    fun show(context: Context, id: Int, title: String, text: String) {
        if (!Settings(context).remindersOn || !canNotify(context)) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Class reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Reminders before the classes you attend or teach."
            },
        )
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_class)
            .setColor(0xFF0B3D8E.toInt())
            .setContentTitle(title)
            .setContentText(text.lineSequence().firstOrNull().orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            manager.notify(id, n)
        } catch (e: SecurityException) {
            // Permission was revoked between the check and the post.
        }
    }
}

/** Fires a reminder; sets the alarms again after a reboot, an app update or a clock change. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Reminders.ACTION_FIRE -> Reminders.show(
                context, intent.getIntExtra("id", 0),
                intent.getStringExtra("title").orEmpty(), intent.getStringExtra("text").orEmpty(),
            )
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> Reminders.rearm(context)
        }
    }
}
