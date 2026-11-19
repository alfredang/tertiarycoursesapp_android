package com.tertiaryinfotech.tertiarycourses

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

private sealed interface DocState {
    data object Loading : DocState
    data class Pdf(val file: File) : DocState
    data class Web(val url: String) : DocState
    data class Failed(val message: String) : DocState
}

/**
 * Class materials, documents and certificates open here instead of the browser. A link that
 * answers with a PDF is downloaded to the cache and drawn page by page (Android's WebView
 * cannot show PDFs); anything else — Google Drive / Docs viewers, web pages — loads in a
 * WebView. The title bar shares the link or opens it in the browser.
 */
@Composable
fun DocumentScreen(title: String, url: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var attempt by remember { mutableIntStateOf(0) }
    val state by produceState<DocState>(DocState.Loading, url, attempt) {
        value = DocState.Loading
        value = resolveDocument(context, url)
    }

    ScreenScaffold(
        title = title.ifEmpty { "Document" },
        onBack = onBack,
        navigationIcon = Icons.Filled.Close,
        actions = {
            IconButton(onClick = { shareText(context, url, title) }) { Icon(Icons.Filled.Share, contentDescription = "Share") }
            IconButton(onClick = { openUrl(context, url) }) { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in browser") }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                DocState.Loading -> LoadingBox("Opening…")
                is DocState.Pdf -> PdfPages(s.file)
                is DocState.Web -> WebPage(s.url)
                is DocState.Failed -> Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ErrorCard(s.message) { scope.launch { attempt++ } }
                    OutlineButton("Open in Browser", Icons.AutoMirrored.Filled.OpenInNew) { openUrl(context, url) }
                }
            }
        }
    }
}

/** Hosts whose links are viewer pages, not files: no need to probe them first. */
private fun isViewerHost(host: String) =
    host.endsWith("google.com") || host.endsWith("youtube.com") || host.endsWith("youtu.be") ||
        host.endsWith("sharepoint.com") || host.endsWith("onedrive.live.com") || host.endsWith("canva.com") ||
        host.endsWith("notion.so") || host.endsWith("github.com")

private fun isSiteHost(host: String) = host == Uri.parse(Site.baseUrl).host

/** Google Drive file links open cleaner (no "open in app" banner) on their /preview page. */
private fun viewerUrl(url: String): String {
    val m = Regex("""^https://drive\.google\.com/file/d/([^/?#]+)""").find(url) ?: return url
    return "https://drive.google.com/file/d/${m.groupValues[1]}/preview"
}

private suspend fun resolveDocument(context: Context, url: String): DocState = withContext(Dispatchers.IO) {
    val uri = try { Uri.parse(url) } catch (e: Exception) { null }
    val host = uri?.host.orEmpty().lowercase()
    if (uri == null || (uri.scheme != "https" && uri.scheme != "http") || host.isEmpty()) {
        return@withContext DocState.Failed("This link can't be opened in the app.")
    }
    if (isViewerHost(host)) return@withContext DocState.Web(viewerUrl(url))

    val name = MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }.take(32)
    val dir = File(context.cacheDir, "documents").apply { mkdirs() }
    val file = File(dir, "$name.pdf")
    try {
        val request = Request.Builder().url(url)
            // The site's bot rules need the app's own User-Agent (see Api.send).
            .apply { if (isSiteHost(host)) header("User-Agent", Api.userAgent) }
            .header("Accept", "application/pdf,text/html;q=0.9,*/*;q=0.8")
            .build()
        Api.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext if (response.code == 404 || response.code == 410) DocState.Failed("This document is no longer available.")
                else DocState.Web(url)
            }
            val body = response.body ?: return@withContext DocState.Web(url)
            val type = body.contentType()?.toString().orEmpty().lowercase()
            val source = body.source()
            val isPdf = "pdf" in type || (source.request(5) && source.peek().readUtf8(5) == "%PDF-")
            if (!isPdf) return@withContext DocState.Web(response.request.url.toString())
            // Keep only a few cached files; certificates and slides are re-downloaded on demand.
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(8)?.forEach { it.delete() }
            val tmp = File(dir, "$name.tmp")
            tmp.outputStream().use { out -> source.inputStream().copyTo(out) }
            tmp.renameTo(file)
            DocState.Pdf(file)
        }
    } catch (e: Exception) {
        if (file.exists()) DocState.Pdf(file) else DocState.Failed("Can't open this document. Check your connection and try again.")
    }
}

