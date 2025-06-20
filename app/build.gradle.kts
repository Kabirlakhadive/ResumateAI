import java.util.Properties
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("com.google.gms.google-services")
}



android {
    namespace = "com.codeNext.resumateAI"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.codeNext.resumateAI"
        minSdk = 24
        targetSdk = 35
        versionCode = 14
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GEMINI_API_KEY", "\"${project.findProperty("GEMINI_API_KEY") ?: ""}\"")
        buildConfigField("String", "GEMINI_MODEL", "\"${project.findProperty("GEMINI_MODEL") ?: ""}\"")

    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation(libs.androidx.media3.common.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.google.code.gson:gson:2.12.1")
    implementation( "com.google.ai.client.generativeai:generativeai:0.9.0" )

    // Firebase BOM (Bill of Materials) - recommended
    implementation(platform("com.google.firebase:firebase-bom:33.0.0")) // Check for latest version

    // Add the dependency for Firebase Firestore KTX
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-installations-ktx")
    // For analytics (optional but good for tracking)
    implementation("com.google.firebase:firebase-analytics-ktx")

    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-database-ktx")


    implementation("com.itextpdf.android:kernel-android:7.2.5")
    implementation("com.itextpdf.android:layout-android:7.2.5")
    implementation ("com.github.barteksc:AndroidPdfViewerV1:1.5.0")

    // Room dependencies
    val room_version = "2.6.1"

    implementation(platform("androidx.compose:compose-bom:2024.02.00")) // BOM for stable Compose versions

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.room:room-runtime:$room_version")

    implementation ("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation ("com.google.android.material:material:1.11.0")


    implementation("androidx.core:core-splashscreen:1.0.0")

    // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
    // See Add the KSP plugin to your project
    ksp("androidx.room:room-compiler:$room_version")

    // If this project only uses Java source, use the Java annotationProcessor
    // No additional plugins are necessary
    annotationProcessor("androidx.room:room-compiler:$room_version")
    implementation("androidx.core:core-splashscreen:1.0.0")


    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$room_version")
    implementation ("com.airbnb.android:lottie:3.7.0")



}