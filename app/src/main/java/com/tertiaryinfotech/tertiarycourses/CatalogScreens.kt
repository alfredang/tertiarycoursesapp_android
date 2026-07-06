package com.tertiaryinfotech.tertiarycourses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val WHATSAPP_NUMBER = "6588666375"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(onCourseClick: (Course) -> Unit) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }

    val categories = listOf("All") + CourseData.courses.map { it.category }.distinct().sorted()
    val query = searchText.trim()
    val filteredCourses = CourseData.courses.filter { course ->
        val categoryMatches = selectedCategory == "All" || course.category == selectedCategory
        val queryMatches = query.isEmpty() || course.searchIndex.contains(query, ignoreCase = true)
        categoryMatches && queryMatches
    }

    Scaffold(
        containerColor = Theme.page,
        topBar = {
            TopAppBar(
                title = { Text("Catalog", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Theme.page)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Search courses") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { searchText = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Theme.accent,
                    unfocusedBorderColor = Theme.cardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryPicker(
                    categories = categories,
                    selected = selectedCategory,
                    onSelected = { selectedCategory = it }
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${filteredCourses.size} course" + if (filteredCourses.size == 1) "" else "s",
                    style = MaterialTheme.typography.bodySmall,
                    color = Theme.secondaryText
                )
            }

            if (filteredCourses.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = Theme.secondaryText,
                        modifier = Modifier.size(44.dp)
                    )
                    Text("No courses found", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Try a different keyword or category.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Theme.secondaryText
                    )
                    OutlinedButton(onClick = {
                        searchText = ""
                        selectedCategory = "All"
                    }) {
                        Text("Clear Search", color = Theme.accent)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredCourses, key = { it.id }) { course ->
                        CourseCard(course = course, onClick = { onCourseClick(course) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPicker(categories: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .background(Theme.accentSoft, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                selected,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Theme.accent
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Filled.UnfoldMore,
                contentDescription = "Choose category",
                tint = Theme.accent,
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onSelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CourseCard(course: Course, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Theme.cardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                imageVector = categoryIcon(course.category),
                contentDescription = null,
                tint = Theme.accent,
                modifier = Modifier.size(30.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    course.courseCode,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Theme.accent
                )
                Text(
                    course.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    course.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Theme.secondaryText,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Schedule,
                contentDescription = null,
                tint = Theme.secondaryText,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(course.duration, style = MaterialTheme.typography.bodySmall, color = Theme.secondaryText)
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Filled.Sell,
                contentDescription = null,
                tint = Theme.secondaryText,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "${currency(course.feeWithGST)} w/GST",
                style = MaterialTheme.typography.bodySmall,
                color = Theme.secondaryText
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            course.fundingBadges.forEach { badge -> SchemeBadge(badge) }
        }
    }
}

/** Small funding-scheme chip (WSQ / SFC / SFEC / PSEA / UTAP) sized to fit five in a row. */
@Composable
fun SchemeBadge(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Theme.accent,
        modifier = Modifier
            .background(Theme.accentSoft, CircleShape)
            .padding(horizontal = 7.dp, vertical = 4.dp)
    )
}

private fun categoryIcon(category: String): ImageVector = when (category) {
    "Cybersecurity" -> Icons.Filled.Security
    "Automation" -> Icons.Filled.SettingsSuggest
    "Microsoft Power Platform" -> Icons.Filled.GridView
    "Programming", "AI and Programming" -> Icons.Filled.Code
    "Data Analytics" -> Icons.AutoMirrored.Filled.ShowChart
    "Data Visualisation", "Business Intelligence" -> Icons.Filled.BarChart
    "Databases" -> Icons.Filled.Storage
    else -> Icons.AutoMirrored.Filled.MenuBook
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(course: Course, onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var showGrantSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = Theme.page,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        course.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        course.category,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Theme.accent
                    )
                    Text(course.summary, style = MaterialTheme.typography.bodyLarge)
                    HorizontalDivider()
                    LabeledRow("Course code", course.courseCode)
                    LabeledRow("Duration", course.duration)
                    LabeledRow("Fee (before GST)", currency(course.fee))
                    LabeledRow("Fee with GST (9%)", currency(course.feeWithGST))
                }
            }

            Button(
                onClick = { uriHandler.openUri(course.registerUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = Theme.accent),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Check Schedule & Register", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = { showGrantSheet = true },
                colors = ButtonDefaults.buttonColors(containerColor = Theme.accent),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Filled.Calculate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Calculate My Grant & Net Fee", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = {
                    val body = "Tertiary Courses Singapore course enquiry\n\n" +
                        "Course: ${course.title}\n" +
                        "Course code: ${course.courseCode}"
                    uriHandler.openUri(whatsAppUrl(WHATSAPP_NUMBER, body))
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Theme.accent)
                Spacer(Modifier.width(8.dp))
                Text("Enquire via WhatsApp", color = Theme.accent, fontWeight = FontWeight.SemiBold)
            }

            InfoCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Funding")
                    course.fundingSchemes.forEach { scheme ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (scheme.eligible) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                contentDescription = if (scheme.eligible) "Eligible" else "Not eligible",
                                tint = if (scheme.eligible) Theme.accent else Theme.secondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                scheme.abbreviation,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(48.dp)
                            )
                            Text(
                                scheme.fullName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Theme.secondaryText
                            )
                        }
                    }
                    Text(
                        course.fundingTier.shortDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = Theme.secondaryText
                    )
                }
            }

            InfoCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Outcomes")
                    course.outcomes.forEach { outcome ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Theme.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(outcome, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    // Grant calculator sheet launched from a course — the course fee is autopopulated;
    // the user enters nationality, age, sponsorship, and SkillsFuture Credit.
    if (showGrantSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGrantSheet = false },
            sheetState = sheetState,
            containerColor = Theme.page
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                InfoCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            course.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        LabeledRow("Course fee (before GST)", currency(course.fee))
                        LabeledRow("Course fee with GST", currency(course.feeWithGST))
                    }
                }
                GrantCalculatorForm(course = course)
            }
        }
    }
}
