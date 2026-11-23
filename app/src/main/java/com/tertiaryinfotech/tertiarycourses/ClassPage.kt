package com.tertiaryinfotech.tertiarycourses

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// The class page, laid out like the website's class page (and the TIA TMS app): an
// always-visible summary card, then collapsible section cards that start collapsed.

/** Titles of the sections the user has opened on this class page (kept across tab switches). */
@Composable
fun rememberOpenSections(runId: Int): MutableState<Set<String>> = rememberSaveable(runId) { mutableStateOf(setOf()) }

fun MutableState<Set<String>>.toggle(title: String) {
    value = if (title in value) value - title else value + title
}

/** Live Online and Hybrid classes show their mode; a classroom class shows its address instead. */
fun showsMode(mode: String): Boolean = mode == "Live Online" || mode == "Hybrid"

/** Always visible at the top of the class page: what, when, where, who, and the online class link. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassSummaryCard(
    title: String,
    sku: String,
    classId: String,
    chips: List<Pair<String, Color>>,
    startDate: String,
    endDate: String,
    startTime: String,
    endTime: String,
    mode: String,
    venue: String,
    room: String,
    trainer: String,
    learnerCount: Int?,
    virtualLink: String,
    onJoin: () -> Unit,
) {
    InfoCard(Modifier.testTag("classSummary"), padding = 18.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp, color = Theme.ink)
            val codeLine = listOf(sku, classId.takeIf { it.isNotEmpty() && it != sku }?.let { "Class $it" }.orEmpty())
                .filter { it.isNotEmpty() }.joinToString("  ·  ")
            if (codeLine.isNotEmpty()) Text(codeLine, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent)
            if (chips.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    chips.forEach { (text, color) -> StatusChip(text, color) }
                }
            }
            IconLine(Icons.Filled.Event, Fmt.range(startDate, endDate), fontSize = 15, iconTint = Theme.accent, color = Theme.ink)
            if (startTime.isNotEmpty()) IconLine(Icons.Filled.Schedule, Fmt.times(startTime, endTime), fontSize = 15, iconTint = Theme.accent, color = Theme.ink)
            if (showsMode(mode)) IconLine(modeIcon(mode), mode, fontSize = 15, iconTint = Theme.accent, color = Theme.ink)
            if (venue.isNotEmpty()) VenueLink(venue)
            if (room.isNotEmpty() && room != venue) IconLine(Icons.Filled.MeetingRoom, room, fontSize = 15, iconTint = Theme.accent, color = Theme.ink)
            if (trainer.isNotEmpty()) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Theme.accent, modifier = Modifier.padding(top = 1.dp).size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = Theme.secondaryText)) { append("Trainer: ") }
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Theme.ink)) { append(trainer) }
                        },
                        fontSize = 15.sp, lineHeight = 21.sp,
                    )
                }
            }
            if (learnerCount != null) {
                IconLine(Icons.Filled.Groups, "$learnerCount learner${if (learnerCount == 1) "" else "s"}", fontSize = 15, iconTint = Theme.accent, color = Theme.ink)
            }
            if (virtualLink.isNotEmpty()) {
                PrimaryButton("Join online class", Icons.Filled.Videocam, modifier = Modifier.padding(top = 4.dp).testTag("joinOnline"), onClick = onJoin)
            }
        }
    }
}

// ── Announcements ──

/** The class's posts, oldest first. [onDelete] is set for the trainer, who can remove a post. */
@Composable
fun AnnouncementList(items: List<Announcement>, openFile: (Announcement) -> Unit, onDelete: ((Announcement) -> Unit)? = null) {
    if (items.isEmpty()) {
        Text("No announcements yet.", fontSize = 14.sp, color = Theme.secondaryText)
        return
    }
    items.forEachIndexed { i, a ->
        if (i > 0) HorizontalDivider(color = Theme.cardBorder)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.testTag("announcement-${a.id}")) {
            if (a.message.isNotBlank()) Text(a.message, fontSize = 15.sp, lineHeight = 21.sp, color = Theme.ink)
            // posted_at arrives formatted for display ("Mon, 23 Nov 2026, 09:00"); an ISO date is formatted here.
            val posted = if (Regex("""^\d{4}-\d{2}-\d{2}""").containsMatchIn(a.postedAt)) Fmt.longDate(a.postedAt) else a.postedAt
            val meta = listOf(a.postedBy, posted).filter { it.isNotBlank() }.joinToString(" · ")
            if (meta.isNotEmpty()) Text(meta, fontSize = 12.sp, color = Theme.secondaryText)
            if (a.fileUrl.isNotEmpty()) {
                LinkRow(Icons.Filled.Description, a.fileName.ifBlank { "Attachment" }) { openFile(a) }
            }
            if (onDelete != null) {
                TextButton(onClick = { onDelete(a) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.testTag("deleteAnnouncement-${a.id}")) {
                    Text("Delete", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Theme.danger)
                }
            }
        }
    }
}

// ── Virtual Class Link ──

