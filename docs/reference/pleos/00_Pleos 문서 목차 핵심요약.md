# Pleos Playground 개발자 문서 — 목차 & 공모전용 핵심 요약

> 수집일: 2026-11-07 (Chrome으로 https://document.pleos.ai 를 열어 수집)
> 원본 포털: https://developers.pleos.ai/playground (2026-10-29부터 `pleos.ai/playground` → `developers.pleos.ai`로 주소 변경)
> 각 문서 섹션마다 `<!-- 원문: 경로 -->` 주석이 있습니다. 앞에 `https://document.pleos.ai`를 붙이면 원문 링크입니다.
> 이미지·Java 예제는 제외했고, 자동 생성 API Reference(약 1,800페이지)는 메서드·필드 이름 색인만 추렸습니다.

## ⚠️ 현재 SDK 사용 가능 현황 (2026-11-08 개발자 포털 확인)
개발자 포털에 표시된 SDK는 6개이고, **지금 사용할 수 있는 것은 Vehicle State와 NaviHelper 2개뿐**입니다.

| SDK | 상태 |
| --- | --- |
| **Vehicle State** (차량 상태 조회) | ✅ 사용 가능 |
| **NaviHelper** | ✅ 사용 가능 |
| FusedLocation | ⏳ Coming soon |
| TextToSpeech | ⏳ Coming soon |
| SpeechToText | ⏳ Coming soon |
| Vehicle Control (차량 제어) | ⏳ Coming soon |

- 포털에서는 Vehicle SDK가 **State(조회)**와 **Control(제어)**로 나뉘어 있습니다. 아래 문서의 조회 API(`get*`, `register*`, 권한 `CAR_*`)는 쓸 수 있고, **제어 API(`set*`, `move*`, 권한 `CONTROL_CAR_*`)는 아직 쓸 수 없습니다**.
- 아래 문서들에 TTS·STT·FusedLocation·Vehicle 제어 내용이 있지만, 현재 기획·개발에서는 **사용 불가 전제**로 다룹니다. 제공 시점이 공모전 일정 안에 들어오는지는 확인이 필요합니다.
## 문서 목록
| 파일 | 내용 |
| --- | --- |
| 01_시작하기_테스트_릴리즈노트 | SDK 개요·API 정책, Quick start, 프로젝트 생성·SDK 신청, **Virtual Console 카드 전체 목록**, 폼 팩터 테스트, 주행 규제 판단 기준, 릴리즈 노트(Emulator 2.1.0, SDK 버전·Breaking change) |
| 02_Vehicle_SDK | Vehicle SDK 가이드 전체(Import, 권한, 도메인 객체, Capability/Get/Set/Callback, Error code), Sample App 튜토리얼, **도메인별 API 메서드 색인**, 주요 enum — *현재는 조회(State)만 사용 가능* |
| 03_NaviHelper_FusedLocation_SDK | 목적지·경로·경유지·이벤트·오류, **NaviHelper 데이터 모델 필드 색인**, FusedLocation(위치·지오펜스) — *FusedLocation은 Coming soon* |
| 04_TTS_STT_SDK | TextToSpeech·SpeechToText(모드, 앱 등록, 리스너, locale, 오디오 전송, ErrorType) — *둘 다 Coming soon* |
| 05_앱_기획설계_가이드 | 주행 안전 UX, **지역별 주행 규제 표**, 앱 카테고리, 멀티 윈도우 화면 모델, 하드웨어 접근, **AAOS vs Pleos SDK 선택 기준**, 성능 수치 기준 |
| 06_앱_개발가이드_오디오_주행규제_CCP | CarUxRestrictions 구현, 오디오 포커스 정책, Suspend, CCP Back 처리(필수), Navigation/Music 앱 정책, FAQ(차속·지역코드·코덱), ADB 명령 |
| 07_앱_심사가이드_제출 | App Market 심사 기준(성능 시간 기준, 주의 분산, 개인정보), 제출 항목 |

## 버전 기준 (2026-10-19 릴리즈)
Vehicle SDK **2.3.2** · NaviHelper **3.0.5** · TextToSpeech **3.3.1** · SpeechToText **3.3.1** · FusedLocation **1.1.3** · Emulator **2.1.0** (API 34 system image, AVD `Pleos_Connect_v2.1`)
Maven: `https://nexus-playground.pleos.ai/repository/maven-releases/` / 좌표 `ai.pleos.playground:<SDK이름>:<버전>`

## 공모전 기획·개발에 바로 쓰이는 핵심 사실
1. **SDK 구성**: 문서 기준 Vehicle / NaviHelper / TextToSpeech / SpeechToText / FusedLocation 5개(포털에서는 Vehicle이 State·Control로 나뉘어 6개). **현재 사용 가능은 Vehicle State·NaviHelper뿐**(위 현황표 참고). **LLM SDK는 2026-10-19 제공 종료**(설명회 자료에는 없던 내용). AI 음성 연동은 Gleo Interface로 안내됨.
2. **차속·기어는 Pleos Vehicle SDK에 없음.** `VehicleSpeedListener`가 2.3.2에서 삭제됐고, 정책상 속도·기어·외기 온도·연료 레벨 같은 표준 데이터는 **AAOS `CarPropertyManager`**(예: `PERF_VEHICLE_SPEED`)로 읽음. 주행/정차 UI 전환은 **`CarUxRestrictions`**로 처리.
3. **EV 관련 데이터가 풍부함** (설명회 예시 "배터리 상태 + 이동 경로" 아이디어에 직결):
    - Vehicle `EvBattery`: 충전율(SOC), 전비(현재/누적/순간/충전 후) 조회 ✅ / 충전 전류·목표 충전량·충전 포트·충전 스위치 **제어는 Coming soon**
    - NaviHelper `TripInfo`: **목적지 도착 예상 SOC(`estimatedSocAtDest`)**, 에너지 소비량, ETA / `DrivingState`: 충전 계획(`ChargingPlan`), 추천 충전소 여부 / `NavigationScreenRequest.nearbyChargingStations`
    - Virtual Console Energy 카드로 SOC·배터리 용량·충전 상태를 바꿔가며 테스트 가능
4. **탑승자 맥락 데이터**(조회는 사용 가능): 좌석 탑승 여부(`getCurrentSeatOccupancy`), 안전벨트, 도어·창문, 공조 상태(좌석별 온도·열선·통풍), 경고등(Telltale), 트립 주행 거리·시간. 공조·좌석·앰비언트 라이트 **제어는 Coming soon**.
5. **위치**: FusedLocation(`pleos.car.permission.FUSED_LOCATION`, dangerous) + Proximity Alert(지오펜스 진입/이탈) — **Coming soon**.
6. **음성**: TTS/STT — **Coming soon**. (문서상: 전 차종 호환, HYBRID 기본(서버 우선→On-device 전환), 서버 모드는 My Project의 Client ID/Secret으로 `registerApp()` 필요, STT는 텍스트만 반환, 한국어 `ko-KR` 지원)
7. **권한**: 매니페스트 `pleos.car.permission.*`. 조회 `CAR_*`, 제어 `CONTROL_CAR_*`. 배포 시엔 App Market 승인 권한만 허용(설명회 내용과 일치). 개발자 포털 기준 **Vehicle State·NaviHelper는 신청 없이 사용 가능**.
8. **화면**: Driving view + 앱 영역(최대 2개 앱). **portrait(1 window)·landscape(2 window) 화면을 각각 설계**해야 함. CCP Back 시 최상위 화면에서는 `moveTaskToBack(true)`로 숨김 처리(미구현 시 심사 반려 대상).
9. **주행 중 제한**: DO 미선언 앱은 주행 중 사용 불가. 동영상·게임·브라우저는 정차 중에만 사용 가능. 한국 포함 "그 외 지역"은 주행 중 키보드·검색 허용, 차량 설정·동영상·QR·매뉴얼은 제한.
10. **성능 기준**(PT·데모 품질 체크용): 실행·콘텐츠 로드 10초 이내, 버튼 반응 2초 이내, 터치 응답 0.25초 이내, CPU 주행 중 5% 미만(내비 15%)·정차 중 25% 이하, RAM 400MB 이하.
11. **테스트 도구**: Virtual Console(기어 P/R/N/D, 배터리, 도어, 공조, 좌석 탑승 등), FormFactorConsole(화면 크기·방향), ADB `driving_regulation on/off`, `screenrecord`(시연 영상 제작).
12. **Sample App**: Vehicle SDK Sample App(Door Position 예제)으로 Capability→Get→Register→Set→Unregister 흐름 학습(Set 단계는 Vehicle Control 제공 후 사용 가능). 제품 앱은 실제 쓰는 API의 최소 권한만 선언.
 
