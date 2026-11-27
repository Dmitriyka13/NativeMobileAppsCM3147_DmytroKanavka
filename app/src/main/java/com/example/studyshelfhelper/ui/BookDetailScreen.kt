package com.example.studyshelfhelper.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.studyshelfhelper.model.BookEntity
import com.example.studyshelfhelper.model.ReadingStatus
import kotlin.math.roundToInt

// Detail screen allowing users to track page progress, change status, open on OpenLibrary, or share progress
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookDetailScreen(
    bookId: String,
    viewModel: StudyShelfViewModel,
    onNavigateBack: () -> Unit
) {
    val book by viewModel.getBookById(bookId).collectAsState(initial = null)
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading Progress") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to shelf"
                        )
                    }
                },
                actions = {
                    if (book != null) {
                        IconButton(onClick = { shareReadingProgress(context, book!!) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share progress"
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete book"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val currentBook = book
        if (currentBook == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Text("Loading book details...")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header with cover image, title, author, and publish year
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 100.dp, height = 145.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (currentBook.coverUrl != null) {
                        AsyncImage(
                            model = currentBook.coverUrl,
                            contentDescription = currentBook.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentBook.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentBook.author,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val yearInfo = currentBook.publishYear?.toString() ?: "N/A"
                    Text(
                        text = "First published: $yearInfo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Total pages: ${currentBook.totalPages}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Status selector chips
            Text(
                text = "Reading Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ReadingStatus.entries.forEach { status ->
                    FilterChip(
                        selected = currentBook.status == status,
                        onClick = { viewModel.updateBookStatus(currentBook, status) },
                        label = { Text(status.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Interactive page tracker card with slider and +/- buttons
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Page Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${currentBook.currentPage} / ${currentBook.totalPages} (${currentBook.progressPercentage}%)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Slider(
                        value = currentBook.currentPage.toFloat(),
                        onValueChange = { sliderValue ->
                            viewModel.updateReadingProgress(currentBook, sliderValue.roundToInt())
                        },
                        valueRange = 0f..currentBook.totalPages.toFloat().coerceAtLeast(1f),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                viewModel.updateReadingProgress(currentBook, currentBook.currentPage - 1)
                            },
                            enabled = currentBook.currentPage > 0
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease page")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.updateBookStatus(currentBook, ReadingStatus.COMPLETED)
                            },
                            enabled = currentBook.status != ReadingStatus.COMPLETED
                        ) {
                            Text("Mark as Completed")
                        }

                        FilledTonalIconButton(
                            onClick = {
                                viewModel.updateReadingProgress(currentBook, currentBook.currentPage + 1)
                            },
                            enabled = currentBook.currentPage < currentBook.totalPages
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Increase page")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Opens the book page / online reader on OpenLibrary via browser Intent
            OutlinedButton(
                onClick = { openBookOnOpenLibrary(context, currentBook) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Read / Preview on OpenLibrary")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { shareReadingProgress(context, currentBook) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Reading Progress")
            }
        }
    }

    // Confirmation dialog before deleting a book from local storage
    if (showDeleteDialog && book != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove Book") },
            text = { Text("Remove \"${book!!.title}\" from your offline shelf?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val bookToDelete = book!!
                        showDeleteDialog = false
                        viewModel.deleteBook(bookToDelete)
                        onNavigateBack()
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Opens the book's OpenLibrary web page for online reading or preview
private fun openBookOnOpenLibrary(context: Context, book: BookEntity) {
    val normalizedPath = if (book.id.startsWith("/")) book.id else "/works/${book.id}"
    val url = "https://openlibrary.org$normalizedPath"
    val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        context.startActivity(viewIntent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No browser app found to open link", Toast.LENGTH_SHORT).show()
    }
}

// Launches Android's native share sheet with formatted book progress text
private fun shareReadingProgress(context: Context, book: BookEntity) {
    val shareText = "I'm tracking \"${book.title}\" by ${book.author} on StudyShelf: " +
            "${book.currentPage}/${book.totalPages} pages (${book.progressPercentage}%) - ${book.status.label}."
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    val chooser = Intent.createChooser(sendIntent, "Share reading progress")
    context.startActivity(chooser)
}