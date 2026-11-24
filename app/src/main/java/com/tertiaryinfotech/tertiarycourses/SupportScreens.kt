package com.tertiaryinfotech.tertiarycourses

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class FeedbackCategory(val label: String, val icon: ImageVector, val prompt: String) {
    FEATURE("Feature Idea", Icons.Filled.Lightbulb, "What would make this app more useful? e.g. class reminders, offline materials…"),
    BUG("Bug Report", Icons.Filled.BugReport, "What went wrong? Tell us what you did, what you expected, and what happened instead."),
    COMMENT("Comment", Icons.Filled.ChatBubble, "Any comments about the app or our courses — we read everything."),
}

/** App feedback, sent to the site's team on WhatsApp. */
@Composable
fun FeedbackScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    var category by rememberSaveable { mutableStateOf(FeedbackCategory.FEATURE) }
    var title by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    val canSend = title.isNotBlank() || message.isNotBlank()

    ScreenScaffold(title = "Feedback", onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            InfoCard {
                IconLine(Icons.Filled.Favorite, "We'd love to hear from you", color = Theme.accent, iconTint = Theme.accent, fontSize = 17, weight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Suggest features, report bugs, or leave a comment. Your feedback goes straight to our team on WhatsApp.",
                    fontSize = 15.sp, color = Theme.secondaryText, lineHeight = 21.sp,
                )
            }

            SectionLabel("What kind of feedback?")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeedbackCategory.entries.forEach { item ->
                    val selected = category == item
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Theme.brand else Theme.accentSoft)
                            .clickable { category = item }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(item.icon, contentDescription = null, tint = if (selected) Color.White else Theme.accent)
                        Text(item.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, color = if (selected) Color.White else Theme.accent)
                    }
                }
            }

            SectionLabel("Your feedback")
            val colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Theme.accent, unfocusedBorderColor = Theme.cardBorder,
                focusedContainerColor = Theme.card, unfocusedContainerColor = Theme.card,
            )
            OutlinedTextField(
                value = title, onValueChange = { title = it }, placeholder = { Text("Short title") }, singleLine = true,
                shape = RoundedCornerShape(12.dp), colors = colors, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = message, onValueChange = { message = it }, placeholder = { Text(category.prompt) }, minLines = 6,
                shape = RoundedCornerShape(12.dp), colors = colors, modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton("Send via WhatsApp", Icons.AutoMirrored.Filled.Send, enabled = canSend) {
                val body = buildString {
                    append("${category.label} — ${Site.appName} app\n")
                    if (title.isNotBlank()) append("*${title.trim()}*\n")
                    append(message.trim())
                }
                openWhatsApp(context, Site.whatsApp, body)
            }
            Text(
                "Sent to ${Site.siteName} (${Site.whatsAppLabel}) via WhatsApp.",
                fontSize = 13.sp, color = Theme.secondaryText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun AboutScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    ScreenScaffold(title = if (onBack != null) "About" else null, onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoCard {
                Text(Site.appName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "The app for learners and trainers of ${Site.siteName}. Sign in with your email code or password to see your current, upcoming and past classes, your class calendar and materials, and get reminders before each class. Learners check in to class, give course feedback, and download their certificates and billing records. Trainers take attendance and open the class materials. You can also browse our courses.",
                    fontSize = 16.sp, color = Theme.secondaryText, lineHeight = 21.sp,
                )
            }

            SectionLabel("Contact", Modifier.padding(top = 10.dp, start = 4.dp))
            InfoCard(padding = 0.dp) {
                AboutLinkRow(Icons.Filled.Language, Site.websiteLabel) { openUrl(context, Site.baseUrl) }
                MenuDivider()
                AboutLinkRow(Icons.Filled.Phone, Site.phone) { openUrl(context, "tel:" + Site.phone.filter { it.isDigit() || it == '+' }) }
                MenuDivider()
                AboutLinkRow(Icons.Filled.Email, Site.email) { openUrl(context, "mailto:" + Site.email) }
                MenuDivider()
                AboutLinkRow(Icons.AutoMirrored.Filled.Chat, "WhatsApp ${Site.whatsAppLabel}") {
                    openWhatsApp(context, Site.whatsApp, "Hi, I have a question about ${Site.siteName}.")
                }
            }

            SectionLabel("Developer", Modifier.padding(top = 10.dp, start = 4.dp))
            InfoCard(padding = 0.dp) {
                Text(Site.academy, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp))
                AboutLinkRow(Icons.Filled.Link, "tertiaryinfotech.com") { openUrl(context, Site.developerUrl) }
                MenuDivider()
                AboutLinkRow(Icons.Filled.PrivacyTip, "Privacy Policy") { openUrl(context, Site.privacyUrl) }
            }

            SectionLabel("Data source", Modifier.padding(top = 10.dp, start = 4.dp))
            InfoCard {
                Text(
                    "Courses, classes, attendance, certificates and billing come from your account on ${Site.websiteLabel}.",
                    fontSize = 15.sp, color = Theme.secondaryText, lineHeight = 21.sp,
                )
            }

            InfoCard {
                LabeledValue("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", fontSize = 15)
            }
        }
    }
}

