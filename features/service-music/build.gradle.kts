import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id(buildPlugins.androidLibrary)
    id(buildPlugins.kotlinAndroid)
    id(buildPlugins.kotlinKapt)
    id(buildPlugins.hilt)
}

apply(from = rootProject.file("buildscripts/configure-android-defaults.gradle"))

android {

    defaultConfig {
        val localProperties = gradleLocalProperties(rootDir)
        buildConfigField("String", "LAST_FM_KEY", localProperties.getProperty("last_fm_key"))
        buildConfigField("String", "LAST_FM_SECRET", localProperties.getProperty("last_fm_secret"))
    }
}

dependencies {
    lintChecks(project(":lint"))

    implementation(project(":core"))

    implementation(project(":features:app-shortcuts"))
    implementation(project(":domain"))
    implementation(project(":libraries:image-loader"))
    implementation(project(":shared-android"))
    implementation(project(":shared"))
    implementation(project(":prefs-keys"))
    implementation(project(":intents"))
    implementation(project(":libraries:equalizer"))

    implementation(libs.kotlin)
    implementation(libs.Coroutines.core)

    implementation(libs.dagger.core)
    kapt(libs.dagger.kapt)
    implementation(libs.dagger.hilt)
    kapt(libs.dagger.hiltKapt)

    implementation(libs.X.media)
    implementation(libs.X.Lifecycle.service)
    implementation(libs.X.Lifecycle.java8)
    implementation(libs.X.Lifecycle.runtime)
    implementation(libs.UX.glide)

    implementation(libs.Utils.lastFmBinding)

    // TODO replace
    implementation("com.google.android.exoplayer:exoplayer-core:2.11.3")
//    implementation project(":exoplayer-library-core")
//    implementation project(":exoplayer-extension-flac")
//    implementation project(":exoplayer-extension-opus")
//    implementation project(":exoplayer-extension-ffmpeg")

    implementation(libs.Debug.timber)

    testImplementation(libs.Test.junit)
    testImplementation(libs.Test.mockito)
    testImplementation(libs.Test.mockitoKotlin)
    testImplementation(libs.Test.android)
    testImplementation(libs.Test.robolectric)
    testImplementation(libs.Coroutines.test)
}