package com.tertiaryinfotech.tertiarycourses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** Pull-to-refresh wrapper used by every list screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Refreshable(onRefresh: suspend () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { scope.launch { refreshing = true; try { onRefresh() } finally { refreshing = false } } },
        modifier = modifier.fillMaxSize(),
    ) { content() }
}

// ── Home header ──

/**
 * Top of the home tab (the staff Dashboard, or My Classes): a welcome, a link to the
 * profile, and (for the owner account only) the role switcher.
 */
@Composable
fun HomeHeader(vm: AppViewModel) {
    var roleMenu by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarView(vm.avatar, vm.learner?.fullName.orEmpty(), 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Welcome", fontSize = 15.sp, color = Theme.secondaryText)
                Text(
                    vm.learner?.firstName?.ifBlank { null } ?: "back", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Theme.accentSoft)
                    .clickable { vm.learnerTab = AppViewModel.TAB_ACCOUNT }
                    .padding(horizontal = 12.dp, vertical = 8.dp).testTag("myProfileLink"),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Theme.accent, modifier = Modifier.size(18.dp))
                Text("My Profile", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent, maxLines = 1)
            }
        }
        if (vm.canSwitchRole) {
            Box {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Theme.brand).clickable { roleMenu = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp).testTag("viewAsPicker"),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(
                        "Viewing as ${AppViewModel.roleName(vm.activeRole)}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = Color.White, modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = roleMenu, onDismissRequest = { roleMenu = false }, containerColor = Theme.card) {
                    vm.availableRoles.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(AppViewModel.roleName(role)) },
                            trailingIcon = { if (role == vm.activeRole) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Theme.accent) },
                            onClick = { roleMenu = false; vm.switchRole(role) },
                            modifier = Modifier.testTag("viewAs-$role"),
                        )
                    }
                }
            }
        }
    }
}

// ── My Classes ──

@Composable
fun ClassesScreen(vm: AppViewModel, push: (Route) -> Unit) {
    val classes = vm.classes
    val bucket = vm.bucket ?: Bucket.CURRENT
    fun items(b: Bucket) = when (b) {
        Bucket.CURRENT -> classes?.current
        Bucket.UPCOMING -> classes?.upcoming
        Bucket.PAST -> classes?.past
    } ?: emptyList()
    val scope = rememberCoroutineScope()

    ScreenScaffold { padding ->
        Refreshable({ vm.refreshClasses() }, Modifier.padding(padding)) {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (vm.staffRole == null) item { HomeHeader(vm) }
                item {
                    BucketPicker(bucket, count = { items(it).size }) { vm.bucket = it }
                }
                val err = vm.loadError
                if (classes != null && err != null) item { ErrorCard(err) { scope.launch { vm.refreshClasses() } } }
                when {
                    classes == null -> item {
                        if (err != null) ErrorCard(err) { scope.launch { vm.refreshClasses() } }
                        else LoadingBox("Loading your classes…")
                    }
                    items(bucket).isEmpty() -> item {
                        EmptyState(Icons.Filled.EventBusy, emptyTitle(bucket), emptyText(bucket, vm.isTrainer))
                    }
                    else -> items(items(bucket), key = { it.id }) { item ->
                        ClassCard(item, showRole = vm.isTrainer, onClick = classRoute(item)?.let { r -> { push(r) } })
                    }
                }
            }
        }
    }
}

