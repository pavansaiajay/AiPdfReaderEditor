plugins {
    id("aipdf.android.feature")
}

android {
    namespace = "pavansaiajayx.aipdfreadereditor.feature.tools"
}

dependencies {
    implementation(project(":core:pdf"))
    implementation(project(":core:database"))
}
