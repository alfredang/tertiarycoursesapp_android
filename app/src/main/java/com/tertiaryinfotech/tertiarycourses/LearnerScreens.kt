package com.tertiaryinfotech.tertiarycourses

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// ── Calendar ──

/** Month view of the learner's class days, with the day's classes below. */
@Composable
fun CalendarScreen(vm: AppViewModel, push: (Route) -> Unit) {
    val context = LocalContext.current
    val today = LocalDate.now()
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val month = YearMonth.parse(monthKey)
    var selected by rememberSaveable { mutableStateOf(today.toString()) }
    var notice by remember { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf(false) }

    val all = vm.classes?.all ?: emptyList()
    val byDay = remember(all) {
        val map = HashMap<String, MutableList<ClassItem>>()
        all.forEach { item ->
            item.classDays.ifEmpty { listOf(item.startDate) }.filter { it.isNotEmpty() }
                .forEach { map.getOrPut(it) { mutableListOf() }.add(item) }
        }
        map
    }
    val upcoming = all.filter { it.bucket != "past" && it.runId > 0 && it.startDate.isNotEmpty() }

    fun add(item: ClassItem) {
        notice = addClassToCalendar(context, item)
    }

    ScreenScaffold { padding ->
        Refreshable({ vm.refreshClasses() }, Modifier.padding(padding)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                InfoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { monthKey = month.minusMonths(1).toString() }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month", tint = Theme.accent)
                        }
                        Text(
                            month.atDay(1).format(Fmt.monthYear), fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        IconButton(onClick = { monthKey = month.plusMonths(1).toString() }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Next month", tint = Theme.accent)
                        }
                    }
                    MonthGrid(month, selected, today.toString(), hasClass = { byDay.containsKey(it) }) { selected = it }
                }

                SectionLabel(Fmt.longDate(selected))
                val items = byDay[selected] ?: emptyList()
                if (items.isEmpty()) {
                    Text(if (vm.classes == null) "Loading your classes…" else "No class on this day.", fontSize = 15.sp, color = Theme.secondaryText)
                }
                items.forEach { item ->
                    ClassCard(item, showRole = vm.isTrainer, onClick = classRoute(item)?.let { r -> { push(r) } })
                }
                if (upcoming.isNotEmpty()) {
                    TonalButton("Add My Classes to Calendar", Icons.Filled.EditCalendar, modifier = Modifier.padding(top = 6.dp)) {
                        if (upcoming.size == 1) add(upcoming.first()) else picking = true
                    }
                }
            }
        }
    }

    if (picking) {
        // One ACTION_INSERT intent per class: no calendar permission is needed.
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton(onClick = { picking = false }) { Text("Done") } },
            title = { Text("Add to Calendar") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Choose a class to add to your calendar app.", fontSize = 14.sp, color = Theme.secondaryText)
                    upcoming.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { add(item) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(Fmt.range(item.startDate, item.endDate), fontSize = 13.sp, color = Theme.secondaryText)
                            }
                            Icon(Icons.Filled.EditCalendar, contentDescription = "Add", tint = Theme.accent)
                        }
                    }
                }
            },
        )
    }
    notice?.let { msg ->
        AlertDialog(
            onDismissRequest = { notice = null },
            confirmButton = { TextButton(onClick = { notice = null }) { Text("OK") } },
            title = { Text("Calendar") },
            text = { Text(msg) },
        )
    }
}

@Composable
private fun MonthGrid(month: YearMonth, selected: String, today: String, hasClass: (String) -> Boolean, onSelect: (String) -> Unit) {
    val lead = month.atDay(1).dayOfWeek.value - 1 // Monday first
    val cells: List<LocalDate?> = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val symbols = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.padding(bottom = 2.dp)) {
            symbols.forEach {
                Text(it, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Theme.secondaryText,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until 7).forEach { i ->
                    val day = week.getOrNull(i)
                    Box(Modifier.weight(1f)) {
                        if (day != null) DayCell(day, selected, today, hasClass(day.toString()), onSelect)
                        else Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, selected: String, today: String, hasClass: Boolean, onSelect: (String) -> Unit) {
    val key = day.toString()
    val isSelected = key == selected
    val isToday = key == today
    val desc = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)) + if (hasClass) ", class day" else ""
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Theme.brand else if (hasClass) Theme.accentSoft else Color.Transparent)
            .clickable { onSelect(key) }
            .semantics(mergeDescendants = true) { contentDescription = desc }
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "${day.dayOfMonth}", fontSize = 15.sp,
            fontWeight = if (isToday || hasClass) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else if (isToday) Theme.accent else Theme.ink,
        )
        Spacer(Modifier.height(3.dp))
        Box(Modifier.size(5.dp).clip(CircleShape).background(if (!hasClass) Color.Transparent else if (isSelected) Color.White else Theme.accent))
    }
}

