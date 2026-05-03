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
            .coerceIn(4, 12)
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

/**
 * Simple k-means clustering on RGB pixels.
 *
 * @param pixels
 * @param k
 * @param iterations
 * @return list of (center, count) pairs
 */
private fun kMeans(pixels: List<Triple<Int, Int, Int>>, k: Int, iterations: Int=10 ): List<Pair<Triple<Int, Int, Int>, Int>> {
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

/**
 * Calculates distance between two colours.
 *
 * @param a first colour as a triple of its rgb values
 * @param b second colour as a triple of its rgb values
 * @return distance between the two colours
 */
private fun colorDistance(a: Triple<Int, Int, Int>, b: Triple<Int, Int, Int>): Int {
    val dr = a.first - b.first
    val dg = a.second - b.second
    val db = a.third - b.third
    return dr*dr + dg*dg + db*db
}