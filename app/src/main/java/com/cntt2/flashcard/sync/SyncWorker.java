package com.cntt2.flashcard.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";
    public static final String KEY_SYNC_RESULT = "sync_result";
    public static final String KEY_ERROR_MESSAGE = "error_message";

    private boolean isSuccessful = true;
    private String errorMessage = null;
    private CountDownLatch latch;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SyncManager syncManager = new SyncManager(getApplicationContext());
        latch = new CountDownLatch(1);

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
                                                isSuccessful = true;
                                                latch.countDown();
                                            }

                                            @Override
                                            public void onFailure(String error) {
                                                Log.e(TAG, "Review sync failed: " + error);
                                                isSuccessful = false;
                                                errorMessage = "Review sync failed: " + error;
                                                latch.countDown();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        Log.e(TAG, "Card sync failed: " + error);
                                        isSuccessful = false;
                                        errorMessage = "Card sync failed: " + error;
                                        latch.countDown();
                                    }
                                });
                            }

                            @Override
                            public void onFailure(String error) {
                                Log.e(TAG, "Session sync failed: " + error);
                                isSuccessful = false;
                                errorMessage = "Session sync failed: " + error;
                                latch.countDown();
                            }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Desk sync failed: " + error);
                        isSuccessful = false;
                        errorMessage = "Desk sync failed: " + error;
                        latch.countDown();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Folder sync failed: " + error);
                isSuccessful = false;
                errorMessage = "Folder sync failed: " + error;
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                Log.e(TAG, "Sync operation timed out");
                isSuccessful = false;
                errorMessage = "Sync operation timed out";
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for sync: " + e.getMessage());
            isSuccessful = false;
            errorMessage = "Sync interrupted: " + e.getMessage();
        }

        if (isSuccessful) {
            return Result.success();
        } else {
            Data outputData = new Data.Builder()
                    .putString(KEY_SYNC_RESULT, "Sync failed")
                    .putString(KEY_ERROR_MESSAGE, errorMessage)
                    .build();
            return Result.failure(outputData);
        }
    }
}