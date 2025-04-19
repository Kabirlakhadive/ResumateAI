package com.codeNext.resumateAI

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.codeNext.resumateAI.basetemplates.Template2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class GeneratePDFActivity : AppCompatActivity() {
    private val STORAGE_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_generate_pdfactivity)

        checkStoragePermission()
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            } else {
                generateResume()
            }
        } else {
            generateResume()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            generateResume()
        } else {
            showToast("Storage Permission Denied")
        }
    }

    private fun generateResume() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val template = Template2()
                val jsonObject = loadJsonFromAssets("1b.json")

                template.generateResume(this@GeneratePDFActivity, jsonObject)

                launch(Dispatchers.Main) { showToast("Resume generated successfully!") }
            } catch (e: Exception) {
                Log.e("GeneratePDF", "Error generating PDF: ${e.message}")
                launch(Dispatchers.Main) { showToast("Failed to generate resume") }
            }
        }
    }

    private fun loadJsonFromAssets(fileName: String): JSONObject {
        return try {
            assets.open("templateJson/$fileName").use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                Log.d("JSON", "Loaded JSON: $jsonString")
                JSONObject(jsonString)
            }
        } catch (e: Exception) {
            Log.e("JSON", "Error reading JSON: ${e.message}")
            JSONObject()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
