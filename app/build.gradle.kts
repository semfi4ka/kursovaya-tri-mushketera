import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.kursach"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.kursach"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}

val startLocalBackend = tasks.register<org.gradle.api.tasks.Exec>("startLocalBackend") {
    group = "application"
    description = "Starts the local MySQL database and REST API with Docker Compose."
    workingDir(rootProject.projectDir)
    commandLine(
        "docker", "compose", "up", "--build", "--detach", "--wait", "--wait-timeout", "120", "api"
    )
}

val localProperties = Properties()
rootProject.file("local.properties").inputStream().use { localProperties.load(it) }
val sdkPath = localProperties.getProperty("sdk.dir")
    ?: error("Android SDK path is missing from local.properties")
val adbName = if (System.getProperty("os.name").lowercase().contains("windows")) "adb.exe" else "adb"
val adb = File(sdkPath).resolve("platform-tools").resolve(adbName)

val forwardLocalApiPort = tasks.register<org.gradle.api.tasks.Exec>("forwardLocalApiPort") {
    group = "application"
    description = "Forwards the local REST API port to connected Android devices."
    dependsOn(startLocalBackend)
    commandLine(adb.absolutePath, "reverse", "tcp:8080", "tcp:8080")
}

tasks.named("preBuild").configure {
    dependsOn(forwardLocalApiPort)
}
