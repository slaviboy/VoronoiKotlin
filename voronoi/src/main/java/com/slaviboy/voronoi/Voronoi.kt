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

import com.slaviboy.delaunator.Delaunator
import com.slaviboy.voronoi.Polygon.Companion.center

/**
 * Class for computing the Voronoi diagram of a set of two-dimensional points. It is based on Delaunator,
 * a fast library for computing the Delaunay triangulation using sweep algorithms. The Voronoi diagram is
 * constructed by connecting the circumcenters of adjacent triangles in the Delaunay triangulation.
 * @param delaunay delaunay object holding information about the delaunay triangulation
 * @param bound boundary box coordinate that determines where the clipping will happen
 */
class Voronoi(var delaunay: Delaunay, var bound: RectD = RectD(0.0, 0.0, 960.0, 500.0)) {

    lateinit var vectors: DoubleArray
    lateinit var circumcenters: DoubleArray              // array with the circumcenters of the circle around the delaunay triangle
    internal lateinit var tempPoint: Delaunator.PointD    // temp point object that is reused and holds the centroid of each polygon(cell)

    init {
        if (bound.right.isNaN() || bound.bottom.isNaN() || bound.right < bound.left || bound.bottom < bound.top) {
            throw IllegalArgumentException("Invalid bounds!")
        }
        init()
    }

    internal fun init() {

        tempPoint = Delaunator.PointD()

        val coordinates = delaunay.coordinates
        val hull = delaunay.hull
        val triangles = delaunay.triangles

        // compute circumcenters
        circumcenters = DoubleArray((triangles.size / 3) * 2)
        var j = 0
        var x: Double
        var y: Double
        for (i in 0 until triangles.size / 3) {

            val i1 = triangles[i * 3] * 2
            val i2 = triangles[i * 3 + 1] * 2
            val i3 = triangles[i * 3 + 2] * 2

            val x1 = coordinates[i1]
            val y1 = coordinates[i1 + 1]

            val x2 = coordinates[i2]
            val y2 = coordinates[i2 + 1]

            val x3 = coordinates[i3]
            val y3 = coordinates[i3 + 1]

            val dx = x2 - x1
            val dy = y2 - y1
            val ex = x3 - x1
            val ey = y3 - y1
            val bl = dx * dx + dy * dy
            val cl = ex * ex + ey * ey
            val ab = (dx * ey - dy * ex) * 2

            if (ab.isNaN() || ab == 0.0) {
                // degenerate case (collinear diagram)
                x = ((x1 + x3) / 2.0 - 1e8 * ey)
                y = ((y1 + y3) / 2.0 + 1e8 * ex)
            } else if (Math.abs(ab) < 1e-8) {
                // almost equal points (degenerate triangle)
                x = (x1 + x3) / 2.0
                y = (y1 + y3) / 2.0
            } else {
                val d = 1.0 / ab
                x = x1 + (ey * bl - dy * cl) * d
                y = y1 + (dx * cl - ex * bl) * d
            }
            circumcenters[j * 2] = x
            circumcenters[j * 2 + 1] = y

            j++
        }

        vectors = DoubleArray(delaunay.coordinates.size * 2)

        // Compute exterior cell rays.
        var h = hull[hull.size - 1]
        var p0: Double
        var p1 = h * 4.0
        var x0: Double
        var x1 = coordinates[2 * h]
        var y0: Double
        var y1 = coordinates[2 * h + 1]
        for (i in hull.indices) {
            h = hull[i]
            p0 = p1
            x0 = x1
            y0 = y1
            p1 = h * 4.0
            x1 = coordinates[2 * h]
            y1 = coordinates[2 * h + 1]
            vectors[(p0 + 2).toInt()] = y0 - y1
            vectors[(p1).toInt()] = y0 - y1
            vectors[(p0 + 3).toInt()] = x1 - x0
            vectors[(p1 + 1).toInt()] = x1 - x0
        }
    }

    /**
     * Update the delaunay after change to the coordinates is made to update
     * all other values.
     */
    fun update(): Voronoi {
        delaunay.update()
        init()
        return this
    }

