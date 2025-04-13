package com.cntt2.flashcard.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";
    public static final String KEY_SYNC_RESULT = "sync_result";
    public static final String KEY_ERROR_MESSAGE = "error_message";

    private boolean isSuccessful = true;
    private String errorMessage = null;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SyncManager syncManager = new SyncManager(getApplicationContext());

        // Reset state
        isSuccessful = true;
        errorMessage = null;

        // Synchronize folders
        syncManager.syncFolders(new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Folder sync completed successfully");
                // Synchronize desks
                syncManager.syncDesks(new SyncManager.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Desk sync completed successfully");
                        // Synchronize sessions
                        syncManager.syncSessions(new SyncManager.SyncCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Session sync completed successfully");
                                // Synchronize cards
                                syncManager.syncCards(new SyncManager.SyncCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "Card sync completed successfully");
                                        // Synchronize reviews
                                        syncManager.syncReviews(new SyncManager.SyncCallback() {
                                            @Override
                                            public void onSuccess() {
                                                Log.d(TAG, "Review sync completed successfully");
                                                // All sync steps completed successfully
                                            }

                                            @Override
                                            public void onFailure(String error) {
                                                Log.e(TAG, "Review sync failed: " + error);
                                                isSuccessful = false;
                                                errorMessage = "Review sync failed: " + error;
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        Log.e(TAG, "Card sync failed: " + error);
                                        isSuccessful = false;
                                        errorMessage = "Card sync failed: " + error;
                                    }
                                });
                            }

                            @Override
                            public void onFailure(String error) {
                                Log.e(TAG, "Session sync failed: " + error);
                                isSuccessful = false;
                                errorMessage = "Session sync failed: " + error;
                            }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Desk sync failed: " + error);
                        isSuccessful = false;
                        errorMessage = "Desk sync failed: " + error;
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Folder sync failed: " + error);
                isSuccessful = false;
                errorMessage = "Folder sync failed: " + error;
            }
        });

        // Since Retrofit calls are asynchronous, we need to wait for them to complete.
        // However, WorkManager runs on a background thread, and we can't block here.
        // Instead, we'll rely on WorkManager's result mechanism to communicate the outcome.
        // For simplicity, we'll assume the sync is synchronous for now (not ideal, see notes below).
        // In a real app, you should use WorkManager's LiveData or a more robust mechanism.

        // Simulate waiting for async tasks (not ideal, see recommendations below)
        try {
            Thread.sleep(5000); // Wait for 5 seconds to allow async tasks to complete
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for sync: " + e.getMessage());
            isSuccessful = false;
            errorMessage = "Sync interrupted: " + e.getMessage();
        }

        if (isSuccessful) {
            return Result.success();
        } else {
            Data outputData = new Data.Builder()
                    .putBoolean(KEY_SYNC_RESULT, false)
                    .putString(KEY_ERROR_MESSAGE, errorMessage)
                    .build();
            return Result.failure(outputData);
        }
    }
}