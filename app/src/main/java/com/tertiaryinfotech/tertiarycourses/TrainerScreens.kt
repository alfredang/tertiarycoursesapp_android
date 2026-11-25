package com.tertiaryinfotech.tertiarycourses

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.launch

/**
 * A class the signed-in trainer teaches, as the website's class page: the summary card,
 * then E-Attendance, Announcements, Virtual Class Link, Courseware, Lesson, Practice Exam
 * (when the course has one) and Course Feedback Summary, each collapsed until tapped.
 */
@Composable
fun TrainerClassScreen(vm: AppViewModel, runId: Int, title: String, push: (Route) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<TrainerClassDetail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var session by remember { mutableStateOf("") }
    // Emails marked present for the chosen session, as edited on screen.
    var present by remember { mutableStateOf(setOf<String>()) }
    var dirty by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var qr by remember { mutableStateOf<QrLink?>(null) }
    var signing by remember { mutableStateOf(false) }
    // Announcements: the composer, the post being deleted, and the latest list from a post or delete.
    var draft by rememberSaveable(runId) { mutableStateOf("") }
    var posting by remember { mutableStateOf(false) }
    var postProblem by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<Announcement?>(null) }
    val open = rememberOpenSections(runId)

    fun resetMarks(d: TrainerClassDetail, key: String) {
        present = d.roster.filter { key in it.present }.map { it.email }.toSet()
        dirty = false
    }

    suspend fun load() {
        try {
            val d = vm.trainerClass(runId)
            detail = d; error = null
            if (session.isEmpty() || d.sessions.none { it.key == session }) {
                session = d.currentSession.ifEmpty { d.sessions.firstOrNull()?.key.orEmpty() }
            }
            resetMarks(d, session)
        } catch (e: Exception) {
            error = e.message
        }
    }
    LaunchedEffect(runId) { load() }

    fun announce(work: suspend () -> List<Announcement>, after: () -> Unit = {}) {
        scope.launch {
            posting = true; postProblem = null
            try {
                val list = work()
                detail = detail?.copy(announcements = list)
                after()
            } catch (e: Exception) {
                postProblem = e.message
            } finally {
                posting = false
            }
        }
    }

    ScreenScaffold(title = title, onBack = onBack) { padding ->
        Refreshable({ load() }, Modifier.padding(padding)) {
            Column(
                Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val d = detail
                when {
                    d != null -> {
                        ClassSummaryCard(
                            d.title, d.sku, d.classId,
                            buildList {
                                add("Teaching" to Theme.accent)
                                if (d.bucket == "current") add("In progress" to Theme.success)
                            },
                            d.startDate, d.endDate, d.startTime, d.endTime, d.mode, d.venue, d.room, "", d.roster.size, d.virtualLink,
                        ) { openUrl(context, d.virtualLink) }

                        FoldCard(
                            "E-Attendance", Icons.Filled.CheckCircle, "E-Attendance" in open.value, { open.toggle("E-Attendance") },
                            trailing = if (d.roster.isNotEmpty()) "${present.size}/${d.roster.size}" else null,
                        ) {
                            TakeAttendanceContent(
                                d, session, present, dirty, saving,
                                onSession = { session = it; resetMarks(d, it) },
                                onToggle = { email, on -> present = if (on) present + email else present - email; dirty = true },
                                onMarkAll = { present = d.roster.map { it.email }.toSet(); dirty = true },
                                onSign = { signing = true },
                                onSave = {
                                    scope.launch {
                                        saving = true
                                        try {
                                            val records = d.roster.map { r -> Triple(r.email, r.name, r.email in present) }
                                            notice = vm.saveAttendance(runId, session, records)
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            load()
                                        } catch (e: Exception) {
                                            notice = e.message
                                        } finally {
                                            saving = false
                                        }
                                    }
                                },
                            )
                        }
                        FoldCard(
                            "Announcements", Icons.Filled.Campaign, "Announcements" in open.value, { open.toggle("Announcements") },
                            trailing = d.announcements.size.takeIf { it > 0 }?.toString(),
                            footer = "Learners see these posts on their class page in the app and on the website.",
                        ) {
                            OutlinedTextField(
                                value = draft, onValueChange = { draft = it.take(5000) },
                                placeholder = { Text("Write an announcement for this class…") },
                                minLines = 3, maxLines = 8, shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Theme.accent, unfocusedBorderColor = Theme.cardBorder),
                                modifier = Modifier.fillMaxWidth().testTag("announcementText"),
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                postProblem?.let { Text(it, fontSize = 13.sp, color = Theme.danger, modifier = Modifier.weight(1f)) }
                                    ?: Spacer(Modifier.weight(1f))
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { announce({ vm.postAnnouncement(runId, draft) }) { draft = "" } },
                                    enabled = !posting && draft.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Theme.brand, contentColor = Color.White),
                                    modifier = Modifier.testTag("announcementPost"),
                                ) { Text(if (posting) "Posting…" else "Post", fontWeight = FontWeight.SemiBold) }
                            }
                            HorizontalDivider(color = Theme.cardBorder)
                            AnnouncementList(
                                d.announcements,
                                openFile = { openDocument(context, push, it.fileName.ifBlank { "Attachment" }, it.fileUrl) },
                                onDelete = { confirmDelete = it },
                            )
                        }
                        FoldCard("Virtual Class Link", Icons.Filled.Videocam, "Virtual Class Link" in open.value, { open.toggle("Virtual Class Link") }) {
                            VirtualClassContent(d.mode, d.virtualLink) { openUrl(context, d.virtualLink) }
                        }
                        FoldCard(
                            "Courseware", Icons.Filled.Folder, "Courseware" in open.value, { open.toggle("Courseware") },
                            footer = "Trainer and learner courseware. Documents open inside the app.",
                        ) {
                            MaterialRows((d.materials + d.documents).filter { it.url.isNotEmpty() }, "No materials are linked to this course yet.") {
                                openDocument(context, push, it.title, it.url)
                            }
                        }
                        FoldCard(
                            "Lesson", Icons.Filled.School, "Lesson" in open.value, { open.toggle("Lesson") },
                            footer = "Show a quiz's QR code to the class. Learners scan it to take the quiz; their scores are saved to this class.",
                        ) {
                            LessonContent(
                                d.lesson,
                                openLink = { link ->
                                    if (link.type == "video") openUrl(context, link.url) else openDocument(context, push, link.title, link.url)
                                },
                                quizRow = { link ->
                                    LinkRow(
                                        Icons.Filled.QrCode2, link.title.ifBlank { "Quiz" },
                                        subtitle = questionCount(link.questions) + if (link.url.isEmpty()) " · no QR link for this class" else "",
                                        trailingText = if (link.url.isNotEmpty()) "Show QR" else "", tag = "quizQr-${link.quizId}",
                                    ) {
                                        if (link.url.isNotEmpty()) qr = QrLink(
                                            link.title.ifBlank { "Quiz" }, link.url, questionCount(link.questions),
                                            "Learners scan this with their phone camera to take the quiz.", "Share Quiz Link", "quizQr",
                                        )
                                    }
                                },
                            )
                        }
                        d.practiceExam?.let { pe ->
                            FoldCard(
                                "Practice Exam", Icons.Filled.Quiz, "Practice Exam" in open.value, { open.toggle("Practice Exam") },
                                trailing = pe.examCode.ifEmpty { null },
                            ) {
                                // No QR in the app: the trainer opens the self-register form (for a learner without the app).
                                PracticeExamContent(
                                    pe, trainer = true,
                                    openForm = { openDocument(context, push, "${pe.examCode} Practice Exam".trim(), it) },
                                    openSite = { openUrl(context, it) },
                                )
                            }
                        }
                        // Course Feedback, as on the website's class page: the QR of the class's feedback form
                        // (the TMS form for a class run from the TMS), its link, and the course code and run id.
                        FoldCard("Course Feedback", Icons.Filled.RateReview, "Course Feedback" in open.value, { open.toggle("Course Feedback") }) {
                            if (d.feedbackUrl.isNotEmpty()) {
                                LinkRow(
                                    Icons.Filled.QrCode2, "Show Course Feedback QR", subtitle = d.feedbackUrl,
                                    trailingText = "Show QR", tag = "showFeedbackQr",
                                ) {
                                    qr = QrLink(
                                        "Course Feedback", d.feedbackUrl, "Course Code: ${d.sku} · Course Run ID: ${d.classId}",
                                        "Learners scan this to share their feedback on the class.", "Share Feedback Link", "feedbackQr",
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { openUrl(context, d.feedbackUrl) },
                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Theme.tonal, contentColor = Theme.accent),
                                    modifier = Modifier.fillMaxWidth().testTag("openFeedbackLink"),
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Open Link", fontSize = 16.sp)
                                }
                                Text(
                                    "Course Code: ${d.sku}   ·   Course Run ID: ${d.classId}", fontSize = 13.sp, color = Theme.secondaryText,
                                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                Text("The feedback form has not been set up for this class yet.", fontSize = 14.sp, color = Theme.secondaryText)
                            }
                        }
                        FoldCard("Course Feedback Summary", Icons.Filled.BarChart, "Course Feedback Summary" in open.value, { open.toggle("Course Feedback Summary") }) {
                            val summary = d.feedbackSummary
                            if (summary != null && summary.count > 0) {
                                FeedbackSummaryView(summary)
                            } else {
                                val n = d.surveyResponses
                                val total = d.roster.size
                                Text(
                                    if (n == 0) "No feedback yet for this class." else "$n of $total learner${if (total == 1) "" else "s"} gave feedback.",
                                    fontSize = 14.sp, color = Theme.secondaryText,
                                )
                            }
                        }
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
    confirmDelete?.let { a ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete this post?") },
            text = { Text("Learners will no longer see it.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = null; announce({ vm.deleteAnnouncement(runId, a.id) }) }, modifier = Modifier.testTag("confirmDeletePost")) {
                    Text("Delete post", color = Theme.danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
    val d = detail
    if (signing && d != null) {
        val key = session
        val signers = listOf(Signer("trainer", "You (trainer)", key in d.trainerSigned)) +
            d.roster.map { Signer(it.email, it.name.ifEmpty { it.email }, key in it.signed) }
        SignatureSheet(
            title = d.title, sessionLabel = d.sessions.firstOrNull { it.key == key }?.label.orEmpty(), signers = signers,
            hint = "Choose who is signing, then sign with a finger in the box.", closeOnSuccess = false,
            submit = { signer, png -> vm.signAttendance(runId, key, signer.id, png) },
            onDismiss = { signing = false; scope.launch { load() } },
        )
    }
    qr?.let { link ->
        if (link.url.isNotEmpty()) QrSheet(
            heading = link.heading, link = link.url, title = d?.title.orEmpty(), sessionLabel = link.caption,
            hint = link.hint, shareLabel = link.shareLabel, tag = link.tag,
        ) { qr = null }
    }
}

/** A link the trainer shows the class as a QR code (check-in, a lesson quiz). */
private data class QrLink(val heading: String, val url: String, val caption: String, val hint: String, val shareLabel: String, val tag: String)

/**
 * A link as a QR code for learners to scan (check-in, a lesson quiz): a full-screen
 * sheet with the class title, the session, the code, a hint and a button that shares the link.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrSheet(
    heading: String, link: String, title: String, sessionLabel: String, hint: String, shareLabel: String, tag: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            containerColor = Theme.page,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(heading, maxLines = 1, fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                    actions = {
                        TextButton(onClick = onDismiss) { Text("Done", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent) }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Theme.page),
                )
            },
        ) { padding ->
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, color = Theme.ink)
                if (sessionLabel.isNotEmpty()) Text(sessionLabel, fontSize = 15.sp, color = Theme.secondaryText, textAlign = TextAlign.Center)
                QrCode(link, Modifier.widthIn(max = 320.dp).fillMaxWidth().testTag(tag), description = "$heading code")
                Text(hint, fontSize = 13.sp, color = Theme.secondaryText, textAlign = TextAlign.Center)
                FilledTonalButton(
                    onClick = { shareText(context, link, heading) },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Theme.tonal, contentColor = Theme.accent),
                    modifier = Modifier.testTag("shareQrLink"),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(shareLabel, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun TakeAttendanceContent(
    d: TrainerClassDetail,
    session: String,
    present: Set<String>,
    dirty: Boolean,
    saving: Boolean,
    onSession: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onMarkAll: () -> Unit,
    onSign: () -> Unit,
    onSave: () -> Unit,
) {
    val open = d.attendanceOpen
    fun label(s: ClassSession) = s.label + if (s.key == d.currentSession) " (now)" else ""

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Take attendance", Modifier.weight(1f))
            if (d.roster.isNotEmpty()) {
                Text(
                    "${present.size} of ${d.roster.size} present", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = Theme.success, modifier = Modifier.testTag("presentCount"),
                )
            }
        }
        if (d.sessions.size > 1) {
            // The session menu: the chosen session, tap to pick another.
            var menu by remember { mutableStateOf(false) }
            Spacer(Modifier.height(6.dp))
            Box {
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = "Choose session") { menu = true }
                        .padding(vertical = 8.dp).testTag("sessionPicker"),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(d.sessions.firstOrNull { it.key == session }?.let { label(it) }.orEmpty(), fontSize = 16.sp, color = Theme.accent)
                    Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = Theme.accent, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Theme.card) {
                    d.sessions.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(label(s)) },
                            trailingIcon = { if (s.key == session) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Theme.accent) },
                            onClick = { menu = false; if (s.key != session) onSession(s.key) },
                        )
                    }
                }
            }
        } else d.sessions.firstOrNull()?.let { only ->
            Spacer(Modifier.height(12.dp))
            Text(only.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        if (!open) {
            Spacer(Modifier.height(12.dp))
            Text(
                if (d.bucket == "upcoming") "Attendance opens on the first day of class."
                else "Attendance is closed for this class. Use ${Site.websiteLabel} to correct a finished class.",
                fontSize = 13.sp, color = Theme.secondaryText,
            )
        }

        Spacer(Modifier.height(12.dp))
        if (d.roster.isEmpty()) {
            Text("No learners are on this class roster yet.", fontSize = 15.sp, color = Theme.secondaryText)
        }
        d.roster.forEachIndexed { i, r ->
            val on = r.email in present
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    .semantics(mergeDescendants = true) { contentDescription = "${r.name.ifEmpty { r.email }}, ${if (on) "present" else "absent"}" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(r.name.ifEmpty { r.email }, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Theme.ink)
                        if (session in r.signed) {
                            Icon(Icons.Filled.Draw, contentDescription = "Signed", tint = Theme.success, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        r.email + if (r.walkin) " · walk-in" else "", fontSize = 12.sp, color = Theme.secondaryText,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = on, onCheckedChange = { onToggle(r.email, it) }, enabled = open && !saving,
                    colors = SwitchDefaults.colors(checkedTrackColor = Theme.success, checkedThumbColor = Color.White),
                )
            }
            if (i < d.roster.lastIndex) HorizontalDivider(color = Theme.cardBorder)
        }

        if (open) {
            if (d.roster.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onMarkAll, contentPadding = PaddingValues(0.dp), modifier = Modifier.testTag("markAllPresent")) {
                    Text("Mark all present", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent)
                }
                Spacer(Modifier.height(4.dp))
                PrimaryButton(
                    "Save Attendance", Icons.Filled.CheckCircle, enabled = dirty && session.isNotEmpty(), busy = saving,
                    modifier = Modifier.testTag("saveAttendance"), onClick = onSave,
                )
            }
            if (session.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                if (session in d.trainerSigned) {
                    IconLine(
                        Icons.Filled.Draw, "You have signed for this session.",
                        color = Theme.success, iconTint = Theme.success, fontSize = 13, weight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                TonalButton("Sign Attendance", Icons.Filled.Draw, modifier = Modifier.testTag("signAttendance"), onClick = onSign)
                Spacer(Modifier.height(6.dp))
                Text(
                    "You sign for the session, or hand your phone to a learner to sign. A learner who signs is marked present.",
                    fontSize = 12.sp, color = Theme.secondaryText,
                )
            }
        }
    }
}

/** The class's documents, opened in the in-app viewer (shared with the learner class page). */
@Composable
fun MaterialRows(all: List<Material>, empty: String, open: (Material) -> Unit) {
    if (all.isEmpty()) {
        Text(empty, fontSize = 14.sp, color = Theme.secondaryText)
        return
    }
    Column {
        all.forEachIndexed { i, m ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { open(m) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(materialIcon(m), contentDescription = null, tint = Theme.accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(m.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Theme.tertiaryText, modifier = Modifier.size(20.dp))
            }
            if (i < all.lastIndex) HorizontalDivider(color = Theme.cardBorder)
        }
    }
}

/** A QR code drawn from the ZXing bit matrix; always black on white so scanners can read it in dark mode. */
@Composable
fun QrCode(text: String, modifier: Modifier = Modifier, description: String = "Check-in QR code") {
    val matrix: BitMatrix? = remember(text) {
        try {
            QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 2, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M))
        } catch (e: Exception) {
            null
        }
    }
    Box(modifier.aspectRatio(1f).clip(RoundedCornerShape(14.dp)).background(Color.White).semantics { contentDescription = description }) {
        if (matrix != null) {
            Canvas(Modifier.fillMaxSize()) {
                val cell = size.width / matrix.width
                for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
                    if (matrix[x, y]) drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
                }
            }
        }
    }
}

// ── Payout (trainer tab) ──

/** Loads a tab's data once (it is kept in the view model), with pull-to-refresh and a retry card. */
@Composable
internal fun <T> LoadedTab(
    data: T?, key: Any, load: suspend () -> Unit, header: @Composable () -> Unit = {},
    /** Set for a tab that reloads every time it is shown: drops what is on screen before the load. */
    resetOnShow: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var error by remember(key) { mutableStateOf<String?>(null) }
    suspend fun run() {
        try { load(); error = null } catch (e: Exception) { error = e.message ?: "Something went wrong. Please try again." }
    }
    LaunchedEffect(key) {
        if (resetOnShow != null) { resetOnShow(); run() } else if (data == null) run()
    }

    ScreenScaffold { padding ->
        Refreshable({ run() }, Modifier.padding(padding)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                header()
                val err = error
                when {
                    data != null -> content(data)
                    err != null -> ErrorCard(err) { scope.launch { run() } }
                    else -> LoadingBox()
                }
            }
        }
    }
}

@Composable
fun PayoutScreen(vm: AppViewModel) {
    LoadedTab(vm.payouts, "payouts", { vm.loadPayouts() }) { reply ->
        when {
            !reply.available -> EmptyState(
                Icons.Filled.Payments, "Payouts not available",
                "Your payout history cannot be shown here right now. Please try again later.",
            )
            reply.payouts.isEmpty() -> EmptyState(
                Icons.Filled.Payments, "No payouts yet", "Payouts for the classes you teach appear here once they are paid.",
            )
            else -> {
                val n = reply.payouts.size
                SummaryCard("Total paid", Fmt.money(reply.total, reply.currency), "$n payout${if (n == 1) "" else "s"}")
                reply.payouts.forEach { PayoutCard(it, reply.currency) }
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, caption: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Theme.brand).padding(18.dp)
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, fontSize = 15.sp, color = Color.White.copy(alpha = 0.85f))
        Text(value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 38.sp)
        Text(caption, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
private fun PayoutCard(payout: Payout, currency: String) {
    InfoCard {
        Row(verticalAlignment = Alignment.Top) {
            Text(payout.title.ifEmpty { "Class" }, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text(Fmt.money(payout.amount, currency), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Theme.success)
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (payout.startDate.isNotEmpty()) LabeledValue("Class", Fmt.range(payout.startDate, payout.endDate), fontSize = 13, color = Theme.secondaryText)
            if (payout.paymentDate.isNotEmpty()) LabeledValue("Paid", Fmt.longDate(payout.paymentDate), fontSize = 13, color = Theme.secondaryText)
            if (payout.learners > 0) LabeledValue("Learners", "${payout.learners}", fontSize = 13, color = Theme.secondaryText)
            if (payout.courseCode.isNotEmpty()) LabeledValue("Course", payout.courseCode, fontSize = 13, color = Theme.secondaryText)
        }
    }
}

// ── Training hours (trainer tab) ──

@Composable
fun TrainingHoursScreen(vm: AppViewModel) {
    LoadedTab(vm.trainingHours, "hours", { vm.loadTrainingHours() }) { reply ->
        if (reply.classes.isEmpty()) {
            EmptyState(Icons.Filled.Schedule, "No training hours yet", "Hours for the classes you teach appear here after each class day.")
            return@LoadedTab
        }
        val n = reply.classes.size
        SummaryCard("Total training hours", Fmt.hours(reply.totalHours), "$n class${if (n == 1) "" else "es"} in the last 3 years")
        Text("By month", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        InfoCard(padding = 0.dp) {
            reply.months.forEachIndexed { i, m ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(Fmt.month(m.month), fontSize = 16.sp, color = Theme.ink, modifier = Modifier.weight(1f))
                    Text(Fmt.hours(m.hours), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
                }
                if (i < reply.months.lastIndex) HorizontalDivider(Modifier.padding(start = 16.dp), color = Theme.cardBorder)
            }
        }
        Text("By class", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        reply.classes.forEach { c ->
            InfoCard {
                Row(verticalAlignment = Alignment.Top) {
                    Text(c.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    Text(Fmt.hours(c.hours), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent)
                }
                Spacer(Modifier.height(6.dp))
                Text("${Fmt.range(c.startDate, c.endDate)} · ${c.days} day${if (c.days == 1) "" else "s"}", fontSize = 13.sp, color = Theme.secondaryText)
            }
        }
        Text(
            "Each class day counts its class time, less a one-hour lunch on days longer than five hours.",
            fontSize = 13.sp, color = Theme.secondaryText,
        )
    }
}

// ── Staff dashboard (Admin, Marketing, Developer, Franchise Admin) ──

/** One glanceable screen per staff role; the cards and lists are whatever the site sends. */
@Composable
fun StaffDashboardScreen(vm: AppViewModel, role: String) {
    LoadedTab(vm.staffDashboard?.takeIf { it.role == role }, role, { vm.loadStaffDashboard(role) }, header = { HomeHeader(vm) }, resetOnShow = { vm.clearStaffDashboard() }) { data ->
        data.cards.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { StaffCardView(it, Modifier.weight(1f).fillMaxHeight()) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        data.lists.forEach { list ->
            Text(list.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
            InfoCard(padding = 0.dp) {
                list.rows.forEachIndexed { i, row ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(row.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
                            if (row.subtitle.isNotEmpty()) Text(row.subtitle, fontSize = 13.sp, color = Theme.secondaryText)
                        }
                        if (row.value.isNotEmpty()) {
                            Spacer(Modifier.width(12.dp))
                            Text(row.value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent, textAlign = TextAlign.End)
                        }
                    }
                    if (i < list.rows.lastIndex) HorizontalDivider(Modifier.padding(start = 16.dp), color = Theme.cardBorder)
                }
            }
        }
        Text("Updated ${data.updatedAt}. Pull down to refresh.", fontSize = 13.sp, color = Theme.secondaryText)
    }
}

@Composable
private fun StaffCardView(card: StaffCard, modifier: Modifier = Modifier) {
    val warning = card.tone == "warning"
    val tint = if (warning) Theme.warning else Theme.accent
    InfoCard(
        modifier = modifier.heightIn(min = 112.dp).semantics(mergeDescendants = true) { contentDescription = "${card.title}: ${card.value}" },
        padding = 14.dp,
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(staffIcon(card.icon), contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(card.value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = if (warning) Theme.warning else Theme.ink, lineHeight = 34.sp)
        Spacer(Modifier.height(4.dp))
        Text(card.title, fontSize = 13.sp, color = Theme.secondaryText)
    }
}

/** The site names icons by their SF Symbol (iOS); these are the Material equivalents of the ones it uses. */
private fun staffIcon(sfSymbol: String): ImageVector = when (sfSymbol) {
    "calendar" -> Icons.Filled.CalendarMonth
    "calendar.badge.clock" -> Icons.Filled.Event
    "person.fill.questionmark" -> Icons.Filled.PersonSearch
    "cart.fill" -> Icons.Filled.ShoppingCart
    "person.badge.plus" -> Icons.Filled.PersonAdd
    "person.2.fill" -> Icons.Filled.Group
    "envelope.badge" -> Icons.Filled.MarkEmailUnread
    "doc.text.magnifyingglass" -> Icons.Filled.FindInPage
    "newspaper.fill" -> Icons.Filled.Newspaper
    "paperplane.fill" -> Icons.AutoMirrored.Filled.Send
    "books.vertical.fill" -> Icons.AutoMirrored.Filled.LibraryBooks
    "eye.slash.fill" -> Icons.Filled.VisibilityOff
    "exclamationmark.triangle.fill" -> Icons.Filled.Warning
    "checklist" -> Icons.Filled.Checklist
    "checkmark.seal.fill" -> Icons.Filled.Verified
    "globe.asia.australia.fill" -> Icons.Filled.Public
    "person.3.fill" -> Icons.Filled.Groups
    "rosette" -> Icons.Filled.WorkspacePremium
    "banknote.fill" -> Icons.Filled.Payments
    else -> Icons.Filled.BarChart
}
