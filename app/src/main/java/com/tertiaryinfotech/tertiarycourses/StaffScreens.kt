package com.tertiaryinfotech.tertiarycourses

import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

// Admin and Developer homes and the admin calendar, in the TIA TMS layout. Read-only apart from
// the Developer's Practice Exam switch; the server checks the role on every call
// (`staff/classes`, `staff/courses`, `staff/course`, `staff/practiceexam`).

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
                if (item.courseType.isNotBlank()) StatusChip(item.courseType, Theme.accent)
            }
            Text(item.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, color = Theme.ink)
            if (item.trainingHours != null || item.assessmentHours != null) {
                SmallLine(Icons.Filled.Schedule, "${devHours(item.trainingHours)} training · ${devHours(item.assessmentHours)} assessment")
            }
        }
    }
}

// ── Developer course page ──

/**
 * A course for the Developer role: its identity, funding validity, type and hours, every
 * courseware link, and the Practice Exam switch. No description or class information.
 */
@Composable
fun DeveloperCourseScreen(vm: AppViewModel, id: Int, sku: String, push: (Route) -> Unit, onBack: () -> Unit) {
    var detail by remember { mutableStateOf<DevCourseDetail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    suspend fun load() { try { detail = vm.loadDeveloperCourse(id); error = null } catch (e: Exception) { error = e.message ?: "The course could not be loaded." } }
    LaunchedEffect(id) { load() }
    ScreenScaffold(title = sku.ifEmpty { "Course" }, onBack = onBack) { padding ->
        Refreshable({ load() }, Modifier.padding(padding)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
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
                        val training = d.trainingHours ?: d.durationHours.toDoubleOrNull()
                        DetailCard("Course details", listOf(
                            "Course Title" to d.title,
                            "Course Code" to d.sku,
                            "Funding Validity" to fundingValidity(d),
                            "Course Type" to d.courseType.ifBlank { if (d.sku.uppercase().startsWith("TGS-")) "WSQ" else "Non-WSQ" },
                            "Duration" to (d.durationDays?.let { if (it > 0) "$it day${if (it == 1) "" else "s"}" else "Not set" }
                                ?: d.durationHours.takeIf { it.isNotBlank() }?.let { "$it hours" }),
                            "Training Hours" to devHours(training),
                            "Assessment Hours" to devHours(d.assessmentHours),
                        ))
                        InfoCard(Modifier.testTag("devCourseware")) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Courseware", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
                                if (d.courseware.isEmpty()) {
                                    Text("No courseware links saved yet.", fontSize = 14.sp, color = Theme.secondaryText)
                                }
                                d.courseware.forEach { link ->
                                    CoursewareLinkRow(link, open = { openDocument(context, push, link.label, link.url) })
                                }
                            }
                        }
                        d.practiceExam?.let { key(it) { PracticeExamSwitch(vm, d.id, it) } }
                    }
                    err != null -> ErrorCard(err) { scope.launch { load() } }
                    else -> LoadingBox("Loading course…")
                }
            }
        }
    }
}

/** The SSG funding window of a WSQ course; non-WSQ courses have none (the web shows NA). */
private fun fundingValidity(d: DevCourseDetail): String {
    if (d.fundingValidityEnd.isBlank()) return "Not applicable (non-funded course)"
    val start = if (d.fundingValidityStart.isBlank()) "N/A" else Fmt.longDate(d.fundingValidityStart)
    return "$start – ${Fmt.longDate(d.fundingValidityEnd)}"
}

/** One courseware link: its name and the link itself. Tap to open; long-press for Copy / Share / Open in browser. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CoursewareLinkRow(link: DevCoursewareLink, open: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var menu by remember { mutableStateOf(false) }
    val icon = when {
        link.key == "courseware_link" || link.key == "lab_url" -> Icons.Filled.Folder
        link.key.contains("slides") -> Icons.Filled.Slideshow
        link.key == "practice_exam_url" -> Icons.Filled.Verified
        else -> Icons.Filled.Description
    }
    Box {
        Row(
            Modifier.fillMaxWidth()
                .combinedClickable(onClick = open, onLongClick = { menu = true }, onLongClickLabel = "Copy or share the link")
                .padding(vertical = 4.dp)
                .testTag("courseware-${link.key}"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = Theme.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(link.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
                Text(link.url, fontSize = 12.sp, color = Theme.secondaryText, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Theme.secondaryText, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Theme.card) {
            DropdownMenuItem(
                text = { Text("Copy link", color = Theme.ink) },
                leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = Theme.ink) },
                onClick = {
                    menu = false
                    clipboard.setText(AnnotatedString(link.url))
                    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                },
            )
            DropdownMenuItem(
                text = { Text("Share", color = Theme.ink) },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null, tint = Theme.ink) },
                onClick = {
                    menu = false
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, link.label).putExtra(Intent.EXTRA_TEXT, link.url)
                    context.startActivity(Intent.createChooser(send, link.label))
                },
            )
            DropdownMenuItem(
                text = { Text("Open in browser", color = Theme.ink) },
                leadingIcon = { Icon(Icons.Filled.Language, contentDescription = null, tint = Theme.ink) },
                onClick = { menu = false; openUrl(context, link.url) },
            )
        }
    }
}

/**
 * The course's Practice Exam switch: on links the course to one exam on Tertiary Exams, off
 * removes it. Saved on the website straight away (`staff/practiceexam`).
 */
