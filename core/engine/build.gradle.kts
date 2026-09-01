plugins {
    alias(libs.plugins.zdon.android.library)
    alias(libs.plugins.zdon.android.hilt)
}

android {
    namespace = "com.zdon.core.engine"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.documentfile)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)

    implementation(libs.youtubedl.android.library)
    implementation(libs.youtubedl.android.ffmpeg)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)

    // org.json ships as a stub inside android.jar; unit tests need the real
    // implementation to parse MediaInfoParser output on the JVM.
    testImplementation("org.json:json:20260814")
}
