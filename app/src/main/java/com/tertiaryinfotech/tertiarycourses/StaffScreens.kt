package com.tertiaryinfotech.tertiarycourses

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

// Admin and Developer homes and the admin calendar, in the TIA TMS layout. Read-only; the
// server checks the role on every call (`staff/classes`, `staff/courses`).

// ── Admin Home ──

/** Welcome + View as; Running today / Next 30 days / Learners today; Current / Upcoming lists with search. */
@Composable
fun AdminHomeScreen(vm: AppViewModel) {
    var current by rememberSaveable { mutableStateOf(true) }
    var query by rememberSaveable { mutableStateOf("") }
    LoadedTab(vm.adminClasses, "adminHome", { vm.loadAdminClasses() }, header = { HomeHeader(vm) }) { data ->
        KpiTiles(
            "Running today" to "${data.runningToday}",
            "Next 30 days" to "${data.next30Days}",
            "Learners today" to "${data.learnersToday}",
        )
        val running = data.current.filter { it.matches(query) }
        val upcoming = data.upcoming.filter { it.matches(query) }
        Segments(listOf("Current (${running.size})", "Upcoming (${upcoming.size})"), if (current) 0 else 1, "adminBucket") { current = it == 0 }
        SearchField(query, "Search course, class ID or trainer") { query = it }
        val shown = if (current) running else upcoming
        if (shown.isEmpty()) {
            EmptyState(
                Icons.Filled.EventBusy,
                if (current) "No classes running today" else "No upcoming classes",
                if (query.isNotBlank()) "Try another search." else if (current) "Classes running today appear here." else "Classes starting in the next 30 days appear here.",
            )
        }
        shown.forEach { StaffClassCard(it) }
    }
}

/** One class in the admin lists and calendar: code, title, dates, trainer (or a warning), learners and mode. */
@Composable
fun StaffClassCard(item: StaffClass, showTime: Boolean = false) {
    InfoCard(Modifier.testTag("staffClass-${item.runId}"), padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                listOf(item.sku, item.classId).filter { it.isNotEmpty() }.joinToString("  ·  ").ifEmpty { "Course" },
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent, maxLines = 1,
            )
            Text(item.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, color = Theme.ink)
            SmallLine(Icons.Filled.Event, Fmt.range(item.startDate, item.endDate))
            if (showTime && item.startTime.isNotEmpty()) SmallLine(Icons.Filled.Schedule, Fmt.times(item.startTime, item.endTime))
            if (item.trainer.isEmpty()) SmallLine(Icons.Filled.Person, "No trainer assigned", Theme.warning, FontWeight.SemiBold)
            else SmallLine(Icons.Filled.Person, item.trainer)
            if (showTime && item.venue.isNotEmpty()) SmallLine(Icons.Filled.Place, item.venue)
            SmallLine(
                Icons.Filled.Groups,
                listOf("${item.learnerCount} learner${if (item.learnerCount == 1) "" else "s"}", item.mode).filter { it.isNotBlank() }.joinToString("  ·  "),
            )
        }
    }
}

@Composable
private fun SmallLine(icon: ImageVector, text: String, color: Color = Theme.secondaryText, weight: FontWeight = FontWeight.Normal) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(top = 1.dp).size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 13.sp, lineHeight = 18.sp, color = color, fontWeight = weight)
    }
}

// ── Developer Home ──

private enum class CourseFilter(val label: String) { ALL("All"), ENABLED("Enabled"), DISABLED("Disabled") }

/** Welcome + View as; Courses / Enabled / Disabled; the non-WSQ course list with a filter and search. */
@Composable
fun DeveloperHomeScreen(vm: AppViewModel, push: (Route) -> Unit) {
    var filter by rememberSaveable { mutableStateOf(CourseFilter.ALL) }
    var query by rememberSaveable { mutableStateOf("") }
    LoadedTab(vm.developerCourses, "developerHome", { vm.loadDeveloperCourses() }, header = { HomeHeader(vm) }) { data ->
        KpiTiles(
            "Courses" to "${data.total}",
            "Enabled" to "${data.total - data.disabled}",
            "Disabled" to "${data.disabled}",
        )
        Segments(CourseFilter.entries.map { it.label }, filter.ordinal, "courseFilter") { filter = CourseFilter.entries[it] }
        SearchField(query, "Search title or course code") { query = it }
        val shown = data.courses.filter { c ->
            when (filter) {
                CourseFilter.ALL -> true
                CourseFilter.ENABLED -> c.enabled
                CourseFilter.DISABLED -> !c.enabled
            } && c.matches(query)
        }
        Text(
            "${shown.size} course${if (shown.size == 1) "" else "s"}", fontSize = 12.sp, color = Theme.secondaryText,
            modifier = Modifier.padding(horizontal = 4.dp).testTag("courseCount"),
        )
        if (shown.isEmpty()) {
            EmptyState(Icons.AutoMirrored.Filled.LibraryBooks, "No courses found", "Try another filter or search.")
        }
        shown.forEach { StaffCourseCard(it) { push(Route.DevCourse(it.id, it.sku)) } }
    }
}

@Composable
private fun StaffCourseCard(item: StaffCourse, onOpen: () -> Unit) {
    InfoCard(Modifier.testTag("staffCourse-${item.id}").clickable(onClick = onOpen), padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.sku.ifEmpty { "No code" }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent, maxLines = 1)
                StatusChip(if (item.enabled) "Enabled" else "Disabled", if (item.enabled) Theme.success else Theme.danger)
            }
            Text(item.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, color = Theme.ink)
            SmallLine(
                Icons.Filled.Event,
                if (item.upcomingClasses == 0) "No upcoming classes"
                else "${item.upcomingClasses} upcoming" + if (item.nextClass.isNotEmpty()) " · next ${Fmt.longDate(item.nextClass)}" else "",
                if (item.upcomingClasses == 0) Theme.secondaryText else Theme.ink,
            )
        }
    }
}

