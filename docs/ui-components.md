# 모심 UI 컴포넌트 사용법

> 기준 디자인: 피그마 `Pleos_Connect_design_assets_215` › **모심 화면 v2** (A~G, 17개 프레임)
> 작성: 2026-11-08 · 코드 위치: `app/src/main/java/com/example/mosim/ui/`
> 화면을 만들 때는 이 문서의 1장(공통)을 먼저 보고, 2장에서 맡은 프레임을 찾는다.

---

## 0. 먼저 알아둘 것

- **피그마 px = dp.** 기본 디스플레이(Connect-S-17)가 160 DPI라서 피그마 숫자를 dp/sp로 그대로 쓴다. 앱 영역(landscape)은 1700×1184.
- **색·글자·간격은 직접 쓰지 않는다.** `Color(0xFF…)`, `16.sp`, `40.dp`를 화면 코드에 쓰지 말고 토큰(1-1)을 쓴다. 토큰에 없는 값이 필요하면 `ui/theme/`에 추가하고 피그마 근거를 주석으로 남긴다.
- **라이트 테마만** 만든다(팀 결정).
- 각 컴포넌트 파일 아래쪽에 `@Preview`가 있다. Android Studio에서 열면 피그마 예시 값으로 그려진다. 쓰는 법이 헷갈리면 Preview 코드를 먼저 본다.
- 표시가 `[확인 필요]`인 값은 피그마에서 숫자를 직접 확인하지 못한 값이다(4장).

```
ui/
├─ theme/                Color · Type · Shape · Dimens · Theme
├─ components/
│  ├─ pleos/             Pleos 공식 컴포넌트 (버튼, 세그먼트, 스위치)
│  └─ *.kt               모심 전용 컴포넌트
res/drawable/ic_*.xml    아이콘 (Lucide)
res/font/asta_sans_*.ttf 글꼴
```

---

## 1. 공통

### 1-1. 테마 토큰 (`ui/theme/`)

모든 화면은 `MosimTheme { … }` 안에서 그린다(`MainActivity`에서 이미 감싸 있다. Preview에서는 직접 감싼다).

**색**

