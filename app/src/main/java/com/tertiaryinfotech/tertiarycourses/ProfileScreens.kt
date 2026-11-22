package com.tertiaryinfotech.tertiarycourses

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch

private val themes = listOf("system" to "System", "light" to "Light", "dark" to "Dark")

// ── Profile sections (the top of the Account page) ──

private fun digits(phone: String, plus: Boolean = false): String =
    (if (plus && phone.trim().startsWith("+")) "+" else "") + phone.filter { it.isDigit() }

/** wa.me needs the country code: a bare 8-digit Singapore number gets 65. */
private fun whatsAppNumber(phone: String): String {
    val d = digits(phone)
    return if (d.length == 8 && Site.code == "SG") "65$d" else d
}

/**
 * Who you are (picture, name, role, trainer badges, contact details), then the editable
 * profile: name, phone, LinkedIn link, certifications and appearance, saved to the account
 * on the site so the website and other devices agree. Emits cards into the Account column.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSections(vm: AppViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val learner = vm.learner
    var first by rememberSaveable { mutableStateOf(learner?.firstName.orEmpty()) }
    var last by rememberSaveable { mutableStateOf(learner?.lastName.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(learner?.phone.orEmpty()) }
    var linkedin by rememberSaveable { mutableStateOf(learner?.linkedinUrl.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var avatarBusy by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf<String?>(null) }
    var pictureMenu by remember { mutableStateOf(false) }

    // Trainer on the website, whatever view is showing.
    val isRealTrainer = learner?.isTrainer == true
    val isStaffOrTrainer = isRealTrainer || vm.availableRoles.any { it in AppViewModel.staffRoleNames }
    val certs = vm.certDrafts
    // Short codes of the trainer qualifications on the certifications list.
    val badges = TrainerBadge.all.filter { b -> certs.any { b.matches(it.name) } }.map { it.code }

    // The system photo picker: no storage permission is needed.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            avatarBusy = true
            try { vm.setAvatar(uri) } catch (e: Exception) { avatarError = e.message ?: "That picture could not be used." }
            avatarBusy = false
        }
    }
    LaunchedEffect(isRealTrainer) {
        if (isRealTrainer && !vm.certsLoaded) try { vm.loadCertifications() } catch (_: Exception) { }
    }

    // Who you are.
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AvatarView(vm.avatar, learner?.fullName.orEmpty(), 104.dp)
            if (avatarBusy) {
                Box(Modifier.size(104.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                }
            }
            Box {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(Theme.card).padding(2.dp).clip(CircleShape).background(Theme.brand)
                        .clickable(enabled = !avatarBusy) { pictureMenu = true }
                        .semantics { contentDescription = "Change picture" }.testTag("changePicture"),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                DropdownMenu(expanded = pictureMenu, onDismissRequest = { pictureMenu = false }, containerColor = Theme.card) {
                    DropdownMenuItem(
                        text = { Text("Choose Photo") },
                        leadingIcon = { Icon(Icons.Filled.Photo, contentDescription = null) },
                        onClick = {
                            pictureMenu = false
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    )
                    if (vm.avatar != null) DropdownMenuItem(
                        text = { Text("Remove Picture", color = Theme.danger) },
                        leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = Theme.danger) },
                        onClick = {
                            pictureMenu = false
                            scope.launch { try { vm.removeAvatar() } catch (_: Exception) { } }
                        },
                    )
                }
            }
        }
        Text(
            learner?.fullName?.ifBlank { null } ?: "My account", fontSize = 22.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, lineHeight = 28.sp,
        )
        StatusChip(AppViewModel.roleName(vm.activeRole), Theme.accent)
        if (badges.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Qualifications: " + badges.joinToString(", ") },
            ) {
                badges.forEach { badge ->
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Theme.success.copy(alpha = 0.12f)).padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = Theme.success, modifier = Modifier.size(14.dp))
                        Text(badge, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Theme.success)
                    }
                }
            }
        }
    }

    SectionLabel("Contact", Modifier.padding(top = 10.dp, start = 4.dp))
    InfoCard(padding = 0.dp) {
        val email = learner?.email.orEmpty()
        ContactRow(Icons.Filled.Email, "Email", email, "mailto:$email")
        val p = learner?.phone.orEmpty()
        if (p.isNotEmpty()) {
            MenuDivider()
            ContactRow(Icons.Filled.Phone, "Phone", p, "tel:" + digits(p, plus = true))
            MenuDivider()
            ContactRow(Icons.AutoMirrored.Filled.Chat, "WhatsApp", p, "https://wa.me/" + whatsAppNumber(p))
        }
        val link = learner?.linkedinUrl.orEmpty()
        if (link.isNotEmpty()) {
            MenuDivider()
            ContactRow(Icons.Filled.Link, "LinkedIn", link.replace("https://", ""), link)
        }
    }

    SectionLabel("Name", Modifier.padding(top = 10.dp, start = 4.dp))
    InfoCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileField("First name", first, KeyboardType.Text, "firstName") { first = it }
            ProfileField("Last name", last, KeyboardType.Text, "lastName") { last = it }
        }
    }

    SectionLabel("Edit contact", Modifier.padding(top = 10.dp, start = 4.dp))
    InfoCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileField("Phone", phone, KeyboardType.Phone, "phone") { phone = it }
            if (isStaffOrTrainer) ProfileField("LinkedIn profile link", linkedin, KeyboardType.Uri, "linkedinField", max = 255) { linkedin = it }
        }
    }
    Footnote("Your phone number is also used for WhatsApp. Your sign-in email is ${learner?.email.orEmpty()}; contact us to change it.")

    if (isRealTrainer) {
        SectionLabel("Certifications", Modifier.padding(top = 10.dp, start = 4.dp))
        InfoCard {
            certs.forEachIndexed { i, c ->
                // Swipe a certification to the left to delete it (as on iOS).
                key(c.id) {
                    val remove = { certs.removeAll { it.id == c.id } }
                    val swipe = rememberSwipeToDismissBoxState(
                        confirmValueChange = { v -> if (v == SwipeToDismissBoxValue.EndToStart) { remove(); true } else false },
                    )
                    SwipeToDismissBox(
                        state = swipe,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Row(
                                Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(Color(0xFFD32F2F)).padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End,
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text("Delete", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            }
                        },
                        modifier = Modifier.testTag("cert$i").semantics {
                            customActions = listOf(CustomAccessibilityAction("Delete certification") { remove(); true })
                        },
                    ) {
                        Column(Modifier.fillMaxWidth().background(Theme.card), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProfileField("Certification", c.name, KeyboardType.Text, "certName$i", max = 255) { certs[i] = c.copy(name = it) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ProfileField("Issuer", c.issuer, KeyboardType.Text, "certIssuer$i", Modifier.weight(1f), max = 255) { certs[i] = c.copy(issuer = it) }
                                ProfileField("Year", c.year, KeyboardType.Number, "certYear$i", Modifier.width(92.dp), max = 4) { v ->
                                    certs[i] = c.copy(year = v.filter { it.isDigit() })
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Theme.cardBorder)
            }
            TextButton(onClick = { certs.add(CertDraft()) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.testTag("addCertification")) {
                Icon(Icons.Filled.AddCircle, contentDescription = null, tint = Theme.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Certification", color = Theme.accent, fontSize = 16.sp)
            }
            // One-tap trainer qualifications that are not on the list yet.
            val missing = TrainerBadge.all.filter { it.code !in badges }
            if (missing.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    missing.forEach { b ->
                        Row(
                            Modifier.clip(RoundedCornerShape(50)).background(Theme.accentSoft)
                                .clickable { certs.add(CertDraft(b.name, b.issuer)) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .semantics { contentDescription = "Add ${b.code}" },
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = Theme.accent, modifier = Modifier.size(14.dp))
                            Text(b.code, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent)
                        }
                    }
                }
            }
        }
        Footnote("Trainer qualifications such as ACLP and DDDLP show as badges under your name. Saved with Save Changes and shared with the web app.")
    }

    SectionLabel("Appearance", Modifier.padding(top = 10.dp, start = 4.dp))
    InfoCard {
        val theme = vm.themePref
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            themes.forEachIndexed { i, (key, label) ->
                SegmentedButton(
                    selected = theme == key,
                    // Shown straight away; Save Changes stores it on the account.
                    onClick = { vm.previewTheme(key) },
                    shape = SegmentedButtonDefaults.itemShape(i, themes.size),
                    icon = {},
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Theme.card, activeContentColor = Theme.accent, activeBorderColor = Theme.cardBorder,
                        inactiveContainerColor = Theme.field, inactiveContentColor = Theme.ink, inactiveBorderColor = Theme.cardBorder,
                    ),
                    modifier = Modifier.testTag("theme-$key"),
                ) { Text(label, fontSize = 14.sp, fontWeight = if (theme == key) FontWeight.SemiBold else FontWeight.Normal) }
            }
        }
    }
    Footnote("System follows your phone's light or dark setting. Your choice is saved to your account.")

    Spacer(Modifier.height(6.dp))
    PrimaryButton("Save Changes", enabled = first.isNotBlank(), busy = saving, modifier = Modifier.testTag("saveProfile")) {
        scope.launch {
            saving = true
            try {
                var notice = vm.updateProfile(first, last, phone, if (isStaffOrTrainer) linkedin else null, vm.themePref)
                if (isRealTrainer && vm.certsLoaded) {
                    vm.saveCertifications()
                    notice = "Profile and certifications saved."
                }
                message = notice
            } catch (e: Exception) {
                message = e.message ?: "Something went wrong. Please try again."
            } finally {
                saving = false
            }
        }
    }

    message?.let { msg ->
        AlertDialog(
            onDismissRequest = { message = null },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } },
            title = { Text("Profile") },
            text = { Text(msg) },
        )
    }
    avatarError?.let { msg ->
        AlertDialog(
            onDismissRequest = { avatarError = null },
            confirmButton = { TextButton(onClick = { avatarError = null }) { Text("OK") } },
            title = { Text("Picture") },
            text = { Text(msg) },
        )
    }
}

@Composable
fun Footnote(text: String) {
    Text(text, fontSize = 12.sp, color = Theme.secondaryText, modifier = Modifier.padding(horizontal = 4.dp))
}

/** The profile picture, or the person's initials on the brand colour. */
@Composable
fun AvatarView(image: ImageBitmap?, name: String, size: Dp = 80.dp) {
    val initials = name.split(" ").filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1) }.uppercase().ifEmpty { "?" }
    Box(Modifier.size(size).clip(CircleShape).background(Theme.brand), contentAlignment = Alignment.Center) {
        if (image != null) Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text(initials, fontSize = (size.value * 0.38f).sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

/** One contact detail; tapping it calls, emails, opens WhatsApp or the LinkedIn profile. */
@Composable
private fun ContactRow(icon: ImageVector, title: String, value: String, url: String) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clickable { openUrl(context, url) }.padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$title: $value" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(Theme.accentSoft), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Theme.accent, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, color = Theme.secondaryText)
            Text(value, fontSize = 15.sp, color = Theme.ink, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
        }
        Icon(Icons.Filled.ArrowOutward, contentDescription = null, tint = Theme.tertiaryText, modifier = Modifier.size(16.dp))
    }
}

