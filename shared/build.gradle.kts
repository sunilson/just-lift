plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

kotlin {
    // Suppress expect/actual Beta warnings
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
            binaryOption("bundleId", "at.sunilson.justlift.shared")
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)

            // Lifecycle & ViewModel (Compose Multiplatform)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.navigation.compose.multiplatform)

            // Koin
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)

            // Kable BLE (KMP)
            implementation(libs.kable.core)

            // Kotlinx
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            // DataStore (KMP)
            implementation(libs.androidx.datastore.preferences)

            // Room (KMP)
            implementation(libs.room.runtime)
        }

        androidMain.dependencies {
            // Koin Android
            implementation(libs.koin.android)
            implementation(libs.koin.android.compose)
            implementation(libs.koin.android.compose.navigation)
            implementation(project.dependencies.platform(libs.koin.annotations.bom))
            implementation(libs.koin.annotations)

            // Room Android
            implementation(libs.room.ktx)

            // Nordic BLE
            implementation(libs.nordicsemi.ble)

            // Accompanist
            implementation(libs.accompanist.permissions)

            // Timber
            implementation(libs.timber)

            // Paging
            implementation(libs.paging.runtime)
            implementation(libs.paging.compose)
            implementation(libs.room.paging)

            // Android Compose (for tooling previews)
            implementation(libs.androidx.compose.ui.tooling.preview)

            // Android Material3 (for APIs not yet in Compose Multiplatform)
            implementation(libs.androidx.compose.material3)

            // Lifecycle runtime compose (for collectAsStateWithLifecycle)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }

        iosMain.dependencies {
            implementation(libs.sqlite.bundled)
        }
    }
}

android {
    namespace = "at.sunilson.justlift.shared"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 31
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// Room KSP for all targets
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
}
