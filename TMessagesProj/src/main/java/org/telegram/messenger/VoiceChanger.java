package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import java.io.File;

public class VoiceChanger {

    public static final int EFFECT_NONE = 0;
    public static final int EFFECT_HELIUM = 1;
    public static final int EFFECT_GIANT = 2;
    public static final int EFFECT_ALIEN = 3;
    public static final int EFFECT_ROBOT = 4;
    public static final int EFFECT_SLOWED = 5;
    public static final int EFFECT_NIGHTCORE = 6;
    public static final int EFFECT_TELEPHONE = 7;
    public static final int EFFECT_RADIO = 8;
    public static final int EFFECT_ECHO = 9;
    public static final int EFFECT_GHOST = 10;
    public static final int EFFECT_BASS = 11;
    public static final int EFFECT_UNDERWATER = 12;

    public static final String[] EFFECT_NAMES = {
        "Off (Original Voice)",
        "Helium / Chipmunk (High Pitch)",
        "Giant / Deep Monster (Low Pitch)",
        "Instagram Reels Alien",
        "Robot / Cybernetic",
        "Slowed + Reverb (Lo-Fi)",
        "Nightcore (Speed + Pitch)",
        "Telephone Call (Bandpass)",
        "Walkie Talkie / Police Radio",
        "Grand Cathedral Hall Echo",
        "Ethereal Ghost Whisper",
        "Mega Subwoofer Bass Boost",
        "Underwater / Muffled"
    };

    public static final String[] FFMPEG_FILTERS = {
        "",
        "asetrate=48000*1.587,aresample=48000,atempo=1/1.587",
        "asetrate=48000*0.63,aresample=48000,atempo=1/0.63",
        "asetrate=48000*1.8,aresample=48000,flanger=delay=10:depth=5:regen=70:width=80:speed=2",
        "afftfilt=real='hypot(re,im)*sin(0)':imag='hypot(re,im)*cos(0)':win_size=512:overlap=0.75,chorus=0.7:0.9:55:0.4:0.25:2",
        "asetrate=48000*0.85,aresample=48000,aecho=0.8:0.88:60:0.4",
        "asetrate=48000*1.25,aresample=48000",
        "highpass=f=300,lowpass=f=3400,volume=1.3",
        "highpass=f=400,lowpass=f=2500,volume=1.5,acrusher=level_in=1:level_out=1:bits=8:mode=log:aa=1",
        "aecho=0.8:0.9:1000|1800:0.3|0.25",
        "flanger=delay=20:depth=10:speed=0.5,aecho=0.7:0.7:200|400:0.4|0.3",
        "equalizer=f=60:width_type=h:width=50:g=14,equalizer=f=120:width_type=h:width=60:g=8",
        "lowpass=f=600,volume=1.8,aecho=0.8:0.7:100:0.3"
    };

    private static SharedPreferences getPrefs() {
        try {
            return MessagesController.getGlobalMainSettings();
        } catch (Throwable e) {
            return null;
        }
    }

    public static int getDMEffect() {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                return prefs.getInt("bhaigram_vc_dm_effect", EFFECT_NONE);
            }
        } catch (Throwable ignored) {}
        return EFFECT_NONE;
    }

    public static void setDMEffect(int effect) {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                prefs.edit().putInt("bhaigram_vc_dm_effect", effect).apply();
            }
        } catch (Throwable ignored) {}
    }

    public static int getVCEffect() {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                return prefs.getInt("bhaigram_vc_call_effect", EFFECT_NONE);
            }
        } catch (Throwable ignored) {}
        return EFFECT_NONE;
    }

    public static void setVCEffect(int effect) {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                prefs.edit().putInt("bhaigram_vc_call_effect", effect).apply();
            }
        } catch (Throwable ignored) {}
    }

    public static File applyEffect(File inputFile) {
        int effectId = getDMEffect();
        if (effectId <= EFFECT_NONE || effectId >= FFMPEG_FILTERS.length || inputFile == null || !inputFile.exists()) {
            return inputFile;
        }

        String filter = FFMPEG_FILTERS[effectId];
        if (filter.isEmpty()) {
            return inputFile;
        }

        try {
            File outputFile = new File(inputFile.getParentFile(), "effect_" + System.currentTimeMillis() + "_" + inputFile.getName());
            String[] cmd = new String[]{
                "ffmpeg", "-y", "-i", inputFile.getAbsolutePath(),
                "-af", filter,
                "-c:a", "libopus", "-b:a", "64k",
                "-vn", outputFile.getAbsolutePath()
            };

            Process process = Runtime.getRuntime().exec(cmd);
            int exitCode = process.waitFor();
            if (exitCode == 0 && outputFile.exists() && outputFile.length() > 0) {
                return outputFile;
            }
        } catch (Throwable t) {
            FileLog.e("VoiceChanger applyEffect failed: " + t.getMessage());
        }

        return inputFile;
    }

    public static void showVoiceChangerSheet(Context context, boolean isVC, Runnable onSelect) {
        if (context == null) return;

        int currentEffect = isVC ? getVCEffect() : getDMEffect();
        String title = isVC ? "Voice Chat (VC) - Voice Changer" : "Direct Messages (DM) - Voice Changer";

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setItems(EFFECT_NAMES, (dialog, which) -> {
            if (isVC) {
                setVCEffect(which);
            } else {
                setDMEffect(which);
            }
            dialog.dismiss();

            String modeName = EFFECT_NAMES[which];
            Toast.makeText(context, "Voice Changer: " + modeName, Toast.LENGTH_SHORT).show();

            if (onSelect != null) {
                onSelect.run();
            }
        });
        builder.setNegativeButton(LocaleController.getString(org.telegram.messenger.R.string.Cancel), null);
        builder.show();
    }
}
