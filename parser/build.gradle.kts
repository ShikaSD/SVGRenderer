plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

repositories {
    mavenCentral()
}
kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {

        }
        desktopMain.dependencies {

        }
    }
}
