package com.example.bookbuddies.errors

import android.content.Context
import android.widget.Toast
import com.example.bookbuddies.R
import timber.log.Timber

/**
 * In case of an error, call this function to display a Toast to the user, that simply indicates that there was an error.
 * The logcat tag Error displays more details about the error.
 *
 * @param context to display the toast
 * @param errorMssg a short text written by and for developers to shortly explain the issue.
 * @param e the caught exception if any
 */
fun handleError(context: Context, errorMssg: String, e: Exception ?= null) {
    val toastMssg = errorMssg.ifBlank { context.getString(R.string.toast_unknownError) }
    Toast.makeText(context, toastMssg, Toast.LENGTH_SHORT).show()
        Timber.tag("Error").e("$errorMssg ${e?.let { "with error: $it" }}")
}