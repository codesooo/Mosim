# 버전 변경 기록

## 변경 전 (Android Studio 캐너리 생성 기본값)

Android Studio가 생성한 초기 상태다. CLI에서 `assembleDebug` 성공을 확인한 조합이다.
문제가 생겨 되돌려야 하면 이 값을 그대로 복원한다.

| 항목 | 값 |
|---|---|
| AGP | 9.5.1 |
| Gradle Wrapper | 9.6.0-bin |
| Gradle 데몬 JDK | 25 (`gradle/gradle-daemon-jvm.properties`, foojay 자동 다운로드) |
| `settings.gradle.kts` 플러그인 | `org.gradle.toolchains.foojay-resolver-convention` 1.0.0 |
| compileSdk | `release(37)` (AGP 9 전용 DSL) |
| targetSdk / minSdk | 37 / 28 |
| sourceCompatibility | Java 11 |
| Kotlin 플러그인 | 없음 |
| Compose | 없음 |
| 의존성 | appcompat 1.6.1, core-ktx 1.10.1, material 1.10.0, junit 4.13.2, espresso 3.5.1, ext-junit 1.1.5 |
| 템플릿 | No Activity (`app/src/main/java/com/example/mosim` 비어 있음) |

변경 전 `app/build.gradle.kts`의 release 블록:

```kotlin
buildTypes {
    release {
        optimization {
            enable = true
            packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
        }
    }
}
```

`optimization { }`은 AGP 9 전용 DSL이다. AGP 8로 내리면서 `isMinifyEnabled` + proguard 파일 방식으로 바꿨다.

## 변경 이유

AGP 9.5.1과 compileSdk 37은 캐너리 조합이다. 안정판 Android Studio는 AGP 9.5.1 프로젝트를 sync하지 않는다.
팀원 중 한 명이라도 안정판을 쓰면 clone 직후 막힌다.

## 변경 후

`README.md`의 "확정 버전" 표를 본다.
