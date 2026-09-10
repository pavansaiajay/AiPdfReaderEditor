plugins {
    id("aipdf.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "pavansaiajayx.aipdfreadereditor.core.navigation"
}

dependencies {
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
}
