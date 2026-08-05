plugins {
    alias(libs.plugins.zdon.android.feature)
}

android {
    namespace = "com.zdon.feature.home"
}

dependencies {
    implementation(libs.coil.compose)
}
