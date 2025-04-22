package com.cntt2.flashcard.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cntt2.flashcard.R;

public class ConfirmDialog {
    public static AlertDialog createConfirmDialog(Context context, String title, String message, View.OnClickListener yes, View.OnClickListener no) {
        return createConfirmDialog(context, title, message, yes, no, true);
    }


    public static AlertDialog createConfirmDialog(Context context, String title, String message, View.OnClickListener yes, View.OnClickListener no, boolean canclable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = View.inflate(context, R.layout.dialog_confirm_cancel, null); // Sử dụng View.inflate với context
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

        btnYes.setOnClickListener(view -> {
            yes.onClick(view);
            dialog.dismiss();
        });

        btnNo.setOnClickListener(view -> {
            no.onClick(view);
            dialog.dismiss();
        });

        dialog.setCancelable(canclable);

        return dialog;
    }
}