plugins {
    alias(libs.plugins.zdon.android.library)
    alias(libs.plugins.zdon.android.hilt)
}

android {
    namespace = "com.zdon.core.datastore"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
