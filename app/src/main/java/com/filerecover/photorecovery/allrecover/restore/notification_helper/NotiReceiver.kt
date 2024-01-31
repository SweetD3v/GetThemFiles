package com.filerecover.photorecovery.allrecover.restore.notification_helper

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.filerecover.photorecovery.allrecover.restore.R
import com.filerecover.photorecovery.allrecover.restore.ui.activities.MainActivity
import com.filerecover.photorecovery.allrecover.restore.utils.isOreoPlus
import com.filerecover.photorecovery.allrecover.restore.utils.isSPlus
import kotlinx.coroutines.coroutineScope
import java.util.Calendar

class NotiReceiver : BroadcastReceiver() {
    private var alarmManager: AlarmManager? = null
    private var notiChannel: NotificationChannel? = null
    private lateinit var notificationManager: NotificationManager
    var ctx: Context? = null

    override fun onReceive(context: Context, intent: Intent?) {
        this.ctx = context
        notificationManager =
            ctx?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel()

        showNotification(ctx)

        val nextTime = Calendar.getInstance().apply {
            this.add(Calendar.DAY_OF_WEEK, 1)
            this.set(Calendar.HOUR_OF_DAY, listOf(9, 18, 21).random())
            this.set(Calendar.MINUTE, 0)
            this.set(Calendar.SECOND, 0)
            this.set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        setupNotification(ctx, nextTime)
    }

    fun setupNotification(ctx: Context?, time: Long) {
        val pi = PendingIntent.getBroadcast(
            ctx,
            111,
            Intent(ctx, NotiReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = AlarmManager.AlarmClockInfo(
            time,
            pi
        )
        alarmManager = ctx?.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
        val thread = Thread {
            if (isSPlus()) {
                if (alarmManager?.canScheduleExactAlarms() == true)
                    alarmManager?.setAlarmClock(alarm, pi)
                else alarmManager?.set(AlarmManager.RTC_WAKEUP, time, pi)
            } else {
                alarmManager?.setAlarmClock(alarm, pi)
            }
        }
        thread.start()
    }

    private fun createChannel() {
        if (isOreoPlus()) {
            // Create the NotificationChannel.
            val name = "Photo Channel"
            val descriptionText = "Photo Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            notiChannel = NotificationChannel("photo_channel_id", name, importance)
            notiChannel!!.description = descriptionText
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            notificationManager.createNotificationChannel(notiChannel!!)
        }
    }

    private fun showNotification(ctx: Context?) {
        Log.e("TAG", "showNotification: ")
        val builder = if (isOreoPlus()) {
            Notification.Builder(ctx, "photo_channel_id")
        } else {
            Notification.Builder(ctx)
        }

        val pi = PendingIntent.getActivity(
            ctx, 222, Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        builder.setContentTitle("Some Message")
            .setContentIntent(pi)
            .setContentText("You've received new messages!")
            .setSmallIcon(R.drawable.splash_icon)
        notificationManager.notify(1, builder.build())
    }
}