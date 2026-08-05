plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.gearloop.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gearloop.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        vectorDrawables { useSupportLibrary = true }
    }

    // Signed with the debug key on purpose. This is a demonstration build, not a
    // Play submission — the point is that `assembleRelease` produces an APK that
    // installs, so the optimised build is the one that gets shown.
    signingConfigs {
        getByName("debug") {
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
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

    // The screens are a design deliverable and are judged by eye. The two things
    // that are not — the fit derivation and the search predicate — are pure
    // functions with real logic in them, so they get tested rather than trusted.
    testImplementation(libs.junit)

    // The generated startup profile, checked in and packaged with the release.
    baselineProfile(project(":baselineprofile"))
}
