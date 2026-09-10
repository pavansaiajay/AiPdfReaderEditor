import java.util.Properties

plugins {
    id("aipdf.android.library")
    id("aipdf.android.hilt")
}

android {
    namespace = "pavansaiajayx.aipdfreadereditor.core.ai"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        val localProps = Properties().apply {
            val propFile = rootProject.file("local.properties")
            if (propFile.exists()) {
                propFile.inputStream().use { load(it) }
            }
        }
        val rawKey = localProps.getProperty("GEMINI_API_KEY")
            ?: System.getenv("GEMINI_API_KEY")
            ?: ""
        val cleanKey = rawKey.trim('"', '\'')
        buildConfigField("String", "GEMINI_API_KEY", "\"$cleanKey\"")
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    implementation(libs.mlkit.document.scanner)
    implementation(libs.mlkit.text.recognition)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.datastore.preferences)
}
