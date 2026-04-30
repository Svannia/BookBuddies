package com.example.bookbuddies.data

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.get
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.bookbuddies.R
import com.example.bookbuddies.ui.theme.MediumGrey
import timber.log.Timber
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val uid: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val cover: String?,
    val coverColours: List<Long> = emptyList(),
    val chosenCoverColour: Int = 0,
    val seriesName: String,
    val seriesNumber: Int,
    val mangaSeriesId: String ?= null,
    val description: String,
    val genre: String,
    val publisher: String,
    val publishedDate: Long,
    val rating: Double,
    val language: String,
    val format: String,
    val read: Boolean,
    val dateStarted: Long,
    val dateFinished: Long,
    val bookshelf: String,
    val source: String,
    val isGift: Boolean,
    val dateAdded: Long
) {
    companion object {
        /**
         * Creates an empty Book data object.
         *
         * @return empty Book data object.
         */
        fun empty(): Book {
            return Book("", "", "", emptyList(), null, emptyList(), 0,
                "", -1, null, "",
                "", "", 0L, 0.0, "", "",
                false, 0L, 0L, "", "", false,  System.currentTimeMillis()
            )
        }
    }
}

/**
 * Displays a book's authors' names as <First Name> <Last Name>, instead of how it's stored as <Last Name>, <First Name>.
 * All the authors are displayed comma-separated in case the authors list is longer than 1.
 *
 * @param authorsList each item is an author stored as <Last Name>, <First Name>
 * @return single string with the author(s), or empty String if there are no authors
 */
fun displayAuthors(authorsList: List<String>): String {
    if (authorsList.isEmpty()) return ""

    return authorsList.joinToString(", ") { author ->
        val parts = author.split(",")
        if (parts.size == 2) {
            val surname = parts[0].trim()
            val names = parts[1].trim()
            "$names $surname"
        } else {
            author
        }
    }
}

/**
 * Displays a single author's name as <First Name> <Last Name>, instead of how it's stored as <Last Name>, <First Name>.
 *
 * @param author string like <Last Name>, <First Name>
 * @return single string with the author, or empty String if there are no authors
 */
fun displayAuthor(author: String): String {
    val parts = author.split(",").map { it.trim() }
    return if (parts.size == 2) "${parts[1]} ${parts[0]}" else author
}

/**
 * Given an author written as <First Name> <Last Name>, returns it as <Last Name>, <First Name> for storage.
 *
 * @param author string like <First Name> <Last Name>
 * @return single string with the author
 */
fun storeAuthor(author: String): String {
    val parts = author.split(" ").map { it.trim() }
    return if (parts.size >= 2) {
        val surname = parts.last()
        val names = parts.dropLast(1).joinToString(" ")
        "$surname, $names"
    } else {
        author
    }
}

/**
 * Displays a series info as <Series Name> #<Series Number>.
 *
 * @param seriesName name of the series
 * @param seriesNumber number in the series. If negative, it is not displayed.
 * @return single string with all series info
 */
fun displaySeries(seriesName: String, seriesNumber: Int): String {
    val number = if (seriesNumber >= 0) "#$seriesNumber" else ""
    return "$seriesName $number"
}

/**
 * Translates a date stored as an Epoch number into a human-readable string, depending on the desired date format.
 *
 * @param date represents a date in milliseconds since the Unix epoch
 * @param format DateFormat entry for different types of date parsing:
 * FULL_DATE : 24 July 2001,
 * FULL_SHORT_DATE : 24 Jul 2001,
 * MONTH_YEAR : July 2001,
 * NUMBERED : 24/07/2001,
 * NUMBERED_REVERSE : 2001/07/24,
 * NUMBERED_WITH_TIME : 24/07/2001 19:50
 * @return date as text, or empty String if input date is 0L
 */
fun displayDate(date: Long, format: DateFormat): String {
    if (date <= 0L) return ""
    val calendar = Calendar.getInstance().apply { timeInMillis = date }
    val locale = Locale.getDefault()

    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val longMonth = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, locale)
    val shortMonth = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, locale)
    val year = calendar.get(Calendar.YEAR)

    return when (format) {
        DateFormat.FULL_DATE -> {
            "$day $longMonth $year"
        }
        DateFormat.FULL_SHORT_DATE -> {
            "$day $shortMonth $year"
        }
        DateFormat.MONTH_YEAR -> {
            "$longMonth $year"
        }
        DateFormat.NUMBERED -> {
            val numberDay = day.toString().padStart(2, '0')
            val numberMonth = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
            "$numberDay/$numberMonth/$year"
        }
        DateFormat.NUMBERED_REVERSE -> {
            val numberDay = day.toString().padStart(2, '0')
            val numberMonth = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
            "$year/$numberMonth/$numberDay"
        }
        DateFormat.NUMBERED_WITH_TIME -> {
            val numberDay = day.toString().padStart(2, '0')
            val numberMonth = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
            val hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
            val minute = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
            "$numberDay/$numberMonth/$year $hour:$minute"
        }
    }
}

// Various date formats for parsing
enum class DateFormat {
    FULL_DATE, FULL_SHORT_DATE, MONTH_YEAR, NUMBERED, NUMBERED_REVERSE, NUMBERED_WITH_TIME
}

// The different possible methods to sort and group books in the main page
enum class BookSorting {
    AUTHOR_SERIES, SERIES, TITLE, RECENTLY_ADDED, RATING, GENRE, LANGUAGE, FORMAT, SOURCE
}

