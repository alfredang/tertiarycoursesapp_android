package com.tertiaryinfotech.tertiarycourses

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

// White / deep-blue house theme (matching the website and the iOS app) with a dark
// counterpart. Every colour is read through [Theme], which flips with [Theme.dark].
object Theme {
    /** Set by [AppTheme]; reading a colour in a composable recomposes when it changes. */
    var dark by mutableStateOf(false)

    /** Brand blue for TEXT and ICONS: #0B3D8E on light, a lighter tint on dark. */
    val accent get() = if (dark) Color(0xFF9DBBFF) else Color(0xFF0B3D8E)
    /** Brand blue for FILLED surfaces that carry white content (buttons, selected day). */
    val brand get() = if (dark) Color(0xFF2B5FC2) else Color(0xFF0B3D8E)
    val brandDark get() = if (dark) Color(0xFF1E4696) else Color(0xFF0A2D6F)
    val accentSoft get() = if (dark) Color(0x339DBBFF) else Color(0x1A0B3D8E)
    /** Tonal container (tonal buttons, nav indicator). */
    val tonal get() = if (dark) Color(0xFF243657) else Color(0xFFDCE4F3)
    val success get() = if (dark) Color(0xFF5FD08A) else Color(0xFF1C8C4A)
    val warning get() = if (dark) Color(0xFFFFB454) else Color(0xFFCC7300)
    val page get() = if (dark) Color(0xFF0F1115) else Color(0xFFF7F7FA)
    val card get() = if (dark) Color(0xFF1A1D23) else Color.White
    val cardBorder get() = if (dark) Color(0xFF2E323B) else Color(0xFFE0E0E6)
    /** Inset fields / unselected segments. */
    val field get() = if (dark) Color(0xFF262A32) else Color(0xFFEDEDF2)
    val ink get() = if (dark) Color(0xFFECEDF1) else Color(0xFF1C1C1E)
    val inkSoft get() = if (dark) Color(0xFFC4C7CF) else Color(0xFF3A3A3C)
    val secondaryText get() = if (dark) Color(0xFFA4A8B2) else Color(0xFF6B6B72)
    val tertiaryText get() = if (dark) Color(0xFF737884) else Color(0xFFAEAEB4)
    val star get() = Color(0xFFFF9500)
    val danger get() = if (dark) Color(0xFFFF7A70) else Color(0xFFD32F2F)
}

private fun scheme(dark: Boolean) = if (!dark) lightColorScheme(
    primary = Color(0xFF0B3D8E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE4F3),
    onPrimaryContainer = Color(0xFF0B3D8E),
    secondary = Color(0xFF0B3D8E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE4F3),
    onSecondaryContainer = Color(0xFF0B3D8E),
    tertiary = Color(0xFF0B3D8E),
    background = Color(0xFFF7F7FA),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFF7F7FA),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFEDEDF2),
    onSurfaceVariant = Color(0xFF6B6B72),
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFEDEDF2),
    outline = Color(0xFFC7C7CC),
    outlineVariant = Color(0xFFE0E0E6),
    error = Color(0xFFD32F2F),
) else darkColorScheme(
    primary = Color(0xFF9DBBFF),
    onPrimary = Color(0xFF06255C),
    primaryContainer = Color(0xFF243657),
    onPrimaryContainer = Color(0xFFDCE4F3),
    secondary = Color(0xFF9DBBFF),
    onSecondary = Color(0xFF06255C),
    secondaryContainer = Color(0xFF243657),
    onSecondaryContainer = Color(0xFFDCE4F3),
    tertiary = Color(0xFF9DBBFF),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFECEDF1),
    surface = Color(0xFF0F1115),
    onSurface = Color(0xFFECEDF1),
    surfaceVariant = Color(0xFF262A32),
    onSurfaceVariant = Color(0xFFA4A8B2),
    surfaceContainer = Color(0xFF1A1D23),
    surfaceContainerLow = Color(0xFF1A1D23),
    surfaceContainerHigh = Color(0xFF22262E),
    surfaceContainerHighest = Color(0xFF262A32),
    outline = Color(0xFF4A4F5A),
    outlineVariant = Color(0xFF2E323B),
    error = Color(0xFFFF7A70),
    onError = Color(0xFF5A0A05),
)

/** [pref] is the account's saved appearance: "system" | "light" | "dark". */
@Composable
fun AppTheme(pref: String = "system", content: @Composable () -> Unit) {
    val dark = when (pref) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    Theme.dark = dark
    MaterialTheme(colorScheme = scheme(dark), typography = Typography(), content = content)
}

// ── Brand ──

/** The TC badge (gradient rounded square, "TC" over the site code). */
@Composable
fun BrandBadge(size: Dp = 30.dp) {
    Column(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.23f))
            .background(Brush.verticalGradient(listOf(Color(0xFF0B3D8E), Color(0xFF0A2D6F)))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("TC", color = Color.White, fontWeight = FontWeight.Black, fontSize = (size.value * 0.37f).sp, lineHeight = (size.value * 0.37f).sp)
        Text(Site.code, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.24f).sp, lineHeight = (size.value * 0.26f).sp)
    }
}

