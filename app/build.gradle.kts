import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.mosim"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mosim"
        // Pleos Playground 배포 요건이 API 28 이상이다.
        minSdk = 28
        // Pleos Vehicle SDK 공식 샘플이 쓰는 값에 맞췄다.
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 키는 Git에 올라가지 않는 local.properties에서만 읽는다. 없으면 빈 문자열이고, 앱은 Mock 장소 검색을 쓴다.
        buildConfigField("String", "KAKAO_REST_API_KEY", "\"${localProperty("KAKAO_REST_API_KEY")}\"")
        // 지도 SDK용 네이티브 앱 키. 없으면 지도 자리에 MapPlaceholder를 그린다.
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"${localProperty("KAKAO_NATIVE_APP_KEY")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.pleos.speech.to.text)
    // 2026-11-13: 이번 F-1 TTS 작업에 한해 개발자가 사전 검증 제한 예외를 승인했다.
    implementation(libs.pleos.text.to.speech)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kakao.map)
    implementation(libs.pleos.vehicle)
    implementation(libs.pleos.navi.helper)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
}

fun localProperty(name: String): String {
    val file = rootProject.file("local.properties")
    if (!file.exists()) return ""
    val properties = Properties()
    file.inputStream().use { properties.load(it) }
    return properties.getProperty(name, "").trim()
}
