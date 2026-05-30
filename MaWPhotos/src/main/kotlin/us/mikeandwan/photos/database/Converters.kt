package us.mikeandwan.photos.database

import androidx.room.TypeConverter
import java.util.Calendar
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format

class Converters {
    @TypeConverter
    fun toCalendar(value: Long?): Calendar? =
        value?.let {
            Calendar.getInstance().apply { timeInMillis = it }
        }

    @TypeConverter
    fun fromCalendar(calendar: Calendar?): Long? = calendar?.timeInMillis

    @TypeConverter
    fun toInstant(value: Long?): Instant? =
        value?.let {
            Instant.fromEpochMilliseconds(it)
        }

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilliseconds()

    @TypeConverter
    fun toUuid(value: String?): Uuid? =
        value?.let {
            Uuid.parse(it)
        }

    @TypeConverter
    fun fromUuid(uuid: Uuid?): String? = uuid?.toHexDashString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? =
        value?.let {
            LocalDate.parse(it, LocalDate.Formats.ISO)
        }

    @TypeConverter
    fun fromLocalDate(localDate: LocalDate?): String? = localDate?.format(LocalDate.Formats.ISO)

    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.joinToString(",")

    @TypeConverter
    fun toStringList(value: String?): List<String>? = value?.split(",")?.filter { it.isNotBlank() }
}
