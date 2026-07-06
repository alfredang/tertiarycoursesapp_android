package com.tertiaryinfotech.tertiarycourses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.RoundingMode

enum class Nationality(val label: String, val shortLabel: String) {
    SINGAPORE_CITIZEN("Singapore Citizen", "Citizen"),
    PERMANENT_RESIDENT("Singapore PR", "PR"),
    OTHERS("Others", "Others")
}

enum class Sponsorship(val label: String) {
    SELF_SPONSORED("Self-Sponsored"),
    EMPLOYER_SME("Employer-Sponsored (SME)"),
    EMPLOYER_NON_SME("Employer-Sponsored (Non-SME)")
}

/**
 * SSG funding estimate per TP Gateway rules
 * (https://www.tpgateway.gov.sg/faq/grant-calculator-and-funding-eligibility):
 * - Baseline: 50% for Singapore Citizens below 40
 * - Enhanced (MCES): 70% for Singapore Citizens 40 and above
 * - Singapore PR: always 50%
 * - SME employer-sponsored (ETSS): 70% for Citizens and PRs regardless of age
 * - GST (9%) is always computed on the ORIGINAL course fee, before any grant.
 */
data class GrantEstimate(
    val courseFee: BigDecimal,          // original fee, before GST
    val skillsFutureClaimable: Boolean,
    val nationality: Nationality,
    val age: Int,
    val sponsorship: Sponsorship,
    val creditBalance: BigDecimal
) {
    companion object {
        val GST_RATE: BigDecimal = BigDecimal("0.09")
        fun roundedCurrency(amount: BigDecimal): BigDecimal = amount.setScale(2, RoundingMode.HALF_UP)
    }

    val fundingRate: BigDecimal
        get() = when (nationality) {
            Nationality.SINGAPORE_CITIZEN ->
                if (sponsorship == Sponsorship.EMPLOYER_SME || age >= 40) BigDecimal("0.70") else BigDecimal("0.50")
            Nationality.PERMANENT_RESIDENT ->
                if (sponsorship == Sponsorship.EMPLOYER_SME) BigDecimal("0.70") else BigDecimal("0.50")
            Nationality.OTHERS -> BigDecimal.ZERO
        }

    val grantName: String
        get() = when (nationality) {
            Nationality.SINGAPORE_CITIZEN ->
                if (sponsorship == Sponsorship.EMPLOYER_SME) "SME Enhanced Training Support (70%)"
                else if (age >= 40) "Enhanced Grant — MCES (70%)" else "Baseline Grant (50%)"
            Nationality.PERMANENT_RESIDENT ->
                if (sponsorship == Sponsorship.EMPLOYER_SME) "SME Enhanced Training Support (70%)"
                else "Baseline Grant (50%)"
            Nationality.OTHERS -> "Not eligible for SSG funding"
        }

    val gst: BigDecimal get() = roundedCurrency(courseFee * GST_RATE)

    val feeWithGST: BigDecimal get() = courseFee + gst

    val grant: BigDecimal get() = roundedCurrency(courseFee * fundingRate)

    val netBeforeCredit: BigDecimal get() = (feeWithGST - grant).max(BigDecimal.ZERO)

    /** SkillsFuture Credit applies only to self-sponsored Singapore Citizens on claimable courses. */
    val canUseSkillsFutureCredit: Boolean
        get() = nationality == Nationality.SINGAPORE_CITIZEN &&
            sponsorship == Sponsorship.SELF_SPONSORED &&
            skillsFutureClaimable

    val skillsFutureCreditApplied: BigDecimal
        get() = if (canUseSkillsFutureCredit) netBeforeCredit.min(creditBalance.max(BigDecimal.ZERO)) else BigDecimal.ZERO

    val netFee: BigDecimal get() = (netBeforeCredit - skillsFutureCreditApplied).max(BigDecimal.ZERO)

    val rateText: String
        get() = "${(fundingRate * BigDecimal(100)).setScale(0, RoundingMode.HALF_UP)}%"
}

// MARK: Grant Calculator tab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrantCalculatorScreen() {
    var selectedCourseId by rememberSaveable { mutableStateOf(CourseData.courses[0].id) }
    val selectedCourse = CourseData.courses.firstOrNull { it.id == selectedCourseId } ?: CourseData.courses[0]

    Scaffold(
        containerColor = Theme.page,
        topBar = {
            TopAppBar(
                title = { Text("Grant Calculator", fontWeight = FontWeight.Bold) },
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Course")
                    CoursePickerMenu(
                        selectedCourse = selectedCourse,
                        onCourseSelected = { selectedCourseId = it.id }
                    )
                    LabeledRow("Course fee (before GST)", currency(selectedCourse.fee))
                    LabeledRow("Course fee with GST", currency(selectedCourse.feeWithGST))
                }
            }

            GrantCalculatorForm(course = selectedCourse)
        }
    }
}

