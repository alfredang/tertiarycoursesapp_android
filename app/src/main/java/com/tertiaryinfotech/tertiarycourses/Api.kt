package com.tertiaryinfotech.tertiarycourses

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.TimeUnit

class ApiException(message: String, val status: Int = 0) : Exception(message)

/**
 * JSON client for the site's learner API (`/mobileapi/`*, MMD_MobileApi in ai-mms).
 * Every response is `{"ok": true, ...}` or `{"ok": false, "error": "<message>"}`.
 */
class Api(private val tokens: TokenStore, private val onSessionExpired: () -> Unit) {

    fun get(path: String, query: Map<String, String> = emptyMap()): JSONObject = send(path, "GET", query, null)

    fun post(path: String, body: JSONObject = JSONObject()): JSONObject = send(path, "POST", emptyMap(), body)

    suspend fun getAsync(path: String, query: Map<String, String> = emptyMap()) =
        withContext(Dispatchers.IO) { get(path, query) }

    suspend fun postAsync(path: String, body: JSONObject = JSONObject()) =
        withContext(Dispatchers.IO) { post(path, body) }

    /** Raw bytes of an authenticated GET (the profile picture). */
    suspend fun getBytes(path: String): ByteArray = withContext(Dispatchers.IO) {
        val token = tokens.token
        val request = Request.Builder().url(Site.apiUrl + path)
            .header("User-Agent", userAgent)
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()
        try {
            client.newCall(request).execute().use { r ->
                (if (r.isSuccessful) r.body?.bytes() else null) ?: throw ApiException("No picture.", r.code)
            }
        } catch (e: IOException) {
            throw ApiException("Can't reach ${Site.siteName}. Check your connection and try again.")
        }
    }

    /**
     * An authenticated GET that answers a file (the attendance workbook), saved in the cache
     * under the server's filename so it can be shared or saved. Errors are the JSON envelope.
     */
    suspend fun download(context: Context, path: String, query: Map<String, String>, fallbackName: String): File =
        withContext(Dispatchers.IO) {
            val url = (Site.apiUrl + path).toHttpUrl().newBuilder().apply {
                query.forEach { (k, v) -> addQueryParameter(k, v) }
            }.build()
            val token = tokens.token
            val request = Request.Builder().url(url).header("User-Agent", userAgent)
                .apply { if (token != null) header("Authorization", "Bearer $token") }
                .build()
            val (status, disposition, bytes) = try {
                client.newCall(request).execute().use { Triple(it.code, it.header("Content-Disposition").orEmpty(), it.body?.bytes() ?: ByteArray(0)) }
            } catch (e: IOException) {
                throw ApiException("The download did not finish. Check your connection and try again.")
            }
            if (status !in 200..299) {
                if (status == 401 && token != null) onSessionExpired()
                val msg = try { JSONObject(String(bytes)).optString("error") } catch (e: Exception) { "" }
                throw ApiException(msg.ifEmpty { "Something went wrong ($status). Please try again." }, status)
            }
            var name = Regex("""filename\s*=\s*"?([^";]+)"?""").find(disposition)?.groupValues?.get(1).orEmpty()
                .replace(Regex("""[/\\:*?"<>|\p{Cntrl}]"""), "-").trim().take(120)
            if (name.isEmpty() || '.' !in name) name = fallbackName
            val root = File(context.cacheDir, "downloads")
            val file = File(File(root, UUID.randomUUID().toString()).apply { mkdirs() }, name)
            file.writeBytes(bytes)
            file
        }

    private fun send(path: String, method: String, query: Map<String, String>, body: JSONObject?): JSONObject {
        val url = (Site.apiUrl + path).toHttpUrl().newBuilder().apply {
            query.forEach { (k, v) -> addQueryParameter(k, v) }
        }.build()
        val token = tokens.token
        val request = Request.Builder().url(url)
            .header("Accept", "application/json")
            // The sites' .htaccess bot list 403s some User-Agents (empty, "python",
            // "CFNetwork"...), so always identify the app explicitly.
            .header("User-Agent", userAgent)
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .apply {
                if (method == "POST") post((body ?: JSONObject()).toString().toRequestBody(JSON))
                else get()
            }
            .build()

        val started = System.currentTimeMillis()
        val (status, text) = try {
            client.newCall(request).execute().use { it.code to (it.body?.string() ?: "") }
        } catch (e: IOException) {
            if (BuildConfig.DEBUG) Log.w("TCLMS", "$method $path failed: ${e.message}")
            throw ApiException("Can't reach ${Site.siteName}. Check your connection and try again.")
        }
        if (BuildConfig.DEBUG) Log.d("TCLMS", "$method $path -> $status in ${System.currentTimeMillis() - started} ms")
        val json = try { JSONObject(text) } catch (e: Exception) { null }
        if (status !in 200..299) {
            if (status == 401 && token != null && !path.startsWith("auth/")) onSessionExpired()
            val msg = json?.optString("error").orEmpty()
            throw ApiException(msg.ifEmpty { "Something went wrong ($status). Please try again." }, status)
        }
        if (json == null || (json.has("ok") && !json.optBoolean("ok"))) {
            val msg = json?.optString("error").orEmpty()
            throw ApiException(msg.ifEmpty { "The server sent an unexpected reply. Please try again." }, status)
        }
        return json
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        val userAgent = "TCLMS-Android/${BuildConfig.VERSION_NAME} (${Site.code}; ${BuildConfig.APPLICATION_ID})"

        val client: OkHttpClient = OkHttpClient.Builder()
            .dns(Ipv4FirstDns)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
    }
}

/**
 * OkHttp 4 tries a host's addresses one after another. The sites publish AAAA records,
 * so on networks where IPv6 is broken every call first waits out an IPv6 connect timeout.
 * Trying IPv4 first avoids that; IPv6-only (NAT64) networks still get their AAAA answers.
 */
private object Ipv4FirstDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> =
        Dns.SYSTEM.lookup(hostname).sortedBy { if (it is Inet4Address) 0 else 1 }
}

/** The session token (and the last learner profile), encrypted on this device only. */
class TokenStore(context: Context) {
    private val prefs: SharedPreferences = open(context.applicationContext)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit { if (value == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, value) }

    var learnerJson: String?
        get() = prefs.getString(KEY_LEARNER, null)
        set(value) = prefs.edit { if (value == null) remove(KEY_LEARNER) else putString(KEY_LEARNER, value) }

    /**
     * Admin sign-in with Google: the verifier of the sign-in that is open in the browser.
     * Kept here so it outlives the app being closed meanwhile; used once, then removed.
     */
    var googleVerifier: String?
        get() = prefs.getString(KEY_GOOGLE_VERIFIER, null)
        set(value) = prefs.edit { if (value == null) remove(KEY_GOOGLE_VERIFIER) else putString(KEY_GOOGLE_VERIFIER, value) }

    fun clear() = prefs.edit { clear() }

    private companion object {
        const val FILE = "session"
        const val KEY_TOKEN = "token"
        const val KEY_LEARNER = "learner"
        const val KEY_GOOGLE_VERIFIER = "google_verifier"

        fun open(context: Context): SharedPreferences {
            fun create(): SharedPreferences {
                val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
                return EncryptedSharedPreferences.create(
                    context, FILE, key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            }
            return try {
                create()
            } catch (e: Exception) {
                // Keystore key lost (e.g. restored from another device): start over signed out.
                context.deleteSharedPreferences(FILE)
                create()
            }
        }
    }
}
