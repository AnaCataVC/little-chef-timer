package com.littlechef.timer

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri

object TimerAlarm {
    private const val ACTION_FIRE = "com.littlechef.timer.FIRE"
    const val ACTION_STOP = "com.littlechef.timer.STOP"
    private const val EXTRA_TITLE = "title"
    private const val NOTIFICATION_ID = 1

    // setAlarmClock fires exactly even in Doze and needs no SCHEDULE_EXACT_ALARM permission.
    fun schedule(context: Context, endAtMillis: Long, title: String) {
        alarmManager(context).setAlarmClock(
            AlarmManager.AlarmClockInfo(endAtMillis, openAppIntent(context)),
            fireIntent(context, title),
        )
    }

    fun cancel(context: Context) = alarmManager(context).cancel(fireIntent(context, ""))

    fun stopRinging(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        RecipeStore(context).saveTimer(null)
    }

    internal fun ring(baseContext: Context, title: String) {
        val context = baseContext.withAppLanguage()
        val stop = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, TimerAlarmReceiver::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, ensureChannel(context))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.notification_title, title))
            .setContentText(context.getString(R.string.notification_text))
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(openAppIntent(context))
            .setFullScreenIntent(openAppIntent(context), true)
            .setDeleteIntent(stop)
            .addAction(Notification.Action.Builder(null, context.getString(R.string.stop), stop).build())
            .setOngoing(true)
            .build()
        // FLAG_INSISTENT loops the channel sound until the notification is dismissed.
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    /**
     * A channel's sound can't be changed after creation, so each chosen sound gets its own
     * channel and the previous ones are deleted.
     */
    private fun ensureChannel(context: Context): String {
        val soundUri = RecipeStore(context).alarmSoundUri?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val channelId = "alarm_${soundUri.toString().hashCode()}"
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notificationChannels.filter { it.id != channelId }.forEach { manager.deleteNotificationChannel(it.id) }
        if (manager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(channelId, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(
                    soundUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 400, 800)
            }
            manager.createNotificationChannel(channel)
        }
        return channelId
    }

    private fun openAppIntent(context: Context) = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
    )

    private fun fireIntent(context: Context, title: String) = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, TimerAlarmReceiver::class.java).setAction(ACTION_FIRE).putExtra(EXTRA_TITLE, title),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun alarmManager(context: Context) = context.getSystemService(AlarmManager::class.java)

    internal fun titleOf(intent: Intent) = intent.getStringExtra(EXTRA_TITLE).orEmpty()
}

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TimerAlarm.ACTION_STOP) TimerAlarm.stopRinging(context)
        else TimerAlarm.ring(context, TimerAlarm.titleOf(intent))
    }
}
