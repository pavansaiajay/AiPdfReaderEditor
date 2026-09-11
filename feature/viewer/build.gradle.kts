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
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