@Composable
private fun BucketPicker(selected: Bucket, count: (Bucket) -> Int, onSelect: (Bucket) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        Bucket.entries.forEachIndexed { i, b ->
            SegmentedButton(
                selected = selected == b,
                onClick = { onSelect(b) },
                shape = SegmentedButtonDefaults.itemShape(i, Bucket.entries.size),
                icon = {},
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = Theme.card,
                    activeContentColor = Theme.accent,
                    activeBorderColor = Theme.cardBorder,
                    inactiveContainerColor = Theme.field,
                    inactiveContentColor = Theme.ink,
                    inactiveBorderColor = Theme.cardBorder,
                ),
            ) {
                Text(
                    "${b.label} (${count(b)})", maxLines = 1, fontSize = 14.sp,
                    fontWeight = if (selected == b) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

private fun emptyTitle(b: Bucket) = when (b) {
    Bucket.CURRENT -> "No class today"
    Bucket.UPCOMING -> "No upcoming classes"
    Bucket.PAST -> "No past classes yet"
}

private fun emptyText(b: Bucket, trainer: Boolean) = if (trainer) when (b) {
    Bucket.CURRENT -> "Classes you are teaching or attending today appear here, with attendance and courseware."
    Bucket.UPCOMING -> "Classes you are scheduled to teach or attend will appear here."
    Bucket.PAST -> "Classes you have taught or attended appear here."
} else when (b) {
    Bucket.CURRENT -> "Classes that are running now appear here, with check-in and your materials."
    Bucket.UPCOMING -> "Register for a course on ${Site.websiteLabel} and it will appear here."
    Bucket.PAST -> "Completed classes, your feedback and certificates appear here."
}

/** Where a class card leads: the trainer or the learner class screen (none while payment is pending). */
fun classRoute(item: ClassItem): Route? = when {
    item.runId <= 0 -> null
    item.isTeaching -> Route.TrainerClass(item.runId, item.title)
    else -> Route.ClassDetail(item.runId, item.title)
}

fun modeIcon(mode: String): ImageVector = if (mode == "Live Online") Icons.Filled.Videocam else Icons.Filled.Apartment

@Composable
fun ClassCard(item: ClassItem, showRole: Boolean = false, onClick: (() -> Unit)?) {
    InfoCard(modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier) {
        Row(verticalAlignment = Alignment.Top) {
            CourseThumb(item.imageUrl)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.classId.ifEmpty { item.sku }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent)
                    if (item.isPendingPayment) StatusChip("Pending payment", Theme.warning)
                    else if (item.bucket == "current") StatusChip("In progress", Theme.success)
                }
                // Trainers see which classes they teach and which they attend.
                if (showRole) StatusChip(if (item.isTeaching) "Teaching" else "Attending", if (item.isTeaching) Theme.accent else Theme.secondaryText)
                Text(item.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
            }
            if (onClick != null) Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Theme.tertiaryText)
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            IconLine(Icons.Filled.Event, Fmt.range(item.startDate, item.endDate), color = Theme.secondaryText, iconTint = Theme.tertiaryText)
            if (item.startTime.isNotEmpty()) IconLine(Icons.Filled.Schedule, Fmt.times(item.startTime, item.endTime), color = Theme.secondaryText, iconTint = Theme.tertiaryText)
            if (item.mode.isNotEmpty()) IconLine(modeIcon(item.mode), item.mode, color = Theme.secondaryText, iconTint = Theme.tertiaryText)
            if (item.trainer.isNotEmpty() && !item.isTeaching) IconLine(Icons.Filled.Person, item.trainer, color = Theme.secondaryText, iconTint = Theme.tertiaryText)
            if (item.isTeaching) IconLine(Icons.Filled.Groups, if (item.learnerCount == 1) "1 learner" else "${item.learnerCount} learners", color = Theme.secondaryText, iconTint = Theme.tertiaryText)
        }
        if (item.certificate != null || item.surveySubmitted) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (item.certificate != null) StatusChip("Certificate issued", Theme.accent)
                if (item.surveySubmitted) StatusChip("Feedback done", Theme.success)
            }
        }
        if (item.isPendingPayment) {
            Spacer(Modifier.height(8.dp))
            Text("Your seat is confirmed once payment for order #${item.orderNo} is received.", fontSize = 12.sp, color = Theme.secondaryText)
        }
    }
}

// ── Class detail ──

