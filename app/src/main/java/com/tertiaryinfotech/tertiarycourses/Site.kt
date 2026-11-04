package com.tertiaryinfotech.tertiarycourses

/**
 * The country site this build of TC LMS serves. This file is the ONLY source
 * difference between TC LMS SG, MY and GH — every other Kotlin file is shared.
 * (Per-app differences outside the source: applicationId, app_name and the
 * launcher icon.) Each app talks only to its own site's LMS.
 */
object Site {
    const val code = "SG"
    const val appName = "TC LMS SG"
    const val countryName = "Singapore"
    const val siteName = "Tertiary Courses Singapore"
    const val baseUrl = "https://www.tertiarycourses.com.sg/"
    const val websiteLabel = "tertiarycourses.com.sg"
    const val email = "enquiry@tertiaryinfotech.com"
    const val phone = "+65 6100 0613"
    const val whatsApp = "6588666375"
    const val whatsAppLabel = "+65 8866 6375"
    const val currency = "SGD"
    /** Class times from the site are local to it. */
    const val timeZone = "Asia/Singapore"
    const val academy = "Tertiary Infotech Academy Pte. Ltd."
    const val developerUrl = "https://www.tertiaryinfotech.com/"
    const val privacyUrl = "https://www.tertiaryinfotech.com/privacy"
    /** Shown beside course fees: SG lists fees before GST. */
    const val priceNote = "before 9% GST"

    /** JSON API root (MMD_MobileApi in ai-mms). */
    const val apiUrl = baseUrl + "mobileapi/"
    const val forgotPasswordUrl = baseUrl + "customer/account/forgotpassword/"
}
