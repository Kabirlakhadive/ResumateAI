package com.example.buildairesume

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
// import android.view.MotionEvent // No longer needed for ScrollView listener
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.buildairesume.dao.ExperienceDao
import com.example.buildairesume.dao.ProjectDao
import com.example.buildairesume.dao.SkillsDao
import com.example.buildairesume.dao.UserProfileDao
import com.example.buildairesume.databinding.ActivityOutputBinding
import com.example.buildairesume.models.Achievement
import com.example.buildairesume.models.Certification
import com.example.buildairesume.models.Education
import com.example.buildairesume.models.Experience
import com.example.buildairesume.models.Project
import com.example.buildairesume.models.Skills
import com.example.buildairesume.models.UserProfile
import com.example.buildairesume.outputadapters.ExperienceOutputAdapter
import com.example.buildairesume.outputadapters.ProjectOutputAdapter
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// import kotlin.math.abs // No longer needed for snapping calculation
import kotlin.random.Random


class OutputActivity : AppCompatActivity() {
    private lateinit var projectDao: ProjectDao
    private lateinit var userProfileDao: UserProfileDao
    private lateinit var experienceDao: ExperienceDao
    private lateinit var userProfile: UserProfile // Consider making nullable if it might not exist
    private lateinit var binding: ActivityOutputBinding
    private lateinit var projectAdapter: ProjectOutputAdapter
    private lateinit var experienceAdapter: ExperienceOutputAdapter
    private lateinit var skillsDao: SkillsDao
    // private lateinit var experience: Experience // These seem unused, can be removed if so
    // private lateinit var project: Project // These seem unused, can be removed if so
    private lateinit var database: UserData
    private val TAG = "AISaveDebug"
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // --- State variable to track the current card ---
    private var currentCardIndex = 0
    private lateinit var cardViews: List<View> // To hold references to the main cards

    // --- Constants for card indices (optional but improves readability) ---
    companion object {
        private const val OBJECTIVE_INDEX = 0
        private const val PROJECTS_INDEX = 1
        private const val EXPERIENCE_INDEX = 2
        private const val TOTAL_CARDS = 3 // Update if you add more cards
    }
    // --- ---


    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOutputBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = UserData.getInstance(this)

        // --- Initialize the list of card views ---
        cardViews = listOf(
            binding.cvObjectiveOutput,
            binding.cvProjectOutput,
            binding.cvExperienceOutput
        )

        val db = UserData.getInstance(this)
        projectDao = db.projectDao()
        experienceDao = db.experienceDao()
        userProfileDao = db.userProfileDao()
        skillsDao = db.skillsDao()

        lifecycleScope.launch {
            val fetchedUserProfile = userProfileDao.getUser() // Fetch and assign
            // Assign to class member if needed elsewhere, handle null appropriately
            if (fetchedUserProfile != null) {
                userProfile = fetchedUserProfile
            } else {
                Log.e(TAG, "User Profile not found in database!")
                // Handle error case: maybe show a message or finish activity
                Toast.makeText(this@OutputActivity, "Error loading user profile", Toast.LENGTH_LONG).show()
                finish() // Example: exit if profile is essential
                return@launch // Stop further execution in this coroutine block
            }

            val experiences = experienceDao.getAllExperiences()
            val projects = projectDao.getAllProjects()
            val education = database.educationDao().getAllEducation()
            val achievements = database.achievementDao().getAllAchievements()
            val certifications = database.certificationDao().getAllCertifications()
            val skills = skillsDao.getAllSkills()

            // Check if adapters need data - if lists are empty, handle gracefully
            if (projects.isEmpty()) Log.w(TAG, "No projects found to display.")
            if (experiences.isEmpty()) Log.w(TAG, "No experiences found to display.")


            projectAdapter = ProjectOutputAdapter(projects, this@OutputActivity)
            experienceAdapter = ExperienceOutputAdapter(experiences, this@OutputActivity)
            Log.d(
                TAG,
                "[OutputActivity] Fetched ${projects.size} projects, ${experiences.size} experiences."
            )

            binding.rvProjects.adapter = projectAdapter
            binding.rvExperiences.adapter = experienceAdapter
            // No need for notifyDataSetChanged here, adapter is initialized with data

            // Set initial state (select first item in NavRail)
            updateUiForCard(OBJECTIVE_INDEX) // Set initial selection, FAB visibility


            // Call AI Model after fetching all data
            if (isInternetAvailable(this@OutputActivity)) {
                // Pass the non-null userProfile
                modelCall(
                    binding,
                    userProfile, // Now guaranteed non-null if reached here
                    experiences,
                    projects,
                    education,
                    achievements,
                    certifications,
                    skills
                )
            } else {
                binding.etObjective.apply {
                    setText("AI generation Failed, Check Internet Connection")
                    setTextColor(Color.RED)
                }
            }
        }

