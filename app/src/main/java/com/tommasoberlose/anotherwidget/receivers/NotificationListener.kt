package com.tommasoberlose.anotherwidget.receivers

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.ActiveNotificationsHelper
import com.tommasoberlose.anotherwidget.helpers.MediaPlayerHelper
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.setExactIfCanSchedule
import java.util.*


class NotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        MediaPlayerHelper.updatePlayingMediaInfo(this)
        ActiveNotificationsHelper.clearLastNotification(this)
        super.onListenerConnected()
    }

    override fun onListenerDisconnected() {
        MediaPlayerHelper.updatePlayingMediaInfo(this)
        ActiveNotificationsHelper.clearLastNotification(this)
        super.onListenerDisconnected()
    }

    // https://stackoverflow.com/questions/73019160/the-getparcelableextra-method-is-deprecated
    private inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
        SDK_INT > TIRAMISU -> getParcelable(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelable(key) as? T
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.notification?.extras?.let { bundle ->
            bundle.parcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)?.let {
                if (Preferences.showMusic)
                    MediaPlayerHelper.updatePlayingMediaInfo(this)
            } ?: run {
                val isGroupHeader = sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
                val isOngoing = sbn.notification.flags and Notification.FLAG_ONGOING_EVENT != 0

                if (Preferences.showNotifications && bundle.containsKey(Notification.EXTRA_TITLE) && !isGroupHeader && !isOngoing && ActiveNotificationsHelper.isAppAccepted(
                        sbn.packageName
                    ) && !sbn.packageName.contains("com.android.systemui")
                ) {
                    Preferences.lastNotificationId = sbn.id
                    Preferences.lastNotificationTitle = bundle.getString(Notification.EXTRA_TITLE) ?: ""
                    try {
                        Preferences.lastNotificationIcon = sbn.notification.smallIcon.resId
                    } catch (ex: Exception) {
                        Preferences.lastNotificationIcon = 0
                    }
                    Preferences.lastNotificationPackage = sbn.packageName
                    MainWidget.updateWidget(this)
                    setTimeout(this)
                }
            }
        }

        super.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (Preferences.showMusic)
            MediaPlayerHelper.updatePlayingMediaInfo(this)
        sbn?.let {
            if (Preferences.showNotifications && sbn.id == Preferences.lastNotificationId && sbn.packageName == Preferences.lastNotificationPackage) {
                ActiveNotificationsHelper.clearLastNotification(this)
            }
        }
        super.onNotificationRemoved(sbn)
    }

    private fun setTimeout(context: Context) {
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val intent = Intent(context, UpdatesReceiver::class.java).apply {
                action = Actions.ACTION_CLEAR_NOTIFICATION
            }
            val timeoutPref = Constants.GlanceNotificationTimer.fromInt(Preferences.hideNotificationAfter)
            if (timeoutPref != Constants.GlanceNotificationTimer.WHEN_DISMISSED) {
                setExactIfCanSchedule(
                    AlarmManager.RTC,
                    Calendar.getInstance().timeInMillis + when (timeoutPref) {
                        Constants.GlanceNotificationTimer.HALF_MINUTE -> 30 * 1000
                        Constants.GlanceNotificationTimer.ONE_MINUTE -> 60 * 1000
                        Constants.GlanceNotificationTimer.FIVE_MINUTES -> 5 * 60 * 1000
                        Constants.GlanceNotificationTimer.TEN_MINUTES -> 10 * 60 * 1000
                        Constants.GlanceNotificationTimer.FIFTEEN_MINUTES -> 15 * 60 * 1000
                        else -> 60 * 1000
                    },
                    PendingIntent.getBroadcast(
                        context,
                        5,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
        }
    }

    companion object {
        fun clearTimeout(context: Context) {
            with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
                val intent = Intent(context, UpdatesReceiver::class.java).apply {
                    action = Actions.ACTION_CLEAR_NOTIFICATION
                }
                cancel(PendingIntent.getBroadcast(context, 5, intent, PendingIntent.FLAG_IMMUTABLE))
            }
        }
    }
}
