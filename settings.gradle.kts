pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()

        // 카카오맵 Android SDK v2 (apis.map.kakao.com/android_v2 시작하기 문서)
        maven { url = uri("https://devrepo.kakao.com/nexus/repository/kakaomap-releases/") }

        // SpeechToText v3.3.1 개발자 승인 연동. Vehicle 설정은 변경하지 않는다.
        // STT 3.3.1 공개 아티팩트는 별도 Maven 인증 없이 다운로드를 확인했다.
        maven { url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/") }
    }
}

rootProject.name = "mosim"
include(":app")