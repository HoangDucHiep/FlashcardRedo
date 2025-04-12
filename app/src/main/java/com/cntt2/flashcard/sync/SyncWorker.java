package com.cntt2.flashcard.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SyncManager syncManager = new SyncManager(getApplicationContext());
        syncManager.syncFolders(new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Folder sync completed successfully");
                syncManager.syncDesks(new SyncManager.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Desk sync completed successfully");
                        syncManager.syncSessions(new SyncManager.SyncCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Session sync completed successfully");
                                syncManager.syncCards(new SyncManager.SyncCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "Card sync completed successfully");
                                        syncManager.syncReviews(new SyncManager.SyncCallback() {
                                            @Override
                                            public void onSuccess() {
                                                Log.d(TAG, "Review sync completed successfully");
                                            }

                                            @Override
                                            public void onFailure(String error) {
                                                Log.e(TAG, "Review sync failed: " + error);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        Log.e(TAG, "Card sync failed: " + error);
                                    }
                                });
                            }

                            @Override
                            public void onFailure(String error) {
                                Log.e(TAG, "Session sync failed: " + error);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Desk sync failed: " + error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Folder sync failed: " + error);
            }
        });
        return Result.success();
    }
}