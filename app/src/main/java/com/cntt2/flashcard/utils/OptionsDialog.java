package com.cntt2.flashcard.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cntt2.flashcard.R;

import java.util.List;

public class OptionsDialog {

    public static class Option {
        private final String text;
        private final int iconResId;
        private final int textColor;
        private final Runnable onClick;

        public Option(String text, int iconResId, int textColor, Runnable onClick) {
            this.text = text;
            this.iconResId = iconResId;
            this.textColor = textColor;
            this.onClick = onClick;
        }

        public String getText() {
            return text;
        }

        public int getIconResId() {
            return iconResId;
        }

        public int getTextColor() {
            return textColor;
        }

        public void performAction() {
            if (onClick != null) {
                onClick.run();
            }
        }
    }

    private static class OptionAdapter extends RecyclerView.Adapter<OptionAdapter.OptionViewHolder> {
        private final List<Option> options;
        private final Dialog dialog;

        public OptionAdapter(List<Option> options, Dialog dialog) {
            this.options = options;
            this.dialog = dialog;
        }

        @NonNull
        @Override
        public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_option, parent, false);
            return new OptionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
            Option option = options.get(position);
            holder.tvOptionText.setText(option.getText());
            holder.ivOptionIcon.setImageResource(option.getIconResId());
            holder.tvOptionText.setTextColor(option.getTextColor());
            holder.itemView.setOnClickListener(v -> {
                option.performAction();
                dialog.dismiss();
            });
        }

        @Override
        public int getItemCount() {
            return options.size();
        }

        static class OptionViewHolder extends RecyclerView.ViewHolder {
            TextView tvOptionText;
            ImageView ivOptionIcon;

            public OptionViewHolder(@NonNull View itemView) {
                super(itemView);
                tvOptionText = itemView.findViewById(R.id.tv_option_text);
                ivOptionIcon = itemView.findViewById(R.id.iv_option_icon);
            }
        }
    }

    public static void showOptionsDialog(Context context, View anchorView, List<Option> options) {
        // Tạo dialog
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_options);

        // Đặt background trong suốt
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Thiết lập RecyclerView
        RecyclerView rvOptions = dialog.findViewById(R.id.rv_options);
        rvOptions.setLayoutManager(new LinearLayoutManager(context));
        rvOptions.setAdapter(new OptionAdapter(options, dialog));

        // Điều chỉnh vị trí của dialog
        if (dialog.getWindow() != null && anchorView != null) {
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();

            // Lấy vị trí của view được nhấn
            int[] location = new int[2];
            anchorView.getLocationOnScreen(location);

            // Lấy kích thước màn hình
            Point size = new Point();
            ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(size);
            int screenHeight = size.y;

            // Ước lượng chiều cao của dialog
            int dialogHeight = options.size() * 60 + 20; // 60dp mỗi item + padding
            if (location[1] + anchorView.getHeight() + dialogHeight > screenHeight) {
                // Hiển thị phía trên view
                params.x = location[0];
                params.y = location[1] - dialogHeight;
            } else {
                // Hiển thị phía dưới view
                params.x = location[0];
                params.y = location[1] + anchorView.getHeight();
            }

            params.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
            dialog.getWindow().setAttributes(params);
        }

        // Hiển thị dialog
        dialog.show();
    }
}