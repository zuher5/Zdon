plugins {
    alias(libs.plugins.zdon.android.library)
    alias(libs.plugins.zdon.android.hilt)
}

android {
    namespace = "com.zdon.core.downloader"
}

dependencies {
    api(projects.core.model)
    api(projects.core.engine)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.datastore)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)

    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
