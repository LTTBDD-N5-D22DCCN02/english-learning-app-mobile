# english-learning-app-mobile

# English Learning App - Project Documentation

Dự án bao gồm hệ thống Backend (Spring Boot) và ứng dụng di động (Android). Dưới đây là hướng dẫn chi tiết để thiết lập và chạy dự án.

---

## 1. Cài đặt Backend

### Yêu cầu
* **IDE:** IntelliJ IDEA hoặc Visual Studio Code.
* **Java:** JDK 17+ (Khuyến nghị).
* **Build Tool:** Maven.

### Các bước thực hiện
1. **Truy cập thư mục backend:**
   ```bash
   cd backend
   ```

2. **Cấu hình File Application:**

* Tạo file mới tên là application.yml trong thư mục src/main/resources/.

* Copy toàn bộ nội dung từ file application-pro.yml sang application.yml.

3. **Chỉnh sửa thông tin cấu hình:**

* Database: Cập nhật url, username, và password phù hợp với máy cục bộ của bạn.

* JWT Signer Key: Truy cập [JWT Secret Key Generator](https://jwtsecretkeygenerator.com/), chọn loại Standard Key 512-bit, tạo key và dán vào mục signerKey trong file yml.

4. **Build và Chạy dự án:**

* Dọn dẹp và cài đặt các dependency:
```Bash
mvn clean install
Khởi chạy ứng dụng:
```

```Bash
mvn spring-boot:run
```
## 2. Cài đặt Frontend (Mobile)
Ứng dụng Android dành cho người dùng cuối.

Yêu cầu: (Kiểm tra lại các phiên bản nếu khác)
* **Android Studio:** Phiên bản **Ladybug (2024.2.1)** hoặc mới hơn (để hỗ trợ AGP 8.13.2).
* **Android SDK:** API **36** (Android 16)
* **Gradle JDK:** Java 17/21.


Các bước thực hiện trên Android Studio.

* Chọn Open thư mục
english-learning-app-mobile/app
* Đợi Gradle đồng bộ (Sync) 
* Nhấn nút Run (biểu tượng Play) để khởi động ứng dụng trên thiết bị.