| 쓰는 곳 | 토큰 |
|---|---|
| 앱 배경 | `PleosColor.BackgroundPrimary` (#F7F8FA) |
| 카드 배경 | `PleosColor.SurfaceBasic` (#FFFFFF) |
| 회색 판 (지도 자리 등) | `PleosColor.SurfaceHigh` (#EDEEF2) |
| 기본 글자 | `PleosColor.TextDarkPrimary` (#131417) |
| 보조 글자 (라벨, 부제) | `PleosColor.TextDarkTertiary` (64%) |
| 흐린 글자 (placeholder) | `PleosColor.TextDarkQuaternary` (32%) |
| 진한 배경 위 글자 | `PleosColor.TextLightPrimary` (흰색) |
| 구분선 | `PleosColor.DividerSecondary` (10%), 카드 안 행 사이는 `MosimColor.RowDivider` (8%) |
| 상태색 (정상/주의/경고) | `MosimStatus.Normal / Caution / Warning` 의 `.accent`, `.container`, `.content` |

**글자** — 기본 굵기는 Normal(400). 굵게는 `.strong()`(700), `.extraStrong()`(800)을 붙인다.

| 피그마 크기 | 토큰 | 주로 쓰는 곳 |
|---|---|---|
| 40 Bold | `PleosTypography.TitleLarge.strong()` | 화면 제목 |
| 36 Bold | `PleosTypography.TitleMedium.strong()` | 카드 안 값 "12%", "8.2km" |
| 32 Bold | `PleosTypography.TitleSmall.strong()` | 버튼 글자, 목록 제목, 프로필 이름 |
| 30 Bold | `PleosTypography.BodyLarge.strong()` | 카드 제목 "차량 상태" |
| 30 Regular | `PleosTypography.BodyLarge` | 입력 글자 |
| 28 Bold | `PleosTypography.BodyMedium.strong()` | 상태 알약 글자 |
| 26 Regular | `PleosTypography.BodySmall` | 부제, 라벨 |
| 24 Regular | `PleosTypography.LabelMedium` | 목록 설명 "0.4km · 경유 +3분" |
| 22 | `MosimTypography.Caption` / `CaptionStrong` | 작은 칩, 입력 칸 라벨 |
| 44 Bold | `MosimTypography.BannerTitle` | 판단 배너 제목, 큰 질문 문구 |
| 72 Bold | `MosimTypography.HeroName` | 에스코트 동승자 이름 |
| 140 Bold | `MosimTypography.HeroEta` | 예상 도착 시각 |
| 30 Medium | `MosimTypography.HeroSub` | 에스코트 보조 문구 |

**모양·간격**

| 쓰는 곳 | 토큰 |
|---|---|
| 카드·배너 라운드 32 | `MosimShape.Card` |
| 입력 칸·카드 안 회색 박스 24 | `MosimShape.Field` |
| 칩 12 | `MosimShape.Chip` |
| 원형 | `MosimShape.Pill` |
| 앱 영역 여백 48 / 섹션 간격 32 | `MosimDimens.AppPadding` / `SectionGap` |
| 카드 안쪽 여백 40 / 카드 사이 24 | `MosimDimens.CardPadding` / `CardGap` |

### 1-2. 화면 틀: `MosimAppContainer`

**모든 화면은 이것으로 시작한다.** 제목·부제 → 본문(남은 높이를 채움) → 오른쪽 아래 버튼 순서로 배치한다.

```kotlin
MosimAppContainer(
    title = "출발 전 점검",
    subtitle = "엄마 · 강남역 2번 출구 08:30 → 서울대병원",
    onBack = onBack,                 // 최상단 화면이면 넘기지 않는다(null)
    headerTrailing = { … },          // 제목 오른쪽 요소(E-2 토스트). 없으면 생략
    bottomActions = {                // 버튼이 없는 화면은 생략
        PleosButton("다시 점검", onClick = {}, modifier = Modifier.width(280.dp))
        PleosButton("에스코트 계획 보기", onClick = {}, type = PleosButtonType.Filled, modifier = Modifier.width(400.dp))
    },
) {
    // 본문: ColumnScope. 2열 배치는 1-6 참고
}
```

**CCP Back 규칙 (심사 필수, 따로 처리할 필요 없음)**
- `onBack = null` → 최상단 화면으로 본다. Back을 누르면 앱을 끝내지 않고 숨긴다(`moveTaskToBack(true)`).
- `onBack != null` → 하위 화면이다. Back을 누르면 `onBack()`을 호출한다.
- 화면 안에 뒤로가기 버튼은 두지 않는다(v2 디자인에 없음).

### 1-3. Pleos 기본 컴포넌트 (`ui/components/pleos/`)

| 컴포넌트 | 사용 예 | 메모 |
|---|---|---|
| `PleosButton` | `PleosButton("다시 점검", onClick)` | 높이 100. **너비는 쓰는 쪽에서** `Modifier.width(…)` 또는 `weight(1f)`로 정한다. |
| | `PleosButton("경유지 추가", onClick, type = PleosButtonType.Filled)` | `Basic` = 회색(피그마 Type "Primary"), `Filled` = 진한색(피그마 Type "Secondary"). 한 화면의 주 행동은 Filled. |
| `PleosSegmentedControl` | `PleosSegmentedControl(listOf("전체","가족","상사","지인","기타"), selected, onSelect = { selected = it })` | 높이 100. 칸 너비는 항목 수로 똑같이 나눈다. |
| `PleosSwitch` | `PleosSwitch(checked, onCheckedChange = { … })` | 80×48. 줄 전체를 눌러 켜고 끄려면 `ToggleRow`를 쓴다. |

### 1-4. 모심 전용 컴포넌트 (`ui/components/`)

| 컴포넌트 | 모양 | 사용 예 |
|---|---|---|
| `JudgmentBanner` | 상태색 배경 + 점 + 44 제목 + 설명 + (버튼) | `JudgmentBanner(MosimStatus.Caution, "픽업 전 충전이 필요합니다", description = "…", actionText = "충전소 보기", onAction = {})` |
| `StatusChip` | 작은 태그 | `StatusChip("부족", ChipTone.Caution)` · 작은 크기 `size = ChipSize.Small` |
| `InfoCard` | 제목 있는 흰 카드 | `InfoCard("차량 상태", Modifier.weight(1f)) { InfoRow(…) }` |
| `InfoRow` | 아이콘·라벨 / 칩·값, 아래 구분선 | `InfoRow("배터리", "12%", icon = painterResource(R.drawable.ic_battery), chipText = "부족", chipTone = ChipTone.Caution)` |
| `CompactInfoCard` | 라벨 / 값 한 줄 카드 | `CompactInfoCard("실내 온도", "24℃")` |
| `PassengerProfileCard` | 프로필 원 + 이름 + 태그, 선택 시 테두리 | `PassengerProfileCard("엄마", listOf(ProfileTag("가족"), ProfileTag("개인화 12회", ChipTone.Dark)), selected = true, onClick = {})` |
| `AddPassengerCard` | 점선 "+ 신규 등록" | `AddPassengerCard(onClick = {})` |
| `NumberedOptionItem` | 번호 원 + 제목 + 태그 + 설명 | `NumberedOptionItem(1, "카페 A", "0.4km · 경유 +3분", onClick = {}, tag = "드라이브스루")` |
| `ToggleRow` | 제목 + 태그 + 설명 + 스위치 (줄 전체 터치) | `ToggleRow("실내 온도", "24℃ → 23℃", checked, { checked = it }, tag = "반복 · 3회째")` |
| `EscortHeroPanel` | 모시는 중 이름(72) + 목적지 + 예상 도착(140) | `EscortHeroPanel("엄마", "서울대병원 본관", "09:12", "남은 거리 14.6km · 43분")` |
| `StatusPill` | 진한 알약 + 초록 점 | `StatusPill("동승자 음성 요청 대기 중")` |
| `MapPlaceholder` | 회색 판 + 안내 문구 | `MapPlaceholder(Modifier.weight(1f), text = "지도 · 경로 + 충전소 3곳")` |
| `MosimTextField` | 라벨이 안쪽에 있는 회색 입력 칸 | `MosimTextField("이름 · 호칭", name, { name = it }, placeholder = "예: 엄마, 김 부장님")` |

**칩 색 고르기 (`ChipTone`)**

| ChipTone | 모양 | 예 |
|---|---|---|
| `Neutral` | 연회색 | "가족", "Default", "빈 자리 1/2" |
| `Dark` | 진회색 + 흰 글자 | "개인화 12회", "반복 · 3회째", "08:17 시작 · 진행 중" |
| `Normal` | 연초록 | "8분 여유", "승인됨", "적용", "감지", "정차" |
| `Caution` | 연주황 | "부족", "짐 있음 추정" |
| `Warning` | 연빨강 | (v2 예시 없음) |

칩만 있고 값이 없는 행(D-1 "감지", G-1 "확인" 등)은 `InfoRow(label, value = "", chipText = …, chipTone = ChipTone.Normal)`로 쓴다.

### 1-5. 아이콘 (`res/drawable/`)

Lucide 아이콘을 28dp, 선 2dp로 변환했다. 색은 `InfoRow`가 알아서 64% 회색으로 칠한다.

| 리소스 | 쓰는 곳 |
|---|---|
| `R.drawable.ic_battery` | 배터리 |
| `R.drawable.ic_map_pin` | 주행 가능, 픽업까지, 목적지까지, 픽업 위치 |
| `R.drawable.ic_thermometer` | 실내 온도, 사전 공조 |
| `R.drawable.ic_users` | 동승석 |
| `R.drawable.ic_clock` | 픽업 도착 예정 |
| `R.drawable.ic_cloud_rain` | 날씨 |

새 아이콘이 필요하면 피그마 레이어 이름(예: `map-pin`)으로 Lucide에서 같은 SVG를 받아 변환한다.

### 1-6. 자주 쓰는 배치

v2 화면 대부분은 **왼쪽 내용 + 오른쪽 보조(지도·카드)** 2열이다.

```kotlin
MosimAppContainer(title = "…") {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
    ) {
        Column(Modifier.weight(1f).fillMaxHeight()) { /* 왼쪽 */ }
        MapPlaceholder(Modifier.weight(1f).fillMaxHeight())  // 오른쪽
    }
}
```

- 카드 높이를 맞출 때는 고정 숫자 대신 `fillMaxHeight()` / `weight(1f)`를 쓴다(폼팩터마다 앱 영역 크기가 다르다).
- 버튼이 카드 열 안에 있는 화면(B-2, F-1, A-3 등)은 `bottomActions` 대신 그 열 맨 아래에 `PleosButton`을 직접 놓는다.

---

## 2. 화면별 사용 컴포넌트 (A~G)

> 각 프레임의 "없음"은 지금 공통 컴포넌트로 바로 만들 수 없는 부분이다. 맡은 사람이 화면 파일 안에 만들고, 두 화면 이상에서 쓰게 되면 `ui/components/`로 옮긴다(3장 목록).

### A. 이동 계획 (운전자 모드 · 정차 중)

**A-1 동승자 선택**
- `MosimAppContainer` ("누구를 모시나요?")
- `PleosSegmentedControl` — 관계 탭 (전체/가족/상사/지인/기타)
- `PassengerProfileCard` × N + `AddPassengerCard` — 왼쪽 그리드
- 오른쪽 "○○의 저장된 선호도": `InfoCard` + `InfoRow` (라벨/값, "자동 적용 동의"는 칩 `Normal`)
- 하단 `PleosButton(Filled)` "이동 설정"

**A-2 신규 동승자 등록**
- `MosimAppContainer` ("신규 동승자 등록")
- `InfoCard`("기본 정보") 안에 `MosimTextField`("이름 · 호칭"), `PleosSegmentedControl`(관계), `MosimTextField`("좌석 위치", trailing = "변경")
- 오른쪽 동의 항목: `ToggleRow` (A-2의 `consent` = `ivi_switch`)
- 하단 버튼 `PleosButton`

**A-3 이동 설정**
- `MosimAppContainer` ("이동 설정")
- 왼쪽 `InfoCard`("어디서 어디로 모시나요?") 안에 픽업 위치 / 목적지 / 약속 시각 행
  - **없음:** 값 + "변경" 표시 행(누르면 A-4로 이동). 임시로 `MosimTextField(trailing = …)`를 쓸 수 있지만 입력 칸이라 키보드가 뜬다. 표시 전용 행이 필요하다.
- 카드 안 맨 아래 `PleosButton(Filled)` "출발 전 점검" (너비 채움)
- 오른쪽 `MapPlaceholder`("경로 미리보기")

**A-4 장소 검색**
- `MosimAppContainer` ("장소 검색")
- 검색 입력: `MosimTextField` 기반. **없음:** 포커스 상태(흰 배경 + 진한 테두리)와 "지우기" 버튼 → `trailing`으로 "지우기"를 넣고 테두리는 화면에서 추가
- `InfoCard`("검색 결과") + 결과 행 — **없음:** 제목·주소(2줄) / 거리 행. `NumberedOptionItem`에서 번호 원을 뺀 모양이다.
- 오른쪽 `MapPlaceholder`

### B. 출발 전 점검 (운전자 모드 · 정차 중)

**B-1 점검 결과** — 공통 컴포넌트로 전부 만들 수 있다.
- `MosimAppContainer` ("출발 전 점검", 부제 = 동승자 · 픽업 → 목적지)
- `JudgmentBanner(MosimStatus.Caution, …, actionText = "충전소 보기")`
- 카드 3열(`Row` + `weight`): `InfoCard`("차량 상태" / "이동 상태" / "외부 환경") + `InfoRow`(아이콘 1-5)
- `bottomActions`: `PleosButton` "다시 점검" + `PleosButton(Filled)` "에스코트 계획 보기"

**B-2 충전소 경유 선택**
- 왼쪽 `InfoCard`("경로상 충전소") + 충전소 목록
  - **없음:** 선택형 목록 행(선택되면 회색 배경, 오른쪽에 칩 "빈 자리 2/4"). 번호 원이 없는 `NumberedOptionItem` + 선택 상태.
- 요약 한 줄 카드: `CompactInfoCard`와 비슷하지만 값 자리에 칩("약속 3분 전", `Normal`)이 들어간다 → `Row` + `StatusChip`
- 왼쪽 열 아래 `PleosButton` "건너뛰기" + `PleosButton(Filled)` "경유지 추가" (`weight(1f)`씩)
- 오른쪽 `MapPlaceholder`("지도 · 경로 + 충전소 3곳")

**B-3 에스코트 계획 확정**
- 왼쪽 `InfoCard`("단계별 계획")
  - **없음:** 타임라인 행(왼쪽 시각 "07:55" + 번호 원 + 제목 + 설명). `NumberedOptionItem` 앞에 시각을 붙인 모양이다.
- 오른쪽 위 `InfoCard`("운전자 승인") + `InfoRow`(value = "", 칩 "승인됨" `Normal`)
- 오른쪽 가운데 `InfoCard`("자동 처리 (동승자 동의 범위)") + 본문 `Text`(`BodySmall.strong()`)
- 오른쪽 아래 `PleosButton(Filled)` "안내 시작" (너비 채움)

### C·D. 픽업 · 탑승

**C-1 픽업지 이동 중** (주행 중 → 큰 글씨, 조작 최소)
- 왼쪽 큰 카드: "픽업 도착 예정" + `MosimTypography.HeroEta` "08:27" + `StatusChip`("약속 08:30 · 3분 여유", `Normal`) + 남은 거리(`HeroSub`). 맨 아래 "충전 완료 · 잔량 45%"
  - `EscortHeroPanel`과 모양이 다르다(이름이 없음). 흰 카드 `Column`에 위 토큰을 직접 쓴다.
- 오른쪽 `InfoCard`("다음 준비") + `InfoRow`(value = "", 칩 `Dark` "08:17 시작 · 진행 중" / `Neutral` "픽업지 정차 후")
- 오른쪽 아래 `CompactInfoCard`("엄마에게 픽업 위치 공유", "공유됨")

**C-2 픽업지 도착 · 실내 준비**
- `MosimAppContainer` ("엄마 맞이 준비")
- 왼쪽 `InfoCard`("실내 온도")
  - **없음:** 저장 당시 / 현재 비교 박스 2개(회색 `MosimShape.Field` 박스), 진한 "적용 온도 24℃" 박스(`ButtonFilledEnabled` 배경 + 흰 글자)
- 가운데 `InfoCard`("준비 항목") + `InfoRow`(값 + 칩 "적용" `Normal`)
- 오른쪽 카드 "준비 상태 4 / 4 완료" + 아래 "● 탑승 대기 중"
  - **없음:** 밝은 배경용 점 + 문구(`StatusPill`은 진한 배경만 있다)
- 오른쪽 아래 `PleosButton` "설정 조정"

**D-1 탑승 확인**
- 왼쪽 `InfoCard`("감지된 신호") + `InfoRow`(value = "", 칩 "열림 → 닫힘" / "감지" / "착용" / "정차" `Normal`) + 아래 안내 문구(`MosimTypography.Caption`, 64%)
- 오른쪽 흰 카드: 질문 "엄마가 탑승하셨나요?"(`MosimTypography.BannerTitle`) + 설명(`BodySmall`, 64%)
- 오른쪽 카드 안 아래 `PleosButton` "다른 동승자" + `PleosButton(Filled)` "탑승 완료 · 에스코트 시작"

### E·F. 이동 중 (에스코트 모드 · 주행 중)

> 주행 중 화면이다. 버튼과 긴 목록을 두지 않는다. Back은 최상단 규칙(`onBack = null`)을 검토한다.

**E-1 에스코트 모드** — 공통 컴포넌트로 전부 만들 수 있다.
- `MosimAppContainer` ("에스코트 모드", 부제 "08:29 엄마 탑승 · 운전자 조작 최소화")
- 왼쪽 `EscortHeroPanel`
- 오른쪽 열: `CompactInfoCard` × 3 (실내 온도 / 음악 / 좌석) + 맨 아래 `StatusPill`("동승자 음성 요청 대기 중")

**E-2 자동 조절 알림**
- E-1과 같다.
- 제목 옆 진한 토스트 "온도를 24℃ → 23℃로 낮췄어요 · 선호 범위 내 자동 조절"(피그마 `toast/auto adjust`)는 `feature/escort/EscortScreen.kt` 안에 직접 그렸다(`MosimAppContainer(headerTrailing)`). `[확인 필요]` Pleos System Notification으로 바꿀지.

**F-1 지연 감지 · 휴식 제안**
- `MosimAppContainer` ("이동 계획 수정")
- **없음:** 위 주황 비교 카드("예상 소요 ~~30분~~ → 60분", "도착 ~~09:12~~ → 09:42"). 배경 `MosimStatus.Caution.container`, 취소선은 `TextDecoration.LineThrough`
- `InfoCard` 형태의 "AI 제안" 카드 + 제안 문구(`BodyLarge.strong()`)
  - **없음:** 안쪽 회색 제안 박스("휴식 장소 A" + 설명 + 칩 "경유지 1/2 여유" `Neutral`)
- 왼쪽 열 아래 `PleosButton` "기존 경로 유지" + `PleosButton(Filled)` "휴식 장소 경유"
- 오른쪽 `MapPlaceholder`("지도 · 정체 구간 + 휴식 장소")

**F-2 음성 요청 인식**
- `MosimAppContainer` ("동승자 요청", 부제 "음성 인식 중")
- 흰 카드 가운데 정렬: "듣고 있어요"(`BodySmall`, 64%) + 인식 문장(`MosimTypography.BannerTitle`) + 칩 "의도: 경유지 추가" 등(`StatusChip`, `Neutral`, `Small`)
- **없음:** 음성 파형 아이콘/애니메이션 (Lucide `audio-lines`로 대체 가능 `[확인 필요]`)

**F-3 경유지 후보 · 승인**
- `MosimAppContainer` ("경유지 후보")
- 왼쪽 흰 카드: 안내 문구 + 칩 "경유지 1/2 여유" + `NumberedOptionItem` × 3
- 왼쪽 열 아래 `PleosButton` "취소" + `PleosButton(Filled)` "카페 A 경유 추가"
- 오른쪽 `MapPlaceholder`("지도 · 현재 위치 + 카페 3곳 번호 핀")

### G. 도착

**G-1 도착 · 에스코트 종료**
- `MosimAppContainer` ("도착", 부제 "09:45 서울대병원 본관 · 에스코트 종료")
- 왼쪽 `InfoCard`("하차 감지") + `InfoRow`(value = "", 칩 "확인" / "열림 → 닫힘" / "해제" / "정차" `Normal`)
- 오른쪽 `InfoCard`("이번 에스코트") + `InfoRow`(이동 시간 / 거리 / 경유 / 자동 조정 / 음성 요청)
  - 피그마 값 글자는 30 Bold라서 `InfoRow`(36)보다 조금 작다. 맞추려면 `InfoRow`에 크기 옵션을 추가한다.
- 오른쪽 카드 안 아래 `PleosButton(Filled)` "프로필 업데이트 확인" (너비 채움)

**G-2 프로필 업데이트 제안**
- `MosimAppContainer` ("프로필 업데이트")
- 왼쪽 흰 카드: "저장할 항목을 선택하세요" + 칩 "AI 추천 2개" + `ToggleRow` × N (태그 "반복 · 3회째" 등)
- 오른쪽 메모 카드 "저장 기준": 흰 카드 + 제목(`BodySmall.strong()`) + 설명(`LabelMedium`, 64%)
- 버튼 `PleosButton` "나중에" + `PleosButton(Filled)` "선택 항목 저장"

---

## 3. 아직 없는 컴포넌트 (필요한 화면)

| 필요한 것 | 화면 | 가까운 기존 컴포넌트 |
|---|---|---|
| 값 + "변경" 표시 행 (키보드 없음) | A-3 | `MosimTextField(trailing)` |
| 검색 입력 포커스 상태 + 지우기 | A-4 | `MosimTextField` |
| 제목·설명 / 오른쪽 값 목록 행 (번호 없음) | A-4, B-2 | `NumberedOptionItem` |
| 선택 상태가 있는 목록 행 + 오른쪽 칩 | B-2 | `NumberedOptionItem` |
| 타임라인 행 (시각 + 번호) | B-3 | `NumberedOptionItem` |
| 비교 박스 / 진한 강조 박스 | C-2 | — |
| 밝은 배경용 점 + 문구 | C-2 | `StatusPill` |
| 상단 토스트 | E-2 | Pleos System Notification `[확인 필요]` |
| 변경 전후 비교 카드 (취소선) | F-1 | `JudgmentBanner` 색 |
| 카드 안 제안 박스 | F-1 | — |
| 음성 파형 | F-2 | — |

같은 모양이 두 화면 이상에서 나오면(목록 행 3종 등) 먼저 만든 사람이 `ui/components/`로 올리고 이 표를 고친다.

---

## 4. [확인 필요] 값

| 항목 | 지금 값 | 근거 |
|---|---|---|
| 버튼 최소 너비 | 280 | 피그마에 변수명만 보임. 가장 좁은 버튼 기준 |
| 세그먼트 라운드 | 24 / 18 | 화면에서 잰 값 |
| `ButtonBasicDisabled` | 흰색 20% | 토큰표 표기가 애매함 |
| 경고(빨강) 색 | Pleos negative #FE3D16 | v2에 예시 없음 |
| 판단 배너 점 크기 | 24 | E-1 알약 점과 같다고 봄 |
| G-2 글자 크기 | 32 / 22 / 24 | F-3 값을 따름 |
| 토큰 밖 글자 행간 | 120% | 피그마 100%, 한글 잘림 방지 |

---

## 5. 출처와 라이선스

- 글꼴 Asta Sans (42dot, OFL 1.1) — `docs/licenses/AstaSans-OFL.txt`
- 아이콘 Lucide (ISC) — `docs/licenses/Lucide-LICENSE.txt`
- 색·글자 토큰 — 피그마 "✦ Foundation" 페이지
- 컴포넌트 수치 — 피그마 "모심 화면 v2" 각 요소 속성
