plugins {
    id("aipdf.android.library")
    id("aipdf.android.hilt")
}

android {
    namespace = "pavansaiajayx.aipdfreadereditor.core.datastore"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

