package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import java.io.File;
import java.io.FileOutputStream;

public class FightTools {

    private static boolean isRecordingVC = false;
    private static Thread vcRecordThread = null;

    public static void showRepeaterDialog(Activity activity, ChatActivity chatActivity) {
        if (activity == null || chatActivity == null) return;

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(10));

        final EditText textInput = new EditText(activity);
        textInput.setHint("Enter text / rebuttal");
        layout.addView(textInput);

        final EditText countInput = new EditText(activity);
        countInput.setHint("Count (e.g. 5, 10, 20)");
        countInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(countInput);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Fast Text Repeater");
        builder.setView(layout);
        builder.setPositiveButton("Send", (dialog, which) -> {
            String text = textInput.getText().toString().trim();
            String countStr = countInput.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(activity, "Please enter text to send", Toast.LENGTH_SHORT).show();
                return;
            }
            int count = 5;
            try {
                count = Integer.parseInt(countStr);
                if (count <= 0) count = 1;
                if (count > 50) count = 50; // Safety cap
            } catch (Exception ignored) {}

            long dialogId = chatActivity.getDialogId();
            int currentAccount = chatActivity.getCurrentAccount();

            final int finalCount = count;
            final String finalText = text;
            new Thread(() -> {
                for (int i = 0; i < finalCount; i++) {
                    final int idx = i;
                    AndroidUtilities.runOnUIThread(() -> {
                        SendMessagesHelper.getInstance(currentAccount).sendMessage(
                            SendMessagesHelper.SendMessageParams.of(finalText, dialogId)
                        );
                    });
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException ignored) {}
                }
            }).start();

            Toast.makeText(activity, "Sending " + count + " messages...", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

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
                if (!dir.exists()) {
                    dir.mkdirs();
                }
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
