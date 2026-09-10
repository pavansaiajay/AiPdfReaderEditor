plugins {
    id("aipdf.android.library.compose")
}

android {
    namespace = "pavansaiajayx.aipdfreadereditor.core.designsystem"
}

dependencies {
    implementation(project(":core:common"))
    testImplementation(libs.junit)
}
