# Ghi chú tích hợp AI — Phần liên quan đến flow của Đào Huyền

> File này tổng hợp những điểm trong phần UC-Study & UC-Statistic của Mai Hương
> cần dùng hoặc có thể dùng tính năng AI từ phần của Đào Huyền.

---

## 1. TÓM TẮT FLOW AI CỦA HUYỀN

Theo use case document, Huyền phụ trách:

| Tính năng | Công nghệ |
|-----------|-----------|
| Sửa lỗi ngữ pháp/chính tả trong định nghĩa từ | Gemini API |
| Embedding từ vựng để tìm kiếm ngữ nghĩa | bge-m3 model |
| Auto-generate câu hỏi / distractor cho quiz | Gemini API |
| Chat hỏi đáp ngữ pháp (Grammar Chatbot) | Gemini API |
| Phát âm (Text-To-Speech) từ tiếng Anh | TTS API (Google / Android TTS) |

---

## 2. CÁC ĐIỂM GIAO NHAU VỚI PHẦN CỦA MAI HƯƠNG

### 2.1 🔊 Phát âm (Audio) trong FlashcardStudyActivity
**File:** `controller/FlashcardStudyActivity.java`
**Vấn đề:** Layout `activity_flashcard_study.xml` có nút `btnPlayAudio`, nhưng hiện tại chưa có logic xử lý.
**Cần từ Huyền:** Endpoint TTS hoặc URL audio từ backend.

**Ghi chú:** `SessionCardResponse` đã có trường `audioUrl` (String). Nếu backend trả về URL hợp lệ, có thể phát bằng `MediaPlayer` mà **không cần Gemini**.
Nếu backend chưa có audio, cần dùng Android `TextToSpeech` hoặc gọi API TTS từ flow của Huyền.

---

### 2.2 🔊 Phát âm trong SpellingActivity
**File:** `controller/SpellingActivity.java`
**Vấn đề:** Hiện chỉ hiển thị IPA bằng text (`tvIPA`). Không có nút nghe phát âm.
**Có thể bổ sung:** Nút nhỏ "🔊 Nghe" kế bên IPA để user nghe phát âm trước khi gõ.
**Cần từ Huyền:** Xác nhận API endpoint TTS hoặc cách gọi.

---

### 2.3 🤖 Distractor generation trong WordQuizActivity
**File:** `controller/WordQuizActivity.java`
**Hiện trạng:** Distractor đang lấy từ `card.getDistractors()` — là danh sách do backend trả về trong `StartSessionResponse`.
**Liên quan đến Huyền:** Nếu backend dùng Gemini để auto-generate distractor "thông minh" (ngữ nghĩa tương tự), phần quiz của Hương sẽ được hưởng lợi tự động — **không cần thay đổi code phía Android**.
**Hành động:** Chỉ cần confirm với Huyền rằng backend endpoint `POST /study/session/start` đã trả về `distractors` đúng format `List<String>`.

---

### 2.4 ✏️ Kiểm tra chính tả trong SpellingActivity
**File:** `controller/SpellingActivity.java`
**Hiện trạng:** Đang so sánh `etAnswer.getText().trim().equalsIgnoreCase(correctTerm)`.
**Có thể cải thiện với AI:** Nếu Huyền có API kiểm tra lỗi chính tả, có thể gọi để nhắc user "Bạn viết gần đúng rồi, thiếu chữ X" thay vì chỉ báo sai/đúng.
**Mức độ ưu tiên:** Thấp — tính năng hiện tại đã đủ cho MVP.

---

### 2.5 📊 Không liên quan trực tiếp — Grammar Chatbot
Flow chatbot ngữ pháp của Huyền là một màn hình riêng biệt, **không ảnh hưởng** đến StudyTodayActivity hay các activity học từ vựng. Không cần tích hợp.

---

## 3. PHẦN DỄ LÀM NGAY — HƯỚNG DẪN CHI TIẾT

### ✅ Tích hợp phát âm bằng Android TextToSpeech (KHÔNG cần API của Huyền)

Đây là cách đơn giản nhất, dùng thư viện có sẵn của Android:

**Bước 1:** Thêm import vào `FlashcardStudyActivity.java` và `SpellingActivity.java`:
```java
import android.speech.tts.TextToSpeech;
import java.util.Locale;
```

**Bước 2:** Khai báo biến trong class:
```java
private TextToSpeech tts;
```

**Bước 3:** Khởi tạo trong `onCreate()`:
```java
tts = new TextToSpeech(this, status -> {
    if (status == TextToSpeech.SUCCESS) {
        tts.setLanguage(Locale.US);
    }
});
```

**Bước 4:** Gọi khi nhấn nút phát âm:
```java
btnPlayAudio.setOnClickListener(v -> {
    String term = cards.get(currentIndex).getTerm();
    if (tts != null && !TextUtils.isEmpty(term)) {
        tts.speak(term, TextToSpeech.QUEUE_FLUSH, null, null);
    }
});
```

**Bước 5:** Giải phóng trong `onDestroy()`:
```java
@Override
protected void onDestroy() {
    if (tts != null) {
        tts.stop();
        tts.shutdown();
    }
    super.onDestroy();
}
```

> **Lưu ý:** Android TTS hoạt động offline, không cần internet, không cần API key.
> Chất lượng phát âm tốt cho tiếng Anh (Locale.US).

---

### ✅ Phát audio từ URL nếu backend đã có sẵn

Nếu `card.getAudioUrl()` không null/rỗng, ưu tiên dùng URL thật thay vì TTS:

```java
private void playAudio(SessionCardResponse card) {
    String url = card.getAudioUrl();
    if (url != null && !url.isEmpty()) {
        // Phát từ URL (forvo.com, backend tự host, v.v.)
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setDataSource(url);
            mp.setOnPreparedListener(MediaPlayer::start);
            mp.setOnCompletionListener(MediaPlayer::release);
            mp.prepareAsync();
        } catch (Exception e) {
            // Fallback sang TTS nếu URL lỗi
            speakWithTTS(card.getTerm());
        }
    } else {
        speakWithTTS(card.getTerm());
    }
}
```

> Thêm `import android.media.MediaPlayer;` vào đầu file.

---

## 4. CHECKLIST PHỐI HỢP VỚI HUYỀN

| Việc cần làm | Ai | Trạng thái |
|---|---|---|
| Confirm API endpoint `/study/session/start` trả về `distractors` | Huyền (backend) | ❓ Cần xác nhận |
| Confirm `audioUrl` trong response là URL có thể stream không | Huyền (backend) | ❓ Cần xác nhận |
| Tích hợp Android TTS vào `FlashcardStudyActivity` | Mai Hương | ⬜ Chưa làm |
| Tích hợp Android TTS vào `SpellingActivity` | Mai Hương | ⬜ Chưa làm |
| API TTS từ flow AI nếu muốn giọng đọc tốt hơn | Huyền (backend) | ❓ Tùy chọn |

---

## 5. KẾT LUẬN

Phần UC-Study của Mai Hương **hoạt động hoàn toàn độc lập** với AI flow của Huyền cho MVP.
Điểm giao nhau duy nhất quan trọng là **phát âm** (audio/TTS) — và có thể giải quyết ngay bằng Android TTS mà không cần đợi backend của Huyền.

Sau khi Huyền hoàn thiện backend, chỉ cần:
1. Kiểm tra `audioUrl` trong response — nếu có thì dùng `MediaPlayer`.
2. Nếu backend expose TTS API riêng → thay `MediaPlayer` bằng `Retrofit` call, không ảnh hưởng logic còn lại.
