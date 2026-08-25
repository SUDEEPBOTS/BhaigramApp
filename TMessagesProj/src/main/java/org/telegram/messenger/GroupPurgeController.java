package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import java.util.ArrayList;

public class GroupPurgeController {

    public static void showPurgeConfirmDialog(Activity activity, ChatActivity chatActivity) {
        if (activity == null || chatActivity == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Purge All My Messages");
        builder.setMessage("Are you sure you want to delete ALL messages sent by you in this group? This action cannot be undone.");
        builder.setPositiveButton("Delete All", (dialog, which) -> {
            dialog.dismiss();
            purgeMessages(activity, chatActivity);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private static void purgeMessages(Activity activity, ChatActivity chatActivity) {
        long dialogId = chatActivity.getDialogId();
        int currentAccount = chatActivity.getCurrentAccount();
        long myId = UserConfig.getInstance(currentAccount).getClientUserId();
        TLRPC.Chat currentChat = chatActivity.getCurrentChat();

        Toast.makeText(activity, "Purging all your messages from group...", Toast.LENGTH_SHORT).show();

        // 1. If admin, try channels.deleteParticipantHistory directly
        if (currentChat != null && ChatObject.isChannel(currentChat)) {
            TLRPC.TL_channels_deleteParticipantHistory req = new TLRPC.TL_channels_deleteParticipantHistory();
            req.channel = MessagesController.getInstance(currentAccount).getInputChannel(currentChat);
            req.participant = new TLRPC.TL_inputPeerSelf();
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                if (error == null) {
                    AndroidUtilities.runOnUIThread(() -> {
                        Toast.makeText(activity, "All your messages have been purged from group!", Toast.LENGTH_LONG).show();
                        MessagesStorage.getInstance(currentAccount).deleteUserChatHistory(dialogId, myId);
                    });
                } else {
                    // Fallback to client-side iterative batch deletion
                    searchAndDeleteBatch(currentAccount, dialogId, myId, currentChat, 0, activity);
                }
            });
        } else {
            // Normal Group batch deletion
            searchAndDeleteBatch(currentAccount, dialogId, myId, currentChat, 0, activity);
        }
    }

    private static void searchAndDeleteBatch(int currentAccount, long dialogId, long myId, TLRPC.Chat chat, int offsetId, Context context) {
        TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
        req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
        req.from_id = new TLRPC.TL_inputPeerSelf();
        req.q = "";
        req.offset_id = offsetId;
        req.limit = 100;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (error == null && response instanceof TLRPC.messages_Messages) {
                TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                ArrayList<Integer> ids = new ArrayList<>();
                for (TLRPC.Message msg : res.messages) {
                    if (msg != null && msg.id > 0) {
                        ids.add(msg.id);
                    }
                }

                if (!ids.isEmpty()) {
                    if (chat != null && ChatObject.isChannel(chat)) {
                        TLRPC.TL_channels_deleteMessages delReq = new TLRPC.TL_channels_deleteMessages();
                        delReq.channel = MessagesController.getInstance(currentAccount).getInputChannel(chat);
                        delReq.id = ids;
                        ConnectionsManager.getInstance(currentAccount).sendRequest(delReq, null);
                    } else {
                        TLRPC.TL_messages_deleteMessages delReq = new TLRPC.TL_messages_deleteMessages();
                        delReq.id = ids;
                        delReq.revoke = true;
                        ConnectionsManager.getInstance(currentAccount).sendRequest(delReq, null);
                    }

                    int lastId = ids.get(ids.size() - 1);
                    if (res.messages.size() >= 100) {
                        searchAndDeleteBatch(currentAccount, dialogId, myId, chat, lastId, context);
                    } else {
                        AndroidUtilities.runOnUIThread(() -> {
                            MessagesStorage.getInstance(currentAccount).deleteUserChatHistory(dialogId, myId);
                            Toast.makeText(context, "All your messages deleted successfully!", Toast.LENGTH_LONG).show();
                        });
                    }
                } else {
                    AndroidUtilities.runOnUIThread(() -> {
                        MessagesStorage.getInstance(currentAccount).deleteUserChatHistory(dialogId, myId);
                        Toast.makeText(context, "All your messages deleted!", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
}
