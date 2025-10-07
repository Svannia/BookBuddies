package com.example.bookbuddies.data

import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState

@OptIn(ExperimentalMaterial3Api::class)
fun confirmDateSelection(
    datePickerState: DatePickerState,
    dateField: MutableLongState?,
    datePickerVisible: MutableState<Boolean>,
    extraConfirmActions: () -> Unit
) {
    datePickerState.selectedDateMillis?.let { millis ->
        dateField?.longValue = millis
        extraConfirmActions()
    }
    datePickerVisible.value = false
}