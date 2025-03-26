package com.example.buildairesume.outputadapters

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.buildairesume.BuildConfig
import com.example.buildairesume.R
import com.example.buildairesume.databinding.ItemProjectOutputBinding
import com.example.buildairesume.models.Project
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProjectOutputAdapter(
    private val projects: List<Project>,
    private val context: Context // Added context for connectivity check
) : RecyclerView.Adapter<ProjectOutputAdapter.ProjectViewHolder>() {
    private val totalTokensConsumed: Int = 0
    private val updatedDescriptions = MutableList(projects.size) { "" } // ✅ Store user input
    private val coroutineScope = CoroutineScope(Dispatchers.IO) // Custom coroutine scope

    inner class ProjectViewHolder(val binding: ItemProjectOutputBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(project: Project) {
            binding.etProjectDescription.setText(project.title)

            var isEditing = false

            binding.ivEdit.setOnClickListener {
                isEditing = !isEditing
                binding.etProjectDescription.isEnabled = isEditing
                binding.etProjectDescription.requestFocus()

                if (isEditing) {
                    binding.ivEdit.setImageResource(R.drawable.icon_tick) // Edit mode image
                } else {
                    binding.ivEdit.setImageResource(R.drawable.icon_write) // Default image
                }
            }

            binding.tvProjectTitle.text = project.title


            binding.etProjectDescription.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(editable: Editable?) {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        updatedDescriptions[position] = editable.toString()
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            // Call AI model only if the internet is available
            if (isInternetAvailable(context)) {
                modelCall(project, binding)
            } else {
                binding.etProjectDescription.apply {
                    setText("AI generation Failed, Check Internet Connection")
                    setTextColor(Color.RED)
                }

            }
        }

        private fun modelCall(project: Project, binding: ItemProjectOutputBinding) {
            val apiKey = BuildConfig.GEMINI_API_KEY

            val generativeModel = GenerativeModel("gemini-2.0-flash", apiKey)
            val numOfLines = if (itemCount > 2) "1" else "1-2"

            Log.d("PDFGeneration", "Number of projects: $itemCount")
            Log.d("PDFGeneration", "Number of lines in project output: $numOfLines")

            val prompt = """
        Rewrite the following project description in a concise and professional tone suitable for a resume.
        Keep it clear, impactful, and formatted as exactly two bullet points, each of $numOfLines line.
        Ensure there are no extra line breaks between bullet points.
        
        - Project Title: ${project.title}
        - Description: ${project.description}
        - Tech Stack: ${project.techStack}

        Format the response in **third-person**, use a **consistent bullet point format**, and **omit any headings**.
    """.trimIndent()

            // 🌟 Show Loading Animation & Disable Input
            binding.etProjectDescription.visibility = View.INVISIBLE
            binding.lottieAnim.visibility = View.VISIBLE
            binding.ivEdit.isEnabled = false

            coroutineScope.launch {
                val outputText = generateAIResponse(generativeModel, prompt)

                withContext(Dispatchers.Main) {
                    binding.etProjectDescription.setText(outputText)
                    binding.etProjectDescription.visibility = View.VISIBLE
                    binding.lottieAnim.visibility = View.GONE
                    binding.ivEdit.isEnabled = true
                }
            }
        }

        /**
         * Generates AI response with error handling and retry mechanism.
         */
        private suspend fun generateAIResponse(
            model: GenerativeModel,
            prompt: String
        ): String {
            return withContext(Dispatchers.IO) {
                try {
                    val response = model.generateContent(prompt)
                    var outputText = response.text ?: "No response from AI"
                    Log.d("AI Response", "Full Response: $response")

                    // 🔥 Replace "*" at the start of lines with bullet points "•"
                    outputText = outputText.replace(Regex("^\\* ", RegexOption.MULTILINE), "• ")

                    outputText
                } catch (e: Exception) {
                    Log.e("AI", "Error generating AI content", e)

                    // Retry once before failing
                    delay(1000)
                    try {
                        val retryResponse = model.generateContent(prompt)
                        retryResponse.text ?: "AI generation failed. Check internet connection."
                    } catch (retryException: Exception) {
                        Log.e("AI", "Retry failed", retryException)
                        "AI generation failed. Check internet connection."
                    }
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding =
            ItemProjectOutputBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(projects[position])
    }

    override fun getItemCount(): Int = projects.size

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ✅ Fetch the latest descriptions stored in `updatedDescriptions`
    fun getUpdatedProjects(): List<Project> {
        return projects.mapIndexed { index, project ->
            project.copy(output = updatedDescriptions[index])
        }
    }


}
