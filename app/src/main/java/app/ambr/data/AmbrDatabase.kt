package app.ambr.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room as RoomBuilder
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ItemEntity::class, ProofEntity::class, CoverageEntity::class, RepairEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AmbrDatabase : RoomDatabase() {
    abstract fun items(): ItemDao

    companion object {
        fun create(context: Context): AmbrDatabase =
            RoomBuilder.databaseBuilder(context, AmbrDatabase::class.java, "ambr.db").build()
    }
}
