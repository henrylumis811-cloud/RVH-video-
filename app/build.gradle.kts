plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

ksp {
    arg("room.schemaLocation", "${projectDir}/schemas")
}

android {
    namespace = "com.rvh.video"
    // compileSdk 37 = Android 17 / API 37, required by the current AndroidX stack.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.rvh.video"
        // minSdk 30 = Android 11 — the floor of our support range.
        minSdk = 30
        // Keep target aligned with the project's API 37 configuration.
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.navigation:navigation-compose:2.10.2")

    // Icons: MovieFilter, MusicNote, PictureInPicture, SkipNext/Previous,
    // MailOutline, and the AutoMirrored Comment icon used across the three
    // sections all live in the extended pack, not the small curated set
    // that ships transitively with material3 — without this, those icon
    // references won't resolve.
    implementation("androidx.compose.material:material-icons-extended")

    // Media3 / ExoPlayer — shared across Shorts, Movies, Music sections
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")

    // Room — resume-state, classification cache, overrides
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    // Paging for large local video libraries (Movies grid)
    implementation("androidx.paging:paging-compose:3.5.1")

    // Accompanist permissions — simplifies the SDK-version-branched
    // permission flow (READ_EXTERNAL_STORAGE vs READ_MEDIA_VIDEO)
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    // Coil, for decoding video-frame thumbnails straight from MediaStore URIs
    // (grid/list thumbnails) without hand-rolling a frame-extraction cache.
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")

    // Classic Material Components XML library — needed because themes.xml's
    // parent theme (Theme.Material3.DayNight.NoActionBar) is defined here,
    // not in the Compose material3 artifact (which ships Kotlin/Compose
    // code only, no XML theme resources). Without this, AAPT fails to
    // resolve that theme during resource linking.
    implementation("com.google.android.material:material:1.13.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
