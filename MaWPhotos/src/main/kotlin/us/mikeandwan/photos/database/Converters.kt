package us.mikeandwan.photos.database

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
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

    @TypeConverter
    fun toLocalDate(v: String?): LocalDate? {
        if (v == null) {
            return null
        }

        return LocalDate.parse(v, LocalDate.Formats.ISO)
    }

    @TypeConverter
    fun fromLocalDate(v: LocalDate?): String? {
        return v?.format(LocalDate.Formats.ISO)
    }
}
