import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.room)
}

val localProperties =
    Properties().apply {
        rootDir
            .resolve("local.properties")
            .takeIf { it.exists() }
            ?.inputStream()
            ?.use { load(it) }
    }

fun secretValue(
    property: String,
    localKey: String,
    environment: String,
): String =
    (project.findProperty(property) as String?)
        ?: localProperties.getProperty(localKey)
        ?: System.getenv(environment)
        ?: ""

fun translatedLocaleTags(): List<String> =
    file("src/main/res")
        .listFiles()
        .orEmpty()
        .filter { it.isDirectory && it.resolve("strings.xml").exists() }
        .mapNotNull { dir ->
            val qualifier = dir.name.removePrefix("values-")
            when {
                qualifier == dir.name -> null
                qualifier.startsWith("b+") -> qualifier.removePrefix("b+").replace('+', '-')
                Regex("[a-z]{2,3}(-r[A-Z]{2})?").matches(qualifier) -> qualifier.replace("-r", "-")
                else -> null
            }
        }.map { if (it == "in" || it.startsWith("in-")) "id" + it.removePrefix("in") else it }
        .sorted()

android {
    namespace = "com.jmtube.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jmtube.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"

        buildConfigField("int", "NIGHTLY_RUN", "0")
        buildConfigField("String", "APP_LANGUAGE_TAGS", "\"${(listOf("en") + translatedLocaleTags()).joinToString(",")}\"")
        buildConfigField("String", "LASTFM_API_KEY", "\"${secretValue("lastfmApiKey", "lastfm.apiKey", "LASTFM_API_KEY")}\"")
        buildConfigField("String", "LASTFM_API_SECRET", "\"${secretValue("lastfmApiSecret", "lastfm.apiSecret", "LASTFM_API_SECRET")}\"")
        buildConfigField("Boolean", "UPDATER_ENABLED", "true")
        buildConfigField("String", "DISCORD_APPLICATION_ID", "\"1526515771021328514\"")
        buildConfigField("String", "UPDATE_CHANNEL", "\"stable\"")
        buildConfigField("String", "FLAVOR", "\"jmtube\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
            storeFile = file(keystorePath)
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = "upload"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isCrunchPngs = false
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val releaseKeystore =
                try {
                    signingConfigs.getByName("release").storeFile
                } catch (e: Exception) {
                    null
                }
            if (releaseKeystore?.exists() == true) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                signingConfig = signingConfigs.getByName("debugConfig")
            }
        }
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        resources {
            excludes +=
                listOf(
                    "/META-INF/{AL2.0,LGPL2.1}",
                    "/META-INF/INDEX.LIST",
                    "/META-INF/DEPENDENCIES",
                    "/META-INF/*.version",
                )
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

androidComponents {
    onVariants { variant ->
        val versionName = android.defaultConfig.versionName ?: "1.0.0"
        val buildType = variant.buildType ?: "release"
        variant.outputs.forEach { output ->
            if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                output.outputFileName.set("JM-Tube-v${versionName}-${buildType}.apk")
            }
        }
    }
}

configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
    exclude(group = "org.conscrypt", module = "conscrypt-android")
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // --- Core Android ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)

    // --- Compose (Using BOM is best practice) ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // --- Navigation ---
    implementation(libs.androidx.navigation.compose)

    // --- Lifecycle & Architecture ---
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.window)
    implementation(libs.androidx.window.core)
    implementation(libs.androidx.material3.adaptive.layout)
    implementation(libs.androidx.material3.adaptive.navigation)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)

    // --- Layouts ---
    implementation(libs.androidx.constraintlayout.compose)

    // --- Image Loading ---
    implementation(libs.coil.compose)
    implementation(libs.compose.reorderable)
    implementation(libs.coil.video)
    implementation(libs.coil.network.okhttp)
    implementation("androidx.palette:palette-ktx:1.0.0")

    // --- Dependency Injection ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // --- Data & Network ---
    implementation(libs.newpipe.extractor)
    implementation(libs.okhttp)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.encoding)

    // --- Device Sync ---
    implementation(libs.ktor.server.core) {
        exclude(group = "org.fusesource.jansi", module = "jansi")
    }
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)
    implementation(libs.zxing.core)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Serialization & JSON
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gson)
    implementation(libs.conscrypt.android)
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.kotlin.bip39)

    // --- Media Playback ---
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.muxer)
    implementation(libs.androidx.media3.inspector)
    implementation(libs.androidx.media)

    // --- Database & Storage ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // --- Home-screen widgets (Jetpack Glance) ---
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.graphics.shapes)

    // --- Async & Utils ---
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.brotli)
    implementation(libs.re2j)

    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.tracing)

    // Desugaring for older Android versions
    coreLibraryDesugaring(libs.desugar.jdk.libs.nio)

    // --- Testing ---
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(libs.conscrypt.openjdk.uber)
    testImplementation(libs.kxml2)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.icu4j)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.glance.appwidget.testing)
}
