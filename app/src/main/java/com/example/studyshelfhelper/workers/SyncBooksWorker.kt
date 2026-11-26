package com.example.studyshelfhelper.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.studyshelfhelper.data.BookRepository
import com.example.studyshelfhelper.data.OpenLibraryApiService
import com.example.studyshelfhelper.data.StudyShelfDatabase

// Background worker that refreshes book details (like page counts) once a day
class SyncBooksWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = StudyShelfDatabase.getDatabase(applicationContext)
            val apiService = OpenLibraryApiService.create()
            val repository = BookRepository(database.bookDao(), apiService)

            repository.syncSavedBooksMetadata()
            Result.success()
        } catch (e: Exception) {
            // Retry later if the network fails during sync
            Result.retry()
        }
    }
}