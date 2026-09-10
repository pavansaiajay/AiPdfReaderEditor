plugins {
    id("aipdf.android.feature")
}

android {
    namespace = "pavansaiajayx.aipdfreadereditor.feature.chat"
}

dependencies {
    implementation(project(":core:ai"))
    implementation(project(":core:pdf"))
}
