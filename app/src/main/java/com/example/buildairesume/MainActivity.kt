package com.example.buildairesume

import android.animation.ValueAnimator
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.buildairesume.databinding.ActivityMainBinding
import com.example.buildairesume.models.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var database: UserData
    private lateinit var binding: ActivityMainBinding
    private var userProfile: UserProfile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val drawer_layout = binding.drawerLayout
        val menu_button = binding.btnMenu
        val navigation_view = binding.navigationView

        menu_button.setOnClickListener {
            drawer_layout.openDrawer(navigation_view)
        }

        navigation_view.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_about -> {
                    // Handle home navigation
                    true}
                R.id.nav_github ->{
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Kabirlakhadive/ResumateAI/tree/main"))
                    startActivity(intent)
                    true
                }
                R.id.nav_feedback ->{
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/forms/d/e/1FAIpQLScK2NCsX4Ih4tBI7NHyzUf-AqzFyAr3nbK05IIydbwCoCJwYA/viewform?usp=header"))
                    startActivity(intent)
                    true
                }
                else -> {false}
            }
        }

        database = UserData.getInstance(this)
        loadUserData()
        setupClickListeners()


        Log.d("MainActivity", "onCreate called")

        setupActivity()


    }

    private fun setupActivity() {
        val welcomeMessages = listOf(
            "Hi there! I'm ResuMate, ready to guide you through the resume-building process. Tap on 'Edit Resume Information' to get started with AI assistance. Need help? Tap on me anytime!",
            "Welcome! I'm ResuMate, your AI-powered resume assistant. Let's build your personalized resume—tap on 'Edit Resume Information' to begin! If you're stuck, tap on my icon for a hint.",
            "Hello! I'm ResuMate, here to make resume creation simple. Tap 'Edit Resume Information' to start crafting your perfect resume with AI. Lost? Just tap on me!",
            "Hey! I'm ResuMate, your AI resume helper. Let's get started—tap on 'Edit Resume Information' and begin building your resume! If you need guidance, tap my icon anytime.",
            "Greetings! I'm ResuMate, your AI assistant for resumes. Tap 'Edit Resume Information' to create a professional resume effortlessly. Need a hint? Just tap on my icon!",
            "Welcome! ResuMate here, ready to assist you in building a great resume. Just tap 'Edit Resume Information' to begin! If you ever feel lost, tap on me for help.",
            "Hi! I'm ResuMate, and I'm here to simplify your resume-building journey. Tap on 'Edit Resume Information' to get started! Not sure what to do next? Just tap my icon.",
            "Hello there! ResuMate at your service. Let's create an amazing resume together—tap 'Edit Resume Information' to begin. Stuck? Tap on me for a helpful hint!",
            "Hey! Need a resume? I'm ResuMate, your AI-powered assistant. Tap 'Edit Resume Information' to start building yours today! If you need a nudge, just tap my icon.",
            "Welcome to ResuMate! I'm here to help you craft a standout resume. Tap on 'Edit Resume Information' to begin the process. If you're ever unsure, tap on me for guidance!"
        )
        val welcomeBackMessages = listOf(
            "Welcome back! Here’s the resume you created earlier.",
            "Hey there! Your previously generated resume is ready for you.",
            "Welcome back! Here’s your resume from last time.",
            "Glad to see you again! Your resume is right here.",
            "You're back! Here’s the resume you worked on earlier.",
            "Nice to have you back! Your resume is ready to continue.",
            "Welcome back! Here’s the resume you generated before.",
            "Hello again! Your saved resume is here for you.",
            "Good to see you! Here’s the resume you created earlier.",
            "Welcome back! Ready to pick up where you left off? Here’s your resume."
        )
        val afterGenerationMessages = listOf(
            "Your AI-generated resume is ready!",
            "Here's your resume, crafted by AI.",
            "AI has generated your resume successfully.",
            "Your resume has been generated using AI.",
            "Check out your AI-created resume!"
        )



        val textView = binding.welcomeText
        val welcomeText = welcomeMessages.random()
        val welcomeBackText = welcomeBackMessages.random()
        val afterGenerationText = afterGenerationMessages.random()
        val delayMillis = 10L

        if (!doesFileExists(this)) {
            binding.resumePreviewContainer.visibility = View.GONE
            binding.chatBot.visibility = View.GONE
            binding.welcomeText.visibility = View.VISIBLE
            binding.btnDownload.visibility = View.GONE
            binding.btnShare.visibility = View.GONE
            binding.welcomeText.height = 600
            binding.btnChangeTemplate.visibility = View.GONE
            binding.startTitle.visibility = View.VISIBLE
            Log.d("MainActivity", "Starting typewriter effect")
            startTypewriterEffect(textView, welcomeText, delayMillis)
        } else {
            binding.resumePreviewContainer.visibility = View.VISIBLE
            binding.welcomeText.textSize = 16F
            binding.btnDownload.visibility = View.VISIBLE
            binding.btnShare.visibility = View.VISIBLE
            binding.btnChangeTemplate.visibility = View.VISIBLE
            binding.startTitle.visibility = View.GONE
            if (intent?.getBooleanExtra("fromTemplateActivity", false) == true) {
                startTypewriterEffect(textView, afterGenerationText, delayMillis)
            } else {
                startTypewriterEffect(textView, welcomeBackText, delayMillis)
            }
            loadPdfInPdfViewer()
        }
    }


    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume called")
        loadPdfInPdfViewer()  // Reload PDF when returning to the activity


    }

    private fun startTypewriterEffect(textView: TextView, fullText: String, delayMillis: Long = 50) {
        textView.text = "" // Clear the text initially
        var currentIndex = 0

        binding.lottieAnim.speed = 2F

        lifecycleScope.launch {
            while (currentIndex < fullText.length) {
                val nextStep =
                    if (Random.nextBoolean()) 1 else Random.nextInt(2, 5) // Type letters or words
                val endIndex =
                    (currentIndex + nextStep).coerceAtMost(fullText.length) // Ensure we don’t exceed text length

                textView.text = fullText.substring(0, endIndex)
                currentIndex = endIndex

                delay(delayMillis + Random.nextLong(10, 50)) // Vary typing speed slightly
            }
            binding.lottieAnim.speed = 0.5F
        }

    }

    private fun doesFileExists(context: Context): Boolean {
        val fileName = "resume.pdf"
        val file = File(context.filesDir, fileName) // Internal storage
        return file.exists()
    }


    private var isPdfLoaded = false // Track if the PDF is already loaded

    private fun loadPdfInPdfViewer() {
        val file = File(filesDir, "resume.pdf")

        if (isPdfLoaded) {
            // If PDF is already loaded, don't show animation or reload
            Log.d("MainActivity", "PDF already loaded, skipping animation and load.")
            return
        }

        // *** Check if the file exists BEFORE starting the animation ***
        if (file.exists()) {
            // File exists: Show animation, then load PDF
            Log.d("MainActivity", "PDF file found. Starting loading animation.")

            binding.loadingAnim.apply {
                visibility = View.VISIBLE
                setMinFrame(0)
                setMaxFrame(30)
                speed = 2f

                // Use playAnimation() for Lottie for continuous looping if desired,
                // or manage with ValueAnimator as you did. Sticking to your ValueAnimator approach:
                val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 600 // Duration of one loop cycle
                    interpolator = LinearInterpolator()
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    addUpdateListener { animation ->
                        progress = animation.animatedValue as Float
                    }
                    start()
                }

                // Assign the animator to the view's tag so we can easily cancel it later
                tag = animator // Store animator reference

                // Stop animation after 3 seconds, then load the PDF
                Handler(Looper.getMainLooper()).postDelayed({
                    // Retrieve the animator from the tag and cancel it
                    (tag as? ValueAnimator)?.cancel()
                    visibility = View.GONE // Hide the animation view

                    // Load the PDF (we already confirmed file exists)
                    binding.pdfView.fromFile(file)
                        .enableDoubletap(true)
                        .defaultPage(0)
                        // Optional: Add callbacks for load completion or errors
                        .onLoad { pages ->
                            Log.d("MainActivity", "PDF loaded successfully. Pages: $pages")
                            isPdfLoaded = true // Mark PDF as loaded AFTER successful load
                        }
                        .onError { throwable ->
                            Log.e("MainActivity", "Error loading PDF", throwable)
                            Toast.makeText(this@MainActivity, "Error loading resume.", Toast.LENGTH_SHORT).show()
                            isPdfLoaded = false // Ensure flag is false on error
                        }
                        .load()

                    Log.d("MainActivity", "PDF loading initiated.")

                }, 3000) // Animation runs for 3 seconds before showing the PDF
            }
        } else {
            // File does NOT exist: Show error message, do not start animation
            Log.e("MainActivity", "PDF file not found! Cannot load.")

            // Ensure animation view is hidden
            binding.loadingAnim.visibility = View.GONE
            // Ensure PDF view is also cleared/hidden if necessary
            // binding.pdfView.recycle() // If it holds a previous pdf
            // binding.pdfView.visibility = View.GONE // Or hide it
        }
    }




    private fun loadUserData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val user = database.userProfileDao().getUser()
                if (user == null) {
                    Log.e("MainActivity", "User profile not found!")
                    return@launch
                }

                launch(Dispatchers.Main) {
                    userProfile = user
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading user data", e)
            }
        }
    }


    private fun setupClickListeners() {
        binding.btnChangeTemplate.setOnClickListener{launchTemplateActivity()}
        binding.btnEditData.setOnClickListener { launchFormActivity() }
        binding.btnDownload.setOnClickListener { downloadResumeFile(this) }
        binding.btnShare.setOnClickListener {
            shareResumeFile(this)
            Log.d("MainActivityy", "Share button clicked")
        }
    }

    private fun downloadResumeFile(context: Context) {
        val sourceFile = File(context.filesDir, "resume.pdf")

        if (!sourceFile.exists()) {

            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+ (Scoped Storage)
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "resume.pdf")
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let { outputUri ->
                    resolver.openOutputStream(outputUri)?.use { outputStream ->
                        FileInputStream(sourceFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Toast.makeText(context, "File downloaded successfully!", Toast.LENGTH_SHORT)
                        .show()
                } ?: throw IOException("Failed to create file in Downloads folder.")

            } else {
                // For Android 9 and below
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destinationFile = File(downloadsDir, "resume.pdf")

                FileInputStream(sourceFile).use { inputStream ->
                    FileOutputStream(destinationFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                Toast.makeText(context, "File saved to Downloads folder!", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            Log.e("Download", "Error saving file", e)
            Toast.makeText(context, "Failed to download file!", Toast.LENGTH_SHORT).show()
        }
    }


    private fun shareResumeFile(context: Context) {
        Log.d("MainActivityy", "Share function called")
        val file = File(context.filesDir, "resume.pdf")
        Log.d("MainActivityy", "File path: ${file.absolutePath}")
        if (!file.exists()) {
            return
        }
        Log.d("MainActivityy", "File exists: ${file.exists()}")
        // Get URI using FileProvider
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Make sure to define FileProvider in Manifest
            file
        )
        Log.d("MainActivityy", "File URI: $fileUri")
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Give permission to apps to read this file
        }
        Log.d("MainActivityy", "Share intent created")

        // Open the Android Share Bottom Sheet
        context.startActivity(Intent.createChooser(shareIntent, "Share Resume via"))
        Log.d("MainActivityy", "Share intent started")
    }


    private fun launchFormActivity() {
        val intent = Intent(this, FormActivity::class.java)
        startActivity(intent)
    }

    private fun launchTemplateActivity() {
        val intent = Intent(this,TemplateActivity::class.java)
        startActivity(intent)
    }
}
