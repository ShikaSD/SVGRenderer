package me.shika.svg

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.shika.svg.renderer.RenderSvg
import me.shika.svg.renderer.parseSvg
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.net.URI
import java.nio.file.Files

@Composable
@Preview
fun App() {
    val urls = Files.list(java.nio.file.Path.of("/Users/shika/projects/social-app/assets/icons")).map { it.toUri() }.toList()
//    val svgUrls = listOf(
//        URI("https://upload.wikimedia.org/wikipedia/commons/0/02/SVG_logo.svg"),
//        URI("https://upload.wikimedia.org/wikipedia/commons/6/6b/Bitmap_VS_SVG.svg"),
//    )
    Column(Modifier.verticalScroll(rememberScrollState())) {
        urls.sorted().forEach {
            Text(it.toString())
            Box(Modifier.size(128.dp)) {
                val svgText = it.toURL().readText()
                val svg = remember {
                    try {
                        parseSvg(svgText)
                    } catch (e: Exception) {
                        System.err.println("Failed to parse SVG: $it")
                        e.printStackTrace()
                        null
                    }
                }
                if (svg != null) {
                    RenderSvg(svg)
                } else {
                    Box(Modifier.fillMaxSize().background(Color.Red))
                }
            }
        }
    }
}
