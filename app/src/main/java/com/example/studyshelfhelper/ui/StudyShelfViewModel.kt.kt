package com.example.studyshelfhelper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studyshelfhelper.data.BookRepository
import com.example.studyshelfhelper.model.BookEntity
import com.example.studyshelfhelper.model.ReadingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Represents the network state of the OpenLibrary book search screen
sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val books: List<BookEntity>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

// Holds all UI state for the user's saved shelf screen
data class ShelfUiState(
    val books: List<BookEntity> = emptyList(),
    val selectedFilter: ReadingStatus? = null,
    val totalBooksCount: Int = 0,
    val completedBooksCount: Int = 0,
    val lastSyncLabel: String = "Never synced"
)

class StudyShelfViewModel(
    private val repository: BookRepository
) : ViewModel() {

    // Filter state for the shelf (null means show all books)
    private val _selectedFilter = MutableStateFlow<ReadingStatus?>(null)

    // Search query and network state for the Discover screen
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    // Combines database stream with the active filter so the UI updates automatically
    val shelfUiState: StateFlow<ShelfUiState> = combine(
        repository.allSavedBooks,
        _selectedFilter
    ) { allBooks, filter ->
        val filteredBooks = if (filter == null) {
            allBooks
        } else {
            allBooks.filter { it.status == filter }
        }
        val completedCount = allBooks.count { it.status == ReadingStatus.COMPLETED }
        val latestTimestamp = allBooks.maxOfOrNull { it.lastSyncedTimestamp }
        val syncText = latestTimestamp?.let { formatTimestamp(it) } ?: "Ready"

        ShelfUiState(
            books = filteredBooks,
            selectedFilter = filter,
            totalBooksCount = allBooks.size,
            completedBooksCount = completedCount,
            lastSyncLabel = syncText
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShelfUiState()
    )

    fun setFilter(status: ReadingStatus?) {
        _selectedFilter.value = status
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    // Runs a search request against OpenLibrary and updates loading/error states
    fun searchBooks(queryOverride: String? = null) {
        val queryToRun = (queryOverride ?: _searchQuery.value).trim()
        if (queryOverride != null) {
            _searchQuery.value = queryToRun
        }
        if (queryToRun.isEmpty()) {
            _searchUiState.value = SearchUiState.Idle
            return
        }

        viewModelScope.launch {
            _searchUiState.value = SearchUiState.Loading
            try {
                val results = repository.searchRemoteBooks(queryToRun)
                _searchUiState.value = SearchUiState.Success(results)
            } catch (e: Exception) {
                _searchUiState.value = SearchUiState.Error(
                    "Could not load books. Check your internet connection and try again."
                )
            }
        }
    }

    // Saves a book from search results into the local Room database
    fun addBookToShelf(book: BookEntity) {
        viewModelScope.launch {
            repository.saveBookToShelf(book)
        }
    }

    // Updates current page and automatically adjusts reading status
    fun updateReadingProgress(book: BookEntity, newPage: Int) {
        val safePage = newPage.coerceIn(0, book.totalPages)
        val updatedStatus = when {
            safePage >= book.totalPages -> ReadingStatus.COMPLETED
            safePage > 0 -> ReadingStatus.READING
            else -> ReadingStatus.WANT_TO_READ
        }
        viewModelScope.launch {
            repository.updateBook(
                book.copy(
                    currentPage = safePage,
                    status = updatedStatus
                )
            )
        }
    }

    // Manually changes status (e.g., marking a book as Completed or Want to Read)
    fun updateBookStatus(book: BookEntity, newStatus: ReadingStatus) {
        val adjustedPage = when (newStatus) {
            ReadingStatus.COMPLETED -> book.totalPages
            ReadingStatus.WANT_TO_READ -> 0
            ReadingStatus.READING -> if (book.currentPage == 0) 1 else book.currentPage
        }
        viewModelScope.launch {
            repository.updateBook(
                book.copy(
                    status = newStatus,
                    currentPage = adjustedPage
                )
            )
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            repository.deleteBook(book)
        }
    }

    fun getBookById(bookId: String) = repository.getBookById(bookId)

    private fun formatTimestamp(timestamp: Long): String {
        val formatter = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    // Factory used to pass BookRepository into the ViewModel
    class Factory(private val repository: BookRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StudyShelfViewModel(repository) as T
        }
    }
}