package com.tertiaryinfotech.tertiarycourses

import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Shapes of the site's learner API (MMD_MobileApi). JSON keys are snake_case.

// ── JSON helpers (org.json turns JSON null into the string "null" for optString) ──

fun JSONObject.str(key: String): String =
    if (!has(key) || isNull(key)) "" else optString(key, "")

fun JSONObject.num(key: String): Double =
    if (!has(key) || isNull(key)) 0.0 else optDouble(key, 0.0).let { if (it.isNaN()) 0.0 else it }

fun JSONObject.int(key: String): Int = num(key).toInt()

/** A number, or null when the key is missing or JSON null (averages with no answers yet). */
fun JSONObject.numOrNull(key: String): Double? =
    if (!has(key) || isNull(key)) null else optDouble(key, Double.NaN).takeIf { !it.isNaN() }

fun JSONObject.bool(key: String): Boolean = optBool(key) ?: false

/** true / false / null — the API also sends 1/0 for some flags. */
fun JSONObject.optBool(key: String): Boolean? {
    if (!has(key) || isNull(key)) return null
    return when (val v = opt(key)) {
        is Boolean -> v
        is Number -> v.toInt() != 0
        is String -> v == "1" || v.equals("true", true)
        else -> null
    }
}

fun JSONObject.obj(key: String): JSONObject? = if (!has(key) || isNull(key)) null else optJSONObject(key)

fun <T> JSONObject.list(key: String, map: (JSONObject) -> T): List<T> {
    val arr = if (!has(key) || isNull(key)) null else optJSONArray(key)
    return arr?.objects()?.map(map) ?: emptyList()
}

fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }

fun JSONObject.strings(key: String): List<String> {
    val arr = (if (!has(key) || isNull(key)) null else optJSONArray(key)) ?: return emptyList()
    return (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotEmpty() }
}

// ── Learner ──

/**
 * The signed-in account. `phone`, `theme` and `roles` arrive from 1.4 of the API; an older
 * backend omits them (learner only, appearance follows the device). `roles` may also hold
 * the staff roles admin, marketing, developer and franchise_admin.
 */
data class Learner(
    val email: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val phone: String = "",
    /** "system" | "light" | "dark" */
    val theme: String = "system",
    val roles: List<String> = listOf("learner"),
    /** Unix time the profile picture was last set; null when there is none. */
    val avatarVersion: Long? = null,
    /** Trainers and staff only; empty when not set. */
    val linkedinUrl: String = "",
) {
    /** Trainer on the website, whatever view the app is showing (see AppViewModel.activeRole). */
    val isTrainer: Boolean get() = "trainer" in roles

    fun toJson(): String = JSONObject()
        .put("email", email).put("first_name", firstName).put("last_name", lastName).put("full_name", fullName)
        .put("phone", phone).put("theme", theme).put("roles", JSONArray(roles))
        .put("linkedin_url", linkedinUrl)
        .apply { if (avatarVersion != null) put("avatar_version", avatarVersion) }
        .toString()

    companion object {
        fun from(j: JSONObject) = Learner(
            j.str("email"), j.str("first_name"), j.str("last_name"), j.str("full_name"), j.str("phone"),
            j.str("theme").lowercase().takeIf { it == "light" || it == "dark" } ?: "system",
            j.strings("roles").map { it.lowercase() }.ifEmpty { listOf("learner") },
            if (!j.has("avatar_version") || j.isNull("avatar_version")) null else j.optLong("avatar_version").takeIf { it > 0 },
            j.str("linkedin_url"),
        )
    }
}

// ── Catalog ──

/** A public short course (C-prefix code) from `/mobileapi/catalog`. */
data class Course(
    val id: Int,
    val sku: String,
    val title: String,
    val summary: String,
    val category: String,
    val duration: String,
    val price: Double,
    val currency: String,
    val url: String,
    val imageUrl: String,
) {
    val searchIndex = "$sku $title $category $summary"

    companion object {
        fun from(j: JSONObject) = Course(
            j.int("id"), j.str("sku"), j.str("title"), j.str("summary"), j.str("category").ifEmpty { "Other Courses" },
            j.str("duration"), j.num("price"), j.str("currency"), j.str("url"), j.str("image_url"),
        )
    }
}

// ── Classes ──

