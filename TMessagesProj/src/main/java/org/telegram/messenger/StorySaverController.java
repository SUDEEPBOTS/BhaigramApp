package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.widget.Toast;
import org.telegram.tgnet.tl.TL_stories;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class StorySaverController {

    private static SharedPreferences getPrefs() {
        try {
            return MessagesController.getGlobalMainSettings();
        } catch (Throwable e) {
            return null;
        }
    }

    public static boolean isAntiStoryDeleteEnabled() {
        try {
            SharedPreferences prefs = getPrefs();
            return prefs != null && prefs.getBoolean("anti_story_delete", true);
        } catch (Throwable ignored) {}
        return true;
    }

    public static void setAntiStoryDeleteEnabled(boolean enabled) {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                prefs.edit().putBoolean("anti_story_delete", enabled).apply();
            }
        } catch (Throwable ignored) {}
    }

    public static boolean isStealthStoryViewEnabled() {
        try {
            SharedPreferences prefs = getPrefs();
            return prefs != null && prefs.getBoolean("stealth_story_view", true);
        } catch (Throwable ignored) {}
        return true;
    }

    public static void setStealthStoryViewEnabled(boolean enabled) {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                prefs.edit().putBoolean("stealth_story_view", enabled).apply();
            }
        } catch (Throwable ignored) {}
    }

    public static void saveStoryToGallery(Context context, File sourceFile, String userTitle) {
        if (context == null || sourceFile == null || !sourceFile.exists()) {
            if (context != null) {
                Toast.makeText(context, "Story media not ready for saving", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        new Thread(() -> {
            try {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Bhaichara_Stories");
                if (!dir.exists()) dir.mkdirs();

                String ext = sourceFile.getName().contains(".mp4") ? ".mp4" : ".jpg";
                File destFile = new File(dir, "Story_" + (userTitle != null ? userTitle.replaceAll("[^a-zA-Z0-9.-]", "_") : "User") + "_" + System.currentTimeMillis() + ext);

                FileInputStream in = new FileInputStream(sourceFile);
                FileOutputStream out = new FileOutputStream(destFile);
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();

                AndroidUtilities.runOnUIThread(() -> {
                    Toast.makeText(context, "Story Saved to Downloads/Bhaichara_Stories!", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> {
                    Toast.makeText(context, "Failed to save story: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
