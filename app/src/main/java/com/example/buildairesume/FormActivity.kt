package com.example.buildairesume

import android.R
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.buildairesume.SkillsList.skillsList
import com.example.buildairesume.adapters.AchievementAdapter
import com.example.buildairesume.adapters.CertificationAdapter
import com.example.buildairesume.adapters.EducationAdapter
import com.example.buildairesume.adapters.ExperienceAdapter
import com.example.buildairesume.adapters.ProjectAdapter
import com.example.buildairesume.databinding.ActivityFormBinding
import com.example.buildairesume.fragments.AddAchievementFragment
import com.example.buildairesume.fragments.AddCertificationFragment
import com.example.buildairesume.fragments.AddEducationFragment
import com.example.buildairesume.fragments.AddExperienceFragment
import com.example.buildairesume.fragments.AddProjectFragment
import com.example.buildairesume.models.Achievement
import com.example.buildairesume.models.Certification
import com.example.buildairesume.models.Education
import com.example.buildairesume.models.Experience
import com.example.buildairesume.models.Project
import com.example.buildairesume.models.Skills
import com.example.buildairesume.models.UserProfile
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = UserData.getInstance(this)
        chatText = binding.formActivityChat
        setupAdapters()
        fillFormWithPreviousData()
        skillsAdapter()
        setupFragments()
        updateRecyclerViewVisibility()
        binding.btnSubmit.setOnClickListener {
            if (areAllFieldsFilled()) {  // ✅ Check if all fields are filled
                saveResumeData()
                val intent = Intent(this, OutputActivity::class.java)
                startActivity(intent)
                Log.d("kabir", "Generate Button pressed")
            } else {
                startTypewriterEffect(
                    chatText,
                    "Uh oh ! Looks like you forgot to fill some important information",
                    50
                )
            }
        }

        binding.fabNext.setOnClickListener {
            Log.d("kabir", "FAB Next Button pressed")
            scrollToNextCard()
        }

        val welcomeTextList = listOf(
            "Let's start with your personal information!",
            "Tell us about yourself first.",
            "Basic details help build a great resume!",
            "Who are you? Let’s add your personal info!",
            "Your journey starts with personal details."
        )

        startTypewriterEffect(chatText, welcomeTextList.random(), 50)

        setupNavigationRail()

        binding.lottieAnimCard.setOnClickListener{
            showChat()
        }

        setupSpinner()

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


    private fun getSectionText(index: Int): String {
        val messages = when (index) {
            0 -> listOf(
                "Let's start with your personal information!",
                "Tell us about yourself first.",
                "Basic details help build a great resume!",
                "Who are you? Let’s add your personal info!",
                "Your journey starts with personal details."
            )

            1 -> listOf(
                "You got skills! Add at least 10-15 skills.",
                "Show off your expertise—list at least 10 skills!",
                "Let’s add your skills. Aim for 10-15 key strengths!",
                "Highlight your abilities! Add at least 10 strong skills.",
                "A strong resume has strong skills—add at least 10!"
            )

            2 -> listOf(
                "Add your important links here.",
                "Do you have a portfolio or LinkedIn? Add them!",
                "Your links help recruiters find you easily.",
                "Make sure to include your professional links!",
                "Boost your profile by adding key links."
            )

            3 -> listOf(
                "Add at least 2 work experiences or internships!",
                "Where have you worked before? Add at least 2 roles!",
                "Showcase your career journey—list at least 2 experiences.",
                "Your work history matters—add 2+ jobs or internships!",
                "Employers love experience—add at least 2 positions!"
            )

            4 -> listOf(
                "Showcase your projects! Add at least 3.",
                "What have you built? Add a minimum of 3 projects!",
                "Highlight your best work—include at least 3 projects.",
                "Impress recruiters with 3+ personal projects!",
                "Your projects tell your story—list at least 3."
            )

            5 -> listOf(
                "List your certifications here.",
                "Certifications boost your resume—add them!",
                "Have any extra qualifications? Add them here!",
                "Showcase your achievements with certifications.",
                "Professional certificates make a difference!"
            )

            6 -> listOf(
                "Add your education details.",
                "Where did you study? Let’s add your education!",
                "Your academic background is important!",
                "Education matters—add your qualifications!",
                "Include your degrees and courses here."
            )

            else -> listOf(
                "Let's get your data!",
                "Tell us more about yourself.",
                "We’re almost done, keep going!",
                "Great progress! Let’s add more details.",
                "Your resume is shaping up nicely!"
            )
        }

        return messages.random()
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

    private fun areAllFieldsFilled(): Boolean {
        return !binding.etName.text.isNullOrEmpty() &&
                !binding.etEmail.text.isNullOrEmpty() &&
                !binding.etPhone.text.isNullOrEmpty()
    }

    private fun scrollToNextCard() {
        Log.d("kabir", "scrollToNextCard called")
        val scrollView = binding.mainScrollView
        val container = binding.mainLinearLayout
        val childCount = container.childCount

        Log.d("kabir", "Child count: $childCount")
        var currentCardIndex = -1

        // Find the card that is currently snapped at the top
        for (i in 0 until childCount) {
            Log.d("kabir", "Checking card index: $i")
            val cardView = container.getChildAt(i)
            Log.d("kabir", "Card top: ${cardView.top}, ScrollY: ${scrollView.scrollY}")

            if (Math.abs(scrollView.scrollY - cardView.top) < scrollView.height / 2.8) {
                currentCardIndex = i
                break
            }
        }

        Log.d("kabir", "Current card index: $currentCardIndex")

        // Scroll to the next card if it exists
        if (currentCardIndex in 0 until childCount - 1) {
            val nextCardIndex = currentCardIndex + 1
            val nextCard = container.getChildAt(nextCardIndex)

            // 🔹 Smooth scroll and update NavigationRail AFTER animation finishes
            scrollView.post {
                scrollView.smoothScrollTo(0, nextCard.top - 20)

                scrollView.postDelayed({
                    updateNavigationRailSelection(nextCardIndex) // Update after scrolling

                    // 🔹 Update and animate the chat text based on the new section
                    val sectionText = getSectionText(nextCardIndex)
                    startTypewriterEffect(chatText, sectionText)

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

            // 🔹 Update FAB visibility when snapping
            val isLastCard = nearestIndex == childCount - 1
            binding.fabNext.visibility = if (isLastCard) View.GONE else View.VISIBLE

            // 🔹 Update and animate the header text based on the section
            val sectionText = getSectionText(nearestIndex)
            startTypewriterEffect(chatText, sectionText)
        }
    }

    private fun updateNavigationRailSelection(index: Int) {
        val navRail = binding.inputNavRail

        // Map card index to navigation item ID
        val menuItems = listOf(
            com.example.buildairesume.R.id.nav_personalInfo,
            com.example.buildairesume.R.id.nav_skills,
            com.example.buildairesume.R.id.nav_links,
            com.example.buildairesume.R.id.nav_experience,
            com.example.buildairesume.R.id.nav_projects,
            com.example.buildairesume.R.id.nav_certification,
            com.example.buildairesume.R.id.nav_education,
        )

        if (index in menuItems.indices) {
            navRail?.menu?.findItem(menuItems[index])?.isChecked = true
        }
    }

    private fun setupNavigationRail() {
        val navigationRailView = binding.inputNavRail

        navigationRailView?.setOnItemSelectedListener { item ->
            val targetView: View? = when (item.itemId) {
                com.example.buildairesume.R.id.nav_personalInfo -> binding.cvPersonalInformation
                com.example.buildairesume.R.id.nav_skills -> binding.cvSkills
                com.example.buildairesume.R.id.nav_links -> binding.cvLinks
                com.example.buildairesume.R.id.nav_certification -> binding.cvCertifications
                com.example.buildairesume.R.id.nav_projects -> binding.cvProjects
                com.example.buildairesume.R.id.nav_experience -> binding.cvExperience
                com.example.buildairesume.R.id.nav_education -> binding.cvEducation
                else -> null
            }

            targetView?.let {
                binding.mainScrollView?.smoothScrollTo(0, it.top)

                // 🔹 Update chat text with typewriter effect
                val sectionIndex = when (item.itemId) {
                    com.example.buildairesume.R.id.nav_personalInfo -> 0
                    com.example.buildairesume.R.id.nav_skills -> 1
                    com.example.buildairesume.R.id.nav_links -> 2
                    com.example.buildairesume.R.id.nav_experience -> 3
                    com.example.buildairesume.R.id.nav_projects -> 4
                    com.example.buildairesume.R.id.nav_certification -> 5
                    com.example.buildairesume.R.id.nav_education -> 6
                    else -> -1
                }

                if (sectionIndex != -1) {
                    val sectionText = getSectionText(sectionIndex)
                    startTypewriterEffect(chatText, sectionText) // 🔹 Update chat text
                }

                // 🔹 Hide FAB when at last section
                binding.fabNext.visibility = if (sectionIndex == 6) View.GONE else View.VISIBLE
                Log.d("kabir", "FAB visibility set to: ${binding.fabNext.visibility}")
            }

            true
        }
    }

    private fun skillsAdapter() {


        val adapter = ArrayAdapter(this, R.layout.simple_dropdown_item_1line, skillsList)
        binding.etSkills.setAdapter(adapter)
        binding.etSkills.threshold = 1 // Show suggestions after 1 character

        // Handle item selection
        binding.etSkills.setOnItemClickListener { _, _, position, _ ->
            val selectedSkill = adapter.getItem(position) ?: return@setOnItemClickListener

            if (!selectedSkills.contains(selectedSkill)) {
                selectedSkills.add(selectedSkill) // Add to list
                addChip(selectedSkill) // Create a chip
            }

            binding.etSkills.text.clear() // Clear input after selection
        }
    }

    private fun addChip(skill: String) {
        Log.d("SkillsDebug", "Adding chip: $skill") // Debug Log
        val chip = Chip(this).apply {
            text = skill
            textSize = 10f
            chipCornerRadius = 12f
            isCloseIconVisible = true // Show the close icon
            setOnCloseIconClickListener {
                selectedSkills.remove(skill) // Remove from list
                binding.chipGroupSkills.removeView(this) // Remove chip
            }
        }
        binding.chipGroupSkills.addView(chip) // Add chip to the group
    }

    // Save resume data to file
    private fun saveResumeData() {
        lifecycleScope.launch(Dispatchers.IO) {  // Runs on a background thread
            val userProfile = UserProfile(
                fullName = binding.etName.text.toString(),
                email = binding.etEmail.text.toString(),
                phoneNumber = binding.etPhone.text.toString(),
                address = binding.etAddress.text.toString(),
                jobProfile = binding.spinnerJobProfile.text.toString(),
                linkedIn = binding.etLinkedin.text.toString(),
                github = binding.etGithub.text.toString(),
                website = binding.etWebsite.text.toString(),
                objective = null
            )

            database.userProfileDao().insertOrUpdate(userProfile)

            database.skillsDao().deleteAll()
            selectedSkills.forEach { database.skillsDao().insert(Skills(skillName = it)) }

            database.achievementDao().deleteAllAchievements()
            achievementAdapter.getAchievements().forEach { database.achievementDao().insert(it) }

            database.projectDao().deleteAllProjects()
            projectAdapter.getProjects().forEach { database.projectDao().insert(it) }

            database.certificationDao().deleteAllCertifications()
            certificationAdapter.getCertifications()
                .forEach { database.certificationDao().insert(it) }

            database.experienceDao().deleteAllExperiences()
            experienceAdapter.getExperiences().forEach { database.experienceDao().insert(it) }

            database.educationDao().deleteAllEducation()
            educationAdapter.getQualifications().forEach { database.educationDao().insert(it) }
        }
    }

    // Fill the form with previously saved data
    private fun fillFormWithPreviousData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val userProfile = database.userProfileDao().getUser()

            withContext(Dispatchers.Main) {
                userProfile?.let {
                    binding.etName.setText(it.fullName)
                    binding.etEmail.setText(it.email)
                    binding.etPhone.setText(it.phoneNumber)
                    binding.etAddress.setText(it.address)
                    binding.spinnerJobProfile.setText(it.jobProfile, false)
                    binding.etLinkedin.setText(it.linkedIn)
                    binding.etGithub.setText(it.github)
                    binding.etWebsite.setText(it.website)
                }
            }

            // Load skills from Room database
            val skills = database.skillsDao().getAllSkills().map { it.skillName }

            withContext(Dispatchers.Main) {
                selectedSkills.clear()  // Clear old data
                selectedSkills.addAll(skills)
                loadSkills(selectedSkills) // Ensure chips are displayed
            }

            // Load other data
            val achievements = database.achievementDao().getAllAchievements()
            val projects = database.projectDao().getAllProjects()
            val certifications = database.certificationDao().getAllCertifications()
            val experiences = database.experienceDao().getAllExperiences()
            val education = database.educationDao().getAllEducation()

            withContext(Dispatchers.Main) {
                achievementAdapter.setAchievements(achievements)
                projectAdapter.setProjects(projects)
                certificationAdapter.setCertifications(certifications)
                experienceAdapter.setExperiences(experiences)
                educationAdapter.setQualifications(education)
            }
        }
    }

    // Update RecyclerView visibility dynamically
    private fun updateRecyclerViewVisibility() {
        lifecycleScope.launch(Dispatchers.IO) {
            val hasAchievements = database.achievementDao().getAllAchievements().isNotEmpty()
            val hasProjects = database.projectDao().getAllProjects().isNotEmpty()
            val hasCertifications = database.certificationDao().getAllCertifications().isNotEmpty()
            val hasExperiences = database.experienceDao().getAllExperiences().isNotEmpty()
            val hasEducation = database.educationDao().getAllEducation().isNotEmpty()

            withContext(Dispatchers.Main) {
                with(binding) {
                    recyclerAchievements.visibility =
                        if (hasAchievements) View.VISIBLE else View.GONE
                    recyclerProjects.visibility = if (hasProjects) View.VISIBLE else View.GONE
                    recyclerCertifications.visibility =
                        if (hasCertifications) View.VISIBLE else View.GONE
                    recyclerExperience.visibility = if (hasExperiences) View.VISIBLE else View.GONE
                    recyclerEducation.visibility = if (hasEducation) View.VISIBLE else View.GONE
                }
            }
        }
    }


    private fun loadSkills(skills: List<String>) {
        binding.chipGroupSkills.removeAllViews() // Clear old chips to prevent duplication

        for (skill in skills) {
            if (!selectedSkills.contains(skill)) {
                selectedSkills.add(skill)
            }
            addChip(skill) // Add chips properly
        }
    }


    private fun setupSpinner() {
        val spinner = binding.spinnerJobProfile
        val adapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, JobList.jobList)

        spinner.setAdapter(adapter)
        spinner.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) spinner.showDropDown()
        }
        spinner.setOnClickListener {
            spinner.showDropDown()
        }
        spinner.setOnItemClickListener { parent, _, position, _ ->
            val selectedJob = parent.getItemAtPosition(position).toString()
            Toast.makeText(this, "Selected: $selectedJob", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFragments() {
        // Open Achievement Fragment with callback
        binding.btnAddAchievement.setOnClickListener {
            openDialog(
                AddAchievementFragment(
                    onAdd = { achievement -> addAchievementToList(achievement) },
                    onEdit = null, // No edit function needed when adding a new achievement
                    achievementToEdit = null, // No achievement to edit
                    position = null
                )
            )
        }

        // Open Project Fragment with callback
        binding.btnAddProject.setOnClickListener {
            openDialog(
                AddProjectFragment(
                    onAdd = { project -> addProjectToList(project) },
                    onEdit = null, // No edit function needed when adding a new project
                    projectToEdit = null, // No project to edit
                    position = null
                )
            )
        }

        // Open Certification Fragment with callback
        binding.btnAddCertification.setOnClickListener {
            openDialog(
                AddCertificationFragment(
                    onAdd = { certification -> addCertificationToList(certification) },
                    onEdit = null, // No edit function needed when adding a new certification
                    certificationToEdit = null, // No certification to edit
                    position = null
                )
            )

        }

        binding.btnAddExperience.setOnClickListener {
            openDialog(
                AddExperienceFragment(
                    onAdd = { experience -> addExperienceToList(experience) },
                    onEdit = null, // No edit function needed when adding a new experience
                    experienceToEdit = null, // No experience to edit
                    position = null
                )
            )
        }

        binding.btnAddEducation.setOnClickListener {
            openDialog(
                AddEducationFragment(
                    onAdd = { education -> addEducationToList(education) },
                    onEdit = null, // No edit function needed when adding a new experience
                    educationToEdit = null, // No experience to edit
                    position = null
                )
            )
        }
    }


    private fun setupAdapters() {
        achievementAdapter = AchievementAdapter(mutableListOf(), onEdit = { achievement, position ->
            editAchievement(
                achievement, position
            )
        },  // Pass edit function
            onRemove = { position -> removeAchievement(position) }  // Pass remove function
        )
        binding.recyclerAchievements.apply {
            layoutManager = LinearLayoutManager(this@FormActivity)
            adapter = achievementAdapter
        }

        projectAdapter = ProjectAdapter(mutableListOf(),
            onEdit = { project, position -> editProject(project, position) },
            onRemove = { position -> removeProject(position) })
        binding.recyclerProjects.apply {
            layoutManager = LinearLayoutManager(this@FormActivity)
            adapter = projectAdapter
        }

        certificationAdapter = CertificationAdapter(mutableListOf(),
            onEdit = { certification, position -> editCertification(certification, position) },
            onRemove = { position -> removeCertification(position) })
        binding.recyclerCertifications.apply {
            layoutManager = LinearLayoutManager(this@FormActivity)
            adapter = certificationAdapter
        }

        experienceAdapter = ExperienceAdapter(mutableListOf(),
            onEdit = { experience, position -> editExperience(experience, position) },
            onRemove = { position -> removeExperience(position) })
        binding.recyclerExperience.apply {
            layoutManager = LinearLayoutManager(this@FormActivity)
            adapter = experienceAdapter
        }

        educationAdapter = EducationAdapter(mutableListOf(),
            onEdit = { education, position -> editEducation(education, position) },
            onRemove = { position -> removeEducation(position) })
        binding.recyclerEducation.apply {
            layoutManager = LinearLayoutManager(this@FormActivity)
            adapter = educationAdapter
        }
    }


    // Function to open a DialogFragment
    private fun openDialog(fragment: DialogFragment) {
        fragment.show(supportFragmentManager, fragment::class.java.simpleName)
    }


    // Functions to update lists (to be implemented)
    private fun addAchievementToList(achievement: Achievement) {
        achievementAdapter.addAchievement(achievement)
        binding.recyclerAchievements.visibility = View.VISIBLE
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


    // **Edit Functions**
    // In FormActivity.kt, modify the editAchievement function:
    private fun editAchievement(achievement: Achievement, position: Int) {
        val fragment = AddAchievementFragment(
            onAdd = { updatedAchievement ->
                achievementAdapter.updateAchievement(position, updatedAchievement)
            },
            onEdit = { updatedAchievement, pos ->
                achievementAdapter.updateAchievement(pos, updatedAchievement)
            }, // Add this line
            achievementToEdit = achievement,
            position = position
        )
        fragment.show(supportFragmentManager, "EditAchievementFragment")
    }

    private fun editCertification(certification: Certification, position: Int) {
        val fragment = AddCertificationFragment(
            onAdd = { updatedCertification ->
                certificationAdapter.updateCertification(position, updatedCertification)
            },
            onEdit = { updatedCertification, pos ->
                certificationAdapter.updateCertification(pos, updatedCertification)
            },
            certificationToEdit = certification,
            position = position
        )
        fragment.show(supportFragmentManager, "EditCertificationFragment")
    }

    private fun editProject(project: Project, position: Int) {
        val fragment = AddProjectFragment(
            onAdd = { updatedProject ->
                projectAdapter.updateProject(position, updatedProject)
            },
            onEdit = { updatedProject, pos ->
                projectAdapter.updateProject(pos, updatedProject)
            },
            projectToEdit = project,
            position = position
        )
        fragment.show(supportFragmentManager, "EditProjectFragment")
    }

    private fun editExperience(experience: Experience, position: Int) {
        val fragment = AddExperienceFragment(
            onAdd = { updatedExperience ->
                experienceAdapter.updateExperience(position, updatedExperience)
            },
            onEdit = { updatedExperience, pos ->
                experienceAdapter.updateExperience(pos, updatedExperience)
            },
            experienceToEdit = experience, position = position
        )
        fragment.show(supportFragmentManager, "EditExperienceFragment")
    }

    private fun editEducation(education: Education, position: Int) {
        val fragment = AddEducationFragment(
            onAdd = { updatedEducation ->
                educationAdapter.updateEducation(position, updatedEducation)
            },
            onEdit = { updatedEducation, pos ->
                educationAdapter.updateEducation(pos, updatedEducation)
            },
            educationToEdit = education, position = position
        )
        fragment.show(supportFragmentManager, "EditEducationFragment")
    }


    // Common function for showing a remove confirmation dialog
    private fun showRemoveDialog(title: String, message: String, onConfirm: () -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Yes") { _, _ -> onConfirm() }
        builder.setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }


    // remove functions
    private fun removeAchievement(position: Int) {
        showRemoveDialog(
            "Remove Achievement", "Are you sure you want to remove this achievement?"
        ) {
            achievementAdapter.removeAchievement(position)
            if (achievementAdapter.itemCount == 0) binding.recyclerAchievements.visibility =
                View.GONE
        }
    }

    private fun removeCertification(position: Int) {
        showRemoveDialog(
            "Remove Certification", "Are you sure you want to remove this certification?"
        ) {
            certificationAdapter.removeCertification(position)
            if (certificationAdapter.itemCount == 0) binding.recyclerCertifications.visibility =
                View.GONE
        }
    }

    private fun removeProject(position: Int) {
        showRemoveDialog("Remove Project", "Are you sure you want to remove this project?") {
            projectAdapter.removeProject(position)
            if (projectAdapter.itemCount == 0) binding.recyclerProjects.visibility = View.GONE
        }
    }

    private fun removeExperience(position: Int) {
        showRemoveDialog("Remove Experience", "Are you sure you want to remove this experience?") {
            experienceAdapter.removeExperience(position)
            if (experienceAdapter.itemCount == 0) binding.recyclerExperience.visibility = View.GONE
        }
    }

    private fun removeEducation(position: Int) {
        showRemoveDialog("Remove Education", "Are you sure you want to remove this education?") {
            educationAdapter.removeEducation(position)
            if (educationAdapter.itemCount == 0) binding.recyclerEducation.visibility = View.GONE
        }
    }


    override fun onStop() {
        super.onStop()
        saveResumeData()
    }


}
