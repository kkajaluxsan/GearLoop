import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.ksp)
}

// Room writes its schema out as JSON on every build. Checked in, so the report's
// data model can be read off the same file the database was generated from, and
// a future migration has a version to be written against.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// The upload key lives outside the repository. `keystore.properties` is gitignored
// and names a .jks that never gets committed, so a clone has no credentials in it
// — and the release build still has to work on a clone, which is why every use of
// this is a null check rather than a `require`.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.gearloop.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gearloop.app"
        minSdk = 26
        targetSdk = 36
        // versionCode is what Play orders uploads by and can never be reused;
        // versionName is what a person reads. 2 / 1.1.0 was the first store-ready
        // build; internal testing found a dark-mode contrast fault in it (QA-08),
        // fixed here as 3 / 1.1.1.
        // 4 / 2.0.0 is Milestone 03: the same screens, now backed by a Room
        // database, so every interface creates, reads, updates or deletes
        // something real instead of rendering a fixed dataset.
        versionCode = 4
        versionName = "2.0.0"
        vectorDrawables { useSupportLibrary = true }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // The demo path. Still here, and still the fallback, because the point of
        // `assembleRelease` on this project has always been that the optimised APK
        // installs on any machine that clones the repo.
        getByName("debug") {
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // The upload key, for the AAB that goes to Play. Declared only when
        // keystore.properties is present, so a clone without it configures cleanly
        // instead of failing at configuration time with a missing-file error.
        if (keystoreProperties.getProperty("storeFile") != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                // No v1 (JAR) signature: every device from API 24 verifies v2,
                // and minSdk is 26, so v1 would only add bytes. v3 is what lets
                // the key be rotated later on API 28+ without a new package.
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            // Upload key when the machine has it, debug key when it doesn't. The
            // build log says which, because a build signed with the wrong key is
            // rejected by Play rather than by Gradle.
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
            // The APK is mostly fonts and photographs, but R8 still strips the
            // preview composables and the unused half of the icon set, and
            // resource shrinking drops whatever imagery a build doesn't reach.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            // Keeps the demo build launching straight from Studio.
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // buildConfig carries DEBUG, which gates the catalogue↔nav-graph check.
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Milestone 03 data layer: Room over SQLite, observed as Flows, held by
    // ViewModels. No network, no DI framework — one container in the
    // Application wires it, which is all an app of this size needs.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // The pure logic — fit, pricing, availability, search — is tested on the
    // JVM. The database and the screens are tested on a device, because that is
    // where Room's SQLite and Compose's semantics tree actually exist.
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)

    // The generated startup profile, checked in and packaged with the release.
    baselineProfile(project(":baselineprofile"))
}
