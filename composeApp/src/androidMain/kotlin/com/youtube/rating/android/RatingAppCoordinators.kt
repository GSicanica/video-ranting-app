package com.youtube.rating.android

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import com.youtube.rating.android.api.BugReportApiClient
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.navigation.Screen
import com.youtube.rating.android.network.NetworkResult
import com.youtube.rating.android.notifications.NotificationNavigator
import com.youtube.rating.android.sentry.SentryLogger
import com.youtube.rating.android.ui.screens.BugReportDialog
import com.youtube.rating.android.utils.DeepLinkUtil.extractVideoIdFromIntent
import com.youtube.rating.android.utils.DeepLinkUtil.extractStartSecondsFromIntent
import com.youtube.rating.core.coroutines.makeIOCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun AppStringsCoordinator(
    currentLanguage: Strings.Language,
    selectedContentLanguages: Set<Strings.Language>
) {
    LaunchedEffect(currentLanguage) {
        Strings.currentLanguage = currentLanguage
    }

    LaunchedEffect(selectedContentLanguages) {
        Strings.selectedContentLanguages = selectedContentLanguages
    }
}

@Composable
internal fun IntentRouter(
    initialIntent: Intent?,
    currentIntent: Intent?,
    onConsumeIntent: () -> Unit,
    navController: NavHostController,
    onSetInitialIntent: (Intent) -> Unit
) {
    LaunchedEffect(initialIntent) {
        if (initialIntent != null) onSetInitialIntent(initialIntent)
    }

    LaunchedEffect(currentIntent) {
        val intent = currentIntent ?: return@LaunchedEffect

        val videoId = extractVideoIdFromIntent(intent = intent)
        if (videoId != null) {
            val startSeconds = extractStartSecondsFromIntent(intent = intent)
            navController.navigate(Screen.RateVideo.createRoute(videoId, startSeconds)) {
                popUpTo(Screen.Home.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        val destination = NotificationNavigator.resolveDestination(intent)
        if (destination != null) {
            navController.navigate(destination.route) {
                launchSingleTop = true
                restoreState = true
            }
        }

        onConsumeIntent()
    }
}

@Composable
internal fun BugReportCoordinator(
    show: Boolean,
    context: Context,
    scope: CoroutineScope,
    onDismiss: () -> Unit
) {
    if (!show) return

    BugReportDialog(
        onDismiss = onDismiss,
        onSubmit = { title, description, email, priority ->
            scope.makeIOCall {
                try {
                    val bugReportApi = BugReportApiClient()
                    val result = bugReportApi.submitBugReport(
                        context = context,
                        title = title,
                        description = description,
                        userEmail = email.takeIf { it.isNotBlank() },
                        priority = priority
                    )

                    withContext(Dispatchers.Main) {
                        if (result is NetworkResult.Success) {
                            android.widget.Toast.makeText(
                                context,
                                "✅ Bug report #${result.data.bugId} uspješno poslan!",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        } else if (result is NetworkResult.Retryable) {
                            android.widget.Toast.makeText(
                                context,
                                "⚠️ Privremena greška: ${result.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else if (result is NetworkResult.Error) {
                            android.widget.Toast.makeText(
                                context,
                                "❌ Greška: ${result.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    SentryLogger.captureException(e)
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "❌ Greška pri slanju: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    )
}
