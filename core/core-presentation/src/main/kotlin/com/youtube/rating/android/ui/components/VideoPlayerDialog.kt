package com.youtube.rating.android.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.withContext

/**
 * Clears/destroys pooled WebViews used by [VideoPlayerDialog].
 *
 * Call from process-level hooks (e.g. Application.onTrimMemory/onLowMemory) to free RAM.
 */
fun clearVideoPlayerWebViewPool(destroy: Boolean = true) {
    WebViewPool.clear(destroy = destroy)
}

private object WebViewPool {
    private var youTubeWebView: WebView? = null
    private var facebookWebView: WebView? = null
    private var prepared = false

    fun acquire(context: Context, isFacebook: Boolean): WebView {
        val appContext = context.applicationContext
        val cached = if (isFacebook) facebookWebView else youTubeWebView
        if (cached != null) {
            (cached.parent as? ViewGroup)?.removeView(cached)
            return cached
        }
        return WebView(appContext)
    }

    fun release(webView: WebView?, isFacebook: Boolean) {
        webView ?: return
        runCatching { webView.webChromeClient = null }
        runCatching { webView.webViewClient = WebViewClient() }
        runCatching { webView.setDownloadListener(null) }
        runCatching { webView.onPause() }
        runCatching { webView.pauseTimers() }
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.clearHistory()
        webView.removeAllViews()
        if (isFacebook) {
            facebookWebView = webView
        } else {
            youTubeWebView = webView
        }
    }

    fun clear(destroy: Boolean) {
        prepared = false
        if (destroy) {
            destroyWebView(webView = youTubeWebView)
            destroyWebView(webView = facebookWebView)
            youTubeWebView = null
            facebookWebView = null
        } else {
            release(youTubeWebView, isFacebook = false)
            release(facebookWebView, isFacebook = true)
        }
    }

    private fun destroyWebView(webView: WebView?) {
        webView ?: return
        runCatching { webView.webChromeClient = null }
        runCatching { webView.webViewClient = WebViewClient() }
        runCatching { webView.setDownloadListener(null) }
        runCatching { webView.stopLoading() }
        runCatching { webView.loadUrl("about:blank") }
        runCatching { webView.clearHistory() }
        runCatching { webView.clearCache(true) }
        runCatching { webView.removeAllViews() }
        runCatching { webView.destroy() }
    }

    fun prepare(context: Context) {
        if (prepared) return
        prepared = true
        val appContext = context.applicationContext
        if (youTubeWebView == null) {
            youTubeWebView = WebView(appContext).apply { loadUrl("about:blank") }
        }
        if (facebookWebView == null) {
            facebookWebView = WebView(appContext).apply { loadUrl("about:blank") }
        }
    }
}

private fun isAllowedVideoHost(url: String, isFacebook: Boolean): Boolean {
    val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull() ?: return false
    fun hostMatches(vararg allowed: String): Boolean = allowed.any { a ->
        host == a || host.endsWith(".$a")
    }

    return if (isFacebook) {
        hostMatches("facebook.com", "fb.watch", "fbcdn.net")
    } else {
        // YouTube and related hosts used during playback/consent.
        hostMatches("youtube.com", "m.youtube.com", "www.youtube.com", "youtu.be", "google.com", "consent.google.com")
    }
}

data class VideoPlayerDialogLabels(
    val continueWatchingTitle: String = "Continue watching",
    val continueWatchingText: String = "Choose where you want to continue watching the video.",
    val continueHere: String = "Continue here",
    val openInYouTube: String = "Open in YouTube",
    val openInBrowser: String = "Open in browser",
    val close: String = "Close",
    val tryAgain: String = "Try again"
)