data class CertificateInfo(
    val runId: Int,
    val classId: String,
    val sku: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val certNo: String,
    val issuedOn: String,
    val name: String,
    val url: String,
) {
    val id: String get() = certNo.ifEmpty { "$runId" }

    companion object {
        fun from(j: JSONObject) = CertificateInfo(
            j.int("run_id"), j.str("class_id"), j.str("sku"), j.str("title"), j.str("start_date"), j.str("end_date"),
            j.str("cert_no"), j.str("issued_on"), j.str("name"), j.str("url"),
        )
    }
}

/** A class in My Classes. `runId` is 0 for a registration still awaiting payment. */
data class ClassItem(
    val runId: Int,
    val classId: String,
    val sku: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val startTime: String,
    val endTime: String,
    val mode: String,
    val venue: String,
    val room: String,
    val virtualLink: String,
    val trainer: String,
    val status: String,
    val orderNo: String,
    val bucket: String,
    val days: List<String>,
    val imageUrl: String,
    val certificate: CertificateInfo?,
    val surveySubmitted: Boolean,
    /** "learner" (attending) or "trainer" (teaching). */
    val role: String = "learner",
    /** Learners on the roster of a class this account teaches; null when the site does not say. */
    val learnerCount: Int? = null,
) {
    val isTeaching: Boolean get() = role == "trainer"
    val id: String get() = if (runId > 0) "$role-run-$runId" else "order-$orderNo-$sku-$startDate"
    val isPendingPayment: Boolean get() = status == "pending_payment"

    /** Actual class dates; falls back to the start…end span when the site doesn't list them. */
    val classDays: List<String>
        get() {
            if (days.isNotEmpty()) return days
            val s = Fmt.date(startDate) ?: return emptyList()
            val e = Fmt.date(endDate) ?: s
            val out = mutableListOf<String>()
            var d = s
            while (!d.isAfter(e) && out.size < 14) {
                out += d.toString(); d = d.plusDays(1)
            }
            return out
        }

    companion object {
        fun from(j: JSONObject) = ClassItem(
            j.int("run_id"), j.str("class_id"), j.str("sku"), j.str("title"), j.str("start_date"), j.str("end_date"),
            j.str("start_time"), j.str("end_time"), j.str("mode"), j.str("venue"), j.str("room"), j.str("virtual_link"),
            j.str("trainer"), j.str("status"), j.str("order_no"), j.str("bucket"), j.strings("days"), j.str("image_url"),
            j.obj("certificate")?.let(CertificateInfo::from), j.bool("survey_submitted"),
            if (j.str("role") == "trainer") "trainer" else "learner",
            if (j.has("learner_count") && !j.isNull("learner_count")) j.int("learner_count") else null,
        )
    }
}

data class ClassesReply(
    val today: String,
    val current: List<ClassItem>,
    val upcoming: List<ClassItem>,
    val past: List<ClassItem>,
) {
    val all: List<ClassItem> get() = current + upcoming + past

    companion object {
        fun from(j: JSONObject) = ClassesReply(
            j.str("today"), j.list("current", ClassItem::from), j.list("upcoming", ClassItem::from), j.list("past", ClassItem::from),
        )

        /** The classes the account attends and the classes it teaches, as one list per bucket. */
        fun merge(learner: ClassesReply?, trainer: ClassesReply?): ClassesReply? {
            if (learner == null && trainer == null) return null
            val l = learner ?: ClassesReply("", emptyList(), emptyList(), emptyList())
            val t = trainer ?: ClassesReply("", emptyList(), emptyList(), emptyList())
            return ClassesReply(
                l.today.ifEmpty { t.today },
                (l.current + t.current).sortedBy { it.startDate },
                (l.upcoming + t.upcoming).sortedBy { it.startDate },
                (l.past + t.past).sortedByDescending { it.endDate },
            )
        }
    }
}

// ── Trainer ──

data class RosterEntry(val name: String, val email: String, val phone: String, val walkin: Boolean, val present: Set<String>) {
    companion object {
        fun from(j: JSONObject) = RosterEntry(j.str("name"), j.str("email"), j.str("phone"), j.bool("walkin"), j.strings("present").toSet())
    }
}

