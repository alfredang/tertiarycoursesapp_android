package com.tertiaryinfotech.tertiarycourses

import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

enum class FundingTier(val label: String, val shortDescription: String) {
    TIER1(
        "Tier 1 SSG",
        "Emerging skills, employability, SCTP, SkillsFuture Series, WPLN, or stackable IHL modules."
    ),
    TIER2(
        "Tier 2 WSQ",
        "WSQ or other approved skills courses supporting general upskilling and reskilling."
    ),
    UNFUNDED(
        "No SSG funding",
        "Not listed as SSG-funded. SkillsFuture Credit may still depend on official approval."
    )
}

data class FundingScheme(val abbreviation: String, val fullName: String, val eligible: Boolean)

/**
 * [fee] is the ORIGINAL course fee before GST. GST is always computed on this
 * original fee, per SSG rules — see
 * https://www.tpgateway.gov.sg/faq/grant-calculator-and-funding-eligibility
 */
data class Course(
    val id: String,
    val title: String,
    val category: String,
    val duration: String,
    val delivery: String,
    val fee: BigDecimal,
    val fundingTier: FundingTier,
    val skillsFutureClaimable: Boolean,
    val courseCode: String,
    val summary: String,
    val outcomes: List<String>,
    val websiteUrl: String? = null,
    val sfecEligible: Boolean = true,
    val pseaEligible: Boolean = true,
    val utapEligible: Boolean = true
) {
    val feeWithGST: BigDecimal
        get() = (fee * (BigDecimal.ONE + GrantEstimate.GST_RATE)).setScale(2, RoundingMode.HALF_UP)

    val isWSQCourse: Boolean
        get() = courseCode.uppercase().startsWith("TGS")

    /** Short funding-scheme badges shown on course cards. */
    val fundingBadges: List<String>
        get() = buildList {
            if (isWSQCourse) add("WSQ")
            if (skillsFutureClaimable) add("SFC")
            if (sfecEligible) add("SFEC")
            if (pseaEligible) add("PSEA")
            if (utapEligible) add("UTAP")
        }

    /** Funding schemes with full names and eligibility, for the course detail page. */
    val fundingSchemes: List<FundingScheme>
        get() = listOf(
            FundingScheme("WSQ", "Workforce Skills Qualifications (SSG-funded)", isWSQCourse),
            FundingScheme("SFC", "SkillsFuture Credit", skillsFutureClaimable),
            FundingScheme("SFEC", "SkillsFuture Enterprise Credit", sfecEligible),
            FundingScheme("PSEA", "Post-Secondary Education Account", pseaEligible),
            FundingScheme("UTAP", "NTUC Union Training Assistance Programme", utapEligible)
        )

    /**
     * Course page on www.tertiarycourses.com.sg for registration. Falls back to a
     * catalog search by title when no explicit page URL is known.
     */
    val registerUrl: String
        get() = websiteUrl ?: (
            "https://www.tertiarycourses.com.sg/catalogsearch/result/?q=" +
                URLEncoder.encode(title.replace("WSQ - ", ""), "UTF-8")
            )

    val searchIndex: String
        get() = (listOf(courseCode, title, category, duration, delivery, fundingTier.label, summary) + outcomes)
            .joinToString(" ")
}

