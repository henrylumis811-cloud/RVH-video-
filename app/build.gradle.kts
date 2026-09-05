plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.rvh.video"
    // compileSdk 36 = Android 16, so we can use the latest APIs where available
    // and gate them behind SDK_INT checks for older OS versions.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rvh.video"
        // minSdk 30 = Android 11 — the floor of our support range.
        minSdk = 30
        // targetSdk 36 = Android 16, matches compileSdk so we opt into
        // the latest behavior changes rather than running in compat shims.
        targetSdk = 36
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
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Icons: MovieFilter, MusicNote, PictureInPicture, SkipNext/Previous,
    // MailOutline, and the AutoMirrored Comment icon used across the three
    // sections all live in the extended pack, not the small curated set
    // that ships transitively with material3 — without this, those icon
    // references won't resolve.
    implementation("androidx.compose.material:material-icons-extended")

    // Media3 / ExoPlayer — shared across Shorts, Movies, Music sections
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")

    // Room — resume-state, classification cache, overrides
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Paging for large local video libraries (Movies grid)
    implementation("androidx.paging:paging-compose:3.3.2")

    // Accompanist permissions — simplifies the SDK-version-branched
    // permission flow (READ_EXTERNAL_STORAGE vs READ_MEDIA_VIDEO)
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Coil, for decoding video-frame thumbnails straight from MediaStore URIs
    // (grid/list thumbnails) without hand-rolling a frame-extraction cache.
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")

    // Classic Material Components XML library — needed because themes.xml's
    // parent theme (Theme.Material3.DayNight.NoActionBar) is defined here,
    // not in the Compose material3 artifact (which ships Kotlin/Compose
    // code only, no XML theme resources). Without this, AAPT fails to
    // resolve that theme during resource linking.
    implementation("com.google.android.material:material:1.11.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
