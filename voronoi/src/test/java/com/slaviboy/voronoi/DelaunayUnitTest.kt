package com.slaviboy.voronoi

import android.content.Context
import android.content.res.Resources
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.slaviboy.voronoi.Voronoi.RectD
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.slaviboy.delaunator.Delaunator.PointD
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class DelaunayUnitTest {

    lateinit var delaunayValues: DelaunayValues

    /**
     * Class for holding the expected value for the delaunay triangulation from the json file
     */
    class DelaunayValues(
        var coordinates: DoubleArray, var halfEdges: IntArray, var hull: IntArray,
        var hullIndexTemp: IntArray, var inedges: IntArray, var triangles: IntArray
    )

    @Test
    fun MainTest() {

        val gson = Gson()
        val context: Context = ApplicationProvider.getApplicationContext()

        // load json file with expected test values
        val jsonDelaunayValues = loadStringFromRawResource(context.resources, R.raw.delaunay_values)
        delaunayValues = gson.fromJson(jsonDelaunayValues, DelaunayValues::class.java)

        DelaunayValueTest()
        DelaunayMainTest()
    }

    fun DelaunayValueTest() {

        // test for matching delaunay values
        var delaunay = Delaunay(*delaunayValues.coordinates)
        assertThat(delaunay.collinear).isEmpty()
        assertThat(delaunay.coordinates).isEqualTo(delaunayValues.coordinates)
        assertThat(delaunay.halfEdges).isEqualTo(delaunayValues.halfEdges)
        assertThat(delaunay.hull).isEqualTo(delaunayValues.hull)
        assertThat(delaunay.hullIndexTemp).isEqualTo(delaunayValues.hullIndexTemp)
        assertThat(delaunay.inedges).isEqualTo(delaunayValues.inedges)
        assertThat(delaunay.triangles).isEqualTo(delaunayValues.triangles)

        // test render methods
        delaunay = Delaunay(345.2, 53.3, 526.21, 53.11, 72.3, 213.54, 54.3, 93.4)
        var path: Path = delaunay.renderTriangle(1) as Path
        assertThat(path.value).isEqualTo("M345.2,53.3L54.3,93.4L72.3,213.54Z")

        path = delaunay.renderTriangle(0) as Path
        assertThat(path.value).isEqualTo("M345.2,53.3L72.3,213.54L526.21,53.11Z")

        path = delaunay.renderHull() as Path
        assertThat(path.value).isEqualTo("M345.2,53.3L54.3,93.4L72.3,213.54L526.21,53.11Z")

        path = delaunay.renderPoints() as Path
        assertThat(path.value).isEqualTo("M348.2,53.3A3.0,3.0,0,1,1,342.2,53.3A3.0,3.0,0,1,1,348.2,53.3M529.21,53.11A3.0,3.0,0,1,1,523.21,53.11A3.0,3.0,0,1,1,529.21,53.11M75.3,213.54A3.0,3.0,0,1,1,69.3,213.54A3.0,3.0,0,1,1,75.3,213.54M57.3,93.4A3.0,3.0,0,1,1,51.3,93.4A3.0,3.0,0,1,1,57.3,93.4")

        path = delaunay.render() as Path
        assertThat(path.value).isEqualTo("M345.2,53.3L72.3,213.54M345.2,53.3L54.3,93.4L72.3,213.54L526.21,53.11Z")

        path = delaunay.renderHalfEdges() as Path
        assertThat(path.value).isEqualTo("M345.2,53.3L72.3,213.54")

        val lineArraySize = delaunay.getLineArraySize()
        assertThat(lineArraySize).isEqualTo(5)

        val lineCoordinates = delaunay.getLinesCoordinates(lineArraySize)
        assertThat(lineCoordinates).isEqualTo(doubleArrayOf(345.2, 53.3, 72.3, 213.54, 345.2, 53.3, 54.3, 93.4, 54.3, 93.4, 72.3, 213.54, 72.3, 213.54, 526.21, 53.11, 345.2, 53.3, 526.21, 53.11))

        val linePointIndices = delaunay.getLinesPointIndices(lineArraySize)
        assertThat(linePointIndices).isEqualTo(intArrayOf(0, 2, 0, 3, 3, 2, 2, 1, 0, 1))

        val trianglesCoordinates = delaunay.getTrianglesCoordinates()
        assertThat(trianglesCoordinates).isEqualTo(doubleArrayOf(345.2, 53.3, 72.3, 213.54, 526.21, 53.11, 345.2, 53.3, 54.3, 93.4, 72.3, 213.54))

        val trianglesPointIndices = delaunay.getTrianglesPointIndices()
        assertThat(trianglesPointIndices).isEqualTo(intArrayOf(0, 2, 1, 0, 3, 2))

        val trianglesCenterCoordinates = delaunay.getTriangleCenterCoordinates()
        assertThat(trianglesCenterCoordinates).isEqualTo(doubleArrayOf(314.57, 106.64999999999999, 157.26666666666668, 120.08))

        // test static methods
        // sort by y
        var points = doubleArrayOf(
            12.0, 30.0,
            12.0, 1.0,
            12.0, 23.0
        )
        var indices = arrayListOf(0, 1, 2)
        Delaunay.getSorterIndices(points, indices)
        assertThat(indices).isEqualTo(arrayListOf(1, 2, 0))

        // sort by x
        points = doubleArrayOf(19.0, 0.0, 12.0, 0.0, 5.0, 0.0)
        indices = arrayListOf(0, 1, 2)
        Delaunay.getSorterIndices(points, indices)
        assertThat(indices).isEqualTo(arrayListOf(2, 1, 0))

        points = doubleArrayOf(12.0, 2.0, 12.0, 1.0, 11.0, 0.0)
        indices = arrayListOf(0, 1, 2)
        Delaunay.getSorterIndices(points, indices)
        assertThat(indices).isEqualTo(arrayListOf(2, 1, 0))

    }

    fun DelaunayMainTest() {

        // Delaunay.from(array)
        var delaunay = Delaunay.from(PointD(0.0, 0.0), PointD(1.0, 0.0), PointD(0.0, 1.0), PointD(1.0, 1.0))
        assertThat(delaunay.coordinates).isEqualTo(doubleArrayOf(0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0))
        assertThat(delaunay.triangles).isEqualTo(intArrayOf(0, 2, 1, 2, 3, 1))
        assertThat(delaunay.halfEdges).isEqualTo(intArrayOf(-1, 5, -1, -1, -1, 1))
        assertThat(delaunay.inedges).isEqualTo(intArrayOf(2, 4, 0, 3))
        assertThat(delaunay.inedges).isEqualTo(intArrayOf(2, 4, 0, 3))
        assertThat(delaunay.neighbors(0).toList()).isEqualTo(listOf(1, 2))
        assertThat(delaunay.neighbors(1).toList()).isEqualTo(listOf(3, 2, 0))
        assertThat(delaunay.neighbors(2).toList()).isEqualTo(listOf(0, 1, 3))
        assertThat(delaunay.neighbors(3).toList()).isEqualTo(listOf(2, 1))

        // Delaunay.from(array) handles coincident points
        delaunay = Delaunay.from(arrayListOf(PointD(0.0, 0.0), PointD(1.0, 0.0), PointD(0.0, 1.0), PointD(1.0, 0.0)))
        assertThat(delaunay.inedges).isEqualTo(intArrayOf(2, 1, 0, -1))
        assertThat(delaunay.neighbors(0).toList()).isEqualTo(listOf(1, 2))
        assertThat(delaunay.neighbors(1).toList()).isEqualTo(listOf(2, 0))
        assertThat(delaunay.neighbors(2).toList()).isEqualTo(listOf(0, 1))
        assertThat(delaunay.neighbors(3).toList()).isEqualTo(emptyList<Int>())

        // Delaunay(iterable)
        delaunay = Delaunay(*sequence {
            yield(0.0)
            yield(0.0)
            yield(1.0)
            yield(0.0)
            yield(0.0)
            yield(1.0)
            yield(1.0)
            yield(1.0)
        }.toList().toDoubleArray())
        assertThat(delaunay.coordinates).isEqualTo(doubleArrayOf(0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0))
        assertThat(delaunay.triangles).isEqualTo(intArrayOf(0, 2, 1, 2, 3, 1))
        assertThat(delaunay.halfEdges).isEqualTo(intArrayOf(-1, 5, -1, -1, -1, 1))

        // Delaunay.from(iterable)
        delaunay = Delaunay.from(sequence {
            yield(PointD(0.0, 0.0))
            yield(PointD(1.0, 0.0))
            yield(PointD(0.0, 1.0))
            yield(PointD(1.0, 1.0))
        }.toList().toMutableList() as ArrayList<PointD>)
        assertThat(delaunay.coordinates).isEqualTo(doubleArrayOf(0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0))
        assertThat(delaunay.triangles).isEqualTo(intArrayOf(0, 2, 1, 2, 3, 1))
        assertThat(delaunay.halfEdges).isEqualTo(intArrayOf(-1, 5, -1, -1, -1, 1))

        // delaunay.find(x, y) returns the index of the cell that contains the specified point
        delaunay = Delaunay.from(PointD(0.0, 0.0), PointD(300.0, 0.0), PointD(0.0, 300.0), PointD(300.0, 300.0), PointD(100.0, 100.0))
        assertThat(delaunay.find(49.0, 49.0)).isEqualTo(0)
        assertThat(delaunay.find(51.0, 51.0)).isEqualTo(4)

        // delaunay.find(x, y) works with one or two points
        var points = arrayListOf(PointD(0.0, 1.0), PointD(0.0, 2.0))
        delaunay = Delaunay.from(points)
        assertThat(points[delaunay.find(0.0, -1.0)].y).isEqualTo(1.0)
        assertThat(points[delaunay.find(0.0, 2.2)].y).isEqualTo(2.0)
        delaunay.coordinates.fill(0.0)
        delaunay.update()
        assertThat(delaunay.find(0.0, -1.0)).isEqualTo(0)
        assertThat(delaunay.find(0.0, 1.2)).isEqualTo(0)

        // delaunay.find(x, y) works with collinear points
        points = arrayListOf(PointD(0.0, 1.0), PointD(0.0, 2.0), PointD(0.0, 4.0), PointD(0.0, 0.0), PointD(0.0, 3.0), PointD(0.0, 4.0), PointD(0.0, 4.0))
        delaunay = Delaunay.from(points)
        assertThat(points[delaunay.find(0.0, -1.0)].y).isEqualTo(0.0)
        assertThat(points[delaunay.find(0.0, 1.2)].y).isEqualTo(1.0)
        assertThat(points[delaunay.find(1.0, 1.9)].y).isEqualTo(2.0)
        assertThat(points[delaunay.find(-1.0, 3.3)].y).isEqualTo(3.0)
        assertThat(points[delaunay.find(10.0, 10.0)].y).isEqualTo(4.0)
        assertThat(points[delaunay.find(10.0, 10.0, 0)].y).isEqualTo(4.0)

        // delaunay.find(x, y) returns the index of the cell that contains the specified point
        delaunay = Delaunay(0.0, 0.0, 300.0, 0.0, 0.0, 300.0, 300.0, 300.0, 100.0, 100.0)
        assertThat(delaunay.find(49.0, 49.0)).isEqualTo(0)
        assertThat(delaunay.find(51.0, 51.0)).isEqualTo(4)

        // delaunay.find(x, y) works with one or two points
        points = arrayListOf(PointD(0.0, 1.0), PointD(0.0, 2.0))
        delaunay = Delaunay.from(points)
        assertThat(points[delaunay.find(0.0, -1.0)].y).isEqualTo(1)
        assertThat(points[delaunay.find(0.0, 2.2)].y).isEqualTo(2)
        delaunay.coordinates.fill(0.0)
        delaunay.update()
        assertThat(delaunay.find(0.0, -1.0)).isEqualTo(0)
        assertThat(delaunay.find(0.0, 1.2)).isEqualTo(0)

        // delaunay.find(x, y) works with collinear points
        points = arrayListOf(PointD(0.0, 1.0), PointD(0.0, 2.0), PointD(0.0, 4.0), PointD(0.0, 0.0), PointD(0.0, 3.0), PointD(0.0, 4.0), PointD(0.0, 4.0))
        delaunay = Delaunay.from(points)
        assertThat(points[delaunay.find(0.0, -1.0)].y).isEqualTo(0)
        assertThat(points[delaunay.find(0.0, 1.2)].y).isEqualTo(1)
        assertThat(points[delaunay.find(1.0, 1.9)].y).isEqualTo(2)
        assertThat(points[delaunay.find(-1.0, 3.3)].y).isEqualTo(3)
        assertThat(points[delaunay.find(10.0, 10.0)].y).isEqualTo(4)
        assertThat(points[delaunay.find(10.0, 10.0, 0)].y).isEqualTo(4)

        // delaunay.find(x, y) works with collinear points 2
        points = Array(120) {
            PointD(it * 4.0, it / 3.0 + 100.0)
        }.toCollection(ArrayList())
        delaunay = Delaunay.from(points)
        assertThat(delaunay.neighbors(2).toList()).isEqualTo(listOf(1, 3))

        // delaunay.find(x, y) works with collinear points (large)
        points = Array(2000) {
            PointD(it.toDouble() * it, it.toDouble() * it)
        }.toCollection(ArrayList())
        delaunay = Delaunay.from(points)
        assertThat(points[delaunay.find(0.0, -1.0)].y).isEqualTo(0)
        assertThat(points[delaunay.find(0.0, 1.2)].y).isEqualTo(1)
        assertThat(points[delaunay.find(3.9, 3.9)].y).isEqualTo(4)
        assertThat(points[delaunay.find(10.0, 9.5)].y).isEqualTo(9)
        assertThat(points[delaunay.find(10.0, 9.5, 0)].y).isEqualTo(9)
        assertThat(points[delaunay.find(1e6, 1e6)].y).isEqualTo(1e6)

        // delaunay.update() allows fast updates
        delaunay = Delaunay(0.0, 0.0, 300.0, 0.0, 0.0, 300.0, 300.0, 300.0, 100.0, 100.0)
        val circumcenters1 = delaunay.voronoi(RectD(-500.0, -500.0, 500.0, 500.0)).circumcenters
        for (i in delaunay.coordinates.indices) {
            delaunay.coordinates[i] = -delaunay.coordinates[i]
        }
        delaunay.update()
        val circumcenters2 = delaunay.voronoi(RectD(-500.0, -500.0, 500.0, 500.0)).circumcenters;
        assertThat(circumcenters1).isEqualTo(doubleArrayOf(150.0, -50.0, -50.0, 150.0, 250.0, 150.0, 150.0, 250.0))
        assertThat(circumcenters2).isEqualTo(doubleArrayOf(-150.0, 50.0, -250.0, -150.0, 50.0, -150.0, -150.0, -250.0))

        // delaunay.update() updates collinear points
        delaunay = Delaunay(*DoubleArray(250))
        assertThat(delaunay.collinear).isEmpty()
        for (i in delaunay.coordinates.indices) {
            delaunay.coordinates[i] = if (i % 2 != 0) i.toDouble() else 0.0
        }
        delaunay.update()
        assertThat(delaunay.collinear.size).isEqualTo(125)
        for (i in delaunay.coordinates.indices) {
            delaunay.coordinates[i] = Math.sin(i.toDouble())
        }
        delaunay.update()
        assertThat(delaunay.collinear).isEmpty()
        for (i in delaunay.coordinates.indices) {
            delaunay.coordinates[i] = if (i % 2 != 0) i.toDouble() else 0.0
        }
        delaunay.update()
        assertThat(delaunay.collinear.size).isEqualTo(125)
        for (i in delaunay.coordinates.indices) {
            delaunay.coordinates[i] = 0.0
        }
        delaunay.update()
        assertThat(delaunay.collinear).isEmpty()

        // delaunay.find(x, y) with coincident point
        delaunay = Delaunay(0.0, 0.0, 0.0, 0.0, 10.0, 10.0, 10.0, -10.0)
        assertThat(delaunay.find(100.0, 100.0)).isEqualTo(2)
        assertThat(delaunay.find(0.0, 0.0, 1)).isGreaterThan(-1)
        delaunay = Delaunay(*DoubleArray(1000 * 2) + doubleArrayOf(10.0, 10.0, 10.0, -10.0))
        assertThat(delaunay.find(0.0, 0.0, 1)).isGreaterThan(-1)

        // delaunay.find(x, y, i) traverses the convex hull
        delaunay = Delaunay(*delaunayValues.coordinates)
        assertThat(delaunay.find(49.0, 311.0)).isEqualTo(31)
        assertThat(delaunay.find(49.0, 311.0, 22)).isEqualTo(31)

        // delaunay.renderHull(path) is closed
        delaunay = Delaunay(0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0)
        val polygon = Path()
        assertThat((delaunay.renderHull(polygon) as Path).toString()).isEqualTo("M0.0,1.0L1.0,1.0L1.0,0.0L0.0,0.0Z")

        // delaunay.voronoi() uses the default bounds
        var voronoi = Delaunay.from(PointD(0.0, 0.0), PointD(1.0, 0.0), PointD(0.0, 1.0), PointD(1.0, 1.0)).voronoi()
        assertThat(voronoi.bound).isEqualTo(RectD(0.0, 0.0, 960.0, 500.0))

        // delaunay.voronoi(RectD[xmin, ymin, xmax, ymax]) uses the specified bounds
        voronoi = Delaunay.from(PointD(0.0, 0.0), PointD(1.0, 0.0), PointD(0.0, 1.0), PointD(1.0, 1.0)).voronoi(RectD(-1.0, -1.0, 2.0, 2.0))
        assertThat(voronoi.bound).isEqualTo(RectD(-1.0, -1.0, 2.0, 2.0))

        // delaunay.voronoi() returns the expected diagram
        voronoi = Delaunay.from(PointD(0.0, 0.0), PointD(1.0, 0.0), PointD(0.0, 1.0), PointD(1.0, 1.0)).voronoi()
        assertThat(voronoi.circumcenters).isEqualTo(doubleArrayOf(0.5, 0.5, 0.5, 0.5))
        assertThat(voronoi.vectors).isEqualTo(doubleArrayOf(0.0, -1.0, -1.0, 0.0, 1.0, 0.0, 0.0, -1.0, -1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0))

        // delaunay.voronoi() skips cells for coincident points
        voronoi = Delaunay.from(PointD(0.0, 0.0), PointD(1.0, 0.0), PointD(0.0, 1.0), PointD(1.0, 0.0)).voronoi(RectD(-1.0, -1.0, 2.0, 2.0))
        assertThat(voronoi.circumcenters).isEqualTo(doubleArrayOf(0.5, 0.5))
        assertThat(voronoi.vectors).isEqualTo(doubleArrayOf(0.0, -1.0, -1.0, 0.0, 1.0, 1.0, 0.0, -1.0, -1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0))

        // delaunay.voronoi() for collinear points
        voronoi = Delaunay(0.0, 0.0, 1.0, 0.0, -1.0, 0.0).voronoi(RectD(-1.0, -1.0, 2.0, 2.0))
        assertThat(voronoi.delaunay.neighbors(0).toList().sorted()).isEqualTo(listOf(1, 2))
        assertThat(voronoi.delaunay.neighbors(1).toList()).isEqualTo(listOf(0))
        assertThat(voronoi.delaunay.neighbors(2).toList()).isEqualTo(listOf(0))
    }

    companion object {
        /**
         * Load string from the raw folder using a resource id of the given file.
         * @param resources resource from the context
         * @param resId resource id of the file
         */
        fun loadStringFromRawResource(resources: Resources, resId: Int): String {
            val rawResource = resources.openRawResource(resId)
            val content = streamToString(rawResource)
            try {
                rawResource.close()
            } catch (e: IOException) {
                throw e
            }
            return content
        }

        /**
         * Read the file from the raw folder using input stream
         */
        private fun streamToString(inputStream: InputStream): String {
            var l: String?
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            try {
                while (bufferedReader.readLine().also { l = it } != null) {
                    stringBuilder.append(l)
                }
            } catch (e: IOException) {
            }
            return stringBuilder.toString()
        }
    }
}