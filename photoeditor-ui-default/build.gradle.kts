plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

// Project-level coordinates so the generated POM for this module and any
// module depending on it resolve to the right GAV (see :photoeditor-core).
group = "com.github.Muzamilabdallah"
// Keep in lockstep with :photoeditor-core — the two artifacts are released
// together and ui-default's POM pins core at this exact version.
version = "0.1.1"

android {
    namespace = "com.muzamil.photoeditor.uidefault"
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

// JitPack consumes this via
// `com.github.Muzamilabdallah.photoEditor:photoeditor-ui-default:<tag>`.
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = project.group.toString()
            artifactId = "photoeditor-ui-default"
            version = project.version.toString()
            afterEvaluate {
                from(components["release"])
            }
            pom {
                name = "photoeditor-ui-default"
                description = "Default UI for the photoeditor library: tool panels, " +
                        "bottom nav, and token-based theme over photoeditor-core."
                url = "https://github.com/Muzamilabdallah/photoEditor"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                scm {
                    url = "https://github.com/Muzamilabdallah/photoEditor"
                }
            }
        }
    }
}

dependencies {
    // `api` so Mode B consumers get core's types (PhotoEditorState,
    // EditorShape, ShapeToolState, …) transitively from this one artifact.
    // The generated POM declares photoeditor-core as a compile dependency.
    api(project(":photoeditor-core"))

    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.unit)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
