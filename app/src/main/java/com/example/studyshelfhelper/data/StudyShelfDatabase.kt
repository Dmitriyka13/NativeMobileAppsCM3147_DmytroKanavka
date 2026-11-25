package com.example.studyshelfhelper.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.studyshelfhelper.model.BookEntity
import com.example.studyshelfhelper.model.ReadingStatus

//Type converters allowing Room SQLite to store and retrieve the ReadingStatus enum.

class Converters {
    @TypeConverter
    fun fromReadingStatus(status: ReadingStatus): String = status.name

    @TypeConverter
    fun toReadingStatus(value: String): ReadingStatus =
        runCatching { ReadingStatus.valueOf(value) }.getOrDefault(ReadingStatus.WANT_TO_READ)
}

//Main Room database definition for StudyShelfHelper.
//Provides offline storage for saved books and reading progress.

@Database(entities = [BookEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class StudyShelfDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var INSTANCE: StudyShelfDatabase? = null

        fun getDatabase(context: Context): StudyShelfDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyShelfDatabase::class.java,
                    "studyshelf_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}