    /**
     * Render the whole voronoi diagram that includes all lines on a given path. It can be draw on Graphic.Path
     * and then drawn on Canvas element or created as voronoi.Path using instructions to generate the svg path data.
     * @param path path object where the triangle will be created
     */
    fun render(path: Any = Path()): Any {
        val halfedges = delaunay.halfEdges
        val inedges = delaunay.inedges
        val hull = delaunay.hull

        if (hull.size <= 1) {
            return path
        }

        for (i in halfedges.indices) {
            val j = halfedges[i]
            if (j < i) {
                continue
            }
            val ti = (Math.floor(i / 3.0) * 2).toInt()
            val tj = (Math.floor(j / 3.0) * 2).toInt()
            val xi = circumcenters[ti]
            val yi = circumcenters[ti + 1]
            val xj = circumcenters[tj]
            val yj = circumcenters[tj + 1]
            renderSegment(xi, yi, xj, yj, path)
        }

        var h0 = 0
        var h1 = hull[hull.size - 1]
        for (element in hull) {
            h0 = h1
            h1 = element
            val t = (Math.floor(inedges[h1] / 3.0) * 2).toInt()
            val x = circumcenters[t]
            val y = circumcenters[(t + 1)]
            val v = h0 * 4
            val p = project(x, y, vectors[v + 2], vectors[v + 3])
            if (p != null) {
                renderSegment(x, y, p.x, p.y, path)
            }
        }

        return path
    }

    /**
     * Render the boundary box as rectangle, for the voronoi diagram. It can be draw on Graphic.Path and then drawn
     * on Canvas element or created as voronoi.Path using instructions to generate the svg path data.
     * @param path path object where the triangle will be created
     */
    fun renderBounds(path: Any = Path()): Any {
        if (path is android.graphics.Path) {
            path.addRect(bound.left.toFloat(), bound.top.toFloat(), bound.right.toFloat(), bound.bottom.toFloat(), android.graphics.Path.Direction.CW)
        } else if (path is Path) {
            path.rect(bound.left, bound.top, bound.width(), bound.height())
        }
        return path
    }

    /**
     * Render particular polygon(cell) from the voronoi diagram given by index. It can be draw on Graphic.Path and then drawn
     * on Canvas element or created as voronoi.Path using instructions to generate the svg path data.
     * @param i index of the cell from the voronoi diagram
     * @param path path object where the triangle will be created
     * @param isPathClosed whether the path should be closed and the first point coordinates should be added at the end of the array list
     */
    fun renderCell(i: Int, path: Any = Path(), isPathClosed: Boolean = true): Any {
        val points = clip(i)

        if (points == null || points.size == 0) {
            return path
        }

        when (path) {
            is android.graphics.Path -> {
                path.moveTo(points[0].toFloat(), points[1].toFloat())
                var n = points.size
                while (points[0] == points[n - 2] && points[1] == points[n - 1] && n > 1) {
                    n -= 2
                }
                var i = 2
                while (i < n) {
                    if (points[i] != points[i - 2] || points[i + 1] != points[i - 1]) {
                        path.lineTo(points[i].toFloat(), points[i + 1].toFloat())
                    }
                    i += 2
                }

                if (isPathClosed) {
                    path.close()
                }
            }
            is Path -> {
                path.bound = RectD(bound)
                path.moveTo(points[0], points[1])
                var n = points.size
                while (points[0] == points[n - 2] && points[1] == points[n - 1] && n > 1) {
                    n -= 2
                }
                var i = 2
                while (i < n) {
                    if (points[i] != points[i - 2] || points[i + 1] != points[i - 1]) {
                        path.lineTo(points[i], points[i + 1])
                    }
                    i += 2
                }

                if (isPathClosed) {
                    path.closePath()
                }
            }
            is Polygon -> {
                path.moveTo(points[0], points[1])
                var n = points.size
                while (points[0] == points[n - 2] && points[1] == points[n - 1] && n > 1) {
                    n -= 2
                }
                var i = 2
                while (i < n) {
                    if (points[i] != points[i - 2] || points[i + 1] != points[i - 1]) {
                        path.lineTo(points[i], points[i + 1])
                    }
                    i += 2
                }

                if (isPathClosed) {
                    path.closePath()
                }
            }
        }

        return path
    }

