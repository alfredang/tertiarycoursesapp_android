package com.tertiaryinfotech.tertiarycourses

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.abs
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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

// ── Calendar ──

/**
 * The TIA calendar: a month grid with each day's class titles (swipe or the arrows to change
 * month), the legend and the month's class count, then the chosen day's classes.
 */
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
    val nextDay = byDay.keys.filter { it >= today.toString() && it != selected }.minOrNull()

    fun add(item: ClassItem) {
        notice = addClassToCalendar(context, item)
    }

    ScreenScaffold { padding ->
        Refreshable({ vm.refreshClasses() }, Modifier.padding(padding)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MonthCalendar(
                    month, { monthKey = it.toString() }, selected, { selected = it }, today,
                    titles = { day -> byDay[day].orEmpty().map { it.title } },
                    onToday = { monthKey = YearMonth.from(today).toString(); selected = today.toString() },
                )
                CalendarLegend(byDay.filterKeys { it.startsWith(monthKey) }.values.flatten().map { it.id }.distinct().size)

                Text(Fmt.longDate(selected), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Theme.secondaryText, modifier = Modifier.padding(top = 4.dp))
                val items = byDay[selected] ?: emptyList()
                if (items.isEmpty()) {
                    if (vm.classes == null) LoadingBox("Loading your classes…")
                    else NoClassesCard(nextDay) { day -> selected = day; monthKey = YearMonth.from(LocalDate.parse(day)).toString() }
                }
                items.forEach { item ->
                    ClassCard(item, onClick = classRoute(item)?.let { r -> { push(r) } })
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

/**
 * The month grid card: the month title with Today and the arrows, an uppercase weekday row,
 * then a cell per day with its number in a circle (selected = brand fill, today = soft fill)
 * and up to two class title chips (brand for upcoming, grey for past) and "+N more".
 * A horizontal swipe changes the month. [titles] gives a day's ("2026-11-04") class titles.
 */
@Composable
fun MonthCalendar(
    month: YearMonth,
    onMonth: (YearMonth) -> Unit,
    selected: String,
    onSelect: (String) -> Unit,
    today: LocalDate,
    titles: (String) -> List<String>,
    onToday: () -> Unit,
) {
    val lead = month.atDay(1).dayOfWeek.value - 1 // Monday first
    val cells: List<LocalDate?> = (List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }).let { c ->
        c + List((7 - c.size % 7) % 7) { null }
    }
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Theme.card).border(1.dp, Theme.cardBorder, shape)
            .pointerInput(month) {
                var drag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = { if (abs(drag) > 60.dp.toPx()) onMonth(if (drag < 0) month.plusMonths(1) else month.minusMonths(1)) },
                    onHorizontalDrag = { _, dx -> drag += dx },
                )
            }
            .testTag("monthCalendar"),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(month.atDay(1).format(Fmt.monthYear), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Theme.ink, modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = onToday, contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.height(32.dp).testTag("calendarToday"),
                border = androidx.compose.foundation.BorderStroke(1.dp, Theme.cardBorder),
            ) { Text("Today", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent) }
            IconButton(onClick = { onMonth(month.minusMonths(1)) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month", tint = Theme.accent)
            }
            IconButton(onClick = { onMonth(month.plusMonths(1)) }, modifier = Modifier.padding(start = 0.dp)) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next month", tint = Theme.accent)
            }
        }
        Row(Modifier.fillMaxWidth().background(Theme.page).padding(vertical = 6.dp)) {
            listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach {
                Text(
                    it, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Theme.secondaryText,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        HorizontalDivider(color = Theme.cardBorder)
        val weeks = cells.chunked(7)
        weeks.forEachIndexed { wi, week ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                week.forEachIndexed { i, day ->
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        if (day != null) DayCell(day, selected, today, titles(day.toString()), onSelect)
                        else Box(Modifier.fillMaxWidth().height(72.dp).background(Theme.page.copy(alpha = 0.6f)))
                    }
                    if (i < 6) Box(Modifier.width(1.dp).fillMaxHeight().background(Theme.cardBorder))
                }
            }
            if (wi < weeks.lastIndex) HorizontalDivider(color = Theme.cardBorder)
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, selected: String, today: LocalDate, titles: List<String>, onSelect: (String) -> Unit) {
    val key = day.toString()
    val isSelected = key == selected
    val isToday = day == today
    val past = day.isBefore(today)
    val count = titles.size
    val desc = day.format(DateTimeFormatter.ofPattern("EEEE d MMMM", java.util.Locale.ENGLISH)) +
        if (count == 0) ", no classes" else ", $count class${if (count == 1) "" else "es"}"
    Column(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(if (isSelected) Theme.accentSoft else Color.Transparent)
            .clickable { onSelect(key) }
            .semantics(mergeDescendants = true) { contentDescription = desc; if (isSelected) this.selected = true }
            .padding(3.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            Modifier.size(24.dp).clip(CircleShape)
                .background(if (isSelected) Theme.brand else if (isToday) Theme.accentSoft else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${day.dayOfMonth}", fontSize = 13.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else if (isToday) Theme.accent else Theme.ink,
            )
        }
        titles.take(2).forEach { t ->
            Text(
                t, fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Clip,
                color = if (past) Theme.ink.copy(alpha = 0.7f) else Color.White,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
                    .background(if (past) Theme.muted else Theme.brand).padding(horizontal = 3.dp, vertical = 1.dp),
            )
        }
        if (count > 2) Text("+${count - 2} more", fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.Medium, color = Theme.secondaryText)
    }
}

/** Upcoming / Past legend and the number of classes in the month. */
@Composable
fun CalendarLegend(monthCount: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf("Upcoming" to Theme.brand, "Past" to Theme.muted).forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.width(12.dp).height(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
                Text(label, fontSize = 12.sp, color = Theme.secondaryText)
            }
        }
        Spacer(Modifier.weight(1f))
        Text("$monthCount class${if (monthCount == 1) "" else "es"}", fontSize = 12.sp, color = Theme.secondaryText, modifier = Modifier.testTag("monthCount"))
    }
}

