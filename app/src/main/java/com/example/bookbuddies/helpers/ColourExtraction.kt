package com.example.bookbuddies.helpers

import android.graphics.BitmapFactory
import androidx.core.graphics.get
import com.example.bookbuddies.data.Book
import timber.log.Timber
import kotlin.math.abs

/**
 * Given a book, fetches its cover and extracts multiple most dominant colours.
 *
 * @param book to extract the cover colours from
 * @return list of Long values representing colours
 */
fun extractColours(book: Book): List<Long> {
    if (book.cover == null) return emptyList()

    return try {
        // scale image resolution down for performance (just enough to see colours)
        val options = BitmapFactory.Options().apply { inSampleSize = 4 }
        val bitmap = BitmapFactory.decodeFile(book.cover, options) ?: return emptyList()

        // list of pixels (RGB channels)
        val stepX = (bitmap.width / 20).coerceAtLeast(1)
        val stepY = (bitmap.height / 20).coerceAtLeast(1)

        // count colours in small buckets
        val colourCounts = mutableMapOf<Int, Int>()
        // loop over image
        for (x in 0 until bitmap.width step stepX) {
            for (y in 0 until bitmap.height step stepY) {
                val pixel = bitmap[x, y]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val quantized = quantizeColor(r, g, b)
                colourCounts[quantized] = (colourCounts[quantized] ?: 0) + 1
            }
        }

        if (colourCounts.isEmpty()) return emptyList()

        val totalPixels = colourCounts.values.sum()

        // group nearby colours dynamically -> larger merge radius for dense regions
        val groups = mutableListOf<MutableList<Int>>()

        for (colour in colourCounts.keys.sortedByDescending { colourCounts[it] }) {
            val r = (colour shr 16) and 0xFF
            val g = (colour shr 8) and 0xFF
            val b = colour and 0xFF

            // find closest group to merge into
            val targetGroup = groups.firstOrNull { group ->
                group.any { existing ->
                    val er = (existing shr 16) and 0xFF
                    val eg = (existing shr 8) and 0xFF
                    val eb = existing and 0xFF
                    val dist = colorDistanceInt(r, g, b, er, eg, eb)
                    // merge threshold smaller is sparse region (to avoid loosing small colours)
                    val freq = (colourCounts[colour] ?: 0) / totalPixels.toFloat()
                    val threshold = if (freq > 0.05f) 9000 else 1000
                    dist < threshold
                }
            }

            if (targetGroup != null) {
                targetGroup.add(colour)
            } else {
                groups.add(mutableListOf(colour))
            }
        }

        // pick most frequent colour in each group (not average)
        val representatives = groups.map { group ->
            val best = group.maxByOrNull { colourCounts[it] ?: 0 }!!
            val count = group.sumOf { colourCounts[it] ?: 0 }
            best to count
        }

        // score and sort colours
        Timber.tag("ColourExtract").d("Extracted cover colours for ${book.title}")
        representatives.sortedByDescending { (color, count) ->
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            // find saturation
            val max = maxOf(r, g, b).toFloat()
            val min = minOf(r, g, b).toFloat()
            val saturation = if (max == 0f) 0f else (max - min) / max
            // find brightness
            val brightness = (r * 0.299 + g * 0.587 + b * 0.114) / 255f
            val brightnessScore = 1f - abs(brightness - 0.5f) * 2f
            // normalize count
            val normalizedCount = count.toFloat() / totalPixels
            // score from saturation, brightness and count
            val score = saturation * 0.3f + brightnessScore * 0.3f + normalizedCount * 0.4f
            score
        }.take(20).map { (color, _) ->
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            (0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
        }

    } catch (e: Exception) {
        Timber.tag("ColourExtract").d("Failed to extract colour with error $e")
        emptyList()
    }
}

/**
 * Maps a given colour to its closest colour bucket.
 *
 * @param r red value
 * @param g green value
 * @param b blue value
 * @param step bucket colour size
 * @return bucket number
 */
private fun quantizeColor(r: Int, g: Int, b: Int, step: Int = 16): Int {
    val qr = (r / step) * step
    val qg = (g / step) * step
    val qb = (b / step) * step
    return (qr shl 16) or (qg shl 8) or qb
}

private fun colorDistanceInt(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Int {
    val dr = r1 - r2
    val dg = g1 - g2
    val db = b1 - b2
    return dr * dr + dg * dg + db * db
}