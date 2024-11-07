package com.tommasoberlose.anotherwidget.ui.activities

import android.Manifest
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.ActivityMainBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var manageFilesPermissionLauncher: ActivityResultLauncher<Intent>

    private lateinit var mediaImagePermissionLauncher: ActivityResultLauncher<String>

    private fun newAndroidWallpaperPermissionsGranted() = SDK_INT >= TIRAMISU && Environment.isExternalStorageManager() && ContextCompat.checkSelfPermission(applicationContext, READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED

    private var mAppWidgetId: Int = -1
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding
    private val mainNavController: NavController? by lazy {
        Navigation.findNavController(
            this,
            R.id.content_fragment
        )
    }
    private val settingsNavController: NavController? by lazy {
        Navigation.findNavController(
            this,
            R.id.settings_fragment
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.nav_default_enter_anim, R.anim.nav_default_exit_anim)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        binding = ActivityMainBinding.inflate(layoutInflater)

        manageFilesPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (newAndroidWallpaperPermissionsGranted())
                Preferences.showWallpaper = true
        }

        mediaImagePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (newAndroidWallpaperPermissionsGranted())
                Preferences.showWallpaper = true
        }

        controlExtras(intent)
        if (Preferences.showWallpaper) {
            requirePermission()
        }

        setContentView(binding.root)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent != null) {
            controlExtras(intent)
        }
    }

    private fun controlExtras(intent: Intent) {
        val extras = intent.extras
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )

            if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                binding.actionAddWidget.visibility = View.VISIBLE
                binding.actionAddWidget.setOnClickListener {
                    addNewWidget()
                }
            }
        }
    }

    private fun addNewWidget() {
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }

    private fun requirePermission() {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    // Wallpaper access requires permission to manage all files on Android 13+ devices when targeting SDK >= 33. Google does not intend to fix this.
                    // https://issuetracker.google.com/issues/237124750
                    if (SDK_INT >= TIRAMISU && !(Environment.isExternalStorageManager() || ContextCompat.checkSelfPermission(applicationContext, READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED)) {
                        MaterialAlertDialogBuilder(this@MainActivity, R.style.CustomAlertDialog)
                            .setTitle("Widget Preview Wallpaper Permissions")
                            .setMessage("On Android 13+ the in-app widget preview now requires access to manage all files and read all images in order to display your wallpaper.\n\nGranting these permissions is OPTIONAL and is only necessary if you wish to see your wallpaper in the widget preview.")
                            .setPositiveButton("Grant") { _, _ ->
                                if (!Environment.isExternalStorageManager()) manageFilesPermissionLauncher.launch(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = Uri.parse("package:" + applicationContext.packageName)
                                })
                                if (ContextCompat.checkSelfPermission(
                                        applicationContext,
                                        READ_MEDIA_IMAGES
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    mediaImagePermissionLauncher.launch(READ_MEDIA_IMAGES)
                                }
                            }
                            .setNegativeButton("Deny") { dialog, _ -> dialog.dismiss() }
                            .show()
                    }
                    report?.let {
                        Preferences.showWallpaper = report.areAllPermissionsGranted() || newAndroidWallpaperPermissionsGranted()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    // Remember to invoke this method when the custom rationale is closed
                    // or just by default if you don't want to use any custom rationale.
                    token?.cancelPermissionRequest()
                }
            })
            .check()
    }

    override fun onResume() {
        super.onResume()

        if (Preferences.showEvents && !checkGrantedPermission(Manifest.permission.READ_CALENDAR)) {
            Preferences.showEvents = false
        }
    }

    override fun onStart() {
        Preferences.preferences.registerOnSharedPreferenceChangeListener(this)
        super.onStart()

        val customBackPressedCallback = object : OnBackPressedCallback(mainNavController?.currentDestination?.id == R.id.appMainFragment) {
            override fun handleOnBackPressed() {
                if (settingsNavController?.navigateUp() == false) {
                    if (mAppWidgetId > 0) {
                        addNewWidget()
                    } else {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                } else viewModel.fragmentScrollY.value = 0
            }
        }

        onBackPressedDispatcher.addCallback(this, customBackPressedCallback)

        // Enable/disable custom back callback depending on the current fragment.
        mainNavController?.addOnDestinationChangedListener { _, destination, _ ->
            customBackPressedCallback.isEnabled = destination.id == R.id.appMainFragment
        }
    }

    override fun onStop() {
        super.onStop()
        Preferences.preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
        MainWidget.updateWidget(this)
    }
}