    /**
     * Render the input delaunay points, using the the initial coordinates. It can be draw on Graphic.Path and then drawn on Canvas
     * element or created as voronoi.Path using instructions to generate the svg path data.
     * @param r radius of the circle that will be draw
     * @param path path object where the circles will be created
     */
    fun renderInputPoints(r: Double = 3.0, path: Any = Path()): Any {
        return delaunay.renderInputPoints(r, path)
    }

    /**
     * Render the polygons(cells) center points. It can be draw on Graphic.Path and then drawn on Canvas
     * element or created as voronoi.Path using instructions to generate the svg path data.
     * @param r radius of the circle that will be draw
     * @param path path object where the circles will be created
     */
    fun renderCenters(r: Double = 3.0, path: Any = Path()): Any {

        val centers = getCellsCenterCoordinates()
        if (path is android.graphics.Path) {
            for (i in 0 until centers.size / 2) {
                val x = centers[i * 2].toFloat()
                val y = centers[i * 2 + 1].toFloat()
                path.moveTo((x + r).toFloat(), y)
                path.addCircle(x, y, r.toFloat(), android.graphics.Path.Direction.CW)
            }
        } else if (path is Path) {
            for (i in 0 until centers.size / 2) {
                val x = centers[i * 2]
                val y = centers[i * 2 + 1]
                path.moveTo(x + r, y)
                path.arc(x, y, r)
            }
        }
        return path
    }

    /**
     * Get the center point as (x,y coordinates pairs) for each polygon(cell) as array list, two double values
     * per center point, as each point has x and y coordinates pairs. Array is in format: [x1,y1,  x2,y2, ...]
     */
    fun getCellsCenterCoordinates(): ArrayList<Double> {
        val coordinates = getCellsCoordinates(false)
        val centers = ArrayList<Double>()
        for (i in coordinates.indices) {
            center(coordinates[i], tempPoint)
            centers.add(tempPoint.x)
            centers.add(tempPoint.y)
        }
        return centers
    }

    /**
     * Get all coordinates for each polygon(cell) as array list. Each point of the polygon has 2 coordinates
     * x and y that means array list is in format: [ [x1,y1,  x2,y2, ...], [x1,y1,  x2,y2, ...], ... ].
     * If path is closed that means the coordinates from the first point are added to the end of the array list.
     * @param isPathClosed whether the path should be closed and the first point coordinates should be added at the end of the array list
     */
    fun getCellsCoordinates(isPathClosed: Boolean = true): ArrayList<ArrayList<Double>> {

        val coordinatesList = ArrayList<ArrayList<Double>>()
        for (i in 0 until delaunay.coordinates.size / 2) {
            val coordinate = clip(i) ?: ArrayList()
            coordinatesList.add(coordinate)
        }

        // if path is closed att the first coordinates of each polygon to the end of there corresponding array list
        if (isPathClosed) {
            for (i in 0 until delaunay.coordinates.size / 2) {
                coordinatesList[i].add(coordinatesList[i][0])
                coordinatesList[i].add(coordinatesList[i][1])
            }
        }
        return coordinatesList
    }

    /**
     * Get the lines (x,y coordinates pairs) for each polygon(cell) as a single array list. Each line
     * is made out of two points and each point has x,y coordinates pair. That give total of four
     * coordinates per line, the array list is in format [x1,y1,x2,y2,  x3,y3,x4,y4, ...]
     */
    fun getLinesCoordinates(): ArrayList<Double> {
        return (render(Polygon()) as Polygon).coordinates
    }

