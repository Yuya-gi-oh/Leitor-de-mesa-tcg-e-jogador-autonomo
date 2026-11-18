package com.yuyagioh.tcgreader.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yuyagioh.tcgreader.app.ui.camera.CameraScreen
import com.yuyagioh.tcgreader.app.ui.game.GameScreen
import com.yuyagioh.tcgreader.app.ui.home.HomeScreen

object Routes {
    const val HOME = "home"
    const val GAME_PATTERN = "game/{gameId}/{difficulty}"
    const val CAMERA_PATTERN = "camera/{gameId}"

    fun game(gameId: String, difficulty: String) = "game/$gameId/$difficulty"

    fun camera(gameId: String) = "camera/$gameId"
}

@Composable
fun TcgReaderApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartGame = { gameId, difficulty ->
                    navController.navigate(Routes.game(gameId.name, difficulty.name))
                },
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
