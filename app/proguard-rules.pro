# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Remove all logging calls in release builds
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

#===============================================================================
# iText 7 Core Rules (Essential for PDF Generation)
#===============================================================================
# Keep the main iText classes, interfaces, and their members.
# This is often necessary because iText might use reflection or expect specific names.
-keep class com.itextpdf.** { *; }
-keep interface com.itextpdf.** { *; }

# Keep names of specific classes often involved in core operations or licensing (if applicable)
-keepnames class com.itextpdf.kernel.**
-keepnames class com.itextpdf.layout.**
-keepnames class com.itextpdf.io.**

# Prevent warnings about iText internal classes or potential dynamic loading it handles itself.
# Use -dontwarn cautiously, but it's often needed for large libraries like iText.
-dontwarn com.itextpdf.**

#-------------------------------------------------------------------------------
# Rules for iText Dependencies (If Used - Add as needed)
#-------------------------------------------------------------------------------
# iText often uses BouncyCastle for encryption/signing. If you use those features:
# -keep class org.bouncycastle.** { *; }
# -keep interface org.bouncycastle.** { *; }
# -dontwarn org.bouncycastle.**

# iText might use a logging framework (like slf4j). If you see related errors:
# -keep class org.slf4j.** { *; }
# -keep interface org.slf4j.** { *; }
# -dontwarn org.slf4j.**

#===============================================================================
# Your Application's Data Models (Used in PDF Generation)
#===============================================================================
# Keep your data models (UserProfile, Project, etc.) and their fields/methods.
# This is crucial if iText or your template logic were to use reflection on them,
# and it's generally safe to keep models unobfuscated.
# Keep constructors <init> and all public members (fields/methods).
-keep class com.codeNext.resumateAI.models.** {
    <init>(...);
    public *;
}
# Keep interfaces in the models package too, if any
-keep interface com.codeNext.resumateAI.models.** { *; }

#===============================================================================
# Room Database (Usually Handled by Annotation Processing, but keep models)
#===============================================================================
# Room generates code, which R8 usually handles.
# Keeping your @Entity and @Dao classes *might* be needed only if you access them
# reflectively elsewhere, which is uncommon. Keeping the models (above) is usually sufficient.
# -keep class androidx.room.** { *; } # Generally not needed
# -keep class com.codeNext.resumateAI.UserData { *; } # Likely not needed
# -keep interface com.codeNext.resumateAI.dao.** { *; } # Likely not needed

#===============================================================================
# Kotlin Coroutines (Recommended General Rule)
#===============================================================================
# Keep certain internal classes used by coroutines to prevent potential runtime issues.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.** { *; }
-keepnames class kotlinx.coroutines.test.** { *; }

#===============================================================================
# JSON Parsing (org.json - Usually Not Needed)
#===============================================================================
# Android's built-in org.json library is typically known by R8.
# Rules are usually only needed for third-party libraries like Gson or Moshi,
# especially if they reflect on obfuscated model classes.
# -keep class org.json.** { *; } # Likely not needed

#===============================================================================
# Android SDK Classes (Usually Not Needed)
#===============================================================================
# R8 knows about standard Android classes (Context, Log, File, View, etc.).
# You don't need to keep them.

#===============================================================================
# Font Loading & Reflection (Potential Issues)
#===============================================================================
# Keep PdfFontFactory methods, especially if using non-standard fonts or encodings.
# The main iText rule above likely covers this, but being explicit can sometimes help.
-keep class com.itextpdf.kernel.font.PdfFontFactory { *; }

# Keep standard Java I/O classes used for font file handling (usually safe, R8 knows them)
# -keep class java.io.File { *; }
# -keep class java.io.InputStream { *; }
# -keep class java.io.FileOutputStream { *; }

# Keep SimpleDateFormat if reflection were involved (unlikely here)
# -keep class java.text.SimpleDateFormat { *; }

#===============================================================================
# General Android Rules (Good Practice)
#===============================================================================
# Keep custom Views, Activities, Services, etc., and their constructors if
# they are referenced from the AndroidManifest.xml or layouts. R8 often handles
# this, but explicit rules can prevent issues.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Keep callback methods referenced from XML (e.g., android:onClick)
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# Keep native methods and the classes containing them
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom Parcelable implementations
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
  public static final ** CREATOR; # Handle obfuscated name too
}

# Keep R class members (needed for reflection on resources, uncommon)
# -keep class **.R$* { *; }

# Keep enum members (needed if accessed by name via valueOf)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

#===============================================================================
# Suppress Warnings (Use Cautiously - See Notes)
#===============================================================================
# Already added -dontwarn for iText above.
# Add other -dontwarn rules ONLY if you understand the warning and have confirmed
# the app works correctly despite it (like the XML service ones previously discussed).
# Example:
# -dontwarn org.w3c.dom.**
# -dontwarn org.xml.sax.**