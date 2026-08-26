package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    public static final String CURRENT_VERSION = "0.0.1";
    public static final String VERSION_URL = "https://raw.githubusercontent.com/SUDEEPBOTS/BhaigramApp/refs/heads/master/Chackv.txt";
    public static final String UPDATE_DOWNLOAD_URL = "https://github.com/SUDEEPBOTS/BhaigramApp";
    public static final long CREATOR_USER_ID = 6356015122L;

    private static boolean isUpdateDialogOpen = false;

    public static void checkVersionAndOnboarding(Activity activity, int currentAccount) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        // 1. First-time Onboarding & Creator Verification
        handleFirstTimeLaunch(activity, currentAccount);

        // 2. Check Remote Version from GitHub
        Utilities.globalQueue.postRunnable(() -> {
            try {
                URL url = new URL(VERSION_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setUseCaches(false);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    StringBuilder content = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        content.append(line.trim());
                    }
                    reader.close();

                    final String remoteVersion = content.toString().trim();
                    if (!remoteVersion.isEmpty() && !remoteVersion.equalsIgnoreCase(CURRENT_VERSION)) {
                        AndroidUtilities.runOnUIThread(() -> showMandatoryUpdateDialog(activity, remoteVersion));
                    }
                }
            } catch (Throwable t) {
                FileLog.e(t);
            }
        });
    }

    private static void showMandatoryUpdateDialog(Activity activity, String newVersion) {
        if (activity == null || activity.isFinishing() || isUpdateDialogOpen) {
            return;
        }
        isUpdateDialogOpen = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("🚀 Mandatory Update Required!");
        builder.setMessage("A new version of Bhaichara (" + newVersion + ") is available!\n\nYour current version (" + CURRENT_VERSION + ") is outdated. You must update to continue using Bhaichara.\n\nClick 'Download Update' below.");
        builder.setCancelable(false);

        builder.setPositiveButton("⬇️ Download Update", (dialog, which) -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(UPDATE_DOWNLOAD_URL));
                activity.startActivity(intent);
            } catch (Exception e) {
                FileLog.e(e);
            }
            activity.finishAffinity();
            System.exit(0);
        });

        builder.setNegativeButton("❌ Exit App", (dialog, which) -> {
            activity.finishAffinity();
            System.exit(0);
        });

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }

    private static void handleFirstTimeLaunch(Activity activity, int currentAccount) {
        try {
            SharedPreferences prefs = MessagesController.getGlobalMainSettings();
            if (prefs != null && !prefs.getBoolean("bhaichara_first_launch_done", false)) {
                prefs.edit().putBoolean("bhaichara_first_launch_done", true).apply();

                // Open creator welcome chat or send initial greeting
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        if (activity instanceof LaunchActivity) {
                            LaunchActivity launchActivity = (LaunchActivity) activity;
                            android.os.Bundle args = new android.os.Bundle();
                            args.putLong("user_id", CREATOR_USER_ID);
                            launchActivity.presentFragment(new ChatActivity(args));
                            Toast.makeText(activity, "👑 Welcome to Bhaichara! Send a message to Creator to register.", Toast.LENGTH_LONG).show();
                        }
                    } catch (Throwable e) {
                        FileLog.e(e);
                    }
                }, 1500);
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}
