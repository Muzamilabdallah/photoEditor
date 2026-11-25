import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
}

// Project-level coordinates (not just publication-level) so the POM that
// ui-default generates references core with the right group and version.
// Both come from gradle.properties. JitPack builds keep the old
// com.github.Muzamilabdallah group so existing JitPack users can still
// resolve this release; it sets JITPACK=true in its build environment.
group = if (System.getenv("JITPACK") == "true") "com.github.Muzamilabdallah"
        else providers.gradleProperty("GROUP").get()
version = providers.gradleProperty("VERSION_NAME").get()

android {
    // Distinct from `:app` and `:photoeditor-ui-default` namespaces so AGP
    // doesn't merge R classes. Kotlin source still lives under
    // io.github.photoforge.* for a clean public API surface.
    namespace = "io.github.photoforge.core"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

}

// Central artifact: io.github.photoforge:photoforge-core. JitPack keeps the
// pre-rename artifact name so its existing coordinates still resolve.
val artifactId = if (System.getenv("JITPACK") == "true") "photoeditor-core" else "photoforge-core"

mavenPublishing {
    coordinates(project.group.toString(), artifactId, project.version.toString())
    configure(
        AndroidSingleVariantLibrary(
            javadocJar = JavadocJar.Empty(),   // Central requires one; the KDoc ships in sources
            sourcesJar = SourcesJar.Sources(),
            variant = "release",
        )
    )
    publishToMavenCentral()
    // Sign only when a key is configured (the release workflow), so local,
    // CI and JitPack builds can still publishToMavenLocal without one.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    pom {
        name = "PhotoForge Core"
        description = "UI-agnostic photo editor engine for Jetpack Compose: editing canvas, element model, undo/redo, state holder, export."
        inceptionYear = "2026"
        url = "https://github.com/Muzamilabdallah/photoEditor"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "Muzamilabdallah"
                name = "Muzamilabdallah"
                url = "https://github.com/Muzamilabdallah"
            }
        }
        scm {
            url = "https://github.com/Muzamilabdallah/photoEditor"
            connection = "scm:git:https://github.com/Muzamilabdallah/photoEditor.git"
            developerConnection = "scm:git:ssh://git@github.com/Muzamilabdallah/photoEditor.git"
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
    // Exporter pixel tests run renderEdits on the JVM with Robolectric's
    // native graphics (real Skia), so they check actual drawn pixels.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)

    // On-device checks for what Robolectric can't do (hardware bitmaps).
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
