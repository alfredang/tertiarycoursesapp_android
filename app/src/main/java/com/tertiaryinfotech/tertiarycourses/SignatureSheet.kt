package com.tertiaryinfotech.tertiarycourses

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/** Someone who can sign in the signature dialog: the trainer ("trainer") or a roster learner (email). */
data class Signer(val id: String, val name: String, val signed: Boolean = false)

/**
 * The dialog that takes a finger signature for attendance (replaces the check-in QR code).
 * Learner: one signer (themselves); signing checks them in and closes the dialog.
 * Trainer: picks who signs (themselves or a learner handed the phone); stays open for the next.
 * [submit] gets the signature as a base64 PNG and answers the message to show.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureSheet(
    title: String,
    sessionLabel: String,
    signers: List<Signer>,
    hint: String = "Sign with your finger in the box below.",
    closeOnSuccess: Boolean = true,
    submit: suspend (Signer, String) -> String,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var strokes by remember { mutableStateOf(listOf<List<Offset>>()) }
    var padSize by remember { mutableStateOf(IntSize.Zero) }
    var signerId by remember { mutableStateOf(signers.firstOrNull()?.id.orEmpty()) }
    var done by remember { mutableStateOf(setOf<String>()) }
    var sending by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val signer = signers.firstOrNull { it.id == signerId } ?: signers.firstOrNull()
    // Enough ink to count as a signature, not a stray tap.
    val hasInk = strokes.sumOf { it.size } >= 8
    fun isSigned(s: Signer) = s.signed || s.id in done
    val inkWidth = with(LocalDensity.current) { 3.dp.toPx() }

    fun send() {
        val s = signer ?: return
        if (padSize.width <= 0 || padSize.height <= 0) return
        val png = signaturePng(strokes, padSize, inkWidth)
        scope.launch {
            sending = true; error = null; message = null
            try {
                val text = submit(s, png)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                strokes = emptyList()
                done = done + s.id
                if (closeOnSuccess) {
                    onDismiss()
                } else {
                    message = text
                    // Hand the phone to the next learner who has not signed.
                    signers.firstOrNull { it.id != "trainer" && !isSigned(it) }?.let { signerId = it.id }
                }
            } catch (e: Exception) {
                error = e.message ?: "The signature could not be sent. Please try again."
            } finally {
                sending = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        // A stroke must draw, never close the dialog.
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Scaffold(
            containerColor = Theme.page,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Sign Attendance", maxLines = 1, fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                    navigationIcon = {
                        TextButton(onClick = onDismiss, modifier = Modifier.testTag("closeSignature")) {
                            Text(if (closeOnSuccess) "Cancel" else "Done", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Theme.page),
                )
            },
        ) { padding ->
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
                    if (sessionLabel.isNotEmpty()) Text(sessionLabel, fontSize = 15.sp, color = Theme.secondaryText)
                }
                if (signers.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Who is signing?", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
                        var menu by remember { mutableStateOf(false) }
                        Box {
                            Row(
                                Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = "Choose who is signing") { menu = true }
                                    .padding(vertical = 8.dp).testTag("signerPicker"),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(signer?.let { it.name + if (isSigned(it)) "  ✓ signed" else "" }.orEmpty(), fontSize = 16.sp, color = Theme.accent)
                                Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = Theme.accent, modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Theme.card) {
                                signers.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.name + if (isSigned(s)) "  ✓ signed" else "") },
                                        trailingIcon = { if (s.id == signerId) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Theme.accent) },
                                        onClick = { menu = false; signerId = s.id; message = null; error = null },
                                    )
                                }
                            }
                        }
                    }
                } else signers.firstOrNull()?.let {
                    IconLine(Icons.Filled.Person, it.name, fontSize = 15, weight = FontWeight.SemiBold)
                }
                Text(hint, fontSize = 13.sp, color = Theme.secondaryText)

                SignaturePad(strokes, onStrokes = { strokes = it }, onSize = { padSize = it }, inkWidth = inkWidth)

                message?.let {
                    IconLine(Icons.Filled.CheckCircle, it, color = Theme.success, iconTint = Theme.success, fontSize = 15, weight = FontWeight.SemiBold)
                }
                error?.let { Text(it, fontSize = 13.sp, color = Theme.danger) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) {
                        TonalButton(
                            "Clear", Icons.Outlined.CleaningServices, enabled = strokes.isNotEmpty() && !sending,
                            modifier = Modifier.testTag("clearSignature"),
                        ) { strokes = emptyList() }
                    }
                    Box(Modifier.weight(1f)) {
                        PrimaryButton(
                            "Submit", Icons.Filled.Draw, enabled = hasInk && signer != null, busy = sending,
                            modifier = Modifier.testTag("submitSignature"),
                        ) { send() }
                    }
                }
                Spacer(Modifier.width(1.dp))
            }
        }
    }
}

/** The box a signature is drawn in with one finger: white like paper in either theme, black ink. */
@Composable
fun SignaturePad(
    strokes: List<List<Offset>>,
    onStrokes: (List<List<Offset>>) -> Unit,
    onSize: (IntSize) -> Unit,
    inkWidth: Float,
    modifier: Modifier = Modifier,
) {
    // The gesture handler outlives recompositions, so it reads the latest strokes through this.
    var latest by remember { mutableStateOf(strokes) }
    latest = strokes
    fun update(new: List<List<Offset>>) { latest = new; onStrokes(new) }
    Box(
        modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp)).background(Color.White)
            .border(1.dp, Color.Black.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .onSizeChanged(onSize)
            .semantics { contentDescription = "Signature pad. Draw your signature with one finger." }
            .testTag("signaturePad")
            // Consumes from touch-down, so a stroke draws instead of scrolling the page.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    update(latest + listOf(listOf(down.position)))
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        val p = Offset(
                            change.position.x.coerceIn(0f, size.width.toFloat()),
                            change.position.y.coerceIn(0f, size.height.toFloat()),
                        )
                        update(latest.dropLast(1) + listOf(latest.last() + p))
                    }
                }
            },
    ) {
        Box(
            Modifier.align(Alignment.BottomStart).padding(start = 24.dp, end = 24.dp, bottom = 44.dp)
                .fillMaxWidth().height(1.dp).background(Color.Black.copy(alpha = 0.25f)),
        )
        if (strokes.isEmpty()) {
            Text(
                "Sign here", fontSize = 15.sp, color = Color.Black.copy(alpha = 0.35f),
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 52.dp),
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            strokes.forEach { stroke ->
                if (stroke.size == 1) {
                    drawCircle(Color.Black, radius = inkWidth / 2, center = stroke[0])
                } else {
                    val path = Path().apply {
                        moveTo(stroke[0].x, stroke[0].y)
                        stroke.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, Color.Black, style = Stroke(width = inkWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}

/**
 * The check-in form, shown in E-Attendance for learners and trainers alike: the signed-in
 * person's details filled in from their profile, the session, a signature box and Check In.
 * It is the same form the web portal's check-in QR opens; the apps never show that QR.
 * [submit] gets the signature as a base64 PNG and answers the message; [onDone] gets that message.
 */
@Composable
fun CheckInForm(
    name: String,
    email: String,
    phone: String,
    sessionLabel: String,
    buttonTitle: String = "Check In",
    submit: suspend (String) -> String,
    onDone: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var strokes by remember { mutableStateOf(listOf<List<Offset>>()) }
    var padSize by remember { mutableStateOf(IntSize.Zero) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val hasInk = strokes.sumOf { it.size } >= 8
    val inkWidth = with(LocalDensity.current) { 3.dp.toPx() }

    @Composable
    fun field(label: String, value: String) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Theme.secondaryText)
            Text(
                value.ifEmpty { "—" }, fontSize = 15.sp, color = Theme.ink,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Theme.page)
                    .border(1.dp, Theme.cardBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }

    Column(Modifier.fillMaxWidth().testTag("checkInForm"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        field("Name", name)
        field("Email", email)
        if (phone.isNotEmpty()) field("Mobile", phone)
        field("Session", sessionLabel)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Signature", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Theme.secondaryText)
            SignaturePad(strokes, onStrokes = { strokes = it }, onSize = { padSize = it }, inkWidth = inkWidth)
        }
        error?.let { Text(it, fontSize = 13.sp, color = Theme.danger) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                TonalButton(
                    "Clear", Icons.Outlined.CleaningServices, enabled = strokes.isNotEmpty() && !sending,
                    modifier = Modifier.testTag("clearSignature"),
                ) { strokes = emptyList() }
            }
            Box(Modifier.weight(1f)) {
                PrimaryButton(
                    buttonTitle, Icons.Filled.CheckCircle, enabled = hasInk, busy = sending,
                    modifier = Modifier.testTag("checkIn"),
                ) {
                    if (padSize.width <= 0 || padSize.height <= 0) return@PrimaryButton
                    val png = signaturePng(strokes, padSize, inkWidth)
                    scope.launch {
                        sending = true; error = null
                        try {
                            val text = submit(png)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            strokes = emptyList()
                            onDone(text)
                        } catch (e: Exception) {
                            error = e.message ?: "The signature could not be sent. Please try again."
                        } finally {
                            sending = false
                        }
                    }
                }
            }
        }
        if (!hasInk) Text("Sign with your finger in the box, then tap $buttonTitle.", fontSize = 12.sp, color = Theme.secondaryText)
    }
}

/** The strokes as a PNG of black ink on a transparent background, base64 for the API. */
private fun signaturePng(strokes: List<List<Offset>>, size: IntSize, inkWidth: Float): String {
    // Keep the upload small: scale a large pad down to at most 1200 px wide.
    val scale = minOf(1f, 1200f / size.width)
    val w = (size.width * scale).toInt().coerceAtLeast(1)
    val h = (size.height * scale).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.BLACK
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = inkWidth * scale
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }
    strokes.forEach { stroke ->
        if (stroke.size == 1) {
            canvas.drawPoint(stroke[0].x * scale, stroke[0].y * scale, paint)
        } else {
            val path = android.graphics.Path()
            path.moveTo(stroke[0].x * scale, stroke[0].y * scale)
            stroke.drop(1).forEach { path.lineTo(it.x * scale, it.y * scale) }
            canvas.drawPath(path, paint)
        }
    }
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    bitmap.recycle()
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}