/**
 * The chosen photo as a centred [side]-pixel square, upright (camera photos carry their
 * rotation in EXIF). Null when it cannot be read.
 */
fun squarePicture(context: Context, photo: Uri, side: Int = 512): Bitmap? = try {
    fun sampleFor(width: Int, height: Int): Int {
        // Decode large photos at a reduced size; [side] pixels are all that is kept.
        var sample = 1
        while (minOf(width, height) / (sample * 2) >= side) sample *= 2
        return sample
    }
    val resolver = context.contentResolver
    val source: Bitmap? = if (Build.VERSION.SDK_INT >= 28) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, photo)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSampleSize(sampleFor(info.size.width, info.size.height))
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(photo)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleFor(bounds.outWidth, bounds.outHeight) }
        val raw = resolver.openInputStream(photo)?.use { BitmapFactory.decodeStream(it, null, options) }
        val degrees = resolver.openInputStream(photo)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        if (raw == null || degrees == 0f) raw
        else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(degrees) }, true)
    }
    if (source == null || source.width <= 0 || source.height <= 0) null else {
        val crop = minOf(source.width, source.height)
        val left = (source.width - crop) / 2
        val top = (source.height - crop) / 2
        Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888).also { out ->
            Canvas(out).apply {
                drawColor(android.graphics.Color.WHITE)   // JPEG has no transparency
                drawBitmap(source, Rect(left, top, left + crop, top + crop), Rect(0, 0, side, side), Paint(Paint.FILTER_BITMAP_FLAG))
            }
        }
    }
} catch (e: Exception) {
    null
}

@Composable
private fun ProfileField(
    label: String, value: String, type: KeyboardType, tag: String,
    modifier: Modifier = Modifier.fillMaxWidth(), max: Int = 60, onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value, onValueChange = { onChange(it.take(max)) }, label = { Text(label, maxLines = 1) }, singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = type,
            capitalization = if (type == KeyboardType.Text) KeyboardCapitalization.Words else KeyboardCapitalization.None,
            autoCorrectEnabled = type == KeyboardType.Text,
        ),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Theme.accent, unfocusedBorderColor = Theme.cardBorder),
        modifier = modifier.testTag(tag),
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
        if (vm.remindersOn && !allowed) {
            MenuDivider()
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)) {
                Text("Notifications are turned off for ${Site.appName} in Settings, so reminders can't be shown.", fontSize = 13.sp, color = Theme.secondaryText)
                TextButton(onClick = { Reminders.openSystemSettings(context) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text("Open Settings", fontSize = 13.sp, color = Theme.accent, fontWeight = FontWeight.SemiBold)
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
