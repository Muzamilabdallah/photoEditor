plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    // Distinct from `:app` namespace so AGP doesn't merge R classes.
    // Kotlin source still lives under com.muzamil.photoeditor.* for a clean
    // public API surface.
    namespace = "com.muzamil.photoeditor.lib"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

// JitPack consumes this via `com.github.Muzamilabdallah.photoEditor:photoeditor:<tag>`.
// JitPack overrides group/version from the git tag, but they're set here so
// `publishToMavenLocal` produces sensible coordinates for local testing too.
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.Muzamilabdallah"
            artifactId = "photoeditor"
            version = "1.0.0"
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

dependencies {
    // Re-exported so consumers don't have to add Compose dependencies themselves.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.material3)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.unit)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
