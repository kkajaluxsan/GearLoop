# GearLoop release rules.
#
# The app has no reflection, no serialisation and no dynamic class loading, so
# there is very little to keep. Compose ships its own consumer rules and
# Navigation Compose resolves routes as plain strings, not by class name.

# The demo dataset is reached only through Compose, which R8 traces fine — but
# keeping the model shapes makes a stack trace from a release build readable.
-keepclassmembers class com.gearloop.app.data.** {
    <fields>;
}

# Preview composables are debug-time tooling. They are private and unreferenced
# in a release build, so they are removed; this is here to say that is intended
# rather than an oversight.
-assumenosideeffects class androidx.compose.ui.tooling.preview.Preview

# Line numbers survive so a crash in a demo can be traced back to a file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
