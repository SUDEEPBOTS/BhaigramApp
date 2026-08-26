package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
    public static final int EFFECT_BITCRUSHER = 13;

    public static final String[] EFFECT_NAMES = {
        "Off (Original Voice)",
        "🐿️ Helium / Chipmunk (High Pitch)",
        "👹 Giant / Deep Monster (Low Pitch)",
        "👽 Instagram Reels Alien",
        "🤖 Robot / Cybernetic",
        "🎵 Slowed + Reverb (Lo-Fi)",
        "⚡ Nightcore (Speed + Pitch)",
        "📞 Telephone Call (Bandpass)",
        "📻 Walkie Talkie / Police Radio",
        "🏰 Grand Cathedral Hall Echo",
        "👻 Ethereal Ghost Whisper",
        "💥 Mega Subwoofer Bass Boost",
        "🌊 Underwater / Muffled",
        "💣 Hyper-Distortion 8-Bit Crusher (Sonic War)"
    };

    // Circular delay buffer for Echo and Reverb (48000 samples = 1 sec at 48kHz)
    private static final int DELAY_BUFFER_SIZE = 48000;
    private static final short[] delayBuffer = new short[DELAY_BUFFER_SIZE];
    private static int delayWriteIndex = 0;

    // Granular Pitch Shifter Ring Buffer
    private static final int GRAIN_BUFFER_SIZE = 8192;
    private static final short[] grainBuffer = new short[GRAIN_BUFFER_SIZE];
    private static int grainWriteIndex = 0;
    private static float grainReadPhase1 = 0;
    private static float grainReadPhase2 = 0;

    // Filter states
    private static float lowPass1 = 0;
    private static float lowPass2 = 0;
    private static float highPass1 = 0;
    private static float highPass2 = 0;
    private static double ringModPhase = 0;
    private static double lfoPhase = 0;

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
        return inputFile;
    }

    /**
     * Real-Time Overlap-Add Granular Pitch Shift Engine
     */
    private static float pitchShiftSample(short sample, float pitchRatio, int grainSize) {
        grainBuffer[grainWriteIndex] = sample;
        grainWriteIndex = (grainWriteIndex + 1) % GRAIN_BUFFER_SIZE;

        int halfGrain = grainSize / 2;
        grainReadPhase1 += pitchRatio;
        if (grainReadPhase1 >= grainSize) {
            grainReadPhase1 -= grainSize;
        }

        grainReadPhase2 = (grainReadPhase1 + halfGrain) % grainSize;

        // Window 1
        float w1 = 0.5f * (1.0f - (float) Math.cos(2.0 * Math.PI * grainReadPhase1 / grainSize));
        int readIdx1 = ((grainWriteIndex - grainSize + (int) grainReadPhase1) % GRAIN_BUFFER_SIZE + GRAIN_BUFFER_SIZE) % GRAIN_BUFFER_SIZE;
        short s1 = grainBuffer[readIdx1];

        // Window 2
        float w2 = 0.5f * (1.0f - (float) Math.cos(2.0 * Math.PI * grainReadPhase2 / grainSize));
        int readIdx2 = ((grainWriteIndex - grainSize + (int) grainReadPhase2) % GRAIN_BUFFER_SIZE + GRAIN_BUFFER_SIZE) % GRAIN_BUFFER_SIZE;
        short s2 = grainBuffer[readIdx2];

        return (s1 * w1 + s2 * w2) * 1.4f;
    }

    public static void processPcmBuffer(ByteBuffer byteBuffer, int bytesRead, boolean isVC) {
        int effectId = isVC ? getVCEffect() : getDMEffect();
        if (effectId <= EFFECT_NONE || byteBuffer == null || bytesRead <= 0) {
            return;
        }

        int origPos = byteBuffer.position();
        byteBuffer.order(ByteOrder.nativeOrder());

        for (int i = 0; i < bytesRead - 1; i += 2) {
            short sample = byteBuffer.getShort(i);
            float processed = sample;

            switch (effectId) {
                case EFFECT_HELIUM: { // Chipmunk High Pitch (+70% Pitch Shift)
                    processed = pitchShiftSample(sample, 1.70f, 384);
                    // Add slight high frequency boost
                    highPass1 += 0.2f * (processed - highPass1);
                    processed = processed + (processed - highPass1) * 0.4f;
                    break;
                }

                case EFFECT_GIANT: { // Monster Deep Low Pitch (-45% Pitch Shift)
                    processed = pitchShiftSample(sample, 0.58f, 768);
                    // Add deep low-pass bass body
                    lowPass1 += 0.35f * (processed - lowPass1);
                    processed = processed * 0.7f + lowPass1 * 1.6f;
                    break;
                }

                case EFFECT_ALIEN: { // Alien Vibrato & Tremolo Pitch
                    lfoPhase += 0.035;
                    if (lfoPhase > Math.PI * 2) lfoPhase -= Math.PI * 2;
                    float modPitch = 1.35f + (float) Math.sin(lfoPhase * 8.0) * 0.25f;
                    processed = pitchShiftSample(sample, modPitch, 320);

                    ringModPhase += 0.05;
                    if (ringModPhase > Math.PI * 2) ringModPhase -= Math.PI * 2;
                    float am = 0.4f + 0.6f * (float) Math.abs(Math.sin(ringModPhase * 25.0));
                    processed *= am;
                    break;
                }

                case EFFECT_ROBOT: { // Cybernetic Ring Modulator & Bit-Crush
                    ringModPhase += 0.045;
                    if (ringModPhase > Math.PI * 2) ringModPhase -= Math.PI * 2;
                    double carrier = Math.sin(ringModPhase * 140.0);
                    // 6-bit quantization
                    int quant = ((int) (sample * carrier) / 64) * 64;
                    processed = quant * 1.8f;
                    break;
                }

                case EFFECT_SLOWED: { // Slowed + Lo-Fi Reverb & Warm Lows
                    processed = pitchShiftSample(sample, 0.82f, 600);
                    lowPass1 += 0.25f * (processed - lowPass1);
                    processed = lowPass1 * 1.2f;

                    int delayRead = (delayWriteIndex - 7200 + DELAY_BUFFER_SIZE) % DELAY_BUFFER_SIZE;
                    short delayed = delayBuffer[delayRead];
                    processed = processed * 0.85f + delayed * 0.45f;
                    delayBuffer[delayWriteIndex] = (short) Math.max(-32768, Math.min(32767, processed));
                    delayWriteIndex = (delayWriteIndex + 1) % DELAY_BUFFER_SIZE;
                    break;
                }

                case EFFECT_NIGHTCORE: { // Nightcore High-Speed Pitch
                    processed = pitchShiftSample(sample, 1.38f, 420);
                    highPass1 += 0.15f * (processed - highPass1);
                    processed = (processed - highPass1) * 1.5f;
                    break;
                }

                case EFFECT_TELEPHONE: { // 1990s Landline Telephone (300Hz - 3400Hz)
                    highPass1 += 0.15f * (sample - highPass1);
                    float hp = sample - highPass1;
                    lowPass1 += 0.35f * (hp - lowPass1);
                    float clipped = lowPass1 * 3.8f;
                    if (clipped > 14000.0f) clipped = 14000.0f;
                    else if (clipped < -14000.0f) clipped = -14000.0f;
                    processed = clipped * 1.6f;
                    break;
                }

                case EFFECT_RADIO: { // Police Scanner / Walkie Talkie
                    highPass1 += 0.20f * (sample - highPass1);
                    float hp = sample - highPass1;
                    lowPass1 += 0.40f * (hp - lowPass1);
                    int crushed = ((int)(lowPass1 * 4.0f) / 128) * 128;
                    processed = (float) crushed;
                    break;
                }

                case EFFECT_ECHO: { // Grand Cathedral Multi-Tap Echo
                    int tap1 = (delayWriteIndex - 8820 + DELAY_BUFFER_SIZE) % DELAY_BUFFER_SIZE;
                    int tap2 = (delayWriteIndex - 17640 + DELAY_BUFFER_SIZE) % DELAY_BUFFER_SIZE;
                    short echo1 = delayBuffer[tap1];
                    short echo2 = delayBuffer[tap2];

                    processed = sample + echo1 * 0.50f + echo2 * 0.30f;
                    delayBuffer[delayWriteIndex] = (short) Math.max(-32768, Math.min(32767, sample + echo1 * 0.45f));
                    delayWriteIndex = (delayWriteIndex + 1) % DELAY_BUFFER_SIZE;
                    break;
                }

                case EFFECT_GHOST: { // Ethereal Ghost Whisper
                    lfoPhase += 0.02;
                    if (lfoPhase > Math.PI * 2) lfoPhase -= Math.PI * 2;
                    float flutter = (float) Math.sin(lfoPhase * 12.0);
                    int tap = (delayWriteIndex - 6000 + DELAY_BUFFER_SIZE) % DELAY_BUFFER_SIZE;
                    short echo = delayBuffer[tap];
                    processed = (float) (sample * (0.35 + 0.65 * Math.abs(flutter)) + echo * 0.45f);
                    delayBuffer[delayWriteIndex] = (short) Math.max(-32768, Math.min(32767, processed));
                    delayWriteIndex = (delayWriteIndex + 1) % DELAY_BUFFER_SIZE;
                    break;
                }

                case EFFECT_BASS: { // Mega Subwoofer Bass Boost
                    lowPass1 += 0.12f * (sample - lowPass1);
                    lowPass2 += 0.12f * (lowPass1 - lowPass2);
                    processed = sample + lowPass2 * 3.8f;
                    break;
                }

                case EFFECT_UNDERWATER: { // Submerged / Muffled
                    lowPass1 += 0.05f * (sample - lowPass1);
                    lowPass2 += 0.05f * (lowPass1 - lowPass2);
                    processed = lowPass2 * 2.5f;
                    break;
                }

                case EFFECT_BITCRUSHER: { // 8-Bit Hyper-Distortion Crusher
                    // 5-bit quantization + square wave clipping
                    int crushed = ((int)(sample * 3.5f) / 128) * 128;
                    float boosted = crushed * 2.2f;
                    if (boosted > 28000.0f) boosted = 28000.0f;
                    else if (boosted < -28000.0f) boosted = -28000.0f;
                    processed = boosted * 1.3f;
                    break;
                }

                default:
                    processed = sample;
                    break;
            }

            if (processed > 32767.0f) processed = 32767.0f;
            else if (processed < -32768.0f) processed = -32768.0f;

            byteBuffer.putShort(i, (short) processed);
        }
        byteBuffer.position(origPos);
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
