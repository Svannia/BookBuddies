package com.appbuddies.bookbuddies.ui

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import coil.compose.rememberAsyncImagePainter
import com.appbuddies.bookbuddies.R
import com.appbuddies.bookbuddies.data.DateFormat
import com.appbuddies.bookbuddies.datastore.ThemeChoice
import com.appbuddies.bookbuddies.helpers.displayDate
import com.appbuddies.bookbuddies.navigation.BOTTOM_DESTINATIONS
import com.appbuddies.bookbuddies.navigation.BURGER_DESTINATIONS
import com.appbuddies.bookbuddies.navigation.NavigationActions
import com.appbuddies.bookbuddies.ui.theme.MyTypography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * This creates the layout for a "primary screen".
 * It mainly contains a body and a larger top bar in a different colour.
 * There is a burger menu in the top bar that opens on the left-side.
 *
 * @param navigationActions to handle screen navigation
 * @param title display in the top bar
 * @param navigationIndex indicates which primary screen is currently selected (for bottom navigation bar)
 * @param addPopUp mutable boolean that triggers the display of the add (book or event) popup
 * @param topBarIcons composable for icons on the right-side of the top bar
 * @param content screen body
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimaryScreen(
    navigationActions: NavigationActions,
    title: String,
    navigationIndex: Int,
    addPopUp: MutableState<Boolean>,
    topBarIcons: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        modifier = Modifier.fillMaxSize(),
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.requiredWidth(200.dp)
            ) {
                Spacer(modifier = Modifier.size(32.dp))
                BURGER_DESTINATIONS.forEach { destination ->
                    NavigationDrawerItem(
                        label = { Text(text = stringResource(destination.text), style = MyTypography.bodyLarge) },
                        selected = false,
                        onClick = {
                            navigationActions.navigateTo(destination.route)
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                modifier = Modifier.size(22.dp),
                                painter = painterResource(destination.icon),
                                contentDescription = stringResource(R.string.desc_dstIcon)
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Box {
                    CenterAlignedTopAppBar(
                        title = { Text(text = title, style = MyTypography.titleMedium) },
                        navigationIcon = { BurgerMenu(scope, drawerState) },
                        actions = { topBarIcons() },
                    )
                    HorizontalDivider(
                        modifier = Modifier.align(Alignment.BottomStart),
                        thickness = 3.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            bottomBar = {
                BottomNavBar(navigationActions, navigationIndex) {
                    addPopUp.value = true
                }
            },
            content = { padding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    content(padding)
                }
            }
        )
    }
}

/**
 * This creates the layout for a "secondary screen".
 * It mainly contains a body and an invisible top bar with an optional title and a GoBack button.
 *
 * @param title display in the top bar (can be empty)
 * @param navigationActions to handle screen navigation
 * @param navExtraActions optional extra block to run when navigating back (e.g navigating back from CreateAccount screen also signs out)
 * @param topBarIcons extra composable on the right-side of the top bar (optional)
 * @param content screen body
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryScreen(
    title: String,
    navigationActions: NavigationActions,
    navExtraActions: () -> Unit,
    topBarIcons: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box {
                CenterAlignedTopAppBar(
                    title = { Text(text = title, style = MyTypography.titleMedium)},
                    navigationIcon = {
                        GoBackButton(navigationActions, navExtraActions)
                    },
                    actions = {
                        Row(
                            modifier = Modifier.fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            topBarIcons()
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.background)
                )}
        },
        content = { content(it) }
    )
}

/**
 * Bottom navigation bar displayed on the primary screens. The buttons in the bar itself are for navigation between primary screens.
 * There is a middle floating button for adding books.
 *
 * @param navigationActions for navigating between screens
 * @param navigationIndex to indicate which primary screen is currently selected
 * @param onAddClick block that runs when clicking the middle floating button (adding book or adding event)
 */
@Composable
fun BottomNavBar(
    navigationActions: NavigationActions,
    navigationIndex: Int,
    onAddClick: () -> Unit
) {
    val destinations = BOTTOM_DESTINATIONS
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(navigationIndex) }

    val barColour = MaterialTheme.colorScheme.surfaceContainer
    val iconColour = MaterialTheme.colorScheme.inversePrimary
    val backgroundColour = MaterialTheme.colorScheme.background

    Box {
        NavigationBar(
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(60.dp),
            containerColor = barColour
        ) {
            destinations.forEachIndexed { index, destination ->

                // divide the list in 2 and leave space in the middle for floating button
                if (index == destinations.size / 2) {
                    Spacer(modifier = Modifier.weight(0.2f))
                }

                NavigationBarItem(
                    selected = selectedItemIndex == index,
                    onClick = {
                        navigationActions.navigateTo(destination.route)
                        selectedItemIndex = index
                    },
                    icon = {
                        Icon(
                            modifier = Modifier.size(28.dp),
                            painter = painterResource(destination.icon),
                            contentDescription = stringResource(destination.text),
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = iconColour,
                        unselectedIconColor = iconColour.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ),
                    alwaysShowLabel = true
                )
            }
        }

        // floating Add button
        Box (
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-35).dp)
                .size(70.dp),
            contentAlignment = Alignment.Center
        ) {
            // half-circle cutout around the button
            Canvas(
                modifier = Modifier.matchParentSize()
            ) {
                val diameter = size.height
                drawArc(
                    color = backgroundColour,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true,
                    size = Size(diameter, diameter)
                )
            }
            FloatingActionButton(
                modifier = Modifier.size(58.dp),
                onClick = { onAddClick() },
                shape = CircleShape,
                containerColor = barColour,
                contentColor = iconColour,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.plus),
                    contentDescription = stringResource(R.string.dst_addBook),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * Implements a fast scroll bar for a LazyColumn.
 * The thumb for the scroll bar only appears when scrolling through the LazyColumn, and disappears after 3sec of not scrolling or releasing the thumb.
 * The thumb auto-scrolls on the side if the user uses normal scrolling on the LazyColumn.
 * When dragging the thumb to go through the LazyColumn faster, a bubble appears next to the thumb and shows the content of the current closest sticky header.
 * The thumb is visually wider when being dragged, but the width of the touch area for the thumb stays constant.
 *
 * @param minThumbWidth width of the thumb when not dragged
 * @param maxThumbWidth width of the thumb when being dragged
 * @param thumbHeight height of the thumb (constant)
 * @param bubbleWidth fixed width of the sticky header bubble. Its height wraps around the length of the text.
 * @param listState state of the List that is being scrolled through.
 * @param headerResolver lambda that receives the index of the closest LazyColumn item and uses it to return its sticky header parent
 * @param listContent content of the LazyColumn
 * @return
 */
@SuppressLint("FrequentlyChangingValue")
@Composable
fun FastScroll(
    minThumbWidth: Int,
    maxThumbWidth: Int,
    thumbHeight: Int,
    bubbleWidth: Int,
    listState: LazyListState,
    headerResolver: (Int) -> String?,
    listContent: LazyListScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val containerHeight = remember { mutableFloatStateOf(0f) }

    val thumbAlpha = remember { Animatable(1f) }
    val isDragging = remember { mutableStateOf(false) }
    val thumbOffset = remember { mutableFloatStateOf(0f) }
    val thumbWidth = remember { mutableIntStateOf(minThumbWidth) }

    // capture the current scrolling/dragging state to fade in/out the thumb
    LaunchedEffect(listState, isDragging.value) {
        snapshotFlow { listState.isScrollInProgress || isDragging.value }
            .collect { active ->
                // User started scrolling
                if (active) {
                    thumbAlpha.snapTo(1f)
                }
                // User stopped scrolling and dragging
                else {
                    delay(3000L)
                    thumbAlpha.animateTo(0f, tween(1000))
                }
            }
    }

    val currentHeader = remember { mutableStateOf<String?>(null) }
    // capture the sticky header currently at the top
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (!isDragging.value) currentHeader.value = headerResolver(index)
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // contents of the LazyColumn
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            listContent()
        }

        // whole vertical drag area (invisible)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .width(maxThumbWidth.dp)
                .padding(end = 8.dp)
                .alpha(thumbAlpha.value)
                .onGloballyPositioned { coordinates ->
                    containerHeight.floatValue = coordinates.size.height.toFloat()
                }
        ) {
            // draggable thumb (larger touch area than visible to it's easier to use)
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, thumbOffset.floatValue.roundToInt()) }
                    .fillMaxWidth()
                    .height(thumbHeight.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging.value = true
                                thumbWidth.intValue = maxThumbWidth
                            },
                            onDragEnd = {
                                isDragging.value = false
                                thumbWidth.intValue = minThumbWidth
                            },
                            onDragCancel = {
                                isDragging.value = false
                                thumbWidth.intValue = minThumbWidth
                            },
                            onDrag = { change, offset ->
                                change.consume()

                                // change the position of the thumb on the screen
                                val maxThumbOffset =
                                    containerHeight.floatValue - thumbHeight.dp.toPx()
                                thumbOffset.floatValue = (thumbOffset.floatValue + offset.y)
                                    .coerceIn(0f, maxThumbOffset)

                                // sync LazyColum position with dragged thumb
                                val scrollFraction =
                                    (thumbOffset.floatValue / maxThumbOffset).coerceIn(0f, 1f)
                                val totalItems = listState.layoutInfo.totalItemsCount
                                if (totalItems > 0) {
                                    val targetIndex = (scrollFraction * (totalItems - 1)).toInt()
                                    coroutineScope.launch {
                                        listState.scrollToItem(targetIndex)
                                    }

                                    currentHeader.value = headerResolver(targetIndex)
                                }
                            }
                        )
                    }
            ) {
                // Visible part of the thumb
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width((thumbWidth.intValue).dp)
                        .align(Alignment.CenterEnd)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        )
                )
            }
        }

        // Text bubble that follows the thumb
        if (isDragging.value) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            -(8 + maxThumbWidth + 20).dp.roundToPx(),
                            thumbOffset.floatValue.roundToInt()
                        )
                    }
                    .align(Alignment.TopEnd)
                    .width(bubbleWidth.dp)
                    .wrapContentHeight()
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentHeader.value ?: "",
                    style = MyTypography.bodyMedium.copy(textAlign = TextAlign.Center),
                )
            }
        }

        // when scrolling normally, automatically adjust the thumb's height
        if (!isDragging.value && containerHeight.floatValue > 0f && listState.layoutInfo.totalItemsCount > 0) {
            // compute the current position in the list
            val firstItemSize = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 1f
            val visibleOffset = listState.firstVisibleItemScrollOffset / firstItemSize
            val scrollFraction = ((listState.firstVisibleItemIndex + visibleOffset)
                    / listState.layoutInfo.totalItemsCount.toFloat()).coerceIn(0f, 1f)

            // adjust thumb height
            val density = LocalDensity.current
            val thumbHeightPx = with(density) { thumbHeight.dp.toPx() }
            val maxThumbOffset = containerHeight.floatValue - thumbHeightPx
            thumbOffset.floatValue = scrollFraction * maxThumbOffset
        }
    }
}

