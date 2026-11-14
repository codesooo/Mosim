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

        // SpeechToText v3.3.1 개발자 승인 연동. Vehicle 설정은 변경하지 않는다.
        // STT 3.3.1 공개 아티팩트는 별도 Maven 인증 없이 다운로드를 확인했다.
        maven { url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/") }
    }
}

rootProject.name = "mosim"
include(":app")