/** Opens the calendar app's "new event" screen for one class. Returns an error message, or null. */
private fun addClassToCalendar(context: Context, item: ClassItem): String? {
    val days = item.classDays.ifEmpty { listOf(item.startDate) }.mapNotNull { Fmt.date(it) }.sorted()
    val first = days.firstOrNull() ?: return "This class has no confirmed date yet."
    val zone = try { ZoneId.of(Site.timeZone) } catch (e: Exception) { ZoneId.systemDefault() }
    fun at(d: LocalDate, hm: String, fallbackHour: Int): Long {
        val p = hm.split(":").mapNotNull { it.toIntOrNull() }
        val t = if (p.size >= 2) LocalTime.of(p[0].coerceIn(0, 23), p[1].coerceIn(0, 59)) else LocalTime.of(fallbackHour, 0)
        return d.atTime(t).atZone(zone).toInstant().toEpochMilli()
    }
    val location = if (item.mode == "Live Online") item.virtualLink else listOf(item.room, item.venue).filter { it.isNotEmpty() }.joinToString(", ")
    val consecutive = days.zipWithNext().all { (a, b) -> b == a.plusDays(1) }
    val notes = buildString {
        append(listOf(item.classId, Site.siteName).filter { it.isNotEmpty() }.joinToString(" · "))
        if (days.size > 1) append("\nClass days: " + days.joinToString(", ") { Fmt.longDate(it.toString()) })
    }
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, at(first, item.startTime, 9))
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, at(first, item.endTime, 17))
        .putExtra(CalendarContract.Events.TITLE, item.title)
        .putExtra(CalendarContract.Events.EVENT_LOCATION, location)
        .putExtra(CalendarContract.Events.DESCRIPTION, notes)
        .putExtra(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
    if (days.size > 1 && consecutive) intent.putExtra(CalendarContract.Events.RRULE, "FREQ=DAILY;COUNT=${days.size}")
    return try {
        context.startActivity(intent)
        null
    } catch (e: ActivityNotFoundException) {
        "No calendar app is installed on this device."
    }
}

// ── Certificates ──

@Composable
fun CertificatesScreen(vm: AppViewModel, push: (Route) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    ScreenScaffold { padding ->
        Refreshable({ vm.refreshCertificates() }, Modifier.padding(padding)) {
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
                val certs = vm.certificates
                when {
                    certs != null && certs.isEmpty() -> item {
                        EmptyState(Icons.Filled.WorkspacePremium, "No certificates yet", "Certificates are issued after class when attendance requirements are met.")
                    }
                    certs != null -> items(certs, key = { it.id }) { cert ->
                        InfoCard {
                            IconLine(Icons.Filled.WorkspacePremium, cert.name, color = Theme.accent, iconTint = Theme.accent, fontSize = 12, weight = FontWeight.SemiBold, iconSize = 16.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(cert.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                listOf(cert.certNo, Fmt.range(cert.startDate, cert.endDate)).filter { it.isNotEmpty() }.joinToString(" · "),
                                fontSize = 13.sp, color = Theme.secondaryText,
                            )
                            if (cert.url.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PrimaryButton("View PDF", Icons.Filled.Description, modifier = Modifier.weight(1f)) { push(Route.Document(cert.title, cert.url)) }
                                    OutlineButton("Share", Icons.Filled.Share, modifier = Modifier.weight(1f)) {
                                        shareText(context, "${cert.title} — ${cert.name}\n${cert.url}", cert.title)
                                    }
                                }
                            }
                        }
                    }
                    vm.loadError != null -> item { ErrorCard(vm.loadError!!) { scope.launch { vm.refreshCertificates() } } }
                    else -> item { LoadingBox() }
                }
            }
        }
    }
}

// ── Billing ──

