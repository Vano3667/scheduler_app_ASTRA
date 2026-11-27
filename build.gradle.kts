plugins {
    id("com.android.application") version "9.1.1" apply false
    // AGP 9 uses built-in Kotlin. Match the Compose compiler to AGP's KGP.
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.3.10" apply false
}
