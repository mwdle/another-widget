package com.tommasoberlose.anotherwidget.network

import android.content.Context
import com.chibatching.kotpref.Kotpref
import com.haroldadmin.cnradapter.NetworkResponse
import com.tommasoberlose.anotherwidget.network.repository.TimeZonesRepository

class TimeZonesApi(val context: Context) {
    suspend fun getTimeZone(lat: String, long: String): String? {
        Kotpref.init(context)
        val repository = TimeZonesRepository()
        var id: String? = null

        when (val response = repository.getTimeZone(lat, long)) {
            is NetworkResponse.Success -> {
                try {
                    id = response.body["timezoneId"] as String
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }

        return id
    }
}
