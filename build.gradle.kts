buildscript {
    val appVersionName by extra("2.1.3")
    val appVersionCode by extra(213)
}
plugins {
    alias(libs.plugins.com.android.application) apply false
    alias(libs.plugins.org.jetbrains.kotlin.android) apply false
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization) apply false
    alias(libs.plugins.com.google.devtools.ksp) apply false
}