# MOSIM 개발 규칙

작업을 시작하기 전에 `docs/requirements.md`와 `docs/architecture.md`를 읽는다.
두 문서에 없는 요구사항은 추측해서 구현하지 않는다. 임시 화면이나 Mock으로 두고 미정이라고 적는다.

## 프로젝트 성격

- 개발 기간 1주일. 빠른 구현과 통합이 우선이다.
- 와이어프레임 미확정. 디자인에 시간을 쓰지 않는다.
- Pleos SDK 미연동. Mock으로 개발한다.

## 하지 말 것

- **Pleos SDK API를 추측해서 구현하지 않는다.** 실제 시그니처를 확인하지 않은 호출을 작성하지 않는다.
  연동 자리는 `platform/pleos/PleosVehicleStatusSource.kt`에 주석으로 남겨 뒀다.
- 멀티모듈로 쪼개지 않는다.
- 라이브러리를 임의로 추가하지 않는다. 필요하면 팀에 먼저 말한다.
- `gradle/libs.versions.toml`의 버전을 혼자 올리지 않는다. 버전 조합 근거는 `README.md`에 있다.
- 화면 코드에서 SDK 타입을 직접 쓰지 않는다. `platform/pleos`의 모델만 쓴다.
- 확정되지 않은 내용을 그럴듯하게 채우지 않는다. 모르면 `[확인 필요]`로 표시한다.

## 디렉터리

```
app/src/main/java/com/example/mosim
├─ MainActivity.kt      앱 진입점
├─ navigation/          경로 상수(MosimRoute), NavHost
├─ ui/                  공통 Compose 요소
│  └─ theme/            색, 테마
├─ feature/<화면>/       화면 하나당 패키지 하나
├─ data/mock/           Mock 구현
└─ platform/pleos/      Pleos SDK 경계
```

화면을 추가할 때는 이 순서로 한다.

1. `navigation/MosimRoute.kt`에 route 상수 추가
2. `feature/<이름>/` 패키지에 Composable 작성
3. `navigation/MosimNavHost.kt`에 `composable(...)` 추가

## 코딩 스타일

- Kotlin, Jetpack Compose, Material3만 쓴다. View/XML 레이아웃은 쓰지 않는다.
- 들여쓰기 4칸, 한 줄 120자. `.editorconfig`가 적용된다.
- Composable 함수는 파스칼케이스, 나머지 함수는 카멜케이스.
- 화면 Composable은 `onBack`, `onNavigate` 같은 콜백을 파라미터로 받는다. NavController를 화면 안으로 넘기지 않는다.
- 화면마다 `@Preview`를 하나 둔다. 외부 데이터가 필요한 화면은 본문을 별도 Composable로 떼서 Preview한다.
- 하드코딩한 임시 데이터에는 Mock이라고 주석을 남긴다.

## 주석

- 왜 그렇게 했는지만 적는다. 코드를 한국어로 다시 읽어주는 주석은 적지 않는다.
- 임시 구현에는 근거와 다음 할 일을 남긴다.
- 확인하지 못한 사실에는 `[확인 필요]`를 붙인다.

## 커밋

형식: `<type>: <무엇을 했는지>`

type은 다음 중 하나다.

- `feat` 기능 추가
- `fix` 버그 수정
- `refactor` 동작 변경 없는 구조 수정
- `chore` 빌드, 설정, 의존성
- `docs` 문서

예시

```
feat: 동승자 선택 화면에 후보 목록 추가
fix: 차량 상태 화면 로딩 중 뒤로가기 크래시 수정
chore: Compose BOM 2025.08.00 고정
```

규칙

- 한 커밋에 한 가지만 담는다.
- `local.properties`, `.idea/`, 서명키, 빌드 산출물은 커밋하지 않는다. `.gitignore`가 막아 준다.
- main에 직접 push하지 않는다. `feature/<이름>` 브랜치에서 작업하고 Pull Request를 올린다.

## 푸시 전 확인

```
./gradlew :app:assembleDebug :app:testDebugUnitTest
```
