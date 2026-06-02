package com.appbuddies.bookbuddies.helpers

import android.content.Context
import android.os.Build
import android.text.Html.FROM_HTML_MODE_LEGACY
import android.text.Html.fromHtml
import com.appbuddies.bookbuddies.BuildConfig
import com.appbuddies.bookbuddies.data.Book
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.util.Locale

/**
 * Given an ISBN, searches Google Books API for book metadata.
 *
 * @param context to fetch cover image
 * @param isbn ISBN number to search for
 * @param isError callback for error handling
 * @param callback returns Book object if found
 */
fun searchByISBN(context: Context, isbn: String, isError: (Boolean) -> Unit, callback: (Book) -> Unit) {
    val apiKey = BuildConfig.GOOGLE_BOOKS_API_KEY

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val client = OkHttpClient()

            // GOOGLE BOOKS API
            // search book results with ISBN (ordered by relevance)
            val googleUrl = "https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn&key=$apiKey"
            val googleRequest = Request.Builder()
                .url(googleUrl)
                .header("User-Agent", "BookBuddiesApp/1.0")
                .build()

            client.newCall(googleRequest).execute().use { googleResponse ->
                if (!googleResponse.isSuccessful) {
                    throw Exception("Google Books ISBN search failed with code ${googleResponse.code}")
                }

                // if GET call was successful -> read body
                val body = googleResponse.body?.string().orEmpty()
                val json = JSONObject(body)
                val items = json.optJSONArray("items")

                if (items == null || items.length() == 0) {
                    throw Exception("No Google Book results for ISBN $isbn")
                }

                // if there is at least one result, get volumeID of first one
                val volumeId = items.getJSONObject(0).optString("id", "")
                if (volumeId.isBlank()) {
                    throw Exception("Empty volumeID for ISBN $isbn")
                }

                // second API call with volumeID (usually has better book metadata)
                val volumeUrl = "https://www.googleapis.com/books/v1/volumes/$volumeId?key=$apiKey"
                val volumeRequest = Request.Builder()
                    .url(volumeUrl)
                    .header("User-Agent", "BookBuddiesApp/1.0")
                    .build()
                client.newCall(volumeRequest).execute().use { volumeResponse ->
                    if (!volumeResponse.isSuccessful) {
                        throw Exception("Google Books volume fetch failed with code ${volumeResponse.code}")
                    }

                    // if volumeID GET call was successful -> read body
                    val volumeBody = volumeResponse.body?.string().orEmpty()
                    // if body is valid -> read volume info
                    val volumeJson = JSONObject(volumeBody)
                    val volumeInfo = volumeJson.getJSONObject("volumeInfo")

                    val title = volumeInfo.optString("title", "")
                    val authors = volumeInfo.optJSONArray("authors")?.let { list ->
                        List(list.length()) { index ->
                            storeAuthor(list.getString(index))
                        }
                    } ?: emptyList()

                    val description = formatHtmlString(volumeInfo.optString("description", ""))
                    val genre = volumeInfo.optJSONArray("categories")?.getString(0) ?: ""

                    val publisher = volumeInfo.optString("publisher", "")
                    val publishedDateInfo = volumeInfo.optString("publishedDate", "")
                    val publishedDate =
                        parseDate(publishedDateInfo) { isError ->
                            Timber.tag("SearchISBN").d("Could not parse published date $publishedDateInfo for ISBN: $isbn")
                        } ?: 0L

                    val languageCode = volumeInfo.optString("language", "")
                    val language = Locale.forLanguageTag(languageCode).displayLanguage
                    val format = volumeInfo.optString("printType", "").lowercase().replaceFirstChar { it.uppercase() }

                    val book = Book(
                        uid = java.util.UUID.randomUUID().toString(),
                        isbn = isbn,
                        title = title,
                        authors = authors,
                        cover = null,
                        seriesName = "",
                        seriesNumber = -1,
                        description = description,
                        genre = genre,
                        publisher = publisher,
                        publishedDate = publishedDate,
                        rating = 0.0,
                        language = language,
                        format = format,
                        read = false,
                        dateStarted = 0L,
                        dateFinished = 0L,
                        bookshelf = "",
                        source = "",
                        isGift = false,
                        dateAdded = System.currentTimeMillis()
                    )

                    // try to fetch cover
                    val bookWithCover = try {
                        fetchCoverForBook(context, book)
                    } catch (e: Exception) {
                        Timber.tag("ISBN").d("Could not fetch cover for book with ISBN: $isbn, with error: $e")
                        book
                    }

                    withContext(Dispatchers.Main) {
                        callback(bookWithCover)
                    }
                    return@launch
                }
            }
        } catch (e: Exception) {
            Timber.tag("ISBN").e("Failed to search book by ISBN: $isbn with error: $e")
            withContext(Dispatchers.Main) {
                isError(true)
            }
        }
    }
}

private fun formatHtmlString(rawText: String) : String {
    if (rawText.isBlank()) return ""

    // decode html entities (e.g. &amp; -> &)
    val decoded = fromHtml(rawText, FROM_HTML_MODE_LEGACY).toString()
    // regex for remaining HTML tags (e.g. <b>...</b>)
    val withoutTags = decoded.replace(Regex("<[^>]+>"), "")
    // normalize whitespaces (keep newlines)
    val normalized = withoutTags.replace(Regex("[ \\t]+"), " ")
    return normalized.trim()
}

