import java.util.Properties

plugins {
    id("aipdf.android.application")
    id("aipdf.android.hilt")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "pavansaiajayx.aipdfreadereditor.app"

    defaultConfig {
        applicationId = "pavansaiajayx.aipdfreadereditor.app"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = Properties().apply {
            val propFile = rootProject.file("local.properties")
            if (propFile.exists()) {
                propFile.inputStream().use { load(it) }
            }
        }
        val admobAppId = localProps.getProperty("ADMOB_APP_ID")
            ?: System.getenv("ADMOB_APP_ID")
            ?: "ca-app-pub-3940256099942544~3347511713"
        manifestPlaceholders["admobAppId"] = admobAppId
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.10")
    }
}

dependencies {
    // Feature Modules
    implementation(project(":feature:home"))
    implementation(project(":feature:viewer"))
    implementation(project(":feature:tools"))
    implementation(project(":feature:chat"))
    implementation(project(":feature:onboarding"))

    // Core Modules
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))

    // Core Android / UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Automatically provision a safe headless mock if google-services.json is absent
val googleServicesFile = layout.projectDirectory.file("google-services.json").asFile
if (!googleServicesFile.exists()) {
    googleServicesFile.writeText(
        """
        {
          "project_info": {
            "project_number": "123456789012",
            "project_id": "aipdfreadereditor",
            "storage_bucket": "aipdfreadereditor.appspot.com"
          },
          "client": [
            {
              "client_info": {
                "mobilesdk_app_id": "1:123456789012:android:abcdef1234567890",
                "android_client_info": {
                  "package_name": "pavansaiajayx.aipdfreadereditor.app"
                }
              },
              "oauth_client": [],
              "api_key": [
                {
                  "current_key": "MOCK_KEY_FOR_LOCAL_DEV_ONLY"
                }
              ],
              "services": {
                "analytics_service": {
                  "status": 1
                }
              }
            }
          ],
          "configuration_version": "1"
        }
        """.trimIndent()
    )
}


