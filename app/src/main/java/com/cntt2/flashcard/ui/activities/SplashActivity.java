package com.cntt2.flashcard.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.remote.ApiClient;
//import com.cntt2.flashcard.sync.SyncWorker;

public class SplashActivity extends AppCompatActivity {
    private TextView syncStatusText;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Khởi tạo UI
        syncStatusText = findViewById(R.id.sync_status_text);
        progressBar = findViewById(R.id.progress_bar);

        // Kiểm tra đăng nhập
        if (!ApiClient.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Schedule the SyncWorker
        //scheduleSync();
        proceedToMainActivity();
    }

    private void scheduleSync() {
        syncStatusText.setText("Starting sync...");

        // Create a one-time work request for SyncWorker
//        OneTimeWorkRequest syncWorkRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
//                .build();
//
//        // Enqueue the work
//        WorkManager workManager = WorkManager.getInstance(this);
//        workManager.enqueue(syncWorkRequest);
//
//        // Observe the work's state
//        workManager.getWorkInfoByIdLiveData(syncWorkRequest.getId())
//                .observe(this, workInfo -> {
//                    if (workInfo != null) {
//                        WorkInfo.State state = workInfo.getState();
//                        switch (state) {
//                            case RUNNING:
//                                syncStatusText.setText("Syncing data...");
//                                break;
//                            case SUCCEEDED:
//                                syncStatusText.setText("Sync completed successfully!");
//                                proceedToMainActivity();
//                                break;
//                            case FAILED:
//                                String errorMessage = workInfo.getOutputData().getString(SyncWorker.KEY_ERROR_MESSAGE);
//                                syncStatusText.setText("Sync failed: " + (errorMessage != null ? errorMessage : "Unknown error"));
//                                Toast.makeText(this, "Sync failed, proceeding to MainActivity", Toast.LENGTH_LONG).show();
//                                proceedToMainActivity();
//                                break;
//                            default:
//                                syncStatusText.setText("Sync status: " + state.name());
//                                break;
//                        }
//                    }
//                });
    }

    private void proceedToMainActivity() {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }
}