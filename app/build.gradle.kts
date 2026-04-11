import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
//    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.jetbrains.serialization)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.ksp)
    alias(libs.plugins.secrets.gradle)
    alias(libs.plugins.room)
}

val keystorePropertiesFile: File = rootProject.file("app/keystores/keystore.properties")
val keystoreProperties: Properties? = if (keystorePropertiesFile.exists()) {
    Properties().apply {
        load(FileInputStream(keystorePropertiesFile))
    }
} else {
    null
}

// Add PostHog API key and host as build-time variables
val posthogApiKey: String = project.findProperty("POSTHOG_API_KEY") as String? ?: System.getenv("POSTHOG_API_KEY") ?: ""
val posthogHost: String = project.findProperty("POSTHOG_HOST") as String? ?: System.getenv("POSTHOG_HOST") ?: "https://us.i.posthog.com"

room {
    schemaDirectory("$projectDir/schemas")
}

extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = "app.gamegrub"
    compileSdk = 36

    // https://developer.android.com/ndk/downloads
//    ndkVersion = "22.1.7171670"
    ndkVersion = "28.2.13676358"

    signingConfigs {
        create("gamegrub") {
            if (keystoreProperties != null) {
                storeFile = file(keystoreProperties["storeFile"].toString())
                storePassword = keystoreProperties["storePassword"].toString()
                keyAlias = keystoreProperties["keyAlias"].toString()
                keyPassword = keystoreProperties["keyPassword"].toString()
            }
        }
    }

    defaultConfig {
        applicationId = "app.gamegrub"

        minSdk = 33
        targetSdk = 36

        versionCode = 1
        versionName = "0.8.1"

        buildConfigField("boolean", "GOLD", "false")
        fun secret(name: String) =
            project.findProperty(name) as String? ?: System.getenv(name) ?: ""

        buildConfigField("String", "POSTHOG_API_KEY", "\"${secret("POSTHOG_API_KEY")}\"")
        buildConfigField("String", "POSTHOG_HOST", "\"${secret("POSTHOG_HOST")}\"")
        buildConfigField("String", "STEAMGRIDDB_API_KEY", "\"${secret("STEAMGRIDDB_API_KEY")}\"")
        buildConfigField("String", "CLOUD_PROJECT_NUMBER", "\"${secret("CLOUD_PROJECT_NUMBER")}\"")
        val iconValue = "@mipmap/ic_launcher"
        val iconRoundValue = "@mipmap/ic_launcher_round"
        manifestPlaceholders.putAll(
            mapOf(
                "icon" to iconValue,
                "roundIcon" to iconRoundValue,
            ),
        )

        ndk {
//            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
            abiFilters.addAll(listOf("arm64-v8a"))
        }

        externalNativeBuild {
            cmake {
                targets(
                    "virglrenderer",
                    "patchelf",
                    "extras",
                    "dummyvk",
                    "evshim",
                    "hook_impl",
                    "main_hook",
                    "winlator",
                    "winlator_11",
                    // Third-party source builds for 16 KB page-size compliance.
                    // See third_party/libsndfile/ and third_party/libltdl/.
                    "sndfile",
                    "ltdl",
                )
            }
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            // getDefaultProguardFile("proguard-android.txt"),
            "proguard-rules.pro",
        )
    }

    androidResources {
        // Localization support - specify which languages to include
        localeFilters += listOf(
            "en", // English (default)
            "es", // Spanish
            "da", // Danish
            "pt-rBR", // Portuguese (Brazilian)
            "zh-rTW", // Traditional Chinese
            "zh-rCN", // Simplified Chinese
            "fr", // French
            "de", // German
            "uk", // Ukrainian
            "it", // Italian
            "ro", // Română
            "pl", // Polish
            "ru", // Russian
            "ko", // Korean
            // TODO: Add more languages here using the ISO 639-1 locale code with regional qualifiers (e.g., "pt-rPT" for European Portuguese)
        )
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
        }
        create("release-signed") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("gamegrub")
        }
        create("release-gold") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("gamegrub")
            applicationIdSuffix = ".gold"
            buildConfigField("boolean", "GOLD", "true")
            val iconValue = "@mipmap/ic_launcher_gold"
            val iconRoundValue = "@mipmap/ic_launcher_gold_round"
            manifestPlaceholders.putAll(
                mapOf(
                    "icon" to iconValue,
                    "roundIcon" to iconRoundValue,
                ),
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

    packaging {
        resources {
            excludes += "/DebugProbesKt.bin"
            excludes += "/junit/runner/smalllogo.gif"
            excludes += "/junit/runner/logo.gif"
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
        jniLibs {
            // 'extractNativeLibs' was not enough to keep the jniLibs and
            // the libs went missing after adding on-demand feature delivery
            useLegacyPackaging = true
            // Keep checked-in prebuilts as fallback, but package source-built arm64 outputs from CMake.
            excludes += "arm64-v8a/libdummyvk.so"
            excludes += "arm64-v8a/libextras.so"
            excludes += "arm64-v8a/libhook_impl.so"
            excludes += "arm64-v8a/libmain_hook.so"
            excludes += "arm64-v8a/libpatchelf.so"
            excludes += "arm64-v8a/libvirglrenderer.so"
            excludes += "arm64-v8a/libwinlator.so"
            // libsndfile and libltdl are now source-built via CMake (third_party/).
            // Exclude prebuilts so only the NDK r28+ 16 KB-aligned outputs are packaged.
            excludes += "arm64-v8a/libsndfile.so"
            excludes += "arm64-v8a/libltdl.so"
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    dynamicFeatures += setOf(":ubuntufs")

    // build extras needed in libwinlator_bionic.so
    // externalNativeBuild {
    //     cmake {
    //         path = file("src/main/cpp/extras/CMakeLists.txt")   // the file shown above
    //         version = "3.22.1"
    //     }
    // }

    // Build source-owned arm64 native libs with NDK r28+ for 16 KB page-size compliance.
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // (For now) Uncomment for LeakCanary to work.
    // configurations {
    //     debugImplementation {
    //         exclude(group = "junit", module = "junit")
    //     }
    // }
}

tasks.register<Exec>("verifyDebug16KbPageSize") {
    group = "verification"
    description = "Verifies arm64-v8a ELF LOAD alignment and APK zip alignment for debug build output"
    dependsOn(":app:assembleDebug")
    commandLine(
        "bash",
        rootProject.file("scripts/verify-16kb-page-size.sh").absolutePath,
        rootProject.file("app/build/outputs/apk/debug/app-debug.apk").absolutePath,
    )
}

// kotlin {
//    compilerOptions {
//        jvmTarget = JvmTarget.JVM_17
//    }
// }

kotlinter {
    ignoreFormatFailures = false
}

dependencies {
    implementation(libs.material)

    // Chrome Custom Tabs for GOG OAuth
    implementation(libs.androidx.browser)

    // JavaSteam
    val localBuild = false // Change to 'true' needed when building JavaSteam manually
    if (localBuild) {
        implementation(files("../../JavaSteam/build/libs/javasteam-1.8.0-12-SNAPSHOT.jar"))
        implementation(files("../../JavaSteam/javasteam-depotdownloader/build/libs/javasteam-depotdownloader-1.8.0-12-SNAPSHOT.jar"))
        implementation(libs.bundles.javasteam.dev)
    } else {
        implementation(libs.javasteam) {
            isChanging = version?.contains("SNAPSHOT") ?: false
        }
        implementation(libs.javasteam.depotdownloader) {
            isChanging = version?.contains("SNAPSHOT") ?: false
        }
    }
    implementation(libs.spongycastle)
    implementation(libs.okhttp.dnsoverhttps)

    // Split Modules
    implementation(libs.bundles.google)

    // Winlator
    implementation(libs.bundles.winlator)
    implementation(libs.zstd.jni) { artifact { type = "aar" } }
    implementation(libs.xz)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.landscapist.coil)
    debugImplementation(libs.androidx.ui.tooling)

    // Support
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.apng)
    implementation(libs.datastore.preferences)
    implementation(libs.jetbrains.kotlinx.json)
    implementation(libs.kotlin.coroutines)
    implementation(libs.timber)
    implementation(libs.zxing)

    // Google Protobufs
    implementation(libs.protobuf.java)

    // Hilt
    implementation(libs.bundles.hilt)

    // KSP (Hilt, Room)
    ksp(libs.bundles.ksp)

    // Room Database
    implementation(libs.bundles.room)

    // Memory Leak Detection
    // debugImplementation("com.squareup.leakcanary:leakcanary-android:3.0-alpha-8")

    // Testing
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.zstd.jni)
    testImplementation(libs.orgJson)
    testImplementation(libs.mockwebserver)

    // Add PostHog Android SDK dependency
    implementation(libs.posthog.android)

    implementation(libs.jwtdecode)
}
