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

    // 제출용 release 서명. 비밀값은 Git에 올라가지 않는 keystore.properties에서만 읽는다.
    // 파일이 없으면 signingConfigs를 비워 두고 release는 서명 없이 빌드된다(CI·팀원 로컬에서 빌드가 깨지지 않게).
    val keystoreProperties = Properties().apply {
        val file = rootProject.file("keystore.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }

    signingConfigs {
        if (keystoreProperties.getProperty("storeFile").isNullOrBlank().not()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                // 서명 스킴은 AGP 기본값을 따른다. minSdk 28이라 v1(JAR)·v2 없이 v3만 붙고,
                // Android 9 이상은 v3를 지원하므로 설치에 문제가 없다(apksigner verify 통과).
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
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
