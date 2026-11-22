package com.tertiaryinfotech.tertiarycourses

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

/** A pushed screen inside a tab. */
sealed interface Route {
    data class ClassDetail(val runId: Int, val title: String) : Route
    data class CourseDetail(val course: Course) : Route
    data class Survey(val runId: Int) : Route
    data class Quiz(val runId: Int, val quizId: Int, val title: String) : Route
    data class TrainerClass(val runId: Int, val title: String) : Route
    /** A material, document or certificate shown inside the app. */
    data class Document(val title: String, val url: String) : Route
    data object Catalog : Route
    data object Feedback : Route
    data object About : Route
}

enum class Bucket(val label: String) { CURRENT("Current"), UPCOMING("Upcoming"), PAST("Past") }

/** Signed-in state, the learner's LMS data, the public catalog and navigation. One per activity. */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val tokens = TokenStore(app)
    private val settings = Settings(app)
    val api = Api(tokens) { viewModelScope.launch { clear() } }

    var restoring by mutableStateOf(true); private set
    var learner by mutableStateOf<Learner?>(null); private set
    /** Classes the account attends / teaches; [classes] is the two merged per bucket. */
    private var learnerClasses by mutableStateOf<ClassesReply?>(null)
    private var trainerClasses by mutableStateOf<ClassesReply?>(null)
    val classes: ClassesReply? get() = ClassesReply.merge(learnerClasses, trainerClasses)

    // ── Roles ──
    //
    // The app shows one role at a time. A trainer can look at the app as a learner
    // (their own classes, certificates and billing). Staff dashboards are offered to
    // the owner account only, and only for roles the website says it holds. The
    // choice changes what this device shows, never what the server allows.

    private var viewRole by mutableStateOf(settings.viewRole)

    val availableRoles: List<String>
        get() {
            val l = learner ?: return emptyList()
            val roles = if (l.isTrainer) mutableListOf("trainer", "learner") else mutableListOf("learner")
            if (l.email.lowercase() == OWNER_EMAIL) {
                // The franchise dashboard belongs to the Singapore site only.
                roles += staffRoleNames.keys.filter { it in l.roles && (it != "franchise_admin" || Site.code == "SG") }
            }
            return roles
        }
    val canSwitchRole get() = availableRoles.size > 1
    val activeRole: String get() = availableRoles.let { if (viewRole in it) viewRole else it.firstOrNull() ?: "learner" }
    /** Showing the trainer view (teaching list, Payout and Hours tabs). */
    val isTrainer get() = activeRole == "trainer"
    /** The staff role being viewed, or null in the trainer and learner views. */
    val staffRole: String? get() = activeRole.takeIf { it in staffRoleNames }

    fun switchRole(role: String) {
        if (role == activeRole) return
        viewRole = role
        settings.viewRole = role
        if (!isTrainer) trainerClasses = null
        // Screens pushed in the other role's tabs do not belong to this one.
        stacks.forEach { (key, stack) -> if (key.startsWith("learner") && key != "learner$TAB_ACCOUNT") stack.clear() }
        viewModelScope.launch { refreshClasses() }
    }

    /** The profile picture; null shows the initials. */
    var avatar by mutableStateOf<ImageBitmap?>(null); private set
    private var avatarLoadedVersion: Long? = null

    /** Trainer certifications being edited on the Account page (kept here across tab switches). */
    val certDrafts = mutableStateListOf<CertDraft>()
    var certsLoaded by mutableStateOf(false); private set

    var payouts by mutableStateOf<PayoutsReply?>(null); private set
    var trainingHours by mutableStateOf<TrainingHoursReply?>(null); private set
    var staffDashboard by mutableStateOf<StaffDashboard?>(null); private set

    /** Appearance: the account's saved theme while signed in, the device setting otherwise. */
    var themePref by mutableStateOf(settings.theme); private set
    var remindersOn by mutableStateOf(settings.remindersOn); private set
    /** Bumped after each successful class load (the UI asks for notification permission once). */
    var classLoads by mutableIntStateOf(0); private set
    private var lastClassRefresh = 0L
    var certificates by mutableStateOf<List<CertificateInfo>?>(null); private set
    var orders by mutableStateOf<List<Order>?>(null); private set
    var loadError by mutableStateOf<String?>(null); private set
    var courses by mutableStateOf<List<Course>>(emptyList()); private set

    val isSignedIn get() = learner != null

    // ── Navigation (kept here so it survives rotation) ──
    var guestTab by mutableIntStateOf(0)
    var learnerTab by mutableIntStateOf(0)
    private val stacks = HashMap<String, SnapshotStateList<Route>>()
    fun stack(key: String): SnapshotStateList<Route> = stacks.getOrPut(key) { mutableStateListOf() }
    var bucket by mutableStateOf<Bucket?>(null)

    /** Debug-only screenshot hooks. */
    private var openSurveyOnce = false
    private var openQuizOnce = 0
    private var pendingRunId: Int? = null
    private var pendingBucket: Bucket? = null

    private val appVersion = BuildConfig.VERSION_NAME
    private val cacheFile = File(app.filesDir, "catalog.json")
    private var started = false

    init {
        viewModelScope.launch { loadCatalogLocal() }
    }

    /** Called once from the activity with the launch intent extras. */
    fun start(extras: Bundle?) {
        if (started) return
        started = true
        viewModelScope.launch {
            val r = async { restore(extras) }
            val c = async { refreshCatalog() }
            r.await(); c.await()
        }
    }

    /** Debug builds: a new `am start` intent while running re-applies the hooks. */
    fun onNewLaunch(extras: Bundle?) {
        if (!BuildConfig.DEBUG || extras == null) return
        viewModelScope.launch { applyDebugHooks(extras) }
    }

    private suspend fun restore(extras: Bundle?) {
        try {
            if (BuildConfig.DEBUG && extras != null && applyDebugHooks(extras)) return
            if (tokens.token == null) return
            try {
                val me = api.getAsync("auth/me")
                rememberLearner(Learner.from(me.getJSONObject("learner")))
                refreshAll()
            } catch (e: ApiException) {
                if (e.status == 401) clear() else {
                    // Offline: keep the token and the last profile; retry on refresh.
                    tokens.learnerJson?.let { rememberLearner(Learner.from(JSONObject(it))) }
                    loadError = e.message
                }
            }
        } finally {
            restoring = false
        }
    }

    /**
     * Screenshot automation (debug builds only), mirroring the iOS launch environment:
     * `--es UITEST_SIGNIN "email|password" --ei START_TAB n --ei OPEN_RUN_ID id --ez OPEN_SURVEY true
     *  --es OPEN_BUCKET current|upcoming|past --ez UITEST_SIGNOUT true`.
     * Returns true when it handled sign-in itself.
     */
    private suspend fun applyDebugHooks(extras: Bundle): Boolean {
        if (!BuildConfig.DEBUG) return false
        val signOut = extras.getBoolean("UITEST_SIGNOUT", false)
        if (signOut) clear()
        if (extras.containsKey("START_TAB")) {
            val t = extras.getInt("START_TAB", 0)
            guestTab = t.coerceIn(0, 2); learnerTab = t.coerceIn(0, 5)
        }
        extras.getString("OPEN_BUCKET")?.let { b -> pendingBucket = Bucket.entries.firstOrNull { it.name.equals(b, true) } }
        if (extras.containsKey("OPEN_RUN_ID")) pendingRunId = extras.getInt("OPEN_RUN_ID").takeIf { it > 0 }
        if (extras.getBoolean("OPEN_SURVEY", false)) openSurveyOnce = true
        openQuizOnce = extras.getInt("OPEN_QUIZ_ID", 0)
        extras.getString("UITEST_THEME")?.let { themeOverride = it; themePref = it }
        stacks.values.forEach { it.clear() }
        if (signOut) return true
        val creds = extras.getString("UITEST_SIGNIN") ?: run { applyPending(); return false }
        val bar = creds.indexOf('|')
        if (bar < 0) return false
        try {
            signIn(creds.substring(0, bar), creds.substring(bar + 1), refresh = false)
            refreshAll()
        } catch (e: Exception) {
            Log.w("TCLMS", "UITEST_SIGNIN failed: ${e.message}")
        }
        return true
    }

    // ── Sign-in ──

    suspend fun signIn(email: String, password: String, refresh: Boolean = true) {
        val reply = api.postAsync("auth/password", JSONObject()
            .put("email", email.trim()).put("password", password)
            .put("platform", "android").put("app_version", appVersion))
        finishSignIn(reply, refresh)
    }

    suspend fun sendCode(email: String): String {
        val reply = api.postAsync("auth/otpsend", JSONObject().put("email", email.trim()))
        return reply.str("message").ifEmpty { "Check your email for a 6-digit code." }
    }

    suspend fun verifyCode(email: String, code: String) {
        val reply = api.postAsync("auth/otpverify", JSONObject()
            .put("email", email.trim()).put("code", code)
            .put("platform", "android").put("app_version", appVersion))
        finishSignIn(reply, true)
    }

    private fun finishSignIn(reply: JSONObject, refresh: Boolean) {
        val token = reply.str("token")
        if (token.isEmpty()) throw ApiException("The server sent an unexpected reply. Please try again.")
        tokens.token = token
        stacks.values.forEach { it.clear() }
        bucket = null
        rememberLearner(Learner.from(reply.getJSONObject("learner")))
        if (refresh) viewModelScope.launch { refreshAll() }
    }

    private var themeOverride: String? = null   // debug screenshots only

    private fun rememberLearner(l: Learner, syncPicture: Boolean = true) {
        learner = l
        tokens.learnerJson = l.toJson()
        applyTheme(themeOverride ?: l.theme)
        if (syncPicture) syncAvatar(l.avatarVersion)
    }

    // ── Profile picture ──

    /** Downloads the picture when the site has a newer one than this device shows. */
    private fun syncAvatar(version: Long?) {
        if (version == null) {
            avatar = null; avatarLoadedVersion = null
            return
        }
        if (version == avatarLoadedVersion) return
        avatarLoadedVersion = version
        viewModelScope.launch {
            try {
                val bytes = api.getBytes("profile/avatar")
                val bitmap = withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                if (bitmap != null && avatarLoadedVersion == version) avatar = bitmap.asImageBitmap()
            } catch (_: Exception) {
                // No picture, or offline: the initials stay.
            }
        }
    }

    /** Shrinks the chosen photo to a 512px square JPEG, shows it at once and uploads it. */
    suspend fun setAvatar(photo: Uri) {
        val (square, jpeg) = withContext(Dispatchers.IO) {
            val bitmap = squarePicture(getApplication(), photo) ?: throw ApiException("That picture could not be opened.")
            val out = ByteArrayOutputStream()
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)) throw ApiException("That picture could not be used.")
            bitmap to out.toByteArray()
        }
        val reply = api.postAsync("profile/avatar", JSONObject().put("image", Base64.encodeToString(jpeg, Base64.NO_WRAP)))
        avatar = square.asImageBitmap()
        reply.obj("learner")?.let { j ->
            val l = Learner.from(j)
            rememberLearner(l, syncPicture = false)
            avatarLoadedVersion = l.avatarVersion
        }
    }

    suspend fun removeAvatar() {
        val reply = api.postAsync("profile/avatar", JSONObject().put("remove", 1))
        avatar = null
        avatarLoadedVersion = null
        reply.obj("learner")?.let { rememberLearner(Learner.from(it), syncPicture = false) }
    }

    /** Shows the chosen appearance straight away; Save Changes stores it on the account. */
    fun previewTheme(pref: String) = applyTheme(pref)

    private fun applyTheme(pref: String) {
        themePref = pref
        settings.theme = pref
    }

    /**
     * Saves the profile (first name, last name, phone, theme, and the LinkedIn link when
     * [linkedin] is not null) to the account. The theme is applied on this device straight
     * away, even when the site can't be reached.
     */
    suspend fun updateProfile(firstName: String, lastName: String, phone: String, linkedin: String?, theme: String): String {
        applyTheme(theme)
        val reply = try {
            api.postAsync("profile", JSONObject()
                .put("first_name", firstName.trim()).put("last_name", lastName.trim())
                .put("phone", phone.trim()).put("theme", theme)
                .apply { if (linkedin != null) put("linkedin_url", linkedin.trim()) })
        } catch (e: ApiException) {
            // A site that hasn't been updated yet has no profile endpoint.
            if (e.status == 404) throw ApiException("Your appearance is saved on this device. Editing your name and phone isn't available on this site yet.", 404)
            throw e
        }
        reply.obj("learner")?.let { rememberLearner(Learner.from(it)) }
        return reply.str("message").ifEmpty { "Profile updated." }
    }

    // ── Trainer certifications ──

    suspend fun loadCertifications() {
        val list = api.getAsync("trainer/certifications").list("certifications", CertDraft::from)
        certDrafts.clear()
        certDrafts.addAll(list)
        certsLoaded = true
    }

    /** Replaces the list on the site (the same list the web My Profile edits). */
    suspend fun saveCertifications() {
        val items = JSONArray()
        certDrafts.forEach { items.put(JSONObject().put("name", it.name).put("issuer", it.issuer).put("year", it.year)) }
        api.postAsync("trainer/certifications", JSONObject().put("items", items))
    }

    // ── Trainer payout and training hours, staff dashboard ──

    suspend fun loadPayouts() {
        payouts = PayoutsReply.from(api.getAsync("trainer/payouts"))
    }

    suspend fun loadTrainingHours() {
        trainingHours = TrainingHoursReply.from(api.getAsync("trainer/hours"))
    }

    suspend fun loadStaffDashboard(role: String) {
        if (staffDashboard?.role != role) staffDashboard = null
        staffDashboard = StaffDashboard.from(api.getAsync("staff/dashboard", mapOf("role" to role)))
    }

    fun changeReminders(on: Boolean) {
        remindersOn = on
        settings.remindersOn = on
        rescheduleReminders()
    }

    private fun rescheduleReminders() {
        val all = if (remindersOn && learner != null) classes?.let { it.current + it.upcoming } ?: emptyList() else emptyList()
        Reminders.schedule(getApplication(), Reminders.build(all))
    }

    /** The app came back to the foreground: reload the classes, at most once every 5 minutes. */
    fun onForeground() {
        if (!isSignedIn || restoring) return
        if (System.currentTimeMillis() - lastClassRefresh < 5 * 60_000) return
        viewModelScope.launch { refreshClasses() }
    }

    fun signOut() {
        viewModelScope.launch {
            try { api.postAsync("auth/logout") } catch (_: Exception) { }
            clear()
        }
    }

    /** Permanently deletes the learner's login account on the site. */
    suspend fun deleteAccount(): String {
        val reply = api.postAsync("auth/delete", JSONObject().put("confirm", "DELETE"))
        clear()
        return reply.str("message").ifEmpty { "Your account has been deleted." }
    }

    fun clear() {
        tokens.clear()
        learner = null
        learnerClasses = null
        trainerClasses = null
        lastClassRefresh = 0
        themeOverride = null
        applyTheme("system")
        avatar = null
        avatarLoadedVersion = null
        viewRole = ""
        settings.viewRole = ""
        certDrafts.clear()
        certsLoaded = false
        payouts = null
        trainingHours = null
        staffDashboard = null
        Reminders.schedule(getApplication(), emptyList())
        certificates = null
        orders = null
        loadError = null
        bucket = null
        stacks.values.forEach { it.clear() }
        // Next session (or the signed-out tabs) start on their first tab.
        guestTab = 0
        learnerTab = 0
    }

    // ── Data ──

    suspend fun refreshAll() {
        coroutineScope {
            val c = async { refreshClasses() }
            val e = async { refreshCertificates() }
            val b = async { refreshBilling() }
            c.await(); e.await(); b.await()
        }
    }

    suspend fun refreshClasses() {
        lastClassRefresh = System.currentTimeMillis()
        var error: String? = null
        coroutineScope {
            val mine = async {
                try { ClassesReply.from(api.getAsync("learner/classes")) } catch (e: ApiException) { error = e.message; null }
            }
            // Only trainers have a teaching list; an older site answers 404 and is ignored.
            val teaching = async {
                if (!isTrainer) null else try {
                    ClassesReply.from(api.getAsync("trainer/classes")).let { r ->
                        fun tag(l: List<ClassItem>) = l.map { it.copy(role = "trainer") }
                        r.copy(current = tag(r.current), upcoming = tag(r.upcoming), past = tag(r.past))
                    }
                } catch (e: ApiException) {
                    if (e.status != 404 && e.status != 403) error = e.message
                    null
                }
            }
            val l = mine.await()
            val t = teaching.await()
            if (l != null) learnerClasses = l
            if (t != null || !isTrainer) trainerClasses = t
        }
        val reply = classes
        if (reply == null) {
            loadError = error
            return
        }
        loadError = error
        if (bucket == null) {
            bucket = when {
                reply.current.isNotEmpty() -> Bucket.CURRENT
                reply.upcoming.isNotEmpty() -> Bucket.UPCOMING
                reply.past.isNotEmpty() -> Bucket.PAST
                else -> Bucket.CURRENT
            }
        }
        if (error == null) {
            classLoads++
            rescheduleReminders()
        }
        applyPending()
    }

    private fun applyPending() {
        if (!BuildConfig.DEBUG) return
        val c = classes ?: return
        pendingBucket?.let { bucket = it; pendingBucket = null }
        pendingRunId?.let { id ->
            c.all.firstOrNull { it.runId == id }?.let { item ->
                stack("learner0").apply {
                    clear()
                    if (item.isTeaching) add(Route.TrainerClass(item.runId, item.title))
                    else {
                        add(Route.ClassDetail(item.runId, item.title))
                        if (openSurveyOnce) add(Route.Survey(item.runId))
                        if (openQuizOnce > 0) add(Route.Quiz(item.runId, openQuizOnce, "Quiz"))
                    }
                }
                openSurveyOnce = false
                openQuizOnce = 0
                learnerTab = 0
            }
            pendingRunId = null
        }
    }

    suspend fun refreshCertificates() {
        try {
            certificates = api.getAsync("learner/certificates").list("certificates", CertificateInfo::from)
        } catch (e: ApiException) {
            loadError = e.message
        }
    }

    suspend fun refreshBilling() {
        try {
            orders = api.getAsync("learner/billing").list("orders", Order::from)
        } catch (e: ApiException) {
            loadError = e.message
        }
    }

    suspend fun classDetail(runId: Int): ClassDetail =
        ClassDetail.from(api.getAsync("learner/class", mapOf("run_id" to "$runId")).getJSONObject("class"))

    suspend fun trainerClass(runId: Int): TrainerClassDetail =
        TrainerClassDetail.from(api.getAsync("trainer/class", mapOf("run_id" to "$runId")).getJSONObject("class"))

    /** Trainer: save who is present for one session. [records] = (email, name, present). */
    suspend fun saveAttendance(runId: Int, sessionKey: String, records: List<Triple<String, String, Boolean>>): String {
        val arr = org.json.JSONArray()
        records.forEach { (email, name, present) -> arr.put(JSONObject().put("email", email).put("name", name).put("present", present)) }
        val body = JSONObject().put("run_id", runId).put("session_key", sessionKey).put("records", arr)
        return api.postAsync("trainer/attendance", body).str("message").ifEmpty { "Attendance saved." }
    }

    suspend fun markAttendance(runId: Int): String =
        api.postAsync("learner/attendance", JSONObject().put("run_id", runId)).str("message")
            .ifEmpty { "You have been marked present." }

    suspend fun survey(runId: Int): Survey =
        Survey.from(api.getAsync("learner/survey", mapOf("run_id" to "$runId")).getJSONObject("survey"))

    suspend fun submitSurvey(runId: Int, answers: Map<String, String>): String {
        val body = JSONObject().put("run_id", runId).put("answers", JSONObject(answers as Map<*, *>))
        val msg = api.postAsync("learner/survey", body).str("message").ifEmpty { "Thank you for your feedback!" }
        refreshClasses()
        return msg
    }

    suspend fun quiz(runId: Int, quizId: Int): Quiz =
        Quiz.from(api.getAsync("learner/quiz", mapOf("run_id" to "$runId", "quiz_id" to "$quizId")).getJSONObject("quiz"))

    /** [answers] maps a question id to the chosen option index. The site scores and records the attempt. */
    suspend fun submitQuiz(runId: Int, quizId: Int, answers: Map<String, Int>): QuizResult {
        val body = JSONObject().put("run_id", runId).put("quiz_id", quizId).put("answers", JSONObject(answers as Map<*, *>))
        return QuizResult.from(api.postAsync("learner/quiz", body))
    }

    // ── Catalog: fetched feed > on-device cache > bundled snapshot ──

    val categories: List<String>
        get() = courses.map { it.category }.distinct().sortedWith { a, b ->
            when {
                a == b -> 0
                a == "Other Courses" -> 1
                b == "Other Courses" -> -1
                else -> a.compareTo(b)
            }
        }

    private suspend fun loadCatalogLocal() {
        val started = System.currentTimeMillis()
        val list = withContext(Dispatchers.IO) {
            val cached = try { if (cacheFile.exists()) parseCourses(cacheFile.readText()) else null } catch (e: Exception) { null }
            cached?.takeIf { it.isNotEmpty() } ?: try {
                getApplication<Application>().assets.open("catalog.json").bufferedReader().use { parseCourses(it.readText()) }
            } catch (e: Exception) { emptyList() }
        }
        if (BuildConfig.DEBUG) Log.d("TCLMS", "local catalog: ${list.size} courses in ${System.currentTimeMillis() - started} ms")
        if (courses.isEmpty()) courses = list
    }

    suspend fun refreshCatalog() {
        try {
            val reply = api.getAsync("catalog")
            val list = withContext(Dispatchers.Default) {
                val arr = reply.optJSONArray("courses") ?: return@withContext emptyList()
                arr.objects().map(Course::from).also { parsed ->
                    if (parsed.isNotEmpty()) withContext(Dispatchers.IO) {
                        val tmp = File(cacheFile.parentFile, "catalog.json.tmp")
                        tmp.writeText(JSONObject().put("courses", arr).toString())
                        tmp.renameTo(cacheFile)
                    }
                }
            }
            if (list.isNotEmpty()) courses = list
        } catch (_: Exception) {
            // Offline: keep the cached / bundled catalog.
        }
    }

    private fun parseCourses(text: String): List<Course> =
        JSONObject(text).optJSONArray("courses")?.objects()?.map(Course::from) ?: emptyList()

    companion object {
        /** The only account that is offered the staff dashboards. */
        private const val OWNER_EMAIL = "angch@tertiaryinfotech.com"

        /** Staff roles, in the order they are offered. */
        val staffRoleNames = linkedMapOf(
            "admin" to "Admin", "marketing" to "Marketing", "developer" to "Developer", "franchise_admin" to "Franchise Admin",
        )

        fun roleName(role: String): String = staffRoleNames[role] ?: if (role == "trainer") "Trainer" else "Learner"

        // Signed-in tab ids (also the back-stack keys "learner<id>"); they stay the same in every role.
        const val TAB_CLASSES = 0
        const val TAB_CALENDAR = 1
        /** Certificates for a learner, Payout for a trainer. */
        const val TAB_THIRD = 2
        /** Billing for a learner, Hours for a trainer. */
        const val TAB_FOURTH = 3
        const val TAB_ACCOUNT = 4
        const val TAB_DASHBOARD = 5
    }
}
