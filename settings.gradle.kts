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

        // Pleos Playground Maven. Vehicle SDK를 실제로 붙일 때 주석을 푼다.
        // 인증이 필요한지는 확인되지 않았다. [확인 필요]
        // maven { url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/") }
    }
}

rootProject.name = "mosim"
include(":app")