/**
 * A simple plain screen with a rotating loading animation.
 */
@Composable
fun LoadingPage() {
    // ensures that the user cannot go back while on the loading page
    BackHandler {}
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingAnimation(100f, 10f)
    }
}

/**
 * A rotating animation to be used for when waiting for information to load/change.
 *
 * @param size diameter of the loading circle
 * @param strokeWidth width of the circle
 */
@Composable
fun LoadingAnimation(size: Float, strokeWidth: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = stringResource(R.string.desc_loading))
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ), label = stringResource(R.string.desc_loading)
    )

    val primaryColour = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.size((size).dp)) {
        drawArc(
            color = primaryColour,
            startAngle = angle,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

/**
 * Column with a small loading animation that can be used as a screen content.
 *
 * @param paddingValues to be used in the main column
 */
@Composable
fun MiniLoading(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.size(16.dp))
        LoadingAnimation(30f, 10f)
    }
}

/**
 * A plain screen with a progress bar that gradually fills up, along with number of processed items over total, and progression percentage.
 *
 * @param processed number of items that have been processed
 * @param total total number of items to process
 */
@Composable
fun ProgressBar(processed: Int, total: Int) {
    // ensures that the user cannot go back while progress is ongoing
    BackHandler {}

    val progress = (processed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val percentage = (progress * 100).toInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // informative text above the progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // number of processed items over total
                Text(
                    text = stringResource(R.string.txt_coversProgress, processed, total),
                    style = MyTypography.bodyLarge
                )
                // progress percentage
                Text(
                    text = "$percentage%",
                    style = MyTypography.bodyLarge
                )
            }
            Spacer(Modifier.height(12.dp))

            // progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(50))
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.inversePrimary,
                        shape = RoundedCornerShape(50)
                    )
                    .background(MaterialTheme.colorScheme.outline)
            ) {
                // bar fill that progressively fills its frame bar
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

/**
 * Rewritten basic TextField composable for constant design throughout the app.
 * Always use this instead of the normal TextField.
 *
 * @param value text passed by the user in the text field
 * @param onValueChange block that runs with the new input value when it is edited
 * @param icon display at the beginning of the text field (use a negative int for no icon)
 * @param iconColour colour applied to the icon, black/white by default (light/dark mode)
 * @param placeHolder text displayed in the empty text field
 * @param singleLine whether or not the value of the text field can contain line breaks
 * @param maxLength maximum amount of characters allowed in the text field
 * @param autoCap whether or not to activate the AutoCap on the keyboard when starting to type. True by default
 * @param focusRequester optional FocusRequester when the TextField needs to be manually put into focus
 * @param onFocusedChanged needed if there is a FocusRequester: block that runs when focus is changed
 * @param showMaxChara whether or not to show supporting text with the max amount of character. True by default
 * @param width width of the TextField
 * @param height optional height for the TextField, usually used for writing big blocks of text
 * @param keyboardActions optional overriding of default keyboard actions
 * @param keyboardOptions optional overriding of default keyboard options
 */
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    icon: Int,
    iconColour: Color = MaterialTheme.colorScheme.inversePrimary,
    placeHolder: String,
    singleLine: Boolean,
    maxLength: Int,
    autoCap: Boolean = true,
    focusRequester: FocusRequester = FocusRequester.Default,
    onFocusedChanged: (FocusState) -> Unit = {},
    showMaxChara: Boolean = true,
    width: Dp,
    height: Dp? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val showingMaxChara = remember { mutableStateOf(false) }
    TextField(
        modifier = if (singleLine) {
            Modifier
                .width(width)
                .padding(0.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusedChanged(it) }
        } else {
            Modifier
                .width(width)
                .height(height!!)
                .padding(0.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusedChanged(it) }
        },
        value = value,
        onValueChange = {
            if (it.length <= maxLength) {
                onValueChange(it)
            }
            showingMaxChara.value = it.length >= maxLength
        },
        textStyle = MyTypography.bodyLarge,
        prefix = {
            if (icon >= 0) {
                Row{
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = stringResource(R.string.desc_textFieldIcon),
                        tint = iconColour,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                }
            }
        },
        placeholder = {
            Text(text = placeHolder, style = MyTypography.bodySmall)
        },
        singleLine = singleLine,
        supportingText = {
            if (showMaxChara && showingMaxChara.value) {
                Text(text = stringResource(R.string.field_maxChar, maxLength), style = MyTypography.labelSmall)
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary
        ),
        keyboardActions = keyboardActions,
        keyboardOptions = keyboardOptions.copy(
            capitalization = if (autoCap) KeyboardCapitalization.Sentences
            else KeyboardCapitalization.None
        )
    )
}

/**
 * Input field specifically designed to edit book information.
 *
 * @param value inside the input field
 * @param icon to display at the beginning of the input field
 * @param width of the input field
 * @param maxLength max characters that can be entered in this input field
 * @param singleLine whether the input field is single line or multi line
 * @param canExpand whether or not suggestions should be shown when typing
 * @param suggestions function that provides a list of suggestions based on the current input
 * @param onFocusEvent callback for focus events on the input field
 * @param onValueChange callback for when the input field value changes
 */
@Composable
fun InputField(
    value: String,
    icon: Int,
    width: Int,
    maxLength: Int,
    singleLine: Boolean,
    canExpand: Boolean,
    suggestions: ((String) -> List<String>),
    onFocusEvent: (FocusState) -> Unit,
    onValueChange: (String) -> Unit
) {
    val showMaxChar = remember { mutableStateOf(false) }
    val expanded = remember { mutableStateOf(false) }
    val suggestions by remember(value) { mutableStateOf(suggestions(value)) }

    var textFieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length)))}
    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue = TextFieldValue(value, TextRange(value.length))
        }
    }

    var userTyping by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.width(width.dp))
    {
        TextField(
            modifier = if (singleLine) Modifier
                .padding(0.dp)
                .onFocusEvent { onFocusEvent(it) }
            else Modifier
                .padding(0.dp)
                .height(400.dp)
                .onFocusEvent { onFocusEvent(it) },
            value = textFieldValue,
            onValueChange = {
                if (it.text.length <= maxLength) {
                    textFieldValue = it
                    onValueChange(it.text)
                }
                showMaxChar.value = it.text.length >= maxLength
                if (userTyping) expanded.value = true
            },
            textStyle = MyTypography.bodyLarge,
            prefix = {
                Row{
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = stringResource(R.string.desc_textFieldIcon),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                }
            },
            placeholder = {
                Text(text = stringResource(R.string.txt_inputFieldPlaceholder), style = MyTypography.bodySmall)
            },
            singleLine = singleLine,
            supportingText = {
                if (showMaxChar.value) {
                    Text(
                        text = stringResource(R.string.txt_maxChar, maxLength.toString()),
                        style = MyTypography.labelSmall
                    )
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary
            )
        )

        if (canExpand && expanded.value && suggestions.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .heightIn(max = 200.dp)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(4.dp)
                    )
            ) {
                LazyColumn {
                    items(suggestions.size) { index ->
                        val suggestion = suggestions[index]
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    userTyping = false
                                    // fill textField with chosen suggestion and jump cursor to end
                                    textFieldValue = TextFieldValue(
                                        suggestion,
                                        TextRange(suggestion.length)
                                    )
                                    onValueChange(suggestion)
                                    expanded.value = false

                                    // reset userTyping flag (to avoid having the suggestion re-triggering expanded = true)
                                    scope.launch {
                                        delay(100)
                                        userTyping = true
                                    }
                                }
                                .padding(8.dp),
                            text = suggestion,
                            style = MyTypography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section title and input field for a simple, single-line field.
 *
 * @param title of the section
 * @param value inside the input field
 * @param fieldWidth max width of the input field
 * @param icon to display at the beginning of the input field
 * @param maxLength max characters that can be entered in this input field
 * @param singleLine whether the input field is single line or multi line (default: true)
 * @param showSuggestions whether to show suggestions when typing
 * @param suggestions function that provides a list of suggestions based on the current input
 * @param onFocusEvent callback for focus events on the input field
 * @param onValueChange callback for when the input field value changes
 * @param extraActions optional content to display at the end of the input field
 */
@Composable
fun SingleInputField(
    title: String,
    value: String,
    fieldWidth: Int,
    icon: Int,
    maxLength: Int,
    singleLine: Boolean = true,
    showSuggestions: Boolean,
    suggestions: ((String) -> List<String>),
    onFocusEvent: (FocusState) -> Unit,
    onValueChange: (String) -> Unit,
    extraActions: (@Composable RowScope.() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = title, style = MyTypography.titleSmall.copy(textAlign = TextAlign.Start))
        Row(modifier = Modifier.padding(start = 16.dp)
        ) {
            InputField(
                value, icon, fieldWidth, maxLength, singleLine, showSuggestions,
                { suggestions(it) },
                { onFocusEvent(it) }
            ) { onValueChange(it) }
            if (extraActions != null) extraActions()
        }
    }
}

/**
 * Input field for selecting a date. The input field itself is not editable, but clicking on it triggers a date picker dialog.
 * There is a suffix button to remove the selected date.
 *
 * @param date current date inside the input field (in milliseconds since epoch)
 * @param onClear function that runs when removing the date
 * @param onClick function that runs when selecting the input field (should open a date picker)
 */
@Composable
fun DateInput(
    date: Long,
    onClear: () -> Unit,
    onClick: () -> Unit
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp)
    ) {
        TextField(
            modifier = Modifier
                .padding(0.dp)
                .clickable { onClick() },
            value = if (date > 0L) displayDate(date, DateFormat.NUMBERED)
            else "",
            onValueChange = {},
            enabled = false,
            textStyle = MyTypography.bodyLarge,
            leadingIcon = {
                Row{
                    IconButton(onClick = { onClick() }) {
                        Icon(
                            painter = painterResource(R.drawable.calendar),
                            contentDescription = stringResource(R.string.desc_textFieldIcon),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.size(16.dp))
                }
            },
            trailingIcon = {
                if (date > 0L) {
                    IconButton(onClick = { onClear() }) {
                        Icon(
                            painter = painterResource(R.drawable.cancel),
                            contentDescription = stringResource(R.string.desc_clearDate),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            placeholder = {
                Text(text = stringResource(R.string.field_date), style = MyTypography.bodySmall)
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
}

/**
 * Creates a dialog window that can pop and be dismissed; its contents can be anything.
 * Warning: always call this function after all other composable elements in code, so that it appears on top of the screen.
 *
 * @param visible whether or not this window should be visible
 * @param padding padding inside the window, default is 16.dp
 * @param content content of the dialog window
 * @param bottomButtons whether or not the window should have 2 buttons in a row at the bottom. If true, the rest of the parameters should be filled out.
 *        If only any one button's content and actions are given, it will be displayed on the left
 * @param leftButtonContent contents of the button on the far left (text or icon for example)
 * @param leftButtonOnClick block that runs when clicking the left button (don't forget to dismiss the window if it's expected)
 * @param rightButtonContent contents of the button on the far right (text or icon for example)
 * @param rightButtonOnClick block that runs when clicking the right button (don't forget to dismiss the window if it's expected)
 */
@Composable
fun CustomContentDialogWindow(
    visible: MutableState<Boolean>,
    padding: Int = 16,
    content: @Composable (ColumnScope.() -> Unit),
    bottomButtons: Boolean,
    leftButtonContent: @Composable (RowScope.() -> Unit)? = null,
    leftButtonOnClick: (() -> Unit)? = null,
    rightButtonContent: @Composable (RowScope.() -> Unit)? = null,
    rightButtonOnClick: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = { visible.value = false }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.outline,
            tonalElevation = 0.dp,
            modifier = Modifier.padding(padding.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                content()
                if (bottomButtons) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (leftButtonContent != null && leftButtonOnClick != null) {
                            Button(
                                modifier = Modifier.background(color = Color.Transparent, shape = RoundedCornerShape(50)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                ),
                                onClick = { leftButtonOnClick() }
                            ) { leftButtonContent() }
                        }
                        if (rightButtonContent != null && rightButtonOnClick != null) {
                            Button(
                                modifier = Modifier.background(color = Color.Transparent, shape = RoundedCornerShape(50)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                ),
                                onClick = { rightButtonOnClick() }
                            ) { rightButtonContent() }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Creates a Dialog Window with a DatePicker (old style since new style is made too large).
 * The colours are set to still fit with the app theme and light/dark modes.
 *
 * @param context for the DatePickerDialog
 * @param themeChoice current app theme, to set the dialog's colours
 * @param visible whether or not the dialog is visible
 * @param dateMillis date selected at the moment of opening the dialog
 * @param onDateSelected block that runs when a date is selected, returning the date in milliseconds
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePicker(
    context: Context,
    themeChoice: ThemeChoice,
    visible: MutableState<Boolean>,
    dateMillis: Long,
    onDateSelected: (Long) -> Unit
) {
    if (visible.value) {
        val calendar = Calendar.getInstance().apply {
            if (dateMillis > 0L) timeInMillis = dateMillis
        }
        val datePickerStyle = when (themeChoice) {
            ThemeChoice.LIGHT -> R.style.MyDatePickerThemeLight
            ThemeChoice.DARK -> R.style.MyDatePickerThemeDark
            ThemeChoice.SYSTEM_DEFAULT -> R.style.MyDatePickerTheme
        }

        val dialog = DatePickerDialog(
            context,
            datePickerStyle,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth, 0, 0, 0)
                onDateSelected(cal.timeInMillis)
                visible.value = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        dialog.setOnDismissListener { visible.value = false }
        dialog.show()
    }
}

/**
 * Tooltip icon with, if visible, a bubble chat-design popup attached next, with a tail pointing to the icon.
 *
 * @param showTooltip whether or not the popup is visible
 * @param onIconClick block that runs when tapping the tooltip icon
 * @param extraOnDismiss extra actions that need to be taken when dismissing the popup
 * @param toolTipText text inside the popup
 */
@Composable
fun Tooltip(showTooltip: MutableState<Boolean>, onIconClick: () -> Unit, extraOnDismiss: () -> Unit, toolTipText: String) {
    // Info icon
    Icon(
        imageVector = Icons.Default.Info,
        contentDescription = stringResource(R.string.desc_tooltip),
        tint = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .size(20.dp)
            .clickable { onIconClick() }
    )

    // Show tooltip popup
    if (showTooltip.value) {
        Popup(
            alignment = Alignment.BottomEnd,
            offset = IntOffset(0, -100),
            onDismissRequest = {
                showTooltip.value = false
                extraOnDismiss()
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

                // Triangle pointer (bubble chat tail)
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

/**
 * Element that creates an icon button that opens a drop-down menu of options when pressed.
 *
 * @param icon identifier for the icon to be used as the IconButton
 * @param options non-exhaustive number of pairs.
 * Each pair contains a string for the name of the action appearing in the drop-down menu,
 * and a block to run when that button is pressed.
 */
@Composable
fun OptionsMenu(icon: Int, vararg options: Pair<String, () -> Unit>) {
    val menuExpanded = remember { mutableStateOf(false) }

    Row{
        IconButton(
            onClick = { menuExpanded.value = !menuExpanded.value }
        ) {
            Icon(
                painter = painterResource(icon),
                modifier = Modifier.size(28.dp),
                contentDescription = stringResource(R.string.desc_options)
            )
        }
        DropdownMenu(
            expanded = menuExpanded.value,
            onDismissRequest = { menuExpanded.value = false }
        ) {
            for ((text, block) in options) {
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        menuExpanded.value = false
                        block()
                    }
                )
            }
        }
    }
}

/**
 * Icon that handles the burger menu.
 *
 * @param scope needed to launch a coroutine to open the burger menu
 * @param drawerState value that determines if the burger menu is opened or closed
 */
@Composable
private fun BurgerMenu(scope: CoroutineScope, drawerState: DrawerState) {
    IconButton(
        onClick = { scope.launch { drawerState.open() }}
    ) {
        Icon(
            painter = painterResource(R.drawable.burger_menu),
            contentDescription = stringResource(R.string.desc_burgerMenu),
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * A button used to navigate back in the screens navigation history.
 *
 * @param navigationActions to handle screen navigation
 * @param navExtraActions optional extra block to run when navigating back (e.g navigating back from CreateAccount screen also signs out)
 * @param route optional route to navigate to instead of navigating back
 */
@Composable
fun GoBackButton(navigationActions: NavigationActions, navExtraActions: () -> Unit, route: String ?= null) {
    IconButton(
        onClick = {
            if (route != null) navigationActions.navigateTo(route)
            else navigationActions.goBack()
            navExtraActions()
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.go_back),
            contentDescription = stringResource(R.string.desc_goBack)
        )
    }
}

/**
 * Sizes a cover image to a fix height, keeping original proportions.
 *
 * @param height height of the image
 * @param picture text linking to the cover image's storage location
 */
@Composable
fun CoverImage(height: Dp, picture: String?, onClick: (() -> Unit) ?= null) {
    Box(
        modifier = if (onClick != null) {
            Modifier
                .height(height)
                .width(height * 0.6f)
                .clip(RectangleShape)
                .clickable { onClick() }
        } else {
            Modifier
                .height(height)
                .width(height * 0.6f)
                .clip(RectangleShape)
        }

    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = rememberAsyncImagePainter(
                model = picture ?: R.drawable.default_cover
            ),
            contentDescription = stringResource(R.string.desc_coverImage),
            contentScale = ContentScale.FillHeight
        )
    }
}

/**
 * Sizes a cover image to a fix height, keeping original proportions.
 *
 * @param height height of the image
 * @param picture Uri of the picture
 */
@Composable
fun CoverImage(height: Dp, picture: Uri, onClick: (() -> Unit) ?= null) {
    Box(
        modifier = if (onClick != null) {
            Modifier
                .height(height)
                .width(height * 0.6f)
                .clip(RectangleShape)
                .clickable { onClick() }
        } else {
            Modifier
                .height(height)
                .width(height * 0.6f)
                .clip(RectangleShape)
        }

    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = rememberAsyncImagePainter(picture),
            contentDescription = stringResource(R.string.desc_coverImage),
            contentScale = ContentScale.FillHeight
        )
    }
}

/**
 * Creates a row of 5 star icons that are filled according to the rating value.
 *
 * @param rating rating value from 0 to 5, with half-values
 * @param starSize size of each star icon
 * @param starSpacing spacing between the stars
 */
@Composable
fun RatingStars(rating: Double, starSize: Dp, starSpacing: Dp, emptyStarColor: Color, onRatingChange: ((Double) -> Unit)? = null) {
    val density = LocalDensity.current
    val starSizePx = with(density) { starSize.toPx() }
    val spacingPx = with(density) { starSpacing.toPx() }
    val totalWidth = 5 * starSizePx + 4 * spacingPx

    Row(
        horizontalArrangement = Arrangement.spacedBy(starSpacing),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(
                if (onRatingChange != null) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val newRating = ((offset.x / totalWidth) * 5)
                                    .coerceIn(0.0f, 5.0f)
                                onRatingChange((newRating * 2).roundToInt() / 2.0) // snap to a half-star
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val newRating = ((change.position.x / totalWidth) * 5)
                                    .coerceIn(0.0f, 5.0f)
                                onRatingChange((newRating * 2).roundToInt() / 2.0)
                            }
                        }
                } else Modifier
            )
    ) {
        for (i in 1..5) {
            val fillRatio = when {
                i <= rating -> 1f
                i - rating < 1 -> (rating - floor(rating)).toFloat()
                else -> 0f
            }

            Box(
                modifier = Modifier.size(starSize)
            ) {
                // background outlined star
                Icon(
                    painter = painterResource(R.drawable.star_filled),
                    contentDescription = stringResource(R.string.desc_rating),
                    modifier = Modifier.fillMaxSize(),
                    tint = emptyStarColor
                )

                // star filling
                if (fillRatio > 0f) {
                    Icon(
                        painter = painterResource(R.drawable.star_filled),
                        contentDescription = stringResource(R.string.desc_rating),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                val width = size.width * fillRatio
                                clipRect(right = width) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                }
            }
        }
    }
}

/**
 * Handles a list of options where exactly one option can and must be selected.
 *
 * @param numberChoices number of options in the list
 * @param currentChoice option that is currently selected
 * @param choicesNames list of all the options' names
 * @param onToggle block that runs when a new option is toggled on, with the name of the new option selected
 */
@Composable
fun SingleOptionList(
    numberChoices: Int,
    currentChoice: String,
    choicesNames: List<String>,
    onToggle: (String) -> Unit
) {
    var toggledIndex by remember { mutableIntStateOf(choicesNames.indexOf(currentChoice)) }

    for (i in 0 until numberChoices) {
        ToggleBox(
            isRadio = true,
            boxHeight = 20.dp,
            rowPadding = PaddingValues(),
            rowSpacing = 8.dp,
            optionText = choicesNames[i],
            textStyle = MyTypography.bodyMedium,
            isToggled = toggledIndex == i
        ) {
            toggledIndex = i
            onToggle(choicesNames[i])
        }
    }
}

/**
 * UI element that can be toggled on or off, with its explanatory text.
 * The elements are placed inside a box that can also be tapped to trigger the toggle element.
 *
 * @param isRadio if true, the item is a Radio button. If false it's a Checkbox
 * @param boxHeight height of the clickable box wrapping all the elements
 * @param rowPadding PaddingValues of the Row that organizes the elements
 * @param rowSpacing spacing between the elements inside the Row
 * @param optionText text next to the toggle element
 * @param textStyle chosen from defined styles in MyTypography
 * @param isToggled current state of the toggle element
 * @param onToggle block that runs when triggering the toggle element by tapping it or the parent box
 */
@Composable
fun ToggleBox(
    isRadio: Boolean,
    boxHeight: Dp,
    rowPadding: PaddingValues,
    rowSpacing: Dp,
    optionText: String,
    textStyle: TextStyle,
    isToggled: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(boxHeight)
            .clickable { onToggle() },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(rowPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(rowSpacing)
        ) {
            if (isRadio) {
                RadioButton(
                    modifier = Modifier.size(20.dp),
                    selected = isToggled,
                    onClick = { onToggle() },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                )
            } else {
                Checkbox(
                    modifier = Modifier.size(20.dp),
                    checked = isToggled,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
            }
            Text(text = optionText, style = textStyle)
        }
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
fun ToggleOptions(boxHeight: Int, startOffset: Int, numberChoices: Int, currentChoice: MutableState<String>, choicesNames: List<String>, onToggle: (String) -> Unit) {
    var toggledIndex by remember { mutableIntStateOf(choicesNames.indexOf(currentChoice.value)) }

    for (i in 0 until numberChoices) {
        ToggleBox(
            isRadio = true,
            boxHeight = boxHeight.dp,
            rowPadding = PaddingValues(start = startOffset.dp, end = 14.dp),
            rowSpacing = 16.dp,
            optionText = choicesNames[i],
            textStyle = MyTypography.bodyLarge,
            isToggled = toggledIndex == i
        ) {
            toggledIndex = i
            currentChoice.value = choicesNames[i]
            onToggle(choicesNames[i])
        }
    }
}

/**
 * A clickable row used as a button for the cover's edit options.
 *
 * @param text to be written on the row
 * @param height height of the clickable row
 * @param onClick block that run upon clicking on that row
 */
@Composable
fun RowTextButton(text: String, height: Dp, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text, style = MyTypography.bodyLarge
        )
    }
}

/**
 * Vertical wheel picker UI to select items.
 *
 * @param modifier for the Box containing the wheel picker
 * @param items list of the item's texts to display in the wheel
 * @param selectedIndex currently selected item (in the middle of the wheel)
 * @param onIndexSelected block that runs with the currently selected item when using the wheel
 */
@Composable
fun WheelPicker(
    modifier: Modifier,
    items: List<String>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit
) {
    val itemHeight = 40.dp
    val visibleItems = 5

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0)
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val centerIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex}
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(centerIndex) {
        onIndexSelected(centerIndex.coerceIn(0, items.size - 1))
    }

    Box(
        modifier = modifier.height(itemHeight * visibleItems)
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItems / 2))
        ) {
            itemsIndexed(items) { index, item ->
                val isSelected = index == centerIndex.coerceIn(0, items.size - 1)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .then(
                            if (!isSelected) Modifier.clickable {
                                scope.launch { listState.animateScrollToItem(index) }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        style = MyTypography.bodyLarge.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}
