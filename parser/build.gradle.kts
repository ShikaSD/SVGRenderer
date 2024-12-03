plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("org.jetbrains.compose-hot-reload") version "1.0.0-dev.31.1"
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
