package com.codeNext.resumateAI.outputadapters

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
import com.codeNext.resumateAI.BuildConfig
import com.codeNext.resumateAI.R
import com.codeNext.resumateAI.databinding.ItemProjectOutputBinding
import com.codeNext.resumateAI.models.Project
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
    private val TAG = "AISaveDebug"
    private val totalTokensConsumed: Int = 0
    private val updatedDescriptions = MutableList(projects.size) { projects[it].output ?: "" }
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
            val model = BuildConfig.GEMINI_MODEL
            val generativeModel = GenerativeModel(model, apiKey)
            val numOfLines = if (itemCount > 2) "1" else "1-2"

            Log.d("PDFGeneration", "Number of projects: $itemCount")
            Log.d("PDFGeneration", "Number of lines in project output: $numOfLines")

            val prompt = """
        Rewrite the following project description in a concise and professional tone suitable for a resume.
        Keep it clear, impactful, and formatted as exactly two bullet points, each of $numOfLines line(s).
        Ensure there are no extra line breaks between bullet points.
        
        - Project Title: ${project.title}
        - Description: ${project.description}
        - Tech Stack: ${project.techStack}

        Format the response in **third-person**, use a **consistent bullet point format**, and **omit any headings**.
    """.trimIndent()

            // 🌟 Show Loading Animation & Disable Input
            binding.projectOutputCard.visibility = View.INVISIBLE
            binding.lottieAnim.visibility = View.VISIBLE
            binding.ivEdit.isEnabled = false

            coroutineScope.launch {
                val outputText = generateAIResponse(generativeModel, prompt)
                val position = absoluteAdapterPosition // Get position safely

                // --- FIX: Update the adapter's data source ---
                if (position != RecyclerView.NO_POSITION) {
                    if (position < updatedDescriptions.size) {
                        Log.d(
                            TAG,
                            "[ProjectAdapter] Updating updatedDescriptions at pos $position with: $outputText"
                        ) // <-- Log update
                        updatedDescriptions[position] = outputText
                    } else {
                        Log.e(
                            TAG,
                            "[ProjectAdapter] Position $position out of bounds for updatedDescriptions (size ${updatedDescriptions.size})"
                        )
                    }
                }
                // --- End of FIX ---

                withContext(Dispatchers.Main) {
                    // Check if the ViewHolder is still valid for this position
                    if (absoluteAdapterPosition == position) {
                        Log.d(
                            TAG,
                            "[ProjectAdapter] Setting UI text for pos $position: $outputText"
                        ) // <-- Log UI set
                        binding.etProjectDescription.setText(outputText.trim())
                        binding.projectOutputCard.visibility = View.VISIBLE
                        binding.lottieAnim.visibility = View.GONE
                        binding.ivEdit.isEnabled = true
                    } else {
                        Log.w(
                            TAG,
                            "[ProjectAdapter] ViewHolder at pos $position is no longer valid (current: $absoluteAdapterPosition) during UI update."
                        )
                    }
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
        Log.d(
            TAG,
            "[ProjectAdapter] getUpdatedProjects called. Current updatedDescriptions: ${
                updatedDescriptions.joinToString(" | ")
            }"
        ) // <-- Log retrieval start
        return projects.mapIndexed { index, project ->
            val outputFromList = updatedDescriptions.getOrNull(index)
            Log.d(
                TAG,
                "[ProjectAdapter] Mapping project index $index ('${project.title}'). Output from list: ${outputFromList ?: "NULL"}"
            ) // <-- Log mapping
            project.copy(
                output = outputFromList ?: project.output
            ) // Use existing output if list entry is null
        }
    }


}
