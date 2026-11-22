package com.tertiaryinfotech.tertiarycourses

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.launch

/** A class the signed-in trainer teaches: overview, attendance, materials, feedback count. */
@Composable
fun TrainerClassScreen(vm: AppViewModel, runId: Int, title: String, push: (Route) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<TrainerClassDetail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var session by remember { mutableStateOf("") }
    // Unsaved toggles: "sessionKey|email" -> present.
    val edits = remember { mutableStateMapOf<String, Boolean>() }
    var saving by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var showQr by remember { mutableStateOf(false) }
    var surveyQr by remember { mutableStateOf<SurveyLink?>(null) }

    suspend fun load() {
        try {
            val d = vm.trainerClass(runId)
            detail = d; error = null
            if (session.isEmpty() || d.sessions.none { it.key == session }) {
                session = d.currentSession.ifEmpty { d.sessions.firstOrNull()?.key.orEmpty() }
            }
        } catch (e: Exception) {
            error = e.message
        }
    }
    LaunchedEffect(runId) { load() }

    ScreenScaffold(title = title, onBack = onBack) { padding ->
        Refreshable({ edits.clear(); load() }, Modifier.padding(padding)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val d = detail
                when {
                    d != null -> {
                        TrainerOverview(d) { openUrl(context, d.virtualLink) }
                        TakeAttendanceCard(
                            d, session, edits, saving,
                            onSession = { session = it },
                            onQr = { showQr = true },
                            onSave = {
                                scope.launch {
                                    saving = true
                                    try {
                                        val records = d.roster.map { r ->
                                            Triple(r.email, r.name, edits["$session|${r.email}"] ?: (session in r.present))
                                        }
                                        notice = vm.saveAttendance(runId, session, records)
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        edits.keys.filter { it.startsWith("$session|") }.forEach { edits.remove(it) }
                                        load()
                                    } catch (e: Exception) {
                                        notice = e.message
                                    } finally {
                                        saving = false
                                    }
                                }
                            },
                        )
                        MaterialsList("Courseware", (d.materials + d.documents).filter { it.url.isNotEmpty() },
                            "The courseware for this class will appear here.") { push(Route.Document(it.title, it.url)) }
                        InfoCard {
                            SectionLabel("Course feedback")
                            Spacer(Modifier.height(10.dp))
                            IconLine(
                                Icons.Filled.RateReview,
                                when (d.surveyResponses) {
                                    0 -> "No feedback responses yet."
                                    1 -> "1 learner has given feedback."
                                    else -> "${d.surveyResponses} learners have given feedback."
                                },
                                fontSize = 15, iconTint = Theme.accent,
                            )
                            if (d.feedbackUrl.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                TonalButton("Show Course Survey QR", Icons.Filled.QrCode2, modifier = Modifier.testTag("showSurveyQR")) {
                                    surveyQr = SurveyLink("Course Survey", d.feedbackUrl, "Learners scan this to give their course feedback.")
                                }
                            }
                            if (d.traqomUrl.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                TonalButton("Show TRAQOM Survey QR", Icons.Filled.QrCode2, modifier = Modifier.testTag("showTraqomQR")) {
                                    surveyQr = SurveyLink("TRAQOM Survey", d.traqomUrl, "Every learner must complete the SSG TRAQOM survey before the class ends.")
                                }
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
    val d = detail
    if (showQr && d != null && d.checkinUrl.isNotEmpty()) {
        QrDialog(
            "Check-in QR", d.checkinUrl,
            "Learners scan this code to check in" + (if (d.currentSessionLabel.isNotEmpty()) " for ${d.currentSessionLabel}." else "."),
            tag = "checkinQr",
        ) { showQr = false }
    }
    surveyQr?.let { s ->
        QrDialog(s.title, s.url, s.hint, tag = "surveyQr", shareLabel = "Share Link") { surveyQr = null }
    }
}

/** A survey the trainer shows the class as a QR code. */
private data class SurveyLink(val title: String, val url: String, val hint: String)

/** The QR sheet for the check-in link and the survey links; [shareLabel] adds a button that shares the link. */
@Composable
private fun QrDialog(heading: String, link: String, hint: String, tag: String, shareLabel: String? = null, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = shareLabel?.let { label -> { TextButton(onClick = { shareText(context, link, heading) }) { Text(label) } } },
        title = { Text(heading) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                QrCode(link, Modifier.fillMaxWidth().testTag(tag), description = "$heading code")
                Spacer(Modifier.height(12.dp))
                Text(hint, fontSize = 14.sp, color = Theme.secondaryText, textAlign = TextAlign.Center)
            }
        },
    )
}

@Composable
private fun TrainerOverview(d: TrainerClassDetail, join: () -> Unit) {
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("Teaching", Theme.accent)
            if (d.bucket == "current") StatusChip("In progress", Theme.success)
        }
        Spacer(Modifier.height(8.dp))
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
            IconLine(Icons.Filled.Groups, if (d.roster.size == 1) "1 learner" else "${d.roster.size} learners", fontSize = 15, iconTint = Theme.ink)
        }
        if (d.virtualLink.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            PrimaryButton("Start Online Class", Icons.Filled.Videocam, onClick = join)
        }
    }
}

@Composable
private fun TakeAttendanceCard(
    d: TrainerClassDetail,
    session: String,
    edits: SnapshotStateMap<String, Boolean>,
    saving: Boolean,
    onSession: (String) -> Unit,
    onQr: () -> Unit,
    onSave: () -> Unit,
) {
    fun present(r: RosterEntry) = edits["$session|${r.email}"] ?: (session in r.present)
    val setEdit: (RosterEntry, Boolean) -> Unit = { r, v ->
        // Toggling back to the saved value is no longer an unsaved change.
        if (v == (session in r.present)) edits.remove("$session|${r.email}") else edits["$session|${r.email}"] = v
    }
    val count = d.roster.count { present(it) }
    val dirty = edits.keys.any { it.startsWith("$session|") }

    InfoCard {
        SectionLabel("Take attendance")
        Spacer(Modifier.height(10.dp))
        if (d.sessions.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                d.sessions.forEach { s ->
                    FilterChip(
                        selected = s.key == session,
                        onClick = { onSession(s.key) },
                        label = { Text(sessionShort(s), fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Theme.brand, selectedLabelColor = Color.White, labelColor = Theme.ink,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(d.sessions.firstOrNull { it.key == session }?.label.orEmpty(), fontSize = 13.sp, color = Theme.secondaryText)
        }
        Spacer(Modifier.height(8.dp))
        Text("$count of ${d.roster.size} present", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("presentCount"))
        if (!d.attendanceOpen) {
            Spacer(Modifier.height(4.dp))
            Text(
                if (d.bucket == "upcoming") "Attendance opens on the first day of class." else "Attendance is closed for this class. Corrections are made on ${Site.websiteLabel}.",
                fontSize = 13.sp, color = Theme.secondaryText,
            )
        }
        if (d.roster.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("No learners are on this class roster yet.", fontSize = 15.sp, color = Theme.secondaryText)
        } else {
            HorizontalDivider(Modifier.padding(top = 12.dp), color = Theme.cardBorder)
        }
        d.roster.forEachIndexed { i, r ->
            val on = present(r)
            Row(
                Modifier.fillMaxWidth()
                    .then(if (d.attendanceOpen) Modifier.clickable { setEdit(r, !on) } else Modifier)
                    .padding(vertical = 8.dp)
                    .semantics(mergeDescendants = true) { contentDescription = "${r.name}, ${if (on) "present" else "absent"}" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(r.name.ifEmpty { r.email }, fontSize = 16.sp, color = Theme.ink)
                    val sub = listOf(r.email.takeIf { r.name.isNotEmpty() }.orEmpty(), if (r.walkin) "Walk-in" else "").filter { it.isNotEmpty() }
                    if (sub.isNotEmpty()) Text(sub.joinToString(" · "), fontSize = 12.sp, color = Theme.secondaryText)
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = on, onCheckedChange = { setEdit(r, it) }, enabled = d.attendanceOpen,
                    colors = SwitchDefaults.colors(checkedTrackColor = Theme.success, checkedThumbColor = Color.White),
                )
            }
            if (i < d.roster.lastIndex) HorizontalDivider(color = Theme.cardBorder)
        }
        if (d.attendanceOpen && d.roster.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Save Attendance", Icons.Filled.Save, enabled = dirty, busy = saving, modifier = Modifier.testTag("saveAttendance"), onClick = onSave)
        }
        if (d.checkinUrl.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            TonalButton("Show Check-in QR", Icons.Filled.QrCode2, onClick = onQr)
        }
    }
}

/** "Day 1 AM" for the session chips. */
private fun sessionShort(s: ClassSession): String =
    if (s.day > 0 && s.part.isNotEmpty()) "Day ${s.day} ${s.part}" else s.label.ifEmpty { s.key }

/** A titled list of documents that open in the in-app viewer (shared with the learner class screen). */
@Composable
fun MaterialsList(label: String, all: List<Material>, empty: String, open: (Material) -> Unit) {
    InfoCard {
        SectionLabel(label)
        Spacer(Modifier.height(6.dp))
        if (all.isEmpty()) {
            Text(empty, fontSize = 15.sp, color = Theme.secondaryText, modifier = Modifier.padding(vertical = 6.dp))
        }
        all.forEachIndexed { i, m ->
            Row(
                Modifier.fillMaxWidth().clickable { open(m) }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(materialIcon(m), contentDescription = null, tint = Theme.ink, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(m.title, fontSize = 16.sp, color = Theme.ink, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open", tint = Theme.tertiaryText, modifier = Modifier.size(20.dp))
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
    Box(modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(Color.White).semantics { contentDescription = description }) {
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
private fun <T> LoadedTab(
    data: T?, key: Any, load: suspend () -> Unit, header: @Composable () -> Unit = {}, content: @Composable (T) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var error by remember(key) { mutableStateOf<String?>(null) }
    suspend fun run() {
        try { load(); error = null } catch (e: Exception) { error = e.message ?: "Something went wrong. Please try again." }
    }
    LaunchedEffect(key) { if (data == null) run() }

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
    LoadedTab(vm.staffDashboard?.takeIf { it.role == role }, role, { vm.loadStaffDashboard(role) }, header = { HomeHeader(vm) }) { data ->
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