        binding.btnSave.setOnClickListener {
            // Show loading/progress indicator
            binding.progressBarSaving.visibility =
                View.VISIBLE // Add a ProgressBar with id progressBarSaving to your layout
            binding.btnSave.isEnabled = false // Disable button during save

            val updatedProjects = projectAdapter.getUpdatedProjects()
            val updatedExperiences = experienceAdapter.getUpdatedExperiences()
            // Get the potentially edited objective text
            val updatedObjective = binding.etObjective.text.toString() // <-- Get updated objective

            lifecycleScope.launch { // Launch a coroutine
                var success = false
                try {

                    withContext(Dispatchers.IO) {
                        // --- Check for empty lists before DB operations ---
                        if (updatedProjects.isNotEmpty()){
                            projectDao.updateProjects(updatedProjects)
                            Log.d(TAG, "[OutputActivitySave] Updated projects in DB.")
                        } else {
                            Log.w(TAG, "[OutputActivitySave] No projects to update in DB.")
                        }

                        if (updatedExperiences.isNotEmpty()){
                            experienceDao.updateExperiences(updatedExperiences)
                            Log.d(TAG, "[OutputActivitySave] Updated experiences in DB.")
                        } else {
                            Log.w(TAG, "[OutputActivitySave] No experiences to update in DB.")
                        }

                        val currentUserProfile = userProfile // Use existing instance if safe
                        currentUserProfile.objective = updatedObjective // Update the objective field
                        userProfileDao.insertOrUpdate(currentUserProfile) // Save the updated profile
                        Log.d(TAG, "[OutputActivitySave] Updated objective in DB.")

                        success = true // Mark as successful
                    }

                    // After IO operations, switch back to Main thread for UI updates and navigation
                    if (success) {
                        Toast.makeText(
                            this@OutputActivity,
                            "Changes saved successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Start TemplateActivity AFTER saving is complete
                        val intent = Intent(this@OutputActivity, TemplateActivity::class.java)
                        startActivity(intent)
                    } else {
                        // This else block might be unreachable if success is always set in the try
                        Toast.makeText(
                            this@OutputActivity,
                            "Failed to save changes!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(
                        this@OutputActivity,
                        "Failed to save changes! Error: ${e.localizedMessage}", // Show error detail
                        Toast.LENGTH_LONG // Longer duration for error
                    ).show()
                } finally {
                    binding.progressBarSaving.visibility = View.GONE // Hide loading
                    binding.btnSave.isEnabled = true // Re-enable button
                }
            }
        }

        binding.lottieAnimCard.setOnClickListener {
            showChat()
        }
        binding.fabNext.setOnClickListener {
            scrollToNextCard()
        }

        setupNavigationRail() // Setup NavRail listeners

        // --- Disable manual scrolling on the ScrollView ---
        binding.mainScrollView.setOnTouchListener { _, _ ->
            // Return true to consume the touch event, preventing the ScrollView
            // from processing it for scrolling. Programmatic scrolling still works.
            true
        }

        val chatTextVariations = listOf(
            "Here is your AI-generated content for your resume. You can edit it by tapping on the pencil icon.",
            "Here's the AI-generated content for your resume. Feel free to edit it using the pencil icon.",
            "Your AI-generated resume content is ready. Tap the pencil icon to make changes.",
            "AI-generated content for your resume is prepared for you. Edit it by tapping the pencil icon.",
            "Here is your automatically generated resume content. You can modify it with the pencil icon.",
            "Your resume content has been generated by AI. Adjust it using the pencil icon.",
            "Here's the AI-crafted content for your resume. Tap the pencil icon to edit.",
            "AI has generated the perfect content for your resume. Edit it anytime with the pencil icon.",
            "Find below your AI-powered resume content. Modify it using the pencil icon.",
            "Your AI-enhanced resume content is ready for review. Edit it by tapping the pencil icon."
        )


        var isEditing = false

        binding.ivEdit.setOnClickListener {
            isEditing = !isEditing
            binding.etObjective.isEnabled = isEditing


            if (isEditing) {
                binding.etObjective.requestFocus() // Request focus only when enabling edit
                binding.ivEdit.setImageResource(com.example.buildairesume.R.drawable.icon_tick) // Edit mode image
            } else {
                binding.etObjective.clearFocus() // Clear focus when done editing
                // Optional: Hide keyboard if needed
                hideKeyboard(binding.etObjective)
                binding.ivEdit.setImageResource(com.example.buildairesume.R.drawable.icon_write) // Default image
                // Optional: Save the objective automatically here if desired
                val updatedObjective = binding.etObjective.text.toString()
//                lifecycleScope.launch(Dispatchers.IO) {
//                    userProfile.objective = updatedObjective
//                    userProfileDao.insertOrUpdate(userProfile)
//                    Log.d(TAG, "Objective saved automatically after edit.")
//                }
            }
        }


        startTypewriterEffect(binding.outputActivityChat, chatTextVariations.random(), 30)

    }

    // --- Helper function to hide keyboard ---
    private fun hideKeyboard(view: View?) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun showChat() {
        val chatCard = binding.outputActivityChatCard

        chatCard.apply {

            alpha = 0f // Start from invisible
            animate()
                .alpha(1f) // Fade in
                .setDuration(200) // Smooth fade-in over 200ms
                .start()
        }

        lifecycleScope.launch {
            delay(4000) // Wait for 4 seconds before fading out

            withContext(Dispatchers.Main) {
                chatCard.animate()
                    .alpha(0f) // Fade out
                    .setDuration(400) // Smooth fade-out over 400ms
                    .start()
            }
        }
    }


    private var typewriterJob: Job? = null

    private fun startTypewriterEffect(
        textView: TextView,
        fullText: String,
        delayMillis: Long = 5
    ) {
        typewriterJob?.cancel()
        textView.text = "" // Clear text initially

        val chatCard = binding.outputActivityChatCard
        chatCard.alpha = 1f // Ensure it's fully visible at the start

        var currentIndex = 0
        binding.lottieAnim.speed = 2F

        typewriterJob = lifecycleScope.launch {
            while (currentIndex < fullText.length) {
                val nextStep = if (Random.nextBoolean()) 1 else Random.nextInt(2, 3)
                val endIndex = (currentIndex + nextStep).coerceAtMost(fullText.length)

                textView.text = fullText.substring(0, endIndex)
                currentIndex = endIndex

                delay(delayMillis + Random.nextLong(10, 50))
            }
            binding.lottieAnim.speed = 0.5F

            // Wait 2 seconds before fading out
            delay(2000)

            withContext(Dispatchers.Main) {
                chatCard.animate()
                    .alpha(0f)
                    .setDuration(500) // Smooth fade-out
                    .start()
            }
        }
    }


    private suspend fun callAIWithRetry(
        prompt: String,
        binding: ActivityOutputBinding // Binding might not be needed here if just returning text
    ): String {

        val apiKey = BuildConfig.GEMINI_API_KEY
        val model = BuildConfig.GEMINI_MODEL
        val generativeModel =
            GenerativeModel(model, apiKey)
        // Removed default text setting here, handle failure in the calling coroutine

        return withContext(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(prompt)
                response.text ?: "Error: No response text from AI." // Provide error message on null text
            } catch (e: Exception) {
                Log.e("AI", "Error generating AI content", e)
                "AI generation failed: ${e.localizedMessage}" // Return error message on exception
            }
        }
    }

