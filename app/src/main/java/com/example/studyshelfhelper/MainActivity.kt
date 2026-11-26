package com.example.studyshelfhelper

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.studyshelfhelper.data.BookRepository
import com.example.studyshelfhelper.data.OpenLibraryApiService
import com.example.studyshelfhelper.data.StudyShelfDatabase
import com.example.studyshelfhelper.ui.BookDetailScreen
import com.example.studyshelfhelper.ui.DiscoverScreen
import com.example.studyshelfhelper.ui.ShelfScreen
import com.example.studyshelfhelper.ui.StudyShelfViewModel
import com.example.studyshelfhelper.ui.theme.StudyShelfHelperTheme
import com.example.studyshelfhelper.workers.SyncBooksWorker
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    // Holds text shared from external apps via Android's Share sheet
    private var incomingSharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Extract shared text if the app was launched via ACTION_SEND
        incomingSharedText = extractSharedText(intent)

        // Schedule daily background metadata sync when network is available
        scheduleBackgroundBookSync()

        // Set up database, network service, and repository
        val database = StudyShelfDatabase.getDatabase(applicationContext)
        val apiService = OpenLibraryApiService.create()
        val repository = BookRepository(database.bookDao(), apiService)

        setContent {
            StudyShelfHelperTheme {
                val viewModel: StudyShelfViewModel = viewModel(
                    factory = StudyShelfViewModel.Factory(repository)
                )

                StudyShelfApp(
                    viewModel = viewModel,
                    sharedText = incomingSharedText,
                    onSharedTextConsumed = { incomingSharedText = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val shared = extractSharedText(intent)
        if (!shared.isNullOrBlank()) {
            incomingSharedText = shared
        }
    }

    // Reads plain text from incoming ACTION_SEND intents
    private fun extractSharedText(intent: Intent?): String? {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            return intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        }
        return null
    }

    // Configures WorkManager to periodically refresh book details in the background
    private fun scheduleBackgroundBookSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncBooksWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "daily_book_metadata_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}

@Composable
fun StudyShelfApp(
    viewModel: StudyShelfViewModel,
    sharedText: String?,
    onSharedTextConsumed: () -> Unit
) {
    val navController = rememberNavController()

    // Automatically open Discover screen and search when text is shared into the app
    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            viewModel.searchBooks(sharedText)
            onSharedTextConsumed()
            navController.navigate("discover") {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "shelf"
    ) {
        composable("shelf") {
            ShelfScreen(
                viewModel = viewModel,
                onBookClick = { bookId ->
                    val encodedId = URLEncoder.encode(bookId, StandardCharsets.UTF_8.toString())
                    navController.navigate("detail/$encodedId")
                },
                onSearchClick = {
                    navController.navigate("discover")
                }
            )
        }

        composable("discover") {
            DiscoverScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "detail/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getString("bookId").orEmpty()
            val decodedId = URLDecoder.decode(rawId, StandardCharsets.UTF_8.toString())
            BookDetailScreen(
                bookId = decodedId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}