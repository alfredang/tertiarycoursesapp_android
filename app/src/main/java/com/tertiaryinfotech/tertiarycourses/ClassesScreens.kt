package com.tertiaryinfotech.tertiarycourses

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.School
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RateReview
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
 * Top of the home tab (the staff Dashboard, or My Classes), in the TIA house layout: a
 * "Welcome back" card with the name, the role and a My Profile link (the whole card opens
 * Account), then, for an account that holds more than one role, a "View as" row to switch role.
 */
@Composable
fun HomeHeader(vm: AppViewModel) {
    var roleMenu by remember { mutableStateOf(false) }
    val fullName = vm.learner?.fullName.orEmpty()
    val role = AppViewModel.roleName(vm.activeRole)
    val shape = RoundedCornerShape(18.dp)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(shape).background(Theme.card).border(1.dp, Theme.cardBorder, shape)
                .clickable(onClickLabel = "My Profile", role = Role.Button) { vm.learnerTab = AppViewModel.TAB_ACCOUNT }
                .padding(18.dp).testTag("myProfileLink"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Welcome back", fontSize = 15.sp, color = Theme.secondaryText, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Theme.tertiaryText, modifier = Modifier.size(20.dp))
            }
            ShrinkingName(if (fullName.isBlank()) "Welcome" else fullName.uppercase(), Theme.ink)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    role, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent, maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Theme.accentSoft).padding(horizontal = 12.dp, vertical = 5.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Theme.accent, modifier = Modifier.size(20.dp))
                    Text("My Profile", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent, maxLines = 1)
                }
            }
        }
        if (vm.canSwitchRole) {
            Box {
                Row(
                    Modifier.fillMaxWidth().clip(shape).background(Theme.card).border(1.dp, Theme.cardBorder, shape)
                        .clickable(onClickLabel = "Change role", role = Role.DropdownList) { roleMenu = true }
                        .padding(horizontal = 18.dp, vertical = 14.dp).testTag("viewAsPicker"),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, tint = Theme.accent, modifier = Modifier.size(22.dp))
                    Text("View as", fontSize = 17.sp, color = Theme.secondaryText, modifier = Modifier.weight(1f))
                    Text(role, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink, maxLines = 1)
                    Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = Theme.secondaryText, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = roleMenu, onDismissRequest = { roleMenu = false }, containerColor = Theme.card) {
                    vm.availableRoles.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(AppViewModel.roleName(r)) },
                            trailingIcon = { if (r == vm.activeRole) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Theme.accent) },
                            onClick = { roleMenu = false; vm.switchRole(r) },
                            modifier = Modifier.testTag("viewAs-$r"),
                        )
                    }
                }
            }
        }
    }
}

/** The name in the header: heavy 26sp on up to two lines, shrinking (to 60%) until it fits. */
@Composable
private fun ShrinkingName(name: String, color: Color) {
    var size by remember(name) { mutableStateOf(26f) }
    var fitted by remember(name) { mutableStateOf(false) }
    Text(
        name, fontSize = size.sp, lineHeight = (size * 1.2f).sp, fontWeight = FontWeight.ExtraBold, color = color,
        maxLines = 2, overflow = TextOverflow.Ellipsis,
        onTextLayout = { if (it.hasVisualOverflow && size > 26f * 0.6f) size = (size - 1f).coerceAtLeast(26f * 0.6f) else fitted = true },
        modifier = Modifier.fillMaxWidth().drawWithContent { if (fitted) drawContent() },
    )
}

// ── My Classes ──

