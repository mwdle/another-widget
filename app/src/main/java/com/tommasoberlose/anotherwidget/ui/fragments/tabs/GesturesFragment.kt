package com.tommasoberlose.anotherwidget.ui.fragments.tabs

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.chibatching.kotpref.bulk
import com.google.android.material.transition.MaterialSharedAxis
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.databinding.FragmentTabGesturesBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.global.RequestCode
import com.tommasoberlose.anotherwidget.helpers.IntentHelper
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.activities.tabs.ChooseApplicationActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GesturesFragment : Fragment() {

    companion object {
        fun newInstance() = GesturesFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var binding: FragmentTabGesturesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel = ViewModelProvider(activity as MainActivity)[MainViewModel::class.java]
        binding = FragmentTabGesturesBinding.inflate(inflater)

        subscribeUi(viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    @Deprecated("Deprecated")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.showMultipleEventsToggle.setCheckedImmediatelyNoEvent(Preferences.showNextEvent)
        setupListener()

        binding.scrollView.viewTreeObserver?.addOnScrollChangedListener {
            viewModel.fragmentScrollY.value = binding.scrollView.scrollY
        }
    }


    @SuppressLint("DefaultLocale")
    private fun subscribeUi(
        viewModel: MainViewModel
    ) {

        viewModel.showNextEvent.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.showMultipleEventsLabel.text =
                    if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
            }
        }

        viewModel.calendarAppName.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.calendarAppLabel.text = when {
                    it == IntentHelper.DO_NOTHING_OPTION -> getString(R.string.gestures_do_nothing)
                    it == IntentHelper.REFRESH_WIDGET_OPTION -> getString(R.string.gestures_refresh_widget)
                    it != IntentHelper.DEFAULT_OPTION -> it
                    else -> getString(R.string.default_calendar_app)
                }
            }
        }

        viewModel.openEventDetails.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.openEventDetailsLabel.text =
                    if (it) getString(R.string.default_event_app) else getString(R.string.default_calendar_app)
            }
        }

        viewModel.clockAppName.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.clockAppLabel.text = when {
                    it == IntentHelper.DO_NOTHING_OPTION -> getString(R.string.gestures_do_nothing)
                    it == IntentHelper.REFRESH_WIDGET_OPTION -> getString(R.string.gestures_refresh_widget)
                    it != IntentHelper.DEFAULT_OPTION -> it
                    else -> getString(R.string.default_clock_app)
                }
            }
        }

        viewModel.weatherAppName.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.weatherAppLabel.text = when {
                    it == IntentHelper.DO_NOTHING_OPTION -> getString(R.string.gestures_do_nothing)
                    it == IntentHelper.REFRESH_WIDGET_OPTION -> getString(R.string.gestures_refresh_widget)
                    it != IntentHelper.DEFAULT_OPTION -> it
                    else -> getString(R.string.default_weather_app)
                }
            }
        }
    }

    private fun setupListener() {

        binding.actionShowMultipleEvents.setOnClickListener {
            binding.showMultipleEventsToggle.isChecked = !binding.showMultipleEventsToggle.isChecked
        }

        binding.showMultipleEventsToggle.setOnCheckedChangeListener { _, isChecked ->
            Preferences.showNextEvent = isChecked
            if (!isChecked) {
                com.tommasoberlose.anotherwidget.db.EventRepository(requireContext()).run {
                    resetNextEventData()
                    close()
                }
            }
        }

        binding.actionOpenEventDetails.setOnClickListener {
            BottomSheetMenu<Boolean>(
                requireContext(),
                header = getString(R.string.settings_event_app_title)
            ).setSelectedValue(Preferences.openEventDetails)
                .addItem(getString(R.string.default_event_app), true)
                .addItem(getString(R.string.default_calendar_app), false)
                .addOnSelectItemListener { value ->
                    Preferences.openEventDetails = value
                }
                .show()
        }

        binding.actionCalendarApp.setOnClickListener {
            startActivityForResult(
                Intent(requireContext(), ChooseApplicationActivity::class.java).apply {
                    putExtra(Constants.RESULT_APP_PACKAGE, Preferences.calendarAppPackage)
                },
                RequestCode.CALENDAR_APP_REQUEST_CODE.code
            )
        }

        binding.actionClockApp.setOnClickListener {
            startActivityForResult(
                Intent(requireContext(), ChooseApplicationActivity::class.java).apply {
                    putExtra(Constants.RESULT_APP_PACKAGE, Preferences.clockAppPackage)
                },
                RequestCode.CLOCK_APP_REQUEST_CODE.code
            )
        }

        binding.actionWeatherApp.setOnClickListener {
            startActivityForResult(
                Intent(requireContext(), ChooseApplicationActivity::class.java).apply {
                    putExtra(Constants.RESULT_APP_PACKAGE, Preferences.weatherAppPackage)
                },
                RequestCode.WEATHER_APP_REQUEST_CODE.code
            )
        }
    }

    private fun maintainScrollPosition(callback: () -> Unit) {
        binding.scrollView.isScrollable = false
        callback.invoke()
        lifecycleScope.launch {
            delay(200)
            binding.scrollView.isScrollable = true
        }
    }

    @Deprecated("Deprecated")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra(Constants.RESULT_APP_NAME) && data.hasExtra(
                Constants.RESULT_APP_PACKAGE
            )
        ) {
            when (requestCode) {
                RequestCode.CALENDAR_APP_REQUEST_CODE.code -> {
                    Preferences.bulk {
                        calendarAppName = data.getStringExtra(Constants.RESULT_APP_NAME) ?: IntentHelper.DEFAULT_OPTION
                        calendarAppPackage =
                            data.getStringExtra(Constants.RESULT_APP_PACKAGE) ?: IntentHelper.DEFAULT_OPTION
                    }
                }

                RequestCode.EVENT_APP_REQUEST_CODE.code -> {
                    Preferences.bulk {
                        eventAppName = data.getStringExtra(Constants.RESULT_APP_NAME) ?: IntentHelper.DEFAULT_OPTION
                        eventAppPackage =
                            data.getStringExtra(Constants.RESULT_APP_PACKAGE) ?: IntentHelper.DEFAULT_OPTION
                    }
                }

                RequestCode.WEATHER_APP_REQUEST_CODE.code -> {
                    Preferences.bulk {
                        weatherAppName = data.getStringExtra(Constants.RESULT_APP_NAME) ?: IntentHelper.DEFAULT_OPTION
                        weatherAppPackage =
                            data.getStringExtra(Constants.RESULT_APP_PACKAGE) ?: IntentHelper.DEFAULT_OPTION
                    }
                }

                RequestCode.CLOCK_APP_REQUEST_CODE.code -> {
                    Preferences.bulk {
                        clockAppName = data.getStringExtra(Constants.RESULT_APP_NAME) ?: IntentHelper.DEFAULT_OPTION
                        clockAppPackage =
                            data.getStringExtra(Constants.RESULT_APP_PACKAGE) ?: IntentHelper.DEFAULT_OPTION
                    }
                }
            }
            MainWidget.updateWidget(requireContext())
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
