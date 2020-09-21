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
package com.slaviboy.voronoikotlinexamples.animation

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceView 
import com.slaviboy.graphics.PointD
import com.slaviboy.graphics.RectD
import com.slaviboy.voronoi.Delaunay
import com.slaviboy.voronoi.Polygon
import com.slaviboy.voronoi.Voronoi
import com.slaviboy.voronoikotlinexamples.drawing.VoronoiView.Companion.afterMeasured
import kotlin.math.roundToInt

/**
 * Simple surface view that displays animation demonstrating the Lloyd relaxation
 * algorithm for the voronoi diagram. The view uses single thread for redrawing the content.
 */
class LloydRelaxationAnimationView : SurfaceView, AnimationThread.OnThreadCallListener {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private lateinit var voronoi: Voronoi                       // voronoi object
    private lateinit var coordinates: DoubleArray               // coordinates with x,y pairs for the random points
    private val centroid: PointD                                // temp point object that is reused and holds the centroid of each path
    private var numberOfRandomPoints: Int                       // shows the number of random points that will be generated
    private var iterations: Int                                 // shows the number of iterations for the applied Lloyd relaxation algorithm
    private lateinit var colors: IntArray                       // array with the two color, that are used to generate color between these two colors
    private lateinit var range: Range                           // range object for generating the color of the current polygon
    var thread: AnimationThread?                                // thread object responsible for locking canvas, and triggering a callback fro redrawing
    private var path: Path                                      // path object that is reused and holds the path for each polygon
    private val textBound: Rect                                 // bound for the text that displays the number of iterations for the Lloyd relaxation
    private val paint: Paint = Paint().apply {                  // paint object for setting stroke and fill colors for the path
        isAntiAlias = true
    }

    init {
        centroid = PointD()
        numberOfRandomPoints = 150
        iterations = 0
        thread = null
        path = Path()
        textBound = Rect()

        afterMeasured {
            paint.textSize = width / 18f
            range = Range(0.0, Math.sqrt(width.toDouble() * height / numberOfRandomPoints) * 4)
            reset(width / 2.0, height / 2.0)
            initVoronoi()
        }
    }

    /**
     * Initialize the voronoi and delaunay objects by setting up random input points
     */
    private fun initVoronoi() {

        // generate delaunay and voronoi objects
        voronoi = Voronoi(Delaunay(*coordinates), RectD(0.0, 0.0, width.toDouble(), height.toDouble()))
        invalidate()
    }

    /**
     * Method for resetting the voronoi diagram by choosing new color and using the
     * coordinates of the current finger position.
     * @param x x coordinate of the current finger position
     * @param y y coordinate of the current finger position
     */
    fun reset(x: Double, y: Double, resetThread: Boolean = false) {

        // stop thread
        if (resetThread) {
            stop()
        }

        // generate two color with random HUE
        val hcl = HCL((0..360).random(), 27, 83)
        colors = intArrayOf(
            hcl.rgb().getInt(),
            hcl.darker(2).rgb().getInt()
        )

        // generate random coordinate positions
        coordinates = DoubleArray(numberOfRandomPoints * 2)
        for (i in 0 until numberOfRandomPoints) {
            coordinates[i * 2] = x + Math.random() - 0.5
            coordinates[i * 2 + 1] = y + Math.random() - 0.5
        }

        // reset the voronoi
        if (::voronoi.isInitialized) {
            voronoi = Voronoi(Delaunay(*coordinates), RectD(0.0, 0.0, width.toDouble(), height.toDouble()))
        }

        // start thread
        if (resetThread) {
            start()
        }

        iterations = 0
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val eventAction = event.action

        val x = event.x.toDouble()
        val y = event.y.toDouble()

        // put your code in here to handle the event
        when (eventAction) {
            MotionEvent.ACTION_DOWN -> {

                // reset the voronoi diagram
                reset(x, y, true)
            }
            MotionEvent.ACTION_UP -> {
            }
            MotionEvent.ACTION_MOVE -> {
            }
        }
        return true
    }

