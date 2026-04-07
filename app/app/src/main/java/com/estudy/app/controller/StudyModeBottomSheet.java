package com.estudy.app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.estudy.app.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet chọn chế độ học (UC-STUDY-02~05).
 * Gọi từ StudyTodayActivity:
 *   StudyModeBottomSheet.newInstance(setId, setName)
 *       .show(getSupportFragmentManager(), "StudyMode");
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

        // Subtitle: tên bộ
        TextView tvSetName = view.findViewById(R.id.tvSetName);
        if (tvSetName != null) tvSetName.setText(setName);

        optFlashcard = view.findViewById(R.id.optFlashcard);
        optWordQuiz  = view.findViewById(R.id.optWordQuiz);
        optMatch     = view.findViewById(R.id.optMatch);
        optSpelling  = view.findViewById(R.id.optSpelling);
        Button    btnStart = view.findViewById(R.id.btnStart);
        ImageButton btnClose = view.findViewById(R.id.btnClose);

        // Mặc định chọn Flashcard
        selectMode("flashcard");

        optFlashcard.setOnClickListener(v -> selectMode("flashcard"));
        optWordQuiz.setOnClickListener(v  -> selectMode("word_quiz"));
        optMatch.setOnClickListener(v     -> selectMode("match"));
        optSpelling.setOnClickListener(v  -> selectMode("spelling"));

        if (btnStart != null) btnStart.setOnClickListener(v -> startStudy());
        if (btnClose != null) btnClose.setOnClickListener(v -> dismiss());
    }

    private void selectMode(String mode) {
        selectedMode = mode;
        setModeBackground(optFlashcard, "flashcard".equals(mode));
        setModeBackground(optWordQuiz,  "word_quiz".equals(mode));
        setModeBackground(optMatch,     "match".equals(mode));
        setModeBackground(optSpelling,  "spelling".equals(mode));
    }

    private void setModeBackground(LinearLayout view, boolean selected) {
        if (view == null) return;
        if (selected) {
            view.setBackgroundResource(R.drawable.bg_mode_selected);
            TextView tv = (TextView) view.getChildAt(0);
            if (tv != null) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            view.setBackgroundResource(android.R.color.transparent);
            TextView tv = (TextView) view.getChildAt(0);
            if (tv != null) tv.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void startStudy() {
        if (setId == null || setId.isEmpty()) {
            Toast.makeText(getContext(),
                    "Vui lòng chọn một bộ từ vựng", Toast.LENGTH_SHORT).show();
            return;
        }
        dismiss();

        Intent intent;
        switch (selectedMode) {
            case "word_quiz":
                intent = new Intent(getActivity(), WordQuizActivity.class);
                intent.putExtra(WordQuizActivity.EXTRA_SET_ID,   setId);
                intent.putExtra(WordQuizActivity.EXTRA_SET_NAME, setName);
                break;
            case "match":
                intent = new Intent(getActivity(), MatchPairActivity.class);
                intent.putExtra(MatchPairActivity.EXTRA_SET_ID,   setId);
                intent.putExtra(MatchPairActivity.EXTRA_SET_NAME, setName);
                break;
            case "spelling":
                intent = new Intent(getActivity(), SpellingActivity.class);
                intent.putExtra(SpellingActivity.EXTRA_SET_ID,   setId);
                intent.putExtra(SpellingActivity.EXTRA_SET_NAME, setName);
                break;
            default: // flashcard
                intent = new Intent(getActivity(), FlashcardStudyActivity.class);
                intent.putExtra(FlashcardStudyActivity.EXTRA_SET_ID,   setId);
                intent.putExtra(FlashcardStudyActivity.EXTRA_SET_NAME, setName);
                break;
        }
        startActivity(intent);
    }
}
