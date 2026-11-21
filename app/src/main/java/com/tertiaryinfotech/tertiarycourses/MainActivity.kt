package com.tertiaryinfotech.tertiarycourses

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Screenshot hooks are read only in debug builds (see AppViewModel.applyDebugHooks).
        vm.start(if (BuildConfig.DEBUG) intent?.extras else null)
        setContent {
            // Signed in: the account's saved appearance. Signed out: the device setting.
            AppTheme(vm.themePref) {
                val dark = Theme.dark
                // System bar icons follow the app's appearance, not the device's.
                LaunchedEffect(dark) {
                    val bar = if (dark) 0xFF1A1D23.toInt() else AndroidColor.WHITE
                    enableEdgeToEdge(
                        statusBarStyle = if (dark) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                        else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
                        navigationBarStyle = if (dark) SystemBarStyle.dark(bar) else SystemBarStyle.light(bar, bar),
                    )
                }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.onForeground() }
                NotificationPermissionPrompt(vm)
                RootScreen(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (BuildConfig.DEBUG) vm.onNewLaunch(intent.extras)
    }
}

private data class TabSpec(val label: String, val icon: ImageVector)

/** Signed out: browse courses + sign in. Signed in: the learner's LMS. */
@Composable
fun RootScreen(vm: AppViewModel) {
    when {
        vm.restoring -> Box(Modifier.fillMaxSize().background(Theme.page), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Theme.accent)
        }
        vm.isSignedIn -> LearnerTabs(vm)
        else -> GuestTabs(vm)
    }
}

@Composable
private fun GuestTabs(vm: AppViewModel) {
    val tabs = listOf(
        TabSpec("Sign In", Icons.AutoMirrored.Filled.Login),
        TabSpec("Courses", Icons.AutoMirrored.Filled.LibraryBooks),
        TabSpec("About", Icons.Filled.Info),
    )
    Tabs(vm, tabs, vm.guestTab, { vm.guestTab = it }, "guest") { index, push ->
        when (index) {
            0 -> SignInScreen(vm)
            1 -> CatalogScreen(vm, push = push)
            else -> AboutScreen()
        }
    }
}

@Composable
private fun LearnerTabs(vm: AppViewModel) {
    val tabs = listOf(
        TabSpec("My Classes", Icons.Filled.School),
        TabSpec("Calendar", Icons.Filled.CalendarMonth),
        TabSpec("Certificates", Icons.Filled.WorkspacePremium),
        TabSpec("Billing", Icons.Filled.CreditCard),
        TabSpec("Account", Icons.Filled.AccountCircle),
    )
    Tabs(vm, tabs, vm.learnerTab, { vm.learnerTab = it }, "learner") { index, push ->
        when (index) {
            0 -> ClassesScreen(vm, push)
            1 -> CalendarScreen(vm, push)
            2 -> CertificatesScreen(vm, push)
            3 -> BillingScreen(vm)
            else -> AccountScreen(vm, push)
        }
    }
}

/**
 * Bottom navigation with one back stack per tab. Each tab root and pushed screen keeps
 * its saveable state (e.g. scroll position) while you visit other tabs.
 */
@Composable
private fun Tabs(
    vm: AppViewModel,
    tabs: List<TabSpec>,
    selected: Int,
    onSelect: (Int) -> Unit,
    prefix: String,
    root: @Composable (index: Int, push: (Route) -> Unit) -> Unit,
) {
    val holder = rememberSaveableStateHolder()
    val key = "$prefix$selected"
    val stack = vm.stack(key)

    Scaffold(
        containerColor = Theme.page,
        bottomBar = {
            NavigationBar(containerColor = Theme.card) {
                tabs.forEachIndexed { index, spec ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = {
                            // Re-tapping the current tab returns to its root.
                            if (selected == index) vm.stack("$prefix$index").clear() else onSelect(index)
                        },
                        icon = { Icon(spec.icon, contentDescription = null) },
                        label = { Text(spec.label, fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Theme.accent,
                            selectedTextColor = Theme.accent,
                            indicatorColor = Theme.tonal,
                            unselectedIconColor = Theme.inkSoft,
                            unselectedTextColor = Theme.inkSoft,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        val bottom = androidx.compose.foundation.layout.PaddingValues(bottom = padding.calculateBottomPadding())
        // Consume the nav-bar padding so imePadding() inside screens adds only the extra keyboard height.
        Box(Modifier.fillMaxSize().padding(bottom).consumeWindowInsets(bottom)) {
            BackHandler(enabled = stack.isNotEmpty()) { stack.removeAt(stack.lastIndex) }
            val push: (Route) -> Unit = { stack.add(it) }
            val pop: () -> Unit = { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
            val top = stack.lastOrNull()
            holder.SaveableStateProvider("$key/${stack.size}/${top?.hashCode() ?: 0}") {
                if (top == null) root(selected, push) else RouteScreen(vm, top, push, pop)
            }
        }
    }
}

@Composable
private fun RouteScreen(vm: AppViewModel, route: Route, push: (Route) -> Unit, pop: () -> Unit) {
    when (route) {
        is Route.ClassDetail -> ClassDetailScreen(vm, route.runId, route.title, push, pop)
        is Route.Survey -> SurveyScreen(vm, route.runId, pop)
        is Route.Quiz -> QuizScreen(vm, route.runId, route.quizId, route.title, pop)
        is Route.TrainerClass -> TrainerClassScreen(vm, route.runId, route.title, push, pop)
        is Route.Document -> DocumentScreen(route.title, route.url, pop)
        Route.EditProfile -> EditProfileScreen(vm, pop)
        is Route.CourseDetail -> CourseDetailScreen(route.course, pop)
        Route.Catalog -> CatalogScreen(vm, push = push, onBack = pop)
        Route.Feedback -> FeedbackScreen(onBack = pop)
        Route.About -> AboutScreen(onBack = pop)
    }
}
