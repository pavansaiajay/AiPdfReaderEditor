plugins {
    id("aipdf.android.library")
    id("aipdf.android.hilt")
}

android {
    namespace = "pavansaiajayx.aipdfreadereditor.core.pdf"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(libs.pdfbox.android)
    implementation(libs.pdf.viewer)
}
