package com.example.bookbuddies.system

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object TelegramBot {
    private const val WORKER_URL = "https://bookbuddies-notify.reymondalice.workers.dev/"
    private const val CHAT_ID = 1357658600

    /**
     * Sends a bug report to the BookBuddiesBot on Telegram.
     *
     * @param message: bug description written by the user
     * @param logFile: text file containing all the app logs
     * @return Whether or not sending the bug report succeeded
     */
    suspend fun sendBugReport(message: String, logFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // post an API request to the Telegram bot as an HTTP request
                val boundary = "----BookBuddiesBoundary${System.currentTimeMillis()}"
                val url = URL(WORKER_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

                val outputStream = connection.outputStream
                val lineEnd = "\r\n"
                val twoHyphens = "--"

                // chat_id
                outputStream.write(
                    (twoHyphens + boundary + lineEnd +
                            "Content-Disposition: form-data; name=\"chat_id\"" + lineEnd +
                            lineEnd + CHAT_ID + lineEnd).toByteArray()
                )
                // caption
                outputStream.write(
                    (twoHyphens + boundary + lineEnd +
                            "Content-Disposition: form-data; name=\"caption\"" + lineEnd +
                            lineEnd + "Bug description: $message" + lineEnd).toByteArray()
                )

                // log file part
                outputStream.write(
                    (twoHyphens + boundary + lineEnd +
                            "Content-Disposition: form-data; name=\"document\"; filename=\"log.txt\"" + lineEnd +
                            "Content-Type: text/plain" + lineEnd +
                            lineEnd).toByteArray()
                )
                logFile.inputStream().copyTo(outputStream)
                outputStream.write((lineEnd + twoHyphens + boundary + twoHyphens + lineEnd).toByteArray())
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                connection.disconnect()
                responseCode == 200
            } catch (e: Exception) {
                Timber.tag("Error").e("Failed to send bug report with error:\n$e")
                false
            }
        }
    }
}