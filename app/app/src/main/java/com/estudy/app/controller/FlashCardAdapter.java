package com.estudy.app.controller;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.estudy.app.R;
import com.estudy.app.model.response.FlashCardResponse;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class FlashCardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_VIEW = 0;
    private static final int TYPE_EDIT = 1;

    private android.speech.tts.TextToSpeech tts;

    public interface Listener {
        void onSave(CardData card, int position);
        void onDelete(CardData card, int position);
        void onSuggest(int position, String term, SuggestCallback cb);
        void onImagePick(int position);
        void onImageSuggest(int position, String term);
        void onImageDelete(int position);
    }

    public interface SuggestCallback {
        void onResult(String definition, String ipa, String example);
    }

    public static class CardData {
        public String  id;
        public String  term;
        public String  definition;
        public String  ipa;
        public String  example;
        public String  imageUrl;
        public boolean isNew;

        // --- THÊM 2 BIẾN NÀY ĐỂ LÀM CAROUSEL ---
        public List<String> suggestedImages; // Danh sách link ảnh gợi ý
        public int currentImgIndex = 0;      // Vị trí ảnh đang xem

        public CardData() { isNew = true; }

        public CardData(FlashCardResponse r) {
            id         = r.getId();
            term       = r.getTerm();
            definition = r.getDefinition();
            ipa        = r.getIpa();
            example    = r.getExample();
            imageUrl   = r.getImage();
            isNew      = false;
        }
    }

    private final Context        context;
    private final boolean        isEditMode;
    private final Listener       listener;
    private final List<FlashCardResponse> rawItems;
    private final List<CardData>          cardList;

    // SỬA LẠI Constructor VIEW mode (để nhận Context)
    public FlashCardAdapter(Context context, List<FlashCardResponse> items) {
        this.context    = context;
        this.isEditMode = false;
        this.listener   = null;
        this.rawItems   = items;
        this.cardList   = null;
        initTTS(); // Gọi khởi tạo âm thanh
    }

    // Constructor EDIT mode (Giữ nguyên, chỉ thêm initTTS vào dòng cuối)
    public FlashCardAdapter(Context context, List<CardData> cards, Listener listener, boolean isEditMode) {
        this.context    = context;
        this.isEditMode = isEditMode;
        this.listener   = listener;
        this.rawItems   = null;
        this.cardList   = cards;
        initTTS(); // Gọi khởi tạo âm thanh
    }

    // Hàm cài đặt giọng đọc Tiếng Anh
    private void initTTS() {
        if (context != null && tts == null) {
            tts = new android.speech.tts.TextToSpeech(context, status -> {
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    tts.setLanguage(java.util.Locale.US); // Cài đặt giọng Anh-Mỹ
                }
            });
        }
    }

    @Override public int getItemViewType(int p) { return isEditMode ? TYPE_EDIT : TYPE_VIEW; }

    @Override
    public int getItemCount() {
        return isEditMode ? (cardList != null ? cardList.size() : 0) : (rawItems != null ? rawItems.size() : 0);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_EDIT)
            return new EditVH(inf.inflate(R.layout.item_flashcard_edit, parent, false));
        return new ViewVH(inf.inflate(R.layout.item_flashcard, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
        if (holder instanceof EditVH) bindEdit((EditVH) holder, pos);
        else                          bindView((ViewVH) holder, pos);
    }

    private void bindView(ViewVH h, int pos) {
        FlashCardResponse item = rawItems.get(pos);
        h.tvTerm.setText(item.getTerm());

        // --- 1. SỬA LỖI 2 DẤU GẠCH CHÉO (IPA) ---
        String ipaText = item.getIpa();
        if (ipaText != null && !ipaText.trim().isEmpty()) {
            ipaText = ipaText.replace("/", ""); // Dọn sạch dấu / cũ
            h.tvIpa.setText("/" + ipaText + "/"); // Tự bọc lại 1 dấu chuẩn
        } else {
            h.tvIpa.setText("");
        }

        h.tvDefinition.setText(item.getDefinition() != null ? item.getDefinition() : "");

        // --- 2. SỬA LỖI CLICK DẤU 3 CHẤM ---
        if (h.btnDotMenu != null) {
            h.btnDotMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.getMenu().add(0, 0, 0, "Chỉnh sửa (Edit)");
                popup.getMenu().add(0, 1, 1, "Xóa (Delete)");

                popup.setOnMenuItemClickListener(menuItem -> {
                    Toast.makeText(v.getContext(), "Vui lòng nhấn biểu tượng Cài đặt trên góc phải để bật chế độ Chỉnh Sửa!", Toast.LENGTH_LONG).show();
                    return true;
                });
                popup.show();
            });
        }

        if (h.btnAudio != null) {
            h.btnAudio.setOnClickListener(v -> speakWord(item.getTerm()));
        }
    }

    private void bindEdit(EditVH h, int pos) {
        CardData card = cardList.get(pos);
        h.tvPosition.setText((pos + 1) + "/" + cardList.size());

        // 1. Gỡ bỏ theo dõi cũ (chống lỗi lặp vô tận khi cuộn màn hình)
        if (h.etTerm.getTag() instanceof android.text.TextWatcher)
            h.etTerm.removeTextChangedListener((android.text.TextWatcher) h.etTerm.getTag());
        if (h.etDefinition.getTag() instanceof android.text.TextWatcher)
            h.etDefinition.removeTextChangedListener((android.text.TextWatcher) h.etDefinition.getTag());
        if (h.etExample.getTag() instanceof android.text.TextWatcher)
            h.etExample.removeTextChangedListener((android.text.TextWatcher) h.etExample.getTag());

        // 2. Set chữ hiện tại lên màn hình
        setET(h.etTerm, card.term);
        setET(h.etDefinition, card.definition);
        setET(h.etExample, card.example);

        // 3. TUYỆT CHIÊU: Lưu chữ ngay lập tức mỗi khi bạn gõ phím
        android.text.TextWatcher termWatcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) { card.term = s.toString(); }
        };
        h.etTerm.addTextChangedListener(termWatcher);
        h.etTerm.setTag(termWatcher);

        android.text.TextWatcher defWatcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) { card.definition = s.toString(); }
        };
        h.etDefinition.addTextChangedListener(defWatcher);
        h.etDefinition.setTag(defWatcher);

        android.text.TextWatcher exWatcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) { card.example = s.toString(); }
        };
        h.etExample.addTextChangedListener(exWatcher);
        h.etExample.setTag(exWatcher);

        refreshImage(h, card);

        h.btnDeleteCard.setOnClickListener(v -> {
            int p = h.getAdapterPosition();
            if (p != RecyclerView.NO_ID && listener != null) listener.onDelete(cardList.get(p), p);
        });

        h.btnSaveCard.setOnClickListener(v -> {
            int p = h.getAdapterPosition();
            if (p == RecyclerView.NO_ID) return;
            CardData c = cardList.get(p);
            c.term       = h.etTerm.getText().toString().trim();
            c.definition = h.etDefinition.getText().toString().trim();
            c.example    = h.etExample.getText().toString().trim();
            if (TextUtils.isEmpty(c.term)) {
                h.etTerm.setError("Term required");
                h.etTerm.requestFocus();
                return;
            }
            if (listener != null) listener.onSave(c, p);
        });

        h.btnSuggest.setOnClickListener(v -> {
            int p = h.getAdapterPosition();
            if (p == RecyclerView.NO_ID) return;

            // Lấy chữ đang gõ
            String term = h.etTerm.getText().toString().trim();
            if (TextUtils.isEmpty(term)) {
                Toast.makeText(context, "Enter a term first", Toast.LENGTH_SHORT).show();
                return;
            }

            if (listener != null) listener.onSuggest(p, term, (def, ipa, ex) -> {
                // 1. Nhét dữ liệu API trả về vào bộ nhớ (cardList)
                cardList.get(p).definition = def;
                cardList.get(p).ipa        = ipa;
                cardList.get(p).example    = ex;

                // 2. ÉP ANDROID VẼ LẠI TOÀN BỘ THẺ NÀY ĐỂ HIỆN CHỮ LÊN MÀN HÌNH
                notifyItemChanged(p);
            });
        });

        // Nút Gợi ý ảnh
        h.btnAddImage.setOnClickListener(v -> {
            int p = h.getAdapterPosition();
            if (p == RecyclerView.NO_ID) return;
            String term = h.etTerm.getText().toString().trim();
            if (TextUtils.isEmpty(term)) {
                Toast.makeText(context, "Enter a term to search image", Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) listener.onImageSuggest(p, term);
        });

        // Nút Tải ảnh lên (cả lúc mới và lúc đang xem ảnh)
        View.OnClickListener uploadListener = v -> {
            int p = h.getAdapterPosition();
            if (p != RecyclerView.NO_ID && listener != null) listener.onImagePick(p);
        };
        h.btnUploadImage.setOnClickListener(uploadListener);
        h.btnReUploadImage.setOnClickListener(uploadListener);

        // Nút Xóa ảnh
        h.btnDeleteImage.setOnClickListener(v -> {
            int p = h.getAdapterPosition();
            if (p == RecyclerView.NO_ID) return;
            cardList.get(p).imageUrl = null;
            refreshImage(h, cardList.get(p));
            if (listener != null) listener.onImageDelete(p);
        });

        // 2 Mũi tên (Tạm thời khóa vì 1 card mới có 1 ảnh)
        // Mũi tên TRÁI
        h.btnPrevImage.setOnClickListener(v -> {
            int p = h.getAdapterPosition();
            if (p == RecyclerView.NO_ID) return;
            CardData c = cardList.get(p);
            if (c.suggestedImages != null && c.suggestedImages.size() > 1) {
                c.currentImgIndex--;
                if (c.currentImgIndex < 0) c.currentImgIndex = c.suggestedImages.size() - 1;
                c.imageUrl = c.suggestedImages.get(c.currentImgIndex);
                refreshImage(h, c); // Cập nhật lại ảnh lên giao diện
            }
        });

        // Mũi tên PHẢI
        h.btnNextImage.setOnClickListener(v -> {
            int p = h.getAdapterPosition();
            if (p == RecyclerView.NO_ID) return;
            CardData c = cardList.get(p);
            if (c.suggestedImages != null && c.suggestedImages.size() > 1) {
                c.currentImgIndex++;
                if (c.currentImgIndex >= c.suggestedImages.size()) c.currentImgIndex = 0;
                c.imageUrl = c.suggestedImages.get(c.currentImgIndex);
                refreshImage(h, c); // Cập nhật lại ảnh lên giao diện
            }
        });

        if (h.btnAudioEdit != null) {
            h.btnAudioEdit.setOnClickListener(v -> {
                // Lấy chữ đang gõ trong ô Term ra đọc
                String currentWord = h.etTerm.getText().toString();
                speakWord(currentWord);
            });
        }
    }

    public void updateCardImage(int position, String urlOrBase64) {
        if (cardList == null || position < 0 || position >= cardList.size()) return;
        cardList.get(position).imageUrl = urlOrBase64;
        notifyItemChanged(position);
    }

    private void refreshImage(EditVH h, CardData card) {
        if (!TextUtils.isEmpty(card.imageUrl)) {
            h.layoutImagePreview.setVisibility(View.VISIBLE);
            h.layoutImageButtons.setVisibility(View.GONE);

            // TỰ ĐỘNG ẨN MŨI TÊN NẾU CHỈ CÓ 1 ẢNH HOẶC KHÔNG PHẢI CHẾ ĐỘ CAROUSEL
            boolean hasMultipleImages = card.suggestedImages != null && card.suggestedImages.size() > 1;
            h.btnPrevImage.setVisibility(hasMultipleImages ? View.VISIBLE : View.INVISIBLE);
            h.btnNextImage.setVisibility(hasMultipleImages ? View.VISIBLE : View.INVISIBLE);

            if (context != null) {
                if (card.imageUrl.startsWith("data:image")) {
                    try {
                        // Tách chuỗi Base64 an toàn hơn
                        String base64Data = card.imageUrl.substring(card.imageUrl.indexOf(",") + 1);
                        byte[] imageBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                        Glide.with(context)
                                .load(imageBytes) // Glide tự hiểu mảng byte, không cần asBitmap()
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .into(h.ivCardImage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Glide.with(context)
                            .load(card.imageUrl)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_report_image)
                            .into(h.ivCardImage);
                }
            }
        } else {
            h.layoutImagePreview.setVisibility(View.GONE);
            h.layoutImageButtons.setVisibility(View.VISIBLE);
        }
    }

    private void setET(EditText et, String val) {
        String next = val != null ? val : "";
        if (!et.getText().toString().equals(next)) et.setText(next);
    }

    public void setSuggestedImages(int position, List<String> images) {
        if (cardList == null || position < 0 || position >= cardList.size()) return;
        if (images != null && !images.isEmpty()) {
            cardList.get(position).suggestedImages = images;
            cardList.get(position).currentImgIndex = 0;
            cardList.get(position).imageUrl = images.get(0); // Hiển thị ảnh đầu tiên
            notifyItemChanged(position);
        }
    }

    static class ViewVH extends RecyclerView.ViewHolder {
        TextView tvTerm, tvIpa, tvDefinition;
        ImageButton btnDotMenu, btnAudio; // Đã sửa thành ImageButton btnDotMenu

        ViewVH(View v) {
            super(v);
            tvTerm       = v.findViewById(R.id.tvTerm);
            tvIpa        = v.findViewById(R.id.tvIpa);
            tvDefinition = v.findViewById(R.id.tvDefinition);
            btnDotMenu   = v.findViewById(R.id.btnDotMenu); // Đã sửa tên biến khớp với XML
            btnAudio     = v.findViewById(R.id.btnAudio); // Nối với ID trong XML
        }
    }
    static class EditVH extends RecyclerView.ViewHolder {
        TextView       tvPosition;
        EditText       etTerm, etDefinition, etExample;
        LinearLayout   layoutImagePreview, layoutImageButtons;
        ImageView      ivCardImage;
        ImageButton    btnDeleteCard, btnSuggest, btnDeleteImage, btnAudioEdit;
        ImageButton    btnAddImage, btnUploadImage, btnReUploadImage;
        ImageButton    btnPrevImage, btnNextImage;
        MaterialButton btnSaveCard;

        EditVH(@NonNull View v) {
            super(v);
            tvPosition         = v.findViewById(R.id.tvPosition);
            etTerm             = v.findViewById(R.id.etTerm);
            etDefinition       = v.findViewById(R.id.etDefinition);
            etExample          = v.findViewById(R.id.etExample);
            layoutImagePreview = v.findViewById(R.id.layoutImagePreview);
            layoutImageButtons = v.findViewById(R.id.layoutImageButtons);
            ivCardImage        = v.findViewById(R.id.ivCardImage);
            btnDeleteCard      = v.findViewById(R.id.btnDeleteCard);
            btnSuggest         = v.findViewById(R.id.btnSuggest);
            btnAudioEdit       = v.findViewById(R.id.btnAudioEdit);
            btnSaveCard        = v.findViewById(R.id.btnSaveCard);
            btnAddImage        = v.findViewById(R.id.btnAddImage);
            btnUploadImage     = v.findViewById(R.id.btnUploadImage);
            btnReUploadImage   = v.findViewById(R.id.btnReUploadImage);
            btnDeleteImage     = v.findViewById(R.id.btnDeleteImage);
            btnPrevImage       = v.findViewById(R.id.btnPrevImage);
            btnNextImage       = v.findViewById(R.id.btnNextImage);
        }
    }

    public void addNewCard() {
        if (cardList == null) return;
        cardList.add(new CardData());
        notifyItemInserted(cardList.size() - 1);
    }

    public void removeCard(int pos) {
        if (cardList == null || pos < 0 || pos >= cardList.size()) return;
        cardList.remove(pos);
        notifyItemRemoved(pos);
        notifyItemRangeChanged(pos, cardList.size());
    }

    public void markSaved(int pos, String serverId) {
        if (cardList == null || pos < 0 || pos >= cardList.size()) return;
        cardList.get(pos).id    = serverId;
        cardList.get(pos).isNew = false;
        notifyItemChanged(pos);
    }

    // Hàm này giúp xử lý đọc an toàn cho cả chế độ Xem và Sửa
    private void speakWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            Toast.makeText(context, "Chưa có từ vựng để đọc!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (tts != null) {
            tts.speak(word, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            Toast.makeText(context, "Đang tải giọng đọc, vui lòng đợi 2 giây rồi bấm lại!", Toast.LENGTH_SHORT).show();
        }
    }
}