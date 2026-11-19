package com.tertiaryinfotech.tertiarycourses

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch

private val themes = listOf("system" to "System", "light" to "Light", "dark" to "Dark")

fun themeLabel(pref: String): String = themes.firstOrNull { it.first == pref }?.second ?: "System"

/** Name, phone and appearance, saved to the account on the site (so the website and other devices agree). */
@Composable
fun EditProfileScreen(vm: AppViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val learner = vm.learner
    var first by rememberSaveable { mutableStateOf(learner?.firstName.orEmpty()) }
    var last by rememberSaveable { mutableStateOf(learner?.lastName.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(learner?.phone.orEmpty()) }
    var theme by rememberSaveable { mutableStateOf(vm.themePref) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf<String?>(null) }

    val changed = first.trim() != learner?.firstName.orEmpty() || last.trim() != learner?.lastName.orEmpty() ||
        phone.trim() != learner?.phone.orEmpty() || theme != (learner?.theme ?: "system")
    val valid = first.isNotBlank()

    ScreenScaffold(title = "Edit Profile", onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionLabel("Your details", Modifier.padding(start = 4.dp))
            InfoCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileField("First name", first, KeyboardType.Text, "firstName") { first = it }
                    ProfileField("Last name", last, KeyboardType.Text, "lastName") { last = it }
                    ProfileField("Phone", phone, KeyboardType.Phone, "phone") { phone = it }
                    LabeledValue("Email", learner?.email.orEmpty(), fontSize = 15)
                    Text("Your email is your sign-in. Contact us to change it.", fontSize = 12.sp, color = Theme.secondaryText)
                }
            }

            SectionLabel("Appearance", Modifier.padding(top = 10.dp, start = 4.dp))
            InfoCard {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    themes.forEachIndexed { i, (key, label) ->
                        SegmentedButton(
                            selected = theme == key,
                            onClick = { theme = key },
                            shape = SegmentedButtonDefaults.itemShape(i, themes.size),
                            icon = {},
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = Theme.brand, activeContentColor = Color.White, activeBorderColor = Theme.brand,
                                inactiveContainerColor = Theme.field, inactiveContentColor = Theme.ink, inactiveBorderColor = Theme.cardBorder,
                            ),
                            modifier = Modifier.testTag("theme-$key"),
                        ) { Text(label, fontSize = 14.sp, fontWeight = if (theme == key) FontWeight.SemiBold else FontWeight.Normal) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "System follows your phone's light or dark setting. Your choice is saved to your account.",
                    fontSize = 12.sp, color = Theme.secondaryText,
                )
            }

            error?.let { Text(it, color = Theme.danger, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp)) }
            Spacer(Modifier.height(6.dp))
            PrimaryButton("Save", enabled = changed && valid, busy = saving, modifier = Modifier.testTag("saveProfile")) {
                scope.launch {
                    saving = true; error = null
                    try {
                        saved = vm.updateProfile(first, last, phone, theme)
                    } catch (e: Exception) {
                        error = e.message
                    } finally {
                        saving = false
                    }
                }
            }
        }
    }

    saved?.let { msg ->
        AlertDialog(
            onDismissRequest = { saved = null; onBack() },
            confirmButton = { TextButton(onClick = { saved = null; onBack() }) { Text("OK") } },
            title = { Text("Profile") },
            text = { Text(msg) },
        )
    }
}

@Composable
private fun ProfileField(label: String, value: String, type: KeyboardType, tag: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = { onChange(it.take(60)) }, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = type,
            capitalization = if (type == KeyboardType.Text) KeyboardCapitalization.Words else KeyboardCapitalization.None,
        ),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Theme.accent, unfocusedBorderColor = Theme.cardBorder),
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}

// ── Class reminders ──

/** Account → "Class reminders" switch, with the fix when notifications are blocked. */
@Composable
fun RemindersRow(vm: AppViewModel) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(Reminders.canNotify(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = Reminders.canNotify(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = Reminders.canNotify(context)
        // Already refused once ("don't ask again"): only the system settings can turn it on.
        if (!granted) Reminders.openSystemSettings(context)
    }
    fun enable() {
        vm.changeReminders(true)
        if (!Reminders.canNotify(context)) {
            if (Build.VERSION.SDK_INT >= 33 &&
                androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else Reminders.openSystemSettings(context)
        }
    }

    Column {
        Row(
            Modifier.fillMaxWidth().clickable { if (vm.remindersOn) vm.changeReminders(false) else enable() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Theme.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Text("Class reminders", fontSize = 16.sp, color = Theme.ink, modifier = Modifier.weight(1f))
            Switch(
                checked = vm.remindersOn, onCheckedChange = { if (it) enable() else vm.changeReminders(false) },
                colors = SwitchDefaults.colors(checkedTrackColor = Theme.brand, checkedThumbColor = Color.White),
                modifier = Modifier.testTag("remindersSwitch"),
            )
        }
        Text(
            "Reminds you 3 days before a class, the evening before each class day, and an hour before it starts.",
            fontSize = 12.sp, color = Theme.secondaryText, modifier = Modifier.padding(start = 52.dp, end = 16.dp, bottom = 12.dp),
        )
        if (vm.remindersOn && !allowed) {
            Column(Modifier.padding(start = 52.dp, end = 16.dp, bottom = 12.dp)) {
                Text("Notifications are turned off for ${Site.appName}, so reminders can't be shown.", fontSize = 13.sp, color = Theme.warning)
                TextButton(onClick = { Reminders.openSystemSettings(context) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text("Open notification settings", color = Theme.accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Asks for the notification permission once, after the first successful class load (Android 13+). */
@Composable
fun NotificationPermissionPrompt(vm: AppViewModel) {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(vm.classLoads > 0, vm.isSignedIn) {
        val settings = Settings(context)
        if (vm.classLoads > 0 && vm.isSignedIn && vm.remindersOn && !settings.askedNotifications && !Reminders.canNotify(context)) {
            settings.askedNotifications = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
