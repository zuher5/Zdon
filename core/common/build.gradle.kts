plugins {
    alias(libs.plugins.zdon.android.library)
    alias(libs.plugins.zdon.android.hilt)
}

android {
    namespace = "com.zdon.core.common"
}

dependencies {
    api(projects.core.model)

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
