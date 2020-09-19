/*
* Copyright (C) 2020 Stanislav Georgiev
* https://github.com/slaviboy
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.slaviboy.voronoi

import android.graphics.Color
import  com.slaviboy.voronoi.Voronoi.RectD
import java.util.*

/**
 * Class for generating svg path data string, from simple methods. Used to generate svg path data
 * from the delaunay triangulation or the voronoi diagram. To learn more about SVG paths visit:
 * https://www.w3.org/TR/SVG/paths.html
 * @param data initial path data string
 */
data class Path(var data: String = "", var bound: RectD = RectD(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)) {

    companion object {
        const val epsilon = 1e-6
    }

    var x0: Double? = null
    var y0: Double? = null
    var x1: Double? = null
    var y1: Double? = null

    /**
     * Add instruction to move the point location to a new position
     * @param x x coordinate of the new position
     * @param y y coordinate of the new position
     */
    fun moveTo(x: Double, y: Double) {

        // add moveTo command
        x0 = x
        y0 = y
        x1 = x
        y1 = y
        data += "M${x},${y}"

        // update bound
        setBound(x, y)
    }

    /**
     * Add instruction to generate new line from previous point to a new point
     * @param x x coordinate of the new point
     * @param y y coordinate of the new point
     */
    fun lineTo(x: Double, y: Double) {

        // add lineTo command
        x1 = x
        y1 = y
        data += "L${x},${y}"

        // update bound
        setBound(x, y)
    }

    /**
     * Add instruction for closing the path, to the svg path data string
     */
    fun closePath() {
        if (x1 != null && y1 != null) {
            x1 = x0
            y1 = y0
            data += "Z"
        }
    }

    /**
     * Add instruction to generate arc, to the svg path data string
     * @param x center x coordinate of the arc
     * @param y center y coordinate of the arc
     * @param r radius of the arc
     * @param isLineToCalled if before drawing the arc, lineTo() or moveTo() command is applied
     */
    fun arc(x: Double, y: Double, r: Double, isLineToCalled: Boolean = false) {
        val x0 = x + r
        val y0 = y

        if (!isLineToCalled || (x1 == null && y1 == null)) {
            data += "M${x0},${y0}"
        } else if (x1 != null && y1 != null && Math.abs(x1!! - x0) > epsilon || Math.abs(y1!! - y0) > epsilon) {
            data += "L${x0},${y0}"
        }

        if (r == 0.0) {
            return
        }

        x1 = x0
        y1 = y0
        data += "A${r},${r},0,1,1,${x - r},${y}A${r},${r},0,1,1,${x0},${y0}"

        // update bound
        setBound(x - r, y - r)
        setBound(x + r, y + r)
    }

    /**
     * Add instruction to generate rectangle, to the svg path data string
     * @param x x coordinate of the rectangle, which is in the top-left corner
     * @param y y coordinate of the rectangle, which is in the top-left corner
     * @param w width of the rectangle
     * @param h height of the rectangle
     */
    fun rect(x: Double, y: Double, w: Double, h: Double) {
        x0 = x
        x1 = x
        y0 = y
        y1 = y
        data += "M${x},${y}h${+w}v${+h}h${-w}Z"

        // update bound
        setBound(x, y)
        setBound(x + w, y + h)
    }

    /**
     * Generate the string for the SVG file using the available data and bound.
     * @param strokeColor stroke color for the path
     * @param strokeWidth stroke width for the path
     * @param fillColor fill color for the path
     */
    fun getSVG(
        strokeColor: Int = Color.BLACK, strokeWidth: Double = 1.0,
        fillColor: Int = Color.TRANSPARENT
    ): String {

        val strokeColorHex = getColorString(strokeColor)
        val fillColorHex = getColorString(fillColor)
        return """ 
            <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.1" width="${bound.right + strokeWidth}" height="${bound.bottom + strokeWidth}" >
	            <path fill="$fillColorHex" stroke="$strokeColorHex" stroke-width="$strokeWidth" d="$data" />
            </svg>
         """.trim()
    }

    /**
     * Get color as integer to color represented as hexadecimal string. If the alpha channel for
     * the color is 0, set the string to none.
     * @param color integer representation of the color
     * @param includeAlpha whether to include the alpha channel to the hexadecimal representation
     */
    fun getColorString(color: Int = Color.TRANSPARENT, includeAlpha: Boolean = true): String {
        val a = Color.alpha(color)
        return if (color == Color.TRANSPARENT || a == 0) {
            "none"
        } else {
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            if (includeAlpha) {
                "#" + String.format("%02x%02x%02x%02x", r, g, b, a).toUpperCase(Locale.ROOT)
            } else {
                "#" + String.format("%02x%02x%02x", r, g, b).toUpperCase(Locale.ROOT)
            }
        }
    }

    internal fun setBound(x: Double, y: Double) {

        if (x > bound.right) {
            bound.right = x
        }

        if (x < bound.left) {
            bound.left = x
        }

        if (y > bound.bottom) {
            bound.bottom = y
        }

        if (y < bound.top) {
            bound.top = y
        }
    }

    override fun toString(): String {
        return data
    }
}