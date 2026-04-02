# EStudy PTIT

> Ứng dụng học từ vựng tiếng Anh bằng Flashcard tích hợp AI — Dự án môn Lập trình Thiết bị Di động, Nhóm 5 D22CNPM02, PTIT

---

## Mục lục

- [Giới thiệu](#giới-thiệu)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Cấu trúc dự án](#cấu-trúc-dự-án)
- [Cài đặt Backend](#cài-đặt-backend)
- [Cài đặt Frontend (Android)](#cài-đặt-frontend-android)
- [Kiến trúc hệ thống](#kiến-trúc-hệ-thống)
- [Các tính năng chính](#các-tính-năng-chính)
- [API Endpoints](#api-endpoints)
- [Cơ sở dữ liệu](#cơ-sở-dữ-liệu)
- [Phân công thành viên](#phân-công-thành-viên)

---

## Giới thiệu

EStudy PTIT là ứng dụng di động Android giúp sinh viên học từ vựng tiếng Anh chuyên ngành thông qua:

- Tạo và quản lý bộ Flashcard cá nhân
- Học tập qua 4 chế độ: Lật thẻ, Trắc nghiệm, Nối từ, Điền từ
- Thuật toán **Spaced Repetition (SM-2)** nhắc nhở ôn tập đúng lúc
- Theo dõi thống kê tiến độ học tập
- Tạo và tham gia lớp học nhóm
- Tích hợp AI: chatbot luyện tập, tạo câu hỏi tự động, kiểm tra chính tả

---

## Công nghệ sử dụng

### Backend

| Thành phần | Công nghệ | Phiên bản |
|---|---|---|
| Ngôn ngữ | Java | 21 |
| Framework | Spring Boot | 3.5.12 |
| Bảo mật | Spring Security + JWT (nimbus-jose-jwt) | OAuth2 Resource Server |
| ORM | Spring Data JPA + Hibernate | 6.6.x |
| Database | MySQL | 8.x |
| Connection Pool | HikariCP | 6.3.x |
| Mapping | MapStruct | 1.5.5 |
| Code giảm thiếu | Lombok | - |
| API Docs | SpringDoc OpenAPI (Swagger UI) | 2.8.6 |
| Build tool | Maven | 3.9.x |

### Frontend (Android)

| Thành phần | Công nghệ | Phiên bản |
|---|---|---|
| Ngôn ngữ | Java | 11 |
| Min SDK | Android 7.0 | API 24 |
| Target SDK | Android 16 | API 36 |
| HTTP Client | Retrofit2 + OkHttp3 | 2.9.0 / 4.12.0 |
| JSON Parser | Gson | 2.10.1 |
| UI | Material Design 3 | 1.11.0 |
| Build Tool | Gradle (Kotlin DSL) | 8.x |
| IDE | Android Studio Ladybug (2024.2.1+) | - |

---

## Cấu trúc dự án

```
english-learning-app-mobile/
├── backend/                          # Spring Boot REST API
│   ├── src/main/java/com/estudy/backend/
│   │   ├── BackendApplication.java   # Entry point
│   │   ├── configuration/            # Cấu hình bảo mật, JWT, Swagger
│   │   │   ├── CustomJwtDecoder.java
│   │   │   ├── JwtAuthenticationEntryPoint.java
│   │   │   ├── OpenApiConfig.java
│   │   │   ├── PasswordEncoderConfig.java
│   │   │   └── SecurityConfig.java
│   │   ├── controller/               # REST Controllers (API endpoints)
│   │   │   ├── AuthenticationController.java
│   │   │   ├── CommentController.java
│   │   │   └── FlashCardSetController.java
│   │   ├── dto/                      # Data Transfer Objects
│   │   │   ├── request/              # Request bodies từ client
│   │   │   └── response/             # Response bodies trả về client
│   │   ├── entity/                   # JPA Entities (ánh xạ bảng DB)
│   │   │   ├── User.java
│   │   │   ├── FlashCard.java
│   │   │   ├── FlashCardSet.java
│   │   │   ├── Comment.java
│   │   │   └── InvalidatedToken.java # Blacklist JWT đã logout
│   │   ├── enums/                    # Enum values
│   │   │   └── Privacy.java          # PUBLIC / PRIVATE
│   │   ├── exception/                # Xử lý lỗi tập trung
│   │   │   ├── AppException.java
│   │   │   ├── ErrorCode.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── mapper/                   # MapStruct mappers (Entity ↔ DTO)
│   │   │   ├── CommentMapper.java
│   │   │   ├── FlashCardSetMapper.java
│   │   │   └── UserMapper.java
│   │   ├── repository/               # Spring Data JPA Repositories
│   │   │   ├── CommentRepository.java
│   │   │   ├── FlashCardSetRepository.java
│   │   │   ├── InvalidatedTokenRepository.java
│   │   │   └── UserRepository.java
│   │   └── service/                  # Business Logic
│   │       ├── AuthenticationService.java
│   │       ├── CommentService.java
│   │       └── FlashCardSetService.java
│   ├── src/main/resources/
│   │   ├── application.yml           # Config local (tự tạo, không commit)
│   │   └── application-prod.yml      # Config mẫu (template)
│   └── pom.xml                       # Maven dependencies
│
└── app/                              # Android Application
    └── app/src/main/java/com/estudy/app/
        ├── api/                      # Kết nối API
        │   ├── ApiClient.java        # Cấu hình Retrofit instance
        │   ├── ApiService.java       # Định nghĩa các API endpoints
        │   └── AuthInterceptor.java  # Tự động gắn JWT vào request
        ├── controller/               # Activities (màn hình)
        │   ├── SplashActivity.java
        │   ├── LoginActivity.java
        │   ├── RegisterActivity.java
        │   ├── HomeActivity.java
        │   └──
        ├── model/                    # Data models
        │   ├── request/              # Request objects gửi lên API
        │   └── response/             # Response objects nhận từ API
        └── utils/
            └── TokenManager.java     # Lưu/đọc JWT từ SharedPreferences
```

---

## Cài đặt Backend

### Yêu cầu

- **JDK:** 21 (Temurin/Eclipse Adoptium khuyến nghị)
- **Maven:** 3.9+
- **MySQL:** 8.x
- **IDE:** IntelliJ IDEA (khuyến nghị) hoặc VS Code

### Bước 1 — Set JAVA_HOME (nếu có nhiều JDK)

**Windows (PowerShell — tạm thời):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot"
```

**Set vĩnh viễn:** Vào `System Properties > Environment Variables`, sửa biến `JAVA_HOME` trỏ đến JDK 21.

**Kiểm tra:**
```powershell
mvn -version  # Phải hiện Java version: 21.x.x
```

### Bước 2 — Tạo file cấu hình

Tạo file `backend/src/main/resources/application.yml` (copy từ `application-prod.yml`):

```yaml
spring:
  application:
    name: backend

  datasource:
    url: jdbc:mysql://localhost:3306/estudy?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root          # Đổi thành username MySQL của bạn
    password: your_password # Đổi thành password MySQL của bạn

  jpa:
    hibernate:
      ddl-auto: update      # Tự tạo/cập nhật bảng khi chạy
    show-sql: false

jwt:
  signerKey: your_512bit_key_here  # Tạo tại https://jwtsecretkeygenerator.com (512-bit)
  valid-duration: 3600
  refreshable-duration: 36000

app:
  default-admin:
    allowed-origins: http://localhost:3000
```

> **Lưu ý:** `createDatabaseIfNotExist=true` giúp MySQL tự tạo database `estudy` nếu chưa tồn tại — không cần tạo thủ công.

> **Lưu ý:** File `application.yml` đã được thêm vào `.gitignore` — không bao giờ commit file này lên Git vì chứa thông tin nhạy cảm.

### Bước 3 — Build và chạy

```bash
cd backend

# Lần đầu hoặc khi pom.xml thay đổi
mvn clean install -DskipTests

# Chạy ứng dụng
mvn spring-boot:run
```

Backend sẽ khởi động tại `http://localhost:8080`

### Swagger UI

Sau khi chạy, truy cập tài liệu API tại:
```
http://localhost:8080/swagger-ui/index.html
```

### Các lệnh hay dùng

| Tình huống | Lệnh |
|---|---|
| Chạy lại bình thường | `mvn spring-boot:run` |
| Sau khi pull code mới (code thay đổi) | `mvn spring-boot:run` |
| Sau khi pull code mới (pom.xml thay đổi) | `mvn clean install -DskipTests` → `mvn spring-boot:run` |
| Máy mới, lần đầu setup | `mvn clean install -DskipTests` → `mvn spring-boot:run` |

---

## Cài đặt Frontend (Android)

### Yêu cầu

- **Android Studio:** Ladybug (2024.2.1) trở lên
- **Android SDK:** API 36 (Android 16)
- **Gradle JDK:** Java 17 hoặc 21
- **Thiết bị:** Android 7.0+ (API 24+) hoặc Android Emulator

### Các bước

1. Mở **Android Studio**
2. Chọn **Open** → chọn thư mục `english-learning-app-mobile/app`
3. Đợi **Gradle Sync** hoàn tất (có thể mất vài phút lần đầu)
4. Cấu hình địa chỉ API trong `ApiClient.java`:
   ```java
   private static final String BASE_URL = "http://10.0.2.2:8080/"; // Emulator
   // hoặc
   private static final String BASE_URL = "http://192.168.x.x:8080/"; // Thiết bị thật (IP máy tính)
   ```
5. Nhấn **Run** (▶) để build và chạy

> **Lưu ý:** Emulator Android dùng `10.0.2.2` để trỏ về `localhost` của máy tính. Thiết bị thật cần dùng IP LAN của máy tính và phải kết nối cùng WiFi.

---

## Kiến trúc hệ thống

```
┌─────────────────────────────────────────────────────────┐
│                    Android App (Client)                  │
│                                                         │
│  Activities ──► ApiService (Retrofit) ──► OkHttp        │
│                        │                                │
│                  AuthInterceptor (tự gắn JWT)            │
└────────────────────────┼────────────────────────────────┘
                         │ HTTPS REST API
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Spring Boot Backend (Server)               │
│                                                         │
│  Controller ──► Service ──► Repository ──► MySQL DB     │
│       │                                                 │
│  SecurityConfig (JWT Filter)                            │
│  GlobalExceptionHandler                                 │
└─────────────────────────────────────────────────────────┘
                         │
                    ┌────┴────┐
                    │  MySQL  │
                    │  DB     │
                    └─────────┘
```

### Luồng xác thực (Authentication Flow)

```
1. Client gửi POST /auth/login với email + password
2. Backend xác thực, tạo JWT access token + refresh token
3. Client lưu token vào SharedPreferences (qua TokenManager)
4. Mọi request tiếp theo: AuthInterceptor tự gắn "Authorization: Bearer <token>"
5. Khi logout: Client gửi POST /auth/logout, backend blacklist token vào bảng InvalidatedToken
```

---

## Các tính năng chính

### Đã hoàn thiện

- [x] Đăng ký / Đăng nhập / Đăng xuất (JWT)
- [ ] Tạo, sửa, xóa, xem bộ Flashcard
- [ ] Thêm, sửa, xóa Flashcard
- [ ] Bình luận trên bộ Flashcard
- [ ] Phân quyền Public/Private cho bộ Flashcard

### Đang phát triển

- [ ] Chế độ học: Flashcard (lật thẻ)
- [ ] Chế độ học: Word Quiz (trắc nghiệm)
- [ ] Chế độ học: Match the Pair (nối từ)
- [ ] Chế độ học: Spelling (điền từ)
- [ ] Spaced Repetition (SM-2)
- [ ] Thống kê học tập (6 chỉ số)
- [ ] Lớp học (tạo, mời, duyệt thành viên)
- [ ] Thông báo
- [ ] Tính năng AI (Gemini API)

---

## API Endpoints

Sau khi chạy backend, xem đầy đủ tại Swagger: `http://localhost:8080/swagger-ui/index.html`

### Authentication

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/auth/register` | Đăng ký tài khoản |
| POST | `/auth/login` | Đăng nhập, nhận JWT |
| POST | `/auth/logout` | Đăng xuất, blacklist token |
| POST | `/auth/refresh` | Làm mới access token |
| POST | `/auth/introspect` | Kiểm tra token còn hạn không |

### Flashcard Set

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/flashcard-sets` | Lấy danh sách bộ của user |
| POST | `/flashcard-sets` | Tạo bộ mới |
| GET | `/flashcard-sets/{id}` | Xem chi tiết bộ |
| PUT | `/flashcard-sets/{id}` | Sửa bộ |
| DELETE | `/flashcard-sets/{id}` | Xóa bộ |

### Flashcard

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/flashcards?deckId={setId}` | Lấy danh sách thẻ trong bộ |
| POST | `/flashcards` | Thêm thẻ mới |
| PUT | `/flashcards/{id}` | Sửa thẻ |
| DELETE | `/flashcards/{id}` | Xóa thẻ |

### Comment

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/comments?setId={id}` | Lấy bình luận của bộ |
| POST | `/comments` | Thêm bình luận |
| DELETE | `/comments/{id}` | Xóa bình luận của mình |

---

## Cơ sở dữ liệu

Database gồm 10 bảng, chia 3 nhóm chức năng:

### Nhóm 1 — Người dùng & Flashcard

| Bảng | Mô tả |
|---|---|
| `User` | Tài khoản người dùng, streak học tập |
| `FlashCardSet` | Bộ flashcard (public/private) |
| `FlashCard` | Thẻ từ vựng (term, definition, IPA, audio, image) |
| `Comment` | Bình luận trên bộ flashcard |

### Nhóm 2 — Học tập & Thống kê

| Bảng | Mô tả |
|---|---|
| `StudyRecord` | Lịch sử học từng thẻ + thông số SM-2 (easeFactor, intervalDays, nextReviewAt) |
| `QuizSession` | Phiên làm bài (mode, correctCount, totalQuestions, startedAt, endedAt) |

### Nhóm 3 — Lớp học & Thông báo

| Bảng | Mô tả |
|---|---|
| `Class` | Thông tin lớp học, invite code |
| `ClassMember` | Thành viên lớp (role: admin/member, status: pending/approved) |
| `ClassFlashcardSet` | Bộ flashcard được chia sẻ vào lớp |
| `Notification` | Thông báo (polymorphic: idRef + refType) |

> **Lưu ý quan trọng:** Hibernate `ddl-auto: update` sẽ tự tạo và cập nhật bảng dựa theo các Entity class. Không cần chạy SQL thủ công.

---

## Phân công thành viên

| Thành viên           | Chức năng phụ trách                                                      |
|----------------------|--------------------------------------------------------------------------|
| Đặng Thị Huyền       | Authentication (đăng ký, đăng nhập, Google OAuth), Quản lý Flashcard set |
| Nguyễn Thị Khánh Vân | Quản lý Flashcard (thêm, sửa, xóa, import hàng loạt)                     |
| Trần Mai Hương       | Học tập (4 chế độ học, Spaced Repetition) , Thống kê                     |
| Lê Thị Hải Yến       | Lớp học (tạo lớp, quản lý thành viên, flashcard lớp), Thông báo          |
| Đào Thị Huyền        | Quản lý người dùng, Tính năng AI (Gemini, chatbot)                       |

---

## Hướng phát triển thêm

- Đồng bộ phiên bản Web
- Gamification (điểm thưởng, xếp hạng)
- Leaderboard trong lớp học
- Offline mode

---

## Tài liệu tham khảo

- [SRS Document](./docs/SRS.pdf) — Đặc tả yêu cầu phần mềm
- [Use Case Document](./docs/UseCaseDocument.pdf) — Tài liệu Use Case chi tiết
- [Database Design](./docs/DatabaseDesign.docx) — Thiết kế cơ sở dữ liệu
- [AI Flow](./docs/AIFlow.pdf) — Luồng xử lý tính năng AI

---

*EStudy PTIT — Nhóm 5, D22CNPM02 — Học kỳ 2, Năm học 2025–2026*
