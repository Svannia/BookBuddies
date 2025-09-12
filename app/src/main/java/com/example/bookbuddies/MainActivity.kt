package com.example.bookbuddies

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookbuddies.errors.FileLoggingTree
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.navigation.Route
import com.example.bookbuddies.ui.settings.Settings
import com.example.bookbuddies.ui.home.HomeScreen
import com.example.bookbuddies.ui.theme.BookBuddiesTheme
import com.example.bookbuddies.datastore.BookRepository
import com.example.bookbuddies.viewModels.BookViewModel
import com.example.bookbuddies.viewModels.BookViewModelFactory
import com.example.bookbuddies.viewModels.DataViewModel
import timber.log.Timber
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val logFile = File(filesDir, "log.txt")
        Timber.plant(FileLoggingTree(logFile))
        Timber.i("---------------- App started ----------------")

        setContent {
            val dataVM: DataViewModel = viewModel()
            val currentTheme by dataVM.currentTheme.collectAsState()

            val bookRepository = BookRepository(LocalContext.current)
            val bookVM: BookViewModel = viewModel(
                factory = BookViewModelFactory(bookRepository)
            )

            BookBuddiesTheme(themeChoice = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val navigationActions = NavigationActions(navController)

                    NavHost(navController, Route.HOME) {
                        composable(Route.HOME) {
                            HomeScreen(bookVM, navigationActions)
                            Timber.tag("Compose").d("Successfully composed screen Home")
                        }

                        composable(Route.SETTINGS) {
                            Settings(dataVM, bookVM, navigationActions)
                            Timber.tag("Compose").d("Successfully composed screen Settings")
                        }
                    }
                }
            }
        }
    }
}