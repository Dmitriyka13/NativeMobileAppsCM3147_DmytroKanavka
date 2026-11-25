package com.example.studyshelfhelper.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.studyshelfhelper.model.BookEntity
import kotlinx.coroutines.flow.Flow

//Data Access Object (DAO) for performing CRUD operations on the local Room database.
//Uses Flow to emit reactive updates to the UI whenever the database changes.

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY lastSyncedTimestamp DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books")
    suspend fun getAllBooksSync(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    fun getBookById(bookId: String): Flow<BookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)
}