plugins {
    id("aipdf.android.feature")
}

android {
    namespace = "pavansaiajayx.aipdfreadereditor.feature.chat"
}

dependencies {
    implementation(project(":core:ai"))
    implementation(project(":core:pdf"))
    implementation(project(":core:datastore"))
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.datastore.preferences)
}
