# Models are parsed by hand with org.json (part of the Android framework) — no reflection,
# so no keep rules are needed for them. OkHttp 4.12 ships its own consumer rules.

# Tink (via androidx.security:security-crypto) references compile-time-only annotations.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.api.client.http.**
-dontwarn org.joda.time.**

# OkHttp optional TLS providers.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
