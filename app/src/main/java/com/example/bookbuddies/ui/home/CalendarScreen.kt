package com.example.bookbuddies.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bookbuddies.R
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.navigation.Route
import com.example.bookbuddies.ui.OptionsMenu
import com.example.bookbuddies.ui.PrimaryScreen
import com.example.bookbuddies.ui.theme.MyTypography
import com.example.bookbuddies.viewModels.BookViewModel
import com.example.bookbuddies.viewModels.CalendarViewModel
import java.util.Calendar

@Composable
fun CalendarScreen(bookVM: BookViewModel, calendarVM: CalendarViewModel, navigationActions: NavigationActions) {

    // when using the phone's built-in back function -> back to Home screen
    BackHandler {
        navigationActions.navigateTo(Route.HOME, true)
    }

    val today = remember {
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
    }

    PrimaryScreen(
        navigationActions = navigationActions,
        title = stringResource(R.string.title_calendar),
        navigationIndex = 1,
        topBarIcons = {
            Row(
                modifier = Modifier.padding(0.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // date of the month button to jump to today
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.inversePrimary,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { /* todo: jump to today */ }
                ) {
                    Text(
                        text = today,
                        style = MyTypography.bodyMedium.copy(textAlign = TextAlign.Center),
                        color = MaterialTheme.colorScheme.inversePrimary,
                    )
                }
                // options menu for various options
                OptionsMenu(
                    icon = R.drawable.options,
                    "Show/hide read books" to { /* todo */ }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {

        }
    }

}