    // Made userProfile parameter non-nullable as it's checked before calling
    private fun modelCall(
        binding: ActivityOutputBinding,
        userProfile: UserProfile, // Now non-nullable
        experiences: List<Experience>,
        projects: List<Project>,
        education: List<Education>,
        achievements: List<Achievement>,
        certifications: List<Certification>,
        skills: List<Skills>
    ) {
        lifecycleScope.launch {
            binding.etObjective.visibility = View.INVISIBLE
            startReversibleAnimation() // Show loading animation
            binding.btnSave.isEnabled = false // Disable save button

            val prompt = """
            Write a professional **objective section** for a resume in 4-5 lines, ensuring it is in **third-person** and **omits any headings**.
           
            The objective should concisely summarize the candidate’s background, skills, and career aspirations based on the following details:

            - **Name**: ${userProfile.fullName}
            - **Years of Experience**: ${experiences.size} years (if available)
            - **Current / Most Recent Role**: ${experiences.firstOrNull()?.position ?: "Not specified"}
            - **Industry**: Based on experience at ${experiences.firstOrNull()?.companyName ?: "various companies"}
            --**Applying for Job Profile**: ${userProfile.jobProfile ?: "Not specified"}
            - **Key Skills**: ${skills.joinToString(", ") { it.skillName }}
            - **Gender** : ${userProfile.gender ?: "Not specified"}
            - **Relevant Work Experiences**: ${experiences.joinToString(", ") { it.position }}
            - **Notable Projects**: ${projects.firstOrNull()?.title ?: "No significant projects listed"}
            - **Education**: ${education.firstOrNull()?.qualification} in ${education.firstOrNull()?.branch ?: education.firstOrNull()?.degree ?: "relevant field"}
            - **Achievements**: ${achievements.firstOrNull()?.title ?: "No major achievements listed"}
            - **Certifications**: ${certifications.firstOrNull()?.name ?: "No certifications listed"}
            - **Career Goals**: ${userProfile.objective ?: "Looking for opportunities to contribute and grow"}

            Ensure the response is **concise, impactful, and tailored for a resume**. Avoid excessive adjectives or unnecessary filler words.
        """.trimIndent()

            val objectiveText = callAIWithRetry(prompt, binding) // Binding might not be needed by callAIWithRetry

//            // Update the userProfile object in memory
//            userProfile.objective = objectiveText
//
//            // --- Update database in IO context ---
//            withContext(Dispatchers.IO) {
//                userProfileDao.insertOrUpdate(userProfile) // Persist the updated objective
//            }

            // --- Update UI on Main context ---
            withContext(Dispatchers.Main) {
                binding.etObjective.setText(objectiveText.trim())
                // Handle potential AI failure display
                if (objectiveText.startsWith("AI generation failed") || objectiveText.startsWith("Error:")) {
                    binding.etObjective.setTextColor(Color.RED)
                } else {
                     // Or your default text color
                }
                binding.etObjective.visibility = View.VISIBLE
                binding.loadingAnim.visibility = View.GONE // Hide loading
                binding.btnSave.isEnabled = true // Enable save button
            }
        }
    }

