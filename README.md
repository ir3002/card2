# 💳 카드 가계부

카드 SMS 자동 수집 + 수동 입력 + 예산 알림 안드로이드 앱

![Android](https://img.shields.io/badge/Android-API%2026+-green)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-최신-orange)

---

## ✨ 주요 기능

| 기능 | 설명 |
|------|------|
| 📱 SMS 자동 수집 | 신한·국민·삼성·현대·롯데·하나·우리·NH 8개 카드사 문자 자동 파싱 |
| ✍️ 수동 입력 | 카드·현금 거래 직접 등록, 카테고리 분류 |
| 💳 카드별 관리 | 결제일·이용기간·목표금액 카드별 설정 |
| 🔔 스마트 알림 | 예산 70%/90%/100% 도달 + 결제일 D-3 알림 |
| 📊 대시보드 | 월별 사용현황, 카드별 잔액, 카테고리 분석 |

---

## 🚀 빠른 시작

### 방법 1 — GitHub Actions로 APK 받기 (가장 쉬움)

```
1. 이 저장소를 Fork
2. Actions 탭 → "Android APK Build" → "Run workflow"
3. 빌드 완료 후 Artifacts에서 APK 다운로드
4. 안드로이드 폰에 설치
```

### 방법 2 — 로컬 빌드 (Android Studio)

```bash
git clone https://github.com/YOUR_ID/CardBudget.git
cd CardBudget
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

---

## 📦 GitHub에 올리는 방법

```bash
# 1. 압축 해제 후 해당 폴더로 이동
cd CardBudget

# 2. Git 초기화
git init
git add .
git commit -m "feat: 카드 가계부 앱 초기 커밋"

# 3. GitHub에 새 저장소 만들고 연결
git remote add origin https://github.com/YOUR_ID/CardBudget.git
git branch -M main
git push -u origin main

# → push하면 GitHub Actions가 자동으로 APK 빌드 시작!
```

---

## 🔑 Release APK를 위한 Secrets 설정

GitHub → Settings → Secrets → Actions에서 추가:

| Secret 이름 | 설명 |
|-------------|------|
| `KEYSTORE_BASE64` | `base64 keystore.jks` 결과값 |
| `KEYSTORE_PASSWORD` | 키스토어 비밀번호 |
| `KEY_ALIAS` | 키 별칭 |
| `KEY_PASSWORD` | 키 비밀번호 |

```bash
# keystore 생성 (최초 1회)
keytool -genkey -v -keystore keystore.jks \
  -alias cardbudget -keyalg RSA -keysize 2048 -validity 10000

# base64로 인코딩해서 Secret에 붙여넣기
base64 -i keystore.jks | pbcopy   # macOS
base64 keystore.jks               # Linux
```

---

## 🏗️ 기술 스택

```
Language    : Kotlin 1.9
UI          : Jetpack Compose + Material 3
DB          : Room (SQLite)
DI          : Hilt
Background  : WorkManager
Architecture: MVVM + Repository
Min SDK     : 26 (Android 8.0)
```

---

## 📁 프로젝트 구조

```
app/src/main/java/com/cardbudget/
├── data/
│   ├── entity/   # Room 엔티티 (Card, Transaction, Budget)
│   ├── dao/      # SQL 쿼리
│   ├── db/       # Database 설정
│   └── repository/
├── ui/
│   ├── home/     # 대시보드
│   ├── transaction/  # 거래내역 + 수동입력
│   ├── card/     # 카드관리
│   ├── notification/ # 알림설정
│   └── theme/
└── util/
    ├── SmsParser.kt          # 카드사별 SMS 파싱
    ├── SmsBroadcastReceiver.kt
    └── NotificationHelper.kt # WorkManager 알림
```

---

## ⚠️ 주의사항

- SMS 권한은 실기기에서만 동작 (에뮬레이터 불가)
- `keystore.jks`는 절대 GitHub에 올리지 마세요 (`.gitignore`에 포함됨)
