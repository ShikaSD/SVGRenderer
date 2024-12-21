package me.shika.svg

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

// Stolen from androidx.compose.ui.graphics.PathParser
internal fun drawArc(
    p: androidx.compose.ui.graphics.Path,
    x0: Double,
    y0: Double,
    x1: Double,
    y1: Double,
    a: Double,
    b: Double,
    theta: Double,
    isMoreThanHalf: Boolean,
    isPositiveArc: Boolean
) {

    /* Convert rotation angle from degrees to radians */
    val thetaD = theta.toRadians()
    /* Pre-compute rotation matrix entries */
    val cosTheta = cos(thetaD)
    val sinTheta = sin(thetaD)
    /* Transform (x0, y0) and (x1, y1) into unit space */
    /* using (inverse) rotation, followed by (inverse) scale */
    val x0p = (x0 * cosTheta + y0 * sinTheta) / a
    val y0p = (-x0 * sinTheta + y0 * cosTheta) / b
    val x1p = (x1 * cosTheta + y1 * sinTheta) / a
    val y1p = (-x1 * sinTheta + y1 * cosTheta) / b

    /* Compute differences and averages */
    val dx = x0p - x1p
    val dy = y0p - y1p
    val xm = (x0p + x1p) / 2
    val ym = (y0p + y1p) / 2
    /* Solve for intersecting unit circles */
    val dsq = dx * dx + dy * dy
    if (dsq == 0.0) {
        return /* Points are coincident */
    }
    val disc = 1.0 / dsq - 1.0 / 4.0
    if (disc < 0.0) {
        val adjust = (sqrt(dsq) / 1.99999).toFloat()
        drawArc(
            p, x0, y0, x1, y1, a * adjust,
            b * adjust, theta, isMoreThanHalf, isPositiveArc
        )
        return /* Points are too far apart */
    }
    val s = sqrt(disc)
    val sdx = s * dx
    val sdy = s * dy
    var cx: Double
    var cy: Double
    if (isMoreThanHalf == isPositiveArc) {
        cx = xm - sdy
        cy = ym + sdx
    } else {
        cx = xm + sdy
        cy = ym - sdx
    }

    val eta0 = atan2(y0p - cy, x0p - cx)

    val eta1 = atan2(y1p - cy, x1p - cx)

    var sweep = eta1 - eta0
    if (isPositiveArc != (sweep >= 0)) {
        if (sweep > 0) {
            sweep -= 2 * PI
        } else {
            sweep += 2 * PI
        }
    }

    cx *= a
    cy *= b
    val tcx = cx
    cx = cx * cosTheta - cy * sinTheta
    cy = tcx * sinTheta + cy * cosTheta

    arcToBezier(
        p, cx, cy, a, b, x0, y0, thetaD,
        eta0, sweep
    )
}

/**
 * Converts an arc to cubic Bezier segments and records them in p.
 *
 * @param p The target for the cubic Bezier segments
 * @param cx The x coordinate center of the ellipse
 * @param cy The y coordinate center of the ellipse
 * @param a The radius of the ellipse in the horizontal direction
 * @param b The radius of the ellipse in the vertical direction
 * @param e1x E(eta1) x coordinate of the starting point of the arc
 * @param e1y E(eta2) y coordinate of the starting point of the arc
 * @param theta The angle that the ellipse bounding rectangle makes with horizontal plane
 * @param start The start angle of the arc on the ellipse
 * @param sweep The angle (positive or negative) of the sweep of the arc on the ellipse
 */
private fun arcToBezier(
    p: androidx.compose.ui.graphics.Path,
    cx: Double,
    cy: Double,
    a: Double,
    b: Double,
    e1x: Double,
    e1y: Double,
    theta: Double,
    start: Double,
    sweep: Double
) {
    var eta1x = e1x
    var eta1y = e1y
    // Taken from equations at: http://spaceroots.org/documents/ellipse/node8.html
    // and http://www.spaceroots.org/documents/ellipse/node22.html

    // Maximum of 45 degrees per cubic Bezier segment
    val numSegments = ceil(abs(sweep * 4 / PI)).toInt()

    var eta1 = start
    val cosTheta = cos(theta)
    val sinTheta = sin(theta)
    val cosEta1 = cos(eta1)
    val sinEta1 = sin(eta1)
    var ep1x = (-a * cosTheta * sinEta1) - (b * sinTheta * cosEta1)
    var ep1y = (-a * sinTheta * sinEta1) + (b * cosTheta * cosEta1)

    val anglePerSegment = sweep / numSegments
    for (i in 0 until numSegments) {
        val eta2 = eta1 + anglePerSegment
        val sinEta2 = sin(eta2)
        val cosEta2 = cos(eta2)
        val e2x = cx + (a * cosTheta * cosEta2) - (b * sinTheta * sinEta2)
        val e2y = cy + (a * sinTheta * cosEta2) + (b * cosTheta * sinEta2)
        val ep2x = (-a * cosTheta * sinEta2) - (b * sinTheta * cosEta2)
        val ep2y = (-a * sinTheta * sinEta2) + (b * cosTheta * cosEta2)
        val tanDiff2 = tan((eta2 - eta1) / 2)
        val alpha = sin(eta2 - eta1) * (sqrt(4 + 3.0 * tanDiff2 * tanDiff2) - 1) / 3
        val q1x = eta1x + alpha * ep1x
        val q1y = eta1y + alpha * ep1y
        val q2x = e2x - alpha * ep2x
        val q2y = e2y - alpha * ep2y

        // TODO (njawad) figure out if this is still necessary?
        // Adding this no-op call to workaround a proguard related issue.
        // p.relativeLineTo(0.0, 0.0)

        p.cubicTo(
            q1x.toFloat(),
            q1y.toFloat(),
            q2x.toFloat(),
            q2y.toFloat(),
            e2x.toFloat(),
            e2y.toFloat()
        )
        eta1 = eta2
        eta1x = e2x
        eta1y = e2y
        ep1x = ep2x
        ep1y = ep2y
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Double.toRadians(): Double = this / 180 * PI