object CourseData {
    val courses: List<Course> = listOf(
        Course(
            id = "TGS-2022015539",
            title = "WSQ - Applications Integration with Power Apps and Power Automate",
            category = "Microsoft Power Platform",
            duration = "2 days",
            delivery = "Classroom",
            fee = BigDecimal(800),
            fundingTier = FundingTier.TIER2,
            skillsFutureClaimable = true,
            courseCode = "TGS-2022015539",
            summary = "Build app and workflow integrations using Microsoft Power Apps and Power Automate.",
            outcomes = listOf(
                "Create low-code business apps",
                "Automate approval workflows",
                "Connect app data sources"
            ),
            websiteUrl = "https://www.tertiarycourses.com.sg/wsq-applications-integration-with-power-apps-and-power-automate.html"
        ),
        Course(
            id = "TGS-2019503161",
            title = "WSQ - Python Fundamental Course for Beginners",
            category = "Programming",
            duration = "2 days",
            delivery = "Classroom",
            fee = BigDecimal(750),
            fundingTier = FundingTier.TIER2,
            skillsFutureClaimable = true,
            courseCode = "TGS-2019503161",
            summary = "Learn beginner-friendly Python syntax, data structures, and practical scripting.",
            outcomes = listOf(
                "Write Python scripts",
                "Use functions and collections",
                "Solve beginner programming tasks"
            ),
            websiteUrl = "https://www.tertiarycourses.com.sg/wsq-python-fundamental-course-for-beginners.html"
        ),
        Course(
            id = "TGS-2019504058",
            title = "WSQ - R Fundamental and Statistical Analysis for Beginners",
            category = "Data Analytics",
            duration = "2 days",
            delivery = "Classroom",
            fee = BigDecimal(750),
            fundingTier = FundingTier.TIER2,
            skillsFutureClaimable = true,
            courseCode = "TGS-2019504058",
            summary = "Use R for introductory statistics, analysis workflows, and data exploration.",
            outcomes = listOf(
                "Run R scripts",
                "Summarise datasets",
                "Apply basic statistical analysis"
            ),
            websiteUrl = "https://www.tertiarycourses.com.sg/wsq-r-fundamental-and-statistical-analysis-for-beginners.html"
        ),
        Course(
            id = "TGS-2019504591",
            title = "WSQ - Build and Deploy Python Applications with Vibe Coding",
            category = "AI and Programming",
            duration = "2 days",
            delivery = "Classroom",
            fee = BigDecimal(750),
            fundingTier = FundingTier.TIER2,
            skillsFutureClaimable = true,
            courseCode = "TGS-2019504591",
            summary = "Create Python applications faster with AI-assisted coding workflows.",
            outcomes = listOf(
                "Plan app features",
                "Use AI coding tools",
                "Deploy a Python application"
            ),
            websiteUrl = "https://www.tertiarycourses.com.sg/wsq-build-and-deploy-python-applications-with-vibe-coding.html"
        ),
        Course(
            id = "TGS-2020503177",
            title = "WSQ - Data Visualisation with Tableau",
            category = "Data Visualisation",
            duration = "2 days",
            delivery = "Classroom",
            fee = BigDecimal(750),
            fundingTier = FundingTier.TIER2,
            skillsFutureClaimable = true,
            courseCode = "TGS-2020503177",
            summary = "Build Tableau dashboards and visual analytics for business reporting.",
            outcomes = listOf(
                "Connect data in Tableau",
                "Design visual dashboards",
                "Publish interactive views"
            ),
            websiteUrl = "https://www.tertiarycourses.com.sg/wsq-data-visualisation-with-tableau.html"
        ),
        Course(
            id = "TGS-2020503626",
            title = "WSQ - Cyber Security Awareness Course for Personal and Businesses",
            category = "Cybersecurity",
            duration = "1 day",
            delivery = "Classroom",
            fee = BigDecimal(350),
            fundingTier = FundingTier.TIER2,
            skillsFutureClaimable = true,
            courseCode = "TGS-2020503626",
            summary = "Understand cyber threats, common attack patterns, and practical protection steps.",
            outcomes = listOf(
                "Identify phishing risks",
                "Harden accounts and devices",
                "Respond to common incidents"
            ),
            websiteUrl = "https://www.tertiarycourses.com.sg/wsq-cyber-security-awareness-course-for-personal-and-businesses.html",
            pseaEligible = false
        ),
        Course(
            id = "TGS-2020504808",
            title = "WSQ - Robotics Process Automation (RPA) for Beginners",
            category = "Automation",
            duration = "2 days",
            delivery = "Classroom",
            fee = BigDecimal(750),
            fundingTier = FundingTier.TIER2,
            skillsFutureClaimable = true,
            courseCode = "TGS-2020504808",
            summary = "Use RPA concepts and tools to automate repeatable office processes.",
            outcomes = listOf(
                "Map automation workflows",
                "Build simple bots",
                "Test automated processes"
            ),
            websiteUrl = "https://www.tertiarycourses.com.sg/wsq-robotics-process-automation-rpa-for-beginners.html"
        ),
        Course(
            id = "TGS-2020505444",
            title = "WSQ - Data Analytics and Visualization with Power BI",
            category = "Business Intelligence",
            duration = "2 days",
            delivery = "Classroom",
            fee = BigDecimal(800),
            fundingTier = FundingTier.TIER2,
            skillsFutureClaimable = true,
            courseCode = "TGS-2020505444",
            summary = "Create decision-ready Power BI reports with data shaping, models, and visual dashboards.",
            outcomes = listOf(
                "Transform data with Power Query",
                "Create report visuals",
                "Publish Power BI dashboards"
            ),
            websiteUrl = "https://www.tertiarycourses.com.sg/wsq-data-analytics-and-visualization-with-power-bi.html"
        ),
        Course(
            id = "TGS-2020505790",
            title = "WSQ - SQL Fundamental for Beginners",
            category = "Databases",
            duration = "1 day",
            delivery = "Classroom",
            fee = BigDecimal(350),
            fundingTier = FundingTier.TIER2,
            skillsFutureClaimable = true,
            courseCode = "TGS-2020505790",
            summary = "Learn SQL queries for retrieving, filtering, joining, and summarising relational data.",
            outcomes = listOf(
                "Write SELECT queries",
                "Filter and join tables",
                "Aggregate business data"
            ),
            websiteUrl = "https://www.tertiarycourses.com.sg/wsq-sql-fundamental-for-beginners.html",
            pseaEligible = false
        )
    )

    fun courseById(id: String): Course = courses.first { it.id == id }
}

fun currency(amount: BigDecimal): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    val value = amount.setScale(2, RoundingMode.HALF_UP)
    return if (value < BigDecimal.ZERO) "-S$${formatter.format(value.negate())}" else "S$${formatter.format(value)}"
}