    /**
     * Sequence function for generating all cell coordinates as sequence, that can be converted
     * to list. Each element list is ArrayList<Double> containing the coordinates for the current
     * polygon(cell) at that particular index.
     * @param isPathClosed whether the path should be closed and the first point coordinates should be added at the end of the array list
     */
    fun getCellsCoordinatesSequence(isPathClosed: Boolean = true) = sequence {
        for (i in 0 until delaunay.coordinates.size / 2) {
            val cell = getCellCoordinates(i, isPathClosed)
            if (cell.coordinates.size > 0) {
                cell.index = i
                yield(cell)
            }
        }
    }

    /**
     * Get coordinates for polygon(cell) at given index. If path is closed that means the coordinates
     * from the first point are added to the end of the array list. And then return object that has the
     * coordinates and also holds the index of the polygon.
     * @param i index of the polygon(cell)
     * @param isPathClosed whether the path should be closed and the first point coordinates should be added at the end of the array list
     */
    fun getCellCoordinates(i: Int, isPathClosed: Boolean = true): CellValues {
        val polygon = Polygon()
        renderCell(i, polygon, isPathClosed)
        return CellValues(polygon.coordinates)
    }

    /**
     * Apply Lloyd relaxation for all cells in the current voronoi diagram. The algorithm computes the Voronoi
     * diagram for a set of points, moves each point towards the centroid of its Voronoi region, and repeats for
     * the number of iterations given by the user.
     * @param iterations how many time to do the relaxation algorithm
     */
    fun relax(iterations: Int) {

        // loop for the iterations
        for (t in 0 until iterations) {

            // update coordinates and apply update after each iterations
            for (i in 0 until delaunay.coordinates.size / 2) {

                val coordinate = clip(i) ?: ArrayList()
                center(coordinate, tempPoint)
                delaunay.coordinates[i * 2] = tempPoint.x
                delaunay.coordinates[i * 2 + 1] = tempPoint.y
                update()
            }
        }
    }

    internal fun renderSegment(x0: Double, y0: Double, x1: Double, y1: Double, path: Any) {
        val c0 = regionCode(x0, y0)
        val c1 = regionCode(x1, y1)

        if (path is android.graphics.Path) {
            if (c0 == 0 && c1 == 0) {
                path.moveTo(x0.toFloat(), y0.toFloat())
                path.lineTo(x1.toFloat(), y1.toFloat())
            } else {
                val s = clipSegment(x0, y0, x1, y1, c0, c1)
                if (s != null) {
                    path.moveTo(s[0].toFloat(), s[1].toFloat())
                    path.lineTo(s[2].toFloat(), s[3].toFloat())
                }
            }
        } else if (path is Path) {
            path.bound = RectD(bound)
            if (c0 == 0 && c1 == 0) {
                path.moveTo(x0, y0)
                path.lineTo(x1, y1)
            } else {
                val s = clipSegment(x0, y0, x1, y1, c0, c1)
                if (s != null) {
                    path.moveTo(s[0], s[1])
                    path.lineTo(s[2], s[3])
                }
            }
        } else if (path is Polygon) {
            if (c0 == 0 && c1 == 0) {
                path.moveTo(x0, y0)
                path.lineTo(x1, y1)
            } else {
                val s = clipSegment(x0, y0, x1, y1, c0, c1)
                if (s != null) {
                    path.moveTo(s[0], s[1])
                    path.lineTo(s[2], s[3])
                }
            }
        }
    }

    fun neighbors(i: Int) = sequence {
        val ci = clip(i)
        if (ci != null)
            for (j in delaunay.neighbors(i)) {
                val cj = clip(j)
                // find the common edge
                if (cj != null)
                    loop@ for (ai in 0 until ci.size step 2) {
                        val li = ci.size
                        for (aj in 0 until cj.size step 2) {
                            val lj = cj.size
                            if (ci[ai] == cj[aj]
                                && ci[ai + 1] == cj[aj + 1]
                                && ci[(ai + 2) % li] == cj[(aj + lj - 2) % lj]
                                && ci[(ai + 3) % li] == cj[(aj + lj - 1) % lj]
                            ) {
                                yield(j)
                                break@loop
                            }
                        }
                    }
            }
    }