/** One class the account teaches, from `/mobileapi/trainer/class`. */
data class TrainerClassDetail(
    val runId: Int,
    val classId: String,
    val sku: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val startTime: String,
    val endTime: String,
    val mode: String,
    val venue: String,
    val room: String,
    val virtualLink: String,
    val trainer: String,
    val bucket: String,
    val sessions: List<ClassSession>,
    val currentSession: String,
    val currentSessionLabel: String,
    val attendanceOpen: Boolean,
    val checkinUrl: String,
    val roster: List<RosterEntry>,
    val materials: List<Material>,
    val documents: List<Material>,
    val surveyResponses: Int,
    /** The course survey link (kept for older data; the class page no longer shows it to trainers). */
    val feedbackUrl: String,
    // 1.6: the website's class page sections (absent from an older site).
    val announcements: List<Announcement> = emptyList(),
    val lesson: List<LessonTopic> = emptyList(),
    val practiceExam: PracticeExam? = null,
    val feedbackSummary: FeedbackSummary? = null,
) {
    companion object {
        fun from(j: JSONObject) = TrainerClassDetail(
            j.int("run_id"), j.str("class_id"), j.str("sku"), j.str("title"), j.str("start_date"), j.str("end_date"),
            j.str("start_time"), j.str("end_time"), j.str("mode"), j.str("venue"), j.str("room"), j.str("virtual_link"),
            j.str("trainer"), j.str("bucket"), j.list("sessions", ClassSession::from), j.str("current_session"),
            j.str("current_session_label"), j.bool("attendance_open"), j.str("checkin_url"), j.list("roster", RosterEntry::from),
            j.list("materials", Material::from), j.list("documents", Material::from), j.int("survey_responses"),
            j.str("feedback_url"),
            j.list("announcements", Announcement::from), j.list("lesson", LessonTopic::from),
            j.obj("practice_exam")?.let(PracticeExam::from), j.obj("feedback_summary")?.let(FeedbackSummary::from),
        )
    }
}

// ── Class page sections (1.6) ──

/** A post on the class page, oldest first. [fileUrl] is an attachment posted on the website. */
data class Announcement(val id: Int, val message: String, val fileName: String, val fileUrl: String, val postedBy: String, val postedAt: String) {
    companion object {
        fun from(j: JSONObject) = Announcement(
            j.int("id"), j.str("message"), j.str("file_name"), j.str("file_url"), j.str("posted_by"), j.str("posted_at"),
        )
    }
}

/**
 * One link of a Lesson sub topic. [type] is web, document, activity, video or quiz. A quiz
 * carries [quizId] and its question count; for a trainer [url] is the QR link learners scan,
 * for a learner it is empty and [previous] is the latest score.
 */
data class LessonLink(val type: String, val title: String, val url: String, val quizId: Int, val questions: Int, val previous: QuizScore?) {
    val isQuiz: Boolean get() = type == "quiz" && quizId > 0

    companion object {
        fun from(j: JSONObject) = LessonLink(
            j.str("type"), j.str("title"), j.str("url"), j.int("quiz_id"), j.int("questions"), j.obj("previous")?.let(QuizScore::from),
        )
    }
}

data class LessonSubtopic(val title: String, val links: List<LessonLink>) {
    companion object {
        fun from(j: JSONObject) = LessonSubtopic(j.str("title"), j.list("links", LessonLink::from))
    }
}

data class LessonTopic(val title: String, val subtopics: List<LessonSubtopic>) {
    companion object {
        fun from(j: JSONObject) = LessonTopic(j.str("title"), j.list("subtopics", LessonSubtopic::from))
    }
}

/**
 * The course's practice exam and the class's self-register form (pre-filled for a learner).
 * The apps never show the form as a QR code: the QR is on the web class page only.
 * [registered] is whether the signed-in person is registered for this class's exam (a trainer is
 * registered automatically); null when the exams site could not be reached or the site predates it.
 */
data class PracticeExam(val examCode: String, val examUrl: String, val selfRegisterUrl: String, val open: Boolean, val registered: Boolean? = null) {
    companion object {
        fun from(j: JSONObject) = PracticeExam(
            j.str("exam_code"), j.str("exam_url"), j.str("self_register_url"), j.optBool("open") ?: true, j.optBool("registered"),
        )
    }
}

data class FeedbackQuestion(val label: String, val average: Double?) {
    companion object {
        fun from(j: JSONObject) = FeedbackQuestion(j.str("label"), j.numOrNull("average"))
    }
}

/** The class's course feedback: [overall] and each average are out of 5 (null with no answers). Comments newest first. */
data class FeedbackSummary(val count: Int, val overall: Double?, val questions: List<FeedbackQuestion>, val comments: List<String>) {
    companion object {
        fun from(j: JSONObject) = FeedbackSummary(
            j.int("count"), j.numOrNull("overall"), j.list("questions", FeedbackQuestion::from), j.strings("comments"),
        )
    }
}

