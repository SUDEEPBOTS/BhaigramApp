package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class FightTools {

    private static boolean isRecordingVC = false;
    private static Thread vcRecordThread = null;
    private static volatile boolean isSpamRunning = false;
    private static Thread spamThread = null;

    // ── 1. Fast Text Spammer / Multi-Line Rebuttal Bomb ──
    public static void showSpammerDialog(Activity activity, ChatActivity chatActivity) {
        if (activity == null || chatActivity == null) return;

        if (isSpamRunning) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Spam Attack in Progress");
            builder.setMessage("A text flood is currently running. Do you want to stop it?");
            builder.setPositiveButton("STOP SPAM", (dialog, which) -> {
                isSpamRunning = false;
                Toast.makeText(activity, "Spam Stopped!", Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
            builder.show();
            return;
        }

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(10));

        final EditText textInput = new EditText(activity);
        textInput.setHint("Enter Text or Multi-Line Rebuttals");
        textInput.setMinLines(3);
        layout.addView(textInput);

        final EditText countInput = new EditText(activity);
        countInput.setHint("Message Count (e.g. 10, 25, 50, 100)");
        countInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        countInput.setText("10");
        layout.addView(countInput);

        final EditText delayInput = new EditText(activity);
        delayInput.setHint("Delay per message in ms (e.g. 100, 250, 500)");
        delayInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        delayInput.setText("150");
        layout.addView(delayInput);

        final CheckBox randomCheck = new CheckBox(activity);
        randomCheck.setText("Randomize Multi-Line Rebuttals");
        layout.addView(randomCheck);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("⚡ Fast Text Spammer & Raider");
        builder.setView(layout);
        builder.setPositiveButton("START RAID", (dialog, which) -> {
            String text = textInput.getText().toString().trim();
            String countStr = countInput.getText().toString().trim();
            String delayStr = delayInput.getText().toString().trim();

            if (TextUtils.isEmpty(text)) {
                Toast.makeText(activity, "Please enter message text", Toast.LENGTH_SHORT).show();
                return;
            }

            int count = 10;
            int delayMs = 150;
            try {
                count = Math.max(1, Math.min(150, Integer.parseInt(countStr)));
            } catch (Exception ignored) {}
            try {
                delayMs = Math.max(50, Math.min(2000, Integer.parseInt(delayStr)));
            } catch (Exception ignored) {}

            final String[] lines = text.split("\n");
            final boolean randomize = randomCheck.isChecked();
            final int finalCount = count;
            final int finalDelay = delayMs;
            final long dialogId = chatActivity.getDialogId();
            final int currentAccount = chatActivity.getCurrentAccount();

            isSpamRunning = true;
            spamThread = new Thread(() -> {
                Random rand = new Random();
                for (int i = 0; i < finalCount && isSpamRunning; i++) {
                    String msgToSend;
                    if (lines.length > 1) {
                        msgToSend = randomize ? lines[rand.nextInt(lines.length)] : lines[i % lines.length];
                    } else {
                        msgToSend = text;
                    }

                    final String msg = msgToSend.trim();
                    if (!TextUtils.isEmpty(msg)) {
                        AndroidUtilities.runOnUIThread(() -> {
                            SendMessagesHelper.getInstance(currentAccount).sendMessage(
                                SendMessagesHelper.SendMessageParams.of(msg, dialogId)
                            );
                        });
                    }
                    try {
                        Thread.sleep(finalDelay);
                    } catch (InterruptedException ignored) {
                        break;
                    }
                }
                isSpamRunning = false;
            });
            spamThread.start();

            Toast.makeText(activity, "Raid started: Sending " + count + " messages!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    // ── 2. @all / @everyone Silent Group Mention Tool ──
    public static void showMentionAllDialog(Activity activity, ChatActivity chatActivity) {
        if (activity == null || chatActivity == null) return;

        final long dialogId = chatActivity.getDialogId();
        final int currentAccount = chatActivity.getCurrentAccount();

        TLRPC.Chat chat = chatActivity.getCurrentChat();
        if (chat == null) {
            Toast.makeText(activity, "Mention All is only available in Group Chats", Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText calloutInput = new EditText(activity);
        calloutInput.setHint("Announcement message (e.g. Wake up boys!)");
        calloutInput.setText("📢 ATTENTION EVERYONE:");

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("📢 Tag All Group Members (@all)");
        builder.setView(calloutInput);
        builder.setPositiveButton("TAG ALL", (dialog, which) -> {
            String callout = calloutInput.getText().toString().trim();
            Toast.makeText(activity, "Fetching group members to tag...", Toast.LENGTH_SHORT).show();

            // Load participants from cache/MessagesController
            MessagesController.getInstance(currentAccount).loadChannelParticipants(chat.id, 0, 200, 0, (response, error) -> {
                if (response instanceof TLRPC.TL_channels_channelParticipants) {
                    TLRPC.TL_channels_channelParticipants participants = (TLRPC.TL_channels_channelParticipants) response;
                    ArrayList<String> mentions = new ArrayList<>();
                    for (TLRPC.User user : participants.users) {
                        if (user != null && !user.bot && !user.self) {
                            if (!TextUtils.isEmpty(user.username)) {
                                mentions.add("@" + user.username);
                            } else if (!TextUtils.isEmpty(user.first_name)) {
                                mentions.add("<a href=\"tg://user?id=" + user.id + "\">" + user.first_name + "</a>");
                            }
                        }
                    }

                    if (mentions.isEmpty()) {
                        Toast.makeText(activity, "No active members found to tag", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Send chunked 5 mentions per message
                    new Thread(() -> {
                        for (int i = 0; i < mentions.size(); i += 5) {
                            int end = Math.min(i + 5, mentions.size());
                            StringBuilder sb = new StringBuilder();
                            sb.append(callout).append("\n\n");
                            for (int j = i; j < end; j++) {
                                sb.append(mentions.get(j)).append(" ");
                            }
                            final String fullMsg = sb.toString();
                            AndroidUtilities.runOnUIThread(() -> {
                                SendMessagesHelper.getInstance(currentAccount).sendMessage(
                                    SendMessagesHelper.SendMessageParams.of(fullMsg, dialogId)
                                );
                            });
                            try {
                                Thread.sleep(400);
                            } catch (InterruptedException ignored) {}
                        }
                        AndroidUtilities.runOnUIThread(() -> {
                            Toast.makeText(activity, "Tagged " + mentions.size() + " members successfully!", Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                } else {
                    Toast.makeText(activity, "Unable to fetch member list. Admin rights may be required.", Toast.LENGTH_SHORT).show();
                }
            });
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    // ── 3. Auto Reaction Raider ──
    public static void showAutoReactionDialog(Context context, Runnable onUpdate) {
        if (context == null) return;

        boolean isEnabled = MessagesController.getGlobalMainSettings().getBoolean("auto_reaction_enabled", false);
        String targetUserId = MessagesController.getGlobalMainSettings().getString("auto_reaction_target_id", "");
        String targetEmoji = MessagesController.getGlobalMainSettings().getString("auto_reaction_emoji", "🤡");

        String[] options = new String[]{
            "Auto Reaction Raider: " + (isEnabled ? "[ACTIVE]" : "[DISABLED]"),
            "Target User ID: " + (targetUserId.isEmpty() ? "[NOT SET]" : targetUserId),
            "Selected Emoji: " + targetEmoji,
            "Emoji: 🤡 Clown",
            "Emoji: 🤮 Vomit",
            "Emoji: 💀 Skull",
            "Emoji: 🔥 Fire",
            "Emoji: ⚡ Lightning",
            "Emoji: 🖕 Middle Finger",
            "Emoji: 🚀 Rocket"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Auto Reaction Raider");
        builder.setItems(options, (dialog, which) -> {
            dialog.dismiss();
            switch (which) {
                case 0:
                    MessagesController.getGlobalMainSettings().edit().putBoolean("auto_reaction_enabled", !isEnabled).apply();
                    Toast.makeText(context, "Auto Reaction: " + (!isEnabled ? "ENABLED" : "DISABLED"), Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    promptTargetUserId(context, onUpdate);
                    return;
                case 2:
                case 3:
                    MessagesController.getGlobalMainSettings().edit().putString("auto_reaction_emoji", "🤡").apply();
                    break;
                case 4:
                    MessagesController.getGlobalMainSettings().edit().putString("auto_reaction_emoji", "🤮").apply();
                    break;
                case 5:
                    MessagesController.getGlobalMainSettings().edit().putString("auto_reaction_emoji", "💀").apply();
                    break;
                case 6:
                    MessagesController.getGlobalMainSettings().edit().putString("auto_reaction_emoji", "🔥").apply();
                    break;
                case 7:
                    MessagesController.getGlobalMainSettings().edit().putString("auto_reaction_emoji", "⚡").apply();
                    break;
                case 8:
                    MessagesController.getGlobalMainSettings().edit().putString("auto_reaction_emoji", "🖕").apply();
                    break;
                case 9:
                    MessagesController.getGlobalMainSettings().edit().putString("auto_reaction_emoji", "🚀").apply();
                    break;
            }
            if (onUpdate != null) onUpdate.run();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private static void promptTargetUserId(Context context, Runnable onUpdate) {
        final EditText input = new EditText(context);
        input.setHint("Enter Target User Telegram ID");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Set Target User ID");
        builder.setView(input);
        builder.setPositiveButton("Set", (dialog, which) -> {
            String uid = input.getText().toString().trim();
            if (!TextUtils.isEmpty(uid)) {
                MessagesController.getGlobalMainSettings().edit().putString("auto_reaction_target_id", uid).putBoolean("auto_reaction_enabled", true).apply();
                Toast.makeText(context, "Target ID set to: " + uid, Toast.LENGTH_SHORT).show();
                if (onUpdate != null) onUpdate.run();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    // ── 4. Group Members Exporter (.txt) ──
    public static void exportGroupMembers(Activity activity, ChatActivity chatActivity) {
        if (activity == null || chatActivity == null) return;
        TLRPC.Chat chat = chatActivity.getCurrentChat();
        if (chat == null) {
            Toast.makeText(activity, "Only available in Group Chats", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentAccount = chatActivity.getCurrentAccount();
        Toast.makeText(activity, "Exporting member list...", Toast.LENGTH_SHORT).show();

        MessagesController.getInstance(currentAccount).loadChannelParticipants(chat.id, 0, 500, 0, (response, error) -> {
            if (response instanceof TLRPC.TL_channels_channelParticipants) {
                TLRPC.TL_channels_channelParticipants participants = (TLRPC.TL_channels_channelParticipants) response;
                new Thread(() -> {
                    try {
                        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Bhaichara_Exports");
                        if (!dir.exists()) dir.mkdirs();

                        File file = new File(dir, "Members_" + chat.title.replaceAll("[^a-zA-Z0-9.-]", "_") + "_" + System.currentTimeMillis() + ".txt");
                        FileWriter writer = new FileWriter(file);
                        writer.write("=== BHAICHARA GROUP MEMBERS EXPORT ===\n");
                        writer.write("Group: " + chat.title + " (ID: " + chat.id + ")\n");
                        writer.write("Total Extracted: " + participants.users.size() + "\n\n");

                        for (TLRPC.User u : participants.users) {
                            if (u != null) {
                                writer.write("ID: " + u.id + " | Username: @" + (u.username != null ? u.username : "None") +
                                        " | Name: " + u.first_name + " " + (u.last_name != null ? u.last_name : "") +
                                        (u.bot ? " [BOT]" : "") + "\n");
                            }
                        }
                        writer.close();

                        AndroidUtilities.runOnUIThread(() -> {
                            Toast.makeText(activity, "Exported " + participants.users.size() + " members to Downloads/Bhaichara_Exports/" + file.getName(), Toast.LENGTH_LONG).show();
                        });
                    } catch (Exception e) {
                        FileLog.e(e);
                        AndroidUtilities.runOnUIThread(() -> {
                            Toast.makeText(activity, "Failed to save export file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            } else {
                Toast.makeText(activity, "Failed to retrieve member list", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── 5. VC Secret Recording ──
    public static synchronized boolean isVcRecording() {
        return isRecordingVC;
    }

    public static synchronized void toggleVcRecording(Context context) {
        if (isRecordingVC) {
            isRecordingVC = false;
            Toast.makeText(context, "VC Secret Recording Stopped & Saved to Downloads/Bhaigram_Records", Toast.LENGTH_LONG).show();
        } else {
            isRecordingVC = true;
            startSilentVcRecording(context);
            Toast.makeText(context, "VC Secret Recording Started (Silent Background Mode)", Toast.LENGTH_LONG).show();
        }
    }

    private static void startSilentVcRecording(Context context) {
        vcRecordThread = new Thread(() -> {
            try {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Bhaigram_Records");
                if (!dir.exists()) dir.mkdirs();
                File outFile = new File(dir, "VC_Record_" + System.currentTimeMillis() + ".pcm");
                FileOutputStream fos = new FileOutputStream(outFile);

                int sampleRate = 44100;
                int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

                byte[] buffer = new byte[bufferSize];
                recorder.startRecording();

                while (isRecordingVC) {
                    int read = recorder.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        fos.write(buffer, 0, read);
                    }
                }

                recorder.stop();
                recorder.release();
                fos.close();
            } catch (Throwable t) {
                FileLog.e(t);
                isRecordingVC = false;
            }
        });
        vcRecordThread.start();
    }
}
