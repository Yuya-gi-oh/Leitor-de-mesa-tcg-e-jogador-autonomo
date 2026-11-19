package com.yuyagioh.tcgreader.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yuyagioh.tcgreader.app.ui.camera.CameraScreen
import com.yuyagioh.tcgreader.app.ui.game.GameScreen
import com.yuyagioh.tcgreader.app.ui.home.HomeScreen
import com.yuyagioh.tcgreader.app.ui.setup.DeckSetupScreen

object Routes {
    const val HOME = "home"
    const val SETUP_PATTERN = "setup/{gameId}/{difficulty}/{format}"
    const val GAME_PATTERN = "game/{gameId}/{difficulty}/{format}"
    const val CAMERA_PATTERN = "camera/{gameId}"

    fun setup(gameId: String, difficulty: String, format: String) = "setup/$gameId/$difficulty/$format"

    fun game(gameId: String, difficulty: String, format: String) = "game/$gameId/$difficulty/$format"

    fun camera(gameId: String) = "camera/$gameId"
}

@Composable
fun TcgReaderApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartGame = { gameId, difficulty, format ->
                    navController.navigate(Routes.setup(gameId.name, difficulty.name, format.name))
                },
            )
        }
        composable(Routes.SETUP_PATTERN) { entry ->
            val gameId = entry.arguments?.getString("gameId") ?: "MAGIC"
            val difficulty = entry.arguments?.getString("difficulty") ?: "MEDIUM"
            val format = entry.arguments?.getString("format") ?: "STANDARD"
            DeckSetupScreen(
                onStartGame = { navController.navigate(Routes.game(gameId, difficulty, format)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.GAME_PATTERN) { entry ->
            val gameId = entry.arguments?.getString("gameId") ?: "MAGIC"
            GameScreen(
                gameId = gameId,
                onBack = { navController.popBackStack() },
                onOpenCamera = { navController.navigate(Routes.camera(gameId)) },
            )
        }
        composable(Routes.CAMERA_PATTERN) { entry ->
            val gameId = entry.arguments?.getString("gameId") ?: "MAGIC"
            CameraScreen(
                gameId = gameId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
