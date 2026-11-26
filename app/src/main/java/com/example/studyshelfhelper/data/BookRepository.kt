package com.example.studyshelfhelper.data

import com.example.studyshelfhelper.model.BookEntity
import kotlinx.coroutines.flow.Flow

//Single Source of Truth repository mediating between the local Room database and the remote OpenLibrary REST API
class BookRepository(
    private val bookDao: BookDao,
    private val apiService: OpenLibraryApiService
) {
    //Reactive stream of all books saved on the user's shelf
    val allSavedBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()

    //Reactive stream of a single book by its unique ID
    fun getBookById(bookId: String): Flow<BookEntity?> = bookDao.getBookById(bookId)

    //Fetches matching books from the OpenLibrary REST API and converts them to BookEntity objects
    suspend fun searchRemoteBooks(query: String): List<BookEntity> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return emptyList()
        val response = apiService.searchBooks(query = cleanQuery)
        return response.docs.map { it.toBookEntity() }
    }

    //Saves or replaces a book in the local Room database for offline access
    suspend fun saveBookToShelf(book: BookEntity) {
        bookDao.insertBook(book.copy(lastSyncedTimestamp = System.currentTimeMillis()))
    }

    //Updates reading progress or status of an existing book in Room
    suspend fun updateBook(book: BookEntity) {
        bookDao.updateBook(book)
    }

    //Removes a book from the local shelf
    suspend fun deleteBook(book: BookEntity) {
        bookDao.deleteBook(book)
    }

    //Synchronizes metadata (e.g., page counts or publish year) for saved books during background WorkManager execution
    suspend fun syncSavedBooksMetadata(): Int {
        val savedBooks = bookDao.getAllBooksSync()
        var updatedCount = 0
        for (book in savedBooks) {
            try {
                val results = apiService.searchBooks(query = book.title, limit = 1).docs
                val match = results.firstOrNull()
                if (match != null) {
                    val refreshedPages = match.medianPages ?: book.totalPages
                    val refreshedYear = match.firstPublishYear ?: book.publishYear
                    bookDao.updateBook(
                        book.copy(
                            totalPages = refreshedPages.coerceAtLeast(1),
                            publishYear = refreshedYear,
                            lastSyncedTimestamp = System.currentTimeMillis()
                        )
                    )
                    updatedCount++
                }
            } catch (_: Exception) {
                // Skip individual failures during background sync so other books still update
            }
        }
        return updatedCount
    }
}