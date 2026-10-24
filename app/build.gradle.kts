import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secretOrProperty(name: String): String? =
    System.getenv(name) ?: keystoreProperties.getProperty(name)

android {
    namespace = "ru.konspekt.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.konspekt.app"
        minSdk = 26
        targetSdk = 35
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 22
        versionName = (project.findProperty("versionName") as String?) ?: "2.4.0"

        vectorDrawables.useSupportLibrary = true
        ndk {
            // Vosk ships large native libraries for every ABI. ARM covers
            // essentially all real Android phones and keeps the APK small.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        create("release") {
            val storePath = secretOrProperty("KEYSTORE_FILE")
            if (!storePath.isNullOrBlank()) {
                val resolved = rootProject.file(storePath)
                if (resolved.exists()) {
                    storeFile = resolved
                    storeType = secretOrProperty("KEYSTORE_TYPE") ?: "PKCS12"
                    storePassword = secretOrProperty("KEYSTORE_PASSWORD")
                    keyAlias = secretOrProperty("KEY_ALIAS")
                    keyPassword = secretOrProperty("KEY_PASSWORD")
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig = if (releaseSigning.storeFile != null) {
                releaseSigning
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.03.01"))
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.navigation:navigation-compose:2.9.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("io.github.huarangmeng:latex-renderer:1.6.1")
    implementation("io.github.huarangmeng:latex-fonts-katex:1.6.1")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.bihe0832.android:lib-sherpa-onnx:6.25.21")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
