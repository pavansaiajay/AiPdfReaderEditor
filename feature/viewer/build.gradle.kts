plugins {
    id("aipdf.android.feature")
}

android {
    namespace = "pavansaiajayx.aipdfreadereditor.feature.viewer"
}

dependencies {
    implementation(project(":core:pdf"))
    implementation(project(":core:ai"))
    implementation(project(":core:database"))
}