/** The chosen day has no class: say so, and offer the next class day when there is one. */
@Composable
fun NoClassesCard(nextDay: String?, jump: (String) -> Unit) {
    InfoCard {
        Text("No classes on this day", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
        Spacer(Modifier.height(6.dp))
        Text("Pick another day, or swipe the calendar to change month.", fontSize = 14.sp, color = Theme.secondaryText)
        if (nextDay != null) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { jump(nextDay) }, modifier = Modifier.testTag("jumpNextClass")) {
                Text("Jump to next class", color = Theme.accent, fontWeight = FontWeight.SemiBold)
            }
        }
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
                                    PrimaryButton("View PDF", Icons.Filled.Description, modifier = Modifier.weight(1f)) { openDocument(context, push, cert.title, cert.url) }
                                    OutlineButton("Share", Icons.Filled.Share, modifier = Modifier.weight(1f)) {
                                        shareText(context, cert.url, cert.title)
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
fun BillingScreen(vm: AppViewModel, onBack: (() -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    ScreenScaffold(title = if (onBack != null) "Billing" else null, onBack = onBack) { padding ->
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

    ScreenScaffold { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Picture, name, role, contact details and the editable profile (one page, no separate edit screen).
            ProfileSections(vm)

            Spacer(Modifier.height(10.dp))
            InfoCard(padding = 0.dp) {
                RemindersRow(vm)
            }
            Footnote("Reminds you 3 days before a class starts, the evening before each class day, and on the morning of each class.")

            // The learner's Billing tab became Support; their orders and invoices are here.
            if (!vm.isTrainer && vm.staffRole == null) {
                SectionLabel("Your account", Modifier.padding(top = 10.dp, start = 4.dp))
                InfoCard(padding = 0.dp) {
                    MenuRow(Icons.Filled.CreditCard, "Billing", value = vm.orders?.size?.takeIf { it > 0 }?.toString().orEmpty()) { push(Route.Billing) }
                }
            }

            SectionLabel("Explore", Modifier.padding(top = 10.dp, start = 4.dp))
            InfoCard(padding = 0.dp) {
                MenuRow(Icons.AutoMirrored.Filled.LibraryBooks, "Browse Courses") { push(Route.Catalog) }
                MenuDivider()
                MenuRow(Icons.Filled.Language, Site.websiteLabel, external = true) { openUrl(context, Site.baseUrl) }
            }

            SectionLabel("Support", Modifier.padding(top = 10.dp, start = 4.dp))
            InfoCard(padding = 0.dp) {
                // The learner view has its own Support tab.
                if (vm.isTrainer || vm.staffRole != null) {
                    MenuRow(Icons.Filled.SupportAgent, "Contact Support") { push(Route.Support) }
                    MenuDivider()
                }
                MenuRow(Icons.Filled.Forum, "Send Feedback") { push(Route.Feedback) }
                MenuDivider()
                MenuRow(Icons.Filled.Info, "About") { push(Route.About) }
                MenuDivider()
                MenuRow(Icons.Filled.PrivacyTip, "Privacy Policy", external = true) { openUrl(context, Site.privacyUrl) }
            }

            Spacer(Modifier.height(10.dp))
            InfoCard(padding = 0.dp) {
                Text(
                    "Sign Out", fontSize = 16.sp, color = Theme.accent,
                    modifier = Modifier.fillMaxWidth().clickable { confirmSignOut = true }.padding(horizontal = 16.dp, vertical = 14.dp),
                )
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
            confirmButton = { TextButton(onClick = { confirmSignOut = false; vm.signOut() }) { Text("Sign Out", color = Theme.danger) } },
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
