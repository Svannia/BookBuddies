package com.appbuddies.bookbuddies.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.appbuddies.bookbuddies.R
import com.appbuddies.bookbuddies.navigation.NavigationActions
import com.appbuddies.bookbuddies.navigation.Route
import com.appbuddies.bookbuddies.ui.SecondaryScreen
import com.appbuddies.bookbuddies.ui.theme.MyTypography

@Composable
fun EnterISBN(navigationActions: NavigationActions) {
    val isbn = remember { mutableStateOf("") }
    SecondaryScreen(
        title = stringResource(R.string.title_enterISBN),
        navigationActions = navigationActions,
        navExtraActions = {},
        topBarIcons = {}
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // text field to display entered ISBN
            item {
                TextField(
                    modifier = Modifier.padding(0.dp),
                    value = isbn.value,
                    onValueChange = {},
                    enabled = false,
                    textStyle = MyTypography.bodyLarge,
                    leadingIcon = {
                        Row{
                            Icon(
                                painter = painterResource(R.drawable.barcode),
                                contentDescription = stringResource(R.string.desc_textFieldIcon),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.size(16.dp))
                        }
                    },
                    placeholder = {
                        Text(text = stringResource(R.string.field_isbnPlaceholder), style = MyTypography.bodySmall)
                    },
                    colors = TextFieldDefaults.colors(
                        disabledContainerColor = Color.Transparent,
                        disabledIndicatorColor = MaterialTheme.colorScheme.inversePrimary,
                        disabledTextColor = MaterialTheme.colorScheme.inversePrimary,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.inversePrimary,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.inversePrimary
                    )
                )
            }

            // keypad
            item {
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "X", "0", "⌫")

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    keys.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                        ) {
                            row.forEach { key ->
                                KeypadButton(64.dp, 64.dp, key) {
                                    when (key) {
                                        "⌫" -> {
                                            if (isbn.value.isNotEmpty()) {
                                                isbn.value = isbn.value.dropLast(1)
                                            }
                                        }
                                        else -> {
                                            if (isbn.value.length < 13) {
                                                isbn.value += key
                                            }
                                        }}
                                }
                            }
                        }
                    }
                }
            }

            // clear all + search buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // clear all button
                    KeypadButton(150.dp, 64.dp, stringResource(R.string.button_clearAll)) {
                        isbn.value = ""
                    }

                    // search button
                    KeypadButton(150.dp, 64.dp, stringResource(R.string.button_search)) {
                        navigationActions.navigateTo("${Route.BOOK_CREATE}?isbn=${isbn.value}")
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(width: Dp, height: Dp, label: String, onClick: () -> Unit) {
    TextButton(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.outline),
        onClick = { onClick() }
    ) {
        Text(
            text = label, style = MyTypography.titleMedium
        )
    }
}
