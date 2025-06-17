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
import com.codeNext.resumateAI.databinding.ItemExperienceOutputBinding
import com.codeNext.resumateAI.models.Experience
import com.codeNext.resumateAI.utils.FirebaseUsageManager
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExperienceOutputAdapter(
    private val experiences: List<Experience>,
    private val context: Context,
    private val canGenerate: Boolean,
) : RecyclerView.Adapter<ExperienceOutputAdapter.ExperienceViewHolder>() {
    private val TAG = "AISaveDebug"
    private val updatedDescriptions = MutableList(experiences.size) { experiences[it].output ?: "" }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    inner class ExperienceViewHolder(val binding: ItemExperienceOutputBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(experience: Experience) {

            binding.etExperienceDescription.setText(experience.description)
            // Set experience details
            binding.tvExperienceTitle.text = "${experience.position} at ${experience.companyName}"
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val endDate = sdf.parse(experience.endDate)
            val currentDate = Calendar.getInstance().time
            binding.tvExperienceDate.text =
                if (experience.isEndDateEmpty() || endDate.after(currentDate)) {
                    "${experience.startDate} - till date"
                } else {
                    "${experience.startDate} - ${experience.endDate}"
                }
            var isEditing = false

            binding.ivEdit.setOnClickListener {
                isEditing = !isEditing
                binding.etExperienceDescription.isEnabled = isEditing
                binding.etExperienceDescription.requestFocus()

                if (isEditing) {
                    binding.ivEdit.setImageResource(R.drawable.icon_tick) // Edit mode image
                } else {
                    binding.ivEdit.setImageResource(R.drawable.icon_write) // Default image
                }
            }


            binding.etExperienceDescription.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(editable: Editable) {
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

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

            })



            // Call AI Model if teh internet is available
            if (isInternetAvailable(context)) {

                if(canGenerate) {
                    modelCall(experience, binding)
                }
                else{
                    binding.etExperienceDescription.apply {
                        setText("Daily AI generation limit reached. Please check back tomorrow for more.")
                    }
                }
            } else {
                binding.etExperienceDescription.apply {
                    setText("AI generation Failed, Check Internet Connection")
                }
            }
        }

        private fun modelCall(experience: Experience, binding: ItemExperienceOutputBinding) {
            val apiKey = BuildConfig.GEMINI_API_KEY
            val model = BuildConfig.GEMINI_MODEL
            val generativeModel = GenerativeModel(model, apiKey)
            val numOfLines = if (itemCount > 2) "1" else "2-3"

            Log.d("PDFGeneration", "Item count: $itemCount")
            Log.d("PDFGeneration", "Number of lines in experience output: $numOfLines")

            val prompt = """
        Rewrite the following experience description in a concise and professional tone suitable for a resume.
        Keep it clear, impactful, and formatted as exactly two bullet points, each of $numOfLines lines.
        Ensure there are no extra line breaks between bullet points.
        
        - Position: ${experience.position}
        - Company: ${experience.companyName}
        - Description: ${experience.description}
        
        Format the response in **third-person**, use a **consistent bullet point format**, and **omit any headings**.
    """.trimIndent()

            // 🌟 Show Loading Animation & Disable Input
//            binding.etExperienceDescription.visibility = View.INVISIBLE
            binding.etExperienceOutputCard.visibility = View.INVISIBLE
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
                            "[ExperienceAdapter] Updating updatedDescriptions at pos $position with: $outputText"
                        ) // <-- Log update
                        updatedDescriptions[position] = outputText
                    } else {
                        Log.e(
                            TAG,
                            "[ExperienceAdapter] Position $position out of bounds for updatedDescriptions (size ${updatedDescriptions.size})"
                        )
                    }
                }
                // --- End of FIX ---

                withContext(Dispatchers.Main) {
                    // Check if the ViewHolder is still valid for this position
                    if (absoluteAdapterPosition == position) {
                        Log.d(
                            TAG,
                            "[ExperienceAdapter] Setting UI text for pos $position: $outputText"
                        ) // <-- Log UI set
                        binding.etExperienceDescription.setText(outputText.trim())
//                        binding.etExperienceDescription.visibility = View.VISIBLE
                        binding.etExperienceOutputCard.visibility = View.VISIBLE
                        binding.lottieAnim.visibility = View.GONE
                        binding.ivEdit.isEnabled = true
                    } else {
                        Log.w(
                            TAG,
                            "[ExperienceAdapter] ViewHolder at pos $position is no longer valid (current: $absoluteAdapterPosition) during UI update."
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

                    // Replace "*" at the start of lines with bullet points "•"
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExperienceViewHolder {
        val binding =
            ItemExperienceOutputBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExperienceViewHolder(binding)
    }

    override fun getItemCount(): Int = experiences.size

    override fun onBindViewHolder(holder: ExperienceViewHolder, position: Int) {
        holder.bind(experiences[position])
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun getUpdatedExperiences(): List<Experience> {
        Log.d(
            TAG,
            "[ExperienceAdapter] getUpdatedExperiences called. Current updatedDescriptions: ${
                updatedDescriptions.joinToString(" | ")
            }"
        ) // <-- Log retrieval start
        return experiences.mapIndexed { index, experience ->
            val outputFromList = updatedDescriptions.getOrNull(index)
            Log.d(
                TAG,
                "[ExperienceAdapter] Mapping experience index $index ('${experience.position}'). Output from list: ${outputFromList ?: "NULL"}"
            ) // <-- Log mapping
            experience.copy(
                output = outputFromList ?: experience.output
            ) // Use existing output if list entry is null
        }
    }
}
