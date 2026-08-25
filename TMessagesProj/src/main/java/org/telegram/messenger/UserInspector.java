package org.telegram.messenger;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;

public class UserInspector {

    public static String getDcString(int dcId) {
        switch (dcId) {
            case 1:
                return "DC1 (Miami, USA)";
            case 2:
                return "DC2 (Amsterdam, Netherlands)";
            case 3:
                return "DC3 (Miami, USA)";
            case 4:
                return "DC4 (Amsterdam, Netherlands)";
            case 5:
                return "DC5 (Singapore, Asia)";
            default:
                return dcId > 0 ? "DC" + dcId : "Unknown DC";
        }
    }

    public static int getUserDc(TLRPC.User user) {
        if (user == null || user.photo == null) {
            return 0;
        }
        if (user.photo.dc_id != 0) {
            return user.photo.dc_id;
        }
        if (user.photo.photo_big != null && user.photo.photo_big.dc_id != 0) {
            return user.photo.photo_big.dc_id;
        }
        if (user.photo.photo_small != null && user.photo.photo_small.dc_id != 0) {
            return user.photo.photo_small.dc_id;
        }
        return 0;
    }

    public static void copyToClipboard(Context context, String text, String label) {
        if (context == null || text == null) return;
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Copied " + label + ": " + text, Toast.LENGTH_SHORT).show();
        }
    }

    public static void showInspector(Context context, TLRPC.User user) {
        if (context == null || user == null) return;

        int dc = getUserDc(user);
        String dcText = getDcString(dc);
        String idStr = String.valueOf(user.id);
        String username = UserObject.getPublicUsername(user);
        String usernameStr = username != null ? "@" + username : "None";
        String permanentLink = "tg://user?id=" + user.id;

        String[] options = new String[]{
            "User ID: " + idStr + " (Tap to copy)",
            "Data Center: " + dcText,
            "Username: " + usernameStr + " (Tap to copy)",
            "Permanent Link: " + permanentLink + " (Tap to copy)",
            "Is Bot: " + (user.bot ? "Yes" : "No"),
            "Is Verified: " + (user.verified ? "Yes" : "No"),
            "Is Premium: " + (user.premium ? "Yes" : "No"),
            "Is Scam/Fake: " + (user.scam || user.fake ? "Yes" : "No")
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("User Inspector: " + UserObject.getUserName(user));
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    copyToClipboard(context, idStr, "User ID");
                    break;
                case 1:
                    copyToClipboard(context, dcText, "Data Center");
                    break;
                case 2:
                    if (username != null) {
                        copyToClipboard(context, usernameStr, "Username");
                    }
                    break;
                case 3:
                    copyToClipboard(context, permanentLink, "Permanent Link");
                    break;
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }
}
