package com.example.studyshelfhelper.model

import androidx.room.Entity
import androidx.room.PrimaryKey

//Enum representing the reading progress status of a book on the user's shelf.

enum class ReadingStatus(val label: String) {
    WANT_TO_READ("Want to Read"),
    READING("Reading"),
    COMPLETED("Completed")
}

// Room database entity representing a book stored locally for offline persistence.
//Maps directly to the 'books' SQLite table (COULD requirement #6).

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String,
    val publishYear: Int?,
    val coverUrl: String?,
    val totalPages: Int,
    val currentPage: Int = 0,
    val status: ReadingStatus = ReadingStatus.WANT_TO_READ,
    val lastSyncedTimestamp: Long = System.currentTimeMillis()
) {
    // Calculates reading progress from 1 to 100 in percents

    val progressPercentage: Int
        get() = if (totalPages > 0) {
            ((currentPage.toFloat() / totalPages.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
}