package com.example.studyshelfhelper.model

import com.google.gson.annotations.SerializedName

//Root response DTO returned by the OpenLibrary Search API
data class OpenLibrarySearchResponse(
    @SerializedName("numFound")
    val numFound: Int = 0,
    @SerializedName("docs")
    val docs: List<OpenLibraryBookDto> = emptyList()
)

//Individual book document DTO from OpenLibrary JSON response
data class OpenLibraryBookDto(
    @SerializedName("key")
    val key: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("author_name")
    val authorNames: List<String>?,
    @SerializedName("first_publish_year")
    val firstPublishYear: Int?,
    @SerializedName("cover_i")
    val coverId: Int?,
    @SerializedName("number_of_pages_median")
    val medianPages: Int?
) {
    //Converts a network DTO into a local BookEntity ready for UI display or Room storage
    fun toBookEntity(): BookEntity {
        val cleanId = key?.removePrefix("/works/") ?: title.hashCode().toString()
        val fullCoverUrl = coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" }
        return BookEntity(
            id = cleanId,
            title = title ?: "Unknown Title",
            author = authorNames?.joinToString(", ") ?: "Unknown Author",
            publishYear = firstPublishYear,
            coverUrl = fullCoverUrl,
            totalPages = (medianPages ?: 200).coerceAtLeast(1),
            currentPage = 0,
            status = ReadingStatus.WANT_TO_READ
        )
    }
}