// ── Support tab ──

/** The academy's contacts from `GET support`; an empty field falls back to the [Site] constant. */
data class SupportInfo(val email: String, val phone: String, val whatsApp: String, val address: String) {
    companion object {
        val fallback = SupportInfo(Site.email, Site.phone, Site.whatsAppLabel, Site.address)

        fun from(j: JSONObject?) = SupportInfo(
            j?.str("email")?.ifEmpty { null } ?: Site.email,
            j?.str("phone")?.ifEmpty { null } ?: Site.phone,
            j?.str("whatsapp")?.ifEmpty { null } ?: Site.whatsAppLabel,
            j?.str("address")?.ifEmpty { null } ?: Site.address,
        )
    }
}

data class ClassSession(val key: String, val day: Int, val part: String, val date: String, val label: String, val present: Boolean?) {
    companion object {
        fun from(j: JSONObject) = ClassSession(j.str("key"), j.int("day"), j.str("part"), j.str("date"), j.str("label"), j.optBool("present"))
    }
}

data class AttendanceState(
    val open: Boolean,
    val currentSession: String,
    val currentSessionLabel: String,
    val markedPresent: Boolean,
    val sessionsPresent: Int,
    val sessionsTotal: Int,
) {
    companion object {
        fun from(j: JSONObject?) = AttendanceState(
            j?.bool("open") ?: false, j?.str("current_session") ?: "", j?.str("current_session_label") ?: "",
            j?.bool("marked_present") ?: false, j?.int("sessions_present") ?: 0, j?.int("sessions_total") ?: 0,
        )
    }
}

data class Material(val key: String, val title: String, val url: String) {
    companion object {
        fun from(j: JSONObject) = Material(j.str("key"), j.str("title"), j.str("url"))
    }
}

data class SurveyState(val available: Boolean, val submitted: Boolean, val externalUrl: String, val message: String) {
    companion object {
        fun from(j: JSONObject?) = SurveyState(
            j?.bool("available") ?: false, j?.bool("submitted") ?: false, j?.str("external_url") ?: "", j?.str("message") ?: "",
        )
    }
}

data class ClassDetail(
    val runId: Int,
    val classId: String,
    val sku: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val startTime: String,
    val endTime: String,
    val mode: String,
    val venue: String,
    val room: String,
    val virtualLink: String,
    val trainer: String,
    val bucket: String,
    val sessions: List<ClassSession>,
    val attendance: AttendanceState,
    val materials: List<Material>,
    val documents: List<Material>,
    val survey: SurveyState,
    val certificate: CertificateInfo?,
    val certificateRule: String,
    /** Lesson quizzes of the class's course (empty on a site that predates them). */
    val quizzes: List<QuizSummary>,
    // 1.6: the website's class page sections (absent from an older site).
    val announcements: List<Announcement> = emptyList(),
    val lesson: List<LessonTopic> = emptyList(),
    /** The course's practice exam, opened with the self-register form (absent before 1.6.1). */
    val practiceExam: PracticeExam? = null,
) {
    companion object {
        fun from(j: JSONObject) = ClassDetail(
            j.int("run_id"), j.str("class_id"), j.str("sku"), j.str("title"), j.str("start_date"), j.str("end_date"),
            j.str("start_time"), j.str("end_time"), j.str("mode"), j.str("venue"), j.str("room"), j.str("virtual_link"),
            j.str("trainer"), j.str("bucket"), j.list("sessions", ClassSession::from), AttendanceState.from(j.obj("attendance")),
            j.list("materials", Material::from), j.list("documents", Material::from), SurveyState.from(j.obj("survey")),
            j.obj("certificate")?.let(CertificateInfo::from), j.str("certificate_rule"),
            j.list("quizzes", QuizSummary::from),
            j.list("announcements", Announcement::from), j.list("lesson", LessonTopic::from),
            j.obj("practice_exam")?.let(PracticeExam::from),
        )
    }
}

// ── Lesson quizzes ──

data class QuizScore(val score: Int, val total: Int) {
    companion object {
        fun from(j: JSONObject) = QuizScore(j.int("score"), j.int("total"))
    }
}

