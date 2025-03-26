package com.example.buildairesume

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.buildairesume.adapters.TemplateAdapter
import com.example.buildairesume.basetemplates.Template1
import com.example.buildairesume.basetemplates.Template2
import com.example.buildairesume.databinding.ActivityTemplateBinding
import com.example.buildairesume.models.TemplateItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.InputStream

class TemplateActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var binding: ActivityTemplateBinding
    private val templateList = mutableListOf<TemplateItem>()
    private lateinit var adapter: TemplateAdapter
    private var selectedTemplate: TemplateItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTemplateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("PDFGenerator", "Activity created, initializing views.")

        binding.rvTemplates.layoutManager = GridLayoutManager(this, 2)
        adapter = TemplateAdapter(templateList) { selectedItem, cardView ->
            selectedTemplate = selectedItem
            Toast.makeText(this, "Selected: ${selectedItem.imageName}", Toast.LENGTH_SHORT).show()
            Log.d("PDFGenerator", "Template selected: ${selectedItem.imageName}")
        }
        binding.rvTemplates.adapter = adapter

        loadTemplates()

        binding.btnGenerateResume.setOnClickListener {
            selectedTemplate?.let {
                Log.d("PDFGenerator", "Generate Resume button clicked.")
                launch(Dispatchers.IO) { generateResume(it.imageName) }
            } ?: showToast("Please select a template first.")
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("fromTemplateActivity", true)
            startActivity(intent)
        }
    }

    private fun loadTemplates() {
        Log.d("PDFGenerator", "Loading templates...")

        val images = listOf(
            "a1.png", "a2.png", "c1.png", "b1.png", "b2.png","d1.png"
        )

        for (image in images) {
            val resId = resources.getIdentifier(image.removeSuffix(".png"), "drawable", packageName)
            templateList.add(TemplateItem(image, resId))
            Log.d("PDFGenerator", "Loaded template: $image with resource ID: $resId")
        }

        adapter.notifyDataSetChanged()
    }

    private suspend fun generateResume(templateName: String) {
        Log.d("PDFGenerator", "Generating resume for template: $templateName")

        val jsonFileName = "templateJson/${templateName.replace(".png", ".json")}" // JSON filename
        val jsonObject = loadJsonFromAssets(jsonFileName)

        Log.d("PDFGenerator", "Loaded JSON file: $jsonFileName")
        Log.d("PDFGenerator", "$templateName")
        Log.d("PDFGenerator", "$jsonObject")
        if (templateName[1] == '1') {
            Log.d("PDFGenerator", "Using Template1")
            Template1().generateResume(this, jsonObject)
        } else if (templateName[1] == '2') {
            Log.d("PDFGenerator", "Using Template2")
            Template2().generateResume(this, jsonObject)
        } else {
            Log.e("PDFGenerator", "Invalid template selection")
            showToast("Invalid template selection")
        }
    }

    private fun loadJsonFromAssets(fileName: String): JSONObject {
        return try {
            Log.d("PDFGenerator", "Attempting to load JSON file: $fileName")
            val inputStream: InputStream = assets.open(fileName)
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()
            JSONObject(String(buffer, Charsets.UTF_8)).also {
                Log.d("PDFGenerator", "Successfully loaded JSON: $fileName")
            }
        } catch (e: Exception) {
            Log.e("PDFGenerator", "Error reading JSON: ${e.message}")
            JSONObject()
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            Log.d("PDFGenerator", "Toast displayed: $message")
        }
    }
}
