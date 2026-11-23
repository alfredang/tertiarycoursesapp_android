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
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        // Opened by the site at the end of admin sign-in with Google (app was closed meanwhile).
        vm.onAuthCallback(intent?.data)
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
        vm.onAuthCallback(intent.data)
        if (BuildConfig.DEBUG) vm.onNewLaunch(intent.extras)
    }
}

/** [id] names the tab's back stack; it stays the same when the set of tabs changes with the role. */
private data class TabSpec(val id: Int, val label: String, val icon: ImageVector)

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
        TabSpec(0, "Sign In", Icons.Filled.AccountCircle),
        TabSpec(1, "Courses", Icons.AutoMirrored.Filled.LibraryBooks),
        TabSpec(2, "About", Icons.Filled.Info),
    )
    Tabs(vm, tabs, vm.guestTab, { vm.guestTab = it }, "guest") { id, push ->
        when (id) {
            0 -> SignInScreen(vm)
            1 -> CatalogScreen(vm, push = push)
            else -> AboutScreen()
        }
    }
}

@Composable
private fun LearnerTabs(vm: AppViewModel) {
    val staff = vm.staffRole
    val trainer = vm.isTrainer
    // Learner: My Classes, Calendar, Certificates, Support, Account (Billing is a row in Account).
    // Trainer: My Classes, Calendar, Payout, Hours, Account.
    // Admin: Home, Calendar, Courses, Account. Developer, Marketing, Franchise Admin: Home, Courses, Account.
    val tabs = buildList {
        if (staff != null) {
            add(TabSpec(AppViewModel.TAB_DASHBOARD, "Home", Icons.Filled.Home))
            if (staff == "admin") add(TabSpec(AppViewModel.TAB_CALENDAR, "Calendar", Icons.Filled.CalendarMonth))
            add(TabSpec(AppViewModel.TAB_COURSES, "Courses", Icons.AutoMirrored.Filled.LibraryBooks))
        } else {
            add(TabSpec(AppViewModel.TAB_CLASSES, "My Classes", Icons.Filled.School))
            add(TabSpec(AppViewModel.TAB_CALENDAR, "Calendar", Icons.Filled.CalendarMonth))
            if (trainer) {
                add(TabSpec(AppViewModel.TAB_THIRD, "Payout", Icons.Filled.Payments))
                add(TabSpec(AppViewModel.TAB_FOURTH, "Hours", Icons.Filled.Schedule))
            } else {
                add(TabSpec(AppViewModel.TAB_THIRD, "Certificates", Icons.Filled.WorkspacePremium))
                add(TabSpec(AppViewModel.TAB_SUPPORT, "Support", Icons.Filled.SupportAgent))
            }
        }
        add(TabSpec(AppViewModel.TAB_ACCOUNT, "Account", Icons.Filled.AccountCircle))
    }
    Tabs(vm, tabs, vm.learnerTab, { vm.learnerTab = it }, "learner") { id, push ->
        when (id) {
            AppViewModel.TAB_DASHBOARD -> when (val role = staff ?: vm.activeRole) {
                "admin" -> AdminHomeScreen(vm)
                "developer" -> DeveloperHomeScreen(vm)
                else -> StaffDashboardScreen(vm, role)
            }
            AppViewModel.TAB_CLASSES -> ClassesScreen(vm, push)
            AppViewModel.TAB_CALENDAR -> if (staff == "admin") AdminCalendarScreen(vm) else CalendarScreen(vm, push)
            AppViewModel.TAB_COURSES -> CatalogScreen(vm, push = push)
            AppViewModel.TAB_THIRD -> if (trainer) PayoutScreen(vm) else CertificatesScreen(vm, push)
            AppViewModel.TAB_FOURTH -> TrainingHoursScreen(vm)
            AppViewModel.TAB_SUPPORT -> SupportScreen(vm)
            else -> AccountScreen(vm, push)
        }
    }
    vm.roleNotice?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.roleNotice = null },
            confirmButton = { TextButton(onClick = { vm.roleNotice = null }) { Text("OK") } },
            title = { Text("Signed in") },
            text = { Text(msg) },
        )
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
    root: @Composable (id: Int, push: (Route) -> Unit) -> Unit,
) {
    val holder = rememberSaveableStateHolder()
    // A tab that the current role does not have (after a role change) falls back to the first.
    val current = (tabs.firstOrNull { it.id == selected } ?: tabs.first()).id
    val key = "$prefix$current"
    val stack = vm.stack(key)

    Scaffold(
        containerColor = Theme.page,
        bottomBar = {
            NavigationBar(containerColor = Theme.card) {
                tabs.forEach { spec ->
                    NavigationBarItem(
                        selected = current == spec.id,
                        onClick = {
                            // Re-tapping the current tab returns to its root.
                            if (current == spec.id) vm.stack("$prefix${spec.id}").clear() else onSelect(spec.id)
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
                if (top == null) root(current, push) else RouteScreen(vm, top, push, pop)
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
        is Route.CourseDetail -> CourseDetailScreen(route.course, pop)
        Route.Catalog -> CatalogScreen(vm, push = push, onBack = pop)
        Route.Feedback -> FeedbackScreen(onBack = pop)
        Route.About -> AboutScreen(onBack = pop)
        Route.Billing -> BillingScreen(vm, onBack = pop)
        Route.Support -> SupportScreen(vm, onBack = pop)
    }
}
