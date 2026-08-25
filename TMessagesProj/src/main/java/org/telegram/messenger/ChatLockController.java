package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ChatLockController {

    private static final Set<Long> unlockedSessionChats = new HashSet<>();
    private static boolean revealingHiddenChats = false;

    private static SharedPreferences getPrefs() {
        try {
            return MessagesController.getGlobalMainSettings();
        } catch (Throwable e) {
            return null;
        }
    }

    public static boolean isRevealingHiddenChats() {
        return revealingHiddenChats;
    }

    public static void setRevealingHiddenChats(boolean revealing) {
        revealingHiddenChats = revealing;
    }

    public static boolean isChatHidden(long dialogId) {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                Set<String> hiddenSet = prefs.getStringSet("bhaichara_hidden_chats", new HashSet<>());
                return hiddenSet.contains(String.valueOf(dialogId));
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static void toggleChatHide(Activity activity, long dialogId, Runnable onComplete) {
        if (activity == null) return;
        boolean currentlyHidden = isChatHidden(dialogId);

        promptPin(activity, currentlyHidden ? "Enter PIN to Unhide Chat" : "Enter PIN to Hide Chat", true, false, enteredPin -> {
            if (validatePin(enteredPin)) {
                if (currentlyHidden) {
                    removeHiddenChat(dialogId);
                    Toast.makeText(activity, "Chat Unhidden!", Toast.LENGTH_SHORT).show();
                } else {
                    addHiddenChat(dialogId);
                    addLockedChat(dialogId); // Hidden chats are also PIN locked
                    Toast.makeText(activity, "Chat Hidden from main list!", Toast.LENGTH_SHORT).show();
                }
                if (onComplete != null) onComplete.run();
            } else {
                Toast.makeText(activity, "Incorrect PIN!", Toast.LENGTH_SHORT).show();
            }
        }, null);
    }

    public static boolean isChatLocked(long dialogId) {
        try {
            if (unlockedSessionChats.contains(dialogId)) {
                return false;
            }
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                Set<String> lockedSet = prefs.getStringSet("bhaichara_locked_chats", new HashSet<>());
                return lockedSet.contains(String.valueOf(dialogId)) || isChatHidden(dialogId);
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static void toggleChatLock(Activity activity, long dialogId, Runnable onComplete) {
        if (activity == null) return;
        boolean currentlyLocked = isChatLocked(dialogId) || isDirectlyLocked(dialogId);

        if (currentlyLocked) {
            promptPin(activity, "Enter PIN to Unlock Chat", true, false, enteredPin -> {
                if (validatePin(enteredPin)) {
                    removeLockedChat(dialogId);
                    removeHiddenChat(dialogId);
                    unlockedSessionChats.remove(dialogId);
                    Toast.makeText(activity, "Chat Unlocked!", Toast.LENGTH_SHORT).show();
                    if (onComplete != null) onComplete.run();
                } else {
                    Toast.makeText(activity, "Incorrect PIN!", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } else {
            if (!hasMasterPin()) {
                promptPin(activity, "Set 4-Digit Master PIN", false, false, newPin -> {
                    if (newPin.length() >= 4) {
                        setMasterPin(newPin);
                        addLockedChat(dialogId);
                        Toast.makeText(activity, "Master PIN set & Chat Locked!", Toast.LENGTH_SHORT).show();
                        if (onComplete != null) onComplete.run();
                    } else {
                        Toast.makeText(activity, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show();
                    }
                }, null);
            } else {
                addLockedChat(dialogId);
                unlockedSessionChats.remove(dialogId);
                Toast.makeText(activity, "Chat Locked with PIN!", Toast.LENGTH_SHORT).show();
                if (onComplete != null) onComplete.run();
            }
        }
    }

    public static void verifyPinToOpen(BaseFragment fragment, long dialogId, Runnable onSuccess) {
        if (fragment == null || fragment.getParentActivity() == null || !isChatLocked(dialogId)) {
            if (onSuccess != null) onSuccess.run();
            return;
        }

        Activity activity = fragment.getParentActivity();
        promptPin(activity, "🔒 Enter PIN to Open Chat", true, true, enteredPin -> {
            if (validatePin(enteredPin)) {
                unlockedSessionChats.add(dialogId);
                if (onSuccess != null) onSuccess.run();
            } else {
                Toast.makeText(activity, "Incorrect PIN!", Toast.LENGTH_SHORT).show();
                fragment.finishFragment();
            }
        }, () -> {
            fragment.finishFragment();
        });
    }

    public static ArrayList<org.telegram.tgnet.TLRPC.Dialog> filterHiddenDialogs(ArrayList<org.telegram.tgnet.TLRPC.Dialog> dialogs) {
        if (dialogs == null || revealingHiddenChats) {
            return dialogs;
        }
        ArrayList<org.telegram.tgnet.TLRPC.Dialog> result = new ArrayList<>();
        for (int i = 0; i < dialogs.size(); i++) {
            org.telegram.tgnet.TLRPC.Dialog d = dialogs.get(i);
            if (d != null && !isChatHidden(d.id)) {
                result.add(d);
            }
        }
        return result;
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

    private static void addHiddenChat(long dialogId) {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                Set<String> set = new HashSet<>(prefs.getStringSet("bhaichara_hidden_chats", new HashSet<>()));
                set.add(String.valueOf(dialogId));
                prefs.edit().putStringSet("bhaichara_hidden_chats", set).apply();
            }
        } catch (Throwable ignored) {}
    }

    private static void removeHiddenChat(long dialogId) {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                Set<String> set = new HashSet<>(prefs.getStringSet("bhaichara_hidden_chats", new HashSet<>()));
                set.remove(String.valueOf(dialogId));
                prefs.edit().putStringSet("bhaichara_hidden_chats", set).apply();
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
                return !TextUtils.isEmpty(saved) && saved.equals(pin);
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

    public static void promptPin(Context context, String title, boolean checkExisting, boolean forceModal, PinCallback successCallback, Runnable cancelCallback) {
        if (context == null) return;
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Enter 4-digit PIN");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(12));
        layout.addView(input);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setView(layout);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String pin = input.getText().toString().trim();
            if (successCallback != null) successCallback.onPinEntered(pin);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), (dialog, which) -> {
            if (cancelCallback != null) cancelCallback.run();
        });
        builder.setOnCancelListener(dialog -> {
            if (cancelCallback != null) cancelCallback.run();
        });

        AlertDialog dialog = builder.create();
        if (forceModal) {
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
        }
        dialog.show();
    }
}
