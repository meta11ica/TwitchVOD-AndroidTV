import java.util.Calendar
import java.io.File


plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
val majorVersion = 1
val minorVersion = 0
val customVersionCode = calculateVersionCode()
android {
    namespace = "meta11ica.tn.twitchvod"
    compileSdk = 34
    useLibrary("org.apache.http.legacy")
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    defaultConfig {
        // Set the version code using a dynamic value
        applicationId = namespace
        minSdk = 21
        targetSdk = 34
        versionCode = customVersionCode
        resValue("integer", "app_version_code", versionCode.toString())

        versionName = "$majorVersion.$minorVersion.$versionCode"
        println("Version Code:"+versionCode.toString())
    }

    buildTypes {

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    // MY POSTBUILD TASK

        gradle.buildFinished {
            val versionJson = """
            {
              "latestVersion": "$majorVersion.$minorVersion.$customVersionCode",
              "version": $customVersionCode,
              "filename": "https://github.com/meta11ica/TwitchVOD-AndroidTV/releases",
              "releaseNotes": [
                "- Bug fixes"
              ]
            }
        """.trimIndent()
            val outputJsonFile = File(buildDir, "version.json")
            outputJsonFile.writeText(versionJson)
        }
    }

}



dependencies {
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.google.android.material:material:1.11.0")

    val leanback_version = "1.2.0-alpha04"
    implementation("io.karn:khttp-android:-SNAPSHOT")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.leanback:leanback:$leanback_version")
    implementation("com.github.bumptech.glide:glide:4.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC2")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.1.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")


}



// Function to calculate the version code
fun calculateVersionCode(): Int {
    // Get the current date in milliseconds
    val currentDateMillis = System.currentTimeMillis()
    // Subtract the base date (e.g., January 1, 2020) in milliseconds
    val baseDateMillis = getDateInMillis(2020, 0, 1)
    // Calculate the number of days since the base date
    val hoursSinceBaseDate = ((currentDateMillis - baseDateMillis) / (1000 * 60 * 60)).toInt()

    // Return the version code based on the number of days
    return hoursSinceBaseDate
}

// Function to get the date in milliseconds
fun getDateInMillis(year: Int, month: Int, day: Int): Long {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, day, 0, 0, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
