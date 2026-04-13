package com.estudy.app.controller;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.estudy.app.R;
import com.estudy.app.model.response.SuggestResponse;

import java.util.List;

/**
 * Bottom sheet hiển thị danh sách nghĩa gợi ý.
 * User chọn 1 nghĩa → callback trả về definition + example để điền vào card.
 */
public class SuggestDialog extends BottomSheetDialogFragment {

    public interface OnMeaningSelected {
        void onSelected(String definition, String ipa, String example);
    }

    private static final String ARG_TERM     = "term";
    private static final String ARG_IPA      = "ipa";
    private static final String ARG_DEF_LIST = "definitions";
    private static final String ARG_EX_LIST  = "examples";
    private static final String ARG_POS_LIST = "partsOfSpeech";

    private OnMeaningSelected callback;

    // ── Factory ───────────────────────────────────────────────────────────────

    public static SuggestDialog newInstance(SuggestResponse response,
                                            OnMeaningSelected callback) {
        SuggestDialog dialog = new SuggestDialog();
        dialog.callback = callback;

        Bundle args = new Bundle();
        args.putString(ARG_TERM, response.term);
        args.putString(ARG_IPA,  response.ipa != null ? response.ipa : "");

        if (response.meanings != null && !response.meanings.isEmpty()) {
            String[] defs = new String[response.meanings.size()];
            String[] exs  = new String[response.meanings.size()];
            String[] pos  = new String[response.meanings.size()];
            for (int i = 0; i < response.meanings.size(); i++) {
                SuggestResponse.Meaning m = response.meanings.get(i);
                defs[i] = m.definition  != null ? m.definition  : "";
                exs[i]  = m.example     != null ? m.example     : "";
                pos[i]  = m.partOfSpeech != null ? m.partOfSpeech : "";
            }
            args.putStringArray(ARG_DEF_LIST, defs);
            args.putStringArray(ARG_EX_LIST,  exs);
            args.putStringArray(ARG_POS_LIST, pos);
        }

        dialog.setArguments(args);
        return dialog;
    }

    // ── View ──────────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_suggest, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = requireArguments();
        String term    = args.getString(ARG_TERM, "");
        String ipa     = args.getString(ARG_IPA,  "");
        String[] defs  = args.getStringArray(ARG_DEF_LIST);
        String[] exs   = args.getStringArray(ARG_EX_LIST);
        String[] pos   = args.getStringArray(ARG_POS_LIST);

        // Title
        TextView tvTitle = view.findViewById(R.id.tvSuggestTitle);
        tvTitle.setText("Meanings for \"" + term + "\"");

        // IPA
        TextView tvIpa = view.findViewById(R.id.tvSuggestIpa);
        if (!TextUtils.isEmpty(ipa)) {
            tvIpa.setVisibility(View.VISIBLE);
            tvIpa.setText(ipa);
        } else {
            tvIpa.setVisibility(View.GONE);
        }

        // Danh sách nghĩa
        LinearLayout container2 = view.findViewById(R.id.containerMeanings);
        container2.removeAllViews();

        if (defs == null || defs.length == 0) {
            TextView empty = new TextView(requireContext());
            empty.setText("No meanings found.");
            empty.setPadding(32, 16, 32, 16);
            container2.addView(empty);
            return;
        }

        for (int i = 0; i < defs.length; i++) {
            final String def = defs[i];
            final String ex  = exs  != null ? exs[i]  : "";
            final String p   = pos  != null ? pos[i]  : "";
            final String ipaFinal = ipa;

            // Inflate từng item nghĩa
            View item = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_suggest_meaning, container2, false);

            TextView tvPos   = item.findViewById(R.id.tvPartOfSpeech);
            TextView tvDef   = item.findViewById(R.id.tvMeaningDef);
            TextView tvEx    = item.findViewById(R.id.tvMeaningExample);

            tvPos.setText(p.isEmpty() ? "—" : p);
            tvDef.setText(def);

            if (!TextUtils.isEmpty(ex)) {
                tvEx.setVisibility(View.VISIBLE);
                tvEx.setText("e.g. " + ex);
            } else {
                tvEx.setVisibility(View.GONE);
            }

            // Tap → chọn nghĩa này
            item.setOnClickListener(v -> {
                if (callback != null) {
                    // Format definition: "(partOfSpeech) /ipa/\ndefinition"
                    StringBuilder formatted = new StringBuilder();
                    if (!p.isEmpty())          formatted.append("(").append(p).append(") ");
                    if (!ipaFinal.isEmpty())   formatted.append(ipaFinal).append("\n");
                    formatted.append(def);

                    callback.onSelected(formatted.toString(), ipaFinal, ex);
                }
                dismiss();
            });

            container2.addView(item);

            // Divider (trừ item cuối)
            if (i < defs.length - 1) {
                View divider = new View(requireContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                lp.setMargins(32, 0, 32, 0);
                divider.setLayoutParams(lp);
                divider.setBackgroundColor(0xFFEEEEEE);
                container2.addView(divider);
            }
        }
    }
}