    /**
     * Get the polygon cell points as raw data, without
     * applying clipping.
     */
    internal fun rawCell(i: Int): ArrayList<Double>? {
        val inedges = delaunay.inedges
        val halfedges = delaunay.halfEdges
        val triangles = delaunay.triangles
        val e0 = inedges[i]
        if (e0 == -1) return null // coincident point

        val points = ArrayList<Double>()
        var e = e0
        do {
            val t = (Math.floor(e / 3.0)).toInt()
            points.add(circumcenters[t * 2])
            points.add(circumcenters[t * 2 + 1])
            e = if (e % 3 == 2) {
                e - 2
            } else {
                e + 1
            }
            if (triangles[e] != i) break // bad triangulation
            e = halfedges[e]
        } while (e != e0 && e != -1)
        return points
    }

    internal fun clip(i: Int): ArrayList<Double>? {

        // degenerate case (1 valid point: return the box)
        if (i == 0 && delaunay.hull.size == 1) {
            return arrayListOf(this.bound.right, this.bound.top, this.bound.right, this.bound.bottom, this.bound.left, this.bound.bottom, this.bound.left, this.bound.top)
        }
        val points = rawCell(i) ?: return null

        val v = i * 4
        return if (vectors[v] != 0.0 || vectors[v + 1] != 0.0) {
            clipInfinite(i, points, vectors[v], vectors[v + 1], vectors[v + 2], vectors[v + 3])
        } else {
            clipFinite(i, points)
        }
    }

    internal fun clipInfinite(i: Int, points: ArrayList<Double>, vx0: Double, vy0: Double, vxn: Double, vyn: Double): ArrayList<Double>? {
        var pts = ArrayList<Double>(points)
        var p: Delaunator.PointD? = null

        p = project(pts[0], pts[1], vx0, vy0)
        if (p != null) {
            pts.add(0, p.x)
            pts.add(1, p.y)
        }

        p = project(pts[pts.size - 2], pts[pts.size - 1], vxn, vyn)
        if (p != null) {
            pts.add(p.x)
            pts.add(p.y)
        }

        val ptsTemp = clipFinite(i, pts)

        return when {
            ptsTemp != null -> {
                pts = ptsTemp

                var n = pts.size
                if (n < 2) {
                    return pts
                }

                var c0 = 0
                var c1 = edgeCode(pts[n - 2], pts[n - 1])

                var j = 0
                while (j < n) {
                    c0 = c1
                    c1 = edgeCode(pts[j], pts[j + 1])
                    if (c0 != 0 && c1 != 0) {
                        j = edge(i, c0, c1, pts, j)
                        n = pts.size
                    }
                    j += 2
                }
                pts
            }
            contains(i, (bound.left + bound.right) / 2, (bound.top + bound.bottom) / 2) -> {
                arrayListOf(bound.left, bound.top, bound.right, bound.top, bound.right, bound.bottom, bound.left, bound.bottom)
            }
            else -> return ptsTemp
        }
    }

