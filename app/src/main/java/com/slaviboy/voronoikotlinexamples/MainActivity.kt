package com.slaviboy.voronoikotlinexamples

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.slaviboy.delaunator.Delaunator
import com.slaviboy.voronoi.Delaunay
import com.slaviboy.voronoi.Path
import com.slaviboy.voronoi.Voronoi
import com.slaviboy.voronoikotlinexamples.animation.LloydRelaxationAnimationView
import com.slaviboy.voronoikotlinexamples.drawing.DelaunayView
import com.slaviboy.voronoikotlinexamples.drawing.VoronoiView
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.sql.Timestamp
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    lateinit var view: Any
    lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hideSystemUI()
        view = findViewById(R.id.canvas)
        button = findViewById(R.id.save_svg)

        button.setOnClickListener {
            requestPermission()

            // save SVG as file to local storage of the device
            val data = when (view) {
                is VoronoiView -> {
                    val path = (view as VoronoiView).voronoi.render(Path()) as Path
                    //(view as VoronoiView).voronoi.renderBounds(path) as Path
                    path.getSVG(Color.GREEN)
                }
                is DelaunayView -> {
                    val path = (view as DelaunayView).delaunay.render(Path()) as Path
                    path.getSVG(Color.GREEN)
                }
                else -> {
                    ""
                }
            }
            saveFileToLocalStorage(data)
        }
    }

    override fun onResume() {
        super.onResume()

        if (view is LloydRelaxationAnimationView) {
            (view as LloydRelaxationAnimationView).start()
        }
    }

    override fun onPause() {
        super.onPause()

        if (view is LloydRelaxationAnimationView) {
            (view as LloydRelaxationAnimationView).stop()
        }
    }

    /**
     * Hide the system UI
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    /**
     * Request the permission from the user to allow witting in the external storage
     * of the device.
     */
    fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                val permissionRequestCode = 1
                requestPermissions(permissions, permissionRequestCode)
            }
        }
    }

    /**
     * Save string as svg,text or json file to local storage on the phone with given folder location and file name
     * @param value string value that will be saved to the file
     * @param folderName name of the folder where the file will be located
     * @param fileName name of the file
     * @param fileFormat format of the file *svg, *txt or *json
     * @param putTimeStampToName whether to include the date stamp to the name of the file
     */
    fun saveFileToLocalStorage(
        value: String, folderName: String = "Files", fileName: String = "file", fileFormat: Int = FILE_FORMAT_SVG,
        putTimeStampToName: Boolean = false
    ) {

        try {
            val file: File
            val dir = File(Environment.getExternalStorageDirectory(), folderName)
            var success = true

            if (!dir.exists()) {
                success = dir.mkdirs()
            }

            if (success) {

                val date = Date()
                val timeStampName = if (putTimeStampToName) {
                    Timestamp(date.time).toString()
                } else {
                    ""
                }

                file = File(dir.absolutePath + File.separator + timeStampName + "$fileName.${fileFormatString[fileFormat]}")
                file.createNewFile()
            } else {
                return
            }

            // save the file stream
            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(value.toByteArray(Charset.defaultCharset()))
            fileOutputStream.close();

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    companion object {
        val fileFormatString: ArrayList<String> = arrayListOf("svg", "txt", "json")
        const val FILE_FORMAT_SVG = 0
        const val FILE_FORMAT_TXT = 1
        const val FILE_FORMAT_JSON = 2
    }

}