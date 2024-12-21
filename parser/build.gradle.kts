plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.hotReload)
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
