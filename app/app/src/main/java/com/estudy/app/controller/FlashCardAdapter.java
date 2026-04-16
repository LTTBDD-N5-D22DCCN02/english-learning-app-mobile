package com.estudy.app.controller;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.estudy.app.R;
import com.estudy.app.model.response.FlashCardResponse;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class FlashCardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_VIEW = 0;
    private static final int TYPE_EDIT = 1;

    private TextToSpeech tts;

    // ── Interfaces ────────────────────────────────────────────────────────────

    public interface Listener {
        void onSave(CardData card, int position);
        void onDelete(CardData card, int position);
        /**
         * Multi-meaning: Activity gọi API → mở SuggestDialog.
         * Không dùng SuggestCallback nữa.
         */
        void onSuggest(int position, String term);
        void onImagePick(int position);
        void onImageSuggest(int position, String term);
        void onImageDelete(int position);
    }

    // ── CardData ──────────────────────────────────────────────────────────────

    public static class CardData {
        public String  id;
        public String  term;
        public String  definition;
        public String  ipa;
        public String  example;
        public String  imageUrl;
        public boolean isNew;
        public List<String> suggestedImages;
        public int currentImgIndex = 0;

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

    // ── Fields ────────────────────────────────────────────────────────────────

    private final Context        context;
    private final boolean        isEditMode;
    private final Listener       listener;
    private final List<FlashCardResponse> rawItems;
    private final List<CardData>          cardList;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Constructor VIEW mode — nhận Context để dùng TTS */
    public FlashCardAdapter(Context context, List<FlashCardResponse> items, Listener listener) {
        this.context    = context;
        this.isEditMode = false;
        this.listener  = listener;
        this.rawItems   = items;
        this.cardList   = null;
        initTTS();
    }

    /** Constructor EDIT mode */
    public FlashCardAdapter(Context context, List<CardData> cards,
                            Listener listener, boolean isEditMode) {
        this.context    = context;
        this.isEditMode = isEditMode;
        this.listener   = listener;
        this.rawItems   = null;
        this.cardList   = cards;
        initTTS();
    }

    private void initTTS() {
        if (context == null || tts != null) return;
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS)
                tts.setLanguage(Locale.US);
        });
    }

    private void speakWord(String word) {
        if (TextUtils.isEmpty(word)) {
            Toast.makeText(context, "Chưa có từ để đọc!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (tts != null) {
            tts.speak(word.trim(), TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            Toast.makeText(context, "Đang tải giọng đọc, thử lại sau!", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Core ──────────────────────────────────────────────────────────────────

    @Override public int getItemViewType(int p) { return isEditMode ? TYPE_EDIT : TYPE_VIEW; }

    @Override
    public int getItemCount() {
        return isEditMode
                ? (cardList != null ? cardList.size() : 0)
                : (rawItems != null ? rawItems.size() : 0);
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

    // ── VIEW mode ─────────────────────────────────────────────────────────────

    private void bindView(ViewVH h, int pos) {
        FlashCardResponse item = rawItems.get(pos);
        h.tvTerm.setText(item.getTerm());

        // IPA — tránh double slash
        String ipa = item.getIpa();
        if (ipa != null && !ipa.trim().isEmpty()) {
            h.tvIpa.setText("/" + ipa.replace("/", "").trim() + "/");
        } else {
            h.tvIpa.setText("");
        }

        h.tvDefinition.setText(item.getDefinition() != null ? item.getDefinition() : "");

        // 3-dot menu — thông báo chuyển edit mode
        // --- SỬA LỖI CLICK DẤU 3 CHẤM (CHỈ CHO XÓA) ---
        if (h.btnDotMenu != null) {
            h.btnDotMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, v);
                // Chỉ add 1 nút duy nhất là Xóa
                popup.getMenu().add(0, 0, 0, "Delete");

                popup.setOnMenuItemClickListener(menuItem -> {
                    if (listener != null) {
                        // Ép kiểu thẻ hiện tại sang CardData để truyền vào hàm Delete của Activity
                        CardData cardToDelete = new CardData(rawItems.get(pos));
                        listener.onDelete(cardToDelete, pos);
                    }
                    return true;
                });
                popup.show();
            });
        }

        // Audio button (view mode)
        if (h.btnAudio != null)
            h.btnAudio.setOnClickListener(v -> speakWord(item.getTerm()));
    }

    // ── EDIT mode ─────────────────────────────────────────────────────────────

    private void bindEdit(EditVH h, int pos) {
        CardData card = cardList.get(pos);
        h.tvPosition.setText((pos + 1) + "/" + cardList.size());

        // Remove old TextWatchers trước khi setText (chống lặp vô tận)
        removeWatcher(h.etTerm);
        removeWatcher(h.etDefinition);
        removeWatcher(h.etExample);

        setET(h.etTerm,       card.term);
        setET(h.etDefinition, card.definition);
        setET(h.etExample,    card.example);

        // TextWatchers — lưu dữ liệu realtime khi gõ phím
        attachWatcher(h.etTerm,       s -> card.term       = s);
        attachWatcher(h.etDefinition, s -> card.definition = s);
        attachWatcher(h.etExample,    s -> card.example    = s);

        refreshImage(h, card);

        // Delete card
        h.btnDeleteCard.setOnClickListener(v -> {
            int p = h.getAdapterPosition();
            if (p != RecyclerView.NO_ID && listener != null)
                listener.onDelete(cardList.get(p), p);
        });

        // Save card
        h.btnSaveCard.setOnClickListener(v -> {
            int p = h.getAdapterPosition();
            if (p == RecyclerView.NO_ID) return;
            CardData c = cardList.get(p);
            if (TextUtils.isEmpty(c.term)) {
                h.etTerm.setError("Term required");
                h.etTerm.requestFocus();
                return;
            }
            if (listener != null) listener.onSave(c, p);
        });

        // Suggest definition — Activity xử lý, mở SuggestDialog (multi-meaning)
        h.btnSuggest.setOnClickListener(v -> {
            int p = h.getAdapterPosition();
            if (p == RecyclerView.NO_ID) return;
            String term = card.term != null ? card.term.trim() : "";
            if (TextUtils.isEmpty(term)) {
                Toast.makeText(context, "Nhập từ trước khi tra nghĩa", Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) listener.onSuggest(p, term);
        });

        // Audio (edit mode) — đọc term đang gõ
        if (h.btnAudioEdit != null)
            h.btnAudioEdit.setOnClickListener(v -> speakWord(h.etTerm.getText().toString()));

        // Suggest image
        h.btnAddImage.setOnClickListener(v -> {
            int p = h.getAdapterPosition();
            if (p == RecyclerView.NO_ID) return;
            String term = card.term != null ? card.term.trim() : "";
            if (TextUtils.isEmpty(term)) {
                Toast.makeText(context, "Nhập từ trước khi tìm ảnh", Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) listener.onImageSuggest(p, term);
        });

        // Upload image
        View.OnClickListener uploadClick = v -> {
            int p = h.getAdapterPosition();
            if (p != RecyclerView.NO_ID && listener != null) listener.onImagePick(p);
        };
        h.btnUploadImage.setOnClickListener(uploadClick);
        if (h.btnReUploadImage != null) h.btnReUploadImage.setOnClickListener(uploadClick);

        // Delete image
        h.btnDeleteImage.setOnClickListener(v -> {
            int p = h.getAdapterPosition();
            if (p == RecyclerView.NO_ID) return;
            CardData c = cardList.get(p);
            c.imageUrl = null;
            c.suggestedImages = null;
            refreshImage(h, c);
            if (listener != null) listener.onImageDelete(p);
        });

        // Carousel prev
        h.btnPrevImage.setOnClickListener(v -> {
            int p = h.getAdapterPosition();
            if (p == RecyclerView.NO_ID) return;
            CardData c = cardList.get(p);
            if (c.suggestedImages != null && c.suggestedImages.size() > 1) {
                c.currentImgIndex = (c.currentImgIndex - 1 + c.suggestedImages.size())
                        % c.suggestedImages.size();
                c.imageUrl = c.suggestedImages.get(c.currentImgIndex);
                refreshImage(h, c);
            }
        });

        // Carousel next
        h.btnNextImage.setOnClickListener(v -> {
            int p = h.getAdapterPosition();
            if (p == RecyclerView.NO_ID) return;
            CardData c = cardList.get(p);
            if (c.suggestedImages != null && c.suggestedImages.size() > 1) {
                c.currentImgIndex = (c.currentImgIndex + 1) % c.suggestedImages.size();
                c.imageUrl = c.suggestedImages.get(c.currentImgIndex);
                refreshImage(h, c);
            }
        });
    }

    // ── Image ─────────────────────────────────────────────────────────────────

    private void refreshImage(EditVH h, CardData card) {
        if (!TextUtils.isEmpty(card.imageUrl)) {
            h.layoutImagePreview.setVisibility(View.VISIBLE);
            h.layoutImageButtons.setVisibility(View.GONE);

            boolean multi = card.suggestedImages != null && card.suggestedImages.size() > 1;
            h.btnPrevImage.setVisibility(multi ? View.VISIBLE : View.INVISIBLE);
            h.btnNextImage.setVisibility(multi ? View.VISIBLE : View.INVISIBLE);

            if (context != null) {
                if (card.imageUrl.startsWith("data:image")) {
                    try {
                        String b64 = card.imageUrl.substring(card.imageUrl.indexOf(",") + 1);
                        byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
                        Glide.with(context).load(bytes)
                                .placeholder(android.R.drawable.ic_menu_gallery).into(h.ivCardImage);
                    } catch (Exception e) { e.printStackTrace(); }
                } else {
                    Glide.with(context).load(card.imageUrl)
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

    // ── TextWatcher helpers ───────────────────────────────────────────────────

    interface TextSetter { void set(String s); }

    private void attachWatcher(EditText et, TextSetter setter) {
        android.text.TextWatcher w = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) { setter.set(s.toString()); }
        };
        et.addTextChangedListener(w);
        et.setTag(w);
    }

    private void removeWatcher(EditText et) {
        if (et.getTag() instanceof android.text.TextWatcher)
            et.removeTextChangedListener((android.text.TextWatcher) et.getTag());
    }

    private void setET(EditText et, String val) {
        String next = val != null ? val : "";
        if (!et.getText().toString().equals(next)) et.setText(next);
    }

    // ── ViewHolders ───────────────────────────────────────────────────────────

    static class ViewVH extends RecyclerView.ViewHolder {
        TextView    tvTerm, tvIpa, tvDefinition;
        ImageButton btnDotMenu, btnAudio;

        ViewVH(View v) {
            super(v);
            tvTerm       = v.findViewById(R.id.tvTerm);
            tvIpa        = v.findViewById(R.id.tvIpa);
            tvDefinition = v.findViewById(R.id.tvDefinition);
            btnDotMenu   = v.findViewById(R.id.btnDotMenu);
            btnAudio     = v.findViewById(R.id.btnAudio);
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

    // ── Public helpers ────────────────────────────────────────────────────────

    public void addNewCard() {
        if (cardList == null) return;
        cardList.add(new CardData());
        notifyItemInserted(cardList.size() - 1);
    }

    public void removeCard(int pos) {
        try {
            if (isEditMode) {
                if (cardList == null || pos < 0 || pos >= cardList.size()) return;
                cardList.remove(pos);
                notifyItemRemoved(pos);
                // Đã trừ đi pos để tính đúng số lượng thẻ bị dịch chuyển
                notifyItemRangeChanged(pos, cardList.size() - pos);
            } else {
                if (rawItems == null || pos < 0 || pos >= rawItems.size()) return;
                rawItems.remove(pos);
                notifyItemRemoved(pos);
                // Đã trừ đi pos để tính đúng số lượng thẻ bị dịch chuyển
                notifyItemRangeChanged(pos, rawItems.size() - pos);
            }
        } catch (Exception e) {
            Toast.makeText(context, "Lỗi giao diện: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void markSaved(int pos, String serverId) {
        if (cardList == null || pos < 0 || pos >= cardList.size()) return;
        cardList.get(pos).id    = serverId;
        cardList.get(pos).isNew = false;
        notifyItemChanged(pos);
    }

    public void updateCardImage(int position, String url) {
        if (cardList == null || position < 0 || position >= cardList.size()) return;
        cardList.get(position).imageUrl = url;
        notifyItemChanged(position);
    }

    public void setSuggestedImages(int position, List<String> images) {
        if (cardList == null || position < 0 || position >= cardList.size()) return;
        if (images != null && !images.isEmpty()) {
            cardList.get(position).suggestedImages = images;
            cardList.get(position).currentImgIndex = 0;
            cardList.get(position).imageUrl = images.get(0);
            notifyItemChanged(position);
        }
    }

    /** Gọi từ Activity sau khi SuggestDialog trả kết quả (multi-meaning) */
    public void applySuggestion(int position, String definition, String ipa, String example) {
        if (cardList == null || position < 0 || position >= cardList.size()) return;
        CardData card   = cardList.get(position);
        card.definition = definition;
        card.ipa        = ipa;
        card.example    = example;
        notifyItemChanged(position);
    }

    public List<CardData> getCardList() { return cardList; }
}