plugins {
    id("aipdf.android.library")
}

android {
    namespace = "pavansaiajayx.aipdfreadereditor.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
