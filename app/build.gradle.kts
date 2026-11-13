import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// -----------------------------------------------------------------------------
// Firebase (push notifications): the Google Services plugin is applied ONLY when
// app/google-services.json exists. Without the file the app still builds and runs;
// push notifications are simply inactive. See INSTALLATION_AND_CONFIGURATION_GUIDE.md.
// -----------------------------------------------------------------------------
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// -----------------------------------------------------------------------------
// Local, NON-committed configuration (local.properties is in .gitignore):
//   zensa.api.url           e.g. http://10.0.2.2:8080/  (emulator) or http://192.168.0.12:8080/ (phone)
//   zensa.api.url.release   e.g. https://zensa-api.azurewebsites.net/
//   zensa.google.clientId   Google OAuth *Android* client id for SSO (…apps.googleusercontent.com)
// -----------------------------------------------------------------------------
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun configValue(name: String, default: String): String =
    localProps.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }
        ?: providers.gradleProperty(name).orNull?.trim()?.takeIf { it.isNotEmpty() }
        ?: default

fun withTrailingSlash(url: String) = if (url.endsWith("/")) url else "$url/"

val googleClientId = configValue("zensa.google.clientId", "")
// Google's native-app redirect scheme is the client id with its parts reversed.
val googleRedirectScheme =
    if (googleClientId.endsWith(".apps.googleusercontent.com"))
        "com.googleusercontent.apps." + googleClientId.removeSuffix(".apps.googleusercontent.com")
    else
        "com.zensa.app.oauth.notconfigured" // keeps manifest merging valid when SSO is not configured yet

// Release signing: read from keystore.properties (NEVER committed). See PLAY_STORE_PREPARATION.md.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.zensa.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zensa.app"
        minSdk = 26
        targetSdk = 36

        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")
        buildConfigField("String", "GOOGLE_REDIRECT_SCHEME", "\"$googleRedirectScheme\"")

        // Required by the AppAuth library's redirect activity (manifest placeholder).
        manifestPlaceholders["appAuthRedirectScheme"] = googleRedirectScheme
    }

    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"https://socket-eyed-screensaver-whom.trycloudflare.com/\""
            )
        }

        release {
            isMinifyEnabled = false

            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"https://zensa-api.azurewebsites.net/\""
            )

            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // android.util.Log etc. return defaults in plain JVM unit tests instead of throwing.
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {

    // ---------------- Core ----------------

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat) // per-app language switching (English / isiZulu / Sesotho / isiXhosa)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.activity.compose)


    // ---------------- Compose ----------------

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)


    // ---------------- Network ----------------

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.kotlinx.serialization.json)


    // ---------------- Local Storage ----------------

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    ksp(libs.androidx.room.compiler)


    // ---------------- Background Work ----------------

    implementation(libs.androidx.work.runtime.ktx)


    // ---------------- SSO (OAuth 2.0 / OpenID Connect, Authorization Code + PKCE) ----------------

    implementation(libs.appauth)


    // ---------------- Push notifications (Firebase Cloud Messaging) ----------------

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)


    // ---------------- Unit Tests ----------------

    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)


    // ---------------- Instrumented Tests ----------------

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(platform(libs.androidx.compose.bom))

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)

    androidTestImplementation(libs.androidx.room.testing)
}
