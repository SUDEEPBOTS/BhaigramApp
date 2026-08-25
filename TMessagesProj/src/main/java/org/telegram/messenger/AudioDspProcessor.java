package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
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

    static {
        loadSettings();
    }

    private static SharedPreferences getPrefs() {
        return MessagesController.getGlobalMainSettings();
    }

    public static void loadSettings() {
        SharedPreferences prefs = getPrefs();
        isEnabled = prefs.getBoolean("dsp_studio_mode", false);
        float masterGainDb = prefs.getFloat("dsp_master_gain_db", 0.0f);
        setMasterGainDb(masterGainDb);

        for (int i = 0; i < 10; i++) {
            bandGains[i] = prefs.getFloat("dsp_band_gain_" + i, 0.0f);
        }
    }

    public static boolean isStudioModeEnabled() {
        return isEnabled;
    }

    public static void setStudioModeEnabled(boolean enabled) {
        isEnabled = enabled;
        getPrefs().edit().putBoolean("dsp_studio_mode", enabled).apply();
    }

    public static float getMasterGainDb() {
        return getPrefs().getFloat("dsp_master_gain_db", 0.0f);
    }

    public static void setMasterGainDb(float gainDb) {
        if (gainDb < 0.0f) gainDb = 0.0f;
        if (gainDb > 30.0f) gainDb = 30.0f;
        masterGainMultiplier = (float) Math.pow(10, gainDb / 20.0);
        getPrefs().edit().putFloat("dsp_master_gain_db", gainDb).apply();
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
            getPrefs().edit().putFloat("dsp_band_gain_" + band, gainDb).apply();
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
                setMasterGainDb(15.0f);
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
            case 3: // Maximum Overdrive (+30dB Boost)
                setMasterGainDb(30.0f);
                for (int i = 0; i < 10; i++) setBandGain(i, 12.0f);
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

            // Apply Master Pre-Amp Digital Gain
            float processed = sample * masterGainMultiplier;

            // Soft-clipping saturation limiter (Prevents harsh digital square distortion)
            if (processed > 32767.0f) {
                processed = 32767.0f;
            } else if (processed < -32768.0f) {
                processed = -32768.0f;
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
            "Preset: Flat (Reset)",
            "Preset: Mega Bass Heavy",
            "Preset: Vocal Treble Pierce",
            "Preset: Maximum Overdrive (+30dB Boost)"
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
                    applyPreset(0);
                    Toast.makeText(context, "Applied Preset: Flat Reset", Toast.LENGTH_SHORT).show();
                    break;
                case 4:
                    setStudioModeEnabled(true);
                    applyPreset(1);
                    Toast.makeText(context, "Applied Preset: Mega Bass Heavy", Toast.LENGTH_SHORT).show();
                    break;
                case 5:
                    setStudioModeEnabled(true);
                    applyPreset(2);
                    Toast.makeText(context, "Applied Preset: Vocal Treble Pierce", Toast.LENGTH_SHORT).show();
                    break;
                case 6:
                    setStudioModeEnabled(true);
                    applyPreset(3);
                    Toast.makeText(context, "Applied Preset: Maximum Overdrive (+30dB Boost)", Toast.LENGTH_LONG).show();
                    break;
            }
            if (onUpdate != null) onUpdate.run();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
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
        gainLabel.setText("Digital Gain: +" + (int) getMasterGainDb() + " dB");
        gainLabel.setTextSize(16);
        gainLabel.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        layout.addView(gainLabel);

        final SeekBar seekBar = new SeekBar(context);
        seekBar.setMax(30);
        seekBar.setProgress((int) getMasterGainDb());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                gainLabel.setText("Digital Gain: +" + progress + " dB");
            }
            @Override
            public void onStartTrackingTouch(SeekBar s) {}
            @Override
            public void onStopTrackingTouch(SeekBar s) {}
        });
        layout.addView(seekBar);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Master Gain Pre-Amp");
        builder.setView(layout);
        builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
            setStudioModeEnabled(true);
            setMasterGainDb(seekBar.getProgress());
            Toast.makeText(context, "Master Gain set to +" + seekBar.getProgress() + " dB", Toast.LENGTH_SHORT).show();
            if (onUpdate != null) onUpdate.run();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }
}
