package com.cntt2.flashcard.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.cntt2.flashcard.R;

public class ConfirmDialog {
    public static android.app.AlertDialog createConfirmDialog(Fragment fr, Context context, String title, String message, View.OnClickListener yes, View.OnClickListener no) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        View dialogView = fr.getLayoutInflater().inflate(R.layout.dialog_confirm_cancel, null);
        builder.setView(dialogView);

        TextView dialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView dialogMessage = dialogView.findViewById(R.id.tv_dialog_message);
        Button btnYes = dialogView.findViewById(R.id.btn_dialog_yes);
        Button btnNo = dialogView.findViewById(R.id.btn_dialog_no);

        AlertDialog dialog = builder.create();

        dialogTitle.setText(title);
        dialogMessage.setText(message);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                yes.onClick(view);
                dialog.dismiss();
            }
        });

        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                no.onClick(view);
                dialog.dismiss();
            }
        });
        return dialog;
    }
}
