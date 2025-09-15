package com.example.bookbuddies.ui.settings

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.bookbuddies.R
import com.example.bookbuddies.datastore.ThemeChoice
import com.example.bookbuddies.datastore.findBookCovers
import com.example.bookbuddies.datastore.importBooksFromCsv
import com.example.bookbuddies.errors.handleError
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.navigation.Route
import com.example.bookbuddies.system.TelegramBot
import com.example.bookbuddies.ui.CustomContentDialogWindow
import com.example.bookbuddies.ui.CustomTextField
import com.example.bookbuddies.ui.LoadingPage
import com.example.bookbuddies.ui.SecondaryScreen
import com.example.bookbuddies.ui.theme.MyTypography
import com.example.bookbuddies.ui.theme.ValidGreen
import com.example.bookbuddies.viewModels.BookViewModel
import com.example.bookbuddies.viewModels.DataViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val HEIGHT = 52
private const val OFFSET = 45

@Composable
fun Settings(dataVM: DataViewModel, bookVM: BookViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loading = remember { mutableStateOf(false) }

    val books by bookVM.allBooks.collectAsState(initial = emptyList())

    val importDialogue = remember { mutableStateOf(false) }

    val coversVisible = remember { mutableStateOf(false) }
    val failedCovers = remember { mutableListOf<String>() }

    // storage access permission
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let {
            // after selecting a doc, check if it is a csv file
            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            }

            // open and read file
            if (fileName != null && fileName.endsWith(".csv", ignoreCase = true)) {
                loading.value = true
                scope.launch {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val tempFile = File(context.cacheDir, fileName)
                        tempFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }

                        // import file data into BookRepository
                        importBooksFromCsv(
                            context,
                            tempFile,
                            bookVM::insertBooks,
                            callBack = {
                                loading.value = false
                                importDialogue.value = false
                            }
                        ) { isError ->
                            if (isError) handleError(context, "An error occurred while importing the selected file.")
                            else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_successfulImport),
                                    Toast.LENGTH_SHORT
                                ).show()
                                navigationActions.navigateTo(Route.HOME, true)
                            }
                        }
                    } else {
                        loading.value = false
                        handleError(context, "Failed to open the selected file.")
                    }
                }
            } else {
                Toast.makeText(context,
                    context.getString(R.string.toast_invalidCSV), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // variables for setting theme
    val themeChoice = convertThemeToText(dataVM.currentTheme.collectAsState().value)
    val themeChoices = ThemeChoice.entries.map { convertThemeToText(it) }
    val themeChoiceState = remember { mutableStateOf(themeChoice) }
    val darkTheme = stringResource(R.string.txt_systemDark)
    val lightTheme = stringResource(R.string.txt_systemLight)

    // variables for bug reporting
    val reportVisible = remember { mutableStateOf(false) }
    val bugReport = remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    if (loading.value) LoadingPage()
    else {
        SecondaryScreen(
            title = "Settings",
            navigationActions = navigationActions,
            navExtraActions = {},
            topBarIcons = {}
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // settings category for the theme
                item {
                    SettingCategory(stringResource(R.string.title_theme)) {
                        ToggleOptions(
                            numberChoices = themeChoices.size,
                            currentChoice = themeChoiceState,
                            choicesNames = themeChoices
                        ){ newChoice ->
                            var newTheme = ThemeChoice.SYSTEM_DEFAULT
                            if (newChoice == lightTheme) {
                                newTheme = ThemeChoice.LIGHT
                            } else if (newChoice == darkTheme) {
                                newTheme = ThemeChoice.DARK
                            }
                            dataVM.setTheme(newTheme)
                        }
                    }
                }
                // settings category for book data
                item {
                    SettingCategory(stringResource(R.string.title_data)) {
                        // Import data
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HEIGHT.dp)
                                .clickable {
                                    importDialogue.value = true
                                },
                            contentAlignment = Alignment.CenterStart
                        ) { Text(modifier = Modifier.padding(start = OFFSET.dp), text = stringResource(R.string.button_import), style = MyTypography.bodyLarge) }
                        // Export data
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HEIGHT.dp)
                                .clickable {
                                    // TODO
                                },
                            contentAlignment = Alignment.CenterStart
                        ) { Text(modifier = Modifier.padding(start = OFFSET.dp), text = stringResource(R.string.button_export), style = MyTypography.bodyLarge) }
                        // Find covers
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HEIGHT.dp)
                                .clickable {
                                    loading.value = true
                                    scope.launch {
                                        findBookCovers(
                                            context = context,
                                            books = books,
                                            insertBook = bookVM::insertBook,
                                            updateMangaSeriesId = bookVM::updateMangaSeriesId,
                                            callBack = { failedBooks ->
                                                loading.value = false
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.toast_successfulCovers),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                if (failedBooks.isEmpty()) navigationActions.navigateTo(Route.HOME, true)
                                                else {
                                                    failedCovers.clear()
                                                    failedCovers.addAll(failedBooks)
                                                    coversVisible.value = true
                                                }
                                            }
                                        ) { isError ->
                                            if (isError) {
                                                loading.value = false
                                                handleError(context, "An error occurred while finding covers.")
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.CenterStart
                        ) { Text(modifier = Modifier.padding(start = OFFSET.dp), text = stringResource(R.string.button_findCovers), style = MyTypography.bodyLarge) }
                        // Remove all covers
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HEIGHT.dp)
                                .clickable {
                                    loading.value = true
                                    scope.launch {
                                        bookVM.clearAllCovers({
                                            if (it) {
                                                loading.value = false
                                                handleError(context, "Failed to remove some covers.")
                                            }
                                        }) {
                                            loading.value = false
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.toast_removeCovers),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            navigationActions.navigateTo(Route.HOME, true)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.CenterStart
                        ) { Text(modifier = Modifier.padding(start = OFFSET.dp), text = stringResource(R.string.button_removeCovers), style = MyTypography.bodyLarge) }
                    }
                }
                // settings category for About information
                item {
                    SettingCategory(stringResource(R.string.title_about)) {
                        // button to open a dialog for sending bug information
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HEIGHT.dp)
                                .clickable { reportVisible.value = true },
                            contentAlignment = Alignment.CenterStart
                        ) { Text(modifier = Modifier.padding(start = OFFSET.dp), text = stringResource(R.string.button_sendBug), style = MyTypography.bodyLarge) }
                        // Credits for icons
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HEIGHT.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val annotatedString = buildAnnotatedString {
                                append(stringResource(R.string.txt_iconsBy))
                                pushLink(LinkAnnotation.Url("https://icons8.com"))
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append("Icons8")
                                }
                                pop()
                            }
                            Text(
                                modifier = Modifier.padding(start = OFFSET.dp),
                                text = annotatedString,
                                style = MyTypography.bodyMedium.copy(color = MaterialTheme.colorScheme.outline),
                            )
                        }
                    }
                }
            }

            // List of failed covers
            if (coversVisible.value) {
                CustomContentDialogWindow(
                    visible = coversVisible,
                    confirmText = stringResource(R.string.button_confirm),
                    confirmColour = ValidGreen,
                    onConfirm = { navigationActions.navigateTo(Route.HOME, true) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = context.getString(R.string.title_failedCovers), style = MyTypography.titleSmall)
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            failedCovers.forEach { cover ->
                                item {
                                    Text(text = cover, style = MyTypography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            // Import a CSV file instructions
            if (importDialogue.value) {
                CustomContentDialogWindow(
                    visible = importDialogue,
                    confirmText = stringResource(R.string.button_confirm),
                    confirmColour = ValidGreen,
                    onConfirm = { importLauncher.launch(arrayOf("*/*")) }
                ) {
                    Text(
                        text = stringResource(R.string.txt_importInstructions),
                        style = MyTypography.bodyLarge
                    )
                }
            }

            // Report a bug dialog window
            if (reportVisible.value) {
                CustomContentDialogWindow(
                    visible = reportVisible,
                    confirmText = stringResource(R.string.button_send),
                    confirmColour = ValidGreen,
                    onConfirm = {
                        bugReport.value = bugReport.value.trimEnd()
                        if (bugReport.value.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.toast_emptyBugReport), Toast.LENGTH_SHORT).show()
                        } else {
                            reportVisible.value = false
                            coroutineScope.launch {
                                val success = TelegramBot.sendBugReport(bugReport.value, File(context.filesDir, "log.txt"))

                                withContext(Dispatchers.Main) {
                                    if (success) {
                                        Toast.makeText(context, context.getString(R.string.toast_bugReport), Toast.LENGTH_SHORT).show()
                                        bugReport.value = ""
                                    } else {
                                        handleError(context, "Failed to send bug report")
                                    }
                                }
                            }
                        }
                    }
                ) {
                    // title for Report a bug, input text field and log.txt explanation
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = context.getString(R.string.button_sendBug), style = MyTypography.titleSmall)
                        CustomTextField(
                            value = bugReport.value,
                            onValueChange = { bugReport.value = it },
                            icon = -1,
                            placeHolder = stringResource(R.string.field_bugReport),
                            singleLine = false,
                            maxLength = 700,
                            showMaxChara = false,
                            width = 250.dp,
                            height = 350.dp
                        )
                        Text(text = stringResource(R.string.txt_reportBugNote), style = MyTypography.bodyMedium)
                    }
                }
            }
        }
    }
}

