package com.soundscheduler.app.utils

import com.soundscheduler.app.data.Routine
import org.json.JSONArray
import org.json.JSONObject

object RoutineBackupManager {
    fun exportRoutinesToJson(routines: List<Routine>): String {
        val jsonArray = JSONArray()
        for (r in routines) {
            val obj = JSONObject().apply {
                put("title", r.title)
                put("type", r.type)
                put("time", r.time ?: JSONObject.NULL)
                put("location", r.location ?: JSONObject.NULL)
                put("latitude", r.latitude ?: JSONObject.NULL)
                put("longitude", r.longitude ?: JSONObject.NULL)
                put("radiusMeters", r.radiusMeters ?: JSONObject.NULL)
                put("locationTransition", r.locationTransition ?: JSONObject.NULL)
                put("chargingTransition", r.chargingTransition ?: JSONObject.NULL)
                put("bluetoothDeviceAddress", r.bluetoothDeviceAddress ?: JSONObject.NULL)
                put("wifiSsid", r.wifiSsid ?: JSONObject.NULL)
                put("calendarEventId", r.calendarEventId ?: JSONObject.NULL)
                put("calendarKeyword", r.calendarKeyword ?: JSONObject.NULL)
                put("calendarBufferMinutes", r.calendarBufferMinutes)
                put("batteryThreshold", r.batteryThreshold ?: JSONObject.NULL)
                put("batteryTriggerDirection", r.batteryTriggerDirection ?: JSONObject.NULL)
                put("webhookUrl", r.webhookUrl ?: JSONObject.NULL)
                put("recurrence", r.recurrence ?: JSONObject.NULL)
                put("soundProfile", r.soundProfile)
                put("isEnabled", r.isEnabled)
                put("daysOfWeek", r.daysOfWeek ?: JSONObject.NULL)
            }
            jsonArray.put(obj)
        }
        return JSONObject().put("version", 2).put("routines", jsonArray).toString(2)
    }

    fun importRoutinesFromJson(jsonString: String): List<Routine> {
        val routines = mutableListOf<Routine>()
        val root = JSONObject(jsonString)
        val array = root.getJSONArray("routines")
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val routine = Routine(
                title = obj.getString("title"),
                type = obj.getString("type"),
                time = if (obj.has("time") && !obj.isNull("time")) obj.getLong("time") else null,
                location = if (obj.has("location") && !obj.isNull("location")) obj.getString("location") else null,
                latitude = if (obj.has("latitude") && !obj.isNull("latitude")) obj.getDouble("latitude") else null,
                longitude = if (obj.has("longitude") && !obj.isNull("longitude")) obj.getDouble("longitude") else null,
                radiusMeters = if (obj.has("radiusMeters") && !obj.isNull("radiusMeters")) obj.getInt("radiusMeters") else null,
                locationTransition = if (obj.has("locationTransition") && !obj.isNull("locationTransition")) obj.getString("locationTransition") else null,
                chargingTransition = if (obj.has("chargingTransition") && !obj.isNull("chargingTransition")) obj.getString("chargingTransition") else null,
                bluetoothDeviceAddress = if (obj.has("bluetoothDeviceAddress") && !obj.isNull("bluetoothDeviceAddress")) obj.getString("bluetoothDeviceAddress") else null,
                wifiSsid = if (obj.has("wifiSsid") && !obj.isNull("wifiSsid")) obj.getString("wifiSsid") else null,
                calendarEventId = if (obj.has("calendarEventId") && !obj.isNull("calendarEventId")) obj.getString("calendarEventId") else null,
                calendarKeyword = if (obj.has("calendarKeyword") && !obj.isNull("calendarKeyword")) obj.getString("calendarKeyword") else null,
                calendarBufferMinutes = if (obj.has("calendarBufferMinutes")) obj.getInt("calendarBufferMinutes") else 5,
                batteryThreshold = if (obj.has("batteryThreshold") && !obj.isNull("batteryThreshold")) obj.getInt("batteryThreshold") else null,
                batteryTriggerDirection = if (obj.has("batteryTriggerDirection") && !obj.isNull("batteryTriggerDirection")) obj.getString("batteryTriggerDirection") else null,
                webhookUrl = if (obj.has("webhookUrl") && !obj.isNull("webhookUrl")) obj.getString("webhookUrl") else null,
                recurrence = if (obj.has("recurrence") && !obj.isNull("recurrence")) obj.getString("recurrence") else null,
                soundProfile = obj.optString("soundProfile", Routine.PROFILE_RING),
                isEnabled = obj.optBoolean("isEnabled", true),
                daysOfWeek = if (obj.has("daysOfWeek") && !obj.isNull("daysOfWeek")) obj.getString("daysOfWeek") else null
            )
            routines.add(routine)
        }
        return routines
    }
}
