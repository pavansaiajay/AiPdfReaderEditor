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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.documentfile)
    implementation(libs.pdfbox.android)
    implementation(libs.pdf.viewer)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

