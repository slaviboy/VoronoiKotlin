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

/**
 * Class for generating svg path data string, from simple methods. Used to generate svg path data
 * from the delaunay triangulation or the voronoi diagram. To learn more about SVG paths visit:
 * https://www.w3.org/TR/SVG/paths.html
 * @param value initial path data string
 */
data class Path(var value: String = "") {

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
        x0 = x
        y0 = y
        x1 = x
        y1 = y
        value += "M${x},${y}"
    }

    /**
     * Add instruction to generate new line from previous point to a new point
     * @param x x coordinate of the new point
     * @param y y coordinate of the new point
     */
    fun lineTo(x: Double, y: Double) {
        x1 = x
        y1 = y
        value += "L${x},${y}"
    }

    /**
     * Add instruction for closing the path, to the svg path data string
     */
    fun closePath() {
        if (x1 != null) {
            x1 = x0
            y1 = y0
            value += "Z"
        }
    }

    /**
     * Add instruction to generate arc, to the svg path data string
     * @param x center x coordinate of the arc
     * @param y center y coordinate of the arc
     * @param r radius of the arc
     */
    fun arc(x: Double, y: Double, r: Double) {
        val x0 = x + r
        val y0 = y

        if (this.x1 == null) {
            value += "M${x0},${y0}"
        } else if (Math.abs(this.x1!! - x0) > epsilon || Math.abs(this.y1!! - y0) > epsilon) {
            value += "L${x0},${y0}"
        }

        if (r == 0.0) {
            return
        }

        this.x1 = x0
        this.y1 = y0
        value += "A${r},${r},0,1,1,${x - r},${y}A${r},${r},0,1,1,${x0},${y0}"
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
        value += "M${x},${y}h${+w}v${+h}h${-w}Z"
    }

    override fun toString(): String {
        return value
    }
}