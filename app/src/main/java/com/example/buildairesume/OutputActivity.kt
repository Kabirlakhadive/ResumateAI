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
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.core.animation.doOnEnd
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
import kotlin.math.abs
import kotlin.random.Random


class OutputActivity : AppCompatActivity() {
    private lateinit var projectDao: ProjectDao
    private lateinit var userProfileDao: UserProfileDao
    private lateinit var experienceDao: ExperienceDao
    private lateinit var userProfile: UserProfile
    private lateinit var binding: ActivityOutputBinding
    private lateinit var projectAdapter: ProjectOutputAdapter
    private lateinit var experienceAdapter: ExperienceOutputAdapter
    private lateinit var skillsDao: SkillsDao
    private lateinit var experience: Experience
    private lateinit var project: Project
    private lateinit var database: UserData
    private val TAG = "AISaveDebug"
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOutputBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = UserData.getInstance(this)

        val db = UserData.getInstance(this)
        projectDao = db.projectDao()
        experienceDao = db.experienceDao()
        userProfileDao = db.userProfileDao()
        skillsDao = db.skillsDao()

        lifecycleScope.launch {
            val userProfile = userProfileDao.getUser()
            val experiences = experienceDao.getAllExperiences()
            val projects = projectDao.getAllProjects()
            val education = database.educationDao().getAllEducation()
            val achievements = database.achievementDao().getAllAchievements()
            val certifications = database.certificationDao().getAllCertifications()
            val skills = skillsDao.getAllSkills()
            projectAdapter = ProjectOutputAdapter(projects, this@OutputActivity)
            experienceAdapter = ExperienceOutputAdapter(experiences, this@OutputActivity)
            Log.d(TAG, "[OutputActivity] Fetched ${projects.size} projects, ${experiences.size} experiences.")

            binding.rvProjects.adapter = projectAdapter
            binding.rvExperiences.adapter = experienceAdapter
            projectAdapter.notifyDataSetChanged()
            experienceAdapter.notifyDataSetChanged()


            // Call AI Model after fetching all data
            if (isInternetAvailable(this@OutputActivity)) {
                modelCall(
                    binding,
                    userProfile,
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
            binding.progressBarSaving.visibility = View.VISIBLE // Add a ProgressBar with id progressBarSaving to your layout
            binding.btnSave.isEnabled = false // Disable button during save

            val updatedProjects = projectAdapter.getUpdatedProjects()
            val updatedExperiences = experienceAdapter.getUpdatedExperiences()
            // Get the potentially edited objective text
            val updatedObjective = binding.etObjective.text.toString() // <-- Get updated objective

            lifecycleScope.launch { // Launch a coroutine
                var success = false
                try {
                    Log.d(TAG, "[OutputActivitySave] Save Button Clicked.")
                    Log.d(TAG, "[OutputActivitySave] Projects from adapter BEFORE DB: ${updatedProjects.joinToString { it.title + " -> output: [" + (it.output ?: "NULL") + "]" }}")
                    Log.d(TAG, "[OutputActivitySave] Experiences from adapter BEFORE DB: ${updatedExperiences.joinToString { it.position + " -> output: [" + (it.output ?: "NULL") + "]" }}")
                    Log.d(TAG, "[OutputActivitySave] Objective text BEFORE DB: [$updatedObjective]")
                    // Perform database operations on IO thread
                    withContext(Dispatchers.IO) {
                        Log.d(TAG, "[OutputActivitySave] Entering DB context...")
                        // Log IDs before update
                        Log.d(TAG, "[OutputActivitySave] Project IDs being updated: ${updatedProjects.map { it.projectId }}")
                        Log.d(TAG, "[OutputActivitySave] Experience IDs being updated: ${updatedExperiences.map { it.experienceId }}")
                        Log.d(TAG, "[OutputActivitySave] Entering DB context...")
                        projectDao.updateProjects(updatedProjects)
                        Log.d(TAG, "[OutputActivitySave] Updated projects in DB.")
                        experienceDao.updateExperiences(updatedExperiences)
                        Log.d(TAG, "[OutputActivitySave] Updated experiences in DB.")

                        // Also save the updated objective back to UserProfile
                        val currentUserProfile = userProfileDao.getUser() // Fetch current profile
                        currentUserProfile?.let { profile ->
                            profile.objective = updatedObjective // Update the objective field
                            userProfileDao.insertOrUpdate(profile) // Save the updated profile
                            Log.d(TAG, "[OutputActivitySave] Updated objective in DB.")
                        }?: Log.w(TAG, "[OutputActivitySave] UserProfile was null, couldn't save objective.")
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
                        Toast.makeText(
                            this@OutputActivity,
                            "Failed to save changes!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "[OutputActivitySave] Error during save process", e) // Log exception
                    Toast.makeText(this@OutputActivity, "Failed to save changes!", Toast.LENGTH_SHORT).show()
                    Toast.makeText(
                        this@OutputActivity,
                        "Failed to save changes!",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    // Ensure UI updates happen on the Main thread
                    binding.progressBarSaving.visibility = View.GONE // Hide loading
                    binding.btnSave.isEnabled = true // Re-enable button
                }
            }
        }

        binding.lottieAnimCard.setOnClickListener{
            showChat()
        }
        binding.fabNext.setOnClickListener {
            scrollToNextCard()
        }

        setupNavigationRail()
        binding.mainScrollView?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                binding.mainScrollView!!.postDelayed({
                    snapToNearestCard()
                }, 100)

                val lastCard =
                    binding.mainLinearLayout.getChildAt(binding.mainLinearLayout.childCount - 1)
                val isLastCardVisible =
                    abs(binding.mainScrollView.scrollY - lastCard.top) < binding.mainScrollView.height / 2

                binding.fabNext.visibility = if (isLastCardVisible) View.GONE else View.VISIBLE
            }
            false
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
            binding.etObjective.requestFocus()

            if (isEditing) {
                binding.ivEdit.setImageResource(R.drawable.icon_tick) // Edit mode image
            } else {
                binding.ivEdit.setImageResource(R.drawable.icon_write) // Default image
            }
        }


        startTypewriterEffect(binding.outputActivityChat, chatTextVariations.random(), 50)

    }

    private fun showChat() {
        val chatCard = binding.formActivityChatCard

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
        delayMillis: Long = 50
    ) {
        typewriterJob?.cancel()
        textView.text = "" // Clear text initially

        val chatCard = binding.formActivityChatCard
        chatCard.alpha = 1f // Ensure it's fully visible at the start

        var currentIndex = 0
        binding.lottieAnim.speed = 2F

        typewriterJob = lifecycleScope.launch {
            while (currentIndex < fullText.length) {
                val nextStep = if (Random.nextBoolean()) 1 else Random.nextInt(2, 5)
                val endIndex = (currentIndex + nextStep).coerceAtMost(fullText.length)

                textView.text = fullText.substring(0, endIndex)
                currentIndex = endIndex

                delay(delayMillis + Random.nextLong(10, 50))
            }
            binding.lottieAnim.speed = 0.5F

            // Wait 2 seconds before fading out
            delay(4000)

            withContext(Dispatchers.Main) {
                chatCard.animate()
                    .alpha(0f)
                    .setDuration(500) // Smooth fade-out
                    .start()
            }
        }
    }

    // Function to collapse the chat card smoothly
    private fun collapseChatCard() {
        val chatCard = binding.formActivityChatCard
        val collapseAnim = ValueAnimator.ofInt(chatCard.width, 0).apply {
            duration = 500
            addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                val params = chatCard.layoutParams
                params.width = value
                chatCard.layoutParams = params
            }
        }
        collapseAnim.start()
    }



    private suspend fun callAIWithRetry(
        prompt: String,
        binding: ActivityOutputBinding
    ): String {

        val apiKey = BuildConfig.GEMINI_API_KEY
        val model = BuildConfig.GEMINI_MODEL
        val generativeModel =
            GenerativeModel(model, apiKey)
        var outputText = "AI generation failed. Check internet connection."

        return withContext(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(prompt)
                response.text ?: "No response from AI"
            } catch (e: Exception) {
                Log.e("AI", "Error generating AI content", e)
                outputText
            }
        }
    }

