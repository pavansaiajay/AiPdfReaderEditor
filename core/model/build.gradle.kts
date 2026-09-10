plugins {
    id("aipdf.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "pavansaiajayx.aipdfreadereditor.core.model"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
