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

import android.graphics.Canvas
import android.view.SurfaceHolder

/**
 * Thread class with callback methods onThreadCall() triggered when the runnable is
 * looped for redrawing the scene to the locked canvas.
 */
class AnimationThread(var surfaceHolder: SurfaceHolder) : Thread() {

    var running = false

    fun startThread() {
        running = true
        super.start()
    }

    fun stopThread() {
        running = false
    }

    override fun run() {
        var canvas: Canvas? = null
        while (running) {
            canvas = null
            try {
                canvas = surfaceHolder.lockCanvas()
                synchronized(surfaceHolder) {
                    if (canvas != null) {
                        onThreadCallListener.onThreadCall(canvas)
                    }
                }
                //sleep(SLEEP_TIME.toLong())
            } catch (ie: InterruptedException) {
            } finally {
                // do this in a finally so that if an exception is thrown
                // we don't leave the Surface in an inconsistent state
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    interface OnThreadCallListener {
        fun onThreadCall(canvas: Canvas)
    }

    lateinit var onThreadCallListener: OnThreadCallListener

    companion object {
        private const val SLEEP_TIME = 1
    }
}