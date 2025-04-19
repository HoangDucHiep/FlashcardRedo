package com.cntt2.flashcard.ui.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.cntt2.flashcard.R;
import com.cntt2.flashcard.auth.AuthManager;
import com.cntt2.flashcard.data.local.DatabaseHelper;
import com.cntt2.flashcard.sync.SyncManager;
import com.cntt2.flashcard.sync.SyncWorker;
import com.cntt2.flashcard.ui.activities.LoginActivity;
import com.cntt2.flashcard.utils.ConfirmDialog;
import com.cntt2.flashcard.utils.NotificationWorker;

import java.util.Calendar;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SettingsFragment extends Fragment {

    private TextView tvUserName;
    private TextView tvReminderTime;
    private Switch switchReminder;
    private Button btnLogout;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "FlashcardPrefs";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String TAG = "SettingsFragment";
    private int pendingHour = -1;
    private int pendingMinute = -1;

    // Launcher for requesting notification permission
    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    if (pendingHour != -1 && pendingMinute != -1) {
                        scheduleDailyNotification(pendingHour, pendingMinute);
                        pendingHour = -1;
                        pendingMinute = -1;
                    }
                } else {
                    Toast.makeText(requireContext(), "Notification permission denied. You won't receive study reminders.", Toast.LENGTH_LONG).show();
                    switchReminder.setChecked(false);
                    saveReminderEnabled(false);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        // Initialize views
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvReminderTime = view.findViewById(R.id.tv_reminder_time);
        switchReminder = view.findViewById(R.id.switch_reminder);
        btnLogout = view.findViewById(R.id.btn_logout);

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Display user's name
        AuthManager authManager = new AuthManager(requireContext());
        String userName = authManager.getUsername();
        tvUserName.setText(userName);

        // Load and display saved reminder time and state
        updateReminderUI();

        // Set up reminder time picker
        tvReminderTime.setOnClickListener(v -> {
            if (switchReminder.isChecked()) {
                showTimePickerDialog();
            } else {
                Toast.makeText(requireContext(), "Please enable reminders first", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up switch for enabling/disabling reminders
        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // If enabling, prompt to set time if not set
                if (!hasReminderTime()) {
                    showTimePickerDialog();
                } else {
                    // Re-schedule existing reminder
                    int savedHour = sharedPreferences.getInt(KEY_REMINDER_HOUR, -1);
                    int savedMinute = sharedPreferences.getInt(KEY_REMINDER_MINUTE, -1);
                    if (savedHour != -1 && savedMinute != -1) {
                        pendingHour = savedHour;
                        pendingMinute = savedMinute;
                        checkAndRequestPermissions();
                    }
                }
            } else {
                // Show dialog to confirm clearing reminder time
                if (hasReminderTime()) {
                    ConfirmDialog.createConfirmDialog(
                            requireContext(),
                            "Turn off reminders",
                            "Do you want to clear the reminder time?",
                            v -> {
                                cancelReminder();
                            },
                            v -> {
                                cancelScheduledNotification();
                                saveReminderEnabled(false);
                            }
                            ).show();
                } else {
                    cancelScheduledNotification();
                    saveReminderEnabled(false);
                }
            }
        });

        // Set up logout button
        btnLogout.setOnClickListener(v -> {
            ConfirmDialog.createConfirmDialog(
                    requireContext(),
                    "Logout",
                    "Are you sure you want to log out?",
                    v1 -> {
                        logout();
                    },
                    v12 -> {
                        // Do nothing on cancel
                    }
            ).show();
        });

        return view;
    }

    private void showTimePickerDialog() {
        Calendar currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minuteOfDay) -> {
                    // Save the selected time
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt(KEY_REMINDER_HOUR, hourOfDay);
                    editor.putInt(KEY_REMINDER_MINUTE, minuteOfDay);
                    editor.apply();

                    // Update UI
                    tvReminderTime.setText(String.format("%02d:%02d", hourOfDay, minuteOfDay));

                    // Store pending time and check permissions
                    pendingHour = hourOfDay;
                    pendingMinute = minuteOfDay;
                    checkAndRequestPermissions();
                },
                hour,
                minute,
                true // 24-hour format
        );
        timePickerDialog.show();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }
        if (pendingHour != -1 && pendingMinute != -1) {
            scheduleDailyNotification(pendingHour, pendingMinute);
            pendingHour = -1;
            pendingMinute = -1;
        }
    }

    private void scheduleDailyNotification(int hour, int minute) {
        Calendar currentTime = Calendar.getInstance();
        Calendar targetTime = Calendar.getInstance();

        targetTime.setTimeInMillis(System.currentTimeMillis());
        targetTime.set(Calendar.HOUR_OF_DAY, hour);
        targetTime.set(Calendar.MINUTE, minute);
        targetTime.set(Calendar.SECOND, 0);
        targetTime.set(Calendar.MILLISECOND, 0);

        if (targetTime.getTimeInMillis() <= currentTime.getTimeInMillis()) {
            targetTime.add(Calendar.DAY_OF_MONTH, 1);
        }

        long initialDelay = targetTime.getTimeInMillis() - currentTime.getTimeInMillis();

        PeriodicWorkRequest notificationRequest = new PeriodicWorkRequest.Builder(
                NotificationWorker.class,
                24,
                TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build())
                .build();

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "daily_notification_work",
                ExistingPeriodicWorkPolicy.REPLACE,
                notificationRequest
        );

        Log.d(TAG, "Scheduled notification for " + String.format("%02d:%02d", hour, minute));
        Toast.makeText(requireContext(), "Reminder set for " + String.format("%02d:%02d", hour, minute), Toast.LENGTH_SHORT).show();
        switchReminder.setChecked(true);
        saveReminderEnabled(true);
    }

    private void cancelReminder() {
        cancelScheduledNotification();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_REMINDER_HOUR);
        editor.remove(KEY_REMINDER_MINUTE);
        editor.apply();
        tvReminderTime.setText("Not set");
        switchReminder.setChecked(false);
        saveReminderEnabled(false);
        Toast.makeText(requireContext(), "Reminder cancelled", Toast.LENGTH_SHORT).show();
    }

    private void logout() {
        // Show a loading dialog
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Syncing data before logout...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Perform sync in a background thread
        OneTimeWorkRequest syncWorkRequest = new OneTimeWorkRequest.Builder(SyncWorker.class).build();

        WorkManager workManager = WorkManager.getInstance(requireContext());
        workManager.enqueue(syncWorkRequest);

        workManager.getWorkInfoByIdLiveData(syncWorkRequest.getId())
                .observe(getViewLifecycleOwner(), new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            progressDialog.dismiss();
                            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                Toast.makeText(requireContext(), "Sync completed successfully.", Toast.LENGTH_SHORT).show();
                                performLogout();
                            } else {
                                String errorMessage = workInfo.getOutputData().getString(SyncWorker.KEY_ERROR_MESSAGE);
                                boolean isTimeout = workInfo.getOutputData().getBoolean(SyncWorker.KEY_TIMEOUT, false);

                                if (isTimeout) {
                                    Toast.makeText(requireContext(), "Sync timed out, logging out...", Toast.LENGTH_LONG).show();
                                } else if (errorMessage != null) {
                                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(requireContext(), "Sync failed, logging out...", Toast.LENGTH_LONG).show();
                                }
                                performLogout();
                            }

                            workManager.getWorkInfoByIdLiveData((syncWorkRequest.getId()))
                                    .removeObservers(getViewLifecycleOwner());
                        }
                    }
                });
    }

    private void performLogout() {
        // Clear login state
        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
        AuthManager authManager = new AuthManager(requireContext());

        cancelScheduledNotification();

        authManager.logout();

        // Clear SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        dbHelper.clearAllData();

        // Navigate to LoginActivity
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void cancelScheduledNotification() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork("daily_notification_work");
        Log.d(TAG, "Cancelled scheduled notification");
    }

    private void saveReminderEnabled(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_REMINDER_ENABLED, enabled);
        editor.apply();
    }

    private boolean hasReminderTime() {
        int savedHour = sharedPreferences.getInt(KEY_REMINDER_HOUR, -1);
        int savedMinute = sharedPreferences.getInt(KEY_REMINDER_MINUTE, -1);
        return savedHour != -1 && savedMinute != -1;
    }

    private void updateReminderUI() {
        boolean isReminderEnabled = sharedPreferences.getBoolean(KEY_REMINDER_ENABLED, false);
        int savedHour = sharedPreferences.getInt(KEY_REMINDER_HOUR, -1);
        int savedMinute = sharedPreferences.getInt(KEY_REMINDER_MINUTE, -1);

        switchReminder.setChecked(isReminderEnabled);
        if (isReminderEnabled && savedHour != -1 && savedMinute != -1) {
            tvReminderTime.setText(String.format("%02d:%02d", savedHour, savedMinute));
        } else {
            tvReminderTime.setText("Not set");
        }
    }
}