/** A quiz listed on the class page, with the learner's latest score. */
data class QuizSummary(val quizId: Int, val title: String, val questions: Int, val previous: QuizScore?) {
    companion object {
        fun from(j: JSONObject) = QuizSummary(j.int("quiz_id"), j.str("title"), j.int("questions"), j.obj("previous")?.let(QuizScore::from))
    }
}

data class QuizQuestion(val id: String, val question: String, val options: List<String>) {
    companion object {
        fun from(j: JSONObject) = QuizQuestion(j.str("id"), j.str("question"), j.strings("options"))
    }
}

/** The questions of one quiz. The correct answers stay on the server until it is submitted. */
data class Quiz(val quizId: Int, val title: String, val questions: List<QuizQuestion>) {
    companion object {
        fun from(j: JSONObject) = Quiz(j.int("quiz_id"), j.str("title"), j.list("questions", QuizQuestion::from))
    }
}

/** One marked answer: [selectedIndex] is -1 when the question was left blank. */
data class QuizAnswerResult(val id: String, val correct: Boolean, val correctIndex: Int, val selectedIndex: Int) {
    companion object {
        fun from(j: JSONObject) = QuizAnswerResult(
            j.str("id"), j.bool("correct"), j.int("correct_index"),
            if (j.isNull("selected_index")) -1 else j.int("selected_index"),
        )
    }
}

data class QuizResult(val score: Int, val total: Int, val breakdown: List<QuizAnswerResult>) {
    companion object {
        fun from(j: JSONObject) = QuizResult(j.int("score"), j.int("total"), j.list("breakdown", QuizAnswerResult::from))
    }
}

// ── Survey ──

data class SurveyField(
    val id: String,
    val label: String,
    val type: String,
    val required: Boolean,
    val readonly: Boolean,
    val options: List<String>,
    val value: String,
) {
    companion object {
        fun from(j: JSONObject) = SurveyField(
            j.str("id"), j.str("label"), j.str("type"), j.bool("required"), j.bool("readonly"), j.strings("options"), j.str("value"),
        )
    }
}

data class SurveySection(val id: String, val title: String, val fields: List<SurveyField>) {
    companion object {
        fun from(j: JSONObject) = SurveySection(j.str("id"), j.str("title"), j.list("fields", SurveyField::from))
    }
}

data class Survey(val title: String, val state: SurveyState, val sections: List<SurveySection>) {
    companion object {
        fun from(j: JSONObject) = Survey(j.str("title"), SurveyState.from(j.obj("state")), j.list("sections", SurveySection::from))
    }
}

// ── Billing ──

data class OrderLine(val title: String, val sku: String, val qty: Double, val amount: Double, val classDate: String, val classTime: String) {
    companion object {
        fun from(j: JSONObject) = OrderLine(j.str("title"), j.str("sku"), j.num("qty"), j.num("amount"), j.str("class_date"), j.str("class_time"))
    }
}

data class Order(
    val orderNo: String,
    val date: String,
    val status: String,
    val statusLabel: String,
    val total: Double,
    val paid: Double,
    val refunded: Double,
    val currency: String,
    val paymentMethod: String,
    val invoices: List<String>,
    val items: List<OrderLine>,
) {
    companion object {
        fun from(j: JSONObject) = Order(
            j.str("order_no"), j.str("date"), j.str("status"), j.str("status_label"), j.num("total"), j.num("paid"),
            j.num("refunded"), j.str("currency"), j.str("payment_method"), j.strings("invoices"), j.list("items", OrderLine::from),
        )
    }
}

// ── Trainer profile, payout and training hours ──

/** A trainer's certification as edited on the Account page. */
data class CertDraft(
    val name: String = "", val issuer: String = "", val year: String = "",
    /** Identifies the row on screen while it is edited; never sent to the site. */
    val id: String = java.util.UUID.randomUUID().toString(),
) {
    companion object {
        fun from(j: JSONObject) = CertDraft(j.str("name"), j.str("issuer"), j.int("year").takeIf { it > 0 }?.toString().orEmpty())
    }
}

/** Trainer qualifications shown as badges under the name. */
data class TrainerBadge(val code: String, val name: String, val issuer: String) {
    fun matches(certName: String): Boolean {
        val n = certName.lowercase()
        return code.lowercase() in n || name.lowercase() in n
    }

