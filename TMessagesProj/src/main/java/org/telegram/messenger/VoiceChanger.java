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
        "🌊 Underwater / Muffled"
    };

    // Circular delay buffer for Echo, Hall and Lo-Fi Reverb (approx 48k samples = 1 sec)
    private static final int DELAY_BUFFER_SIZE = 48000;
    private static final short[] delayBuffer = new short[DELAY_BUFFER_SIZE];
    private static int delayWriteIndex = 0;

    // Filter states
    private static float lowPassFilterState = 0;
    private static float highPassFilterState = 0;
    private static double ringModPhase = 0;
    private static float pitchPhaseAccumulator = 0;
    private static short lastSample = 0;

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
        // Kept for backward compatibility with older callers
        return inputFile;
    }

    public static void processPcmBuffer(ByteBuffer byteBuffer, int bytesRead, boolean isVC) {
        int effectId = isVC ? getVCEffect() : getDMEffect();
        if (effectId <= EFFECT_NONE || byteBuffer == null || bytesRead <= 0) {
            return;
        }

        int position = byteBuffer.position();
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int sampleCount = bytesRead / 2;

        for (int i = position; i < position + bytesRead - 1; i += 2) {
            short sample = byteBuffer.getShort(i);
            float processed = sample;

            switch (effectId) {
                case EFFECT_HELIUM: { // Chipmunk High Pitch
                    pitchPhaseAccumulator += 1.55f;
                    if (pitchPhaseAccumulator >= 2.0f) {
                        pitchPhaseAccumulator -= 1.0f;
                        processed = (float) (sample * 1.35f);
                    } else {
                        processed = (float) ((sample + lastSample) * 0.7f);
                    }
                    lastSample = sample;
                    break;
                }

                case EFFECT_GIANT: { // Monster Deep Low Pitch
                    pitchPhaseAccumulator += 0.65f;
                    lowPassFilterState += 0.35f * (sample - lowPassFilterState);
                    processed = lowPassFilterState * 1.5f;
                    if (pitchPhaseAccumulator >= 1.0f) {
                        pitchPhaseAccumulator -= 1.0f;
                    }
                    break;
                }

                case EFFECT_ALIEN: { // Alien Modulation
                    ringModPhase += 0.045;
                    if (ringModPhase > Math.PI * 2) ringModPhase -= Math.PI * 2;
                    double mod = Math.sin(ringModPhase * 25.0);
                    processed = (float) (sample * (0.4 + 0.8 * Math.abs(mod)));
                    break;
                }

                case EFFECT_ROBOT: { // Robotic Ring Modulator
                    ringModPhase += 0.035;
                    if (ringModPhase > Math.PI * 2) ringModPhase -= Math.PI * 2;
                    double carrier = Math.sin(ringModPhase * 80.0);
                    // Bit-crush & ring modulation
                    int crushed = ((int)(sample * carrier) / 128) * 128;
                    processed = (float) (crushed * 1.2f);
                    break;
                }

                case EFFECT_SLOWED: { // Slowed + Lo-Fi Reverb
                    lowPassFilterState += 0.2f * (sample - lowPassFilterState);
                    int delayReadIndex = (delayWriteIndex - 8000 + DELAY_BUFFER_SIZE) % DELAY_BUFFER_SIZE;
                    short delayed = delayBuffer[delayReadIndex];
                    processed = lowPassFilterState * 0.8f + delayed * 0.45f;
                    delayBuffer[delayWriteIndex] = (short) Math.max(-32768, Math.min(32767, processed));
                    delayWriteIndex = (delayWriteIndex + 1) % DELAY_BUFFER_SIZE;
                    break;
                }

                case EFFECT_NIGHTCORE: { // Fast & Sharp
                    pitchPhaseAccumulator += 1.35f;
                    highPassFilterState += 0.15f * (sample - highPassFilterState);
                    processed = (sample - highPassFilterState) * 1.4f;
                    break;
                }

                case EFFECT_TELEPHONE: { // Narrow Bandpass (300Hz - 3400Hz)
                    highPassFilterState += 0.08f * (sample - highPassFilterState);
                    float hp = sample - highPassFilterState;
                    lowPassFilterState += 0.35f * (hp - lowPassFilterState);
                    // Telephonic saturation overdrive
                    float clipped = lowPassFilterState * 2.5f;
                    if (clipped > 16000.0f) clipped = 16000.0f;
                    else if (clipped < -16000.0f) clipped = -16000.0f;
                    processed = clipped * 1.5f;
                    break;
                }

                case EFFECT_RADIO: { // Police Walkie Talkie
                    highPassFilterState += 0.12f * (sample - highPassFilterState);
                    float hp = sample - highPassFilterState;
                    lowPassFilterState += 0.4f * (hp - lowPassFilterState);
                    int crushed = ((int)(lowPassFilterState * 2.8f) / 256) * 256;
                    processed = (float) crushed;
                    break;
                }

                case EFFECT_ECHO: { // Grand Cathedral Echo
                    int delayReadIndex = (delayWriteIndex - 12000 + DELAY_BUFFER_SIZE) % DELAY_BUFFER_SIZE;
                    short echoSample = delayBuffer[delayReadIndex];
                    processed = sample + echoSample * 0.55f;
                    delayBuffer[delayWriteIndex] = (short) Math.max(-32768, Math.min(32767, sample + echoSample * 0.4f));
                    delayWriteIndex = (delayWriteIndex + 1) % DELAY_BUFFER_SIZE;
                    break;
                }

                case EFFECT_GHOST: { // Ethereal Ghost Whisper
                    ringModPhase += 0.015;
                    if (ringModPhase > Math.PI * 2) ringModPhase -= Math.PI * 2;
                    double mod = Math.sin(ringModPhase * 12.0);
                    int delayReadIndex = (delayWriteIndex - 6000 + DELAY_BUFFER_SIZE) % DELAY_BUFFER_SIZE;
                    short echo = delayBuffer[delayReadIndex];
                    processed = (float) (sample * (0.5 + 0.5 * mod) + echo * 0.4f);
                    delayBuffer[delayWriteIndex] = (short) Math.max(-32768, Math.min(32767, processed));
                    delayWriteIndex = (delayWriteIndex + 1) % DELAY_BUFFER_SIZE;
                    break;
                }

                case EFFECT_BASS: { // Mega Subwoofer Bass Boost
                    lowPassFilterState += 0.12f * (sample - lowPassFilterState);
                    processed = sample + lowPassFilterState * 2.6f;
                    break;
                }

                case EFFECT_UNDERWATER: { // Underwater / Muffled
                    lowPassFilterState += 0.06f * (sample - lowPassFilterState);
                    processed = lowPassFilterState * 1.8f;
                    break;
                }

                default:
                    processed = sample;
                    break;
            }

            // Clamp sample to 16-bit range
            if (processed > 32767.0f) processed = 32767.0f;
            else if (processed < -32768.0f) processed = -32768.0f;

            byteBuffer.putShort(i, (short) processed);
        }
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
