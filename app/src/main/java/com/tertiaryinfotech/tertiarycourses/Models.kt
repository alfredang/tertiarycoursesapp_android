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

data class Learner(val email: String, val firstName: String, val lastName: String, val fullName: String) {
    fun toJson(): String = JSONObject()
        .put("email", email).put("first_name", firstName).put("last_name", lastName).put("full_name", fullName)
        .toString()

    companion object {
        fun from(j: JSONObject) = Learner(j.str("email"), j.str("first_name"), j.str("last_name"), j.str("full_name"))
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
) {
    val id: String get() = if (runId > 0) "run-$runId" else "order-$orderNo-$sku-$startDate"
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
) {
    companion object {
        fun from(j: JSONObject) = ClassDetail(
            j.int("run_id"), j.str("class_id"), j.str("sku"), j.str("title"), j.str("start_date"), j.str("end_date"),
            j.str("start_time"), j.str("end_time"), j.str("mode"), j.str("venue"), j.str("room"), j.str("virtual_link"),
            j.str("trainer"), j.str("bucket"), j.list("sessions", ClassSession::from), AttendanceState.from(j.obj("attendance")),
            j.list("materials", Material::from), j.list("documents", Material::from), SurveyState.from(j.obj("survey")),
            j.obj("certificate")?.let(CertificateInfo::from), j.str("certificate_rule"),
        )
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

    /** "<CUR> 350" / "<CUR> 1,234.50" (site currency code) */
    fun money(amount: Double, code: String): String {
        val whole = Math.round(amount).toDouble() == amount
        val n = DecimalFormat(if (whole) "#,##0" else "#,##0.00").format(amount)
        return "${code.ifEmpty { Site.currency }} $n"
    }
}
