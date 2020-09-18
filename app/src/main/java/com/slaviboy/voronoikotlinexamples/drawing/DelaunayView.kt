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
package com.slaviboy.voronoikotlinexamples.drawing

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.slaviboy.delaunator.Delaunator
import com.slaviboy.voronoi.Delaunay
import com.slaviboy.voronoikotlinexamples.drawing.VoronoiView.Companion.afterMeasured

/**
 * Simple view that demonstrates how to draw the Delaunay triangulation, using different method
 * - renderAll()
 * - generateTrianglesWithPoints()
 * - generateTrianglesWithCenter()
 */
class DelaunayView : View {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var numberOfRandomPoints: Int               // number of points that will be generated
    private lateinit var viewCenter: Delaunator.PointD  // the center of the view once it is calculated
    private var halfDiagonalWidth: Double               // half of the diagonal width
    lateinit var delaunay: Delaunay                     // delaunay object for center points
    private var paint: Paint                            // paint object for the drawing
    private var isInit: Boolean                         // if view size is initialized, right before drawing
    private var gradientPicker: GradientPicker          // gradient picker, that will set color for each cell
    private var useDistantColor: Boolean                // use the gradient picker to generate different color depending how close the cells center is to the center of the canvas
    private val path: Path = Path()                     // the path for the generated cells

    init {

        isInit = false
        halfDiagonalWidth = 0.0
        numberOfRandomPoints = 100
        useDistantColor = true

        // create gradient picker that gets color on certain position
        gradientPicker = GradientPicker(
            arrayListOf(
                Color.parseColor("#9F0342"),
                Color.parseColor("#F06E4A"),
                Color.parseColor("#FEF0A6"),
                Color.parseColor("#438DB4"),
                Color.parseColor("#5B53A4")
            ),
            arrayListOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)
        )

        paint = Paint()
        paint.isAntiAlias = true

        // called on a final measure, when view size is available
        this.afterMeasured {
            isInit = true
            viewCenter = Delaunator.PointD(width / 2.0, height / 2.0)
            halfDiagonalWidth = Math.sqrt((width * width + height * height) / 4.0)
            initVoronoi()
        }
    }

    fun distance(p1: Delaunator.PointD, p2: Delaunator.PointD): Double {
        val a = p1.x - p2.x
        val b = p1.y - p2.y
        return Math.sqrt(1.0 + a * a + b * b)
    }

    /**
     * Initialize the voronoi and delaunay objects by setting up random input points
     */
    private fun initVoronoi() {

        // generate random points
        val points = DoubleArray(numberOfRandomPoints * 2)
        for (i in 0 until numberOfRandomPoints) {
            points[i * 2] = (Math.random() * width - 1)
            points[i * 2 + 1] = (Math.random() * height - 1)
        }
        delaunay = Delaunay(*points)
    }

    /**
     * Render all triangles from the delaunay triangulation with different color from center
     * to the outer edges, and also the input points and the center points as circle.
     * @param canvas canvas where all triangles and points will be drawn
     * @param lineColor stroke color for all lines
     * @param centerCircleColor fill color for all circles
     * @param circleRadius radius for all circles
     */
    fun drawTrianglesWithPoints(
        canvas: Canvas, lineColor: Int = Color.BLACK, inputCircleColor: Int = Color.BLUE,
        centerCircleColor: Int = Color.GREEN, circleRadius: Double = canvas.width / 100.0
    ) {

        // draw triangles with different colors
        for (i in 0 until delaunay.triangles.size / 3) {

            path.reset()
            delaunay.renderTriangle(i, path)

            val triangleCenter = delaunay.getTriangleCenter(i)                          // center of the voronoi shape
            val distance = distance(viewCenter, triangleCenter)                         // distance from the center screen to the triangle center
            val distanceInRange = (distance / halfDiagonalWidth).toFloat()              // fit the distance in range between [0, 1]
            val triangleColor = gradientPicker.getColorFromGradient(distanceInRange)    // get the color corresponding to the distance

            // fill the cell with the corresponding color
            paint.apply {
                color = triangleColor
                style = Paint.Style.FILL
            }
            canvas.drawPath(path, paint)

            // stroke the cell with black color
            paint.apply {
                color = lineColor
                style = Paint.Style.STROKE
                strokeWidth = 2.0f
            }
            canvas.drawPath(path, paint)
        }

        // draw triangle input points
        path.reset()
        paint.apply {
            style = Paint.Style.FILL
            color = inputCircleColor
        }
        delaunay.renderInputPoints(circleRadius, path)
        canvas.drawPath(path, paint)

        // draw triangle centers as circles
        path.reset()
        paint.apply {
            color = centerCircleColor
            style = Paint.Style.FILL
        }
        delaunay.renderCenters(circleRadius, path)
        canvas.drawPath(path, paint)
    }

    /**
     * Render all triangle lines from the delaunay triangulation that includes all lines
     * half-edges, hull.
     * @param canvas canvas where all lines will be drawn
     * @param lineColor stroke color for all lines
     */
    fun drawLines(canvas: Canvas, lineColor: Int = Color.BLACK) {

        // draw lines
        paint.apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 2.0f
        }
        delaunay.render(path)
        canvas.drawPath(path, paint)
    }

    override fun onDraw(canvas: Canvas) {

        drawLines(canvas)
        //drawTrianglesWithPoints(canvas)
    }
}