/** N/A for a classroom class; the join link, or "Not yet configured", for an online or hybrid class. */
@Composable
fun VirtualClassContent(mode: String, link: String, onJoin: () -> Unit) {
    when {
        link.isNotEmpty() -> LinkRow(Icons.Filled.Videocam, "Join online class", subtitle = link, external = true, tag = "virtualLink", onClick = onJoin)
        showsMode(mode) -> Text("Not yet configured.", fontSize = 14.sp, color = Theme.secondaryText)
        else -> Text("N/A for a physical class.", fontSize = 14.sp, color = Theme.secondaryText)
    }
}

// ── Lesson ──

fun lessonIcon(type: String): ImageVector = when (type) {
    "video" -> Icons.Filled.PlayCircle
    "document" -> Icons.Filled.Description
    "activity" -> Icons.Filled.Science
    "quiz" -> Icons.Filled.Quiz
    else -> Icons.Filled.Language
}

/**
 * Topics > sub topics > links. Non-quiz links call [openLink]; each quiz is drawn by
 * [quizRow] (the learner's Start / Retake row, or the trainer's QR row).
 */
@Composable
fun LessonContent(topics: List<LessonTopic>, openLink: (LessonLink) -> Unit, quizRow: @Composable (LessonLink) -> Unit) {
    val shown = topics.filter { t -> t.title.isNotBlank() || t.subtopics.any { it.links.isNotEmpty() } }
    if (shown.isEmpty()) {
        Text("No lesson content for this course yet.", fontSize = 14.sp, color = Theme.secondaryText)
        return
    }
    shown.forEachIndexed { ti, topic ->
        if (ti > 0) HorizontalDivider(color = Theme.cardBorder)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (topic.title.isNotBlank()) Text(topic.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 21.sp, color = Theme.ink)
            topic.subtopics.forEach { sub ->
                if (sub.title.isNotBlank()) {
                    Text(sub.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Theme.secondaryText, lineHeight = 19.sp, modifier = Modifier.padding(top = 4.dp))
                }
                sub.links.forEach { link ->
                    if (link.isQuiz) quizRow(link)
                    else LinkRow(
                        lessonIcon(link.type), link.title,
                        subtitle = when (link.type) { "video" -> "Video"; "document" -> "Document"; "activity" -> "Activity"; else -> null },
                        external = link.type == "video",
                    ) { openLink(link) }
                }
            }
        }
    }
}

fun questionCount(n: Int) = "$n question${if (n == 1) "" else "s"}"

// ── Course Feedback Summary (trainer) ──

private fun scoreText(v: Double?): String = if (v == null) "–" else String.format(Locale.ENGLISH, "%.1f", v)

/** Overall score out of 5 with stars and the response count, a bar per question, then the comments (three, then Show all). */
@Composable
fun FeedbackSummaryView(summary: FeedbackSummary) {
    var allComments by rememberSaveable { mutableStateOf(false) }
    fun tint(v: Double?): Color = when {
        v == null -> Theme.tertiaryText
        v >= 4 -> Theme.success
        v >= 3 -> Theme.warning
        else -> Theme.danger
    }
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }.testTag("feedbackSummary"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(scoreText(summary.overall), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = tint(summary.overall), lineHeight = 44.sp)
            Text("out of 5", fontSize = 12.sp, color = Theme.secondaryText)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                val v = summary.overall ?: 0.0
                (1..5).forEach { i ->
                    val icon = when {
                        v >= i -> Icons.Filled.Star
                        v >= i - 0.5 -> Icons.AutoMirrored.Filled.StarHalf
                        else -> Icons.Filled.StarBorder
                    }
                    Icon(icon, contentDescription = null, tint = Theme.star, modifier = Modifier.size(18.dp))
                }
            }
            Text("Overall average", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
            Text("${summary.count} ${if (summary.count == 1) "response" else "responses"}", fontSize = 12.sp, color = Theme.secondaryText)
        }
    }
    summary.questions.forEachIndexed { i, q ->
        Column(Modifier.semantics(mergeDescendants = true) { }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text("${i + 1}. ${q.label}", fontSize = 14.sp, lineHeight = 19.sp, color = Theme.ink, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text("${scoreText(q.average)}/5", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Theme.ink)
            }
            LinearProgressIndicator(
                progress = { ((q.average ?: 0.0).coerceIn(0.0, 5.0) / 5.0).toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = tint(q.average), trackColor = Theme.field, drawStopIndicator = {},
            )
        }
    }
    if (summary.comments.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Comments", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
            (if (allComments) summary.comments else summary.comments.take(3)).forEach { comment ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Theme.field).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("“", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Theme.accent, lineHeight = 18.sp)
                    Text(comment, fontSize = 14.sp, lineHeight = 19.sp, color = Theme.ink)
                }
            }
            if (summary.comments.size > 3) {
                TextButton(onClick = { allComments = !allComments }, contentPadding = PaddingValues(0.dp), modifier = Modifier.testTag("allComments")) {
                    Text(if (allComments) "Show fewer" else "Show all ${summary.comments.size} comments", fontWeight = FontWeight.SemiBold, color = Theme.accent)
                }
            }
        }
    }
}