// ── Developer course details (read only, as the admin edit page) ──

@Composable
fun DeveloperCourseScreen(vm: AppViewModel, id: Int, sku: String, onBack: () -> Unit) {
    var detail by remember { mutableStateOf<DevCourseDetail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    suspend fun load() { try { detail = vm.loadDeveloperCourse(id); error = null } catch (e: Exception) { error = e.message ?: "The course could not be loaded." } }
    LaunchedEffect(id) { load() }
    ScreenScaffold(title = sku.ifEmpty { "Course" }, onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val d = detail
            val err = error
            when {
                d != null -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(d.sku, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent)
                        StatusChip(if (d.enabled) "Enabled" else "Disabled", if (d.enabled) Theme.success else Theme.danger)
                    }
                    Text(d.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
                    DetailCard("Course details", listOf(
                        "Price" to d.price?.let { "${d.currency} %.2f".format(it) }, "Duration (hours)" to d.durationHours,
                        "Sessions" to d.sessions, "Level" to d.level, "Software" to d.software, "Programming language" to d.programmingLanguage,
                    ))
                    DetailText("Short description", d.shortDescription)
                    if (d.topics.isNotEmpty()) InfoCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Topics", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
                            d.topics.forEachIndexed { i, t ->
                                Text("${i + 1}. ${t.title}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
                                t.subTopics.forEach { Text("• $it", fontSize = 13.sp, color = Theme.secondaryText) }
                            }
                        }
                    }
                    DetailText("Prerequisite", d.prerequisite)
                    DetailText("Additional note", d.additionalNote)
                    DetailText("Trainer profile", d.trainerProfile)
                    if (d.classes.isNotEmpty()) InfoCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Upcoming classes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
                            d.classes.forEach {
                                Text("${it.classId} · ${Fmt.longDate(it.startDate)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
                                Text("${it.startTime}–${it.endTime} · ${it.mode} · vacancy ${it.vacancy}", fontSize = 13.sp, color = Theme.secondaryText)
                            }
                        }
                    }
                    DetailCard("Search listing", listOf(
                        "Meta title" to d.metaTitle, "Meta description" to d.metaDescription, "Meta keywords" to d.metaKeywords, "Page" to d.url,
                    ))
                }
                err != null -> ErrorCard(err) { scope.launch { load() } }
                else -> LoadingBox("Loading course…")
            }
        }
    }
}

@Composable
private fun DetailText(title: String, value: String) {
    if (value.isNotBlank()) InfoCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
            Text(value, fontSize = 14.sp, color = Theme.ink)
        }
    }
}

@Composable
private fun DetailCard(title: String, rows: List<Pair<String, String?>>) {
    val shown = rows.filter { !it.second.isNullOrBlank() }
    if (shown.isEmpty()) return
    InfoCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
            shown.forEach { (k, v) ->
                Text(k, fontSize = 12.sp, color = Theme.secondaryText)
                Text(v.orEmpty(), fontSize = 14.sp, color = Theme.ink)
            }
        }
    }
}

// ── Admin Calendar ──

/** The TIA month grid fed by `staff/classes?from&to` for the visible month, then the chosen day's classes. */
@Composable
fun AdminCalendarScreen(vm: AppViewModel) {
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val month = YearMonth.parse(monthKey)
    var selected by rememberSaveable { mutableStateOf(today.toString()) }
    var error by rememberSaveable(monthKey) { mutableStateOf<String?>(null) }

    suspend fun load(key: String) {
        try { vm.loadAdminCalendar(key); error = null } catch (e: Exception) { error = e.message ?: "The calendar could not be loaded." }
    }
    LaunchedEffect(monthKey) { if (monthKey !in vm.adminCalendar) load(monthKey) }

    val classes = vm.adminCalendar[monthKey]
    // Every month loaded so far (a class that spans two months is in both replies once).
    val byDay = HashMap<String, MutableList<StaffClass>>().apply {
        vm.adminCalendar.values.flatten().distinctBy { it.runId }.forEach { c -> c.days.forEach { getOrPut(it) { mutableListOf() }.add(c) } }
    }
    val nextDay = byDay.keys.filter { it >= today.toString() && it != selected }.minOrNull()

    ScreenScaffold { padding ->
        Refreshable({ load(monthKey) }, Modifier.padding(padding)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MonthCalendar(
                    month, { monthKey = it.toString() }, selected, { selected = it }, today,
                    titles = { day -> byDay[day].orEmpty().map { it.title } },
                    onToday = { monthKey = YearMonth.from(today).toString(); selected = today.toString() },
                )
                CalendarLegend(classes?.size ?: 0)
                Text(Fmt.longDate(selected), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Theme.secondaryText, modifier = Modifier.padding(top = 4.dp))
                val err = error
                val day = byDay[selected].orEmpty()
                when {
                    classes == null && err != null -> ErrorCard(err) { scope.launch { load(monthKey) } }
                    classes == null -> LoadingBox("Loading classes…")
                    selected.take(7) !in vm.adminCalendar -> Text("Pick a day in ${month.atDay(1).format(Fmt.monthYear)}.", fontSize = 14.sp, color = Theme.secondaryText)
                    day.isEmpty() -> NoClassesCard(nextDay) { selected = it }
                    else -> day.forEach { StaffClassCard(it, showTime = true) }
                }
            }
        }
    }
}
