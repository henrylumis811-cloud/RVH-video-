plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

ksp {
    arg("room.schemaLocation", "${projectDir}/schemas")
}

// Release variants are signed only with the dedicated RVH Video identity.
// Credentials are supplied by the build environment and are never committed.
val uploadStoreFile = providers.environmentVariable("RVH_UPLOAD_STORE_FILE").orNull
val uploadStorePassword = providers.environmentVariable("RVH_UPLOAD_STORE_PASSWORD").orNull
val uploadKeyAlias = providers.environmentVariable("RVH_UPLOAD_KEY_ALIAS").orNull
val uploadKeyPassword = providers.environmentVariable("RVH_UPLOAD_KEY_PASSWORD").orNull

val hasReleaseSigning = listOf(
    uploadStoreFile,
    uploadStorePassword,
    uploadKeyAlias,
    uploadKeyPassword
).all { !it.isNullOrBlank() }

val releaseKeystore = uploadStoreFile?.let { file(it) }
if (hasReleaseSigning && releaseKeystore?.isFile != true) {
    throw GradleException("RVH release keystore does not exist: ${releaseKeystore?.absolutePath}")
}

android {
    namespace = "com.rvh.video"
    // Android 17 / API 37 is required by the resolved AndroidX dependencies.
    // compileSdk is independent of minSdk and targetSdk.
    // minSdk remains 26, preserving Android 8.0 compatibility.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.rvh.video"
        // Android 8.0 (API 26) is the oldest supported release.
        minSdk = 26
        // Target the stable Android 16 / API 36 release.
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("rvhRelease") {
                storeFile = releaseKeystore
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // Missing credentials leave the variant unsigned, never debug-signed.
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("rvhRelease") else null
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

    // Classic Material Components XML library — needed because themes.xml's
    // parent theme (Theme.Material3.DayNight.NoActionBar) is defined here,
    // not in the Compose material3 artifact (which ships Kotlin/Compose
    // code only, no XML theme resources). Without this, AAPT fails to
    // resolve that theme during resource linking.
    implementation("com.google.android.material:material:1.13.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}


// Guard release packaging explicitly. Debug builds and unit tests remain usable
// without private signing material, but no release APK/AAB can be produced by
// accidentally falling back to the Android debug certificate.
val releasePackagingTasks = setOf(
    "assembleRelease",
    "bundleRelease",
    "packageRelease",
    "packageReleaseBundle",
    "validateSigningRelease",
    "signReleaseBundle",
    "signReleaseApk"
)
tasks.configureEach {
    if (name in releasePackagingTasks) {
        doFirst {
            if (!hasReleaseSigning) {
                throw GradleException(
                    "Refusing to package RVH Video release: configure RVH_UPLOAD_STORE_FILE, " +
                        "RVH_UPLOAD_STORE_PASSWORD, RVH_UPLOAD_KEY_ALIAS and RVH_UPLOAD_KEY_PASSWORD. " +
                        "Debug signing is never used for release builds."
                )
            }
        }
    }
}