    internal fun clipFinite(i: Int, points: ArrayList<Double>): ArrayList<Double>? {
        val n = points.size
        var pts: ArrayList<Double>? = null
        var x0 = 0.0
        var y0 = 0.0
        var x1 = points[n - 2]
        var y1 = points[n - 1]
        var c0 = 0
        var c1 = regionCode(x1, y1)
        var e0 = 0
        var e1 = 0
        for (j in 0 until points.size step 2) {
            x0 = x1
            y0 = y1
            x1 = points[j]
            y1 = points[j + 1]
            c0 = c1
            c1 = regionCode(x1, y1)
            if (c0 == 0 && c1 == 0) {
                e0 = e1
                e1 = 0
                if (pts != null) {
                    pts.add(x1)
                    pts.add(y1)
                } else {
                    pts = arrayListOf(x1, y1)
                }
            } else {
                var s: DoubleArray?
                var sx0 = 0.0
                var sy0 = 0.0
                var sx1 = 0.0
                var sy1 = 0.0
                if (c0 == 0) {
                    s = clipSegment(x0, y0, x1, y1, c0, c1)
                    if (s == null) continue
                    sx0 = s[0]
                    sy0 = s[1]
                    sx1 = s[2]
                    sy1 = s[3]
                } else {
                    s = clipSegment(x1, y1, x0, y0, c1, c0)
                    if (s == null) continue
                    sx1 = s[0]
                    sy1 = s[1]
                    sx0 = s[2]
                    sy0 = s[3]
                    e0 = e1
                    e1 = edgeCode(sx0, sy0)
                    if (e0 != 0 && e1 != 0) {
                        if (pts != null) {
                            edge(i, e0, e1, pts, pts.size)
                        }
                    }
                    if (pts != null) {
                        pts.add(sx0)
                        pts.add(sy0)
                    } else {
                        pts = arrayListOf(sx0, sy0)
                    }
                }
                e0 = e1
                e1 = edgeCode(sx1, sy1)
                if (e0 != 0 && e1 != 0) {
                    if (pts != null) {
                        edge(i, e0, e1, pts, pts.size)
                    }
                }
                if (pts != null) {
                    pts.add(sx1)
                    pts.add(sy1)
                } else {
                    pts = arrayListOf(sx1, sy1)
                }
            }
        }
        if (pts != null) {
            e0 = e1
            e1 = edgeCode(pts[0], pts[1])
            if (e0 != 0 && e1 != 0) edge(i, e0, e1, pts, pts.size)
        } else if (contains(i, (bound.left + bound.right) / 2, (bound.top + bound.bottom) / 2)) {
            return arrayListOf(bound.right, bound.top, bound.right, bound.bottom, bound.left, bound.bottom, bound.left, bound.top)
        }
        return pts
    }

    internal fun contains(i: Int, x: Double, y: Double): Boolean {
        if (x.isNaN() || y.isNaN()) return false
        return delaunay.step(i, x, y) == i
    }

    internal fun edgeCode(x: Double, y: Double): Int {
        val b1 = if (x == bound.left) {
            0b0001
        } else {
            if (x == bound.right) {
                0b0010
            } else {
                0b0000
            }
        }

        val b2 = if (y == bound.top) {
            0b0100
        } else {
            if (y == bound.bottom) {
                0b1000
            } else {
                0b0000
            }
        }
        return b1 or b2
    }

    internal fun edge(_i: Int, _e0: Int, e1: Int, p: ArrayList<Double>, _j: Int): Int {
        var j = _j
        var i = _i
        var e0 = _e0
        loop@ while (e0 != e1) {
            var x = 0.0
            var y = 0.0
            when (e0) {
                0b0101 -> {
                    // top-left
                    e0 = 0b0100
                    continue@loop
                }
                0b0100 -> {
                    // top
                    e0 = 0b0110
                    x = bound.right
                    y = bound.top
                }
                0b0110 -> {
                    // top-right
                    e0 = 0b0010
                    continue@loop
                }
                0b0010 -> {
                    // right
                    e0 = 0b1010
                    x = bound.right
                    y = bound.bottom
                }
                0b1010 -> {
                    // bottom-right
                    e0 = 0b1000
                    continue@loop
                }
                0b1000 -> {
                    // bottom
                    e0 = 0b1001
                    x = bound.left
                    y = bound.bottom
                }
                0b1001 -> {
                    // bottom-left
                    e0 = 0b0001
                    continue@loop
                }
                0b0001 -> {
                    // left
                    e0 = 0b0101
                    x = bound.left
                    y = bound.top
                }
            }
            if ((p.size <= _j * 2 || p[j] != x || p[j + 1] != y) && contains(i, x, y)) {
                p.add(j, x)
                p.add(j + 1, y)
                j += 2
            }
        }

        if (p.size > 4) {
            var i = 0
            while (i < p.size) {
                val j = (i + 2) % p.size
                val k = (i + 4) % p.size

                if ((p[i] == p[j] && p[j] == p[k]) || (p[i + 1] == p[j + 1] && p[j + 1] == p[k + 1])) {
                    for (t in j until (j + 2)) {
                        p.removeAt(j)
                    }
                    i -= 2
                }
                i += 2
            }
        }
        return j
    }

