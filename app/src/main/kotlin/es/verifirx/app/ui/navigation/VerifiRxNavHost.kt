package es.verifirx.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import es.verifirx.app.di.ServiceLocator
import es.verifirx.app.ui.capture.CaptureScreen
import es.verifirx.app.ui.capture.CaptureViewModel
import es.verifirx.app.ui.history.HistoryScreen
import es.verifirx.app.ui.history.HistoryViewModel
import es.verifirx.app.ui.home.HomeScreen
import es.verifirx.app.ui.home.HomeViewModel
import es.verifirx.app.ui.results.ResultsScreen
import es.verifirx.app.ui.results.ResultsViewModel

private object Routes {
    const val HOME = "home"
    const val CAPTURE = "capture"
    const val HISTORY = "history"
    const val RESULTS = "results/{sessionId}"
    fun results(sessionId: String) = "results/$sessionId"
}

@Composable
fun VerifiRxNavHost(services: ServiceLocator, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val viewModel: HomeViewModel = viewModel(
                factory = viewModelFactory { initializer { HomeViewModel(services.sessionRepository) } },
            )
            HomeScreen(
                viewModel = viewModel,
                onNewVerification = { navController.navigate(Routes.CAPTURE) },
                onOpenSession = { id -> navController.navigate(Routes.results(id)) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
            )
        }

        composable(Routes.CAPTURE) {
            val viewModel: CaptureViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { CaptureViewModel(services.documentImageProcessor, services.sessionRepository) }
                },
            )
            CaptureScreen(
                viewModel = viewModel,
                onNavigateToResults = { id ->
                    navController.navigate(Routes.results(id)) {
                        popUpTo(Routes.HOME)
                    }
                },
            )
        }

        composable(Routes.HISTORY) {
            val viewModel: HistoryViewModel = viewModel(
                factory = viewModelFactory { initializer { HistoryViewModel(services.sessionRepository) } },
            )
            HistoryScreen(viewModel = viewModel, onOpenSession = { id -> navController.navigate(Routes.results(id)) })
        }

        composable(Routes.RESULTS) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")
                ?: error("results route requires a sessionId argument")
            val viewModel: ResultsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { ResultsViewModel(sessionId, services.sessionRepository) }
                },
            )
            ResultsScreen(viewModel = viewModel)
        }
    }
}
