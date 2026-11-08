# MOSIM 개발 규칙

모든 AI 코딩 도구가 공통으로 읽는 규칙이다. `CLAUDE.md`는 이 파일을 불러오기만 한다. 규칙은 여기에만 쓴다.

## 작업 전에 읽을 문서

항상 읽는다.

- `docs/context/project-status.md` — 지금 무엇이 있고, 무엇이 정해졌고, 무엇이 미정인지
- `docs/requirements.md`, `docs/architecture.md`

작업에 따라 추가로 읽는다.

| 이런 작업이면 | 먼저 읽는다 |
|---|---|
| SDK, 차량 데이터, 위치, 음성, 외부 API를 다룬다 | `docs/context/sdk-status.md` |
| README, APK 빌드, 제출물, 데모 시나리오, 화면 문구를 다룬다 | `docs/context/contest.md` |
| SDK 메서드 이름이나 시그니처가 필요하다 | `docs/reference/pleos/` (Pleos 문서 정리본). 없으면 쓰지 말고 `[확인 필요]` |

문서끼리 내용이 다르면 `docs/requirements.md` > `docs/context/project-status.md` > 나머지 순으로 따른다.
충돌을 발견하면 어느 쪽으로든 구현하지 말고 사람에게 알린다.

`docs/context/` 문서는 팀 합의로만 고친다. 내용이 틀렸다고 판단되면 고치지 말고 PR 설명이나 답변에 적는다.

요구사항 문서에 없는 기능은 추측해서 구현하지 않는다. 임시 화면이나 Mock으로 두고 미정이라고 적는다.

## 프로젝트 성격

- 1차 접수 마감은 2026-11-15 23:59. 개발 기간은 약 1주일이다. 빠른 구현과 통합이 우선이다.
- 와이어프레임 미확정. 디자인에 시간을 쓰지 않는다.
- Pleos SDK 미연동. Mock으로 개발한다.
- 팀원 전원이 최종 PT 기술 질의응답에서 구조와 로직을 직접 설명해야 한다(공모전 규칙).
  설명하기 어려운 과한 추상화, 마법 같은 코드, 쓰지 않는 확장 지점을 만들지 않는다.

## 하지 말 것

- **Pleos SDK API를 추측해서 구현하지 않는다.** 실제 시그니처를 확인하지 않은 호출을 작성하지 않는다.
  연동 자리는 `platform/pleos/PleosVehicleStatusSource.kt`에 주석으로 남겨 뒀다.
- `sdk-status.md`에서 상태가 ✅(사용 가능)가 아니고 "에뮬레이터 검증"도 안 된 SDK는 실제 호출 코드를 쓰지 않는다.
  인터페이스와 Mock까지만 만든다.
- 종료된 **LLM SDK**는 쓰지 않는다.
- 멀티모듈로 쪼개지 않는다.
- 라이브러리를 임의로 추가하지 않는다. 필요하면 팀에 먼저 말한다.
- `gradle/libs.versions.toml`의 버전을 혼자 올리지 않는다. 버전 조합 근거는 `README.md`에 있다.
- 화면 코드에서 SDK 타입을 직접 쓰지 않는다. `platform/pleos`의 모델만 쓴다.
- `VehicleStatusSourceProvider`의 `USE_MOCK`를 혼자 바꾸지 않는다.
- 확정되지 않은 내용을 그럴듯하게 채우지 않는다. 모르면 `[확인 필요]`로 표시한다.
- 코드, 커밋, README, 스크린샷, 로그에 개발자 실명, 연락처, 계정 정보, Client ID/Secret, API 키를 넣지 않는다.
  키는 `local.properties`에서 읽는다(Git에 올라가지 않는다).

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
docs/
├─ requirements.md      요구사항
├─ architecture.md      구조와 미결정 항목
├─ context/             AI가 참조하는 프로젝트 컨텍스트 (팀 합의로만 수정)
└─ reference/pleos/     Pleos 개발자 문서 정리본 (시그니처 확인용)
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
- Mock은 실제 SDK와 같은 성질을 흉내 낸다: 비동기 결과, `null` 가능, 실패 경우, Virtual Console 값 범위.
  기준은 `sdk-status.md`의 "Mock 작성 기준"을 따른다.

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

## 작업을 마칠 때

- 바꾼 파일 목록과 이유를 요약한다.
- 새로 생긴 `[확인 필요]` 항목이 있으면 따로 모아 알린다.
