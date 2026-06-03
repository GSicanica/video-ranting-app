package com.youtube.rating.android.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.youtube.rating.android.core.AppKeys
import com.youtube.rating.android.ui.screens.BibleHomeScreen
import com.youtube.rating.android.ui.screens.BiblePlannerScreen
import com.youtube.rating.android.ui.screens.BibleStatsScreen
import com.youtube.rating.android.ui.screens.BibleTabsScreen
import com.youtube.rating.android.ui.screens.GospelOfDayScreen
import com.youtube.rating.android.ui.screens.LocalBibleBookScreen
import com.youtube.rating.android.ui.screens.LocalBibleLibraryScreen
import com.youtube.rating.android.ui.screens.MainPsalmScreen
import com.youtube.rating.android.ui.screens.ReaderScreen

fun NavGraphBuilder.bibleNavigationGraph(
    navCoordinator: NavigationCoordinator,
    onOpenLeftDrawer: () -> Unit = {},
    onOpenRightDrawer: () -> Unit = {}
) {
    // Bible Home Screen
    composable(Screen.Bible.route) { backStackEntry ->
        val refreshFlow = backStackEntry.savedStateHandle.getStateFlow(AppKeys.SavedState.Home.PSALM_REFRESH, 0L)
        BibleTabsScreen(
            onOpenPsalm = { psalm -> navCoordinator.navigateToBiblePsalm(psalm) },
            refreshSignal = refreshFlow,
            onOpenLeftDrawer = onOpenLeftDrawer,
            onOpenRightDrawer = onOpenRightDrawer
        )
    }

    // Local Bible Library
    composable(Screen.LocalBible.route) {
        LocalBibleLibraryScreen(
            onOpenBook = { book ->
                navCoordinator.navigateToBibleBook(book)
            },
            onNavigateBack = { navCoordinator.navigateUp() }
        )
    }

    // Bible Planner
    composable(Screen.BiblePlanner.route) {
        BiblePlannerScreen()
    }

    // Training (Bible Home)
    composable(Screen.Training.route) {
        BibleHomeScreen(
            onOpenPsalm = { psalm -> navCoordinator.navigateToBiblePsalm(psalm) },
            onOpenLeftDrawer = onOpenLeftDrawer,
            onOpenRightDrawer = onOpenRightDrawer
        )
    }

    // Bible Stats
    composable(Screen.BibleStats.route) {
        BibleStatsScreen(onBack = { navCoordinator.navigateUp() })
    }

    // Local Bible Book with arguments
    composable(
        route = Screen.LocalBibleBook.route,
        arguments = listOf(navArgument("book") {
            type = NavType.StringType
        })
    ) { backStackEntry ->
        val book = ArgumentParser.parseBookArgument(backStackEntry)
        LocalBibleBookScreen(
            bookId = book,
            onOpenChapter = { chapter ->
                navCoordinator.navigateToBibleChapter(book, chapter)
            },
            onNavigateBack = { navCoordinator.navigateUp() }
        )
    }

    // Bible Psalm with arguments
    composable(
        route = Screen.BiblePsalm.route,
        arguments = listOf(navArgument("psalm") { type = NavType.IntType })
    ) { backStackEntry ->
        val psalm = ArgumentParser.parsePsalmArgument(backStackEntry)
        MainPsalmScreen(initialPsalm = psalm)
    }

    // Unified Reader (Bible / Gospel / Local)
    composable(
        route = Screen.Reader.route,
        arguments = listOf(
            navArgument("type") { type = NavType.StringType },
            navArgument("book") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("chapter") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("lang") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("line") {
                type = NavType.IntType
                defaultValue = -1
            },
            navArgument("date") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val type = backStackEntry.arguments?.getString("type") ?: Screen.Reader.TYPE_BIBLE
        val book = backStackEntry.arguments?.getString("book")
        val chapter = backStackEntry.arguments?.getString("chapter")
        val lang = backStackEntry.arguments?.getString("lang")
        val line = backStackEntry.arguments?.getInt("line") ?: -1
        val date = backStackEntry.arguments?.getString("date")
        ReaderScreen(
            type = type,
            book = book,
            chapter = chapter,
            lang = lang,
            line = if (line >= 0) line else null,
            date = date,
            onDismiss = { navCoordinator.navigateUp() },
            onOpenLeftDrawer = onOpenLeftDrawer,
            onOpenRightDrawer = onOpenRightDrawer,
            gospelContent = { initialDate ->
                GospelOfDayScreen(
                    initialDate = initialDate,
                    onBack = { navCoordinator.navigateUp() }
                )
            }
        )
    }
}