@Composable
fun CoursePickerMenu(selectedCourse: Course, onCourseSelected: (Course) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Theme.accentSoft, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedCourse.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Theme.accent,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.Filled.UnfoldMore,
                contentDescription = "Choose course",
                tint = Theme.accent,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CourseData.courses.forEach { course ->
                DropdownMenuItem(
                    text = { Text(course.title, style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        onCourseSelected(course)
                        expanded = false
                    }
                )
            }
        }
    }
}

// MARK: Shared calculator form (used by the tab and by each course's detail page)

@Composable
fun GrantCalculatorForm(course: Course) {
    var nationality by rememberSaveable { mutableStateOf(Nationality.SINGAPORE_CITIZEN) }
    var age by rememberSaveable { mutableIntStateOf(35) }
    var sponsorship by rememberSaveable { mutableStateOf(Sponsorship.SELF_SPONSORED) }
    var creditText by rememberSaveable { mutableStateOf("") }

    // Parse whatever the user typed ("350", "$1,200.50", …) into a BigDecimal; empty = 0.
    val creditBalance = creditText.filter { it.isDigit() || it == '.' }
        .toBigDecimalOrNull() ?: BigDecimal.ZERO

    val estimate = GrantEstimate(
        courseFee = course.fee,
        skillsFutureClaimable = course.skillsFutureClaimable,
        nationality = nationality,
        age = age,
        sponsorship = sponsorship,
        creditBalance = creditBalance
    )

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        InfoCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("Learner")

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    Nationality.entries.forEachIndexed { index, item ->
                        SegmentedButton(
                            selected = nationality == item,
                            onClick = { nationality = item },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = Nationality.entries.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = Theme.accentSoft,
                                activeContentColor = Theme.accent
                            )
                        ) {
                            Text(item.shortLabel, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Age", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { if (age > 16) age-- }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease age", tint = Theme.accent)
                    }
                    Text(
                        "$age",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { if (age < 90) age++ }) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase age", tint = Theme.accent)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Sponsorship", style = MaterialTheme.typography.bodyLarge)
                    SponsorshipMenu(sponsorship) { sponsorship = it }
                }
            }
        }

        InfoCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("SkillsFuture Credit")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Credit to claim", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    OutlinedTextField(
                        value = creditText,
                        onValueChange = { creditText = it },
                        prefix = { Text("S$") },
                        placeholder = { Text("0") },
                        enabled = estimate.canUseSkillsFutureCredit,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(150.dp)
                    )
                }
                Text(
                    "Enter the SkillsFuture Credit you want to claim — it is deducted from the net fee. " +
                        "Applies only to self-sponsored Singapore Citizens on claimable courses.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Theme.secondaryText
                )
            }
        }

        InfoCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Estimate")

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Verified,
                        contentDescription = null,
                        tint = Theme.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        estimate.grantName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Theme.accent
                    )
                }

                HorizontalDivider()

                EstimateRow("Course fee (before GST)", estimate.courseFee)
                EstimateRow("GST 9% (on original fee)", estimate.gst)
                EstimateRow("Course fee with GST", estimate.feeWithGST)
                EstimateRow("SSG grant (${estimate.rateText})", estimate.grant.negate())
                if (estimate.canUseSkillsFutureCredit) {
                    EstimateRow("SkillsFuture Credit", estimate.skillsFutureCreditApplied.negate())
                }

                HorizontalDivider()

                EstimateRow("Net fee payable", estimate.netFee, isTotal = true)
            }
        }

        InfoCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "This is an estimate based on SSG funding rules for self-sponsored and " +
                        "employer-sponsored (SME) learners. Final payable amounts depend on official " +
                        "course approval, learner eligibility, and available SkillsFuture Credit balance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Theme.secondaryText
                )
                LinkText(
                    "TP Gateway — Grant Calculator and Funding Eligibility",
                    "https://www.tpgateway.gov.sg/faq/grant-calculator-and-funding-eligibility"
                )
            }
        }
    }
}

@Composable
fun SponsorshipMenu(selected: Sponsorship, onSelected: (Sponsorship) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Theme.accentSoft, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selected.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Theme.accent,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Filled.UnfoldMore,
                contentDescription = "Choose sponsorship",
                tint = Theme.accent,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Sponsorship.entries.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun EstimateRow(title: String, value: BigDecimal, isTotal: Boolean = false) {
    Row {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.SemiBold else FontWeight.Normal
        )
        Spacer(Modifier.weight(1f))
        Text(
            currency(value),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = when {
                value < BigDecimal.ZERO -> Theme.positiveGreen
                isTotal -> Theme.accent
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
