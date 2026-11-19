package com.tertiaryinfotech.tertiarycourses

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
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
        AlertDialog(
            onDismissRequest = { showQr = false },
            confirmButton = { TextButton(onClick = { showQr = false }) { Text("Done") } },
            title = { Text("Check-in QR") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    QrCode(d.checkinUrl, Modifier.fillMaxWidth().testTag("checkinQr"))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Learners scan this code to check in" + (if (d.currentSessionLabel.isNotEmpty()) " for ${d.currentSessionLabel}." else "."),
                        fontSize = 14.sp, color = Theme.secondaryText, textAlign = TextAlign.Center,
                    )
                }
            },
        )
    }
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
fun QrCode(text: String, modifier: Modifier = Modifier) {
    val matrix: BitMatrix? = remember(text) {
        try {
            QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 2, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M))
        } catch (e: Exception) {
            null
        }
    }
    Box(modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(Color.White).semantics { contentDescription = "Check-in QR code" }) {
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
