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

import com.slaviboy.delaunator.Delaunator.PointD

/**
 * Simple class for holding polygon point values as array list with (x,y coordinate pairs) for
 * each point of the polygon.
 * @param coordinates array list with all the coordinates of the points of the polygon
 */
data class Polygon(var coordinates: ArrayList<Double> = ArrayList()) {

    /**
     * Add instruction moveTo for the polygon to a given position
     * @param x x coordinate of the point position
     * @param y y coordinate of the point position
     */
    fun moveTo(x: Double, y: Double) {
        coordinates.add(x)
        coordinates.add(y)
    }

    /**
     * Add instruction lineTo for the polygon to a given position
     * @param x x coordinate of the point position
     * @param y y coordinate of the point position
     */
    fun lineTo(x: Double, y: Double) {
        coordinates.add(x)
        coordinates.add(y)
    }

    /**
     * Close path by adding the fist point to the end of the array list that holds the
     * point for the polygon
     */
    fun closePath() {

        // add again the first point
        if (coordinates.size >= 2) {
            coordinates.add(coordinates[0]) // x
            coordinates.add(coordinates[1]) // y
        }
    }

    /**
     * Get the area of current polygon
     */
    fun area(): Double {
        return Polygon.area(coordinates)
    }

    /**
     * Get the center point of a polygon
     */
    fun center(): PointD {
        return Polygon.center(coordinates)
    }

    /**
     * Check if point is inside current polygon
     * @param x x coordinate of the point
     * @param y y coordinate of the point
     */
    fun contains(x: Double, y: Double): Boolean {
        return Polygon.contains(coordinates, x, y)
    }

    /**
     * Get the perimeter of current polygon
     */
    fun perimeter(): Double {
        return Polygon.perimeter(coordinates)
    }

    /**
     * Get the boundary box of current polygon
     */
    fun bound(): Voronoi.RectD {
        return Polygon.bound(coordinates)
    }

    override fun toString(): String {
        return coordinates.joinToString(",")
    }

    companion object {

        /**
         * Calculate the area of the polygon given by array of (x,y coordinate pairs)
         * for each of the polygon points [x1,y1, x2,y2, x3,y3, ...].
         * @param coordinates array with coordinates of the polygon points
         */
        fun area(coordinates: ArrayList<Double>): Double {

            val n = coordinates.size / 2
            var i = 0
            var aX: Double
            var aY: Double
            var bX = coordinates[n * 2 - 2]
            var bY = coordinates[n * 2 - 1]
            var area = 0.0

            while (i < n) {
                aX = bX
                aY = bY
                bX = coordinates[i * 2]
                bY = coordinates[i * 2 + 1]
                area += (aY * bX - aX * bY)
                i++
            }
            return Math.abs(area / 2.0)
        }

        /**
         * Returns the centroid(center) point of polygon given by array of (x,y coordinate pairs)
         * for each of the polygon points [x1,y1, x2,y2, x3,y3, ...].
         * @param coordinates array with coordinates of the polygon points
         * @param point point object that will receive the center position, that way there is no need to create new PointD object each time
         */
        fun center(coordinates: ArrayList<Double>, point: PointD? = null): PointD {

            if (coordinates.size == 0) {
                return point ?: PointD()
            }

            val n = coordinates.size / 2
            var i = 0
            var x = 0.0
            var y = 0.0
            var aX: Double
            var aY: Double
            var bX = coordinates[n * 2 - 2]
            var bY = coordinates[n * 2 - 1]
            var c: Double
            var k = 0.0

            while (i < n) {
                aX = bX
                aY = bY
                bX = coordinates[i * 2]
                bY = coordinates[i * 2 + 1]
                c = aX * bY - bX * aY
                k += c
                x += (aX + bX) * c
                y += (aY + bY) * c
                i++
            }
            k *= 3.0

            if (point != null) {
                point.x = x / k
                point.y = y / k
            }

            return point ?: PointD(x / k, y / k)
        }

        /**
         * Check whether point with given x,y coordinates is inside a polygon given by array
         * of (x,y coordinate pairs) for each of the polygon points [x1,y1, x2,y2, x3,y3, ...].
         * @param coordinates array with coordinates of the polygon points
         */
        fun contains(coordinates: ArrayList<Double>, x: Double, y: Double): Boolean {

            val n = coordinates.size / 2
            var x0 = coordinates[coordinates.size - 2]
            var y0 = coordinates[coordinates.size - 1]
            var x1: Double
            var y1: Double
            var inside = false

            for (i in 0 until n) {
                x1 = coordinates[i * 2]
                y1 = coordinates[i * 2 + 1]
                if (((y1 > y) != (y0 > y)) && (x < (x0 - x1) * (y - y1) / (y0 - y1) + x1)) {
                    inside = !inside
                }
                x0 = x1
                y0 = y1
            }
            return inside
        }

        /**
         * Calculate the perimeter (outer length) of a polygon given by array of (x,y coordinate pairs)
         * for each of the polygon points [x1,y1, x2,y2, x3,y3, ...].
         * @param coordinates array with coordinates of the polygon points
         */
        fun perimeter(coordinates: ArrayList<Double>): Double {

            var i = -1
            val n = coordinates.size / 2
            var bX = coordinates[n * 2 - 2]
            var bY = coordinates[n * 2 - 1]
            var xa: Double
            var ya: Double
            var xb = bX
            var yb = bY
            var perimeter = 0.0

            while (++i < n) {
                xa = xb
                ya = yb
                bX = coordinates[i * 2]
                bY = coordinates[i * 2 + 1]
                xb = bX
                yb = bY
                xa -= xb
                ya -= yb
                perimeter += Math.sqrt(0.0 + xa * xa + ya * ya)
            }
            return perimeter
        }

        /**
         * Get the bound rectangle of a polygon, and return its top, left, right and bottom position
         * as a RectD object.
         * @param coordinates array with coordinates of the polygon points
         */
        fun bound(coordinates: ArrayList<Double>): Voronoi.RectD {

            var left = Double.MAX_VALUE
            var rights = Double.MIN_VALUE
            var top = Double.MAX_VALUE
            var bottom = Double.MIN_VALUE

            for (i in 0 until coordinates.size / 2) {
                val x = coordinates[i * 2]
                val y = coordinates[i * 2 + 1]

                if (x < left) {
                    left = x
                }
                if (x > rights) {
                    rights = x
                }
                if (y < top) {
                    top = y
                }
                if (y > bottom) {
                    bottom = y
                }
            }

            return Voronoi.RectD(left, top, rights, bottom)
        }
    }
}