    private fun startReversibleAnimation() {
        binding.loadingAnim.apply {
            visibility = View.VISIBLE
            setMinFrame(0) // Start from the first frame
            setMaxFrame(30) // Adjust max frame based on animation length
            speed = 2f // Slightly reduced speed for smoother flow

            val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 600 // Smoother speed transition
                interpolator = LinearInterpolator() // Ensures constant smooth motion
                repeatMode = ValueAnimator.REVERSE // Forward then reverse
                repeatCount = ValueAnimator.INFINITE // Infinite loop
                addUpdateListener { animation ->
                    progress = animation.animatedValue as Float
                }
            }
            animator.start()
        }
    }


    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager? // Make nullable safe call
                ?: return false // Return false if manager is null

        val network = connectivityManager.activeNetwork ?: return false // Return false if network is null
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false // Return false if capabilities are null
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // --- Helper function to update NavRail, Chat, and FAB based on card index ---
    private fun updateUiForCard(index: Int) {
        if (index < 0 || index >= TOTAL_CARDS) {
            Log.w("UpdateUI", "Invalid index received: $index")
            return // Avoid index out of bounds
        }
        Log.d("UpdateUI", "Updating UI for card index: $index")
        currentCardIndex = index // Update the tracked index

        // Update NavigationRail Selection
        updateNavigationRailSelection(index)

        // --- Optional: Update Chat Text (if desired for OutputActivity) ---
        // val sectionText = getSectionTextForOutput(index) // Need to define this function if needed
        // startTypewriterEffect(binding.outputActivityChat, sectionText)

        // Update FAB Visibility
        val isLastCard = (index == TOTAL_CARDS - 1) // Check against total cards
        binding.fabNext.visibility = if (isLastCard) View.GONE else View.VISIBLE
        Log.d("UpdateUI", "FAB visibility set to: ${binding.fabNext.visibility}")
    }

    // --- Updated function to explicitly set the NavRail selection ---
    private fun updateNavigationRailSelection(index: Int) {
        val navRail = binding.outputNavRail
        val menu = navRail.menu ?: return // Exit if menu is null

        // Map card index to navigation item ID
        val menuItems = listOf(
            com.example.buildairesume.R.id.nav_objective_output,
            com.example.buildairesume.R.id.nav_projects_output,
            com.example.buildairesume.R.id.nav_experience_output,
        )

        if (index in menuItems.indices) {
            val targetItemId = menuItems[index]
            // Deselect all items first (safer)
            for (i in 0 until menu.size()) {
                menu.getItem(i).isChecked = false
            }
            // Select the target item
            menu.findItem(targetItemId)?.isChecked = true
            Log.d("NavRailUpdate", "Set item ${menu.findItem(targetItemId)?.title} as selected for index $index")

        } else {
            Log.w("NavRailUpdate", "Index $index out of bounds for menu items.")
        }
    }

    // --- Updated function to scroll to the next card based on state ---
    private fun scrollToNextCard() {
        Log.d("Scroll", "scrollToNextCard called, current index: $currentCardIndex")
        if (currentCardIndex < cardViews.size - 1) {
            val nextIndex = currentCardIndex + 1
            scrollToCard(nextIndex) // Use the common scroll function
        } else {
            Log.d("Scroll", "Already at the last card ($currentCardIndex).")
        }
    }


    // --- NEW Helper function to scroll to a specific card index ---
    private fun scrollToCard(index: Int) {
        if (index in cardViews.indices) {
            Log.d("Scroll", "Scrolling to card index: $index")
            val targetCard = cardViews[index]
            val targetScrollY = targetCard.top // Consider small offset: targetCard.top - 20

            binding.mainScrollView.post { // Use post for smoother UI transitions
                binding.mainScrollView.smoothScrollTo(0, targetScrollY)
            }

            // Update UI elements immediately or with a slight delay
            // Immediate update is usually fine for state, NavRail, FAB
            updateUiForCard(index) // Updates state, NavRail, FAB visibility

        } else {
            Log.w("Scroll", "Attempted to scroll to invalid index: $index")
        }
    }


    // --- Updated NavigationRail setup to use the new state and scroll function ---
    private fun setupNavigationRail() {
        val navigationRailView = binding.outputNavRail

        navigationRailView.setOnItemSelectedListener { item ->
            Log.d("NavRailClick", "Item selected: ${item.title}")
            val targetIndex = when (item.itemId) {
                com.example.buildairesume.R.id.nav_objective_output -> OBJECTIVE_INDEX
                com.example.buildairesume.R.id.nav_projects_output -> PROJECTS_INDEX
                com.example.buildairesume.R.id.nav_experience_output -> EXPERIENCE_INDEX
                else -> -1 // Indicate no matching card
            }

            if (targetIndex != -1 && targetIndex != currentCardIndex) { // Only scroll if index is valid and different
                scrollToCard(targetIndex) // Scroll to the selected card
                true // Event consumed
            } else if (targetIndex == currentCardIndex) {
                Log.d("NavRailClick", "Item already selected (index $targetIndex). No scroll.")
                true // Consume event even if not scrolling
            }
            else {
                Log.w("NavRailClick", "No target index found for item ${item.itemId}")
                false // Event not consumed
            }
        }
    }

}