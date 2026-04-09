package com.estudy.app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.estudy.app.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet dialog cho phép user chọn chế độ học.
 * Gọi từ StudyTodayActivity hoặc StudySetWordsActivity.
 */
public class StudyModeBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_SET_ID   = "setId";
    private static final String ARG_SET_NAME = "setName";

    private String setId, setName;
    private String selectedMode = "flashcard";

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

        if (getArguments() != null) {
            setId   = getArguments().getString(ARG_SET_ID,   "");
            setName = getArguments().getString(ARG_SET_NAME, "");
        }

        TextView tvSetName = view.findViewById(R.id.tvSetName);
        if (tvSetName != null) tvSetName.setText(setName);

        optFlashcard = view.findViewById(R.id.optFlashcard);
        optWordQuiz  = view.findViewById(R.id.optWordQuiz);
        optMatch     = view.findViewById(R.id.optMatch);
        optSpelling  = view.findViewById(R.id.optSpelling);
        Button    btnStart = view.findViewById(R.id.btnStart);
        ImageButton btnClose = view.findViewById(R.id.btnClose);

        selectMode("flashcard");

        optFlashcard.setOnClickListener(v -> selectMode("flashcard"));
        optWordQuiz.setOnClickListener(v  -> selectMode("word_quiz"));
        optMatch.setOnClickListener(v     -> selectMode("match"));
        optSpelling.setOnClickListener(v  -> selectMode("spelling"));

        btnStart.setOnClickListener(v -> startStudy());
        if (btnClose != null) btnClose.setOnClickListener(v -> dismiss());
    }

    private void selectMode(String mode) {
        selectedMode = mode;

        resetBackground(optFlashcard);
        resetBackground(optWordQuiz);
        resetBackground(optMatch);
        resetBackground(optSpelling);

        switch (mode) {
            case "flashcard": highlightMode(optFlashcard); break;
            case "word_quiz": highlightMode(optWordQuiz);  break;
            case "match":     highlightMode(optMatch);     break;
            case "spelling":  highlightMode(optSpelling);  break;
        }
    }

    private void highlightMode(LinearLayout view) {
        if (view == null) return;
        view.setBackgroundResource(R.drawable.bg_mode_selected);
        setChildTextBold(view, true);
    }

    private void resetBackground(LinearLayout view) {
        if (view == null) return;
        view.setBackgroundResource(android.R.color.transparent);
        setChildTextBold(view, false);
    }

    private void setChildTextBold(LinearLayout view, boolean bold) {
        if (view == null || view.getChildCount() == 0) return;
        View child = view.getChildAt(0);
        if (child instanceof TextView) {
            ((TextView) child).setTypeface(null,
                    bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    private void startStudy() {
        if (setId == null || setId.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng chọn một bộ flashcard cụ thể", Toast.LENGTH_SHORT).show();
            return;
        }
        dismiss();

        Intent intent;
        switch (selectedMode) {
            case "spelling":
                intent = new Intent(getActivity(), SpellingActivity.class);
                intent.putExtra(SpellingActivity.EXTRA_SET_ID, setId);
                intent.putExtra(SpellingActivity.EXTRA_SET_NAME, setName);
                startActivity(intent);
                break;
            case "match":
                intent = new Intent(getActivity(), MatchPairActivity.class);
                intent.putExtra(MatchPairActivity.EXTRA_SET_ID, setId);
                intent.putExtra(MatchPairActivity.EXTRA_SET_NAME, setName);
                startActivity(intent);
                break;
            case "word_quiz":
                intent = new Intent(getActivity(), WordQuizActivity.class);
                intent.putExtra(WordQuizActivity.EXTRA_SET_ID, setId);
                intent.putExtra(WordQuizActivity.EXTRA_SET_NAME, setName);
                startActivity(intent);
                break;
            case "flashcard":
            default:
                intent = new Intent(getActivity(), FlashcardStudyActivity.class);
                intent.putExtra(FlashcardStudyActivity.EXTRA_SET_ID, setId);
                intent.putExtra(FlashcardStudyActivity.EXTRA_SET_NAME, setName);
                startActivity(intent);
                break;
        }
    }
}