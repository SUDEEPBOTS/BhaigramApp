package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

public class BhaigramController {

    private static SharedPreferences getPrefs() {
        try {
            return MessagesController.getGlobalMainSettings();
        } catch (Throwable e) {
            return null;
        }
    }

    public static void checkAndShowFirstTimeWelcome(Activity activity) {
        try {
            if (activity == null || activity.isFinishing()) return;
            if (android.os.Build.VERSION.SDK_INT >= 17 && activity.isDestroyed()) return;
            boolean alreadyShown = getPrefs() != null && getPrefs().getBoolean("first_time_welcome_shown", false);
            if (alreadyShown) return;

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("WELCOME TO BHAICHARA");
            builder.setMessage(
                "Welcome to Bhaichara - The Ultimate Super Edition!\n\n" +
                "Thanks for downloading Bhaichara.\n\n" +
                "[+] Pro Studio Audio DSP: 10-Band EQ & +36dB Overdrive\n" +
                "[+] Voice Changer: 13 DM & Live VC Vocal Effects\n" +
                "[+] Stealth Privacy: Invisible Ghost & Anti-Delete\n" +
                "[+] Glass Chat Customizer: Bubble Opacity & Radius\n" +
                "[+] Raider & Spammer: Fast Text Bomb & @all Mentioner\n" +
                "[+] Private Chat Vault: PIN Lock for Individual Chats\n" +
                "[+] Anti-Story Delete & Stealth Story Downloader\n" +
                "[+] Media Saver: Restricted Channel Media Downloader\n" +
                "[+] Turbo Engine: 16x Multi-Thread Download Booster\n" +
                "[+] Privacy Mask: Hide Phone Number\n\n" +
                "Developed with passion by SUDEEP\n" +
                "GitHub: github.com/SUDEEPBOTS"
            );
            builder.setPositiveButton("ENTER BHAICHARA", (dialog, which) -> {
                if (getPrefs() != null) {
                    getPrefs().edit().putBoolean("first_time_welcome_shown", true).apply();
                }
                dialog.dismiss();
                Toast.makeText(activity, "Welcome to Bhaichara!", Toast.LENGTH_SHORT).show();
            });
            builder.setNeutralButton("OPEN GITHUB", (dialog, which) -> {
                if (getPrefs() != null) {
                    getPrefs().edit().putBoolean("first_time_welcome_shown", true).apply();
                }
                dialog.dismiss();
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/SUDEEPBOTS"));
                    activity.startActivity(browserIntent);
                } catch (Exception ignored) {}
            });
            builder.show();
        } catch (Throwable ignored) {}
    }

    public static void showVipSettings(Context context, Runnable onUpdate) {
        try {
            if (context == null) return;

            String[] options = new String[]{
                "📢 Mentions & Tag Hub (All Groups Dashboard)",
                "🎨 Glass Chat & Room Customizer (Opacity & Radius)",
                "✨ 40+ Fancy Text Styles & Caps: " + FancyTextTransformer.STYLE_NAMES[FancyTextTransformer.getSelectedStyle()],
                "App-Wide Custom Font: " + FontController.FONT_NAMES[FontController.getSelectedFont()],
                "Pro Studio Audio DSP (EQ & Gain): " + (AudioDspProcessor.isStudioModeEnabled() ? "[ENABLED]" : "[DISABLED]"),
                "💥 VC Danger Mic Overdrive (+36dB): " + (AudioDspProcessor.isDangerModeEnabled() ? "[ACTIVE ⚠️]" : "[OFF]"),
                "🎭 Auto Reaction Raider: " + (getPrefs() != null && getPrefs().getBoolean("auto_reaction_enabled", false) ? "[ACTIVE]" : "[OFF]"),
                "16x Download Speed Booster: " + (getPrefs() != null && getPrefs().getBoolean("download_booster", true) ? "[MAX SPEED]" : "[DEFAULT]"),
                "Hide Phone Number (Privacy Mask): " + (getPrefs() != null && getPrefs().getBoolean("hide_phone_number", false) ? "[MASKED]" : "[VISIBLE]"),
                "Exact Message Timestamps (Seconds): " + (getPrefs() != null && getPrefs().getBoolean("exact_timestamp_seconds", false) ? "[ON (HH:mm:ss)]" : "[OFF]"),
                "Smart Spam & Ad Keyword Filter: " + (SpamFilter.isSpamFilterEnabled() ? "[ENABLED]" : "[DISABLED]"),
                "Voice Changer Mode: " + VoiceChanger.EFFECT_NAMES[VoiceChanger.getDMEffect()],
                "Ghost Mode (Invisible Checks & Typing): " + (getPrefs() != null && getPrefs().getBoolean("ghost_mode", false) ? "[ON]" : "[OFF]"),
                "Anti-Delete Mode (Preserve Deleted Messages): " + (getPrefs() != null && getPrefs().getBoolean("anti_delete_mode", false) ? "[ON]" : "[OFF]"),
                "Anti-Edit Mode (Show Edit History): " + (getPrefs() != null && getPrefs().getBoolean("anti_edit_mode", false) ? "[ON]" : "[OFF]"),
                "Anti-Story Delete & Stealth Viewer: " + (StorySaverController.isAntiStoryDeleteEnabled() ? "[ACTIVE]" : "[OFF]"),
                "Anti-Freeze & Crash Guard: [ACTIVE]",
                "VC Secret Audio Recorder: " + (FightTools.isVcRecording() ? "[RECORDING]" : "[IDLE]"),
                "Clean Forward (No Author Tag): " + (getPrefs() != null && getPrefs().getBoolean("clean_forward_mode", false) ? "[ON]" : "[OFF]"),
                "Send Gallery Video as Round Note: " + (getPrefs() != null && getPrefs().getBoolean("send_video_as_round", false) ? "[ON]" : "[OFF]"),
                "Unlimited View-Once Timer: [PERMANENT ACTIVE]",
                "Restricted Media Downloader: [PERMANENT ACTIVE]",
                "Unlimited Pinned Chats: [ACTIVE]",
                "🔄 Restart Bhaichara App (Instant Relaunch)",
                "⚠️ Reset All Settings to Factory Default"
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Bhaichara VIP Control Center");
            builder.setItems(options, (dialog, which) -> {
                dialog.dismiss();
                switch (which) {
                    case 0:
                        if (context instanceof Activity) {
                            MentionsHubController.showMentionsHub((Activity) context, UserConfig.selectedAccount);
                        } else {
                            Toast.makeText(context, "Open Mentions Hub from main app", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 1:
                        GlassChatController.showGlassCustomizerDialog(context, onUpdate);
                        break;
                    case 2:
                        FancyTextTransformer.showStyleSelectorDialog(context, onUpdate);
                        break;
                    case 3:
                        FontController.showFontSelectorDialog(context, onUpdate);
                        break;
                    case 4:
                        AudioDspProcessor.showDspControlDialog(context, onUpdate);
                        break;
                    case 5:
                        AudioDspProcessor.showDangerWarningDialog(context, onUpdate);
                        break;
                    case 6:
                        FightTools.showAutoReactionDialog(context, onUpdate);
                        break;
                    case 7:
                        togglePref("download_booster", "16x Download Speed Booster", context, onUpdate);
                        break;
                    case 8:
                        togglePref("hide_phone_number", "Hide Phone Number (Privacy Mask)", context, onUpdate);
                        break;
                    case 9:
                        togglePref("exact_timestamp_seconds", "Exact Message Timestamps (Seconds)", context, onUpdate);
                        break;
                    case 10:
                        SpamFilter.showSpamFilterDialog(context, onUpdate);
                        break;
                    case 11:
                        VoiceChanger.showVoiceChangerSheet(context, false, onUpdate);
                        break;
                    case 12:
                        togglePref("ghost_mode", "Ghost Mode", context, onUpdate);
                        break;
                    case 13:
                        togglePref("anti_delete_mode", "Anti-Delete Mode", context, onUpdate);
                        break;
                    case 14:
                        togglePref("anti_edit_mode", "Anti-Edit Mode", context, onUpdate);
                        break;
                    case 15:
                        StorySaverController.setAntiStoryDeleteEnabled(!StorySaverController.isAntiStoryDeleteEnabled());
                        Toast.makeText(context, "Anti-Story Delete: " + (StorySaverController.isAntiStoryDeleteEnabled() ? "ENABLED" : "DISABLED"), Toast.LENGTH_SHORT).show();
                        if (onUpdate != null) onUpdate.run();
                        break;
                    case 16:
                        Toast.makeText(context, "Zero-Lag Anti-Freeze & Crash Guard is permanently active!", Toast.LENGTH_SHORT).show();
                        break;
                    case 17:
                        FightTools.toggleVcRecording(context);
                        if (onUpdate != null) onUpdate.run();
                        break;
                    case 18:
                        togglePref("clean_forward_mode", "Clean Forward (No Author Tag)", context, onUpdate);
                        break;
                    case 19:
                        togglePref("send_video_as_round", "Round Video Note Converter", context, onUpdate);
                        break;
                    case 20:
                        Toast.makeText(context, "Unlimited View-Once is permanently active!", Toast.LENGTH_SHORT).show();
                        break;
                    case 21:
                        Toast.makeText(context, "Restricted Channel Media Downloader is permanently active!", Toast.LENGTH_SHORT).show();
                        break;
                    case 22:
                        Toast.makeText(context, "Unlimited Pinned Chats is permanently active!", Toast.LENGTH_SHORT).show();
                        break;
                    case 23:
                        restartApp(context);
                        break;
                    case 24:
                        resetAllSettings(context, onUpdate);
                        break;
                }
            });
            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
            builder.show();
        } catch (Throwable ignored) {}
    }

    public static void restartApp(Context context) {
        try {
            if (context == null) return;
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent);
            }
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        } catch (Throwable ignored) {}
    }

    public static void resetAllSettings(Context context, Runnable onUpdate) {
        if (context == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Reset All Settings to Default");
        builder.setMessage("Are you sure you want to reset all Bhaichara VIP features, Equalizer, Chat Styling, and Filters to default factory state?");
        builder.setPositiveButton("RESET ALL", (dialog, which) -> {
            try {
                SharedPreferences prefs = getPrefs();
                if (prefs != null) {
                    prefs.edit()
                        .remove("dsp_studio_mode")
                        .remove("dsp_master_gain_db")
                        .remove("dsp_raw_clipper")
                        .remove("bhaigram_selected_font")
                        .remove("bhaigram_vc_dm_effect")
                        .remove("bhaigram_vc_call_effect")
                        .remove("ghost_mode")
                        .remove("anti_delete_mode")
                        .remove("anti_edit_mode")
                        .remove("download_booster")
                        .remove("hide_phone_number")
                        .remove("exact_timestamp_seconds")
                        .remove("spam_filter_enabled")
                        .remove("glass_bubble_alpha")
                        .remove("glass_bubble_radius")
                        .remove("glass_amoled_black")
                        .remove("auto_reaction_enabled")
                        .apply();
                }
                Toast.makeText(context, "All Bhaichara Settings Reset to Default!", Toast.LENGTH_LONG).show();
                if (onUpdate != null) onUpdate.run();
            } catch (Throwable ignored) {}
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private static void togglePref(String key, String title, Context context, Runnable onUpdate) {
        try {
            boolean current = getPrefs() != null && getPrefs().getBoolean(key, false);
            if (getPrefs() != null) {
                getPrefs().edit().putBoolean(key, !current).apply();
            }
            Toast.makeText(context, title + ": " + (!current ? "[ENABLED]" : "[DISABLED]"), Toast.LENGTH_SHORT).show();
            if (onUpdate != null) {
                onUpdate.run();
            }
        } catch (Throwable ignored) {}
    }
}
