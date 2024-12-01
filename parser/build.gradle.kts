plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

repositories {
    google()
    mavenCentral()
}
kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(libs.androidx.compose.ui.graphics)
            implementation(libs.androidx.compose.ui.util)
        }
        desktopMain.dependencies {
        }
    }
}
