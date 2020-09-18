package com.slaviboy.voronoi

import android.content.Context
import com.slaviboy.delaunator.Delaunator.PointD
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class VoronoiUnitTest {

    lateinit var voronoiValues: VoronoiValues

    /**
     * Class for holding the expected value for the voronoi diagram from the json file
     */
    class VoronoiValues(
        var coordinates: DoubleArray, var circumcenters: DoubleArray, var vectors: DoubleArray, var delaunay_triangles: IntArray,
        var delaunay_inedges: IntArray, var delaunay_hull: IntArray, var delaunay_halfEdges: IntArray,
        var method_render: String, var method_renderBound: String, var method_renderCell: String,
        var lloyd_relaxation: DoubleArray, var cells_centers: ArrayList<Double>, var cells_line_coordinates: ArrayList<Double>,
    )

    @Test
    fun MainTest() {

        val gson = Gson()
        val context: Context = ApplicationProvider.getApplicationContext()

        // load json file with expected test values
        val jsonDelaunayValues = DelaunayUnitTest.loadStringFromRawResource(context.resources, R.raw.voronoi_values)
        voronoiValues = gson.fromJson(jsonDelaunayValues, VoronoiValues::class.java)

        VoronoiMainTest()
        VoronoiValueTest()
    }

    fun VoronoiValueTest() {

        var delaunay = Delaunay(*voronoiValues.coordinates)
        var voronoi = Voronoi(delaunay)

        // check voronoi expected values
        assertThat(voronoi.bound).isEqualTo(Voronoi.RectD(0.0, 0.0, 960.0, 500.0))
        assertThat(voronoi.circumcenters).isEqualTo(voronoiValues.circumcenters)
        assertThat(voronoi.vectors).isEqualTo(voronoiValues.vectors)

        // check the delaunay
        assertThat(voronoi.delaunay.coordinates).isEqualTo(voronoiValues.coordinates)
        assertThat(voronoi.delaunay.triangles).isEqualTo(voronoiValues.delaunay_triangles)
        assertThat(voronoi.delaunay.inedges).isEqualTo(voronoiValues.delaunay_inedges)
        assertThat(voronoi.delaunay.hull).isEqualTo(voronoiValues.delaunay_hull)
        assertThat(voronoi.delaunay.halfEdges).isEqualTo(voronoiValues.delaunay_halfEdges)

        // test Lloyd relaxation expected values after 66 iterations
        voronoi.relax(66)
        assertThat(voronoi.delaunay.coordinates).isEqualTo(voronoiValues.lloyd_relaxation)


        // check voronoi render methods
        delaunay = Delaunay(*voronoiValues.coordinates)
        voronoi = Voronoi(delaunay)

        var path: Path = voronoi.render() as Path
        assertThat(path.data).isEqualTo(voronoiValues.method_render)

        path = voronoi.renderBounds() as Path
        assertThat(path.data).isEqualTo(voronoiValues.method_renderBound)

        path = voronoi.renderCell(2) as Path
        assertThat(path.data).isEqualTo(voronoiValues.method_renderCell)

        // center values of the polygons(cells)
        val centers = voronoi.getCellsCenterCoordinates()
        assertThat(centers).isEqualTo(voronoiValues.cells_centers)

        // the line coordinates of all polygons(cells)
        val coordinates = voronoi.getLinesCoordinates()
        assertThat(coordinates).isEqualTo(voronoiValues.cells_line_coordinates)
    }

    fun VoronoiMainTest() {

        // voronoi.renderCell(i, context) is a noop for coincident points
        var voronoi = Delaunay(0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0).voronoi(-1.0, -1.0, 2.0, 2.0)
        assertThat(voronoi.renderCell(3, com.slaviboy.voronoi.Path()) as com.slaviboy.voronoi.Path).isEqualTo(com.slaviboy.voronoi.Path())

        // voronoi.renderCell(i, context) handles midpoint coincident with circumcenter
        voronoi = Delaunay(0.0, 0.0, 1.0, 0.0, 0.0, 1.0).voronoi(-1.0, -1.0, 2.0, 2.0)
        assertThat(voronoi.renderCell(0).toString()).isEqualTo("M-1.0,-1.0L0.5,-1.0L0.5,0.5L-1.0,0.5Z")
        assertThat(voronoi.renderCell(1).toString()).isEqualTo("M2.0,-1.0L2.0,2.0L0.5,0.5L0.5,-1.0Z")
        assertThat(voronoi.renderCell(2).toString()).isEqualTo("M-1.0,2.0L-1.0,0.5L0.5,0.5L2.0,2.0Z")

        // voronoi.contains(i, x, y) is false for coincident points
        voronoi = Delaunay(0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0).voronoi(-1.0, -1.0, 2.0, 2.0)
        assertThat(voronoi.contains(3, 1.0, 0.0)).isFalse()
        assertThat(voronoi.contains(1, 1.0, 0.0)).isTrue()

        // voronoi.update() updates the voronoi
        var delaunay = Delaunay(0.0, 0.0, 300.0, 0.0, 0.0, 300.0, 300.0, 300.0, 100.0, 100.0)
        voronoi = delaunay.voronoi(-500.0, -500.0, 500.0, 500.0)
        for (i in delaunay.coordinates.indices) {
            delaunay.coordinates[i] = 10 - delaunay.coordinates[i]
        }
        val expectedP = arrayListOf(
            -500.0, 500.0,
            -500.0, -140.0,
            -240.0, -140.0,
            -140.0, 60.0,
            -140.0, 500.0,
            -500.0, 500.0
        )
        var p = voronoi.update().getCellCoordinates(1) // correct after voronoi.update
        assertThat(p.coordinates).isEqualTo(expectedP)

        // voronoi.update() updates a degenerate voronoi
        var pts = doubleArrayOf(10.0, 10.0, -290.0, 10.0, 10.0, -290.0, -290.0, -290.0, -90.0, -90.0)
        delaunay = Delaunay(*DoubleArray(pts.size))
        voronoi = delaunay.voronoi(-500.0, -500.0, 500.0, 500.0)
        assertThat(voronoi.getCellCoordinates(0).coordinates).isEqualTo(
            arrayListOf(
                500.0, -500.0,
                500.0, 500.0,
                -500.0, 500.0,
                -500.0, -500.0,
                500.0, -500.0
            )
        )
        assertThat(voronoi.getCellCoordinates(1).coordinates).isEmpty()

        for (i in delaunay.coordinates.indices) {
            delaunay.coordinates[i] = pts[i]
        }
        p = voronoi.update().getCellCoordinates(1)
        assertThat(p.coordinates).isEqualTo(expectedP)

        // zero-length edges are removed
        val voronoi1 = Delaunay(50.0, 10.0, 10.0, 50.0, 10.0, 10.0, 200.0, 100.0).voronoi(40.0, 40.0, 440.0, 180.0)
        assertThat(voronoi1.getCellCoordinates(0).coordinates.size).isEqualTo(4 * 2)
        val voronoi2 = Delaunay(10.0, 10.0, 20.0, 10.0).voronoi(0.0, 0.0, 30.0, 20.0)
        assertThat(voronoi2.getCellCoordinates(0).coordinates).isEqualTo(
            arrayListOf(
                15.0, 20.0,
                0.0, 20.0,
                0.0, 0.0,
                15.0, 0.0,
                15.0, 20.0
            )
        )

        // voronoi neighbors are clipped
        voronoi = Delaunay(300.0, 10.0, 200.0, 100.0, 300.0, 100.0, 10.0, 10.0, 350.0, 200.0, 350.0, 400.0).voronoi(0.0, 0.0, 500.0, 150.0)
        assertThat(voronoi.neighbors(0).toList().sorted()).isEqualTo(listOf(1, 2))
        assertThat(voronoi.neighbors(1).toList().sorted()).isEqualTo(listOf(0, 2))
        assertThat(voronoi.neighbors(2).toList().sorted()).isEqualTo(listOf(0, 1, 4))
        assertThat(voronoi.neighbors(3).toList().sorted()).isEmpty()
        assertThat(voronoi.neighbors(4).toList().sorted()).isEqualTo(listOf(2))
        assertThat(voronoi.neighbors(5).toList().sorted()).isEmpty()

        // unnecessary points on the corners are avoided (#88)
        val points = arrayListOf(
            arrayListOf(PointD(289.0, 25.0), PointD(3.0, 22.0), PointD(93.0, 165.0), PointD(282.0, 184.0), PointD(65.0, 89.0)),
            arrayListOf(PointD(189.0, 13.0), PointD(197.0, 26.0), PointD(47.0, 133.0), PointD(125.0, 77.0), PointD(288.0, 15.0)),
            arrayListOf(PointD(44.0, 42.0), PointD(210.0, 193.0), PointD(113.0, 103.0), PointD(185.0, 43.0), PointD(184.0, 37.0))
        )
        val lengths = arrayListOf(
            arrayListOf(6, 4, 6, 5, 6),
            arrayListOf(4, 6, 5, 6, 5),
            arrayListOf(5, 5, 7, 5, 6)
        )
        for (i in 0 until points.size) {
            val voronoi = Delaunay.from(points[i]).voronoi(0.0, 0.0, 290.0, 190.0)
            assertThat(voronoi.getCellsCoordinatesSequence().toList().map {
                it.coordinates.size / 2
            }).isEqualTo(lengths[i])
        }

        // a degenerate triangle is avoided
        val pts2 = arrayListOf(
            PointD(424.75, 253.75), PointD(424.75, 253.74999999999997), PointD(407.17640687119285, 296.17640687119285), PointD(364.75, 313.75),
            PointD(322.32359312880715, 296.17640687119285), PointD(304.75, 253.75), PointD(322.32359312880715, 211.32359312880715), PointD(364.75, 193.75),
            PointD(407.17640687119285, 211.32359312880715), PointD(624.75, 253.75), PointD(607.1764068711929, 296.17640687119285), PointD(564.75, 313.75),
            PointD(522.3235931288071, 296.17640687119285), PointD(504.75, 253.75), PointD(564.75, 193.75)
        )
        voronoi = Delaunay.from(pts2).voronoi(10.0, 10.0, 960.0, 500.0)
        assertThat(voronoi.getCellCoordinates(0).coordinates.toList().size).isEqualTo(4 * 2)

        // cellPolygons filter out empty cells and have the cell index as a property
        pts = doubleArrayOf(0.0, 0.0, 3.0, 3.0, 1.0, 1.0, -3.0, -2.0)
        voronoi = Delaunay(*pts).voronoi(0.0, 0.0, 2.0, 2.0)
        assertThat(voronoi.getCellsCoordinatesSequence().toList()).isEqualTo(
            listOf(
                Voronoi.CellValues(arrayListOf(0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0), 0),
                Voronoi.CellValues(arrayListOf(0.0, 1.0, 1.0, 0.0, 2.0, 0.0, 2.0, 2.0, 0.0, 2.0, 0.0, 1.0), 2)
            )
        )
    }
}