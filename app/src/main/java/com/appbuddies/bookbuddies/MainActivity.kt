package com.appbuddies.bookbuddies

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appbuddies.bookbuddies.errors.FileLoggingTree
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.appbuddies.bookbuddies.navigation.NavigationActions
import com.appbuddies.bookbuddies.navigation.Route
import com.appbuddies.bookbuddies.ui.settings.Settings
import com.appbuddies.bookbuddies.ui.home.HomeScreen
import com.appbuddies.bookbuddies.ui.theme.BookBuddiesTheme
import com.appbuddies.bookbuddies.datastore.BookRepository
import com.appbuddies.bookbuddies.datastore.CalendarRepository
import com.appbuddies.bookbuddies.ui.book.BookCreate
import com.appbuddies.bookbuddies.ui.book.BookEdit
import com.appbuddies.bookbuddies.ui.book.BookView
import com.appbuddies.bookbuddies.ui.book.EnterISBN
import com.appbuddies.bookbuddies.ui.book.ScanISBN
import com.appbuddies.bookbuddies.ui.event.EventCreate
import com.appbuddies.bookbuddies.ui.event.EventEdit
import com.appbuddies.bookbuddies.ui.event.EventView
import com.appbuddies.bookbuddies.ui.home.CalendarScreen
import com.appbuddies.bookbuddies.ui.settings.BehindTheScenes
import com.appbuddies.bookbuddies.viewModels.BookViewModel
import com.appbuddies.bookbuddies.viewModels.BookViewModelFactory
import com.appbuddies.bookbuddies.viewModels.CalendarViewModel
import com.appbuddies.bookbuddies.viewModels.CalendarViewModelFactory
import com.appbuddies.bookbuddies.viewModels.DataViewModel
import timber.log.Timber
import java.io.File

class MainActivity : ComponentActivity() {
    private val pendingEventID = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val logFile = File(filesDir, "log.txt")
        Timber.plant(FileLoggingTree(logFile))
        Timber.i("---------------- App started ----------------")

        // create notifications channel
        val channel = NotificationChannel("bookbuddies_events", "Event Reminders", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Calendar events reminders" }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        // ask permission for notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        intent.getStringExtra("eventID")?.let { pendingEventID.value = it }

        setContent {
            val dataVM: DataViewModel = viewModel()
            val currentTheme by dataVM.currentTheme.collectAsState()

            val bookRepository = BookRepository(LocalContext.current)
            val bookVM: BookViewModel = viewModel(
                factory = BookViewModelFactory(bookRepository)
            )

            val calendarRepository = CalendarRepository(LocalContext.current)
            val calendarVM: CalendarViewModel = viewModel(
                factory = CalendarViewModelFactory(calendarRepository, application)
            )

            BookBuddiesTheme(themeChoice = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val navigationActions = NavigationActions(navController)

                    // navigate when notification is tapped
                    val eventID = pendingEventID.value
                    LaunchedEffect(eventID) {
                        if (eventID != null) {
                            navigationActions.navigateTo("${Route.EVENT}/$eventID")
                            pendingEventID.value = null
                        }
                    }

                    NavHost(navController, Route.HOME) {
                        // Main screens
                        composable(Route.HOME) {
                            HomeScreen(bookVM, navigationActions)
                            Timber.tag("Compose").d("Successfully composed screen Home")
                        }
                        composable(Route.CALENDAR) {
                            CalendarScreen(bookVM, calendarVM, navigationActions)
                            Timber.tag("Compose").d("Successfully composed screen Calendar")
                        }
                        composable(Route.SETTINGS) {
                            Settings(dataVM, bookVM, navigationActions)
                            Timber.tag("Compose").d("Successfully composed screen Settings")
                        }
                        composable(Route.BTS) {
                            BehindTheScenes(navigationActions)
                            Timber.tag("Compose").d("Successfully composed screen BehindTheScenes")
                        }

                        // viewing and editing books
                        composable(
                            route = "${Route.BOOK}/{bookID}",
                            arguments = listOf(navArgument("bookID") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val bookID = backStackEntry.arguments?.getString("bookID") ?: return@composable
                            BookView(bookID, bookVM, navigationActions)
                            Timber.tag("Compose").d("Successfully composed screen BookView")
                        }
                        composable(
                            route = "${Route.BOOK_CREATE}?isbn={isbn}",
                            arguments = listOf(navArgument("isbn") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val isbn = backStackEntry.arguments?.getString("isbn")
                            BookCreate(isbn, bookVM, dataVM, navigationActions)
                            Timber.tag("Compose").d("Successfully composed screen BookCreate")
                        }
                        composable(
                            route = "${Route.BOOK_EDIT}/{bookID}",
                            arguments = listOf(navArgument("bookID") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val bookID = backStackEntry.arguments?.getString("bookID") ?: return@composable
                            BookEdit(bookID, bookVM, dataVM, navigationActions)
                            Timber.tag("Compose").d("Successfully composed screen BookEdit")
                        }

                        // automatic book adding
                        composable(Route.SCAN_ISBN) {
                            ScanISBN(navigationActions)
                            Timber.tag("Compose").d("Successfully composed screen ScanISBN")
                        }
                        composable(Route.ENTER_ISBN) {
                            EnterISBN(navigationActions)
                            Timber.tag("Compose").d("Successfully composed screen EnterISBN")
                        }

                        // calendar events
                        composable(
                            route = "${Route.EVENT}/{eventID}",
                            arguments = listOf(navArgument("eventID") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val eventID = backStackEntry.arguments?.getString("eventID") ?: return@composable
                            EventView(eventID, calendarVM, navigationActions)
                            Timber.tag("Compose").d("Successfully composed screen EventView")
                        }
                        composable(
                            route = "${Route.EVENT_CREATE}/{dateStart}",
                            arguments = listOf(navArgument("dateStart") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val dateStart = backStackEntry.arguments?.getLong("dateStart") ?: return@composable
                            EventCreate(dateStart, calendarVM, dataVM, navigationActions)
                            Timber.tag("Compose").d("Successfully composed screen EventCreate")
                        }
                        composable(
                            route = "${Route.EVENT_EDIT}/{eventID}",
                            arguments = listOf(navArgument("eventID") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val eventID = backStackEntry.arguments?.getString("eventID") ?: return@composable
                            EventEdit(eventID, calendarVM, dataVM, navigationActions)
                            Timber.tag("Compose").d("Successfully composed screen EventEdit")
                        }
                    }
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("eventID")?.let { pendingEventID.value = it }
    }
}
