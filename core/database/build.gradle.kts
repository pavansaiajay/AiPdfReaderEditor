plugins {
    id("aipdf.android.room")
    id("aipdf.android.hilt")
}

android {
    namespace = "pavansaiajayx.aipdfreadereditor.core.database"
}

dependencies {
    implementation(project(":core:model"))
}
