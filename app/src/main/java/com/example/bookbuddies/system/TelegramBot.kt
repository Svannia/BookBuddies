package com.example.bookbuddies.system

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import com.example.bookbuddies.BuildConfig

object TelegramBot {
    private const val BOT_TOKEN = BuildConfig.TELEGRAM_BOT_TOKEN
    private const val CHAT_ID = BuildConfig.TELEGRAM_CHAT_ID

    /**
     * Sends a bug report to the BookBuddiesBot on Telegram.
     *
     * @param message: bug description written by the user
     * @param logFile: text file containing all the app logs
     * @return Whether or not sending the bug report succeeded
     */
    suspend fun sendBugReport( message: String, logFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // post an API request to the Telegram bot as an HTTP request
                val boundary = "----BookBuddiesBoundary${System.currentTimeMillis()}"
                val url = URL("https://api.telegram.org/bot$BOT_TOKEN/sendDocument")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

                val outputStream = connection.outputStream
                writeMultipart(outputStream, boundary, message, logFile)
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

    /**
     * Constructs a multipart HTTP request body to a provided output stream.
     *
     * @param output: output stream to write to
     * @param boundary: a unique string that separates the different parts of the data body
     * @param message: bug description written by the user
     * @param logFile: the file whose contents will be uploaded as part of the multipart body
     */
    private fun writeMultipart(output: OutputStream, boundary: String, message: String, logFile: File) {
        val lineEnd = "\r\n"
        val twoHyphens = "--"

        // first part: username and bug description of the user
        output.write(
            (twoHyphens + boundary + lineEnd +
                    "Content-Disposition: form-data; name=\"caption\"" + lineEnd +
                    lineEnd + "Bug description: $message" + lineEnd).toByteArray()
        )

        // second part: Telegram bot chatID to be able to send a message
        output.write(
            (twoHyphens + boundary + lineEnd +
                    "Content-Disposition: form-data; name=\"chat_id\"" + lineEnd +
                    lineEnd + CHAT_ID + lineEnd).toByteArray()
        )

        // last part: send a document along with the Telegram message
        output.write(
            (twoHyphens + boundary + lineEnd +
                    "Content-Disposition: form-data; name=\"document\"; filename=\"log.txt\"" + lineEnd +
                    "Content-Type: text/plain" + lineEnd +
                    lineEnd).toByteArray()
        )

        // copy the contents of the text file to the output stream
        logFile.inputStream().copyTo(output)
        // end the request with a closing boundary
        output.write((lineEnd + twoHyphens + boundary + twoHyphens + lineEnd).toByteArray())
    }
}