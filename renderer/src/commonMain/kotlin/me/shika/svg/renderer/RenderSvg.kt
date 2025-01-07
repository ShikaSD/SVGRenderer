package me.shika.svg.renderer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

@Composable
fun RenderSvg(document: SvgDocument, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        // todo handle viewBox offsets
        val width = document.width ?: size.width
        val height = document.height ?: size.height
        val viewBoxWidth = document.viewBox?.width ?: width
        val viewBoxHeight = document.viewBox?.height ?: height

        val context = DrawContext(document.definitions, width, height)
        withTransform({
            val scaleX = width / viewBoxWidth
            val scaleY = height / viewBoxHeight
            scale(scaleX, scaleY, pivot = Offset.Zero)
        }) {
            for (child in document.children) {
                drawElement(child, context)
            }
        }
    }
}

class DrawContext(
    definitions: List<SvgDefinition>,
    width: Float,
    height: Float
) {
    var svgDrawContext: SvgDrawContext = SvgDrawContext(
        definitions = definitions,
        width = width,
        height = height
    )
}

class SvgDrawContext(
    val fill: ColorDef? = null,
    val stroke: ColorDef? = null,
    val strokeWidth: Float = 1f,
    val strokeLineCap: StrokeLineCap = StrokeLineCap.Butt,
    val strokeLineJoin: StrokeLineJoin = StrokeLineJoin.Miter,
    val fillRule: FillRule = FillRule.NonZero,
    val definitions: List<SvgDefinition>,
    val width: Float,
    val height: Float,
) {
    fun merge(other: SvgGraphicsElement): SvgDrawContext {
        return SvgDrawContext(
            fill = other.fill ?: fill,
            stroke = other.stroke ?: stroke,
            strokeWidth = other.strokeWidth ?: strokeWidth,
            strokeLineCap = other.strokeLineCap ?: strokeLineCap,
            strokeLineJoin = other.strokeLineJoin ?: strokeLineJoin,
            fillRule = other.fillRule ?: fillRule,
            definitions = definitions,
            width = width,
            height = height,
        )
    }
}

private inline fun DrawContext.mutate(
    mutate: (SvgDrawContext) -> SvgDrawContext,
    block: () -> Unit,
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
                context.mutate({ it.merge(e.graphics) }) {
                    for (child in e.children) {
                        drawElement(child, context)
                    }
                }
            }
        }

        is Path -> {
            context.mutate({ it.merge(e.graphics) }) {
                drawPath(e, context.svgDrawContext)
            }
        }

        is Rect -> {
            context.mutate({ it.merge(e.graphics) }) {
                drawRect(context, e)
            }
        }

        is Circle -> {
            context.mutate({ it.merge(e.graphics) }) {
                drawCircle(context, e)
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
    graphicsPath.fillType = when (context.fillRule) {
        FillRule.EvenOdd -> PathFillType.EvenOdd
        FillRule.NonZero -> PathFillType.NonZero
    }
    if (context.fill != null) {
        drawPath(graphicsPath, brush = context.fill.toBrush(context))
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
            brush = context.stroke.toBrush(context),
            style = Stroke(context.strokeWidth, cap = strokeLineCap, join = strokeLineJoin)
        )
    }
}

private fun DrawScope.drawCircle(
    context: DrawContext,
    e: Circle,
) {
    val fill = context.svgDrawContext.fill
    if (fill != null) {
        drawCircle(
            center = Offset(e.cx, e.cy),
            radius = e.r,
            brush = fill.toBrush(context.svgDrawContext)
        )
    }
    val stroke = context.svgDrawContext.stroke
    if (stroke != null) {
        drawCircle(
            center = Offset(e.cx, e.cy),
            radius = e.r,
            brush = stroke.toBrush(context.svgDrawContext),
            style = Stroke(context.svgDrawContext.strokeWidth)
        )
    }
}


private fun DrawScope.drawRect(
    context: DrawContext,
    e: Rect,
) {
    val fill = context.svgDrawContext.fill
    if (fill != null) {
        drawRoundRect(
            topLeft = Offset(e.x, e.y),
            cornerRadius = CornerRadius(e.rx, e.ry),
            size = Size(e.width, e.height),
            brush = fill.toBrush(context.svgDrawContext),
        )
    }
    val stroke = context.svgDrawContext.stroke
    if (stroke != null) {
        drawRoundRect(
            topLeft = Offset(e.x, e.y),
            cornerRadius = CornerRadius(e.rx, e.ry),
            size = Size(e.width, e.height),
            brush = stroke.toBrush(context.svgDrawContext),
            style = Stroke(context.svgDrawContext.strokeWidth)
        )
    }
}

private fun ColorDef.toBrush(context: SvgDrawContext): Brush {
    return when (this) {
        is ColorValue -> SolidColor(value)
        is ColorReference -> toBrush(context)
    }
}

private fun ColorReference.toBrush(context: SvgDrawContext): Brush {
    val definition = context.definitions.find { it.id == id }
    return when (definition) {
        is SvgLinearGradient -> {
            val x1 = definition.x1 ?: 0f
            val y1 = definition.y1 ?: 0f
            val x2 = definition.x2 ?: context.width
            val y2 = definition.y2 ?: 0f
            val start = Offset(x1, y1)
            val end = Offset(x2, y2)
            Brush.linearGradient(
                colorStops = definition.stops.map {
                    Pair(it.offset, it.color)
                }.toTypedArray(),
                start = start,
                end = end,
                tileMode = when (definition.spreadMethod) {
                    SpreadMethod.Pad -> androidx.compose.ui.graphics.TileMode.Clamp
                    SpreadMethod.Reflect -> androidx.compose.ui.graphics.TileMode.Mirror
                    SpreadMethod.Repeat -> androidx.compose.ui.graphics.TileMode.Repeated
                }
            )
        }

        null -> {
            error("Could not find definition with id #$id in ${context.definitions}")
        }
    }
}
