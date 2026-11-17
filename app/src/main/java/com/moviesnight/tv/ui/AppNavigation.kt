package com.moviesnight.tv.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.moviesnight.tv.ui.detail.DetailScreen
import com.moviesnight.tv.ui.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class DetailRoute(val id: Long)

private const val FADE_MS = 220

/** Home grid → detail. System Back pops the detail screen. */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        enterTransition = { fadeIn(tween(FADE_MS)) },
        exitTransition = { fadeOut(tween(FADE_MS)) },
        popEnterTransition = { fadeIn(tween(FADE_MS)) },
        popExitTransition = { fadeOut(tween(FADE_MS)) },
    ) {
        composable<HomeRoute> {
            HomeScreen(onOpenRecord = { id ->
                navController.navigate(DetailRoute(id)) { launchSingleTop = true }
            })
        }
        composable<DetailRoute> {
            DetailScreen()
        }
    }
}
