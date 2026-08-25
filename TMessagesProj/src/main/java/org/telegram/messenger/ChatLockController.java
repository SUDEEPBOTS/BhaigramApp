package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;
import org.telegram.ui.ActionBar.AlertDialog;
import java.util.HashSet;
import java.util.Set;

public class ChatLockController {

    private static final Set<Long> unlockedSessionChats = new HashSet<>();

    private static SharedPreferences getPrefs() {
        try {
            return MessagesController.getGlobalMainSettings();
        } catch (Throwable e) {
            return null;
        }
    }

    public static boolean isChatLocked(long dialogId) {
        try {
            if (unlockedSessionChats.contains(dialogId)) {
                return false;
            }
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                Set<String> lockedSet = prefs.getStringSet("bhaichara_locked_chats", new HashSet<>());
                return lockedSet.contains(String.valueOf(dialogId));
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static void toggleChatLock(Activity activity, long dialogId, Runnable onComplete) {
        if (activity == null) return;
        boolean currentlyLocked = isChatLocked(dialogId) || isDirectlyLocked(dialogId);

        if (currentlyLocked) {
            // Unlock
            promptPin(activity, "Enter PIN to Unlock Chat", enteredPin -> {
                if (validatePin(enteredPin)) {
                    removeLockedChat(dialogId);
                    unlockedSessionChats.remove(dialogId);
                    Toast.makeText(activity, "Chat Unlocked!", Toast.LENGTH_SHORT).show();
                    if (onComplete != null) onComplete.run();
                } else {
                    Toast.makeText(activity, "Incorrect PIN!", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Check if master PIN is set
            if (!hasMasterPin()) {
                promptPin(activity, "Set 4-Digit Master PIN", newPin -> {
                    if (newPin.length() >= 4) {
                        setMasterPin(newPin);
                        addLockedChat(dialogId);
                        Toast.makeText(activity, "Master PIN set & Chat Locked!", Toast.LENGTH_SHORT).show();
                        if (onComplete != null) onComplete.run();
                    } else {
                        Toast.makeText(activity, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                addLockedChat(dialogId);
                unlockedSessionChats.remove(dialogId);
                Toast.makeText(activity, "Chat Locked with PIN!", Toast.LENGTH_SHORT).show();
                if (onComplete != null) onComplete.run();
            }
        }
    }

    public static void verifyPinToOpen(Activity activity, long dialogId, Runnable onSuccess) {
        if (activity == null || !isChatLocked(dialogId)) {
            if (onSuccess != null) onSuccess.run();
            return;
        }

        promptPin(activity, "🔒 Enter PIN to Open Chat", enteredPin -> {
            if (validatePin(enteredPin)) {
                unlockedSessionChats.add(dialogId);
                if (onSuccess != null) onSuccess.run();
            } else {
                Toast.makeText(activity, "Incorrect PIN!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static boolean isDirectlyLocked(long dialogId) {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                Set<String> set = prefs.getStringSet("bhaichara_locked_chats", new HashSet<>());
                return set.contains(String.valueOf(dialogId));
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static void addLockedChat(long dialogId) {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                Set<String> set = new HashSet<>(prefs.getStringSet("bhaichara_locked_chats", new HashSet<>()));
                set.add(String.valueOf(dialogId));
                prefs.edit().putStringSet("bhaichara_locked_chats", set).apply();
            }
        } catch (Throwable ignored) {}
    }

    private static void removeLockedChat(long dialogId) {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                Set<String> set = new HashSet<>(prefs.getStringSet("bhaichara_locked_chats", new HashSet<>()));
                set.remove(String.valueOf(dialogId));
                prefs.edit().putStringSet("bhaichara_locked_chats", set).apply();
            }
        } catch (Throwable ignored) {}
    }

    public static boolean hasMasterPin() {
        try {
            SharedPreferences prefs = getPrefs();
            return prefs != null && !TextUtils.isEmpty(prefs.getString("bhaichara_chat_pin", ""));
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean validatePin(String pin) {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                String saved = prefs.getString("bhaichara_chat_pin", "");
                return saved.equals(pin);
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static void setMasterPin(String pin) {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                prefs.edit().putString("bhaichara_chat_pin", pin).apply();
            }
        } catch (Throwable ignored) {}
    }

    public interface PinCallback {
        void onPinEntered(String pin);
    }

    private static void promptPin(Context context, String title, PinCallback callback) {
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Enter 4-digit PIN");

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String pin = input.getText().toString().trim();
            if (callback != null) callback.onPinEntered(pin);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }
}
