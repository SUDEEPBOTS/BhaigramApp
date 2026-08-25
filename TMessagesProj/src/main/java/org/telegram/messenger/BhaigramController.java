package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

public class BhaigramController {

    private static SharedPreferences getPrefs() {
        return MessagesController.getGlobalMainSettings();
    }

    public static void showVipSettings(Context context, Runnable onUpdate) {
        if (context == null) return;

        String[] options = new String[]{
            "App-Wide Custom Font: " + FontController.FONT_NAMES[FontController.getSelectedFont()],
            "Pro Studio Audio DSP (EQ & Gain): " + (AudioDspProcessor.isStudioModeEnabled() ? "[ENABLED]" : "[DISABLED]"),
            "Voice Changer Mode: " + VoiceChanger.EFFECT_NAMES[VoiceChanger.getDMEffect()],
            "Ghost Mode: " + (getPrefs().getBoolean("ghost_mode", false) ? "[ON]" : "[OFF]"),
            "Anti-Delete Mode: " + (getPrefs().getBoolean("anti_delete_mode", false) ? "[ON]" : "[OFF]"),
            "Anti-Edit Mode: " + (getPrefs().getBoolean("anti_edit_mode", false) ? "[ON]" : "[OFF]"),
            "Anti-Freeze & Anti-Crash Guard: " + (getPrefs().getBoolean("anti_freeze_guard", true) ? "[Active]" : "[OFF]"),
            "Unlimited Multi-Accounts (50 Max): [Active]",
            "One-Time Voice Unlimited & Saver: [Active]",
            "VC Secret Audio Recorder: " + (FightTools.isVcRecording() ? "[RECORDING]" : "[IDLE]"),
            "Clean Forward (No Author Tag): " + (getPrefs().getBoolean("clean_forward_mode", false) ? "[ON]" : "[OFF]"),
            "Send Gallery Video as Round Note: " + (getPrefs().getBoolean("send_video_as_round", false) ? "[ON]" : "[OFF]"),
            "Unlimited View-Once Timer: " + (getPrefs().getBoolean("unlimited_view_once", true) ? "[Active]" : "[OFF]"),
            "Restricted Media Downloader: " + (getPrefs().getBoolean("bypass_restricted_media", true) ? "[Active]" : "[OFF]"),
            "Unlimited Pinned Chats: " + (getPrefs().getBoolean("unlimited_pins", true) ? "[Active]" : "[OFF]")
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Bhaigram VIP Control Center");
        builder.setItems(options, (dialog, which) -> {
            dialog.dismiss();
            switch (which) {
                case 0:
                    FontController.showFontSelectorDialog(context, onUpdate);
                    break;
                case 1:
                    AudioDspProcessor.showDspControlDialog(context, onUpdate);
                    break;
                case 2:
                    VoiceChanger.showVoiceChangerSheet(context, false, onUpdate);
                    break;
                case 3:
                    togglePref("ghost_mode", "Ghost Mode", context, onUpdate);
                    break;
                case 4:
                    togglePref("anti_delete_mode", "Anti-Delete Mode", context, onUpdate);
                    break;
                case 5:
                    togglePref("anti_edit_mode", "Anti-Edit Mode", context, onUpdate);
                    break;
                case 6:
                    togglePref("anti_freeze_guard", "Anti-Freeze & Anti-Crash Guard", context, onUpdate);
                    break;
                case 7:
                    Toast.makeText(context, "Unlimited Accounts enabled: Login up to 50 accounts!", Toast.LENGTH_SHORT).show();
                    break;
                case 8:
                    Toast.makeText(context, "One-Time Voice Saver & Unlimited Player is permanently Active!", Toast.LENGTH_SHORT).show();
                    break;
                case 9:
                    FightTools.toggleVcRecording(context);
                    if (onUpdate != null) onUpdate.run();
                    break;
                case 10:
                    togglePref("clean_forward_mode", "Clean Forward (No Author Tag)", context, onUpdate);
                    break;
                case 11:
                    togglePref("send_video_as_round", "Round Video Note Converter", context, onUpdate);
                    break;
                case 12:
                    togglePref("unlimited_view_once", "Unlimited View-Once Timer", context, onUpdate);
                    break;
                case 13:
                    togglePref("bypass_restricted_media", "Restricted Media Downloader", context, onUpdate);
                    break;
                case 14:
                    togglePref("unlimited_pins", "Unlimited Pinned Chats", context, onUpdate);
                    break;
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private static void togglePref(String key, String title, Context context, Runnable onUpdate) {
        boolean current = getPrefs().getBoolean(key, false);
        getPrefs().edit().putBoolean(key, !current).apply();
        Toast.makeText(context, title + ": " + (!current ? "[ENABLED]" : "[DISABLED]"), Toast.LENGTH_SHORT).show();
        if (onUpdate != null) {
            onUpdate.run();
        }
    }
}
