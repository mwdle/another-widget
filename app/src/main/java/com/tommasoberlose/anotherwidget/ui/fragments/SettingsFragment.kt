package com.tommasoberlose.anotherwidget.ui.fragments

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tommasoberlose.anotherwidget.BuildConfig
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.databinding.FragmentAppSettingsBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.ActiveNotificationsHelper
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper
import com.tommasoberlose.anotherwidget.helpers.MediaPlayerHelper
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity.Companion.newAndroidWallpaperPermissionsGranted
import com.tommasoberlose.anotherwidget.ui.activities.settings.IntegrationsActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import com.tommasoberlose.anotherwidget.utils.openURI
import com.tommasoberlose.anotherwidget.utils.setOnSingleClickListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SettingsFragment : Fragment() {

    companion object {
        fun newInstance() = SettingsFragment()
    }

    private lateinit var wallpaperPermissionResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var viewModel: MainViewModel
    private lateinit var binding: FragmentAppSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        viewModel = ViewModelProvider(activity as MainActivity)[MainViewModel::class.java]
        binding = FragmentAppSettingsBinding.inflate(inflater)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        wallpaperPermissionResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (newAndroidWallpaperPermissionsGranted())
                Preferences.showWallpaper = true
        }

        subscribeUi(viewModel)

        binding.actionBack.setOnSingleClickListener {
            Navigation.findNavController(it).popBackStack()
        }

        binding.showWidgetPreviewToggle.setCheckedImmediatelyNoEvent(Preferences.showPreview)
        binding.showWallpaperToggle.setCheckedImmediatelyNoEvent(Preferences.showWallpaper)

        setupListener()

        binding.appVersion.text = getString(R.string.appVersionTextPlaceholder).format(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)

        binding.scrollView.viewTreeObserver.addOnScrollChangedListener {
            binding.toolbar.cardElevation = if (binding.scrollView.scrollY > 0) 32f else 0f
        }

        return binding.root
    }

    private fun subscribeUi(
        viewModel: MainViewModel,
    ) {
        viewModel.darkThemePreference.observe(viewLifecycleOwner) {
            AppCompatDelegate.setDefaultNightMode(it)
            maintainScrollPosition {
                binding.theme.text = when (it) {
                    AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.settings_subtitle_dark_theme_light)
                    AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.settings_subtitle_dark_theme_dark)
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY -> getString(R.string.settings_subtitle_dark_theme_by_battery_saver)
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> getString(R.string.settings_subtitle_dark_theme_follow_system)
                    else -> ""
                }
            }
        }

        viewModel.installedIntegrations.observe(viewLifecycleOwner) {
            binding.integrationsCountLabel.text =
                getString(R.string.label_count_installed_integrations).format(
                    it
                )
        }

        viewModel.showPreview.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.showWidgetPreviewLabel.text =
                    if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
            }
        }

        viewModel.showWallpaper.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.showWallpaperLabel.text =
                    if (it && (requireActivity().checkGrantedPermission(Manifest.permission.READ_EXTERNAL_STORAGE) || newAndroidWallpaperPermissionsGranted())) getString(
                        R.string.settings_visible
                    ) else getString(R.string.settings_not_visible)
            }
        }
    }

    private fun setupListener() {
        binding.actionShowWidgetPreview.setOnClickListener {
            binding.showWidgetPreviewToggle.isChecked = !binding.showWidgetPreviewToggle.isChecked
        }

        binding.showWidgetPreviewToggle.setOnCheckedChangeListener { _, isChecked ->
            Preferences.showPreview = isChecked
        }

        binding.actionShowWallpaper.setOnClickListener {
            binding.showWallpaperToggle.isChecked = !binding.showWallpaperToggle.isChecked
        }

        binding.showWallpaperToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requirePermission()
            } else {
                Preferences.showWallpaper = false
            }
        }

        binding.actionIntegrations.setOnClickListener {
            startActivity(Intent(requireContext(), IntegrationsActivity::class.java))
        }

        binding.actionChangeTheme.setOnClickListener {
            maintainScrollPosition {
                BottomSheetMenu<Int>(
                    requireContext(),
                    header = getString(R.string.settings_theme_title)
                )
                    .setSelectedValue(Preferences.darkThemePreference)
                    .addItem(
                        getString(R.string.settings_subtitle_dark_theme_light),
                        AppCompatDelegate.MODE_NIGHT_NO
                    )
                    .addItem(
                        getString(R.string.settings_subtitle_dark_theme_dark),
                        AppCompatDelegate.MODE_NIGHT_YES
                    )
                    .addItem(
                        getString(R.string.settings_subtitle_dark_theme_default),
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                    )
                    .addOnSelectItemListener { value ->
                        Preferences.darkThemePreference = value
                    }.show()
            }
        }

        binding.actionPrivacyPolicy.setOnClickListener {
            requireActivity().openURI("https://github.com/tommasoberlose/another-widget/blob/master/privacy-policy.md")
        }

        binding.actionRefreshWidget.setOnClickListener {
            binding.actionRefreshIcon
                .animate()
                .rotation((binding.actionRefreshIcon.rotation - binding.actionRefreshIcon.rotation % 360f) + 360f)
                .withEndAction {
                    try {
                        WeatherHelper.updateWeather(requireContext())
                        CalendarHelper.updateEventList(requireContext())
                        MediaPlayerHelper.updatePlayingMediaInfo(requireContext())
                        ActiveNotificationsHelper.clearLastNotification(requireContext())
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
                .start()
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

    override fun onResume() {
        super.onResume()
        binding.showWallpaperToggle.setCheckedNoEvent(
            Preferences.showWallpaper && (requireActivity().checkGrantedPermission(Manifest.permission.READ_EXTERNAL_STORAGE) || newAndroidWallpaperPermissionsGranted())
        )
    }

    private fun requirePermission() {
        Dexter.withContext(requireContext())
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    // Wallpaper access requires permission to manage all files on Android 13+ devices when targeting SDK >= 33. Google does not intend to fix this.
                    // https://issuetracker.google.com/issues/237124750
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Environment.isExternalStorageManager()) {
                        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
                            .setTitle("Widget Preview Wallpaper Permissions")
                            .setMessage("On Android 13+ the in-app widget preview now requires access to manage all files in order to display your wallpaper.\n\nGranting this permission is OPTIONAL and is only necessary if you wish to see your wallpaper in the widget preview.")
                            .setPositiveButton("Grant") { _, _ ->
                                wallpaperPermissionResultLauncher.launch(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = Uri.parse("package:" + requireContext().packageName)
                                })
                            }
                            .setNegativeButton("Deny") { dialog, _ -> dialog.dismiss() }
                            .show()
                    }
                    report?.let {
                        if (report.areAllPermissionsGranted() || newAndroidWallpaperPermissionsGranted()) {
                            Preferences.showWallpaper = true
                        } else {
                            binding.showWallpaperToggle.setCheckedNoEvent(false)
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?,
                ) {
                    // Remember to invoke this method when the custom rationale is closed
                    // or just by default if you don't want to use any custom rationale.
                    token?.continuePermissionRequest()
                }
            })
            .check()
    }
}
