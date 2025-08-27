package com.example.bookbuddies.errors

import android.annotation.SuppressLint
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * This class overwrites the use of Log to use Timber instead, which allows to write a file with all app logs.
 *
 * @property logFile File to which the Timber logs are written
 * @property maxLines max number of lines in the logs file. When the limit is reached, older logs are deleted to leave space for new logs
 */
class FileLoggingTree(
    private val logFile: File,
    private val maxLines: Int = 500
) : Timber.DebugTree() {
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    /**
     * Overrides the Timber logs to log them in the terminal and write them on a log.txt file saved locally on the user's phone.
     *
     * @param priority priority as set by the original log function
     * @param tag string tags set to help sort out logs
     * @param message actual log message
     * @param t optional: used if the log should throw
     */
    @SuppressLint("LogNotTimber")
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        Log.d(tag, message)
        val logMessage = "[${dateFormat.format(Date())}] ${tag ?: "General"}: $message\n"
        try {
            logFile.appendText(logMessage)
            truncateIfTooLong()
        } catch (e: IOException) {
            Log.e("FileLoggingTree", "Failed to write log", e)
        }
    }

    /**
     * If the log.txt file has reached its lines limit, remove the oldest line and add the new one at the end of the file.
     */
    private fun truncateIfTooLong() {
        val lines = logFile.readLines()
        if (lines.size <= maxLines) return

        val trimmedLines = lines.takeLast(maxLines)
        logFile.writeText(trimmedLines.joinToString("\n") + "\n")
    }
}