@Composable
fun ClassDetailScreen(vm: AppViewModel, runId: Int, title: String, push: (Route) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<ClassDetail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var checkingIn by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        try {
            detail = vm.classDetail(runId); error = null
        } catch (e: Exception) {
            error = e.message
        }
    }
    LaunchedEffect(runId) { load() }

    ScreenScaffold(title = title, onBack = onBack) { padding ->
        Refreshable({ load() }, Modifier.padding(padding)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val d = detail
                when {
                    d != null -> {
                        Overview(d) { openUrl(context, d.virtualLink) }
                        AttendanceCard(d, checkingIn) {
                            scope.launch {
                                checkingIn = true
                                try {
                                    notice = vm.markAttendance(runId)
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    load()
                                } catch (e: Exception) {
                                    notice = e.message
                                } finally {
                                    checkingIn = false
                                }
                            }
                        }
                        MaterialsList("Class materials", (d.materials + d.documents).filter { it.url.isNotEmpty() },
                            "Your trainer will share the materials for this class here.") { push(Route.Document(it.title, it.url)) }
                        if (d.quizzes.isNotEmpty()) QuizzesCard(d.quizzes) { push(Route.Quiz(runId, it.quizId, it.title)) }
                        SurveyCard(d, openExternal = { openUrl(context, d.survey.externalUrl) }) { push(Route.Survey(runId)) }
                        CertificateCard(d) { push(Route.Document("Certificate", it.url)) }
                        SessionsCard(d)
                    }
                    error != null -> ErrorCard(error!!) { scope.launch { load() } }
                    else -> LoadingBox()
                }
            }
        }
    }

    notice?.let { msg ->
        AlertDialog(
            onDismissRequest = { notice = null },
            confirmButton = { TextButton(onClick = { notice = null }) { Text("OK") } },
            title = { Text("Attendance") },
            text = { Text(msg) },
        )
    }
}

@Composable
private fun Overview(d: ClassDetail, join: () -> Unit) {
    InfoCard {
        Text(d.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
        Spacer(Modifier.height(6.dp))
        Text(listOf(d.classId, d.sku).filter { it.isNotEmpty() }.joinToString(" · "), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent)
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Theme.cardBorder)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconLine(Icons.Filled.Event, Fmt.range(d.startDate, d.endDate), fontSize = 15, iconTint = Theme.ink)
            if (d.startTime.isNotEmpty()) IconLine(Icons.Filled.Schedule, Fmt.times(d.startTime, d.endTime), fontSize = 15, iconTint = Theme.ink)
            if (d.mode.isNotEmpty()) IconLine(modeIcon(d.mode), d.mode, fontSize = 15, iconTint = Theme.ink)
            val place = listOf(d.room, d.venue).filter { it.isNotEmpty() }
            if (place.isNotEmpty()) IconLine(Icons.Filled.Place, place.joinToString("\n"), fontSize = 15, iconTint = Theme.ink)
            if (d.trainer.isNotEmpty()) IconLine(Icons.Filled.Person, "Trainer: ${d.trainer}", fontSize = 15, iconTint = Theme.ink)
        }
        if (d.virtualLink.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            PrimaryButton("Join Online Class", Icons.Filled.Videocam, onClick = join)
        }
    }
}

