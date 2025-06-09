package com.codeNext.resumateAI

// import android.view.MotionEvent // No longer needed
// import kotlin.math.abs // No longer needed for snapping
import android.R
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.codeNext.resumateAI.SkillsList.skillsList
import com.codeNext.resumateAI.adapters.AchievementAdapter
import com.codeNext.resumateAI.adapters.CertificationAdapter
import com.codeNext.resumateAI.adapters.EducationAdapter
import com.codeNext.resumateAI.adapters.ExperienceAdapter
import com.codeNext.resumateAI.adapters.ProjectAdapter
import com.codeNext.resumateAI.databinding.ActivityFormBinding
import com.codeNext.resumateAI.fragments.AddAchievementFragment
import com.codeNext.resumateAI.fragments.AddCertificationFragment
import com.codeNext.resumateAI.fragments.AddEducationFragment
import com.codeNext.resumateAI.fragments.AddExperienceFragment
import com.codeNext.resumateAI.fragments.AddProjectFragment
import com.codeNext.resumateAI.models.Achievement
import com.codeNext.resumateAI.models.Certification
import com.codeNext.resumateAI.models.Education
import com.codeNext.resumateAI.models.Experience
import com.codeNext.resumateAI.models.Project
import com.codeNext.resumateAI.models.Skills
import com.codeNext.resumateAI.models.UserProfile
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random


class FormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormBinding
    private lateinit var achievementAdapter: AchievementAdapter
    private lateinit var projectAdapter: ProjectAdapter
    private lateinit var certificationAdapter: CertificationAdapter
    private lateinit var experienceAdapter: ExperienceAdapter
    private lateinit var educationAdapter: EducationAdapter
    private val selectedSkills = mutableListOf<String>()
    private lateinit var database: UserData
    private lateinit var chatText: TextView
    private var currentVisibleCardIndex = 0 // Track the current card index
    private var isKeyboardShowing = false
    private val keyboardVisibilityListener = ViewTreeObserver.OnGlobalLayoutListener {
        checkKeyboardVisibility()
    }

    // --- Constants for card indices (optional but improves readability) ---
    companion object {
        private const val PERSONAL_INFO_INDEX = 0
        private const val SKILLS_INDEX = 1
        private const val LINKS_INDEX = 2
        private const val EXPERIENCE_INDEX = 3
        private const val PROJECTS_INDEX = 4
        private const val CERTIFICATION_INDEX = 5
        private const val EDUCATION_INDEX = 6
        private const val TOTAL_CARDS = 7 // Update if you add more cards
    }
    // --- ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        database = UserData.getInstance(this)
        chatText = binding.formActivityChat

        // --- Disable scrolling on the ScrollView ---
        // The most straightforward way is often just *not* setting an OnTouchListener
        // that allows scrolling or handles snapping.
        // If needed, you could explicitly disable it, but removing the listener is key.
        // binding.mainScrollView?.setOnTouchListener { _, _ -> true } // This would block all touch

        setupAdapters()
        fillFormWithPreviousData()
        skillsAdapter()
        setupFragments()
        updateRecyclerViewVisibility()
        setupKeyboardVisibilityListener()
        genderAdapter()

