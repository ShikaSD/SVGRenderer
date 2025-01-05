package me.shika.svg

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.net.URI
import java.nio.file.Files

@OptIn(ExperimentalLayoutApi::class)
@Composable
@Preview
fun App() {
    val urls = Files.list(java.nio.file.Path.of("/Users/shika/projects/social-app/assets/icons")).map { it.toUri() }.toList()
    val svgUrls = listOf(
        URI("https://upload.wikimedia.org/wikipedia/commons/0/02/SVG_logo.svg"),
        URI("https://upload.wikimedia.org/wikipedia/commons/6/6b/Bitmap_VS_SVG.svg"),
    )
    Column(Modifier.verticalScroll(rememberScrollState())) {
        svgUrls.sorted().forEach {
            Text(it.toString())
            Box(Modifier.size(128.dp)) {
                RenderSvg(it)
            }
        }
    }
}

@Composable
fun RenderSvg(uri: URI) {
    val svg = uri.toURL().readText()
    val document = remember {
        try {
            val tokens = tokenize(svg)
            val parserState = ParserState(tokens, svg)
            val parsedRoot = parserState.parseNextElement() as ParsedTag
            convertToDocument(parsedRoot)
        } catch (e: Exception) {
            println("Failed to parse SVG: $uri")
            e.printStackTrace()
            null
        }
    }
    if (document != null) {
        Canvas(Modifier.fillMaxSize()) {
            val context = DrawContext()
            withTransform({
                val scaleX = size.width / document.viewBox.width
                val scaleY = size.height / document.viewBox.height
                scale(scaleX, scaleY, pivot = Offset.Zero)
            }) {
                for (child in document.children) {
                    drawElement(child, context)
                }
            }
        }
    } else {
        Box(Modifier.fillMaxSize().background(Color.Red))
    }
}

class DrawContext(
    var svgDrawContext: SvgDrawContext = SvgDrawContext()
)

data class SvgDrawContext(
    val fill: Color? = null,
    val stroke: Color? = null,
    val strokeLineCap: StrokeLineCap = StrokeLineCap.Butt,
    val strokeLineJoin: StrokeLineJoin = StrokeLineJoin.Miter
)

private inline fun DrawContext.mutate(
    mutate: (SvgDrawContext) -> SvgDrawContext,
    block: () -> Unit
) {
    val oldContext = svgDrawContext
    svgDrawContext = mutate(oldContext)
    block()
    svgDrawContext = oldContext
}

fun DrawScope.drawElement(e: SvgElement, context: DrawContext) {
    when (e) {
        is Group -> {
            withTransform({ transform(e.transform) }) {
                context.mutate({
                    it.copy(
                        fill = e.fill ?: context.svgDrawContext.fill,
                        stroke = e.stroke ?: context.svgDrawContext.stroke,
                        strokeLineCap = e.strokeLineCap ?: context.svgDrawContext.strokeLineCap,
                        strokeLineJoin = e.strokeLineJoin ?: context.svgDrawContext.strokeLineJoin
                    )
                }) {
                    for (child in e.children) {
                        drawElement(child, context)
                    }
                }
            }
        }
        is Path -> {
            context.mutate({
                it.copy(
                    fill = e.fill ?: context.svgDrawContext.fill,
                    stroke = e.stroke ?: context.svgDrawContext.stroke,
                    strokeLineCap = e.strokeLineCap ?: context.svgDrawContext.strokeLineCap,
                    strokeLineJoin = e.strokeLineJoin ?: context.svgDrawContext.strokeLineJoin
                ) }) {
                drawPath(e, context.svgDrawContext)
            }
        }
    }
}

