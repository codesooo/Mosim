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

        // Pleos Playground Maven. 인증 없이 받아진다(2026-11-12 확인).
        maven { url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/") }
    }
}

rootProject.name = "mosim"
include(":app")