// ── PDF ──

private class PdfDoc(file: File) {
    private val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(fd)
    private val lock = Mutex()
    val pageCount = renderer.pageCount
    /** width / height of each page, read up front so the list can lay out before rendering. */
    val ratios: List<Float> = (0 until pageCount).map { i ->
        renderer.openPage(i).use { p -> if (p.height > 0) p.width.toFloat() / p.height else 0.7f }
    }

    suspend fun render(index: Int, widthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        lock.withLock {
            try {
                renderer.openPage(index).use { page ->
                    val w = widthPx.coerceIn(320, 2000)
                    val h = (w * page.height.toFloat() / page.width).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun close() {
        try { renderer.close(); fd.close() } catch (e: Exception) { }
    }
}

/** Pages in a scrolling list; pinch or double-tap to zoom, drag sideways to pan when zoomed. */
@Composable
private fun PdfPages(file: File) {
    val doc = remember(file) { try { PdfDoc(file) } catch (e: Exception) { null } }
    DisposableEffect(doc) { onDispose { doc?.close() } }
    if (doc == null || doc.pageCount == 0) {
        Box(Modifier.padding(20.dp)) { Text("This PDF can't be displayed. Use Open in Browser at the top.", color = Theme.secondaryText) }
        return
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableIntStateOf(0) }
    fun clamp() {
        val max = width * (scale - 1f) / 2f
        offsetX = offsetX.coerceIn(-max, max)
    }

    Box(
        Modifier.fillMaxSize().clipToBounds().background(Theme.field).onSizeChanged { width = it.width }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { scale = if (scale > 1f) 1f else 2.5f; offsetX = 0f })
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.count { it.pressed }
                        if (fingers > 1) {
                            scale = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                            offsetX += event.calculatePan().x
                            clamp()
                            event.changes.forEach { it.consume() }
                        } else if (scale > 1f) {
                            // One finger while zoomed: pan sideways here, the list scrolls vertically.
                            offsetX += event.calculatePan().x
                            clamp()
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        LazyColumn(
            Modifier.fillMaxSize().testTag("pdfPages").graphicsLayer { scaleX = scale; scaleY = scale; translationX = offsetX },
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(doc.pageCount, key = { it }) { index ->
                val bitmap by produceState<Bitmap?>(null, index, width) {
                    if (width > 0) value = doc.render(index, (width * 1.5f).toInt())
                }
                Box(Modifier.fillMaxWidth().aspectRatio(doc.ratios[index]).background(Color.White), contentAlignment = Alignment.Center) {
                    val b = bitmap
                    if (b != null) Image(b.asImageBitmap(), contentDescription = "Page ${index + 1} of ${doc.pageCount}", contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxSize())
                    else Text("Page ${index + 1}", color = Color(0xFF8E8E93), fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Web ──

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebPage(url: String) {
    val context = LocalContext.current
    var progress by remember { mutableIntStateOf(0) }
    var web by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    BackHandler(enabled = canGoBack) { web?.goBack() }
    DisposableEffect(Unit) { onDispose { web?.apply { stopLoading(); destroy() } } }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize().testTag("webDocument"),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    // Identify the app (the site's bot rules) on top of the normal browser string.
                    settings.userAgentString = settings.userAgentString + " " + Api.userAgent
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val scheme = request.url.scheme.orEmpty()
                            if (scheme == "http" || scheme == "https") return false
                            // intent:, market:, mailto:, tel: … belong to other apps.
                            openUrl(context, request.url.toString())
                            return true
                        }

                        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                            canGoBack = view.canGoBack()
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            progress = newProgress
                        }
                    }
                    // A file the page offers for download (zip, pptx…) goes to the browser.
                    setDownloadListener { link, _, _, _, _ -> openUrl(context, link) }
                    loadUrl(url)
                    web = this
                }
            },
        )
        if (progress in 0..99) {
            LinearProgressIndicator(progress = { progress / 100f }, color = Theme.accent, modifier = Modifier.fillMaxWidth())
        }
    }
}
