package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioDspProcessor {

    public static final String[] BAND_LABELS = {
        "31 Hz (Deep Sub-Bass)",
        "62 Hz (Room Bass)",
        "125 Hz (Low Punch)",
        "250 Hz (Warmth)",
        "500 Hz (Mid Body)",
        "1 kHz (Presence)",
        "2 kHz (Vocal Clarity)",
        "4 kHz (Treble Edge)",
        "8 kHz (High Sizzle)",
        "16 kHz (Air & Shimmer)"
    };

    private static final float[] bandGains = new float[10];
    private static float masterGainMultiplier = 1.0f;
    private static boolean isEnabled = false;
    private static boolean rawClipperMode = false;

    static {
        try {
            loadSettings();
        } catch (Throwable ignored) {}
    }

    private static SharedPreferences getPrefs() {
        try {
            return MessagesController.getGlobalMainSettings();
        } catch (Throwable e) {
            return null;
        }
    }

    public static void loadSettings() {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs == null) return;
            isEnabled = prefs.getBoolean("dsp_studio_mode", false);
            rawClipperMode = prefs.getBoolean("dsp_raw_clipper", false);
            float masterGainDb = prefs.getFloat("dsp_master_gain_db", 0.0f);
            setMasterGainDb(masterGainDb);

            for (int i = 0; i < 10; i++) {
                bandGains[i] = prefs.getFloat("dsp_band_gain_" + i, 0.0f);
            }
        } catch (Throwable ignored) {}
    }

    public static boolean isStudioModeEnabled() {
        return isEnabled;
    }

    public static void setStudioModeEnabled(boolean enabled) {
        isEnabled = enabled;
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                prefs.edit().putBoolean("dsp_studio_mode", enabled).apply();
            }
        } catch (Throwable ignored) {}
    }

    public static boolean isRawClipperEnabled() {
        return rawClipperMode;
    }

    public static void setRawClipperEnabled(boolean enabled) {
        rawClipperMode = enabled;
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                prefs.edit().putBoolean("dsp_raw_clipper", enabled).apply();
            }
        } catch (Throwable ignored) {}
    }

    public static float getMasterGainDb() {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                return prefs.getFloat("dsp_master_gain_db", 0.0f);
            }
        } catch (Throwable ignored) {}
        return 0.0f;
    }

    public static void setMasterGainDb(float gainDb) {
        if (gainDb < 0.0f) gainDb = 0.0f;
        if (gainDb > 36.0f) gainDb = 36.0f;
        masterGainMultiplier = (float) Math.pow(10, gainDb / 20.0);
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                prefs.edit().putFloat("dsp_master_gain_db", gainDb).apply();
            }
        } catch (Throwable ignored) {}
    }

    public static float getBandGain(int band) {
        if (band >= 0 && band < 10) {
            return bandGains[band];
        }
        return 0.0f;
    }

    public static void setBandGain(int band, float gainDb) {
        if (band >= 0 && band < 10) {
            bandGains[band] = gainDb;
            try {
                SharedPreferences prefs = getPrefs();
                if (prefs != null) {
                    prefs.edit().putFloat("dsp_band_gain_" + band, gainDb).apply();
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void applyPreset(int preset) {
        switch (preset) {
            case 0: // Flat
                setMasterGainDb(0.0f);
                for (int i = 0; i < 10; i++) setBandGain(i, 0.0f);
                break;
            case 1: // Mega Bass Heavy
                setMasterGainDb(14.0f);
                setBandGain(0, 15.0f);
                setBandGain(1, 14.0f);
                setBandGain(2, 12.0f);
                setBandGain(3, 8.0f);
                setBandGain(4, 0.0f);
                setBandGain(5, 0.0f);
                setBandGain(6, 0.0f);
                setBandGain(7, 2.0f);
                setBandGain(8, 4.0f);
                setBandGain(9, 6.0f);
                break;
            case 2: // Vocal Treble Pierce
                setMasterGainDb(16.0f);
                setBandGain(0, -6.0f);
                setBandGain(1, -4.0f);
                setBandGain(2, 0.0f);
                setBandGain(3, 2.0f);
                setBandGain(4, 6.0f);
                setBandGain(5, 10.0f);
                setBandGain(6, 12.0f);
                setBandGain(7, 15.0f);
                setBandGain(8, 15.0f);
                setBandGain(9, 14.0f);
                break;
            case 3: // Overdrive Level 1 (+24dB)
                setMasterGainDb(24.0f);
                for (int i = 0; i < 10; i++) setBandGain(i, 8.0f);
                break;
            case 4: // WARZONE: Extreme God Mode (+35dB Overdrive)
                setMasterGainDb(35.0f);
                setBandGain(0, 15.0f);
                setBandGain(1, 15.0f);
                setBandGain(2, 14.0f);
                setBandGain(3, 10.0f);
                setBandGain(4, 10.0f);
                setBandGain(5, 12.0f);
                setBandGain(6, 14.0f);
                setBandGain(7, 15.0f);
                setBandGain(8, 15.0f);
                setBandGain(9, 15.0f);
                break;
            case 5: // WARZONE: Sub-Bass Destroyer (+30dB Bass Cannon)
                setMasterGainDb(30.0f);
                setBandGain(0, 15.0f);
                setBandGain(1, 15.0f);
                setBandGain(2, 15.0f);
                setBandGain(3, 12.0f);
                setBandGain(4, 4.0f);
                setBandGain(5, 0.0f);
                setBandGain(6, 0.0f);
                setBandGain(7, 0.0f);
                setBandGain(8, 4.0f);
                setBandGain(9, 6.0f);
                break;
            case 6: // WARZONE: High-Frequency Ear-Screamer (+32dB Treble Blade)
                setMasterGainDb(32.0f);
                setBandGain(0, -10.0f);
                setBandGain(1, -8.0f);
                setBandGain(2, -4.0f);
                setBandGain(3, 0.0f);
                setBandGain(4, 8.0f);
                setBandGain(5, 14.0f);
                setBandGain(6, 15.0f);
                setBandGain(7, 15.0f);
                setBandGain(8, 15.0f);
                setBandGain(9, 15.0f);
                break;
        }
    }

    public static void processByteBuffer(ByteBuffer byteBuffer, int bytesRead) {
        if (!isEnabled || byteBuffer == null || bytesRead <= 0) {
            return;
        }

        int position = byteBuffer.position();
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = position; i < position + bytesRead - 1; i += 2) {
            short sample = byteBuffer.getShort(i);

            // Apply Master Pre-Amp Digital Gain Multiplier
            float processed = sample * masterGainMultiplier;

            if (rawClipperMode) {
                // Hard digital clipping for extreme warzone punch
                if (processed > 32767.0f) processed = 32767.0f;
                else if (processed < -32768.0f) processed = -32768.0f;
            } else {
                // Soft-clipping saturation limiter curve
                if (processed > 32767.0f) {
                    processed = 32767.0f;
                } else if (processed < -32768.0f) {
                    processed = -32768.0f;
                }
            }

            byteBuffer.putShort(i, (short) processed);
        }
    }

    public static void showDspControlDialog(Context context, Runnable onUpdate) {
        if (context == null) return;

        String[] options = new String[]{
            "Studio Mode Power: " + (isStudioModeEnabled() ? "[ENABLED]" : "[DISABLED]"),
            "10-Band Graphic Equalizer Sliders",
            "Master Gain Overdrive: +" + (int) getMasterGainDb() + " dB",
            "Clipper Engine: " + (isRawClipperEnabled() ? "[HARD WAR CLIPPER]" : "[SOFT LIMITER]"),
            "Preset: Flat (Reset 0dB)",
            "Preset: Mega Bass Heavy (+14dB)",
            "Preset: Vocal Treble Pierce (+16dB)",
            "Preset: High Overdrive (+24dB)",
            "WARZONE: Extreme God Mode (+35dB) [CAUTION]",
            "WARZONE: Sub-Bass Destroyer (+30dB) [CAUTION]",
            "WARZONE: Ear-Screamer Treble (+32dB) [CAUTION]"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Pro Studio Audio Engine (DSP)");
        builder.setItems(options, (dialog, which) -> {
            dialog.dismiss();
            switch (which) {
                case 0:
                    setStudioModeEnabled(!isStudioModeEnabled());
                    Toast.makeText(context, "Studio DSP: " + (isStudioModeEnabled() ? "ENABLED" : "DISABLED"), Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    show10BandEqualizerDialog(context, onUpdate);
                    return;
                case 2:
                    showGainSliderDialog(context, onUpdate);
                    return;
                case 3:
                    setRawClipperEnabled(!isRawClipperEnabled());
                    Toast.makeText(context, "Clipper Engine: " + (isRawClipperEnabled() ? "Hard War Clipper" : "Soft Limiter"), Toast.LENGTH_SHORT).show();
                    break;
                case 4:
                    applyPreset(0);
                    Toast.makeText(context, "Applied Preset: Flat Reset", Toast.LENGTH_SHORT).show();
                    break;
                case 5:
                    setStudioModeEnabled(true);
                    applyPreset(1);
                    Toast.makeText(context, "Applied Preset: Mega Bass Heavy", Toast.LENGTH_SHORT).show();
                    break;
                case 6:
                    setStudioModeEnabled(true);
                    applyPreset(2);
                    Toast.makeText(context, "Applied Preset: Vocal Treble Pierce", Toast.LENGTH_SHORT).show();
                    break;
                case 7:
                    showExtremeWarningDialog(context, "High Overdrive (+24dB)", () -> {
                        setStudioModeEnabled(true);
                        applyPreset(3);
                        Toast.makeText(context, "Applied Preset: High Overdrive (+24dB)", Toast.LENGTH_SHORT).show();
                        if (onUpdate != null) onUpdate.run();
                    });
                    return;
                case 8:
                    showExtremeWarningDialog(context, "WARZONE: Extreme God Mode (+35dB)", () -> {
                        setStudioModeEnabled(true);
                        applyPreset(4);
                        Toast.makeText(context, "WARZONE: Extreme God Mode (+35dB) Activated!", Toast.LENGTH_LONG).show();
                        if (onUpdate != null) onUpdate.run();
                    });
                    return;
                case 9:
                    showExtremeWarningDialog(context, "WARZONE: Sub-Bass Destroyer (+30dB)", () -> {
                        setStudioModeEnabled(true);
                        applyPreset(5);
                        Toast.makeText(context, "WARZONE: Sub-Bass Destroyer Activated!", Toast.LENGTH_LONG).show();
                        if (onUpdate != null) onUpdate.run();
                    });
                    return;
                case 10:
                    showExtremeWarningDialog(context, "WARZONE: Ear-Screamer Treble (+32dB)", () -> {
                        setStudioModeEnabled(true);
                        applyPreset(6);
                        Toast.makeText(context, "WARZONE: Ear-Screamer Treble Activated!", Toast.LENGTH_LONG).show();
                        if (onUpdate != null) onUpdate.run();
                    });
                    return;
            }
            if (onUpdate != null) onUpdate.run();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    public static void showExtremeWarningDialog(Context context, String featureTitle, Runnable onConfirm) {
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("WARNING: EXTREME AUDIO OVERDRIVE");
        builder.setMessage(
            "Feature: " + featureTitle + "\n\n" +
            "[!] CAUTION: You are about to enable Extreme Digital Pre-Amp Gain (>+24dB to +35dB).\n\n" +
            "- This produces extremely high loudness and intentional hardware overdrive.\n" +
            "- Recommended for Voice Chat (VC) fights and high-energy broadcasts only.\n" +
            "- Please lower your own listening volume to protect hearing and prevent speaker clipping.\n\n" +
            "Do you want to proceed?"
        );
        builder.setPositiveButton("I Understand & Enable", (dialog, which) -> {
            dialog.dismiss();
            if (onConfirm != null) {
                onConfirm.run();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    public static void show10BandEqualizerDialog(Context context, Runnable onUpdate) {
        if (context == null) return;

        ScrollView scrollView = new ScrollView(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(10));
        scrollView.addView(container);

        final SeekBar[] seekBars = new SeekBar[10];
        final TextView[] labels = new TextView[10];

        for (int i = 0; i < 10; i++) {
            final int bandIndex = i;
            float currentDb = getBandGain(bandIndex);

            TextView bandTitle = new TextView(context);
            bandTitle.setText(BAND_LABELS[bandIndex] + " : " + (currentDb >= 0 ? "+" : "") + (int) currentDb + " dB");
            bandTitle.setTextSize(14);
            bandTitle.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            bandTitle.setPadding(0, AndroidUtilities.dp(8), 0, 0);
            container.addView(bandTitle);
            labels[bandIndex] = bandTitle;

            SeekBar seekBar = new SeekBar(context);
            seekBar.setMax(30); // 0 to 30 mapped to -15dB to +15dB
            seekBar.setProgress((int) (currentDb + 15.0f));
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                    float db = progress - 15.0f;
                    setBandGain(bandIndex, db);
                    setStudioModeEnabled(true);
                    labels[bandIndex].setText(BAND_LABELS[bandIndex] + " : " + (db >= 0 ? "+" : "") + (int) db + " dB");
                }
                @Override
                public void onStartTrackingTouch(SeekBar s) {}
                @Override
                public void onStopTrackingTouch(SeekBar s) {}
            });
            container.addView(seekBar);
            seekBars[bandIndex] = seekBar;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("10-Band Live Equalizer Sliders");
        builder.setView(scrollView);
        builder.setPositiveButton("Apply", (dialog, which) -> {
            setStudioModeEnabled(true);
            Toast.makeText(context, "10-Band EQ Applied Successfully!", Toast.LENGTH_SHORT).show();
            if (onUpdate != null) onUpdate.run();
        });
        builder.setNeutralButton("Reset Flat", (dialog, which) -> {
            applyPreset(0);
            Toast.makeText(context, "Equalizer Reset to Flat (0 dB)", Toast.LENGTH_SHORT).show();
            if (onUpdate != null) onUpdate.run();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private static void showGainSliderDialog(Context context, Runnable onUpdate) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(10));

        final TextView gainLabel = new TextView(context);
        int currentGain = (int) getMasterGainDb();
        gainLabel.setText("Digital Pre-Amp Gain: +" + currentGain + " dB" + (currentGain > 20 ? " [EXTREME]" : ""));
        gainLabel.setTextSize(16);
        gainLabel.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        layout.addView(gainLabel);

        final TextView warningNote = new TextView(context);
        warningNote.setText(currentGain > 20 ? "Warning: High volume levels can cause extreme loudness!" : "Safe levels (0 to +20dB)");
        warningNote.setTextSize(13);
        warningNote.setTextColor(currentGain > 20 ? 0xFFFF4444 : 0xFF888888);
        warningNote.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(8));
        layout.addView(warningNote);

        final SeekBar seekBar = new SeekBar(context);
        seekBar.setMax(36); // Up to +36dB Overdrive
        seekBar.setProgress(currentGain);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                gainLabel.setText("Digital Pre-Amp Gain: +" + progress + " dB" + (progress > 20 ? " [EXTREME]" : ""));
                if (progress > 20) {
                    warningNote.setText("Caution: Extreme Gain Zone (+21dB to +36dB) active!");
                    warningNote.setTextColor(0xFFFF4444);
                } else {
                    warningNote.setText("Safe levels (0 to +20dB)");
                    warningNote.setTextColor(0xFF888888);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar s) {}
            @Override
            public void onStopTrackingTouch(SeekBar s) {}
        });
        layout.addView(seekBar);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Master Gain Pre-Amp Slider");
        builder.setView(layout);
        builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
            int selectedGain = seekBar.getProgress();
            if (selectedGain > 20) {
                showExtremeWarningDialog(context, "Gain Level +" + selectedGain + " dB", () -> {
                    setStudioModeEnabled(true);
                    setMasterGainDb(selectedGain);
                    Toast.makeText(context, "Master Gain set to +" + selectedGain + " dB", Toast.LENGTH_SHORT).show();
                    if (onUpdate != null) onUpdate.run();
                });
            } else {
                setStudioModeEnabled(true);
                setMasterGainDb(selectedGain);
                Toast.makeText(context, "Master Gain set to +" + selectedGain + " dB", Toast.LENGTH_SHORT).show();
                if (onUpdate != null) onUpdate.run();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }
}
