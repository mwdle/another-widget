package com.tommasoberlose.anotherwidget.helpers

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Typeface
import android.os.Handler
import android.os.HandlerThread
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Preferences

object WidgetHelper {
    class WidgetSizeProvider(
        private val context: Context,
        private val appWidgetManager: AppWidgetManager
    ) {

        fun getWidgetsSize(widgetId: Int): Pair<Int, Int> {
            val portrait = context.resources.configuration.orientation == ORIENTATION_PORTRAIT
            val width = getWidgetSizeInDp(
                widgetId,
                if (portrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
            )
            val height = getWidgetSizeInDp(
                widgetId,
                if (portrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
            )
            val widthInPx = context.dip(width)
            val heightInPx = context.dip(height)
            return widthInPx to heightInPx
        }

        private fun getWidgetSizeInDp(widgetId: Int, key: String): Int =
            appWidgetManager.getAppWidgetOptions(widgetId).getInt(key, 0)

        private fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    }

    fun runWithCustomTypeface(context: Context, function: (typeface: Typeface?) -> Unit) {
        if (Preferences.customFontFile != "") {
            val request = FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                Preferences.customFontFile,
                R.array.com_google_android_gms_fonts_certs
            )

            val handlerThread = HandlerThread("generateView")
            val callback = object : FontsContractCompat.FontRequestCallback() {
                override fun onTypefaceRetrieved(typeface: Typeface) {
                    handlerThread.quit()
                    function.invoke(typeface)
                }

                override fun onTypefaceRequestFailed(reason: Int) {
                    handlerThread.quit()
                    function.invoke(null)
                }
            }

            handlerThread.start()

            Handler(handlerThread.looper).run {
                FontsContractCompat.requestFont(context, request, callback, this)
            }
        } else {
            function.invoke(null)
        }
    }
}