    internal fun project(x0: Double, y0: Double, vx: Double, vy: Double): Delaunator.PointD? {
        var t = Double.POSITIVE_INFINITY
        var c = 0.0
        var x = 0.0
        var y = 0.0
        if (vy < 0) {
            // top
            if (y0 <= bound.top) return null
            c = (bound.top - y0) / vy
            if (c < t) {
                y = bound.top
                t = c
                x = x0 + t * vx
            }
        } else if (vy > 0) {
            // bottom
            if (y0 >= bound.bottom) return null
            c = (bound.bottom - y0) / vy
            if (c < t) {
                y = bound.bottom
                t = c
                x = x0 + t * vx
            }
        }
        if (vx > 0) {
            // right
            if (x0 >= bound.right) return null
            c = (bound.right - x0) / vx
            if (c < t) {
                x = bound.right
                t = c
                y = y0 + t * vy
            }
        } else if (vx < 0) {
            // left
            if (x0 <= bound.left) return null
            c = (bound.left - x0) / vx
            if (c < t) {
                x = bound.left
                t = c
                y = y0 + t * vy
            }
        }
        return Delaunator.PointD(x, y)
    }

    internal fun regionCode(x: Double, y: Double): Int {
        val b1 = if (x < bound.left) {
            0b0001
        } else {
            if (x > bound.right) 0b0010
            else 0b0000
        }

        val b2 = if (y < bound.top) {
            0b0100
        } else {
            if (y > bound.bottom) 0b1000
            else 0b0000
        }
        return b1 or b2
    }

    internal fun clipSegment(x0: Double, _y0: Double, _x1: Double, _y1: Double, _c0: Int, _c1: Int): DoubleArray? {

        var x0 = x0
        var y0 = _y0
        var x1 = _x1
        var y1 = _y1
        var c0 = _c0
        var c1 = _c1
        while (true) {
            if (c0 == 0 && c1 == 0) {
                return doubleArrayOf(x0, y0, x1, y1)
            }

            if ((c0 and c1) != 0) return null
            var x: Double
            var y: Double
            val c = if (c0 != 0) c0 else c1

            if ((c and 0b1000) != 0) {
                x = x0 + (x1 - x0) * (bound.bottom - y0) / (y1 - y0)
                y = bound.bottom
            } else if ((c and 0b0100) != 0) {
                x = x0 + (x1 - x0) * (bound.top - y0) / (y1 - y0)
                y = bound.top
            } else if ((c and 0b0010) != 0) {
                y = y0 + (y1 - y0) * (bound.right - x0) / (x1 - x0)
                x = bound.right
            } else {
                y = y0 + (y1 - y0) * (bound.left - x0) / (x1 - x0)
                x = bound.left
            }

            if (c0 != 0) {
                x0 = x
                y0 = y
                c0 = regionCode(x0, y0)
            } else {
                x1 = x
                y1 = y
                c1 = regionCode(x1, y1)
            }
        }
    }

    /**
     * Class for representing bound using double values, the class is same as RectF.
     * @param left left coordinate of the bound
     * @param top top coordinate of the bound
     * @param right right coordinate of the bound
     * @param bottom bottom coordinate of the bound
     */
    data class RectD(var left: Double = 0.0, var top: Double = 0.0, var right: Double = 100.0, var bottom: Double = 50.0) {

        constructor(bound: RectD) : this(bound.left, bound.top, bound.right, bound.bottom)

        /**
         * Get the width of the bound
         */
        fun width(): Double {
            return right - left
        }

        /**
         * Get the height of the bound
         */
        fun height(): Double {
            return bottom - top
        }

        override fun toString(): String {
            return "left:$left, top:$top, right:$right, bottom:$bottom"
        }
    }

    /**
     * Holding the polygon(cell) coordinate for each points x,y pair and the index
     * @param coordinates array list with all the points coordinates from the polygon
     * @param index index of the polygon
     */
    data class CellValues(var coordinates: ArrayList<Double>, var index: Int = -1)
}