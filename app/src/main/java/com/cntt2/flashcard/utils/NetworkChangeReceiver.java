package com.cntt2.flashcard.utils;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private static boolean isDialogShowing = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            boolean isConnected = isNetworkAvailable(context);

            if (!isConnected && !isDialogShowing) {
                showNetworkDialog(context);
            }
        }
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void showNetworkDialog(Context context) {
        isDialogShowing = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Mất kết nối mạng");
        builder.setMessage("Không có kết nối internet. Vui lòng kiểm tra kết nối và thử lại.");
        builder.setCancelable(false);

        // Nút "Thử lại"
        builder.setPositiveButton("Thử lại", (dialog, which) -> {
            isDialogShowing = false;
            if (isNetworkAvailable(context)) {
                Toast.makeText(context, "Đã kết nối lại!", Toast.LENGTH_SHORT).show();
            } else {
                showNetworkDialog(context); // Hiển thị lại dialog nếu vẫn không có mạng
            }
        });

        // Nút "Thoát"
        builder.setNegativeButton("Thoát", (dialog, which) -> {
            isDialogShowing = false;
            // Thoát ứng dụng
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            if (context instanceof AppCompatActivity) {
                ((AppCompatActivity) context).finishAffinity();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}