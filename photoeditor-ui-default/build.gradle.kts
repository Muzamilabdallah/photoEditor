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
    namespace = "io.github.photoforge.uidefault"
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

}

// Central artifact: io.github.photoforge:photoforge-ui. JitPack keeps the
// pre-rename artifact name so its existing coordinates still resolve.
val artifactId = if (System.getenv("JITPACK") == "true") "photoeditor-ui-default" else "photoforge-ui"

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
        name = "PhotoForge UI"
        description = "Default UI for PhotoForge: tool panels, bottom nav, and token-based theme over photoforge-core."
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
