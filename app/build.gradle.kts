plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    kotlin("plugin.serialization") version "2.2.0"
}

fun Project.secret(name: String): String? =
    providers.environmentVariable(name)
        .orElse(providers.gradleProperty(name))
        .orNull

android {
    namespace = "at.sunilson.justlift"
    compileSdk {
        version = release(36)
    }

    signingConfigs {
        create("release") {
            val ksPass = project.secret("JUST_LIFT_KEYSTORE_PASSWORD")
            val alias = project.secret("JUST_LIFT_KEY_ALIAS")
            val keyPass = project.secret("JUST_LIFT_KEY_PASSWORD")
            if (ksPass != null && alias != null && keyPass != null) {
                storeFile = file("$rootDir/justlift.jks")
                storePassword = ksPass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    defaultConfig {
        applicationId = "at.sunilson.justlift"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
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
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui.navigation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.nordicsemi.ble)
    implementation(libs.kable.core)
    implementation(libs.kable.default.permissions)
    implementation(libs.accompanist.permissions)
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.android.compose)
    implementation(libs.koin.android.compose.navigation)
    implementation(platform(libs.koin.annotations.bom))
    implementation(libs.koin.annotations)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.timber)
    ksp(libs.koin.ksp.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
