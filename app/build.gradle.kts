import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Supabase credentials live in local.properties (git-ignored).
// The publishable key only grants the public read access already allowed by RLS.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
fun localProp(name: String): String =
    localProps.getProperty(name) ?: error("Missing '$name' in local.properties")

// TMDB v3 API key (TMDB_API_KEY in the git-ignored .env, shared with the website).
// Optional: without it the detail screen just has no description.
val tmdbApiKey: String = rootProject.file(".env").takeIf { it.exists() }
    ?.readLines()
    ?.firstNotNullOfOrNull { line ->
        line.trim().takeIf { it.startsWith("TMDB_API_KEY=") }
            ?.substringAfter("=")?.trim()?.trim('"', '\'')
    }
    .orEmpty()

/** Points a build type at a Supabase project: "" = production, "local." = local Docker. */
fun com.android.build.api.dsl.ApplicationBuildType.supabase(prefix: String) {
    buildConfigField("String", "SUPABASE_URL", "\"${localProp("supabase.${prefix}url")}\"")
    buildConfigField("String", "SUPABASE_KEY", "\"${localProp("supabase.${prefix}key")}\"")
}

android {
    namespace = "com.moviesnight.tv"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.moviesnight.tv"
        minSdk = 28
        targetSdk = 37
        versionCode = 6
        versionName = "0.6.0"
        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
    }

    // release → production Supabase (HTTPS). debug/profile → local Docker over the LAN.
    buildTypes {
        debug {
            supabase("local.")
        }
        release {
            supabase("")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Personal sideloaded app: sign release with the debug key for now.
            signingConfig = signingConfigs.getByName("debug")
        }
        // Release-optimised (R8, no debug overhead) but able to reach the local HTTP
        // Supabase. Use it to judge real performance: gradlew installProfile
        create("profile") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            supabase("local.")
        }
    }

    // profile shares the debug-only cleartext network config
    sourceSets.getByName("profile") {
        res.directories.add("src/debug/res")
        manifest.srcFile("src/debug/AndroidManifest.xml")
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
        unitTests.all {
            // `gradlew testDebugUnitTest -Plive` also runs the tests that hit the real database.
            it.systemProperty("live", providers.gradleProperty("live").isPresent.toString())
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.paging.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.tv.material)
    debugImplementation(libs.compose.ui.tooling)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
}