@Composable
private fun PracticeExamSwitch(vm: AppViewModel, courseId: Int, initial: PracticeExamSetting) {
    val scope = rememberCoroutineScope()
    var setting by remember { mutableStateOf(initial) }
    var on by remember { mutableStateOf(initial.enabled) }
    var slug by remember { mutableStateOf(initial.examSlug) }
    var saving by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    var picker by remember { mutableStateOf(false) }

    fun save(value: String) {
        saving = true
        problem = null
        scope.launch {
            try {
                setting = vm.setPracticeExam(courseId, value)
            } catch (e: Exception) {
                problem = e.message ?: "The practice exam could not be saved."
            }
            on = setting.enabled
            slug = setting.examSlug
            saving = false
        }
    }
    fun picked(value: String) {
        slug = value
        if (value.isNotEmpty() && value != setting.examSlug) save(value)
    }
    // Off saves at once; on waits for an exam to be chosen (or saves the only one there is).
    fun switched(value: Boolean) {
        problem = null
        on = value
        if (!value) {
            slug = ""
            if (setting.enabled) save("")
        } else if (setting.exams.size == 1) {
            picked(setting.exams[0].slug)
        }
    }

    val current = setting.exams.firstOrNull { it.slug == setting.examSlug }?.title ?: setting.examCode
    InfoCard(Modifier.testTag("practiceExamCard")) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Practice Exam", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
                    Text(if (setting.enabled) "On · $current" else "Off", fontSize = 13.sp, color = Theme.secondaryText)
                }
                Switch(
                    checked = on,
                    onCheckedChange = { switched(it) },
                    enabled = setting.configured && !saving,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White, checkedTrackColor = Theme.brand, checkedBorderColor = Theme.brand,
                    ),
                    modifier = Modifier.testTag("practiceExamToggle").semantics { contentDescription = "Practice Exam" },
                )
            }
            when {
                !setting.configured -> Text("Practice exams are not set up on this site.", fontSize = 13.sp, color = Theme.secondaryText)
                on && setting.exams.isEmpty() ->
                    Text(setting.warning.ifBlank { "No practice exams are available." }, fontSize = 13.sp, color = Theme.warning)
                on -> Box {
                    val chosen = setting.exams.firstOrNull { it.slug == slug }
                    OutlinedButton(
                        onClick = { picker = true }, enabled = !saving,
                        modifier = Modifier.fillMaxWidth().testTag("practiceExamPicker"),
                    ) {
                        Text(
                            chosen?.let { "${it.code} · ${it.title}" } ?: "Choose an exam",
                            color = Theme.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Theme.ink)
                    }
                    DropdownMenu(expanded = picker, onDismissRequest = { picker = false }, containerColor = Theme.card) {
                        setting.exams.forEach { exam ->
                            DropdownMenuItem(
                                text = { Text("${exam.code} · ${exam.title}", color = Theme.ink) },
                                onClick = { picker = false; picked(exam.slug) },
                            )
                        }
                    }
                }
            }
            if (saving) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Theme.accent)
            problem?.let { Text(it, fontSize = 13.sp, color = Theme.danger) }
            Text(
                "Learners and trainers of this course's classes get a Practice Exam section on the class page.",
                fontSize = 12.sp, color = Theme.secondaryText,
            )
        }
    }
}

@Composable
private fun DetailCard(title: String, rows: List<Pair<String, String?>>) {
    val shown = rows.filter { !it.second.isNullOrBlank() }
    if (shown.isEmpty()) return
    InfoCard(Modifier.testTag("devCourseDetails")) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
            shown.forEach { (k, v) ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(k, fontSize = 14.sp, color = Theme.secondaryText)
                    Spacer(Modifier.width(12.dp))
                    Text(v.orEmpty(), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Theme.ink, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                }
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
