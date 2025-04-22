package com.cntt2.flashcard.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cntt2.flashcard.R;

public abstract class BaseActivity extends AppCompatActivity {

    private NetworkChangeReceiver networkChangeReceiver;
    private boolean isDialogShowing = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Đăng ký BroadcastReceiver
        networkChangeReceiver = new NetworkChangeReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void showNetworkDialog() {
        isDialogShowing = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No connection!");
        builder.setMessage("No internet connection. Please check your connection and try again.");
        builder.setCancelable(false);

        // Nút "Thử lại"
        builder.setPositiveButton("Retry", (dialog, which) -> {
            isDialogShowing = false;
            if (isNetworkAvailable()) {
                Toast.makeText(this, "Reconnected", Toast.LENGTH_SHORT).show();
            } else {
                showNetworkDialog();
            }
        });

        // Nút "Thoát"
        builder.setNegativeButton("Exit", (dialog, which) -> {
            isDialogShowing = false;
            // Thoát ứng dụng
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finishAffinity();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private class NetworkChangeReceiver extends android.content.BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                if (!isNetworkAvailable() && !isDialogShowing) {
                    showNetworkDialog();
                }
            }
        }
    }
}