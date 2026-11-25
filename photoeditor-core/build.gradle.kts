plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

// Project-level coordinates (not just publication-level) so that sibling
// modules' generated POMs reference this artifact with the right GAV —
// maven-publish derives POM dependency entries from project coordinates.
group = "com.github.Muzamilabdallah"
// 0.x: the public API is still moving — breaking changes land in minor bumps
// until it settles enough to promise stability with 1.0.0.
version = "0.2.0"

android {
    // Distinct from `:app` and `:photoeditor-ui-default` namespaces so AGP
    // doesn't merge R classes. Kotlin source still lives under
    // com.muzamil.photoeditor.* for a clean public API surface.
    namespace = "com.muzamil.photoeditor.core"
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

// JitPack consumes this via `com.github.Muzamilabdallah.photoEditor:photoeditor-core:<tag>`.
// JitPack overrides group/version from the git tag, but they're set here so
// `publishToMavenLocal` produces sensible coordinates for local testing too.
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = project.group.toString()
            artifactId = "photoeditor-core"
            version = project.version.toString()
            afterEvaluate {
                from(components["release"])
            }
            pom {
                name = "photoeditor-core"
                description = "UI-agnostic photo editor engine for Jetpack Compose: " +
                        "editing canvas, element model, undo/redo, state holder, export."
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
    // Re-exported so consumers don't have to add Compose dependencies
    // themselves. Deliberately NO material3 here: core is UI-toolkit-agnostic
    // (canvas + engine + state only) — panels and theming live in
    // :photoeditor-ui-default.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.foundation)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.ui.unit)

    testImplementation(libs.junit)
}
