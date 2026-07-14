package com.smartnexus.nexusflow.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromIntList(list: List<Int>?): String? {
        if (list == null) return null
        return list.joinToString(separator = ",")
    }

    @TypeConverter
    fun toIntList(data: String?): List<Int>? {
        if (data.isNullOrEmpty()) return emptyList()
        return data.split(",").mapNotNull { it.toIntOrNull() }
    }
}
