package com.tertiaryinfotech.tertiarycourses

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// White theme with the Tertiary Infotech deep-blue brand accent (sampled from the T logo),
// matching the iOS app and tertiarycourses.com.sg.
object Theme {
    val accent = Color(0xFF0B3D8E)          // deep blue #0B3D8E
    val accentSoft = Color(0x1A0B3D8E)      // 10% deep blue
    val page = Color.White
    val card = Color.White
    val cardBorder = Color(0xFFE0E0E6)
    val secondaryText = Color(0xFF6B6B70)
    val positiveGreen = Color(0xFF1B7A3D)
}

private val LightColors = lightColorScheme(
    primary = Theme.accent,
    onPrimary = Color.White,
    primaryContainer = Theme.accentSoft,
    onPrimaryContainer = Theme.accent,
    secondary = Theme.accent,
    background = Theme.page,
    surface = Theme.page,
    surfaceVariant = Color(0xFFF4F4F7),
    onSurfaceVariant = Theme.secondaryText
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = LightColors) {
                Surface(modifier = Modifier.fillMaxSize(), color = Theme.page) {
                    RootScreen()
                }
            }
        }
    }
}

private data class TabSpec(
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun RootScreen() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val navController = rememberNavController()

    val tabs = listOf(
        TabSpec("Catalog") { Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Catalog") },
        TabSpec("Grant Calc") { Icon(Icons.Filled.Calculate, contentDescription = "Grant Calculator") },
        TabSpec("Feedback") { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Feedback") },
        TabSpec("About") { Icon(Icons.Filled.Info, contentDescription = "About") }
    )

    Scaffold(
        containerColor = Theme.page,
        bottomBar = {
            NavigationBar(containerColor = Theme.page) {
                tabs.forEachIndexed { index, spec ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = spec.icon,
                        label = { Text(spec.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Theme.accent,
                            selectedTextColor = Theme.accent,
                            indicatorColor = Theme.accentSoft,
                            unselectedIconColor = Theme.secondaryText,
                            unselectedTextColor = Theme.secondaryText
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            when (tab) {
                0 -> NavHost(navController = navController, startDestination = "catalog") {
                    composable("catalog") {
                        CatalogScreen(onCourseClick = { course ->
                            navController.navigate("course/${course.id}")
                        })
                    }
                    composable("course/{courseId}") { backStackEntry ->
                        val courseId = backStackEntry.arguments?.getString("courseId") ?: return@composable
                        CourseDetailScreen(
                            course = CourseData.courseById(courseId),
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                1 -> GrantCalculatorScreen()
                2 -> FeedbackScreen()
                else -> AboutScreen()
            }
        }
    }
}

// MARK: Shared components (house style, matching the iOS InfoCard / SectionLabel)

@Composable
fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Theme.cardBorder, RoundedCornerShape(14.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = Theme.secondaryText
    )
}

@Composable
fun LabeledRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = Theme.secondaryText,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun LinkText(text: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Theme.accent,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.clickable { uriHandler.openUri(url) }
    )
}

fun whatsAppUrl(number: String, body: String): String =
    "https://wa.me/$number?text=" + java.net.URLEncoder.encode(body, "UTF-8")
