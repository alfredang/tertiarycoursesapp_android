package com.tertiaryinfotech.tertiarycourses

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val ALL = "All"

fun categoryIcon(category: String): ImageVector = when (category) {
    "Artificial Intelligence" -> Icons.Filled.AutoAwesome
    "Data & Analytics" -> Icons.Filled.Insights
    "Programming" -> Icons.Filled.Code
    "Cloud & DevOps" -> Icons.Filled.Cloud
    "Cybersecurity" -> Icons.Filled.Security
    "Digital Marketing" -> Icons.Filled.Campaign
    "Design & Media" -> Icons.Filled.Brush
    "Microsoft Office" -> Icons.Filled.GridView
    "Business & Soft Skills" -> Icons.Filled.BusinessCenter
    "Engineering & IoT" -> Icons.Filled.Memory
    "Quality & Compliance" -> Icons.Filled.Verified
    else -> Icons.AutoMirrored.Filled.LibraryBooks
}

/** Public short-course catalog of this site, grouped into collapsible categories. */
@Composable
fun CatalogScreen(vm: AppViewModel, push: (Route) -> Unit, onBack: (() -> Unit)? = null) {
    var category by rememberSaveable { mutableStateOf(ALL) }
    var search by rememberSaveable { mutableStateOf("") }
    var collapsed by rememberSaveable { mutableStateOf(setOf<String>()) }
    var menuOpen by remember { mutableStateOf(false) }

    val categories = vm.categories
    if (category != ALL && category !in categories && categories.isNotEmpty()) category = ALL
    val query = search.trim()
    val filtered = vm.courses.filter {
        (category == ALL || it.category == category) && (query.isEmpty() || it.searchIndex.contains(query, ignoreCase = true))
    }
    val grouped = filtered.groupBy { it.category }
    val sections = categories.mapNotNull { name -> grouped[name]?.takeIf { it.isNotEmpty() }?.let { name to it } }

    // The brand mark stays in the bar when the catalog is opened from Account → Browse Courses, as on iOS.
    ScreenScaffold(title = null, onBack = onBack) { padding ->
        Refreshable({ vm.refreshCatalog() }, Modifier.padding(padding)) {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    OutlinedTextField(
                        value = search, onValueChange = { search = it },
                        placeholder = { Text("Search courses") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (search.isNotEmpty()) IconButton(onClick = { search = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Clear search") }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Theme.field, unfocusedContainerColor = Theme.field,
                            unfocusedBorderColor = Color.Transparent, focusedBorderColor = Theme.accent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            TextButton(onClick = { menuOpen = true }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                                Text(if (category == ALL) "All categories" else category, fontSize = 15.sp)
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text("All categories") }, onClick = { category = ALL; menuOpen = false })
                                categories.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c) },
                                        onClick = { category = c; menuOpen = false },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text("${filtered.size} course${if (filtered.size == 1) "" else "s"}", fontSize = 12.sp, color = Theme.secondaryText)
                    }
                }

                when {
                    vm.courses.isEmpty() -> item { LoadingBox("Loading courses…") }
                    filtered.isEmpty() -> item {
                        EmptyState(Icons.Filled.SearchOff, "No courses found", "Try a different keyword or category.") {
                            OutlinedButton(onClick = { search = ""; category = ALL }) { Text("Clear Filters") }
                        }
                    }
                    else -> sections.forEach { (name, list) ->
                        val expanded = query.isNotEmpty() || name !in collapsed
                        item(key = "h-$name") {
                            CategoryHeader(name, list.size, expanded) {
                                collapsed = if (name in collapsed) collapsed - name else collapsed + name
                            }
                        }
                        if (expanded) items(list, key = { "c-${it.id}-${it.sku}" }) { course ->
                            CourseCard(course) { push(Route.CourseDetail(course)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(name: String, count: Int, expanded: Boolean, toggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = toggle)
            .semantics(mergeDescendants = true) { contentDescription = "$name, $count courses, ${if (expanded) "expanded" else "collapsed"}" }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(categoryIcon(name), contentDescription = null, tint = Theme.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Text(
            "$count", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(Theme.accentSoft).padding(horizontal = 7.dp, vertical = 3.dp),
        )
        Spacer(Modifier.weight(1f))
        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = Theme.secondaryText)
    }
}

@Composable
private fun CourseCard(course: Course, onClick: () -> Unit) {
    InfoCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.Top) {
            CourseThumb(course.imageUrl)
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(course.sku, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent)
                Text(course.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
            }
        }
        if (course.summary.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(course.summary, fontSize = 14.sp, color = Theme.secondaryText, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (course.duration.isNotEmpty()) IconLine(Icons.Filled.Schedule, course.duration, color = Theme.secondaryText, fontSize = 12, iconSize = 15.dp)
            Spacer(Modifier.weight(1f).width(10.dp))
            if (course.price > 0) IconLine(Icons.Filled.Sell, Fmt.money(course.price, course.currency), color = Theme.secondaryText, fontSize = 12, iconSize = 15.dp)
        }
    }
}

@Composable
fun CourseDetailScreen(course: Course, onBack: () -> Unit) {
    val context = LocalContext.current
    ScreenScaffold(title = course.title, onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (course.imageUrl.isNotEmpty()) {
                RemoteImage(
                    course.imageUrl, maxPx = 1200, contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
                )
            }
            InfoCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(course.category, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Theme.accent)
                    Text(course.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
                    Text(course.summary, fontSize = 15.sp, lineHeight = 21.sp)
                    HorizontalDivider(color = Theme.cardBorder)
                    LabeledValue("Course code", course.sku, fontSize = 15)
                    if (course.duration.isNotEmpty()) LabeledValue("Duration", course.duration, fontSize = 15)
                    if (course.price > 0) {
                        LabeledValue(
                            "Course fee",
                            Fmt.money(course.price, course.currency) + if (Site.priceNote.isEmpty()) "" else " (${Site.priceNote})",
                            fontSize = 15,
                        )
                    }
                }
            }
            if (course.url.isNotEmpty()) PrimaryButton("View Schedule & Register", Icons.Filled.Event) { openUrl(context, course.url) }
            TonalButton("Enquire via WhatsApp", Icons.AutoMirrored.Filled.Send) {
                openWhatsApp(context, Site.whatsApp, "${Site.siteName} course enquiry\n\nCourse: ${course.title}\nCourse code: ${course.sku}")
            }
        }
    }
}
