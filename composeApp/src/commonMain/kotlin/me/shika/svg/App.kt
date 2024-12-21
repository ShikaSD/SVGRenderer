package me.shika.svg

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathFillType
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
            var currentX = 0f
            var currentY = 0f
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
                        val controlX1 = p.x1 + if (p.relative) currentX else 0f
                        val controlY1 = p.y1 + if (p.relative) currentY else 0f
                        val controlX2 = p.x2 + if (p.relative) currentX else 0f
                        val controlY2 = p.y2 + if (p.relative) currentY else 0f
                        val endX = p.x + if (p.relative) currentX else 0f
                        val endY = p.y + if (p.relative) currentY else 0f
                        graphicsPath.cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY)
                        currentX = endX
                        currentY = endY
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
            }
            graphicsPath.fillType = when (e.fillRule) {
                FillRule.EvenOdd -> PathFillType.EvenOdd
                FillRule.NonZero, null -> PathFillType.NonZero
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
