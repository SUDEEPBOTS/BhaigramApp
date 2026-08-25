package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.widget.Toast;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import java.io.File;
import java.util.HashMap;

public class FontController {

    public static final int FONT_DEFAULT = 0;
    public static final int FONT_SAN_FRANCISCO = 1;
    public static final int FONT_PRODUCT_SANS = 2;
    public static final int FONT_JETBRAINS_MONO = 3;
    public static final int FONT_SAMSUNG_ONE = 4;
    public static final int FONT_COMIC = 5;
    public static final int FONT_CUSTOM_FILE = 6;

    public static final String[] FONT_NAMES = {
        "Telegram Default",
        "Apple San Francisco (iOS)",
        "Google Product Sans",
        "JetBrains Mono (Code/Terminal)",
        "Samsung One (Modern Sans)",
        "Casual Handwriting Style",
        "Custom Font File (.ttf / .otf)"
    };

    private static final HashMap<String, Typeface> customTypefaceCache = new HashMap<>();

    private static SharedPreferences getPrefs() {
        return MessagesController.getGlobalMainSettings();
    }

    public static int getSelectedFont() {
        return getPrefs().getInt("bhaigram_selected_font", FONT_DEFAULT);
    }

    public static void setSelectedFont(int font) {
        getPrefs().edit().putInt("bhaigram_selected_font", font).apply();
        customTypefaceCache.clear();
        AndroidUtilities.typefaceCache.clear();
    }

    public static String getCustomFontPath() {
        return getPrefs().getString("bhaigram_custom_font_path", "");
    }

    public static void setCustomFontPath(String path) {
        getPrefs().edit().putString("bhaigram_custom_font_path", path).apply();
        setSelectedFont(FONT_CUSTOM_FILE);
    }

    public static Typeface getCustomTypeface(String assetPath) {
        int selected = getSelectedFont();
        if (selected == FONT_DEFAULT) {
            return null; // Fallback to Telegram default asset typeface
        }

        boolean isBold = assetPath != null && (assetPath.contains("bold") || assetPath.contains("medium") || assetPath.contains("rextrabold"));
        boolean isItalic = assetPath != null && assetPath.contains("italic");
        int style = Typeface.NORMAL;
        if (isBold && isItalic) {
            style = Typeface.BOLD_ITALIC;
        } else if (isBold) {
            style = Typeface.BOLD;
        } else if (isItalic) {
            style = Typeface.ITALIC;
        }

        String key = selected + "_" + style;
        Typeface cached = customTypefaceCache.get(key);
        if (cached != null) {
            return cached;
        }

        Typeface result = null;
        try {
            switch (selected) {
                case FONT_SAN_FRANCISCO:
                    result = Typeface.create("sans-serif", style);
                    break;
                case FONT_PRODUCT_SANS:
                    result = Typeface.create("google-sans", style);
                    if (result == null || result == Typeface.DEFAULT) {
                        result = Typeface.create("sans-serif-medium", style);
                    }
                    break;
                case FONT_JETBRAINS_MONO:
                    result = Typeface.create(Typeface.MONOSPACE, style);
                    break;
                case FONT_SAMSUNG_ONE:
                    result = Typeface.create("sans-serif-light", style);
                    break;
                case FONT_COMIC:
                    result = Typeface.create("casual", style);
                    break;
                case FONT_CUSTOM_FILE:
                    String path = getCustomFontPath();
                    if (!path.isEmpty() && new File(path).exists()) {
                        Typeface tf = Typeface.createFromFile(path);
                        result = Typeface.create(tf, style);
                    }
                    break;
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }

        if (result != null) {
            customTypefaceCache.put(key, result);
        }
        return result;
    }

    public static void showFontSelectorDialog(Context context, Runnable onFontChanged) {
        if (context == null) return;

        int currentFont = getSelectedFont();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("App-Wide Font Style");
        builder.setSingleChoiceItems(FONT_NAMES, currentFont, (dialog, which) -> {
            setSelectedFont(which);
            dialog.dismiss();
            Toast.makeText(context, "Font Applied: " + FONT_NAMES[which], Toast.LENGTH_SHORT).show();
            if (onFontChanged != null) {
                onFontChanged.run();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }
}