    companion object {
        val all = listOf(
            TrainerBadge("ACLP", "WSQ Advanced Certificate in Learning and Performance (ACLP)", "Institute for Adult Learning"),
            TrainerBadge("ACTA", "WSQ Advanced Certificate in Training and Assessment (ACTA)", "Institute for Adult Learning"),
            TrainerBadge("DDDLP", "WSQ Diploma in Design and Development of Learning for Performance (DDDLP)", "Institute for Adult Learning"),
            TrainerBadge("DACE", "WSQ Diploma in Adult and Continuing Education (DACE)", "Institute for Adult Learning"),
        )
    }
}

data class Payout(
    val id: String,
    val title: String,
    val courseCode: String,
    val learners: Int,
    val amount: Double,
    val paymentDate: String,
    val startDate: String,
    val endDate: String,
) {
    companion object {
        fun from(j: JSONObject) = Payout(
            j.str("id"), j.str("title"), j.str("course_code"), j.int("learners"), j.num("amount"),
            j.str("payment_date"), j.str("start_date"), j.str("end_date"),
        )
    }
}

/** [available] is false when this site has no payout records to show (or they cannot be reached). */
data class PayoutsReply(val available: Boolean, val payouts: List<Payout>, val total: Double, val currency: String) {
    companion object {
        fun from(j: JSONObject) = PayoutsReply(j.bool("available"), j.list("payouts", Payout::from), j.num("total"), j.str("currency"))
    }
}

/** [month] is "2026-11". */
data class HoursMonth(val month: String, val hours: Double) {
    companion object {
        fun from(j: JSONObject) = HoursMonth(j.str("month"), j.num("hours"))
    }
}

data class HoursClass(
    val runId: Int,
    val title: String,
    val sku: String,
    val startDate: String,
    val endDate: String,
    val days: Int,
    val hours: Double,
) {
    companion object {
        fun from(j: JSONObject) = HoursClass(
            j.int("run_id"), j.str("title"), j.str("sku"), j.str("start_date"), j.str("end_date"), j.int("days"), j.num("hours"),
        )
    }
}

data class TrainingHoursReply(val totalHours: Double, val months: List<HoursMonth>, val classes: List<HoursClass>) {
    companion object {
        fun from(j: JSONObject) = TrainingHoursReply(j.num("total_hours"), j.list("months", HoursMonth::from), j.list("classes", HoursClass::from))
    }
}

// ── Staff dashboard (cards and lists decided by the server) ──

/** [icon] is an SF Symbol name (the iOS app's icon set); [tone] is "normal" or "warning". */
data class StaffCard(val title: String, val value: String, val icon: String, val tone: String) {
    companion object {
        fun from(j: JSONObject) = StaffCard(j.str("title"), j.str("value"), j.str("icon"), j.str("tone"))
    }
}

data class StaffRow(val title: String, val subtitle: String, val value: String) {
    companion object {
        fun from(j: JSONObject) = StaffRow(j.str("title"), j.str("subtitle"), j.str("value"))
    }
}

data class StaffList(val title: String, val rows: List<StaffRow>) {
    companion object {
        fun from(j: JSONObject) = StaffList(j.str("title"), j.list("rows", StaffRow::from))
    }
}

data class StaffDashboard(val role: String, val title: String, val cards: List<StaffCard>, val lists: List<StaffList>, val updatedAt: String) {
    companion object {
        fun from(j: JSONObject) = StaffDashboard(
            j.str("role"), j.str("title"), j.list("cards", StaffCard::from), j.list("lists", StaffList::from), j.str("updated_at"),
        )
    }
}

// ── Admin and developer homes ──

/** A class in the admin lists and calendar (`staff/classes`). [trainer] is empty when none is assigned. */
data class StaffClass(
    val runId: Int,
    val classId: String,
    val sku: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val startTime: String,
    val endTime: String,
    val mode: String,
    val venue: String,
    val trainer: String,
    val learnerCount: Int,
) {
    fun matches(query: String): Boolean {
        val q = query.trim()
        return q.isEmpty() || listOf(title, sku, classId, trainer).any { it.contains(q, ignoreCase = true) }
    }

    /** Every day from the start to the end date (at most 62). */
    val days: List<String>
        get() {
            val s = Fmt.date(startDate) ?: return emptyList()
            val e = Fmt.date(endDate)?.takeIf { !it.isBefore(s) } ?: s
            val out = mutableListOf<String>()
            var d = s
            while (!d.isAfter(e) && out.size < 62) { out += d.toString(); d = d.plusDays(1) }
            return out
        }

    companion object {
        fun from(j: JSONObject) = StaffClass(
            j.int("run_id"), j.str("class_id"), j.str("sku"), j.str("title"), j.str("start_date"), j.str("end_date"),
            j.str("start_time"), j.str("end_time"), j.str("mode"), j.str("venue"), j.str("trainer"), j.int("learner_count"),
        )
    }
}