@Composable
private fun AboutLinkRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Theme.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, fontSize = 16.sp, color = Theme.accent)
    }
}

// ── Support tab ──

/**
 * Support, like the TIA app's: a feedback form emailed to the academy (`POST support`),
 * then the ways to reach it (Email, Tel, WhatsApp) and its address. The contacts come
 * from `GET support`, falling back to this site's constants.
 */
@Composable
fun SupportScreen(vm: AppViewModel, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var subject by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf<String?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { if (vm.support == null) vm.loadSupport() }
    val info = vm.support ?: SupportInfo.fallback

    ScreenScaffold(title = if (onBack != null) "Support" else null, onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InfoCard {
                Text("We're here to help", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent)
                Spacer(Modifier.height(6.dp))
                Text("Ask a question or tell us what went wrong. Your message is emailed to the academy with your name and email, so we can reply.", fontSize = 14.sp, color = Theme.secondaryText)
            }
            SectionLabel("Your message", Modifier.padding(top = 12.dp, start = 4.dp))
            InfoCard {
                val colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Theme.accent, unfocusedBorderColor = Theme.cardBorder,
                    focusedContainerColor = Theme.card, unfocusedContainerColor = Theme.card,
                )
                OutlinedTextField(
                    value = subject, onValueChange = { subject = it.take(150); sent = null }, placeholder = { Text("Subject") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = colors, modifier = Modifier.fillMaxWidth().testTag("supportSubject"),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = message, onValueChange = { message = it.take(5000); sent = null },
                    placeholder = { Text("How can we help? Tell us what happened.") }, minLines = 4, maxLines = 10,
                    shape = RoundedCornerShape(12.dp), colors = colors, modifier = Modifier.fillMaxWidth().testTag("supportMessage"),
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    if (sending) "Sending…" else "Send message", Icons.AutoMirrored.Filled.Send,
                    enabled = subject.isNotBlank() && message.isNotBlank(), busy = sending, modifier = Modifier.testTag("supportSend"),
                ) {
                    scope.launch {
                        sending = true; sent = null; problem = null
                        try {
                            sent = vm.sendSupport(subject, message)
                            subject = ""; message = ""
                        } catch (e: Exception) {
                            problem = e.message ?: "Your message could not be sent. Please try again."
                        } finally {
                            sending = false
                        }
                    }
                }
                sent?.let {
                    Spacer(Modifier.height(10.dp))
                    IconLine(Icons.Filled.CheckCircle, it, color = Theme.success, iconTint = Theme.success, fontSize = 14, weight = FontWeight.SemiBold)
                }
                problem?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, fontSize = 13.sp, color = Theme.danger)
                }
            }

            SectionLabel("Contact us", Modifier.padding(top = 12.dp, start = 4.dp))
            InfoCard(padding = 0.dp) {
                SupportContactRow(Icons.Filled.Email, "Email", info.email) { openUrl(context, "mailto:" + info.email) }
                MenuDivider()
                SupportContactRow(Icons.Filled.Phone, "Tel", info.phone) { openUrl(context, "tel:" + info.phone.filter { it.isDigit() || it == '+' }) }
                MenuDivider()
                SupportContactRow(Icons.AutoMirrored.Filled.Chat, "WhatsApp", info.whatsApp) {
                    val digits = info.whatsApp.filter { it.isDigit() }.ifEmpty { Site.whatsApp }
                    openUrl(context, "https://wa.me/$digits")
                }
            }

            if (info.address.isNotEmpty()) {
                SectionLabel("Address", Modifier.padding(top = 12.dp, start = 4.dp))
                InfoCard { VenueLink(info.address) }
            }
        }
    }
}

@Composable
private fun SupportContactRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp).testTag("contact-$title"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Theme.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, color = Theme.secondaryText)
            Text(value, fontSize = 16.sp, color = Theme.ink)
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Theme.tertiaryText, modifier = Modifier.size(18.dp))
    }
}
