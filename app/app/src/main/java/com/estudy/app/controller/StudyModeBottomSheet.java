package com.estudy.app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.estudy.app.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet dialog cho phép user chọn chế độ học.
 * Thay thế StudyModeActivity — hiển thị đúng thiết kế Figma.
 *
 * Cách gọi từ StudyTodayActivity:
 *   StudyModeBottomSheet sheet = StudyModeBottomSheet.newInstance(setId, setName);
 *   sheet.show(getSupportFragmentManager(), "StudyMode");
 */
public class StudyModeBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_SET_ID   = "setId";
    private static final String ARG_SET_NAME = "setName";

    private String setId, setName;
    private String selectedMode = "flashcard"; // mặc định Flashcard

    // Views mode options
    private LinearLayout optFlashcard, optWordQuiz, optMatch, optSpelling;

    public static StudyModeBottomSheet newInstance(String setId, String setName) {
        StudyModeBottomSheet sheet = new StudyModeBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_SET_ID,   setId);
        args.putString(ARG_SET_NAME, setName);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_study_mode_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Lấy arguments
        if (getArguments() != null) {
            setId   = getArguments().getString(ARG_SET_ID,   "");
            setName = getArguments().getString(ARG_SET_NAME, "");
        }

        // Set subtitle
        TextView tvSetName = view.findViewById(R.id.tvSetName);
        if (tvSetName != null) tvSetName.setText(setName);

        // Views
        optFlashcard = view.findViewById(R.id.optFlashcard);
        optWordQuiz  = view.findViewById(R.id.optWordQuiz);
        optMatch     = view.findViewById(R.id.optMatch);
        optSpelling  = view.findViewById(R.id.optSpelling);
        Button btnStart  = view.findViewById(R.id.btnStart);
        ImageButton btnClose = view.findViewById(R.id.btnClose);

        // Mặc định chọn Flashcard
        selectMode("flashcard");

        // Click từng mode
        optFlashcard.setOnClickListener(v -> selectMode("flashcard"));
        optWordQuiz.setOnClickListener(v  -> selectMode("word_quiz"));
        optMatch.setOnClickListener(v     -> selectMode("match"));
        optSpelling.setOnClickListener(v  -> selectMode("spelling"));

        // Nút START
        btnStart.setOnClickListener(v -> startStudy());

        // Nút Close
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }
    }

    // ── Highlight mode đang chọn ────────────────────────────────
    private void selectMode(String mode) {
        selectedMode = mode;

        // Reset tất cả về background trong suốt
        setModeBackground(optFlashcard, false);
        setModeBackground(optWordQuiz,  false);
        setModeBackground(optMatch,     false);
        setModeBackground(optSpelling,  false);

        // Highlight cái được chọn
        switch (mode) {
            case "flashcard": setModeBackground(optFlashcard, true); break;
            case "word_quiz": setModeBackground(optWordQuiz,  true); break;
            case "match":     setModeBackground(optMatch,     true); break;
            case "spelling":  setModeBackground(optSpelling,  true); break;
        }
    }

    private void setModeBackground(LinearLayout view, boolean selected) {
        if (view == null) return;
        if (selected) {
            view.setBackgroundResource(R.drawable.bg_mode_selected);
            // Bold text
            TextView tv = (TextView) view.getChildAt(0);
            if (tv != null) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            view.setBackgroundResource(android.R.color.transparent);
            TextView tv = (TextView) view.getChildAt(0);
            if (tv != null) tv.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    // ── Start học với mode đã chọn ──────────────────────────────
    private void startStudy() {
        dismiss();

        Intent intent;
        switch (selectedMode) {
            case "word_quiz":
                // TODO: intent = new Intent(getActivity(), QuizActivity.class);
                Toast.makeText(getContext(), "Word Quiz — coming soon!", Toast.LENGTH_SHORT).show();
                return;
            case "match":
                // TODO: intent = new Intent(getActivity(), MatchActivity.class);
                Toast.makeText(getContext(), "Match — coming soon!", Toast.LENGTH_SHORT).show();
                return;
            case "spelling":
                // TODO: intent = new Intent(getActivity(), SpellingActivity.class);
                Toast.makeText(getContext(), "Spelling — coming soon!", Toast.LENGTH_SHORT).show();
                return;
            default: // flashcard
                // TODO: intent = new Intent(getActivity(), FlashcardStudyActivity.class);
                Toast.makeText(getContext(), "Flashcard — coming soon!", Toast.LENGTH_SHORT).show();
                return;
        }
    }
}