data class AdminClassesReply(
    val today: String,
    val runningToday: Int,
    val next30Days: Int,
    val learnersToday: Int,
    val current: List<StaffClass>,
    val upcoming: List<StaffClass>,
) {
    companion object {
        fun from(j: JSONObject): AdminClassesReply {
            val k = j.obj("kpis")
            val current = j.list("current", StaffClass::from)
            val upcoming = j.list("upcoming", StaffClass::from)
            return AdminClassesReply(
                j.str("today"), k?.int("running_today") ?: current.size, k?.int("next_30_days") ?: upcoming.size,
                k?.int("learners_today") ?: current.sumOf { it.learnerCount }, current, upcoming,
            )
        }
    }
}

/** A non-WSQ course in the developer home (`staff/courses`). [nextClass] is "" when none is scheduled. */
data class StaffCourse(val id: Int, val sku: String, val title: String, val enabled: Boolean, val upcomingClasses: Int, val nextClass: String) {
    fun matches(query: String): Boolean {
        val q = query.trim()
        return q.isEmpty() || title.contains(q, ignoreCase = true) || sku.contains(q, ignoreCase = true)
    }

    companion object {
        fun from(j: JSONObject) = StaffCourse(
            j.int("id"), j.str("sku"), j.str("title"), j.optBool("enabled") ?: true, j.int("upcoming_classes"), j.str("next_class"),
        )
    }
}

data class DeveloperCoursesReply(val today: String, val total: Int, val withClasses: Int, val disabled: Int, val courses: List<StaffCourse>) {
    companion object {
        fun from(j: JSONObject): DeveloperCoursesReply {
            val k = j.obj("kpis")
            val courses = j.list("courses", StaffCourse::from)
            return DeveloperCoursesReply(
                j.str("today"), k?.int("courses") ?: courses.size, k?.int("with_classes") ?: courses.count { it.upcomingClasses > 0 },
                k?.int("disabled") ?: courses.count { !it.enabled }, courses,
            )
        }
    }
}

// ── Formatting ──

object Fmt {
    private val en = Locale.ENGLISH
    private val long = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", en)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", en)
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", en)
    val monthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", en)

    fun date(ymd: String): LocalDate? = try { if (ymd.isEmpty()) null else LocalDate.parse(ymd.take(10)) } catch (e: Exception) { null }

    /** "Wed, 4 Nov 2026" */
    fun longDate(ymd: String): String = date(ymd)?.format(long) ?: "Date to be confirmed"

    /** "4 Nov – 5 Nov 2026" */
    fun range(start: String, end: String): String {
        val s = date(start) ?: return "Date to be confirmed"
        val e = date(end)
        if (e == null || end == start) return longDate(start)
        return s.format(dayMonth) + " – " + e.format(dayMonthYear)
    }

    /** "09:30" → "9:30 am" */
    fun time(hm: String): String {
        val p = hm.split(":").mapNotNull { it.toIntOrNull() }
        if (p.size < 2) return hm
        val h12 = if (p[0] % 12 == 0) 12 else p[0] % 12
        return String.format(en, "%d:%02d %s", h12, p[1], if (p[0] < 12) "am" else "pm")
    }

    fun times(start: String, end: String): String =
        if (start.isEmpty()) "" else if (end.isEmpty()) time(start) else "${time(start)} – ${time(end)}"

    /** "2026-11" → "November 2026" */
    fun month(ym: String): String = date("$ym-01")?.format(monthYear) ?: ym

    /** 16.0 → "16 h", 7.5 → "7.5 h" */
    fun hours(h: Double): String =
        (if (h == Math.rint(h)) h.toLong().toString() else String.format(en, "%.1f", h)) + " h"

    /** "<CUR> 350" / "<CUR> 1,234.50" (site currency code) */
    fun money(amount: Double, code: String): String {
        val whole = Math.round(amount).toDouble() == amount
        val n = DecimalFormat(if (whole) "#,##0" else "#,##0.00").format(amount)
        return "${code.ifEmpty { Site.currency }} $n"
    }
}