@Composable
fun VideoPlayerDialog(
    videoId: String,
    videoTitle: String,
    onDismiss: () -> Unit,
    videoUrl: String? = null, // For Facebook videos
    webViewJsAutomationEnabled: Boolean = true,
    labels: VideoPlayerDialogLabels = VideoPlayerDialogLabels()
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var showContinueOptions by remember { mutableStateOf(false) }
    val isFacebookVideo = remember(videoUrl) {
        videoUrl != null && (videoUrl.contains("facebook.com") || videoUrl.contains("fb.watch"))
    }

    DisposableEffect(isFacebookVideo) {
        onDispose {
            WebViewPool.release(webViewRef, isFacebookVideo)
            webViewRef = null
        }
    }

    if (showContinueOptions) {
        AlertDialog(
            onDismissRequest = { showContinueOptions = false },
            title = { Text(labels.continueWatchingTitle) },
            text = { Text(labels.continueWatchingText) },
            confirmButton = {
                TextButton(onClick = { showContinueOptions = false }) {
                    Text(labels.continueHere)
                }
            },
            dismissButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showContinueOptions = false
                            val url = if (videoUrl.isNullOrBlank()) {
                                "https://www.youtube.com/watch?v=$videoId"
                            } else {
                                videoUrl
                            }
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    ) {
                        Text(labels.openInYouTube)
                    }
                    TextButton(
                        onClick = {
                            showContinueOptions = false
                            val url = if (videoUrl.isNullOrBlank()) {
                                "https://m.youtube.com/watch?v=$videoId"
                            } else {
                                videoUrl
                            }
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    ) {
                        Text(labels.openInBrowser)
                    }
                    TextButton(
                        onClick = {
                            showContinueOptions = false
                            onDismiss()
                        }
                    ) {
                        Text(labels.close)
                    }
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = videoTitle,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            val url = if (videoUrl.isNullOrBlank()) {
                                "https://www.youtube.com/watch?v=$videoId"
                            } else {
                                videoUrl
                            }
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    ) {
                        Text(labels.openInYouTube)
                    }
                    IconButton(
                        onClick = { showContinueOptions = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Nastavi gledanje",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = { showContinueOptions = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Opcije zatvaranja")
                    }
                }

                // Video player - YouTube or Facebook
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (isFacebookVideo && videoUrl != null) {
                        // Facebook video
                        FacebookWebView(
                            videoUrl = videoUrl,
                            enableJsAutomation = webViewJsAutomationEnabled,
                            onWebViewReady = { webViewRef = it },
                            onLoadError = { loadError = it.ifBlank { null } }
                        )
                    } else {
                        // YouTube video (default)
                        YouTubeWebView(
                            videoId = videoId,
                            enableJsAutomation = webViewJsAutomationEnabled,
                            onWebViewReady = { webViewRef = it },
                            onLoadError = { loadError = it.ifBlank { null } }
                        )
                    }

                    loadError?.let { message ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .align(Alignment.Center),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = message,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            loadError = null
                                            val url = if (videoUrl.isNullOrBlank()) {
                                                "https://m.youtube.com/watch?v=$videoId"
                                            } else {
                                                videoUrl
                                            }
                                            webViewRef?.loadUrl(url)
                                        }
                                    ) {
                                        Text(labels.tryAgain)
                                    }
                                    Button(
                                        onClick = {
                                            val url = if (videoUrl.isNullOrBlank()) {
                                                "https://www.youtube.com/watch?v=$videoId"
                                            } else {
                                                videoUrl
                                            }
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        }
                                    ) {
                                        Text(labels.openInYouTube)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YouTubeWebView(
    videoId: String,
    enableJsAutomation: Boolean = true,
    onWebViewReady: (WebView) -> Unit = {},
    onLoadError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val enableJsAutomationState by rememberUpdatedState(enableJsAutomation)
    var prepared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withContext(kotlinx.coroutines.Dispatchers.Main) {
            WebViewPool.prepare(context)
            prepared = true
        }
    }
    if (!prepared) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    AndroidView(
        factory = { ctx ->
            WebViewPool.acquire(ctx, isFacebook = false).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    // Tighten WebView: we only need network access.
                    allowFileAccess = false
                    allowContentAccess = false
                    allowFileAccessFromFileURLs = false
                    allowUniversalAccessFromFileURLs = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    javaScriptCanOpenWindowsAutomatically = false
                    setSupportMultipleWindows(false)
                    // Important: Set user agent to make YouTube think it's a real browser
                    userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }
                setBackgroundColor(android.graphics.Color.BLACK)
                if (Build.VERSION.SDK_INT >= 26) {
                    settings.safeBrowsingEnabled = true
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString().orEmpty()
                        if (url.isBlank()) return false
                        val scheme = request?.url?.scheme?.lowercase()
                        if (scheme != "http" && scheme != "https") return true
                        if (!isAllowedVideoHost(url, isFacebook = false)) {
                            // Block unexpected navigations inside WebView.
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                            return true
                        }
                        return false
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onLoadError("")
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (!enableJsAutomationState) return
                        view?.evaluateJavascript(
                            """
                            (function() {
                              try {
                                var tries = 0;
                                var maxTries = 50;
                                var timer = setInterval(function() {
                                  tries++;
                                  var buttons = document.querySelectorAll('button');
                                  for (var i = 0; i < buttons.length; i++) {
                                    var t = (buttons[i].innerText || '').toLowerCase();
                                    if (t.includes('accept') || t.includes('prihvati') || t.includes('slažem') || t.includes('agree')) {
                                      buttons[i].scrollIntoView({behavior:'smooth', block:'center'});
                                      clearInterval(timer);
                                      return;
                                    }
                                  }
                                  if (tries >= maxTries) {
                                    clearInterval(timer);
                                  }
                                }, 800);
                              } catch (e) {}
                            })();
                            """.trimIndent(),
                            null
                        )
                        view?.evaluateJavascript(
                            """
                            (function() {
                              try {
                                var attempts = 0;
                                var maxAttempts = 20;
                                function tryUnmute() {
                                  var v = document.querySelector('video');
                                  if (v) {
                                    v.muted = false;
                                    v.volume = 1.0;
                                    try { var p = v.play(); if (p && p.catch) p.catch(function(){}); } catch (e) {}
                                  }
                                  var btns = document.querySelectorAll('button');
                                  for (var i = 0; i < btns.length; i++) {
                                    var t = (btns[i].getAttribute('aria-label') || btns[i].title || '').toLowerCase();
                                    if (t.includes('unmute') || t.includes('uklju') || t.includes('sound on')) {
                                      try { btns[i].click(); } catch (e) {}
                                      break;
                                    }
                                  }
                                  return v != null;
                                }
                                if (tryUnmute()) return;
                                var t = setInterval(function() {
                                  attempts++;
                                  if (tryUnmute() || attempts >= maxAttempts) {
                                    clearInterval(t);
                                  }
                                }, 250);
                              } catch (e) {}
                            })();
                            """.trimIndent(),
                            null
                        )
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            val desc = error?.description?.toString()?.ifBlank { "Greška učitavanja videa." }
                                ?: "Greška učitavanja videa."
                            onLoadError(desc)
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        if (request?.isForMainFrame == true) {
                            val code = errorResponse?.statusCode ?: 0
                            onLoadError("Greška učitavanja (HTTP $code).")
                        }
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            Logger.debug("VideoPlayerDialog", "YouTube WebView Console: ${it.message()}")
                        }
                        return true
                    }
                }

                // Load YouTube mobile site instead of embed (works better)
                tag = videoId
                loadUrl("https://m.youtube.com/watch?v=$videoId")
                onWebViewReady(this)
            }
        },
        update = { webView ->
            if (webView.tag != videoId) {
                webView.tag = videoId
                webView.loadUrl("https://m.youtube.com/watch?v=$videoId")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun FacebookWebView(
    videoUrl: String,
    enableJsAutomation: Boolean = true,
    onWebViewReady: (WebView) -> Unit = {},
    onLoadError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val enableJsAutomationState by rememberUpdatedState(enableJsAutomation)
    var prepared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withContext(kotlinx.coroutines.Dispatchers.Main) {
            WebViewPool.prepare(context)
            prepared = true
        }
    }
    if (!prepared) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    AndroidView(
        factory = { ctx ->
            WebViewPool.acquire(ctx, isFacebook = true).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = false
                    allowContentAccess = false
                    allowFileAccessFromFileURLs = false
                    allowUniversalAccessFromFileURLs = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    javaScriptCanOpenWindowsAutomatically = false
                    setSupportMultipleWindows(false)
                    // Facebook works better with desktop user agent
                    userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                }
                if (Build.VERSION.SDK_INT >= 26) {
                    settings.safeBrowsingEnabled = true
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString().orEmpty()
                        if (url.isBlank()) return false
                        val scheme = request?.url?.scheme?.lowercase()
                        if (scheme != "http" && scheme != "https") return true
                        if (!isAllowedVideoHost(url, isFacebook = true)) {
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                            return true
                        }
                        return false
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onLoadError("")
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (!enableJsAutomationState) return
                        // Try to scroll to video and auto-play if possible
                        view?.evaluateJavascript("""
                            // Scroll to video element if it exists
                            var video = document.querySelector('video');
                            if (video) {
                                video.scrollIntoView({behavior: 'smooth', block: 'center'});
                                // Try to play (url = may be blocked by browser policy)
                                try {
                                    video.play().catch(function(e) {
                                        console.log('Auto-play blocked:', e);
                                    });
                                } catch(e) {
                                    console.log('Play failed:', e);
                                }
                            }
                        """, null)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            val desc = error?.description?.toString()?.ifBlank { "Greška učitavanja videa." }
                                ?: "Greška učitavanja videa."
                            onLoadError(desc)
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        if (request?.isForMainFrame == true) {
                            val code = errorResponse?.statusCode ?: 0
                            onLoadError("Greška učitavanja (HTTP $code).")
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            Logger.debug("VideoPlayerDialog", "Facebook WebView Console: ${it.message()}")
                        }
                        return true
                    }
                }

                // Load Facebook video URL
                tag = videoUrl
                loadUrl(videoUrl)
                onWebViewReady(this)
            }
        },
        update = { webView ->
            if (webView.tag != videoUrl) {
                webView.tag = videoUrl
                webView.loadUrl(videoUrl)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