@Composable
fun ClassesScreen(vm: AppViewModel, push: (Route) -> Unit) {
    val classes = vm.classes
    val bucket = vm.bucket ?: Bucket.CURRENT
    var query by rememberSaveable { mutableStateOf("") }
    fun matches(c: ClassItem): Boolean {
        val q = query.trim()
        return q.isEmpty() || listOf(c.title, c.sku, c.classId).any { it.contains(q, ignoreCase = true) }
    }
    fun items(b: Bucket) = when (b) {
        Bucket.CURRENT -> classes?.current
        Bucket.UPCOMING -> classes?.upcoming
        Bucket.PAST -> classes?.past
    }.orEmpty().filter(::matches)
    val scope = rememberCoroutineScope()

    ScreenScaffold { padding ->
        Refreshable({ vm.refreshClasses() }, Modifier.padding(padding)) {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "search") { SearchField(query, "Find your course", Modifier.testTag("findCourse")) { query = it } }
                if (vm.staffRole == null) item(key = "header") { HomeHeader(vm) }
                item(key = "buckets") {
                    BucketPicker(bucket, count = { items(it).size }) { vm.bucket = it }
                }
                val err = vm.loadError
                when {
                    classes == null -> item {
                        if (err != null) ErrorCard(err) { scope.launch { vm.refreshClasses() } }
                        else LoadingBox("Loading your classes…")
                    }
                    items(bucket).isEmpty() && query.isNotBlank() -> item {
                        EmptyState(Icons.Filled.SearchOff, "No matching classes", "No ${bucket.label.lowercase()} class matches “${query.trim()}”. Search by course title or course code.")
                    }
                    items(bucket).isEmpty() -> item {
                        EmptyState(Icons.Filled.EventBusy, emptyTitle(bucket), emptyText(bucket, vm.isTrainer))
                    }
                    else -> items(items(bucket), key = { it.id }) { item ->
                        ClassCard(item, onClick = classRoute(item)?.let { r -> { push(r) } })
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
    Bucket.CURRENT -> "Classes running now appear here, with attendance and the class materials."
    Bucket.UPCOMING -> "Classes you are scheduled to teach or attend appear here."
    Bucket.PAST -> "Completed classes you taught or attended appear here."
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

/** Teaching / In progress / Pending payment (and the certificate and feedback marks) for a class card or page. */
fun classChips(item: ClassItem): List<Pair<String, Color>> = buildList {
    if (item.isTeaching) add("Teaching" to Theme.accent)
    if (item.isPendingPayment) add("Pending payment" to Theme.warning)
    else if (item.bucket == "current") add("In progress" to Theme.success)
    if (item.certificate != null) add("Certificate issued" to Theme.accent)
    if (item.surveySubmitted) add("Feedback done" to Theme.success)
}

/**
 * A class in My Classes and the calendar, in the TIA layout: course code; chips; title; date
 * range; the mode only for Live Online or Hybrid; the venue address; learners or the trainer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassCard(item: ClassItem, onClick: (() -> Unit)?) {
    InfoCard(modifier = (if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).testTag("classCard-${item.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.sku.ifEmpty { item.classId }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent,
                maxLines = 1, modifier = Modifier.weight(1f),
            )
            if (onClick != null) Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Theme.tertiaryText, modifier = Modifier.size(20.dp))
        }
        val chips = classChips(item)
        if (chips.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                chips.forEach { (text, color) -> StatusChip(text, color) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(item.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, color = Theme.ink)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            IconLine(Icons.Filled.Event, Fmt.range(item.startDate, item.endDate), color = Theme.secondaryText, iconTint = Theme.tertiaryText)
            if (showsMode(item.mode)) IconLine(modeIcon(item.mode), item.mode, color = Theme.secondaryText, iconTint = Theme.tertiaryText)
            if (item.venue.isNotEmpty()) IconLine(Icons.Filled.Place, item.venue, color = Theme.secondaryText, iconTint = Theme.tertiaryText)
            if (item.isTeaching) {
                item.learnerCount?.let { n -> IconLine(Icons.Filled.Groups, "$n learner${if (n == 1) "" else "s"}", color = Theme.secondaryText, iconTint = Theme.tertiaryText) }
            } else if (item.trainer.isNotEmpty()) {
                IconLine(Icons.Filled.Person, item.trainer, color = Theme.secondaryText, iconTint = Theme.tertiaryText)
            }
        }
        if (item.isPendingPayment) {
            Spacer(Modifier.height(8.dp))
            Text("Your seat is confirmed once payment for order #${item.orderNo} is received.", fontSize = 12.sp, color = Theme.secondaryText)
        }
    }
}

// ── Class page (learner) ──

/**
 * The learner's class page, as the website's: the summary card, then E-Attendance,
 * Announcements, Virtual Class Link, Courseware, Lesson, Practice Exam (when the course has
 * one), Course Survey and Certificate, each a collapsible card that starts collapsed.
 */
@Composable
fun ClassDetailScreen(vm: AppViewModel, runId: Int, title: String, push: (Route) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<ClassDetail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var signing by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val open = rememberOpenSections(runId)

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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val d = detail
                when {
                    d != null -> {
                        val item = vm.classes?.all?.firstOrNull { it.runId == runId && !it.isTeaching }
                        ClassSummaryCard(
                            d.title, d.sku, d.classId,
                            buildList {
                                if (d.bucket == "current") add("In progress" to Theme.success)
                                if (item?.isPendingPayment == true) add("Pending payment" to Theme.warning)
                            },
                            d.startDate, d.endDate, d.startTime, d.endTime, d.mode, d.venue, d.room, d.trainer, null, d.virtualLink,
                        ) { openUrl(context, d.virtualLink) }

                        val a = d.attendance
                        FoldCard(
                            "E-Attendance", Icons.Filled.CheckCircle, "E-Attendance" in open.value, { open.toggle("E-Attendance") },
                            trailing = if (a.sessionsTotal > 0) "${a.sessionsPresent}/${a.sessionsTotal}" else null,
                        ) {
                            AttendanceContent(d) { signing = true }
                        }
                        FoldCard(
                            "Announcements", Icons.Filled.Campaign, "Announcements" in open.value, { open.toggle("Announcements") },
                            trailing = d.announcements.size.takeIf { it > 0 }?.toString(),
                        ) {
                            AnnouncementList(d.announcements, openFile = { openDocument(context, push, it.fileName.ifBlank { "Attachment" }, it.fileUrl) })
                        }
                        FoldCard("Virtual Class Link", Icons.Filled.Videocam, "Virtual Class Link" in open.value, { open.toggle("Virtual Class Link") }) {
                            VirtualClassContent(d.mode, d.virtualLink) { openUrl(context, d.virtualLink) }
                        }
                        val courseware = (d.materials + d.documents).filter { it.url.isNotEmpty() }
                        FoldCard(
                            "Courseware", Icons.Filled.Folder, "Courseware" in open.value, { open.toggle("Courseware") },
                            footer = "Documents open inside the app.",
                        ) {
                            MaterialRows(courseware, "Your trainer will share the materials for this class here.") { openDocument(context, push, it.title, it.url) }
                        }
                        FoldCard("Lesson", Icons.Filled.School, "Lesson" in open.value, { open.toggle("Lesson") }) {
                            val openQuiz: (Int, String) -> Unit = { id, t -> push(Route.Quiz(runId, id, t)) }
                            if (d.lesson.isEmpty() && d.quizzes.isNotEmpty()) {
                                // A site that predates the Lesson list still sends the class's quizzes.
                                d.quizzes.forEach { q -> LearnerQuizRow(q.quizId, q.title, q.questions, q.previous, openQuiz) }
                            } else {
                                LessonContent(
                                    d.lesson,
                                    openLink = { link ->
                                        if (link.type == "video") openUrl(context, link.url) else openDocument(context, push, link.title, link.url)
                                    },
                                    quizRow = { link -> LearnerQuizRow(link.quizId, link.title, link.questions, link.previous, openQuiz) },
                                )
                            }
                        }
                        d.practiceExam?.let { pe ->
                            FoldCard(
                                "Practice Exam", Icons.Filled.Quiz, "Practice Exam" in open.value, { open.toggle("Practice Exam") },
                                trailing = pe.examCode.ifEmpty { null },
                            ) {
                                // The self-register form opens in the in-app viewer. Coming back recomposes this
                                // screen, which loads the class again, so the section shows "You are registered".
                                PracticeExamContent(
                                    pe, trainer = false,
                                    openForm = { openDocument(context, push, "${pe.examCode} Practice Exam".trim(), it) },
                                    openSite = { openUrl(context, it) },
                                )
                            }
                        }
                        FoldCard("Course Survey", Icons.Filled.RateReview, "Course Survey" in open.value, { open.toggle("Course Survey") }) {
                            SurveyContent(d, openExternal = { openUrl(context, d.survey.externalUrl) }) { push(Route.Survey(runId)) }
                        }
                        FoldCard("Certificate", Icons.Filled.WorkspacePremium, "Certificate" in open.value, { open.toggle("Certificate") }) {
                            CertificateContent(d) { openDocument(context, push, "Certificate", it.url) }
                        }
                    }
                    error != null -> ErrorCard(error!!) { scope.launch { load() } }
                    else -> LoadingBox()
                }
            }
        }
    }

    val signFor = detail
    if (signing && signFor != null) {
        val me = vm.learner
        SignatureSheet(
            title = signFor.title, sessionLabel = signFor.attendance.currentSessionLabel,
            signers = listOf(Signer("me", me?.fullName?.ifBlank { me.email } ?: "Your signature")),
            submit = { _, png -> vm.markAttendance(runId, png).also { notice = it } },
            onDismiss = { signing = false; scope.launch { load() } },
        )
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

/** Check in, the sessions with a tick for each one attended, and "Attended x of y". */
@Composable
private fun AttendanceContent(d: ClassDetail, sign: () -> Unit) {
    val a = d.attendance
    when {
        a.markedPresent -> {
            IconLine(
                Icons.Filled.CheckCircle, "You're checked in for ${a.currentSessionLabel}.",
                color = Theme.success, iconTint = Theme.success, fontSize = 15, weight = FontWeight.SemiBold,
            )
            // Marked present by the trainer but not signed yet: the learner can still sign.
            if (a.open && a.signed == false) {
                TonalButton("Add Your Signature", Icons.Filled.Draw, modifier = Modifier.testTag("addSignature"), onClick = sign)
            }
        }
        a.open -> {
            Text("Sign for ${a.currentSessionLabel} with your finger when you arrive.", fontSize = 15.sp, color = Theme.ink)
            PrimaryButton("Check In Now", Icons.Filled.Draw, modifier = Modifier.testTag("checkIn"), onClick = sign)
        }
        else -> Text(
            if (d.bucket == "upcoming") "Check-in opens on the first day of class." else "Check-in is closed for this class.",
            fontSize = 15.sp, color = Theme.secondaryText,
        )
    }
    if (d.sessions.isNotEmpty()) {
        HorizontalDivider(color = Theme.cardBorder)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            d.sessions.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.label, fontSize = 15.sp, color = Theme.ink, modifier = Modifier.weight(1f))
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
    Text("Attended ${a.sessionsPresent} of ${a.sessionsTotal} sessions", fontSize = 13.sp, color = Theme.secondaryText)
}

fun materialIcon(m: Material): ImageVector = when (m.key) {
    "learner_slides_url", "trainer_slides_url" -> Icons.Filled.Slideshow
    "learner_guide_url" -> Icons.AutoMirrored.Filled.MenuBook
    "lesson_plan_url" -> Icons.AutoMirrored.Filled.ListAlt
    "lab_url", "lab_simulator_url" -> Icons.Filled.Science
    "practice_exam_url" -> Icons.Filled.Quiz
    "facilitator_guide_url" -> Icons.Filled.AssignmentInd
    "assessment_plan_url" -> Icons.Filled.FindInPage
    else -> Icons.Filled.Description
}

/** A lesson quiz with the learner's latest score; Start or Retake opens it in the app. */
@Composable
private fun LearnerQuizRow(quizId: Int, title: String, questions: Int, previous: QuizScore?, open: (Int, String) -> Unit) {
    val count = questionCount(questions)
    LinkRow(
        if (previous == null) Icons.Filled.Quiz else Icons.Filled.CheckCircle,
        title.ifBlank { "Quiz" },
        subtitle = previous?.let { "$count · your score ${it.score} / ${it.total}" } ?: count,
        trailingText = if (previous == null) "Start" else "Retake",
        iconTint = if (previous == null) Theme.accent else Theme.success,
        tag = "quiz-$quizId",
    ) { open(quizId, title.ifBlank { "Quiz" }) }
}

@Composable
private fun SurveyContent(d: ClassDetail, openExternal: () -> Unit, give: () -> Unit) {
    val s = d.survey
    when {
        s.submitted -> IconLine(Icons.Filled.Verified, s.message.ifEmpty { "Course feedback sent. Thank you." }, color = Theme.success, iconTint = Theme.success, fontSize = 15, weight = FontWeight.SemiBold)
        s.externalUrl.isNotEmpty() -> PrimaryButton("Open Feedback Form", Icons.Filled.EditNote, onClick = openExternal)
        s.available -> {
            Text("Tell us how the class went. It takes about a minute.", fontSize = 15.sp, color = Theme.ink)
            PrimaryButton("Give Feedback", Icons.Filled.RateReview, modifier = Modifier.testTag("giveFeedback"), onClick = give)
        }
        else -> Text(s.message.ifEmpty { "The course survey opens on the first day of class." }, fontSize = 15.sp, color = Theme.secondaryText)
    }
}

@Composable
private fun CertificateContent(d: ClassDetail, open: (CertificateInfo) -> Unit) {
    val cert = d.certificate
    if (cert != null && cert.url.isNotEmpty()) {
        IconLine(Icons.Filled.WorkspacePremium, "${cert.name} · ${cert.certNo}", fontSize = 15, weight = FontWeight.SemiBold, iconTint = Theme.accent, color = Theme.ink)
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
        navigationText = "Cancel",
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
        navigationText = if (result == null) "Cancel" else "Done",
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
                                        marked == null -> if (chosen) Theme.brand else Theme.secondaryText
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