    private fun modelCall(
        binding: ActivityOutputBinding,
        userProfile: UserProfile?,
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

            - **Name**: ${userProfile?.fullName ?: "N/A"}
            - **Years of Experience**: ${experiences.size} years (if available)
            - **Current / Most Recent Role**: ${experiences.firstOrNull()?.position ?: "Not specified"}
            - **Industry**: Based on experience at ${experiences.firstOrNull()?.companyName ?: "various companies"}
            --**Applying for Job Profile**: ${userProfile?.jobProfile ?: "Not specified"}
            - **Key Skills**: ${skills.joinToString(", ") { it.skillName }}
            - **Gender** : ${userProfile?.gender ?: "Not specified"}
            - **Relevant Work Experiences**: ${experiences.joinToString(", ") { it.position }}
            - **Notable Projects**: ${projects.firstOrNull()?.title ?: "No significant projects listed"}
            - **Education**: ${education.firstOrNull()?.qualification} in ${education.firstOrNull()?.branch ?: education.firstOrNull()?.degree ?: "relevant field"}
            - **Achievements**: ${achievements.firstOrNull()?.title ?: "No major achievements listed"}
            - **Certifications**: ${certifications.firstOrNull()?.name ?: "No certifications listed"}
            - **Career Goals**: ${userProfile?.objective ?: "Looking for opportunities to contribute and grow"}

            Ensure the response is **concise, impactful, and tailored for a resume**. Avoid excessive adjectives or unnecessary filler words.
        """.trimIndent()

            val objectiveText = callAIWithRetry(prompt, binding)
            userProfile?.let {
                it.objective = objectiveText
                userProfileDao.insertOrUpdate(it)  // Persist the updated objective in Room database
            }

            withContext(Dispatchers.Main) {
                binding.etObjective.setText(objectiveText)
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
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun updateNavigationRailSelection(index: Int) {
        val navRail = binding.outputNavRail

        // Map card index to navigation item ID
        val menuItems = listOf(
            R.id.nav_objective_output,
            R.id.nav_projects_output,
            R.id.nav_experience_output,

            )

        if (index in menuItems.indices) {
            navRail?.menu?.findItem(menuItems[index])?.isChecked = true
        }
    }

    private fun scrollToNextCard() {
        val scrollView = binding.mainScrollView
        val container = binding.mainLinearLayout
        val childCount = container.childCount

        var currentCardIndex = -1

        // Find the card that is currently snapped at the top
        for (i in 0 until childCount) {
            val cardView = container.getChildAt(i)
            if (Math.abs(scrollView.scrollY - cardView.top) < scrollView.height / 2) {
                currentCardIndex = i
                break
            }
        }

        // Scroll to the next card if it exists
        if (currentCardIndex in 0 until childCount - 1) {
            val nextCardIndex = currentCardIndex + 1
            val nextCard = container.getChildAt(nextCardIndex)

            // 🔹 Smooth scroll and update NavigationRail AFTER animation finishes
            scrollView.post {
                scrollView.smoothScrollTo(0, nextCard.top - 20)

                scrollView.postDelayed({
                    updateNavigationRailSelection(nextCardIndex) // Update after scrolling
                }, 300) // Adjust delay based on smoothScrollTo speed
            }
        }

        // Hide FAB if we reach the last card
        binding.fabNext.visibility =
            if (currentCardIndex == childCount - 2) View.GONE else View.VISIBLE
    }

    private fun snapToNearestCard() {
        val scrollView = binding.mainScrollView
        val container = binding.mainLinearLayout
        val childCount = container!!.childCount

        var nearestCard: View? = null
        var minDistance = Int.MAX_VALUE
        var nearestIndex = -1

        for (i in 0 until childCount) {
            val cardView = container.getChildAt(i)
            val cardTop = cardView.top
            val distance = scrollView!!.scrollY - cardTop  // Difference from scroll position

            // Adjust snapping logic for both up & down
            if (Math.abs(distance) < minDistance && Math.abs(distance) < scrollView.height / 1.2) {
                minDistance = Math.abs(distance)
                nearestCard = cardView
                nearestIndex = i
            }
        }

        nearestCard?.let {
            scrollView?.post {
                scrollView.smoothScrollTo(0, it.top - 20)
            }
            updateNavigationRailSelection(nearestIndex)

            // 🔹 Update FAB visibility when snapping (fix for NavigationRail issue)
            val isLastCard = nearestIndex == childCount - 1
            binding.fabNext.visibility = if (isLastCard) View.GONE else View.VISIBLE
        }
    }

    private fun setupNavigationRail() {
        val navigationRailView = binding.outputNavRail

        navigationRailView?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_objective_output -> {
                    binding.cvObjectiveOutput?.let {
                        binding.mainScrollView.smoothScrollTo(
                            0,
                            it.top
                        )
                    }
                    binding.fabNext.visibility = View.VISIBLE
                    true
                }

                R.id.nav_projects_output -> {
                    Log.d("PDFGenerator", "project output button pressed")
                    binding.cvProjectOutput?.let {
                        binding.mainScrollView.smoothScrollTo(
                            0,
                            it.top
                        )
                    }
                    binding.fabNext.visibility = View.VISIBLE
                    true
                }

                R.id.nav_experience_output -> {
                    binding.cvExperienceOutput?.let {
                        binding.mainScrollView.smoothScrollTo(
                            0,
                            it.top
                        )
                    }
                    binding.fabNext.visibility = View.GONE
                    true
                }

                else -> false
            }
        }
    }

}

