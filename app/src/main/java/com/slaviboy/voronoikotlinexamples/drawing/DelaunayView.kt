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
import com.slaviboy.delaunator.Delaunator
import com.slaviboy.voronoi.Delaunay
import com.slaviboy.voronoikotlinexamples.drawing.VoronoiView.Companion.afterMeasured

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
    private lateinit var delaunay: Delaunay             // delaunay object for center points
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
        var points = FloatArray(numberOfRandomPoints * 2)
        for (i in 0 until numberOfRandomPoints) {
            points[i * 2] = (Math.random() * width - 1).toFloat()
            points[i * 2 + 1] = (Math.random() * height - 1).toFloat()
        }

        //points = floatArrayOf(433.2546725094038f,52.8908547064485f,166.3545612062273f,209.87463611231803f,455.81662519849027f,431.47575387627006f,202.19411255453824f,110.2066418258287f,126.99577355326792f,290.30904532121104f,6.746130602716178f,362.4418512116239f,293.7933020344226f,308.9930458681651f,369.08867871007095f,90.02672032266057f,179.27923559339635f,379.05411715486537f,276.7084843088718f,0.7107637017101087f,51.62530132543537f,78.2173697170224f,335.9765320173444f,22.327370827331592f,151.1487534263568f,411.5529579434867f,20.23079409840676f,521.3883905801861f,408.99227603583006f,386.51765612241604f,217.46070213905574f,64.54100425498643f,149.8079190386468f,425.54765435737255f,249.42663673411522f,672.6948134751584f,216.51294959883438f,389.2040248362078f,62.426304974433506f,630.2859701007798f,278.1619460176433f,487.4403415577839f,413.10739506852906f,617.9549442776063f,353.27336504053727f,182.39370388686496f,456.1919428911278f,198.7801209163979f,189.146110445899f,226.9884690518588f,246.0931605104256f,224.00294609510598f,202.22303793404552f,390.0974584088756f,194.3750163931499f,109.20292681210667f,71.21895327510515f,19.10501378060104f,74.99702570004158f,436.7608871845866f,188.67043891231273f,356.3874993556364f,299.83614862657333f,31.52100660024006f,90.15996786929304f,35.02137015285467f,424.4803546403341f,317.25906901119146f,267.8488602762746f,186.63818237440893f,369.3556622552127f,454.7372377388478f,196.6102997417131f,316.27530583521786f,433.07241287765487f,438.74226810629233f,115.30657738535851f,517.0997097146699f,349.0837253620376f,592.3201325208983f,78.49733167584532f,173.87353948595918f,430.5993776906461f,72.59655341402517f,166.1363782904652f,128.73099709947283f,243.45023098281405f,23.64520694559439f,275.66952337707403f,163.9224054620477f,152.31525411023932f,670.9600571028959f,367.8491828315334f,405.58586896407473f,48.36320519362779f,528.0971849412514f,95.43067529627152f,484.0198725995759f,144.30964175555755f,202.29244778622316f,42.678113158760844f,315.4202983101348f,163.09216764375452f,15.751960283648367f,434.63634718784334f,230.60897357823916f,33.21771397961303f,66.30633042361157f,78.9012987914285f,105.84347920245595f,163.5853589209575f,171.9408903679255f,401.3097242015391f,227.59978273637486f,434.8692164462444f,120.88074497807835f,73.68936116917347f,244.39278013493183f,349.2952968985989f,272.0498776838f,273.36990190890697f,325.24265012278204f,318.5352664529526f,188.07386359727965f,189.70576510015195f,424.59304570640745f,459.86229294134483f,547.900171033992f,148.5889694595677f,581.7956257293449f,90.41539996932403f,39.550215073257284f,195.27839697646158f,595.2517411993335f,349.2825048708137f,442.85843274626717f,157.58990965597152f,66.7302496414524f,140.54414925836596f,545.5921627473006f,200.6637339128897f,151.8072346526287f,22.876646086219523f,532.4207822179204f,136.75767228134742f,646.0897239658595f,103.62193389066938f,654.8017138740909f,408.3012253067644f,51.86587099066563f,462.08472513328553f,4.002767426813563f,396.86491825007937f,426.66286757353424f,0.5556936401440282f,277.8285089078997f,134.44554434815248f,467.04390318129464f,198.37157286854875f,8.556514217789895f,393.1078566674154f,137.88358675275282f,447.0728901454396f,432.0936415045903f,396.8659185129727f,419.31217540842033f,109.62921828954812f,337.88126173580076f,78.71477649154218f,425.5254430814115f,85.37193155072607f,425.06843107769856f,77.61672845185338f,256.3721348416527f,286.20774178334324f,475.5435132538417f,349.67082423026307f,214.53477363391715f,32.710360541115584f,328.86497441736947f,271.6332995258171f,624.5348119910633f,138.45183538505222f,596.9984502096078f,358.52446097041525f,105.51885599817525f,460.4288535571014f,11.018301086891888f,203.20956896054415f,654.0366007065701f,259.4306981614904f,132.34301166979452f,176.4219987984008f,479.317577316948f,4.49025571428372f,195.23242931689867f,125.26760321152196f,315.98435553941965f,254.85933508831818f,298.1566456623435f)
        delaunay = Delaunay(*points)
    }

    fun generateTrianglesWithPoints(canvas: Canvas) {

        // for each center point
        for (i in 0 until delaunay.triangles.size / 3) {

            path.reset()
            delaunay.renderTriangle(i, path)

            val triangleCenter = delaunay.triangleCenter(i)                     // center of the voronoi shape
            val distance = distance(viewCenter, triangleCenter)                 // distance from the center screen to the triangle center
            val distanceInRange = (distance / halfDiagonalWidth).toFloat()      // fit the distance in range between [0, 1]
            val color = gradientPicker.getColorFromGradient(distanceInRange)    // get the color corresponding to the distance

            // fill the cell with the corresponding color
            paint.color = color
            paint.style = Paint.Style.FILL
            canvas.drawPath(path, paint)

            // stroke the cell with black color
            paint.color = Color.BLACK
            paint.style = Paint.Style.STROKE
            canvas.drawPath(path, paint)
        }

        // render points
        for (i in 0 until delaunay.coordinates.size / 2) {

            // original point
            paint.color = Color.GREEN
            paint.style = Paint.Style.FILL
            canvas.drawCircle(
                delaunay.coordinates[i * 2].toFloat(),
                delaunay.coordinates[i * 2 + 1].toFloat(),
                4.0f,
                paint
            )
        }
    }

    fun generateTrianglesWithCenter(canvas: Canvas) {

        paint.color = Color.RED
        paint.style = Paint.Style.FILL
        for (i in 0 until delaunay.triangles.size / 3) {
            val triangleCenter = delaunay.triangleCenter(i)
            canvas.drawCircle(triangleCenter.x.toFloat(), triangleCenter.y.toFloat(), 3f, paint)
        }

        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        path.reset()
        delaunay.render(path)
        canvas.drawPath(path, paint)
    }

    fun renderAll(canvas: Canvas) {

        path.reset()
        delaunay.renderPoints(3.0, path)
        delaunay.render(path)
        delaunay.renderHull(path)
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        canvas.drawPath(path, paint)
    }

    override fun onDraw(canvas: Canvas) {

        renderAll(canvas)
        //generateTrianglesWithPoints(canvas)
        //generateTrianglesWithCenter(canvas)
    }
}