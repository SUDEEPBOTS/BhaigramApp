package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;
import org.telegram.ui.ActionBar.AlertDialog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SpamFilter {

    private static final Set<String> defaultSpamKeywords = new HashSet<>(Arrays.asList(
        "casino", "betting", "100% profit", "free bonus", "crypto pump", "airdrop claim", "earn money daily"
    ));

    private static SharedPreferences getPrefs() {
        return MessagesController.getGlobalMainSettings();
    }

    public static boolean isSpamFilterEnabled() {
        return getPrefs().getBoolean("spam_filter_enabled", false);
    }

    public static void setSpamFilterEnabled(boolean enabled) {
        getPrefs().edit().putBoolean("spam_filter_enabled", enabled).apply();
    }

    public static Set<String> getCustomKeywords() {
        return getPrefs().getStringSet("spam_custom_keywords", new HashSet<>());
    }

    public static void addCustomKeyword(String keyword) {
        if (TextUtils.isEmpty(keyword)) return;
        Set<String> set = new HashSet<>(getCustomKeywords());
        set.add(keyword.toLowerCase().trim());
        getPrefs().edit().putStringSet("spam_custom_keywords", set).apply();
    }

    public static void clearKeywords() {
        getPrefs().edit().remove("spam_custom_keywords").apply();
    }

    public static boolean isSpam(CharSequence text) {
        if (!isSpamFilterEnabled() || TextUtils.isEmpty(text)) {
            return false;
        }

        String lower = text.toString().toLowerCase();

        for (String kw : defaultSpamKeywords) {
            if (lower.contains(kw)) {
                return true;
            }
        }

        for (String kw : getCustomKeywords()) {
            if (lower.contains(kw)) {
                return true;
            }
        }

        return false;
    }

    public static void showSpamFilterDialog(Context context, Runnable onUpdate) {
        if (context == null) return;

        String[] options = new String[]{
            "Spam Filter: " + (isSpamFilterEnabled() ? "[ENABLED]" : "[DISABLED]"),
            "Add Custom Blocked Word",
            "View / Clear Custom Blocked Words (" + getCustomKeywords().size() + ")",
            "Reset to Default Keyword List"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Smart Spam & Ad Filter");
        builder.setItems(options, (dialog, which) -> {
            dialog.dismiss();
            switch (which) {
                case 0:
                    setSpamFilterEnabled(!isSpamFilterEnabled());
                    Toast.makeText(context, "Spam Filter: " + (isSpamFilterEnabled() ? "ENABLED" : "DISABLED"), Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    showAddKeywordDialog(context, onUpdate);
                    return;
                case 2:
                    clearKeywords();
                    Toast.makeText(context, "Custom keywords cleared", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    clearKeywords();
                    setSpamFilterEnabled(true);
                    Toast.makeText(context, "Reset to default spam keyword protection", Toast.LENGTH_SHORT).show();
                    break;
            }
            if (onUpdate != null) onUpdate.run();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private static void showAddKeywordDialog(Context context, Runnable onUpdate) {
        final EditText input = new EditText(context);
        input.setHint("Enter keyword / phrase to auto-block");

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Add Blocked Word");
        builder.setView(input);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String word = input.getText().toString().trim();
            if (!TextUtils.isEmpty(word)) {
                setSpamFilterEnabled(true);
                addCustomKeyword(word);
                Toast.makeText(context, "Added blocked word: " + word, Toast.LENGTH_SHORT).show();
                if (onUpdate != null) onUpdate.run();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }
}
