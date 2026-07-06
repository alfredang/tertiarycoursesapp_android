package com.tertiaryinfotech.tertiarycourses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val WHATSAPP_NUMBER = "6588666375"

enum class FeedbackCategory(val label: String, val prompt: String) {
    FEATURE(
        "Feature Idea",
        "What feature would make this app more useful? e.g. course schedules, favourites, reminders…"
    ),
    BUG(
        "Bug Report",
        "What went wrong? Tell us what you did, what you expected, and what happened instead."
    ),
    COMMENT(
        "Comment",
        "Any comments about the app or our courses — we read everything."
    );

    val icon: ImageVector
        get() = when (this) {
            FEATURE -> Icons.Filled.Lightbulb
            BUG -> Icons.Filled.BugReport
            COMMENT -> Icons.Filled.ChatBubble
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen() {
    val uriHandler = LocalUriHandler.current
    var category by rememberSaveable { mutableStateOf(FeedbackCategory.FEATURE) }
    var title by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }

    val canSend = title.isNotBlank() || message.isNotBlank()

    Scaffold(
        containerColor = Theme.page,
        topBar = {
            TopAppBar(
                title = { Text("Feedback", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Theme.page)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            InfoCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Theme.accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "We'd love to hear from you",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Theme.accent
                        )
                    }
                    Text(
                        "Suggest features you'd like, report bugs you found, or just leave a comment. " +
                            "Your feedback goes straight to our team on WhatsApp.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Theme.secondaryText
                    )
                }
            }

            SectionLabel("What kind of feedback?")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeedbackCategory.entries.forEach { item ->
                    val selected = category == item
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) Theme.accent else Theme.accentSoft,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { category = item }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            tint = if (selected) Color.White else Theme.accent
                        )
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = if (selected) Color.White else Theme.accent
                        )
                    }
                }
            }

            SectionLabel("Your feedback")
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Short title") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Theme.accent,
                    unfocusedBorderColor = Theme.cardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                placeholder = { Text(category.prompt) },
                minLines = 6,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Theme.accent,
                    unfocusedBorderColor = Theme.cardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val body = buildString {
                        append("${category.label} — Tertiary Courses SG app\n")
                        if (title.isNotBlank()) append("*${title.trim()}*\n")
                        append(message.trim())
                    }
                    uriHandler.openUri(whatsAppUrl(WHATSAPP_NUMBER, body))
                },
                enabled = canSend,
                colors = ButtonDefaults.buttonColors(containerColor = Theme.accent),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Send via WhatsApp", fontWeight = FontWeight.SemiBold)
            }

            Text(
                "Sent to Tertiary Courses Singapore (+65 8866 6375) via WhatsApp.",
                style = MaterialTheme.typography.bodySmall,
                color = Theme.secondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        containerColor = Theme.page,
        topBar = {
            TopAppBar(
                title = { Text("About", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Theme.page)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            InfoCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Tertiary Courses SG",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Browse the Tertiary Courses Singapore catalog, estimate SSG grants " +
                            "(Baseline, MCES, and SME) with SkillsFuture Credit, and get your " +
                            "net course fee — all in one app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Theme.secondaryText
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Official site")
                InfoCard {
                    Column {
                        AboutLinkRow(Icons.Filled.Language, "tertiarycourses.com.sg") {
                            uriHandler.openUri("https://www.tertiarycourses.com.sg/")
                        }
                        HorizontalDivider()
                        AboutLinkRow(Icons.Filled.Phone, "+65 6100 0613") {
                            uriHandler.openUri("tel:+6561000613")
                        }
                        HorizontalDivider()
                        AboutLinkRow(Icons.Filled.Email, "enquiry@tertiaryinfotech.com") {
                            uriHandler.openUri("mailto:enquiry@tertiaryinfotech.com")
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Developer")
                InfoCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Tertiary Infotech Academy Pte. Ltd.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        AboutLinkRow(Icons.Filled.Link, "tertiaryinfotech.com") {
                            uriHandler.openUri("https://www.tertiaryinfotech.com/")
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Data source")
                InfoCard {
                    Column {
                        AboutLinkRow(Icons.AutoMirrored.Filled.MenuBook, "Tertiary Courses Singapore catalog") {
                            uriHandler.openUri("https://www.tertiarycourses.com.sg/")
                        }
                        HorizontalDivider()
                        AboutLinkRow(Icons.Filled.Verified, "TP Gateway grant calculator & funding eligibility") {
                            uriHandler.openUri("https://www.tpgateway.gov.sg/faq/grant-calculator-and-funding-eligibility")
                        }
                        HorizontalDivider()
                        AboutLinkRow(Icons.Filled.Person, "MySkillsFuture course and credit checks") {
                            uriHandler.openUri("https://www.myskillsfuture.gov.sg/")
                        }
                    }
                }
            }

            InfoCard {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Version", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Theme.secondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutLinkRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Theme.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = Theme.accent,
            fontWeight = FontWeight.Medium
        )
    }
}
