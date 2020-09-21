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
import android.view.ViewTreeObserver
import com.slaviboy.graphics.RectD
import com.slaviboy.voronoi.Delaunay
import com.slaviboy.voronoi.Polygon
import com.slaviboy.voronoi.Voronoi
import kotlin.math.sqrt

/**
 * Simple view, that creates random points, and generates the voronoi diagram. It draw
 * each cells with color corresponding to the distance from the view center to each
 * cell center.
 */
class VoronoiView : View {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var numberOfRandomPoints: Int          // number of points that will be generated
    private var viewCenterX: Double                // the center of the view once it is calculated
    private var viewCenterY: Double                // the center of the view once it is calculated
    private var halfDiagonalWidth: Double          // half of the diagonal width
    private lateinit var delaunay: Delaunay        // delaunay object for center points
    lateinit var voronoi: Voronoi                  // voronoi object for cells
    private var paint: Paint                       // paint object for the drawing
    private var isInit: Boolean                    // if view size is initialized, right before drawing
    private var gradientPicker: GradientPicker     // gradient picker, that will set color for each cell
    private var relaxationLoops = 0                // how many times to apply the llyod relaxation
    private var useDistantColor: Boolean = true    // use the gradient picker to generate different color depending how close the cells center is to the center of the canvas
    private val path: Path = Path()                // the path for the generated cells

    companion object {

        /**
         * Inline function that is called, when the final measurement is made and
         * the view is about to be draw.
         */
        inline fun View.afterMeasured(crossinline f: View.() -> Unit) {
            viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (measuredWidth > 0 && measuredHeight > 0) {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                        f()
                    }
                }
            })
        }
    }

    init {

        isInit = false
        halfDiagonalWidth = 0.0
        numberOfRandomPoints = 100

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

        paint = Paint().apply {
            isAntiAlias = true
        }
        viewCenterX = 0.0
        viewCenterY = 0.0

        // called on a final measure, when view size is available
        this.afterMeasured {
            paint.textSize = width / 25f

            isInit = true
            viewCenterX = width / 2.0
            viewCenterY = height / 2.0
            halfDiagonalWidth = Math.sqrt((width * width + height * height) / 4.0)
            initVoronoi()
        }
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
        // generate delaunay and voronoi objects
        delaunay = Delaunay(*points)
        voronoi = Voronoi(delaunay, RectD(0.0, 0.0, width.toDouble(), height.toDouble()))

        // apply relaxation to the points
        voronoi.relax(relaxationLoops)

        invalidate()
    }

    /**
     * Render all cells(polygon) for the voronoi diagram as paths, and also generate
     * the input point and the center of each cell as a circles.
     * @param canvas canvas where all triangles and points will be drawn
     * @param lineColor stroke color for all lines
     * @param centerCircleColor fill color for all circles
     * @param circleRadius radius for all circles
     */
    fun drawCellsWithPoints(
        canvas: Canvas, lineColor: Int = Color.BLACK, inputCircleColor: Int = Color.BLUE,
        centerCircleColor: Int = Color.GREEN, circleRadius: Double = canvas.width / 100.0
    ) {

        // render cells with different color depending on distance from the center
        for (i in 0 until delaunay.coordinates.size / 2) {
            path.reset()
            voronoi.renderCell(i, path)

            var cellColor = Color.WHITE
            if (useDistantColor) {
                val cellCenterX = delaunay.coordinates[i * 2]                                   // center x of the voronoi shape
                val cellCenterY = delaunay.coordinates[i * 2 + 1]                               // center x of the voronoi shape
                val distance = distance(viewCenterX, viewCenterY, cellCenterX, cellCenterY)     // distance from the center screen to the cell center
                val distanceInRange = (distance / halfDiagonalWidth).toFloat()                  // fit the distance in range between [0, 1]
                cellColor = gradientPicker.getColorFromGradient(distanceInRange)                // get the color corresponding to the distance
            }

            // fill the cell with the corresponding color
            paint.apply {
                color = cellColor
                style = Paint.Style.FILL
            }
            canvas.drawPath(path, paint)

            // stroke the cell
            paint.apply {
                color = lineColor
                style = Paint.Style.STROKE
                strokeWidth = 2.0f
            }
            canvas.drawPath(path, paint)
        }

        // draw input points
        path.reset()
        paint.apply {
            color = inputCircleColor
            style = Paint.Style.FILL
        }
        voronoi.renderInputPoints(circleRadius, path)
        canvas.drawPath(path, paint)

        // draw center points
        path.reset()
        paint.color = centerCircleColor
        voronoi.renderCenters(circleRadius, path)
        canvas.drawPath(path, paint)
    }

    fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val a = x1 - x2
        val b = y1 - y2
        return sqrt(a * a + b * b)
    }

    /**
     * Render all voronoi lines for the voronoi diagram.
     * @param canvas canvas where all lines will be drawn
     * @param lineColor stroke color for all lines
     */
    fun drawLines(canvas: Canvas, lineColor: Int = Color.BLACK) {

        // draw voronoi lines
        path.reset()
        paint.apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 2.0f
        }
        voronoi.render(path)
        canvas.drawPath(path, paint)
    }

    override fun onDraw(canvas: Canvas) {

        drawLines(canvas)
        //drawCellsWithPoints(canvas)
    }
}