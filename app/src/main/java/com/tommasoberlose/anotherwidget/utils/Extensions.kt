package com.tommasoberlose.anotherwidget.utils

import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.OnSingleClickListener
import com.tommasoberlose.anotherwidget.global.Preferences
import java.util.*

fun Context.toast(message: String, long: Boolean = false) {
    val toast = Toast.makeText(applicationContext, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
    toast.show()
}

fun Int.toPixel(context: Context): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics).toInt()
fun Float.toPixel(context: Context): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics)

fun Context.openURI(url: String) {
    try {
        val builder: CustomTabsIntent.Builder = CustomTabsIntent.Builder()
        builder.setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary)).build())
        val customTabsIntent: CustomTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(url))
    } catch (e: Exception) {
        try {
            val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(openIntent)
        } catch (ignored: Exception) {
            val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(getString(R.string.app_name), url)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, R.string.error_opening_uri, Toast.LENGTH_LONG).show()
        }
    }
}

fun Context.isDarkTheme(): Boolean {
    return Preferences.darkThemePreference == AppCompatDelegate.MODE_NIGHT_YES || Preferences.darkThemePreference == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM && resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

fun Float.convertDpToPixel(context: Context): Float {
    val resources: Resources = context.resources
    val metrics: DisplayMetrics = resources.displayMetrics
    val px: Float = this * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
    return px
}

fun Context.checkGrantedPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

fun android.app.AlarmManager.setExactIfCanSchedule(type: Int, triggerAtMillis: Long, operation: android.app.PendingIntent) {
    // uncomment the following check after bumping compileSdkVersion/targetSdkVersion to 31
    //if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S || canScheduleExactAlarms())
        setExact(type, triggerAtMillis, operation)
    //else
    //    set(type, triggerAtMillis, operation)
}

fun Context.getCurrentWallpaper(): Drawable? = try {
    WallpaperManager.getInstance(this).drawable
} catch (e: Exception) {
    null
}

fun String.getCapWordString(): String {
    return try {
        val ar = this.split(" ")
        var newText = ""
        for (t: String in ar) {
            newText += " "
            newText += t.substring(0, 1).toUpperCase(Locale.getDefault())
            newText += t.substring(1)
        }
        newText.substring(1)
    } catch (e: Exception) {
        this
    }
}

fun Locale.isMetric(): Boolean {
    return when (country.toUpperCase(this)) {
        "US", "LR", "MM", "GB" -> false
        else -> true
    }
}

fun View.setOnSingleClickListener(l: (View) -> Unit) {
    setOnClickListener(OnSingleClickListener(l))
}

fun ignoreExceptions(function: () -> Unit) = run {
    try {
        function.invoke()
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}
