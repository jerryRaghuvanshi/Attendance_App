plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}


android {
    namespace = "com.example.attendanceapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.attendanceapp"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }



    lint {
        warningsAsErrors = true
        abortOnError = false
        enable += listOf("Interoperability", "Deprecation")
    }
}

dependencies {
    implementation(libs.firebase.functions)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.auth)
    implementation(platform(libs.firebase.bom)) // Use only one BOM
    implementation(libs.firebase.analytics)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.mpandroidchart)

    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.material3)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
    implementation(libs.core.ktx)

    // Firebase
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.material.v160)
    implementation(libs.swiperefreshlayout)
}