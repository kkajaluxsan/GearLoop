plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.baselineprofile)
}

/**
 * The baseline profile generator.
 *
 * This module ships nothing. It drives the app through its opening screens on a
 * connected device, records which code paths the runtime had to compile, and
 * writes that list into `:app` so the next cold start can skip the interpreter.
 * The gain lands on the first launch — which, in a demonstration, is the only
 * launch anyone watches.
 */
android {
    namespace = "com.gearloop.baselineprofile"
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    defaultConfig {
        // Macrobenchmark needs 28+; profile generation is happiest on 33+.
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Macrobenchmark refuses to run on an emulator by default, because
        // *timing* numbers from one are meaningless. Recording which methods get
        // compiled is not a timing measurement, so the refusal is suppressed
        // here — the profile is valid, even though a benchmark would not be.
        // Generate on a physical device if one is to hand; it samples the real
        // scheduler and produces a slightly better ordering.
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] =
            "EMULATOR,LOW-BATTERY,UNLOCKED,DEBUGGABLE,NOT-PROFILEABLE"
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

baselineProfile {
    // The app is a demo: the run that matters is the first cold start, so the
    // profile is generated from the launch path rather than a long journey.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}

androidComponents {
    onVariants { v ->
        val artifactsLoader = v.artifacts.getBuiltArtifactsLoader()
        v.instrumentationRunnerArguments.put(
            "targetAppId",
            v.testedApks.map { artifactsLoader.load(it)?.applicationId ?: "" },
        )
    }
}