@Composable
private fun AttendanceCard(d: ClassDetail, checkingIn: Boolean, checkIn: () -> Unit) {
    val a = d.attendance
    InfoCard {
        SectionLabel("Digital attendance")
        Spacer(Modifier.height(10.dp))
        when {
            a.markedPresent -> IconLine(
                Icons.Filled.CheckCircle, "You're checked in for ${a.currentSessionLabel}.",
                color = Theme.success, iconTint = Theme.success, fontSize = 15, weight = FontWeight.SemiBold,
            )
            a.open -> {
                Text("Check in for ${a.currentSessionLabel} when you arrive.", fontSize = 15.sp)
                Spacer(Modifier.height(10.dp))
                PrimaryButton("Check In Now", Icons.Filled.PanTool, busy = checkingIn, modifier = Modifier.testTag("checkIn"), onClick = checkIn)
            }
            else -> Text(
                if (d.bucket == "upcoming") "Check-in opens on the first day of class." else "Check-in is closed for this class.",
                fontSize = 15.sp, color = Theme.secondaryText,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text("Attended ${a.sessionsPresent} of ${a.sessionsTotal} sessions", fontSize = 12.sp, color = Theme.secondaryText)
    }
}

fun materialIcon(m: Material): ImageVector = when (m.key) {
    "learner_slides_url", "trainer_slides_url" -> Icons.Filled.Slideshow
    "learner_guide_url", "facilitator_guide_url" -> Icons.AutoMirrored.Filled.MenuBook
    "lesson_plan_url", "assessment_plan_url" -> Icons.AutoMirrored.Filled.ListAlt
    "lab_url", "lab_simulator_url" -> Icons.Filled.Science
    "practice_exam_url" -> Icons.Filled.Quiz
    "courseware_folder_url" -> Icons.Filled.Folder
    else -> Icons.Filled.Description
}

/** Quizzes the course developer added to the Lesson, with the learner's latest score. */
@Composable
private fun QuizzesCard(quizzes: List<QuizSummary>, open: (QuizSummary) -> Unit) {
    InfoCard {
        SectionLabel("Quizzes")
        Spacer(Modifier.height(6.dp))
        quizzes.forEachIndexed { i, q ->
            val count = "${q.questions} question${if (q.questions == 1) "" else "s"}"
            Row(
                Modifier.fillMaxWidth().clickable(onClickLabel = if (q.previous == null) "Start quiz" else "Retake quiz") { open(q) }
                    .padding(vertical = 12.dp).testTag("quiz-${q.quizId}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (q.previous == null) Icons.Filled.Quiz else Icons.Filled.CheckCircle, contentDescription = null,
                    tint = if (q.previous == null) Theme.accent else Theme.success, modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(q.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
                    Text(
                        q.previous?.let { "$count · your score ${it.score} / ${it.total}" } ?: count,
                        fontSize = 13.sp, color = Theme.secondaryText,
                    )
                }
                Text(if (q.previous == null) "Start" else "Retake", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent)
            }
            if (i < quizzes.lastIndex) HorizontalDivider(color = Theme.cardBorder)
        }
    }
}

@Composable
private fun SurveyCard(d: ClassDetail, openExternal: () -> Unit, give: () -> Unit) {
    val s = d.survey
    InfoCard {
        SectionLabel("Course feedback")
        Spacer(Modifier.height(10.dp))
        when {
            s.submitted -> IconLine(Icons.Filled.Verified, s.message, color = Theme.success, iconTint = Theme.success, fontSize = 15, weight = FontWeight.SemiBold)
            s.externalUrl.isNotEmpty() -> PrimaryButton("Open Feedback Form", Icons.Filled.EditNote, onClick = openExternal)
            s.available -> {
                Text("Tell us how the class went — it takes about a minute.", fontSize = 15.sp)
                Spacer(Modifier.height(10.dp))
                PrimaryButton("Give Feedback", Icons.Filled.RateReview, modifier = Modifier.testTag("giveFeedback"), onClick = give)
            }
            else -> Text(s.message, fontSize = 15.sp, color = Theme.secondaryText)
        }
    }
}

@Composable
private fun CertificateCard(d: ClassDetail, open: (CertificateInfo) -> Unit) {
    InfoCard {
        SectionLabel("Certificate")
        Spacer(Modifier.height(10.dp))
        val cert = d.certificate
        if (cert != null && cert.url.isNotEmpty()) {
            IconLine(Icons.Filled.WorkspacePremium, "${cert.name} · ${cert.certNo}", fontSize = 15, weight = FontWeight.SemiBold, iconTint = Theme.accent)
            Spacer(Modifier.height(10.dp))
            OutlineButton("View Certificate", Icons.Filled.Description) { open(cert) }
        } else {
            val rule = if (d.certificateRule.isNotEmpty()) " (attendance ${d.certificateRule})" else ""
            Text(
                if (d.bucket == "past") "Your certificate will appear here once it is issued$rule."
                else "Your certificate is issued after the class when attendance requirements are met$rule.",
                fontSize = 15.sp, color = Theme.secondaryText,
            )
        }
    }
}

@Composable
private fun SessionsCard(d: ClassDetail) {
    if (d.sessions.isEmpty()) return
    InfoCard {
        SectionLabel("Sessions")
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            d.sessions.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.label, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    val (icon, tint, label) = when (s.present) {
                        true -> Triple(Icons.Filled.CheckCircle, Theme.success, "Present")
                        false -> Triple(Icons.Filled.HighlightOff, Theme.secondaryText, "Absent")
                        null -> Triple(Icons.Filled.RadioButtonUnchecked, Theme.tertiaryText, "Not recorded")
                    }
                    Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

// ── Survey ──

/** The site's in-house course feedback survey, rendered from its live template. */
@Composable
fun SurveyScreen(vm: AppViewModel, runId: Int, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var survey by remember { mutableStateOf<Survey?>(null) }
    val answers = remember { mutableStateMapOf<String, String>() }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        try {
            val s = vm.survey(runId)
            survey = s
            s.sections.flatMap { it.fields }.filter { !it.readonly && it.id !in answers }.forEach { answers[it.id] = it.value }
            error = null
        } catch (e: Exception) {
            error = e.message
        }
    }
    LaunchedEffect(runId) { load() }

    val missing = survey?.sections?.flatMap { it.fields }?.filter {
        it.required && !it.readonly && answers[it.id].orEmpty().isBlank()
    } ?: emptyList()

    ScreenScaffold(
        title = survey?.title?.ifEmpty { null } ?: "Course Feedback",
        onBack = onClose,
        navigationIcon = Icons.Filled.Close,
        actions = {
            TextButton(
                onClick = {
                    scope.launch {
                        submitting = true
                        try {
                            done = vm.submitSurvey(runId, answers.toMap())
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            submitting = false
                        }
                    }
                },
                enabled = survey != null && missing.isEmpty() && !submitting,
                modifier = Modifier.testTag("submitSurvey"),
            ) { Text("Submit", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val s = survey
            if (s != null) {
                s.sections.forEach { section ->
                    if (section.title.isNotEmpty()) SectionLabel(section.title, Modifier.padding(top = 10.dp, start = 4.dp))
                    InfoCard(padding = 0.dp) {
                        section.fields.forEachIndexed { i, f ->
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                SurveyFieldView(f, answers[f.id].orEmpty()) { answers[f.id] = it }
                            }
                            if (i < section.fields.lastIndex) HorizontalDivider(Modifier.padding(start = 16.dp), color = Theme.cardBorder)
                        }
                    }
                }
                error?.let { Text(it, color = Theme.danger, fontSize = 13.sp) }
                if (missing.isNotEmpty()) {
                    Text("* Required — answer every starred question to submit.", fontSize = 12.sp, color = Theme.secondaryText)
                }
                Spacer(Modifier.height(24.dp))
            } else if (error != null) {
                ErrorCard(error!!) { scope.launch { load() } }
            } else {
                LoadingBox()
            }
        }
    }

    done?.let { msg ->
        AlertDialog(
            onDismissRequest = { done = null; onClose() },
            confirmButton = { TextButton(onClick = { done = null; onClose() }) { Text("Done") } },
            title = { Text("Thank you!") },
            text = { Text(msg) },
        )
    }
}

// ── Quiz ──

/**
 * A lesson quiz: pick one option per question, submit, see the score. The site
 * scores the answers and records the attempt against the learner and the class.
 */
@Composable
fun QuizScreen(vm: AppViewModel, runId: Int, quizId: Int, title: String, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var quiz by remember { mutableStateOf<Quiz?>(null) }
    val answers = remember { mutableStateMapOf<String, Int>() }
    var result by remember { mutableStateOf<QuizResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

    suspend fun load() {
        try {
            quiz = vm.quiz(runId, quizId); error = null
        } catch (e: Exception) {
            error = e.message
        }
    }
    LaunchedEffect(quizId) { load() }

    val unanswered = quiz?.questions?.count { it.id !in answers } ?: 0

    ScreenScaffold(
        title = quiz?.title?.ifEmpty { null } ?: title,
        onBack = onClose,
        navigationIcon = Icons.Filled.Close,
        actions = {
            if (result == null) {
                TextButton(
                    onClick = {
                        scope.launch {
                            submitting = true
                            try {
                                result = vm.submitQuiz(runId, quizId, answers.toMap()); error = null
                            } catch (e: Exception) {
                                error = e.message
                            } finally {
                                submitting = false
                            }
                        }
                    },
                    enabled = quiz != null && unanswered == 0 && !submitting,
                    modifier = Modifier.testTag("submitQuiz"),
                ) { Text(if (submitting) "Submitting…" else "Submit", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val q = quiz
            val r = result
            if (q != null) {
                if (r != null) {
                    InfoCard(Modifier.testTag("quizScore").semantics(mergeDescendants = true) {}) {
                        Text("Your score", fontSize = 14.sp, color = Theme.secondaryText)
                        Text("${r.score} / ${r.total}", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Theme.ink)
                        Text("Your score has been recorded.", fontSize = 13.sp, color = Theme.secondaryText)
                    }
                }
                q.questions.forEachIndexed { qi, question ->
                    val marked = r?.breakdown?.firstOrNull { it.id == question.id }
                    Text(
                        "${qi + 1}. ${question.question}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink,
                        modifier = Modifier.padding(top = 10.dp, start = 4.dp),
                    )
                    InfoCard(padding = 0.dp) {
                        question.options.forEachIndexed { oi, option ->
                            val chosen = answers[question.id] == oi
                            val isCorrect = marked?.correctIndex == oi
                            val wrongPick = marked != null && chosen && !isCorrect
                            val state = when {
                                marked == null -> if (chosen) "Selected" else "Not selected"
                                isCorrect -> "Correct answer"
                                wrongPick -> "Your answer, incorrect"
                                else -> ""
                            }
                            Row(
                                Modifier.fillMaxWidth()
                                    .selectable(selected = chosen, enabled = r == null, role = Role.RadioButton) { answers[question.id] = oi }
                                    .semantics { stateDescription = state }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    when {
                                        marked == null -> if (chosen) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked
                                        isCorrect -> Icons.Filled.CheckCircle
                                        wrongPick -> Icons.Filled.HighlightOff
                                        else -> Icons.Filled.RadioButtonUnchecked
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        marked == null -> if (chosen) Theme.accent else Theme.secondaryText
                                        isCorrect -> Theme.success
                                        wrongPick -> Theme.danger
                                        else -> Theme.secondaryText
                                    },
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(option, fontSize = 16.sp, color = Theme.ink, modifier = Modifier.weight(1f))
                            }
                            if (oi < question.options.lastIndex) HorizontalDivider(Modifier.padding(start = 16.dp), color = Theme.cardBorder)
                        }
                    }
                }
                error?.let { Text(it, color = Theme.danger, fontSize = 13.sp) }
                if (r != null) {
                    TonalButton("Take the Quiz Again") { answers.clear(); result = null }
                } else if (unanswered > 0) {
                    Text("Answer every question to submit ($unanswered left).", fontSize = 13.sp, color = Theme.secondaryText)
                }
                Spacer(Modifier.height(24.dp))
            } else if (error != null) {
                ErrorCard(error!!) { scope.launch { load() } }
            } else {
                LoadingBox()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurveyFieldView(f: SurveyField, value: String, onChange: (String) -> Unit) {
    val label = f.label + if (f.required) " *" else ""
    if (f.readonly) {
        LabeledValue(f.label, if (f.type == "date") Fmt.longDate(f.value) else f.value, fontSize = 15)
        return
    }
    when (f.type) {
        "rating1to5" -> {
            Text(label, fontSize = 15.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(8.dp))
            StarRating(value.toIntOrNull() ?: 0) { onChange(it.toString()) }
        }
        "select" -> {
            var expanded by remember { mutableStateOf(false) }
            Text(label, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = value.ifEmpty { "Select…" }, onValueChange = {}, readOnly = true, singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    f.options.forEach { opt ->
                        DropdownMenuItem(text = { Text(opt) }, onClick = { onChange(opt); expanded = false })
                    }
                }
            }
        }
        else -> {
            Text(label, fontSize = 15.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(6.dp))
            val multi = f.type == "textarea"
            val email = f.type == "email"
            OutlinedTextField(
                value = value, onValueChange = onChange,
                placeholder = { Text(if (email) "you@example.com" else "Your answer") },
                singleLine = !multi, minLines = if (multi) 3 else 1, maxLines = if (multi) 8 else 1,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (email) KeyboardType.Email else KeyboardType.Text,
                    capitalization = if (email) KeyboardCapitalization.None else KeyboardCapitalization.Sentences,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun StarRating(value: Int, onChange: (Int) -> Unit) {
    val labels = listOf("", "Poor", "Fair", "Good", "Very Good", "Excellent")
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) {
            IconButton(
                onClick = { onChange(i) },
                modifier = Modifier.size(44.dp).semantics { contentDescription = "$i star${if (i == 1) "" else "s"}, ${labels[i]}" },
            ) {
                Icon(
                    if (i <= value) Icons.Filled.Star else Icons.Filled.StarBorder, contentDescription = null,
                    tint = if (i <= value) Theme.star else Theme.tertiaryText, modifier = Modifier.size(34.dp),
                )
            }
        }
        Text(labels[value.coerceIn(0, 5)], fontSize = 13.sp, color = Theme.secondaryText, modifier = Modifier.padding(start = 6.dp))
    }
}
