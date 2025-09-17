package com.example.bookbuddies.ui.settings

import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
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
import com.example.bookbuddies.ui.ProgressBar
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

    val progressing = remember { mutableStateOf(false) }
    val processed = remember { mutableIntStateOf(0) }
    val total = remember { mutableIntStateOf(0) }

    val books by bookVM.allBooks.collectAsState(initial = emptyList())

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
                            bookVM::getBookById,
                            callBack = {
                                loading.value = false
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
    else if (progressing.value) {
        ProgressBar(processed.intValue, total.intValue)
    }
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
                        ToolTipRow(
                            onSettingClick = {
                                importLauncher.launch(arrayOf("*/*"))
                            },
                            settingText = stringResource(R.string.button_import),
                            toolTipText = stringResource(R.string.txt_importTooltip)
                        )
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
                        ToolTipRow(
                            onSettingClick = {
                                progressing.value = true
                                scope.launch {
                                    findBookCovers(
                                        context = context,
                                        books = books,
                                        insertBook = bookVM::insertBook,
                                        updateMangaSeriesId = bookVM::updateMangaSeriesId,
                                        callBack = { failedBooks ->
                                            progressing.value = false
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.toast_successfulCovers),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            if (failedBooks.isEmpty()) navigationActions.navigateTo(
                                                Route.HOME,
                                                true
                                            )
                                            else {
                                                failedCovers.clear()
                                                failedCovers.addAll(failedBooks)
                                                coversVisible.value = true
                                            }
                                        },
                                        isError = { isError ->
                                            if (isError) {
                                                progressing.value = false
                                                handleError(
                                                    context,
                                                    "An error occurred while finding covers."
                                                )
                                            }
                                        },
                                        onProgress = { processedNb, totalNb ->
                                            processed.intValue = processedNb
                                            total.intValue = totalNb
                                        }
                                    )
                                }
                            },
                            settingText = stringResource(R.string.button_findCovers),
                            toolTipText = stringResource(R.string.txt_coversTooltip)
                        )
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
                                                handleError(
                                                    context,
                                                    "Failed to remove some covers."
                                                )
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
                            failedCovers.sorted().forEach { cover ->
                                item {
                                    Text(text = cover, style = MyTypography.bodyMedium)
                                }
                            }
                        }
                    }
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

@Composable
fun ToolTipRow(
    onSettingClick: () -> Unit,
    settingText: String,
    toolTipText: String
) {
    val showTooltip = remember { mutableStateOf(false) }
    // to avoid frequent "double-click" issues
    val lastDismissTime = remember { mutableLongStateOf(0L) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HEIGHT.dp)
            .clickable { onSettingClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        // Main text
        Text(
            modifier = Modifier.padding(start = OFFSET.dp),
            text = settingText,
            style = MyTypography.bodyLarge
        )

        // Info icon
        Box(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(R.string.desc_tooltip),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        val now = SystemClock.uptimeMillis()
                        val guardMs = 300L
                        if (now - lastDismissTime.longValue > guardMs) {
                            showTooltip.value = !showTooltip.value
                        }
                    }
            )

            if (showTooltip.value) {
                Popup(
                    alignment = Alignment.BottomEnd,
                    offset = IntOffset(0, -100),
                    onDismissRequest = {
                        showTooltip.value = false
                        lastDismissTime.longValue = SystemClock.uptimeMillis()
                    }
                ) {
                    val bubbleColor = MaterialTheme.colorScheme.outline
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Tooltip bubble
                        Box(
                            modifier = Modifier
                                .background(
                                    color = bubbleColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = toolTipText,
                                style = MyTypography.bodyLarge,
                                color = MaterialTheme.colorScheme.inversePrimary
                            )
                        }

                        // Triangle pointer
                        Box(
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Canvas(modifier = Modifier.size(16.dp)) {
                                val path = Path().apply {
                                    moveTo(size.width / 2, size.height)
                                    lineTo(0f, 0f)
                                    lineTo(size.width, 0f)
                                    close()
                                }
                                drawPath(path = path, color = bubbleColor)
                            }
                        }
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