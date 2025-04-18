package com.cntt2.flashcard.ui.fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.R;
import com.cntt2.flashcard.auth.AuthManager;
import com.cntt2.flashcard.data.local.DatabaseHelper;
import com.cntt2.flashcard.ui.activities.LoginActivity;
import com.cntt2.flashcard.utils.NotificationReceiver;
import com.cntt2.flashcard.sync.SyncManager;

import java.util.Calendar;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SettingsFragment extends Fragment {

    private TextView tvUserName;
    private TextView tvReminderTime;
    private Button btnLogout;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "FlashcardPrefs";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";


    private int pendingHour = -1;
    private int pendingMinute = -1;

    // Launcher for requesting notification permission
    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted, schedule the notification
                    if (pendingHour != -1 && pendingMinute != -1) {
                        scheduleDailyNotification(pendingHour, pendingMinute);
                        pendingHour = -1;
                        pendingMinute = -1;
                    }
                } else {
                    // Permission denied, inform the user
                    Toast.makeText(requireContext(), "Notification permission denied. You won't receive study reminders.", Toast.LENGTH_LONG).show();
                }
            });

    // Launcher for requesting exact alarm permission (Android 12+)
    private final ActivityResultLauncher<Intent> requestExactAlarmLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                    // Permission granted, proceed with scheduling
                    if (pendingHour != -1 && pendingMinute != -1) {
                        scheduleDailyNotification(pendingHour, pendingMinute);
                        pendingHour = -1;
                        pendingMinute = -1;
                    }
                } else {
                    Toast.makeText(requireContext(), "Exact alarm permission denied. Reminders may not work as expected.", Toast.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        // Initialize views
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvReminderTime = view.findViewById(R.id.tv_reminder_time);
        btnLogout = view.findViewById(R.id.btn_logout);

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Display user's name
        AuthManager authManager = new AuthManager(requireContext());
        String userName = authManager.getUsername();
        tvUserName.setText(userName);

        // Load and display saved reminder time
        int savedHour = sharedPreferences.getInt(KEY_REMINDER_HOUR, -1);
        int savedMinute = sharedPreferences.getInt(KEY_REMINDER_MINUTE, -1);
        if (savedHour != -1 && savedMinute != -1) {
            tvReminderTime.setText(String.format("%02d:%02d", savedHour, savedMinute));
        }

        // Set up reminder time picker
        tvReminderTime.setOnClickListener(v -> showTimePickerDialog());

        // Set up logout button
        btnLogout.setOnClickListener(v -> logout());

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

                    // Schedule the daily notification
                    scheduleDailyNotification(hourOfDay, minuteOfDay);
                },
                hour,
                minute,
                true // 24-hour format
        );
        timePickerDialog.show();
    }

    private void scheduleDailyNotification(int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Set the time for the notification
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // If the time is in the past, schedule for the next day
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Schedule the alarm to repeat daily
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }

    private void logout() {
        // Show a loading dialog
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Syncing data before logout...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Perform sync in a background thread
        new Thread(() -> {
            SyncManager syncManager = new SyncManager(requireContext());
            CountDownLatch latch = new CountDownLatch(1);

            syncManager.syncFolders(new SyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    syncManager.syncDesks(new SyncManager.SyncCallback() {
                        @Override
                        public void onSuccess() {
                            syncManager.syncSessions(new SyncManager.SyncCallback() {
                                @Override
                                public void onSuccess() {
                                    syncManager.syncCards(new SyncManager.SyncCallback() {
                                        @Override
                                        public void onSuccess() {
                                            syncManager.syncReviews(new SyncManager.SyncCallback() {
                                                @Override
                                                public void onSuccess() {
                                                    latch.countDown();
                                                }

                                                @Override
                                                public void onFailure(String error) {
                                                    requireActivity().runOnUiThread(() ->
                                                            Toast.makeText(requireContext(), "Review sync failed: " + error, Toast.LENGTH_LONG).show());
                                                    latch.countDown();
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure(String error) {
                                            requireActivity().runOnUiThread(() ->
                                                    Toast.makeText(requireContext(), "Card sync failed: " + error, Toast.LENGTH_LONG).show());
                                            latch.countDown();
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(String error) {
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(), "Session sync failed: " + error, Toast.LENGTH_LONG).show());
                                    latch.countDown();
                                }
                            });
                        }

                        @Override
                        public void onFailure(String error) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "Desk sync failed: " + error, Toast.LENGTH_LONG).show());
                            latch.countDown();
                        }
                    });
                }

                @Override
                public void onFailure(String error) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Folder sync failed: " + error, Toast.LENGTH_LONG).show());
                    latch.countDown();
                }
            });

            try {
                boolean completed = latch.await(30, TimeUnit.SECONDS);
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (!completed) {
                        Toast.makeText(requireContext(), "Sync timed out. Proceeding with logout.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), "Sync completed successfully.", Toast.LENGTH_SHORT).show();
                    }
                    performLogout();
                });
            } catch (InterruptedException e) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(requireContext(), "Sync interrupted: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    performLogout();
                });
            }
        }).start();
    }

    private void performLogout() {
        // Clear login state
        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
        AuthManager authManager = new AuthManager(requireContext());

        cancelScheduledNotification();

        authManager.logout();

        // Clear SharedPreferences (optional, depending on your needs)
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
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }
}