val sortingMap = mapOf(
    BookSorting.AUTHOR_SERIES to R.string.sorting_author,
    BookSorting.SERIES to R.string.sorting_series,
    BookSorting.TITLE to R.string.sorting_title,
    BookSorting.RECENTLY_ADDED to R.string.sorting_added,
    BookSorting.RATING to R.string.sorting_rating,
    BookSorting.GENRE to R.string.sorting_genre,
    BookSorting.LANGUAGE to R.string.sorting_language,
    BookSorting.FORMAT to R.string.sorting_format,
    BookSorting.SOURCE to R.string.sorting_source
)

/**
 * Translates a BookSorting element into its corresponding string from strings.xml.
 *
 * @param context used to access the string resources
 * @return user-readable string
 */
fun BookSorting.getString(context: Context): String {
    return context.getString(sortingMap[this] ?: R.string.sorting_author)
}

/**
 * Translates a string stored in strings.xml into its corresponding BookSorting element.
 *
 * @param context used to access the string resources
 * @return entry from BookSorting enum
 */
fun String.getBookSorting(context: Context): BookSorting {
    return sortingMap.entries.firstOrNull {
        context.getString(it.value) == this
    }?.key ?: BookSorting.AUTHOR_SERIES
}

/**
 * Extracts the dominant colour on a book's cover.
 *
 * @param book book to recover the dominant colour from
 * @return most dominant colour
 */
fun getCoverColour(book: Book): Color {
    if (book.coverColours.isEmpty()) return MediumGrey
    val index = book.chosenCoverColour.coerceIn(0, book.coverColours.size - 1)
    return Color(book.coverColours[index])
}

fun extractColours(book: Book): List<Long> {
    if (book.cover == null) return emptyList()

    return try {
        // scale image resolution down for performance (just enough to see colours)
        val options = BitmapFactory.Options().apply { inSampleSize = 4 }
        val bitmap = BitmapFactory.decodeFile(book.cover, options) ?: return emptyList()

        // list of pixels (RGB channels)
        val pixels = mutableListOf<Triple<Int, Int, Int>>()
        val stepX = (bitmap.width / 20).coerceAtLeast(1)
        val stepY = (bitmap.height / 20).coerceAtLeast(1)

        // loop over image
        for (x in 0 until bitmap.width step stepX) {
            for (y in 0 until bitmap.height step stepY) {
                val pixel = bitmap[x, y]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                pixels.add(Triple(r, g, b))
            }
        }

        if (pixels.isEmpty()) return emptyList()

        // count how many buckets -> env. 5 buckets per cluster
        val k = (pixels.distinctBy { quantizeColor(it.first, it.second, it.third) }.size / 5)
            .coerceIn(2, 8)
        val clusters = kMeans(pixels, k)

        // sort clusters by size + take colour at center of cluster
        clusters.sortedByDescending { (center, count) ->
            val (r, g, b) = center
            val max = maxOf(r, g, b).toFloat()
            val min = minOf(r, g, b).toFloat()
            val saturation = if (max == 0f) 0f else (max - min) / max
            val brightness = (r * 0.299 + g * 0.587 + b * 0.114) / 255f

            // score for sorting: weighted count, saturation and brightness
            val brightnessScore = 1f - abs(brightness - 0.5f) * 2f
            val normalizedCount = count.toFloat() / pixels.size
            // equal weight between vibrancy and dominance
            saturation * 0.3f + brightnessScore * 0.2f + normalizedCount * 0.5f
        }.map { (center, _) ->
            val (r, g, b) = center
            (0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
        }

    } catch (e: Exception) {
        Timber.tag("ColourExtract").d("Failed to extract colour with error $e")
        emptyList()
    }
}

private fun quantizeColor(r: Int, g: Int, b: Int, step: Int = 32): Int {
    val qr = (r / step) * step
    val qg = (g / step) * step
    val qb = (b / step) * step
    return (qr shl 16) or (qg shl 8) or qb
}

/**
 * Simple k-means clustering on RGB pixels.
 *
 * @param pixels
 * @param k
 * @param iterations
 * @return list of (center, count) pairs
 */
fun kMeans(pixels: List<Triple<Int, Int, Int>>, k: Int, iterations: Int=10 ): List<Pair<Triple<Int, Int, Int>, Int>> {
    // initialize centers
    var centers = pixels.filterIndexed { i, _ -> i % (pixels.size / k).coerceAtLeast(1) == 0 }
        .take(k).toMutableList()

    val assignments = IntArray(pixels.size)

    // train
    repeat (iterations) {
        // assign data points to nearest center
        pixels.forEachIndexed { i, pixel ->
            assignments[i] = centers.indices.minByOrNull { colorDistance(pixel, centers[it]) } ?: 0
        }

        // recompute centers given current clusters' means
        centers = (0 until k).map { cluster ->
            val clusterPixels = pixels.filterIndexed { i, _ -> assignments[i] == cluster }
            if (clusterPixels.isEmpty()) centers[cluster]
            else Triple(
                clusterPixels.sumOf { it.first } / clusterPixels.size,
                clusterPixels.sumOf { it.second } / clusterPixels.size,
                clusterPixels.sumOf { it.third } / clusterPixels.size
            )
        }.toMutableList()
    }

    // count data points in each cluster
    val counts = IntArray(k)
    assignments.forEach { counts[it]++ }

    return centers.mapIndexed { i, center -> center to counts[i] }.filter { it.second > 0 }
}

private fun colorDistance(a: Triple<Int, Int, Int>, b: Triple<Int, Int, Int>): Int {
    val dr = a.first - b.first
    val dg = a.second - b.second
    val db = a.third - b.third
    return dr*dr + dg*dg + db*db
}