package com.slaviboy.voronoi

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PathUnitTest {

    @Test
    fun MainTest() {

        val svgPathData = Path()

        val stringMoveTo = "M10.0,231.22"
        svgPathData.moveTo(10.0, 231.22)
        assertThat(svgPathData.data).isEqualTo(stringMoveTo)

        val stringLineTo = "L34.2,42.15"
        svgPathData.lineTo(34.2, 42.15)
        assertThat(svgPathData.data).isEqualTo(stringMoveTo + stringLineTo)

        val stringRect = "M411.82,251.3h743.44v983.32h-743.44Z"
        svgPathData.rect(411.82, 251.3, 743.44, 983.32)
        assertThat(svgPathData.data).isEqualTo(stringMoveTo + stringLineTo + stringRect)

        val stringArc = "L46.55,83.17A23.15,23.15,0,1,1,0.25,83.17A23.15,23.15,0,1,1,46.55,83.17"
        svgPathData.arc(23.4, 83.17, 23.15, true)
        assertThat(svgPathData.data).isEqualTo(stringMoveTo + stringLineTo + stringRect + stringArc)

        val stringClosePath = "Z"
        svgPathData.closePath()
        assertThat(svgPathData.data).isEqualTo(stringMoveTo + stringLineTo + stringRect + stringArc + stringClosePath)

        val pathData = """ 
            <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.1" width="1155.26" height="1234.6200000000001" >
	            <path fill="none" stroke="#000000" d="M10.0,231.22L34.2,42.15M411.82,251.3h743.44v983.32h-743.44ZL46.55,83.17A23.15,23.15,0,1,1,0.25,83.17A23.15,23.15,0,1,1,46.55,83.17Z" />
            </svg>
         """.trim()
        assertThat(svgPathData.getSVG()).isEqualTo(pathData)
    }
}