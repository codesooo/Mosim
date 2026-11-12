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

        // Pleos Playground Maven. Vehicle SDK를 실제로 붙일 때 주석을 푼다.
        // 인증이 필요한지는 확인되지 않았다. [확인 필요]
        // maven { url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/") }
    }
}

rootProject.name = "mosim"
include(":app")