    /**
     * Method triggered when the thread is called for redrawing the canvas for
     * the surface-view
     * @param canvas canvas where the elements will be drawn
     */
    override fun onThreadCall(canvas: Canvas) {

        // clear canvas
        canvas.drawColor(Color.GREEN)

        val cells = voronoi.getCellsCoordinatesSequence().toList()

        // draw each cell and set its color depending on the area of the polygon, bigger area darker polygon
        paint.style = Paint.Style.FILL
        for (i in cells.indices) {

            val cell = cells[i]

            // make sure polygon have at least 3 points (6 coordinates) for calculating area
            if (cell.coordinates.size < 6) {
                continue
            }

            // apply Lloyd relaxation and update the voronoi diagram
            Polygon.center(cell.coordinates, centroid)
            voronoi.delaunay.coordinates[i * 2]  = centroid.x
            voronoi.delaunay.coordinates[i * 2 + 1]  = centroid.y

            val area = Polygon.area(cell.coordinates)
            range.current = Math.sqrt(area)
            paint.color = interpolateColor(
                colors[0],
                colors[1],
                range.getCurrent(0.0, 1.0).toFloat()
            )

            val coordinates = cell.coordinates
            path.rewind()
            path.moveTo(coordinates[0].toFloat(), coordinates[1].toFloat())
            for (i in 1 until coordinates.size / 2) {
                path.lineTo(coordinates[i * 2].toFloat(), coordinates[i * 2 + 1].toFloat())
            }
            canvas.drawPath(path, paint)
        }
        voronoi.update()

        // draw the stroke
        path.rewind()
        for (cell in cells) {
            val coordinates = cell.coordinates

            path.moveTo(coordinates[0].toFloat(), coordinates[1].toFloat())
            for (i in 1 until coordinates.size / 2) {
                path.lineTo(coordinates[i * 2].toFloat(), coordinates[i * 2 + 1].toFloat())
            }
        }
        paint.apply {
            strokeWidth = 3f
            style = Paint.Style.STROKE
            color = Color.WHITE
        }
        canvas.drawPath(path, paint)

        // draw text with the number of iterations
        paint.apply {
            style = Paint.Style.FILL
            color = Color.BLACK
        }
        drawCenteredText(
            canvas = canvas,
            paint = paint,
            text = "iterations: $iterations",
            isCenteredHorizontally = true,
            verticalMargin = 32f
        )
        if (iterations > 1000) {
            stop()
        }
        iterations++
    }

    /**
     * Draw text that can be vertically or horizontally centered
     * @param canvas canvas where the text will be drawn
     * @param paint paint object holding color and text align properties
     * @param text text that will be displayed
     * @param isCenteredVertically if text should be centered vertically
     * @param isCenteredHorizontally if text should be centered horizontally
     * @param verticalMargin vertical margin for the text, it can be positive or negative float values
     * @param horizontalMargin horizontal margin for the text, it can be positive or negative float values
     */
    private fun drawCenteredText(
        canvas: Canvas, paint: Paint, text: String, isCenteredVertically: Boolean = false,
        isCenteredHorizontally: Boolean = false, verticalMargin: Float = 0f, horizontalMargin: Float = 0f
    ) {
        val xPos = if (isCenteredHorizontally) {
            paint.textAlign = Paint.Align.CENTER
            canvas.width / 2f
        } else {
            paint.textAlign = Paint.Align.LEFT
            0f
        }

        val yPos = if (isCenteredVertically) {
            (canvas.height / 2 - (paint.descent() + paint.ascent()) / 2)
        } else {
            -(paint.descent() + paint.ascent())
        }

        canvas.drawText(text, xPos + horizontalMargin, yPos + verticalMargin, paint)
    }

    private fun interpolate(a: Float, b: Float, proportion: Float): Float {
        return a + (b - a) * proportion
    }

    /** Returns an interpolated color, between two color
     * @param a integer representation of the first color
     * @param b integer representation of the second color
     * @param proportion proportion between the two colors value is in range [0,1]
     */
    private fun interpolateColor(a: Int, b: Int, proportion: Float): Int {
        val hsvA = FloatArray(3)
        val hsvB = FloatArray(3)
        Color.colorToHSV(a, hsvA)
        Color.colorToHSV(b, hsvB)
        for (i in 0..2) {
            hsvB[i] = interpolate(hsvA[i], hsvB[i], proportion)
        }
        return Color.HSVToColor(hsvB)
    }

    /**
     * Start the thread for the surface-view
     */
    fun start() {
        if (thread == null) {
            thread = AnimationThread(this.holder)
            thread!!.onThreadCallListener = this
            thread!!.startThread()
        }
    }

