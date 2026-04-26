package com.example.bookbuddies.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.bookbuddies.ui.theme.MediumGrey
import java.util.Calendar
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.get
import timber.log.Timber

data class BookBanner(
    val book: Book,
    val startCol: Int, // 0-6 for Monday-Sunday
    val endCol: Int,
    val continuesBefore: Boolean, // if the span started before this week
    val continuesAfter: Boolean
)

/**
 * Given a list of BookBanners for a calendar week, pack them into rows so that banners that don't overlap in columns share the same row.
 *
 * @param banners a list of BookBanners to pack into rows.
 * @return a list of rows, where each row is a list of BookBanners that can be displayed on the same row without column overlap.
 */
fun packBanners(banners: List<BookBanner>): List<List<BookBanner>> {
    val rows = mutableListOf<MutableList<BookBanner>>()
    for (banner in banners) {
        // find first row where this banner fits (no column overlap)
        val targetRow = rows.firstOrNull { row ->
            row.none { existing ->
                banner.startCol <= existing.endCol && banner.endCol >= existing.startCol
            }
        }
        if (targetRow != null) {
            targetRow.add(banner)
        } else {
            rows.add(mutableListOf(banner))
        }
    }
    return rows
}

/**
 * For one calendar week, compute all banners to show.
 *
 * @param books books which reading interval overlap with the computed week
 * @param weekStartEpoch start of the week in epoch millis
 * @param weekEndEpoch end of the week in epoch millis
 * @return a list of BookBanners to display on this week
 */
fun getBannersForWeek(
    books: List<Book>,
    weekStartEpoch: Long,
    weekEndEpoch: Long
): List<BookBanner> {
    return books
        // filter out books that don't have a set read interval
        .filter { it.dateStarted > 0L && it.dateFinished > 0L }
        // filter out books that haven't been read during this week
        .filter { it.dateStarted <= weekEndEpoch && it.dateFinished >= weekStartEpoch}
        .map { book ->
            // compute at which week columns the banner starts and stops
            val startCol = if (book.dateStarted <= weekStartEpoch) 0
                            else dayOfWeekCol(book.dateStarted)
            val endCol = if (book.dateFinished >= weekEndEpoch) 6
                            else dayOfWeekCol(book.dateFinished)
            BookBanner(
                book = book,
                startCol = startCol,
                endCol = endCol,
                continuesBefore = book.dateStarted < weekStartEpoch,
                continuesAfter = book.dateFinished > weekEndEpoch
            )
        }
}

/**
 * Given the epoch time of the start of a week, compute the epoch time of the end of the week (Sunday 23:59:59).
 *
 * @param weekStart the epoch time of the start of the week in milliseconds
 * @return the epoch time of the end of the week in milliseconds
 */
fun weekEndEpoch(weekStart: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = weekStart
        add(Calendar.DAY_OF_MONTH, 6)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
    }
    return calendar.timeInMillis
}

/**
 * Convert an epoch time to a column index (0-6) for the calendar week, where 0 is Monday and 6 is Sunday.
 */
private fun dayOfWeekCol(epoch: Long): Int {
    val calendar = Calendar.getInstance().apply { timeInMillis = epoch }
    return (calendar.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7
}

/**
 * Extracts the dominant colour on a book's cover.
 *
 * @param coverPath file path to stored cover image
 * @return most dominant non-grey-scale colour
 */
fun coverColour(book: Book): Color {
    if (book.cover == null) return MediumGrey

    val coverPath = book.cover
    return try {
        // scale image resolution down for performance (just enough to see colours)
        val options = BitmapFactory.Options().apply { inSampleSize = 4 }
        val bitmap = BitmapFactory.decodeFile(coverPath, options) ?: return MediumGrey

        // mapping of colours and number of appearances
        val colourCounts = mutableMapOf<Int, Int>()

        val stepX = (bitmap.width / 20).coerceAtLeast(1)
        val stepY = (bitmap.height / 20).coerceAtLeast(1)

        for (x in 0 until bitmap.width step stepX) {
            for (y in 0 until bitmap.height step stepY) {
                val pixel = bitmap[x, y]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // skip neutral colours
                if (isNeutral(r, g, b)) continue
                // group similar colours
                val quantized = quantizeColor(r, g, b)
                val qr = (quantized shr 16) and 0xFF
                val qg = (quantized shr 8) and 0xFF
                val qb = quantized and 0xFF
                if (isNeutral(qr, qg, qb)) continue
                colourCounts[quantized] = (colourCounts[quantized] ?: 0) + 1
            }
        }

        colourCounts.entries
            .sortedByDescending { it.value }
            .take(10) // top 10 most frequent
            .forEach { (color, count) ->
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                Timber.tag("ColorExtract").d("${book.title} RGB($r, $g, $b) — count: $count")
            }

        if (colourCounts.isEmpty()) return MediumGrey

        // pick most frequent and vibrant colour
        val dominantColour = colourCounts.maxByOrNull { it.value }!!.key
        val r = (dominantColour shr 16) and 0xFF
        val g = (dominantColour shr 8) and 0xFF
        val b = dominantColour and 0xFF

        Color(r, g, b)

    } catch (e: Exception) {
        Timber.tag("ColourExtract").d("Failed to extract colour with error $e")
        MediumGrey
    }
}

/**
 * Returns true if the color is neutral (white, black, or grey).
 */
private fun isNeutral(r: Int, g: Int, b: Int): Boolean {
    // check if it's too dark or too light
    val brightness = (r * 0.299 + g * 0.587 + b * 0.114)
    if (brightness < 30) return true  // too dark/black
    if (brightness > 220) return true // too light/white

    // check if it's grey (low saturation)
    val max = maxOf(r, g, b).toFloat()
    val min = minOf(r, g, b).toFloat()
    val saturation = if (max == 0f) 0f else (max - min) / max
    if (saturation < 0.25f) return true // too grey

    return false
}

/**
 * Quantizes a color to reduce noise — groups similar colors into buckets.
 */
private fun quantizeColor(r: Int, g: Int, b: Int): Int {
    val step = 32 // bucket size
    val qr = (r / step) * step
    val qg = (g / step) * step
    val qb = (b / step) * step
    return (qr shl 16) or (qg shl 8) or qb
}