/**
 * Creates the layout for a category (family) of settings on the Settings screen.
 *
 * @param name name of the settings category
 * @param content elements of the settings category contained in a column
 */
@Composable
private fun SettingCategory(name: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(text = name, style = MyTypography.titleSmall, modifier = Modifier.padding(start = 16.dp))
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) { content() }
        HorizontalDivider(thickness = 3.dp, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.size(16.dp))
    }
}

/**
 * For a specific setting, handles a list of options where exactly one option can and must be selected.
 *
 * @param numberChoices number of options in the list
 * @param currentChoice option that is currently selected
 * @param choicesNames list of all the options' names
 * @param onToggle block that runs when a new option is toggled on, with the name of the new option selected
 */
@Composable
private fun ToggleOptions(numberChoices: Int, currentChoice: MutableState<String>, choicesNames: List<String>, onToggle: (String) -> Unit) {
    var toggledIndex by remember { mutableIntStateOf(choicesNames.indexOf(currentChoice.value)) }

    for (i in 0 until numberChoices) {
        ToggleBox(choicesNames[i], toggledIndex == i) {
            toggledIndex = i
            currentChoice.value = choicesNames[i]
            onToggle(choicesNames[i])
        }
    }
}

/**
 * Creates the layout of a single option within a list of options that can be selected / toggled on.
 *
 * @param name displayed as the option's name
 * @param isToggled whether this specific option is toggled on or not
 * @param onToggle block that runs if this option is toggled on
 */
@Composable
private fun ToggleBox(name: String, isToggled: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HEIGHT.dp)
            .clickable { onToggle() },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = OFFSET.dp, end = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                modifier = Modifier.size(20.dp),
                selected = isToggled,
                onClick = { onToggle() },
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary))
            Text(text = name, style = MyTypography.bodyLarge)
        }
    }
}

/**
 * Converts the themes objects understood by the system as a name that can be displayed to the user.
 *
 * @param theme ThemeChoice to be converted
 * @return name of the ThemeChoice as a string
 */
@Composable
private fun convertThemeToText(theme: ThemeChoice): String {
    return when (theme) {
        ThemeChoice.SYSTEM_DEFAULT -> stringResource(R.string.txt_systemDefault)
        ThemeChoice.DARK -> stringResource(R.string.txt_systemDark)
        ThemeChoice.LIGHT -> stringResource(R.string.txt_systemLight)
    }
}