@Composable
fun BillingScreen(vm: AppViewModel) {
    val scope = rememberCoroutineScope()
    ScreenScaffold { padding ->
        Refreshable({ vm.refreshBilling() }, Modifier.padding(padding)) {
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
                val orders = vm.orders
                when {
                    orders != null && orders.isEmpty() -> item {
                        EmptyState(Icons.Filled.CreditCard, "No registrations yet", "Your course registrations and payments appear here.")
                    }
                    orders != null -> items(orders, key = { it.orderNo }) { OrderCard(it) }
                    vm.loadError != null -> item { ErrorCard(vm.loadError!!) { scope.launch { vm.refreshBilling() } } }
                    else -> item { LoadingBox() }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order) {
    val statusColor = when (order.status) {
        "paid" -> Theme.success
        "pending" -> Theme.warning
        else -> Theme.secondaryText
    }
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Order #${order.orderNo}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            StatusChip(order.statusLabel, statusColor)
        }
        order.items.forEach { line ->
            Spacer(Modifier.height(10.dp))
            Text(line.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
            if (line.classDate.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(listOf(line.classDate, line.classTime).filter { it.isNotEmpty() }.joinToString(" · "), fontSize = 13.sp, color = Theme.secondaryText)
            }
            Spacer(Modifier.height(3.dp))
            LabeledValue(line.sku + if (line.qty > 1) " × ${line.qty.toInt()}" else "", Fmt.money(line.amount, order.currency), fontSize = 13, color = Theme.secondaryText)
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Theme.cardBorder)
        LabeledValue("Total", Fmt.money(order.total, order.currency), fontSize = 15, weight = FontWeight.SemiBold)
        if (order.refunded > 0) {
            Spacer(Modifier.height(4.dp))
            LabeledValue("Refunded", Fmt.money(order.refunded, order.currency), fontSize = 13)
        }
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LabeledValue("Ordered", Fmt.longDate(order.date), fontSize = 13, color = Theme.secondaryText)
            if (order.paymentMethod.isNotEmpty()) LabeledValue("Payment", order.paymentMethod, fontSize = 13, color = Theme.secondaryText)
            if (order.invoices.isNotEmpty()) LabeledValue("Invoice", order.invoices.joinToString(", "), fontSize = 13, color = Theme.secondaryText)
        }
    }
}

// ── Account ──

@Composable
fun AccountScreen(vm: AppViewModel, push: (Route) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val learner = vm.learner

    ScreenScaffold { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            InfoCard(modifier = Modifier.clickable { push(Route.EditProfile) }.testTag("editProfile")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(learner?.fullName?.ifBlank { null } ?: "Learner", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            if (vm.isTrainer) StatusChip("Trainer", Theme.accent)
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(learner?.email.orEmpty(), fontSize = 15.sp, color = Theme.secondaryText)
                        if (learner?.phone?.isNotEmpty() == true) Text(learner.phone, fontSize = 15.sp, color = Theme.secondaryText)
                    }
                    Text("Edit", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent)
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Theme.tertiaryText)
                }
            }

            SectionLabel("Preferences", Modifier.padding(top = 10.dp, start = 4.dp))
            InfoCard(padding = 0.dp) {
                MenuRow(Icons.Filled.Palette, "Appearance", value = themeLabel(vm.themePref)) { push(Route.EditProfile) }
                MenuDivider()
                RemindersRow(vm)
            }

            SectionLabel("Explore", Modifier.padding(top = 10.dp, start = 4.dp))
            InfoCard(padding = 0.dp) {
                MenuRow(Icons.AutoMirrored.Filled.LibraryBooks, "Browse Courses") { push(Route.Catalog) }
                MenuDivider()
                MenuRow(Icons.Filled.Language, Site.websiteLabel, external = true) { openUrl(context, Site.baseUrl) }
            }

            SectionLabel("Support", Modifier.padding(top = 10.dp, start = 4.dp))
            InfoCard(padding = 0.dp) {
                MenuRow(Icons.Filled.Forum, "Send Feedback") { push(Route.Feedback) }
                MenuDivider()
                MenuRow(Icons.Filled.Info, "About") { push(Route.About) }
                MenuDivider()
                MenuRow(Icons.Filled.PrivacyTip, "Privacy Policy", external = true) { openUrl(context, Site.privacyUrl) }
            }

            Spacer(Modifier.height(10.dp))
            InfoCard(padding = 0.dp) {
                MenuRow(Icons.AutoMirrored.Filled.Logout, "Sign Out", tint = Theme.accent, chevron = false) { confirmSignOut = true }
            }

            Spacer(Modifier.height(10.dp))
            InfoCard(padding = 0.dp) {
                MenuRow(Icons.Filled.DeleteOutline, "Delete Account", tint = Theme.danger, chevron = false, busy = deleting) {
                    if (!deleting) confirmDelete = true
                }
            }
            Text(
                "Permanently deletes your ${Site.siteName} login account. Records we must keep by law — registrations, invoices, attendance and certificates — are retained.",
                fontSize = 12.sp, color = Theme.secondaryText, modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out of ${Site.appName}?") },
            confirmButton = { TextButton(onClick = { confirmSignOut = false; vm.signOut() }) { Text("Sign Out") } },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete your account?") },
            text = { Text("This removes your login for the app and ${Site.websiteLabel}. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        deleting = true
                        try { vm.deleteAccount() } catch (e: Exception) { message = e.message } finally { deleting = false }
                    }
                }) { Text("Delete Account Permanently", color = Theme.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
    message?.let { msg ->
        AlertDialog(
            onDismissRequest = { message = null },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } },
            title = { Text("Account") },
            text = { Text(msg) },
        )
    }
}

@Composable
fun MenuRow(
    icon: ImageVector,
    text: String,
    tint: Color = Theme.accent,
    chevron: Boolean = true,
    external: Boolean = false,
    busy: Boolean = false,
    value: String = "",
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, fontSize = 16.sp, color = if (tint == Theme.danger) Theme.danger else Theme.ink, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) Text(value, fontSize = 15.sp, color = Theme.secondaryText, modifier = Modifier.padding(end = 4.dp))
        when {
            busy -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            external -> Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Theme.tertiaryText, modifier = Modifier.size(18.dp))
            chevron -> Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Theme.tertiaryText)
        }
    }
}

@Composable
fun MenuDivider() = HorizontalDivider(Modifier.padding(start = 52.dp), color = Theme.cardBorder)
