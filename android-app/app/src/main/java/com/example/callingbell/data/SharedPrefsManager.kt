package com.example.callingbell.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class Member(val id: String, val name: String)
data class Room(val id: String, val name: String, val code: String, val members: List<Member>)
data class AppSettings(val volume: Float = 1.0f, val isVibrationEnabled: Boolean = true, val hapticsIntensity: Float = 1.0f, val defaultChimeId: String = "classic", val themeId: String = "DARK")

class SharedPrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("CallingBellPrefs", Context.MODE_PRIVATE)

    var userName: String
        get() = prefs.getString("USER_NAME", "") ?: ""
        set(value) = prefs.edit().putString("USER_NAME", value).apply()

    var isPro: Boolean
        get() = prefs.getBoolean("IS_PRO", false)
        set(value) = prefs.edit().putBoolean("IS_PRO", value).apply()

    fun getSettings(): AppSettings {
        return AppSettings(
            volume = prefs.getFloat("SETTING_VOLUME", 1.0f),
            isVibrationEnabled = prefs.getBoolean("SETTING_VIBRATION", true),
            hapticsIntensity = prefs.getFloat("SETTING_HAPTICS", 1.0f),
            defaultChimeId = prefs.getString("SETTING_CHIME", "classic") ?: "classic",
            themeId = prefs.getString("SETTING_THEME", "DARK") ?: "DARK"
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putFloat("SETTING_VOLUME", settings.volume)
            .putBoolean("SETTING_VIBRATION", settings.isVibrationEnabled)
            .putFloat("SETTING_HAPTICS", settings.hapticsIntensity)
            .putString("SETTING_CHIME", settings.defaultChimeId)
            .putString("SETTING_THEME", settings.themeId)
            .apply()
    }

    fun getRooms(): List<Room> {
        val roomsStr = prefs.getString("ROOMS_LIST", "[]") ?: "[]"
        val list = mutableListOf<Room>()
        try {
            val jsonArray = JSONArray(roomsStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val membersArray = obj.optJSONArray("members") ?: JSONArray()
                val membersList = mutableListOf<Member>()
                for (j in 0 until membersArray.length()) {
                    val mObj = membersArray.getJSONObject(j)
                    membersList.add(Member(mObj.getString("id"), mObj.getString("name")))
                }
                list.add(Room(obj.getString("id"), obj.getString("name"), obj.getString("code"), membersList))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveRooms(rooms: List<Room>) {
        val jsonArray = JSONArray()
        for (room in rooms) {
            val rObj = JSONObject()
            rObj.put("id", room.id)
            rObj.put("name", room.name)
            rObj.put("code", room.code)
            
            val mArray = JSONArray()
            for (member in room.members) {
                val mObj = JSONObject()
                mObj.put("id", member.id)
                mObj.put("name", member.name)
                mArray.put(mObj)
            }
            rObj.put("members", mArray)
            jsonArray.put(rObj)
        }
        prefs.edit().putString("ROOMS_LIST", jsonArray.toString()).apply()
    }

    fun addRoom(room: Room) {
        val rooms = getRooms().toMutableList()
        rooms.add(room)
        saveRooms(rooms)
    }

    fun deleteRoom(roomId: String) {
        val rooms = getRooms().filter { it.id != roomId }
        saveRooms(rooms)
    }
    
    fun getCustomIntentions(): List<Pair<String, String>> {
        val str = prefs.getString("PRO_CUSTOM_INTENTIONS", "[]") ?: "[]"
        val list = mutableListOf<Pair<String, String>>()
        try {
            val jsonArray = JSONArray(str)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(Pair(obj.getString("label"), obj.getString("emoji")))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun addCustomIntention(label: String, emoji: String) {
        val current = getCustomIntentions().toMutableList()
        current.add(Pair(label, emoji))
        val jsonArray = JSONArray()
        for (item in current) {
            val obj = JSONObject()
            obj.put("label", item.first)
            obj.put("emoji", item.second)
            jsonArray.put(obj)
        }
        prefs.edit().putString("PRO_CUSTOM_INTENTIONS", jsonArray.toString()).apply()
    }

    fun deleteCustomIntention(label: String) {
        val current = getCustomIntentions().filter { it.first != label }
        val jsonArray = JSONArray()
        for (item in current) {
            val obj = JSONObject()
            obj.put("label", item.first)
            obj.put("emoji", item.second)
            jsonArray.put(obj)
        }
        prefs.edit().putString("PRO_CUSTOM_INTENTIONS", jsonArray.toString()).apply()
    }

    fun updateRoom(room: Room) {
        val rooms = getRooms().toMutableList()
        val index = rooms.indexOfFirst { it.id == room.id }
        if (index != -1) {
            rooms[index] = room
            saveRooms(rooms)
        }
    }
}
