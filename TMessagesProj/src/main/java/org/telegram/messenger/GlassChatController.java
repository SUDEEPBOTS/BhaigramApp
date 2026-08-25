package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import org.telegram.ui.ActionBar.AlertDialog;

public class GlassChatController {

    private static SharedPreferences getPrefs() {
        try {
            return MessagesController.getGlobalMainSettings();
        } catch (Throwable e) {
            return null;
        }
    }

    public static int getBubbleAlpha() {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                return prefs.getInt("glass_bubble_alpha", 255); // 255 = 100% solid
            }
        } catch (Throwable ignored) {}
        return 255;
    }

    public static void setBubbleAlpha(int alpha) {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                prefs.edit().putInt("glass_bubble_alpha", Math.max(50, Math.min(255, alpha))).apply();
            }
        } catch (Throwable ignored) {}
    }

    public static int getBubbleCornerRadius() {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                return prefs.getInt("glass_bubble_radius", 16); // default 16dp
            }
        } catch (Throwable ignored) {}
        return 16;
    }

    public static void setBubbleCornerRadius(int radiusDp) {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                prefs.edit().putInt("glass_bubble_radius", radiusDp).apply();
            }
            SharedConfig.setBubbleRadius(radiusDp);
        } catch (Throwable ignored) {}
    }

    public static boolean isAmoledBlackEnabled() {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                return prefs.getBoolean("glass_amoled_black", false);
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static void setAmoledBlackEnabled(boolean enabled) {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                prefs.edit().putBoolean("glass_amoled_black", enabled).apply();
            }
        } catch (Throwable ignored) {}
    }

    public static void showGlassCustomizerDialog(Context context, Runnable onUpdate) {
        if (context == null) return;

        int currentAlpha = getBubbleAlpha();
        int alphaPercent = (int) Math.round((currentAlpha / 255.0) * 100);
        int currentRadius = getBubbleCornerRadius();

        String[] options = new String[]{
            "Bubble Transparency: " + alphaPercent + "% (" + getAlphaDescription(currentAlpha) + ")",
            "Bubble Corner Roundness: " + currentRadius + "dp (" + getRadiusDescription(currentRadius) + ")",
            "AMOLED Pure Black Chat Background: " + (isAmoledBlackEnabled() ? "[ENABLED]" : "[DISABLED]"),
            "Preset: Ultra Glass Cyberpunk (70% Glass + 24dp Round)",
            "Preset: iOS 18 Glass Blur (85% Opacity + 18dp Round)",
            "Reset Chat Style to Default (100% Solid + 16dp)"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Glass Chat & Room Customizer");
        builder.setItems(options, (dialog, which) -> {
            dialog.dismiss();
            switch (which) {
                case 0:
                    showAlphaSelector(context, onUpdate);
                    return;
                case 1:
                    showRadiusSelector(context, onUpdate);
                    return;
                case 2:
                    setAmoledBlackEnabled(!isAmoledBlackEnabled());
                    Toast.makeText(context, "AMOLED Black: " + (isAmoledBlackEnabled() ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    setBubbleAlpha(178); // 70%
                    setBubbleCornerRadius(24);
                    Toast.makeText(context, "Applied Ultra Glass Cyberpunk Style!", Toast.LENGTH_SHORT).show();
                    break;
                case 4:
                    setBubbleAlpha(216); // 85%
                    setBubbleCornerRadius(18);
                    Toast.makeText(context, "Applied iOS 18 Glass Style!", Toast.LENGTH_SHORT).show();
                    break;
                case 5:
                    setBubbleAlpha(255);
                    setBubbleCornerRadius(16);
                    setAmoledBlackEnabled(false);
                    Toast.makeText(context, "Reset Chat Style to Standard Default", Toast.LENGTH_SHORT).show();
                    break;
            }
            if (onUpdate != null) onUpdate.run();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private static String getAlphaDescription(int alpha) {
        if (alpha >= 250) return "Solid";
        if (alpha >= 200) return "Light Glass";
        if (alpha >= 160) return "Medium Glass";
        if (alpha >= 110) return "Semi-Transparent";
        return "Ghost Glass";
    }

    private static String getRadiusDescription(int radius) {
        if (radius <= 8) return "Compact/Square";
        if (radius <= 16) return "Standard";
        if (radius <= 24) return "Super Round";
        return "Pill Shape";
    }

    private static void showAlphaSelector(Context context, Runnable onUpdate) {
        String[] alphaLevels = new String[]{
            "100% - Solid Default (No Transparency)",
            "90% - Subtle Glass Tint",
            "80% - Modern Frosted Glass",
            "70% - Semi-Transparent Glass",
            "55% - High Blur Glass",
            "40% - Ghost Bubble (Maximum Transparency)"
        };
        final int[] values = new int[]{255, 230, 204, 178, 140, 102};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Bubble Glass Opacity");
        builder.setItems(alphaLevels, (dialog, which) -> {
            dialog.dismiss();
            setBubbleAlpha(values[which]);
            Toast.makeText(context, "Bubble Transparency set to " + alphaLevels[which].split("-")[0].trim(), Toast.LENGTH_SHORT).show();
            if (onUpdate != null) onUpdate.run();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private static void showRadiusSelector(Context context, Runnable onUpdate) {
        String[] radiusLevels = new String[]{
            "8dp - Compact Angular (Sharp Modern)",
            "14dp - Subtle Rounded",
            "16dp - Standard Default",
            "20dp - Soft Curvature (iOS Style)",
            "26dp - Ultra Round Bubble",
            "32dp - Full Capsule / Pill Shape"
        };
        final int[] values = new int[]{8, 14, 16, 20, 26, 32};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Bubble Roundness / Size");
        builder.setItems(radiusLevels, (dialog, which) -> {
            dialog.dismiss();
            setBubbleCornerRadius(values[which]);
            Toast.makeText(context, "Bubble Roundness set to " + values[which] + "dp", Toast.LENGTH_SHORT).show();
            if (onUpdate != null) onUpdate.run();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }
}