//        binding.btnSubmit.setOnClickListener {
//            if (areAllFieldsFilled()) {  // ✅ Check if all fields are filled
//                saveResumeData()
//                val intent = Intent(this, OutputActivity::class.java)
//                startActivity(intent)
//                Log.d("kabir", "Generate Button pressed")
//            } else {
//                startTypewriterEffect(
//                    chatText,
//                    "Uh oh ! Looks like you forgot to fill some important information",
//                    50
//                )
//            }
//        }

        binding.btnSubmit.setOnClickListener {
            Log.d("SubmitClick", "Submit button clicked. Performing validation.")

            var message = "Are you sure you want to submit? \nThis will cost you 1 token."
            // 1. Check Basic Personal Info Fields First
            if (binding.etName.text.isNullOrEmpty()) {
                Log.d("SubmitValidation", "Failed: Basic personal info missing.")
                // Use your existing typewriter effect or a simple Toast
                startTypewriterEffect(
                    chatText,
                    "Please fill in your Name, Email, and Phone Number.", // More specific message
                    30
                )

                binding.mainScrollView.smoothScrollTo(0, binding.cvPersonalInformation.top - 20)
                binding.mainScrollView.postDelayed({ updateUiForCard(PERSONAL_INFO_INDEX) }, 150)

                // Or uncomment this Toast if you prefer:
                // Toast.makeText(this, "Please fill in Name, Email, and Phone Number.", Toast.LENGTH_LONG).show()
                return@setOnClickListener // Stop validation here
            }
            if(binding.etEmail.text.isNullOrEmpty()){
                message = "You have not filled your email.\n$message"
            }
            if(binding.etPhone.text.isNullOrEmpty()){
                message = "You have not filled your phone number.\n$message"
            }

            val MINIMUM_SKILLS = 5 // Example minimum
            if (selectedSkills.size < MINIMUM_SKILLS) {
//                Log.d("SubmitValidation", "Failed: Not enough skills added.")
////                Toast.makeText(this, "Please add at least $MINIMUM_SKILLS skills.", Toast.LENGTH_LONG).show()
//                startTypewriterEffect(chatText, "Please add at least $MINIMUM_SKILLS skills.", 30)
//                // Optional: Scroll to Skills
//                binding.mainScrollView.smoothScrollTo(0, binding.cvSkills.top - 20)
//                binding.mainScrollView.postDelayed({ updateUiForCard(SKILLS_INDEX) }, 150)
//                return@setOnClickListener
                message = "You have not selected at least 5 skills.\n$message"
            }

//            // 2. Check Experience
            if (experienceAdapter.itemCount == 0) {
//                Log.d("SubmitValidation", "Failed: No experience added.")
////                Toast.makeText(this, "Please add at least one work experience.", Toast.LENGTH_LONG).show()
//                startTypewriterEffect(chatText, "Please add at least one work experience.", 30)
//                // Optional: Scroll to the Experience section
//                binding.mainScrollView.smoothScrollTo(0, binding.cvExperience.top - 20)
//                binding.mainScrollView.postDelayed({ updateUiForCard(EXPERIENCE_INDEX) }, 150)
//                return@setOnClickListener // Stop validation here
                message = "You have not added any work experience.\n$message"
            }

            // 3. Check Projects
            if (projectAdapter.itemCount == 0) {
//                Log.d("SubmitValidation", "Failed: No projects added.")
////                Toast.makeText(this, "Please add at least one project.", Toast.LENGTH_LONG).show()
//                startTypewriterEffect(chatText, "Please add at least one project.", 30)
//                // Optional: Scroll to the Projects section
//                binding.mainScrollView.smoothScrollTo(0, binding.cvProjects.top - 20)
//                binding.mainScrollView.postDelayed({ updateUiForCard(PROJECTS_INDEX) }, 150)
//                return@setOnClickListener // Stop validation here
                message = "You have not added any projects. \n$message"
            }

            // 4. Check Education
            if (educationAdapter.itemCount == 0) {
                Log.d("SubmitValidation", "Failed: No education added.")
//                Toast.makeText(this, "Please add at least one education entry.", Toast.LENGTH_LONG).show()
                Toast.makeText(this, "Please add at least one education entry.", Toast.LENGTH_SHORT).show()
                // Optional: Scroll to the Education section
                binding.mainScrollView.smoothScrollTo(0, binding.cvEducation.top - 20)
                binding.mainScrollView.postDelayed({ updateUiForCard(EDUCATION_INDEX) }, 150)
                return@setOnClickListener // Stop validation here
            }

            // 5. Check Skills
            // You might want a minimum number, but checking for at least one is common.
            if (selectedSkills.isEmpty()) {
//                Log.d("SubmitValidation", "Failed: No skills added.")
//                Toast.makeText(this, "Please add at least one skill.", Toast.LENGTH_LONG).show()
//                // Optional: Scroll to the Skills section
//                binding.mainScrollView?.smoothScrollTo(0, binding.cvSkills.top - 20)
//                binding.mainScrollView?.postDelayed({ updateUiForCard(SKILLS_INDEX) }, 150)
//                return@setOnClickListener // Stop validation here
                message = "You have not added any skills.\n$message"
            }
//            // --- Add minimum skills check example (optional) ---


            // Check Education
            if (educationAdapter.itemCount == 0) {
                Log.d("SubmitValidation", "Failed: No education added.")
                message = "You have not added any education field.\n$message"
            }

            // 7. If all checks pass, proceed with saving and navigating
            Log.d("SubmitValidation", "All checks passed. Saving data and starting OutputActivity.")
            showConfirmationDialog(message)

        }

        binding.fabNext.setOnClickListener {
            Log.d("kabir", "FAB Next Button pressed")
            unfocus()
            scrollToNextCard()
        }

        setupNavigationRail() // Setup NavRail listeners

        // Initial state setup
        updateUiForCard(PERSONAL_INFO_INDEX) // Set initial NavRail, chat, FAB

        binding.lottieAnimCard.setOnClickListener {
            showChat()
        }

        setupSpinner()

        binding.mainScrollView.setOnTouchListener { _, _ ->
            // Return true to consume the touch event, preventing the ScrollView
            // from processing it for scrolling. Programmatic scrolling via
            // smoothScrollTo() in your NavRail and FAB listeners will still work.
            true
        }

        // REMOVED: setOnTouchListener for mainScrollView - Disables manual scrolling and snapping
        // binding.mainScrollView?.setOnTouchListener { _, event -> ... }
    }

    private fun showConfirmationDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                saveResumeData()
                val intent = Intent(this, OutputActivity::class.java)
                startActivity(intent) }
            .setNegativeButton("No", null) // Simpler way to dismiss

            .show()
    }


    private fun unfocus() {
        // --- Add this section to clear focus ---
        val focusedView =
            currentFocus // Get the view that currently has focus in the activity window
        if (focusedView != null) {
            // Option 1: Just clear focus (usually hides keyboard too)
            focusedView.clearFocus()
            Log.d("FocusClear", "Cleared focus from: ${focusedView.id}")

            // Option 2: Explicitly hide keyboard (more robust if clearFocus alone isn't enough)
            // You might not need both clearFocus() and hideKeyboard(), try clearFocus() first.
            // hideKeyboard(focusedView)
        } else {
            Log.d("FocusClear", "No view currently had focus.")
        }
        // --- End of section ---

        hideKeyboard(focusedView)
        // Proceed with scrolling AFTER clearing focus/hiding keyboard
    }

    private fun hideKeyboard(view: View?) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
        Log.d("Keyboard", "Attempted to hide keyboard.")
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

    private fun setupKeyboardVisibilityListener() {
        // Add the listener to the root view's ViewTreeObserver
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(keyboardVisibilityListener)
    }

    private fun checkKeyboardVisibility() {
        val rootView = binding.root
        val rect = Rect()
        rootView.getWindowVisibleDisplayFrame(rect) // Gets the visible display frame size

        val screenHeight = rootView.rootView.height // Gets the total screen height
        val keypadHeight =
            screenHeight - rect.bottom // Calculates the height occupied by system UI/keyboard

        val currentlyVisible = keypadHeight > screenHeight * 0.15

        if (isKeyboardShowing != currentlyVisible) {
            val wasKeyboardShowing = isKeyboardShowing // Store the previous state
            isKeyboardShowing = currentlyVisible // Update the state

            if (isKeyboardShowing) {
                // Keyboard IS NOW VISIBLE
                binding.chatConstraintView.visibility = View.GONE
                Log.d("KeyboardVisibility", "Keyboard shown, hiding Lottie card.")
            } else {
                // Keyboard IS NOW HIDDEN
                binding.chatConstraintView.visibility = View.VISIBLE
                Log.d("KeyboardVisibility", "Keyboard hidden, showing Lottie card.")

                // --- ADDED SCROLL LOGIC ---
                // Check if the keyboard WAS showing and the current card is Personal Info
                if (wasKeyboardShowing && currentVisibleCardIndex == PERSONAL_INFO_INDEX) {
                    Log.d(
                        "KeyboardVisibility",
                        "Keyboard hidden while on Personal Info. Scrolling to Skills card."
                    )

                    // Target the next card (Skills)
                    val targetView = binding.cvPersonalInformation
                    if (targetView != null) {
                        val targetScrollY = targetView.top - 20 // Adjust padding if needed
                        binding.mainScrollView.smoothScrollTo(0, targetScrollY)

                        // IMPORTANT: Update the UI and tracked index to reflect the scroll
                        // Use postDelayed to allow scroll animation to start
                        binding.mainScrollView.postDelayed({
                            // Update NavRail selection, chat text, FAB visibility, and currentVisibleCardIndex
                            updateUiForCard(PERSONAL_INFO_INDEX)
                            Log.d(
                                "KeyboardVisibility",
                                "UI updated for Skills card after keyboard hide scroll."
                            )
                        }, 150) // Delay might need slight adjustment

                    } else {
                        Log.w(
                            "KeyboardVisibility",
                            "Cannot scroll, target view cvSkills not found."
                        )
                    }
                }
                // --- END OF ADDED SCROLL LOGIC ---
            }
        }
    }

    private fun getSectionText(index: Int): String {
        val messages = when (index) {
            PERSONAL_INFO_INDEX -> listOf(
                // Original Theme: Start here, basic details, who are you
                "Let's start with your personal information!",
                "Tell us about yourself first.",
                "Basic details help build a great resume!",
                "Who are you? Let’s add your personal info!",
                "Your journey starts with personal details.",
                // Expanded Variations: Contact, foundation, identity, getting started
                "Time to enter your essential contact information.",
                "Let's lay the foundation: your personal details.",
                "We need your name, contact info, and location.",
                "Begin by telling us the basics about you.",
                "First things first: Your personal identification.",
                "Provide your core details so recruiters can reach you.",
                "Let's get the personal info section filled out.",
                "Your contact details are crucial - let's add them.",
                "Input your name and how employers can contact you.",
                "Start building your profile with personal info.",
                "This section covers who you are and how to contact you.",
                "Add your fundamental details to get started.",
                "Fill in your personal information to kick things off.",
                "We'll begin with your name, address, and contact details.",
                "Every great resume needs accurate personal info!"
            )

            SKILLS_INDEX -> listOf(
                // Original Theme: Add skills, expertise, strengths, 10-15 count
                "You got skills! Add at least 10-15 skills.",
                "Show off your expertise—list at least 10-15 skills!",
                "Let’s add your skills. Aim for 10-15 key strengths!",
                "Highlight your abilities! Add at least 10-15 strong skills.",
                "A strong resume has strong skills—add at least 10-15!",
                // Expanded Variations: Competencies, technical/soft, value, quantify
                "What are your core competencies? List 10-15.",
                "Showcase 10-15 technical and soft skills here.",
                "Detail your key capabilities – aim for 10-15.",
                "List the skills that make you valuable (10-15 minimum).",
                "Recruiters scan for skills – make sure to list 10-15.",
                "Time to impress with your skillset! Add 10-15.",
                "Add a comprehensive list of your skills (10-15 is a good target).",
                "Highlight 10-15 skills relevant to the jobs you want.",
                "Let's populate the skills section. Aim for 10-15.",
                "Include a mix of hard and soft skills (at least 10-15 total).",
                "What are you good at? List 10-15 skills here.",
                "Demonstrate your expertise by listing 10-15 relevant skills.",
                "Your abilities matter! Add 10-15 of your top skills.",
                "Fill this section with 10-15 of your most important skills.",
                "Quantify your expertise: add 10-15 skills."
            )

            LINKS_INDEX -> listOf(
                // Original Theme: Add links, portfolio/LinkedIn, access, professional
                "Add your important links here.",
                "Do you have a portfolio or LinkedIn? Add them!",
                "Your links help recruiters find you easily.",
                "Make sure to include your professional links!",
                "Boost your profile by adding key links.",
                // Expanded Variations: GitHub, website, online presence, credibility
                "Share links to your LinkedIn, portfolio, or GitHub.",
                "Add relevant online profiles or website links.",
                "Provide URLs that showcase your work or profile.",
                "Got a personal website or online portfolio? Link it here!",
                "Include links to give recruiters more context.",
                "Strengthen your application with professional links.",
                "Your online presence matters - add links here.",
                "Help recruiters learn more about you via links.",
                "Connect your professional profiles (LinkedIn, etc.).",
                "Add URLs for portfolio, blog, or other relevant sites.",
                "Use this space for essential external links.",
                "Show, don't just tell! Add links to your work.",
                "Input any relevant web links (Portfolio, GitHub...).",
                "Links can add depth – include yours.",
                "Make it easy for them to see more: add your links."
            )

            EXPERIENCE_INDEX -> listOf(
                // Original Theme: Experience/internships, past roles, journey, history, 2-4 count
                "Add 2-4 work experiences or internships!",
                "Where have you worked before? Add 2-4 roles!",
                "Showcase your career journey—list 2-4 experiences.",
                "Your work history matters—add 2-4 jobs or internships!",
                "Employers love experience—add 2-4 positions!",
                // Expanded Variations: Responsibilities, achievements, impact, growth, 2-4 count
                "Detail 2-4 of your most relevant past jobs.",
                "List 2-4 previous roles, including internships.",
                "Outline your professional background with 2-4 entries.",
                "Time to add your work history. Aim for 2-4 positions.",
                "Include 2-4 key roles that highlight your expertise.",
                "Describe 2-4 experiences, focusing on achievements.",
                "Where have you made an impact? List 2-4 jobs/internships.",
                "Your professional path: add 2-4 significant experiences.",
                "Show your growth – list 2-4 past roles.",
                "Recruiters want to see experience: add 2-4 here.",
                "Summarize 2-4 of your most impactful work experiences.",
                "Got internships or jobs? Add 2-4.",
                "Document your work trajectory with 2-4 entries.",
                "Let's add 2-4 examples of your professional experience.",
                "Focus on quality: detail 2-4 relevant experiences."
            )

            PROJECTS_INDEX -> listOf(
                // Original Theme: Showcase projects, built, best work, personal, story, 2-4 count
                "Showcase your projects! Add 2-4.",
                "What have you built? Add 2-4 projects!",
                "Highlight your best work—include 2-4 projects.",
                "Impress recruiters with 2-4 personal projects!",
                "Your projects tell your story—list 2-4.",
                // Expanded Variations: Demonstrate skills, initiative, problem-solving, results, 2-4 count
                "List 2-4 projects you're proud of.",
                "Add 2-4 examples of your work or passion projects.",
                "Detail 2-4 significant projects (personal or professional).",
                "Show your skills in action: add 2-4 projects.",
                "Include 2-4 projects that demonstrate your abilities.",
                "What cool things have you made? List 2-4.",
                "Highlight 2-4 key projects and your role in them.",
                "Add 2-4 tangible examples of your capabilities.",
                "Projects show initiative! Include 2-4.",
                "Let's add 2-4 of your notable project experiences.",
                "Got code, designs, or case studies? Link 2-4 projects.",
                "Impress with your portfolio: add 2-4 project highlights.",
                "Add 2-4 projects to illustrate your practical skills.",
                "Show problem-solving through 2-4 projects.",
                "Describe 2-4 significant accomplishments via projects."
            )

            CERTIFICATION_INDEX -> listOf(
                // Original Theme: Certs/Achievements, qualifications, credentials, awards, honors, 1-2 count
                "Add 1-2 certifications or key achievements.",
                "List 1-2 important qualifications or accomplishments.",
                "Showcase your credentials: Add 1-2 certifications or major achievements.",
                "Highlight 1-2 significant awards, certifications, or honors.",
                "Boost your profile with 1-2 relevant certifications or achievements.",
                // Expanded Variations: Recognition, validation, specialized knowledge, stand-out items, 1-2 count
                "Include 1-2 standout certifications or awards.",
                "Got formal recognition? Add 1-2 certifications or honors.",
                "List 1-2 key credentials or major accomplishments.",
                "Add 1-2 notable certifications or significant achievements.",
                "Highlight 1-2 ways you've been recognized (certificated, awards).",
                "Mention 1-2 important certifications or personal achievements.",
                "Showcase specialized knowledge with 1-2 certificates or awards.",
                "List 1-2 of your proudest achievements or certifications.",
                "Time to add 1-2 key qualifications, like certifications or honors.",
                "Include 1-2 items that validate your skills (certificate/achievements).",
                "Did you earn a certification or award? Add 1-2.",
                "Add 1-2 extra credentials or significant accomplishments here.",
                "Round out your profile with 1-2 certificates or key achievements.",
                "Show extra qualifications: Add 1-2 certifications or awards.",
                "Highlight 1-2 key accomplishments or certifications."
            )

            EDUCATION_INDEX -> listOf(
                // Original Theme: Add education, studied, background, qualifications, degrees/courses
                "Add your education details.",
                "Where did you study? Let’s add your education!",
                "Your academic background is important!",
                "Education matters—add your qualifications!",
                "Include your degrees and courses here.",
                // Expanded Variations: Institutions, dates, relevant coursework, GPA (optional), academic achievements
                "List your schools, degrees, and graduation dates.",
                "Detail your academic history: institutions, degrees, years.",
                "Outline your educational qualifications.",
                "Add universities, colleges, or relevant courses.",
                "Your schooling provides valuable context - list it here.",
                "Fill in your educational background details.",
                "Mention your degrees, majors, and institutions attended.",
                "Provide information about your academic journey.",
                "Let's add the Education section to your resume.",
                "Include relevant academic achievements if applicable.",
                "Don't forget your education - add it now!",
                "List your highest level of education and other relevant schooling.",
                "Where did you gain your foundational knowledge? Add it here.",
                "Time to detail your academic credentials.",
                "Enter the details of your degrees and diplomas."
            )

            else -> listOf(
                // Original Theme: Generic encouragement, progress, nearly done
                "Let's get your data!",
                "Tell us more about yourself.",
                "We’re almost done, keep going!",
                "Great progress! Let’s add more details.",
                "Your resume is shaping up nicely!",
                // Expanded Variations: Filling gaps, completeness, stronger resume, next step, polishing
                "Let's continue building your profile.",
                "Time to add details for this section.",
                "Keep up the great work! What's next?",
                "Adding information here makes your resume stronger.",
                "Let's fill in this part of your resume.",
                "Ready for the next section?",
                "What details should we add here?",
                "Your resume is getting better with each step!",
                "Continue providing your information.",
                "Let's complete this section.",
                "One more step closer to a great resume!",
                "Making good headway! Let's add this info.",
                "Focus on this section now.",
                "Help us understand more by filling this out.",
                "Keep the momentum going!"
            )
        }
        // Ensure randomness even if the list is somehow empty (fallback)
        return if (messages.isNotEmpty()) messages.random() else "Please provide the required information."
    }

    private var typewriterJob: Job? = null

    private fun startTypewriterEffect(
        textView: TextView,
        fullText: String,
        delayMillis: Long = 5
    ) {
        typewriterJob?.cancel()
        textView.text = "" // Clear text initially
        val chatCard = binding.formActivityChatCard
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

            // Wait before fading out
            delay(2000)

            withContext(Dispatchers.Main) {
                chatCard.animate()
                    .alpha(0f)
                    .setDuration(500) // Smooth fade-out
                    .start()
            }
        }
    }

