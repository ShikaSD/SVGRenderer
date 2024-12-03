plugins {
    alias(libs.plugins.kotlinMultiplatform)
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
