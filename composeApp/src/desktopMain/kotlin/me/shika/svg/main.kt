package me.shika.svg

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.jetbrains.compose.resources.getResourceUri

fun main() = application {
    val sampleSVG = javaClass.classLoader.getResourceAsStream("sample.svg")?.reader()?.readText().orEmpty()
    Window(
        onCloseRequest = ::exitApplication,
        title = "SVGRenderer",
    ) {
        DevelopmentEntryPoint {
            App(sampleSVG)
        }
    }
}
