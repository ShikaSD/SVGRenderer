package me.shika.svg

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(svg: String) {
        val document = remember {
            val tokens = tokenize(svg)
            val parserState = ParserState(tokens, svg)
            val parsedRoot = parserState.parseNextElement() as ParsedTag
            convertToDocument(parsedRoot)
        }
        Canvas(Modifier.fillMaxSize()) {
            withTransform({
                val scaleX = size.width / document.viewBox.width
                val scaleY = size.height / document.viewBox.height
                scale(scaleX, scaleY, pivot = Offset.Zero)
            }) {
                for (child in document.children) {
                    drawElement(child, SvgDrawContext())
                }
            }
        }
}

class SvgDrawContext(var strokeLineCap: StrokeLineCap = StrokeLineCap.Butt) {
    inline fun withStrokeLineCap(new: StrokeLineCap?, block: () -> Unit) {
        val old = strokeLineCap
        strokeLineCap = new ?: old
        block()
        strokeLineCap = old
    }
}
fun DrawScope.drawElement(e: SvgElement, context: SvgDrawContext) {
    when (e) {
        is Group -> {
            withTransform({ transform(e.transform) }) {
                context.withStrokeLineCap(e.strokeLineCap) {
                    for (child in e.children) {
                        drawElement(child, context)
                    }
                }
            }
        }
        is Path -> {
            val graphicsPath = androidx.compose.ui.graphics.Path()
            for (pathEl in e.data) {
                when (pathEl) {
                    is PathElement.MoveTo -> {
                        graphicsPath.moveTo(pathEl.x, pathEl.y)
                    }

                    is PathElement.CurveTo -> {
                        graphicsPath.cubicTo(pathEl.x1, pathEl.y1, pathEl.x2, pathEl.y2, pathEl.x, pathEl.y)
                    }

                    is PathElement.LineTo -> {
                        graphicsPath.lineTo(pathEl.x, pathEl.y)
                    }

                    PathElement.Close -> {
                        graphicsPath.close()
                    }
                }
            }
            if (e.fill != null) {
                drawPath(graphicsPath, color = e.fill!!)
            }
            if (e.stroke != null) {
                val strokeLineCap = when (context.strokeLineCap) {
                    StrokeLineCap.Butt -> StrokeCap.Butt
                    StrokeLineCap.Round -> StrokeCap.Round
                    StrokeLineCap.Square -> StrokeCap.Square
                }
                drawPath(graphicsPath, color = e.stroke!!, style = Stroke(e.strokeWidth, cap = strokeLineCap))
            }
        }
    }
}
