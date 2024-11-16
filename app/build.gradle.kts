plugins {
//    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
//    alias(libs.plugins.google.gms.google.services)
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.sushii.djsync_user"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sushii.djsync_user"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders.putAll(
            mapOf(
                "redirectSchemeName" to "djsynctwo-user",
                "redirectHostName" to "callback"
            )
        )

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        tasks.register("testClasses")
    }
 
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth.ktx)
    implementation(files("../spotify-app-remote-release-0.8.0.aar"))
//    implementation(files("../shazamkit-android-release.aar"))
    implementation(libs.androidthings)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:20.0.0")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.google.code.gson:gson:2.6.1")
    implementation ("com.spotify.android:auth:2.1.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.1")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
//    implementation("com.github.kittinunf.fuel:fuel:3.0.0-alpha03")
    implementation ("com.github.kittinunf.fuel:fuel-android:2.3.1")
    implementation ("com.github.kittinunf.fuel:fuel-gson:2.3.1")
//    implementation("com.github.kittinunf.result:result-jvm:3.0.0-alpha03")
    implementation ("com.arthenica:ffmpeg-kit-full:6.0-2")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    implementation ("com.google.android.material:material:1.3.0-alpha03")

}