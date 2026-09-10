plugins {
    id("aipdf.android.library.compose")
}

android {
    namespace = "pavansaiajayx.aipdfreadereditor.core.ui"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
}
