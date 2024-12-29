

package me.shika.svg

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getResourceUri
import svgrenderer.composeapp.generated.resources.Res

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SVGRenderer",
    ) {
        DevelopmentEntryPoint {
            App()
        }
    }
}
