package com.tommasoberlose.anotherwidget.helpers

import android.content.Context
import android.text.format.DateUtils
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.utils.getCapWordString
import java.text.SimpleDateFormat
import java.util.*

object DateHelper {
    fun getDateText(context: Context, date: Calendar): String {
        return if (Preferences.dateFormat != "") {
            val text = try {
                SimpleDateFormat(Preferences.dateFormat, Locale.getDefault()).format(date.time)
            } catch (e: Exception) {
                getDefaultDateText(context, date)
            }
            when {
                Preferences.isDateUppercase -> text.toUpperCase(Locale.getDefault())
                Preferences.isDateCapitalize -> text.getCapWordString()
                else -> text
            }
        } else {
            val flags: Int =
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_ABBREV_MONTH
            val text = "%s, %s".format(
                SimpleDateFormat("EEEE", Locale.getDefault()).format(date.time),
                DateUtils.formatDateTime(context, date.timeInMillis, flags)
            )
            when {
                Preferences.isDateUppercase -> text.toUpperCase(Locale.getDefault())
                Preferences.isDateCapitalize -> text.getCapWordString()
                else -> text
            }
        }
    }

    fun getDefaultDateText(context: Context, date: Calendar): String {
        val flags: Int =
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_ABBREV_MONTH
        return "%s, %s".format(
            SimpleDateFormat("EEEE", Locale.getDefault()).format(date.time),
            DateUtils.formatDateTime(context, date.timeInMillis, flags)
        )
    }
}