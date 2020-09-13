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

import android.graphics.Color
import kotlin.math.sqrt

/**
 * Simple gradient picker class that, represents a gradient with attached
 * color and the corresponding position. With method getColor(), that gets
 * the color on a particular position.
 * @param colors integer color representations that will be included in the gradient
 * @param positions array with positions corresponding to each color, each position is between [0, 1]
 */
class GradientPicker(
    private val colors: ArrayList<Int> = ArrayList(),
    private val positions: ArrayList<Float> = ArrayList()
) {

    // simple class representing ARGB with floating point
    class ColorChannels(var alpha: Float, var red: Float, var green: Float, var blue: Float)


    /**
     * Get the color
     * @param position color position between [0, 1]
     */
    fun getColorFromGradient(position: Float): Int {
        require(!(colors.size == 0 || colors.size != positions.size))

        if (colors.size == 1) {
            return colors[0]
        }

        if (position <= positions[0]) {
            return colors[0]
        }

        if (position >= positions[positions.size - 1]) {
            return colors[positions.size - 1]
        }

        // find the two color positions, the current position is between
        for (i in 1 until positions.size) {
            if (position <= positions[i]) {
                val t = (position - positions[i - 1]) / (positions[i] - positions[i - 1])
                return lerpColor(colors[i - 1], colors[i], t)
            }
        }
        throw RuntimeException()
    }

    /**
     * Lerp color between the two colors
     * @param colorA first color
     * @param colorB second color
     * @param t show how far the new color is from the two input color, value is between [0, 1]
     */
    fun lerpColor(colorA: Int, colorB: Int, t: Float): Int {

        // get the new channels values fro each color
        val newColorA = multiplyColorChannels(colorA, 1 - t)
        val newColorB = multiplyColorChannels(colorB, t)

        // merge the two color channel values
        val alpha = (newColorA.alpha + newColorB.alpha).toInt()
        val red = (newColorA.red + newColorB.red).toInt()
        val green = (newColorA.green + newColorB.green).toInt()
        val blue = (newColorA.blue + newColorB.blue).toInt()

        return Color.argb(alpha, red, green, blue)
    }

    /**
     * Multiply all four channel value, with the tension value and return,
     * object containing the new values, for each channel.
     */
    fun multiplyColorChannels(color: Int, t: Float): ColorChannels {
        return ColorChannels(
            Color.alpha(color) * t,
            Color.red(color) * t,
            Color.green(color) * t,
            Color.blue(color) * t
        )
    }

    /**
     * Add new color, to a given position
     * @param color integer color representation
     * @param position floating gradient position for the color between [0, 1]
     */
    fun add(color: Int, position: Float) {
        colors.add(color)
        positions.add(position)
    }
}