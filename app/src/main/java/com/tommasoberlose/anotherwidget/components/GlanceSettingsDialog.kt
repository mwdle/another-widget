package com.tommasoberlose.anotherwidget.components

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.GlanceProviderSettingsLayoutBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.ActiveNotificationsHelper
import com.tommasoberlose.anotherwidget.helpers.AlarmHelper
import com.tommasoberlose.anotherwidget.helpers.GreetingsHelper
import com.tommasoberlose.anotherwidget.helpers.MediaPlayerHelper
import com.tommasoberlose.anotherwidget.ui.activities.tabs.AppNotificationsFilterActivity
import com.tommasoberlose.anotherwidget.ui.activities.tabs.MediaInfoFormatActivity
import com.tommasoberlose.anotherwidget.ui.activities.tabs.MusicPlayersFilterActivity
import com.tommasoberlose.anotherwidget.ui.fragments.MainFragment
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus


class GlanceSettingsDialog(val context: Activity, val provider: Constants.GlanceProviderId, private val statusCallback: (() -> Unit)?) :
    BottomSheetDialog(context, R.style.BottomSheetDialogTheme) {

    private var binding: GlanceProviderSettingsLayoutBinding = GlanceProviderSettingsLayoutBinding.inflate(LayoutInflater.from(context))

    @OptIn(DelicateCoroutinesApi::class)
    override fun show() {

        /* TITLE */
        binding.title.text = when (provider) {
            Constants.GlanceProviderId.PLAYING_SONG -> context.getString(R.string.settings_show_music_title)
            Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> context.getString(R.string.settings_show_next_alarm_title)
            Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> context.getString(R.string.settings_low_battery_level_title)
            Constants.GlanceProviderId.CUSTOM_INFO -> context.getString(R.string.settings_custom_notes_title)
            Constants.GlanceProviderId.NOTIFICATIONS -> context.getString(R.string.settings_show_notifications_title)
            Constants.GlanceProviderId.GREETINGS -> context.getString(R.string.settings_show_greetings_title)
            Constants.GlanceProviderId.EVENTS -> context.getString(R.string.settings_show_events_as_glance_provider_title)
            Constants.GlanceProviderId.WEATHER -> context.getString(R.string.settings_show_weather_as_glance_provider_title)
        }

        /* SUBTITLE*/
        binding.subtitle.text = when (provider) {
            Constants.GlanceProviderId.PLAYING_SONG -> context.getString(R.string.settings_show_music_subtitle)
            Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> context.getString(R.string.settings_show_next_alarm_subtitle)
            Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> context.getString(R.string.settings_low_battery_level_subtitle)
            Constants.GlanceProviderId.CUSTOM_INFO -> ""
            Constants.GlanceProviderId.NOTIFICATIONS -> context.getString(R.string.settings_show_notifications_subtitle)
            Constants.GlanceProviderId.GREETINGS -> context.getString(R.string.settings_show_greetings_subtitle)
            Constants.GlanceProviderId.EVENTS -> context.getString(R.string.settings_show_events_as_glance_provider_subtitle)
            Constants.GlanceProviderId.WEATHER -> context.getString(R.string.settings_show_weather_as_glance_provider_subtitle)
        }

        /* SONG */
        binding.actionFilterMusicPlayers.isVisible = provider == Constants.GlanceProviderId.PLAYING_SONG
        binding.actionChangeMediaInfoFormat.isVisible = provider == Constants.GlanceProviderId.PLAYING_SONG
        if (provider == Constants.GlanceProviderId.PLAYING_SONG) {
            binding.actionFilterMusicPlayers.setOnClickListener {
                dismiss()
                context.startActivityForResult(Intent(context, MusicPlayersFilterActivity::class.java), 0)
            }
            binding.actionChangeMediaInfoFormat.setOnClickListener {
                dismiss()
                context.startActivityForResult(Intent(context, MediaInfoFormatActivity::class.java), 0)
            }
            checkNotificationPermission()
        }

        /* ALARM */
        binding.alarmSetByContainer.isVisible = provider == Constants.GlanceProviderId.NEXT_CLOCK_ALARM
        if (provider == Constants.GlanceProviderId.NEXT_CLOCK_ALARM) {
            binding.header.text = context.getString(R.string.information_header)
            binding.warningContainer.isVisible = false
            checkNextAlarm()
        }

        /* BATTERY INFO */
        if (provider == Constants.GlanceProviderId.BATTERY_LEVEL_LOW) {
            binding.warningContainer.isVisible = false
            binding.header.isVisible = false
            binding.divider.isVisible = false
        }

        /* NOTIFICATIONS */
        binding.actionFilterNotificationsApp.isVisible = provider == Constants.GlanceProviderId.NOTIFICATIONS
        binding.actionChangeNotificationTimer.isVisible = provider == Constants.GlanceProviderId.NOTIFICATIONS
        if (provider == Constants.GlanceProviderId.NOTIFICATIONS) {
            checkLastNotificationsPermission()
            val stringArray = context.resources.getStringArray(R.array.glance_notifications_timeout)
            binding.actionFilterNotificationsApp.setOnClickListener {
                dismiss()
                context.startActivityForResult(Intent(context, AppNotificationsFilterActivity::class.java), 0)
            }
            binding.notificationTimerLabel.text = stringArray[Preferences.hideNotificationAfter]
            binding.actionChangeNotificationTimer.setOnClickListener {
                val dialog = BottomSheetMenu<Int>(
                    context,
                    header = context.getString(R.string.glance_notification_hide_timeout_title)
                ).setSelectedValue(Preferences.hideNotificationAfter)
                Constants.GlanceNotificationTimer.values().forEachIndexed { index, timeout ->
                    dialog.addItem(stringArray[index], timeout.rawValue)
                }
                dialog.addOnSelectItemListener { value ->
                    Preferences.hideNotificationAfter = value
                    this.show()
                }.show()
            }
        }

        /* GREETINGS */
        if (provider == Constants.GlanceProviderId.GREETINGS) {
            binding.warningContainer.isVisible = false
            binding.header.isVisible = false
            binding.divider.isVisible = false
        }

        /* EVENTS */
        if (provider == Constants.GlanceProviderId.EVENTS) {
            binding.header.isVisible = false
            binding.divider.isVisible = false
            checkCalendarConfig()
        }

        /* WEATHER */
        if (provider == Constants.GlanceProviderId.WEATHER) {
            binding.header.isVisible = false
            binding.divider.isVisible = false
            checkWeatherConfig()
        }

        /* TOGGLE */
        binding.providerSwitch.setCheckedImmediatelyNoEvent(
            when (provider) {
                Constants.GlanceProviderId.PLAYING_SONG -> Preferences.showMusic
                Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> Preferences.showNextAlarm
                Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> Preferences.showBatteryCharging
                Constants.GlanceProviderId.CUSTOM_INFO -> true
                Constants.GlanceProviderId.NOTIFICATIONS -> Preferences.showNotifications
                Constants.GlanceProviderId.GREETINGS -> Preferences.showGreetings
                Constants.GlanceProviderId.EVENTS -> Preferences.showEventsAsGlanceProvider
                Constants.GlanceProviderId.WEATHER -> Preferences.showWeatherAsGlanceProvider
            }
        )

        var job: Job? = null

        binding.providerSwitch.setOnCheckedChangeListener { _, isChecked ->
            job?.cancel()
            job = GlobalScope.launch(Dispatchers.IO) {
                delay(300)
                withContext(Dispatchers.Main) {
                    when (provider) {
                        Constants.GlanceProviderId.PLAYING_SONG -> {
                            Preferences.showMusic = isChecked
                            checkNotificationPermission()
                        }

                        Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> {
                            Preferences.showNextAlarm = isChecked
                            checkNextAlarm()
                            if (!isChecked)
                                AlarmHelper.clearTimeout(context)
                        }

                        Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> {
                            Preferences.showBatteryCharging = isChecked
                        }

                        Constants.GlanceProviderId.NOTIFICATIONS -> {
                            Preferences.showNotifications = isChecked
                            checkLastNotificationsPermission()
                            if (!isChecked)
                                ActiveNotificationsHelper.clearLastNotification(context)
                        }

                        Constants.GlanceProviderId.GREETINGS -> {
                            Preferences.showGreetings = isChecked
                            GreetingsHelper.toggleGreetings(context)
                        }

                        Constants.GlanceProviderId.EVENTS -> {
                            Preferences.showEventsAsGlanceProvider = isChecked
                            if (isChecked) {
                                com.tommasoberlose.anotherwidget.db.EventRepository(context).run {
                                    resetNextEventData()
                                    close()
                                }
                            }
                        }

                        Constants.GlanceProviderId.WEATHER -> {
                            Preferences.showWeatherAsGlanceProvider = isChecked
                        }

                        else -> {}
                    }
                    statusCallback?.invoke()
                }
            }
        }

        setContentView(binding.root)
        behavior.run {
            skipCollapsed = true
            state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
        super.show()
    }

    private fun checkNextAlarm() {
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val alarm = nextAlarmClock
            if (alarm != null && alarm.showIntent != null) {
                val pm = context.packageManager as PackageManager
                val appNameOrPackage = try {
                    if (SDK_INT > TIRAMISU)
                        pm.getApplicationLabel(pm.getApplicationInfo(alarm.showIntent?.creatorPackage ?: "", PackageManager.ApplicationInfoFlags.of(0)))
                    else
                        @Suppress("DEPRECATION") pm.getApplicationLabel(pm.getApplicationInfo(alarm.showIntent?.creatorPackage ?: "", 0)) // Deprecated in API 33
                } catch (e: Exception) {
                    alarm.showIntent?.creatorPackage ?: ""
                }
                binding.alarmSetByTitle.text = context.getString(R.string.settings_show_next_alarm_app_title).format(appNameOrPackage)
                binding.alarmSetBySubtitle.text =
                    if (AlarmHelper.isAlarmProbablyWrong(context)) context.getString(R.string.settings_show_next_alarm_app_subtitle_wrong) else context.getString(
                        R.string.settings_show_next_alarm_app_subtitle_correct
                    )
                binding.alarmSetByContainer.isVisible = true
            } else {
                binding.alarmSetByContainer.isVisible = false
                binding.header.isVisible = false
                binding.divider.isVisible = false
            }
        }
        statusCallback?.invoke()
    }

    private fun checkCalendarConfig() {
        if (!Preferences.showEvents || !context.checkGrantedPermission(Manifest.permission.READ_CALENDAR)) {
            binding.warningContainer.isVisible = true
            binding.warningTitle.text = context.getString(R.string.settings_show_events_as_glance_provider_error)
            binding.warningContainer.setOnClickListener {
                dismiss()
                EventBus.getDefault().post(MainFragment.ChangeTabEvent())
            }
        } else {
            binding.warningContainer.isVisible = false
        }
    }

    private fun checkWeatherConfig() {
        if (!Preferences.showWeather || (Preferences.weatherProviderError != "" && Preferences.weatherProviderError != "-") || Preferences.weatherProviderLocationError != "") {
            binding.warningContainer.isVisible = true
            binding.warningTitle.text = context.getString(R.string.settings_show_weather_as_glance_provider_error)
            binding.warningContainer.setOnClickListener {
                dismiss()
                EventBus.getDefault().post(MainFragment.ChangeTabEvent())
            }
        } else {
            binding.warningContainer.isVisible = false
        }
    }

    private fun checkNotificationPermission() {
        when {
            ActiveNotificationsHelper.checkNotificationAccess(context) -> {
                binding.warningContainer.isVisible = false
                MediaPlayerHelper.updatePlayingMediaInfo(context)
            }

            Preferences.showMusic -> {
                binding.warningContainer.isVisible = true
                binding.warningTitle.text = context.getString(R.string.settings_request_notification_access)
                binding.warningContainer.setOnClickListener {
                    context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                }
            }

            else -> {
                binding.warningContainer.isVisible = false
            }
        }
        statusCallback?.invoke()
    }

    private fun checkLastNotificationsPermission() {
        when {
            ActiveNotificationsHelper.checkNotificationAccess(context) -> {
                binding.warningContainer.isVisible = false
            }

            Preferences.showNotifications -> {
                binding.warningContainer.isVisible = true
                binding.warningTitle.text = context.getString(R.string.settings_request_last_notification_access)
                binding.warningContainer.setOnClickListener {
                    context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                }
            }

            else -> {
                binding.warningContainer.isVisible = false
            }
        }
        statusCallback?.invoke()
    }
}