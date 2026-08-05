plugins {
    alias(libs.plugins.zdon.android.library)
    alias(libs.plugins.zdon.android.library.compose)
}

android {
    namespace = "com.zdon.core.designsystem"
}

dependencies {
    api(projects.core.model)

    implementation(libs.androidx.core.ktx)
    implementation(libs.coil.compose)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
