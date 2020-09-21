package com.slaviboy.voronoi

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.slaviboy.delaunator.Delaunator
import com.slaviboy.graphics.PointD
import com.slaviboy.graphics.RectD
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class PolygonUnitTest {

    lateinit var polygonValues: PolygonValues

    /**
     * Class for holding the expected value for the delaunay triangulation from the json file
     */
    class PolygonValues(
        var coordinates: ArrayList<Double>, var area: Double, var center: PointD,
        var perimeter: Double, var bound: RectD
    )

    @Test
    fun MainTest() {

        val gson = Gson()
        val context: Context = ApplicationProvider.getApplicationContext()

        // load json file with expected test values
        val jsonDelaunayValues = DelaunayUnitTest.loadStringFromRawResource(context.resources, R.raw.polygon_values)
        polygonValues = gson.fromJson(jsonDelaunayValues, PolygonValues::class.java)

        PolygonValueTest()
    }

    fun PolygonValueTest() {

        var polygon = Polygon(polygonValues.coordinates)

        // check expected method values
        assertThat(polygon.area()).isEqualTo(polygonValues.area)
        assertThat(polygon.center()).isEqualTo(polygonValues.center)
        assertThat(polygon.contains(3.32, 83.83)).isFalse()
        assertThat(polygon.contains(213.32, 283.83)).isTrue()
        assertThat(polygon.perimeter()).isEqualTo(polygonValues.perimeter)
        assertThat(polygon.bound()).isEqualTo(polygonValues.bound)

        polygon = Polygon()
        polygon.apply {
            moveTo(323.4, 35.1)
            lineTo(41.2, 841.221)
            lineTo(91.2, 19.911)
            closePath()
        }
        assertThat(polygon.coordinates).isEqualTo(
            arrayListOf(
                323.4, 35.1,
                41.2, 841.221,
                91.2, 19.911,
                323.4, 35.1
            )
        )
    }
}