    /**
     * Stop the thread for the surface-view
     */
    fun stop() {
        if (thread != null) {
            thread!!.stopThread()

            // Waiting for the thread to die by calling thread.join,
            // repeatedly if necessary
            var retry = true
            while (retry) {
                try {
                    thread!!.join()
                    retry = false
                } catch (e: InterruptedException) {
                }
            }
            thread = null
        }
    }

    /**
     * Class for holding values for the RGB(Red,Green,Blue) color model
     */
    private class RGB(var r: Int, var g: Int, var b: Int) {

        /**
         * Return the integer value for current color
         */
        fun getInt(): Int {
            return Color.rgb(r, g, b)
        }

        override fun toString(): String {
            return "$r,$g,$b"
        }
    }

    /**
     * Class for representing HCL(Hue,Chroma,Luminance) color model, with methods for conversion to
     * RGB color model.
     */
    private class HCL(var h: Int, var c: Int, var l: Int) {

        companion object {

            const val radians = Math.PI / 180
            const val degrees = 180 / Math.PI

            const val Kn = 18
            const val Xn = 0.950470
            const val Yn = 1
            const val Zn = 1.088830
            const val t0 = 4 / 29
            const val t1 = 6 / 29
            const val t2 = 3 * t1 * t1
            const val t3 = t1 * t1 * t1
        }

        /**
         * Make current color brighter
         * @param k with how much to make the color brighter
         */
        fun brighter(k: Int): HCL {
            return HCL(this.h, this.c, this.l + Kn * k)
        }

        /**
         * Make current color darker
         * @param k with how much to make the color darker
         */
        fun darker(k: Int): HCL {
            return HCL(this.h, this.c, this.l - Kn * k)
        }

        /**
         * Get the RGB representation of current color, that include the r,g,b values
         */
        fun rgb(): RGB {

            val h = h * radians
            val a = Math.cos(h) * c
            val b = Math.sin(h) * c


            var y = (l + 16) / 116.0
            var x = y + a / 500.0
            var z = y - b / 200.0
            x = Xn * lab2xyz(x)
            y = Yn * lab2xyz(y)
            z = Zn * lab2xyz(z)

            return RGB(
                lrgb2rgb(3.2404542 * x - 1.5371385 * y - 0.4985314 * z),
                lrgb2rgb(-0.9692660 * x + 1.8760108 * y + 0.0415560 * z),
                lrgb2rgb(0.0556434 * x - 0.2040259 * y + 1.0572252 * z)
            )
        }

        fun lab2xyz(t: Double): Double {
            return if (t > t1) {
                t * t * t
            } else {
                t2 * (t - t0)
            }
        }

        fun lrgb2rgb(x: Double): Int {
            return (255 * (if (x <= 0.0031308) 12.92 * x else 1.055 * Math.pow(x, 1 / 2.4) - 0.055)).roundToInt()
        }
    }

    /**
     * Class for representing ranges that can convert current value from one range to another
     */
    private class Range(var lower: Double = 0.0, var upper: Double = 100.0, current: Double = Math.min(lower, upper)) {

        // current value that is bound to [lower, upper]
        var current: Double = current
            set(value) {

                // get min and max
                val min = Math.min(lower, upper)
                val max = Math.max(lower, upper)

                // set current with check
                field = when {
                    value < min -> {
                        min
                    }
                    value > max -> {
                        max
                    }
                    else -> {
                        value
                    }
                }
            }

        /**
         * Set current value, using total and current allowed values, for example if a total side length
         * is 10cm and current position is 2cm, and we have range [0,100]. Then current range value is 20.
         * @param total total value
         * @param current current value
         */
        fun setCurrent(total: Double, current: Double) {
            val fact = (current / total)
            this.current = lower - (lower - upper) * fact
        }

        /**
         * Get the current value in a range between [expectedLower, expectedUpper], this is the new bound.
         * For example if the original range is [0,100] and current value is 20. Then if we want to now the
         * current value in new range with values between [-100,100], that will return the current value as -60.
         */
        fun getCurrent(expectedLower: Double, expectedUpper: Double): Double {
            val lowerDistance = Math.abs(current - lower)
            val upperDistance = Math.abs(current - upper)
            val distance = lowerDistance + upperDistance
            val fact = (lowerDistance / distance)
            val newCurrent = expectedLower - (expectedLower - expectedUpper) * fact
            return newCurrent
        }

        override fun toString(): String {
            return "lower: $lower, upper: $upper, current: $current"
        }
    }
}