//    private fun areAllFieldsFilled(): Boolean {
//        // Add more checks here for minimum requirements if needed
//        return !binding.etName.text.isNullOrEmpty() &&
//                !binding.etEmail.text.isNullOrEmpty() &&
//                !binding.etPhone.text.isNullOrEmpty()
//        // Example: && selectedSkills.size >= 10
//        // Example: && experienceAdapter.itemCount >= 2
//    }

    private fun scrollToNextCard() {
        Log.d("kabir", "scrollToNextCard called, current index: $currentVisibleCardIndex")
        val scrollView = binding.mainScrollView
        val container = binding.mainLinearLayout
        val childCount = container.childCount // Or use TOTAL_CARDS

        if (currentVisibleCardIndex < childCount - 1) {
            val nextCardIndex = currentVisibleCardIndex + 1
            val nextCard = container.getChildAt(nextCardIndex) ?: return // Safety check

            // Smooth scroll to the next card
            val targetScrollY = nextCard.top - 20 // Adjust '20' padding as needed
            scrollView.smoothScrollTo(0, targetScrollY)

            // Update UI after a short delay for scroll animation smoothness
            scrollView.postDelayed({
                updateUiForCard(nextCardIndex)
            }, 150) // Shorter delay might suffice for programmatic scroll

        } else {
            // Already at the last card, ensure FAB is hidden
            binding.fabNext.visibility = View.GONE
            Log.d("kabir", "Already at the last card.")
        }
    }

    // REMOVED: snapToNearestCard() function - No longer needed

    // Helper function to update NavRail, Chat, and FAB based on card index
    private fun updateUiForCard(index: Int) {
        if (index < 0 || index >= TOTAL_CARDS) {
            Log.w("UpdateUI", "Invalid index received: $index")
            return // Avoid index out of bounds
        }
        Log.d("UpdateUI", "Updating UI for card index: $index")
        currentVisibleCardIndex = index // Update the tracked index

        // Update NavigationRail Selection
        updateNavigationRailSelection(index)

        // Update Chat Text
        val sectionText = getSectionText(index)
        startTypewriterEffect(chatText, sectionText)

        // Update FAB Visibility
        val isLastCard = (index == TOTAL_CARDS - 1) // Check against total cards
        binding.fabNext.visibility = if (isLastCard) View.GONE else View.VISIBLE
        binding.btnSubmit.visibility = if (isLastCard) View.VISIBLE else View.GONE
        Log.d("UpdateUI", "FAB visibility set to: ${binding.fabNext.visibility}")
    }


    private fun updateNavigationRailSelection(index: Int) {
        val navRail = binding.inputNavRail

        // Map card index to navigation item ID
        val menuItems = listOf(
            com.codeNext.resumateAI.R.id.nav_personalInfo,
            com.codeNext.resumateAI.R.id.nav_skills,
            com.codeNext.resumateAI.R.id.nav_links,
            com.codeNext.resumateAI.R.id.nav_experience,
            com.codeNext.resumateAI.R.id.nav_projects,
            com.codeNext.resumateAI.R.id.nav_certification,
            com.codeNext.resumateAI.R.id.nav_education,
        )

        if (index in menuItems.indices) {
            navRail.menu?.findItem(menuItems[index])?.isChecked = true
        } else {
            Log.w("NavRailUpdate", "Index $index out of bounds for menu items.")
        }
    }

    private fun setupNavigationRail() {
        val navigationRailView = binding.inputNavRail

        navigationRailView.setOnItemSelectedListener { item ->
            val targetView: View?
            val sectionIndex: Int

            when (item.itemId) {
                com.codeNext.resumateAI.R.id.nav_personalInfo -> {
                    targetView = binding.cvPersonalInformation
                    sectionIndex = PERSONAL_INFO_INDEX
                    unfocus()
                }

                com.codeNext.resumateAI.R.id.nav_skills -> {
                    targetView = binding.cvSkills
                    sectionIndex = SKILLS_INDEX
                    unfocus()
                }

                com.codeNext.resumateAI.R.id.nav_links -> {
                    targetView = binding.cvLinks
                    sectionIndex = LINKS_INDEX
                    unfocus()
                }

                com.codeNext.resumateAI.R.id.nav_experience -> { // Corrected mapping
                    targetView = binding.cvExperience
                    sectionIndex = EXPERIENCE_INDEX
                }

                com.codeNext.resumateAI.R.id.nav_projects -> { // Corrected mapping
                    targetView = binding.cvProjects
                    sectionIndex = PROJECTS_INDEX
                    unfocus()
                }

                com.codeNext.resumateAI.R.id.nav_certification -> { // Corrected mapping
                    targetView = binding.cvCertifications
                    sectionIndex = CERTIFICATION_INDEX
                    unfocus()
                }

                com.codeNext.resumateAI.R.id.nav_education -> { // Corrected mapping
                    targetView = binding.cvEducation
                    sectionIndex = EDUCATION_INDEX

                    unfocus()
                }

                else -> {
                    targetView = null
                    sectionIndex = -1
                }
            }

            if (targetView != null && sectionIndex != -1) {
                // Smooth scroll to the target view
                val targetScrollY = targetView.top - 20 // Adjust padding
                binding.mainScrollView.smoothScrollTo(0, targetScrollY)

                // Update UI elements (Chat, FAB visibility, current index)
                // Use postDelayed to ensure UI updates after scroll starts/settles a bit
                binding.mainScrollView.postDelayed({
                    updateUiForCard(sectionIndex)
                }, 150) // Adjust delay if needed

            } else {
                Log.w(
                    "NavRailClick",
                    "No target view or invalid section index for item ${item.itemId}"
                )
            }

            true // Indicate the event was handled
        }
    }

    private fun genderAdapter() {
        val spinner = binding.spinnerGender // Reference to your AutoCompleteTextView for skills
        val genderOptions = listOf("Male", "Female", "Rather Not Say", "Non-Binary")
        val adapter = ArrayAdapter(
            this,
            R.layout.simple_dropdown_item_1line, genderOptions
        )
        spinner.setAdapter(adapter)

        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val desiredPercentage = 0.35f
        val calculatedHeight = (screenHeight * desiredPercentage).toInt()
        spinner.dropDownHeight = calculatedHeight

        spinner.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) spinner.showDropDown() }
        spinner.setOnClickListener { spinner.showDropDown() }
        spinner.setOnItemClickListener { _, _, _, _ ->
            spinner.clearFocus() // Hide keyboard/dropdown
        }
    }

    private fun skillsAdapter() {
        val editTextSkills = binding.etSkills // Reference to your AutoCompleteTextView for skills
        val adapter = ArrayAdapter(this, R.layout.simple_dropdown_item_1line, skillsList)

        editTextSkills.setAdapter(adapter)
        editTextSkills.threshold = 1 // Show suggestions after 1 character

        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val desiredPercentage = 0.35f // 35%
        val calculatedHeight = (screenHeight * desiredPercentage).toInt()
        editTextSkills.dropDownHeight = calculatedHeight

        editTextSkills.setOnItemClickListener { _, _, position, _ ->
            val selectedSkill = adapter.getItem(position) ?: return@setOnItemClickListener
            if (!selectedSkills.contains(selectedSkill)) {
                selectedSkills.add(selectedSkill)
                addChip(selectedSkill)
            }
            editTextSkills.text.clear()
        }
    }

    private fun addChip(skill: String) {
        Log.d("SkillsDebug", "Adding chip: $skill")
        val chip = Chip(this).apply {
            text = skill
            textSize = 10f
            chipCornerRadius = 12f
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                selectedSkills.remove(skill)
                binding.chipGroupSkills.removeView(this)
            }
        }
        binding.chipGroupSkills.addView(chip)
    }

    // Save resume data to file
    private fun saveResumeData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val userProfile = UserProfile(
                fullName = binding.etName.text.toString(),
                email = binding.etEmail.text.toString(),
                phoneNumber = binding.etPhone.text.toString(),
                address = binding.etAddress.text.toString(),
                jobProfile = binding.spinnerJobProfile.text.toString(),
                linkedIn = binding.etLinkedin.text.toString(),
                github = binding.etGithub.text.toString(),
                website = binding.etWebsite.text.toString(),
                objective = null, // Or get from an EditText if you add one
                gender = binding.spinnerGender.text.toString()
            )
            database.userProfileDao().insertOrUpdate(userProfile)

            database.skillsDao().deleteAll()
            selectedSkills.forEach { database.skillsDao().insert(Skills(skillName = it)) }

            // Clear and insert for items managed by adapters to ensure consistency
            database.achievementDao().deleteAllAchievements()
            achievementAdapter.getAchievements().forEach { database.achievementDao().insert(it) }

            database.projectDao().deleteAllProjects() // Clear before inserting current list
            val projectsToSave = projectAdapter.getProjects()
            Log.d("AISaveDebug", "[FormActivity] Saving ${projectsToSave.size} projects.")
            database.projectDao().upsertProjects(projectsToSave) // Use simple insert after delete

            database.certificationDao().deleteAllCertifications()
            certificationAdapter.getCertifications()
                .forEach { database.certificationDao().insert(it) }

            database.experienceDao().deleteAllExperiences() // Clear before inserting current list
            val experiencesToSave = experienceAdapter.getExperiences()
            Log.d("AISaveDebug", "[FormActivity] Saving ${experiencesToSave.size} experiences.")
            database.experienceDao().upsertExperiences(experiencesToSave) // Use simple insert

            database.educationDao().deleteAllEducation()
            educationAdapter.getQualifications().forEach { database.educationDao().insert(it) }

            Log.d("SaveData", "Data saved successfully.")
        }
    }

    // Fill the form with previously saved data
    private fun fillFormWithPreviousData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val userProfile = database.userProfileDao().getUser()
            val skills = database.skillsDao().getAllSkills().map { it.skillName }
            val achievements = database.achievementDao().getAllAchievements()
            val projects = database.projectDao().getAllProjects()
            val certifications = database.certificationDao().getAllCertifications()
            val experiences = database.experienceDao().getAllExperiences()
            val education = database.educationDao().getAllEducation()
            val gender = database.userProfileDao().getUser()?.gender

            withContext(Dispatchers.Main) {
                userProfile?.let {
                    binding.etName.setText(it.fullName)
                    binding.etEmail.setText(it.email)
                    binding.etPhone.setText(it.phoneNumber)
                    binding.etAddress.setText(it.address)
                    binding.spinnerJobProfile.setText(it.jobProfile, false) // Don't filter
                    binding.etLinkedin.setText(it.linkedIn)
                    binding.etGithub.setText(it.github)
                    binding.etWebsite.setText(it.website)
                    binding.spinnerGender.setText(gender, false)
                }

                // Load skills and create chips
                selectedSkills.clear()
                selectedSkills.addAll(skills)
                loadSkills(selectedSkills) // Load chips based on DB data

                // Set adapter data
                achievementAdapter.setAchievements(achievements)
                projectAdapter.setProjects(projects)
                certificationAdapter.setCertifications(certifications)
                experienceAdapter.setExperiences(experiences)
                educationAdapter.setQualifications(education)

                // Update visibility based on loaded data
                updateRecyclerViewVisibility() // Call this AFTER adapters are populated
            }
        }
    }

    // Update RecyclerView visibility dynamically (call this after loading data)
    private fun updateRecyclerViewVisibility() {
        // This can now directly check adapter item counts on the main thread
        // as it's called after data loading.
        lifecycleScope.launch(Dispatchers.Main) {
            with(binding) {
                recyclerAchievements.visibility =
                    if (achievementAdapter.itemCount > 0) View.VISIBLE else View.GONE
                recyclerProjects.visibility =
                    if (projectAdapter.itemCount > 0) View.VISIBLE else View.GONE
                recyclerCertifications.visibility =
                    if (certificationAdapter.itemCount > 0) View.VISIBLE else View.GONE
                recyclerExperience.visibility =
                    if (experienceAdapter.itemCount > 0) View.VISIBLE else View.GONE
                recyclerEducation.visibility =
                    if (educationAdapter.itemCount > 0) View.VISIBLE else View.GONE
            }
        }
    }

    private fun loadSkills(skills: List<String>) {
        binding.chipGroupSkills.removeAllViews() // Clear existing chips
        // Add chips for the loaded skills. The 'selectedSkills' list is already updated.
        skills.forEach { addChip(it) }
    }
    private fun setupSpinner() {
        val spinner = binding.spinnerJobProfile
        val adapter = ArrayAdapter(this, R.layout.simple_dropdown_item_1line, JobList.jobList)
        spinner.setAdapter(adapter)

        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val desiredPercentage = 0.35f
        val calculatedHeight = (screenHeight * desiredPercentage).toInt()
        spinner.dropDownHeight = calculatedHeight

        spinner.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) spinner.showDropDown() }
        spinner.setOnClickListener { spinner.showDropDown() }
        spinner.setOnItemClickListener { parent, _, position, _ ->
            val selectedJob = parent.getItemAtPosition(position).toString()
            // Toast.makeText(this, "Selected: $selectedJob", Toast.LENGTH_SHORT).show() // Optional toast
            spinner.clearFocus() // Hide keyboard/dropdown
        }
    }

    private fun setupFragments() {
        binding.btnAddAchievement.setOnClickListener {
            openDialog(
                AddAchievementFragment(
                    onAdd = { achievement -> addAchievementToList(achievement) },
                    onEdit = null, achievementToEdit = null, position = null
                )
            )
        }
        binding.btnAddProject.setOnClickListener {

            openDialog(
                AddProjectFragment(
                    onAdd = { project -> addProjectToList(project) },
                    onEdit = null, projectToEdit = null, position = null
                )
            )
        }
        binding.btnAddCertification.setOnClickListener {
            openDialog(
                AddCertificationFragment(
                    onAdd = { certification -> addCertificationToList(certification) },
                    onEdit = null, certificationToEdit = null, position = null
                )
            )
        }
        binding.btnAddExperience.setOnClickListener {
            openDialog(
                AddExperienceFragment(
                    onAdd = { experience -> addExperienceToList(experience) },
                    onEdit = null, experienceToEdit = null, position = null
                )
            )
        }
        binding.btnAddEducation.setOnClickListener {
            openDialog(
                AddEducationFragment(
                    onAdd = { education -> addEducationToList(education) },
                    onEdit = null, educationToEdit = null, position = null
                )
            )
        }
    }

    private fun setupAdapters() {
        achievementAdapter = AchievementAdapter(mutableListOf(),
            onEdit = { achievement, position -> editAchievement(achievement, position) },
            onRemove = { position -> removeAchievement(position) }
        )
        binding.recyclerAchievements.apply {
            layoutManager = LinearLayoutManager(this@FormActivity)
            adapter = achievementAdapter
        }

        projectAdapter = ProjectAdapter(mutableListOf(),
            onEdit = { project, position -> editProject(project, position) },
            onRemove = { position -> removeProject(position) }
        )
        binding.recyclerProjects.apply {
            layoutManager = LinearLayoutManager(this@FormActivity)
            adapter = projectAdapter
        }

        certificationAdapter = CertificationAdapter(mutableListOf(),
            onEdit = { certification, position -> editCertification(certification, position) },
            onRemove = { position -> removeCertification(position) }
        )
        binding.recyclerCertifications.apply {
            layoutManager = LinearLayoutManager(this@FormActivity)
            adapter = certificationAdapter
        }

        experienceAdapter = ExperienceAdapter(mutableListOf(),
            onEdit = { experience, position -> editExperience(experience, position) },
            onRemove = { position -> removeExperience(position) }
        )
        binding.recyclerExperience.apply {
            layoutManager = LinearLayoutManager(this@FormActivity)
            adapter = experienceAdapter
        }

        educationAdapter = EducationAdapter(mutableListOf(),
            onEdit = { education, position -> editEducation(education, position) },
            onRemove = { position -> removeEducation(position) }
        )
        binding.recyclerEducation.apply {
            layoutManager = LinearLayoutManager(this@FormActivity)
            adapter = educationAdapter
        }
    }

    private fun openDialog(fragment: DialogFragment) {
        fragment.show(supportFragmentManager, fragment::class.java.simpleName)
    }

    // Add functions
    private fun addAchievementToList(achievement: Achievement) {
        achievementAdapter.addAchievement(achievement)
        binding.recyclerAchievements.visibility =
            View.VISIBLE // Ensure visible when adding first item
    }

    private fun addProjectToList(project: Project) {
        projectAdapter.addProject(project)
        binding.recyclerProjects.visibility = View.VISIBLE
    }

    private fun addCertificationToList(certification: Certification) {
        certificationAdapter.addCertification(certification)
        binding.recyclerCertifications.visibility = View.VISIBLE
    }

    private fun addExperienceToList(experience: Experience) {
        experienceAdapter.addExperience(experience)
        binding.recyclerExperience.visibility = View.VISIBLE
    }

    private fun addEducationToList(education: Education) {
        educationAdapter.addEducation(education)
        binding.recyclerEducation.visibility = View.VISIBLE
    }

    // Edit Functions
    private fun editAchievement(achievement: Achievement, position: Int) {
        val fragment = AddAchievementFragment(
            onAdd = { /* Not used in edit mode */ },
            onEdit = { updatedAchievement, pos -> // Use onEdit lambda
                achievementAdapter.updateAchievement(pos, updatedAchievement)
            },
            achievementToEdit = achievement,
            position = position
        )
        fragment.show(supportFragmentManager, "EditAchievementFragment")
    }

    private fun editCertification(certification: Certification, position: Int) {
        val fragment = AddCertificationFragment(
            onAdd = { /* Not used */ },
            onEdit = { updatedCertification, pos ->
                certificationAdapter.updateCertification(pos, updatedCertification)
            },
            certificationToEdit = certification, position = position
        )
        fragment.show(supportFragmentManager, "EditCertificationFragment")
    }

    private fun editProject(project: Project, position: Int) {
        val fragment = AddProjectFragment(
            onAdd = { /* Not used */ },
            onEdit = { updatedProject, pos ->
                projectAdapter.updateProject(pos, updatedProject)
            },
            projectToEdit = project, position = position
        )
        fragment.show(supportFragmentManager, "EditProjectFragment")
    }

    private fun editExperience(experience: Experience, position: Int) {
        val fragment = AddExperienceFragment(
            onAdd = { /* Not used */ },
            onEdit = { updatedExperience, pos ->
                experienceAdapter.updateExperience(pos, updatedExperience)
            },
            experienceToEdit = experience, position = position
        )
        fragment.show(supportFragmentManager, "EditExperienceFragment")
    }

    private fun editEducation(education: Education, position: Int) {
        val fragment = AddEducationFragment(
            onAdd = { /* Not used */ },
            onEdit = { updatedEducation, pos ->
                educationAdapter.updateEducation(pos, updatedEducation)
            },
            educationToEdit = education, position = position
        )
        fragment.show(supportFragmentManager, "EditEducationFragment")
    }

    // Remove Confirmation Dialog
    private fun showRemoveDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> onConfirm() }
            .setNegativeButton("No", null) // Simpler way to dismiss
            .show()
    }

    // Remove functions
    private fun removeAchievement(position: Int) {
        showRemoveDialog("Remove Achievement", "Are you sure?") {
            achievementAdapter.removeAchievement(position)
            if (achievementAdapter.itemCount == 0) binding.recyclerAchievements.visibility =
                View.GONE
        }
    }

    private fun removeCertification(position: Int) {
        showRemoveDialog("Remove Certification", "Are you sure?") {
            certificationAdapter.removeCertification(position)
            if (certificationAdapter.itemCount == 0) binding.recyclerCertifications.visibility =
                View.GONE
        }
    }

    private fun removeProject(position: Int) {
        showRemoveDialog("Remove Project", "Are you sure?") {
            projectAdapter.removeProject(position)
            if (projectAdapter.itemCount == 0) binding.recyclerProjects.visibility = View.GONE
        }
    }

    private fun removeExperience(position: Int) {
        showRemoveDialog("Remove Experience", "Are you sure?") {
            experienceAdapter.removeExperience(position)
            if (experienceAdapter.itemCount == 0) binding.recyclerExperience.visibility = View.GONE
        }
    }

    private fun removeEducation(position: Int) {
        showRemoveDialog("Remove Education", "Are you sure?") {
            educationAdapter.removeEducation(position)
            if (educationAdapter.itemCount == 0) binding.recyclerEducation.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up the listener to prevent memory leaks
        binding.root.viewTreeObserver.removeOnGlobalLayoutListener(keyboardVisibilityListener)
    }
}