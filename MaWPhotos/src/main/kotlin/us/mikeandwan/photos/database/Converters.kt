package us.mikeandwan.photos.database

import androidx.room.TypeConverter
import us.mikeandwan.photos.domain.models.MediaType
import java.util.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

class Converters {
    @TypeConverter
    fun toCalendar(l: Long?): Calendar? {
        val cal = Calendar.getInstance()

        cal.timeInMillis = l!!

        return cal
    }

    @TypeConverter
    fun fromCalendar(cal: Calendar?): Long? {
        return cal?.timeInMillis
    }

    @TypeConverter
    fun fromMediaType(value: MediaType): String {
        return value.name
    }

    @TypeConverter
    fun toMediaType(value: String): MediaType {
        return MediaType.valueOf(value)
    }

    @TypeConverter
    fun toInstant(l: Long?): Instant? {
        if (l == null) {
            return null
        }

        return Instant.fromEpochMilliseconds(l)
    }

    @TypeConverter
    fun fromInstant(i: Instant?): Long? {
        if (i == null) {
            return null
        }

        return i.toEpochMilliseconds()
    }

    @TypeConverter
    fun toUuid(v: String?): Uuid? {
        if (v == null) {
            return null
        }

        return Uuid.parse(v)
    }

    @TypeConverter
    fun fromUuid(v: Uuid?): String? {
        return v?.toHexDashString()
    }
}