fun DrawScope.drawPath(e: Path, context: SvgDrawContext) {
    val graphicsPath = androidx.compose.ui.graphics.Path()
    var currentX = 0f
    var currentY = 0f

    var lastControlX = 0f
    var lastControlY = 0f

    var lastElement: PathElement? = null
    for (p in e.data.take(100)) {
        when (p) {
            is PathElement.MoveTo -> {
                val endX = p.x + if (p.relative) currentX else 0f
                val endY = p.y + if (p.relative) currentY else 0f
                graphicsPath.moveTo(endX, endY)
                currentX = endX
                currentY = endY
            }

            is PathElement.CurveTo -> {
                val controlX1: Float
                val controlY1: Float
                if (p.x1.isNaN() && p.y1.isNaN()) {
                    if (lastElement !is PathElement.CurveTo) {
                        lastControlX = currentX
                        lastControlY = currentY
                    }
                    controlX1 = currentX + (currentX - lastControlX)
                    controlY1 = currentY + (currentY - lastControlY)
                } else {
                    controlX1 = p.x1 + if (p.relative) currentX else 0f
                    controlY1 = p.y1 + if (p.relative) currentY else 0f
                }
                val controlX2 = p.x2 + if (p.relative) currentX else 0f
                val controlY2 = p.y2 + if (p.relative) currentY else 0f
                val endX = p.x + if (p.relative) currentX else 0f
                val endY = p.y + if (p.relative) currentY else 0f
                graphicsPath.cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY)
                currentX = endX
                currentY = endY
                lastControlX = controlX2
                lastControlY = controlY2
            }

            is PathElement.QuadTo -> {
                val controlX: Float
                val controlY: Float
                if (p.x1.isNaN() && p.y1.isNaN()) {
                    if (lastElement !is PathElement.QuadTo) {
                        lastControlX = currentX
                        lastControlY = currentY
                    }
                    controlX = currentX + (currentX - lastControlX)
                    controlY = currentY + (currentY - lastControlY)
                } else {
                    controlX = p.x1 + if (p.relative) currentX else 0f
                    controlY = p.y1 + if (p.relative) currentY else 0f
                }
                val endX = p.x + if (p.relative) currentX else 0f
                val endY = p.y + if (p.relative) currentY else 0f
                graphicsPath.quadraticTo(controlX, controlY, endX, endY)
                currentX = endX
                currentY = endY
                lastControlX = controlX
                lastControlY = controlY
            }

            is PathElement.LineTo -> {
                val startX = if (p.x.isNaN()) (if (p.relative) 0f else currentX) else p.x
                val startY = if (p.y.isNaN()) (if (p.relative) 0f else currentY) else p.y
                val endX = startX + if (p.relative) currentX else 0f
                val endY = startY + if (p.relative) currentY else 0f
                graphicsPath.lineTo(endX, endY)
                currentX = endX
                currentY = endY
            }

            is PathElement.ArcTo -> {
                val endX = p.x + if (p.relative) currentX else 0f
                val endY = p.y + if (p.relative) currentY else 0f
                drawArc(
                    graphicsPath,
                    currentX.toDouble(),
                    currentY.toDouble(),
                    endX.toDouble(),
                    endY.toDouble(),
                    p.rx.toDouble(),
                    p.ry.toDouble(),
                    p.rotation.toDouble(),
                    p.largeArc,
                    p.sweep
                )
                currentX = endX
                currentY = endY
            }

            PathElement.Close -> {
                graphicsPath.close()
            }
        }
        lastElement = p
    }
    graphicsPath.fillType = when (e.fillRule) {
        FillRule.EvenOdd -> PathFillType.EvenOdd
        FillRule.NonZero, null -> PathFillType.NonZero
    }
    if (context.fill != null) {
        drawPath(graphicsPath, color = context.fill)
    }
    if (context.stroke != null) {
        val strokeLineCap = when (context.strokeLineCap) {
            StrokeLineCap.Butt -> StrokeCap.Butt
            StrokeLineCap.Round -> StrokeCap.Round
            StrokeLineCap.Square -> StrokeCap.Square
        }
        val strokeLineJoin = when (context.strokeLineJoin) {
            StrokeLineJoin.Bevel -> StrokeJoin.Bevel
            StrokeLineJoin.Round -> StrokeJoin.Round
            StrokeLineJoin.Miter -> StrokeJoin.Miter
        }
        drawPath(
            path = graphicsPath,
            color = context.stroke,
            style = Stroke(e.strokeWidth, cap = strokeLineCap, join = strokeLineJoin)
        )
    }
}
