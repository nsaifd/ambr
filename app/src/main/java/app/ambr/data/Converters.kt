package app.ambr.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter fun fromDate(value: LocalDate?): Long? = value?.toEpochDay()
    @TypeConverter fun toDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)
}
