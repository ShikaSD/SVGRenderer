package me.shika.svg.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.runner.AndroidJUnit4
import me.shika.svg.renderer.parseSvg
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SvgBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun benchmarkSvgParse() {
        val svgText = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="40" stroke="black" stroke-width="3" fill="red" />
            </svg>
        """.trimIndent()
        benchmarkRule.measureRepeated {
            val document = parseSvg(svgText)
        }
    }

    @Test
    fun benchmarkSvgRender() {
        // TODO: Implement benchmark
    }
}
