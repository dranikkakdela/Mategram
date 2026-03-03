plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("dagger.hilt.android.plugin")
    id("com.google.firebase.firebase-perf")
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.xxcactussell.mategram"
        compileSdk = 36
        android.buildFeatures.buildConfig = true

        defaultConfig {
            applicationId = "com.xxcactussell.mategram"
            minSdk = 31
            targetSdk = 36

            val myVersionName = rootProject.extra["versionName"] as String
            val myVersionCode = rootProject.extra["versionCode"] as Int
            versionName = myVersionName
            versionCode = myVersionCode

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

            buildConfigField("String", "APP_VERSION_STRING", "\"$myVersionName\"")
        }

        buildTypes {
            getByName("release") {
                isMinifyEnabled = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }

        buildFeatures {
            compose = true
            buildConfig = true
        }

        splits {
            abi {
                isEnable = true
                reset()
                include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                isUniversalApk = true
            }
        }
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.perf)
    implementation(project(":mategram:presentation"))
    implementation(project(":mategram:jni"))
    implementation(project(":mategram:data"))
    implementation(project(":mategram:domain"))
    implementation(project(":mategram:player"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.common)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.compose.foundation.layout)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.android.compiler)
    ksp(libs.androidx.hilt.compiler)
}