plugins {
    alias(libs.plugins.zdon.android.library)
    alias(libs.plugins.zdon.android.hilt)
}

android {
    namespace = "com.zdon.core.data"
}

dependencies {
    api(projects.core.model)
    api(projects.core.downloader)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.engine)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
