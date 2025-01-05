package me.shika.svg.renderer

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import org.junit.Rule
import org.junit.Test

class ScreenshotTests {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    @Test
    fun svgCircle() {
        testSvg(
            """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <circle cx="50" cy="50" r="40" stroke="black" stroke-width="3" fill="red" />
                </svg>
            """
        )
    }

    @Test
    fun svgRect() {
        testSvg(
            """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <rect x="10" y="10" width="80" height="80" stroke="black" stroke-width="3" fill="blue" />
                </svg>
            """
        )
    }

    @Test
    fun svgRountRect() {
        testSvg(
            """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <rect x="10" y="10" rx="10" ry="10" width="80" height="80" stroke="black" stroke-width="3" fill="blue" />
                </svg>
            """
        )
    }

    @Test
    fun svgPath() {
        testSvg(
            """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <path d="M10 10 H 90 V 90 H 10 L 10 10 Z" stroke="black" stroke-width="3" fill="green" />
                </svg>
            """
        )
    }

    private fun testSvg(svg: String) {
        val svg = parseSvg(svg)
        paparazzi.snapshot {
            RenderSvg(svg, Modifier.fillMaxSize().aspectRatio(1f))
        }
    }
}
