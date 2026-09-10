import extensions.libs
import androidx.room3.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "aipdf.android.library")
            apply(plugin = "com.google.devtools.ksp")
            apply(plugin = "androidx.room3")

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                add("implementation", libs.findLibrary("room3-runtime").get())
                add("ksp", libs.findLibrary("room3-compiler").get())
                add("implementation", libs.findLibrary("sqlite-bundled").get())
            }
        }
    }
}