/** Brand mark at the top of every tab: the TC LMS badge + the site name. */
@Composable
fun BrandHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = Site.siteName },
    ) {
        BrandBadge()
        Spacer(Modifier.width(8.dp))
        Text(
            Site.siteName, color = Theme.accent, fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * A tab screen: a top bar with either the brand mark (tab roots, [title] = null) or a
 * title with a back arrow (pushed screens), on the light-grey page background.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    navigationIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = Theme.page,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (title == null) BrandHeader()
                    else Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) { Icon(navigationIcon, contentDescription = "Back") }
                },
                actions = actions,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Theme.page, scrolledContainerColor = Theme.page),
            )
        },
        content = content,
    )
}

// ── Building blocks (house style, matching the iOS InfoCard / SectionLabel) ──

@Composable
fun InfoCard(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Theme.card)
            .border(1.dp, Theme.cardBorder, RoundedCornerShape(14.dp))
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), modifier = modifier, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        color = Theme.secondaryText, letterSpacing = 0.4.sp,
    )
}

@Composable
fun StatusChip(text: String, color: Color) {
    Text(
        text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

/** Icon + text, like SwiftUI's Label. */
@Composable
fun IconLine(
    icon: ImageVector,
    text: String,
    color: Color = Color.Unspecified,
    iconTint: Color = Theme.secondaryText,
    fontSize: Int = 14,
    weight: FontWeight = FontWeight.Normal,
    iconSize: Dp = 18.dp,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.padding(top = 1.dp).size(iconSize))
        Spacer(Modifier.width(10.dp))
        Text(text, color = color, fontSize = fontSize.sp, fontWeight = weight, lineHeight = (fontSize + 6).sp)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    busy: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick, enabled = enabled && !busy, modifier = modifier.fillMaxWidth().height52(),
        colors = ButtonDefaults.buttonColors(containerColor = Theme.brand, contentColor = Color.White),
    ) { ButtonContent(text, icon, busy, Color.White) }
}

@Composable
fun TonalButton(text: String, icon: ImageVector? = null, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().height52(),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Theme.tonal, contentColor = Theme.accent),
    ) { ButtonContent(text, icon, false, Theme.accent) }
}

@Composable
fun OutlineButton(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth().height52()) { ButtonContent(text, icon, false, Theme.accent) }
}

private fun Modifier.height52() = this.heightIn(min = 50.dp)

@Composable
private fun ButtonContent(text: String, icon: ImageVector?, busy: Boolean, tint: Color) {
    if (busy) {
        CircularProgressIndicator(color = tint, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
    } else if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun ErrorCard(message: String, retry: () -> Unit) {
    InfoCard {
        IconLine(Icons.Filled.WifiOff, message, iconTint = Theme.secondaryText)
        Spacer(Modifier.size(10.dp))
        OutlinedButton(onClick = retry) { Text("Try Again") }
    }
}

/** Like ContentUnavailableView: a centred icon, title and description. */
@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 36.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = Theme.tertiaryText, modifier = Modifier.size(52.dp))
        Spacer(Modifier.size(14.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.size(6.dp))
        Text(text, fontSize = 15.sp, color = Theme.secondaryText, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.size(12.dp)); action()
        }
    }
}

@Composable
fun LoadingBox(text: String? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = Theme.accent)
        if (text != null) {
            Spacer(Modifier.size(12.dp))
            Text(text, color = Theme.secondaryText, fontSize = 14.sp)
        }
    }
}

@Composable
fun LabeledValue(label: String, value: String, fontSize: Int = 14, weight: FontWeight = FontWeight.Normal, color: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, fontSize = fontSize.sp, fontWeight = weight, color = color)
        Spacer(Modifier.width(16.dp).weight(1f, fill = true))
        Text(value, fontSize = fontSize.sp, fontWeight = weight, color = if (color == Color.Unspecified) Theme.secondaryText else color, textAlign = TextAlign.End)
    }
}

// ── Remote images (tiny OkHttp loader; no extra image library) ──

private object ImageCache {
    val memory = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    suspend fun load(url: String, maxPx: Int): ImageBitmap? {
        val key = "$url@$maxPx"
        memory.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(url).header("User-Agent", Api.userAgent).build()
                val bytes = Api.client.newCall(req).execute().use { if (it.isSuccessful) it.body?.bytes() else null } ?: return@withContext null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= maxPx && bounds.outHeight / (sample * 2) >= maxPx) sample *= 2
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                bmp?.asImageBitmap()?.also { memory.put(key, it) }
            } catch (e: Exception) {
                null
            }
        }
    }
}

@Composable
fun RemoteImage(url: String, maxPx: Int, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop, placeholder: @Composable () -> Unit = {}) {
    val image by produceState<ImageBitmap?>(null, url) { value = if (url.isEmpty()) null else ImageCache.load(url, maxPx) }
    val img = image
    if (img != null) Image(img, contentDescription = null, contentScale = contentScale, modifier = modifier) else Box(modifier) { placeholder() }
}

@Composable
fun CourseThumb(url: String, size: Dp = 56.dp) {
    RemoteImage(
        url, maxPx = 240,
        modifier = Modifier.size(size).clip(RoundedCornerShape(10.dp)),
    ) {
        Box(Modifier.fillMaxSize().background(Theme.accentSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Theme.accent)
        }
    }
}

// ── Intents ──

fun openUrl(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open this link.", Toast.LENGTH_SHORT).show()
    }
}

fun whatsAppUrl(number: String, body: String): String =
    "https://wa.me/$number?text=" + Uri.encode(body)

fun openWhatsApp(context: Context, number: String, body: String) = openUrl(context, whatsAppUrl(number, body))

fun shareText(context: Context, text: String, subject: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text).putExtra(Intent.EXTRA_SUBJECT, subject)
    context.startActivity(Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
