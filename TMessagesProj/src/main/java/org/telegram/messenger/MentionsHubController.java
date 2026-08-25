package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;
import java.util.ArrayList;

public class MentionsHubController {

    public static void showMentionsHub(Activity activity, int currentAccount) {
        if (activity == null) return;

        ArrayList<TLRPC.Dialog> allDialogs = MessagesController.getInstance(currentAccount).getAllDialogs();
        final ArrayList<TLRPC.Dialog> mentionDialogs = new ArrayList<>();
        final ArrayList<String> displayTitles = new ArrayList<>();

        if (allDialogs != null) {
            for (TLRPC.Dialog dialog : allDialogs) {
                if (dialog != null && dialog.unread_mentions_count > 0) {
                    mentionDialogs.add(dialog);
                    String name = "Chat " + dialog.id;
                    if (DialogObject.isUserDialog(dialog.id)) {
                        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialog.id);
                        if (user != null) {
                            name = ContactsController.formatName(user.first_name, user.last_name);
                        }
                    } else {
                        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialog.id);
                        if (chat != null) {
                            name = chat.title;
                        }
                    }
                    displayTitles.add("📢 " + name + " (" + dialog.unread_mentions_count + " mentions)");
                }
            }
        }

        if (mentionDialogs.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Mentions & Tag Hub");
            builder.setMessage("No unread mentions found in any group or chat! You are all caught up.");
            builder.setPositiveButton("OK", null);
            builder.show();
            return;
        }

        String[] items = displayTitles.toArray(new String[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("📢 Mentions Hub (" + mentionDialogs.size() + " Active Chats)");
        builder.setItems(items, (dialog, which) -> {
            dialog.dismiss();
            TLRPC.Dialog selected = mentionDialogs.get(which);
            if (activity instanceof LaunchActivity) {
                ((LaunchActivity) activity).presentFragment(ChatActivity.of(selected.id));
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }
}
