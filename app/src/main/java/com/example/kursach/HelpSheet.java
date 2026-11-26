package com.example.kursach;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class HelpSheet extends BottomSheetDialogFragment {
    public HelpSheet() {
        super(R.layout.sheet_help);
    }

    @Override
    public int getTheme() {
        return R.style.SupportHelpDialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) requireDialog();
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
