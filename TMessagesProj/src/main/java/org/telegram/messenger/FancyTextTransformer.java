package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import org.telegram.ui.ActionBar.AlertDialog;

public class FancyTextTransformer {

    public static final String[] STYLE_NAMES = new String[]{
        "Default / Normal (Off)",
        "ALL CAPS / CAPITAL (HELLO WORLD)",
        "Small Caps (ʜᴇʟʟᴏ ᴡᴏʀʟᴅ)",
        "Math Serif Bold (𝐇𝐞𝐥𝐥𝐨 𝐖𝐨𝐫𝐥𝐝)",
        "Math Serif Italic (𝐻𝑒𝓁𝓁𝑜 𝒲𝑜𝓇𝓁𝒹)",
        "Math Bold Italic (𝑯𝒆𝒍𝒍𝒐 𝑾𝒐𝒓𝒍𝒅)",
        "Math Sans-Serif (𝖧𝖾𝗅𝗅𝗈 𝖶𝗈𝗋𝗅𝖽)",
        "Math Sans Bold (𝗛𝗲𝗹𝗹𝗼 𝗪𝗼𝗿𝗹𝗱)",
        "Math Sans Italic (𝘏𝘦𝘭𝘭𝘰 𝘞𝘰𝘳𝘭𝘥)",
        "Math Sans Bold Italic (𝙃𝙚𝙡𝙡𝙤 𝙒𝙤𝙧𝙡𝙙)",
        "Monospace Code (𝙷𝚎𝚕𝚕𝚘 𝚆𝚘𝚛𝚕𝚍)",
        "Script / Cursive (𝓗𝓮𝓵𝓵𝓸 𝓦𝓸𝓻𝓵𝓭)",
        "Fraktur / Gothic (𝔈𝔵𝔞𝔪𝔭𝔩𝔢)",
        "Bold Fraktur (𝕳𝖊𝖑𝖑𝖔 𝖂𝖔𝖗𝖑𝖉)",
        "Double-Struck (ℍ𝕖𝕝𝕝𝕠 𝕎𝕠𝕣𝕝𝕕)",
        "Bubble / Circled (Ⓗⓔⓛⓛⓞ Ⓦⓞⓡⓛⓓ)",
        "Dark Bubble (🅗🅔🅛🅛🅞 🅦🅞🅡🅛🅓)",
        "Squared Boxed (🄷🄴🄻🄻🄾 🅆🄾🅁🄻🄳)",
        "Dark Squared (🅷🅴🅻🅻🅾 🆆🅾🆁🅻🅳)",
        "Fullwidth Vaporwave (Ｈｅｌｌｏ Ｗｏｒｌｄ)",
        "Inverted Flip (plɹoM ollǝH)",
        "Strikethrough (H̶e̶l̶l̶o̶ ̶W̶o̶r̶l̶d̶)",
        "Underline Double (H̳e̳l̳l̳o̳ ̳W̳o̳r̳l̳d̳)",
        "Slashed Strike (H̷e̷l̷l̷o̷ ̷W̷o̷r̷l̷d̷)",
        "Wave Underline (H≋e≋l≋l≋o≋ ≋W≋o≋r≋l≋d≋)",
        "Lightning Sparkles (⚡H⚡e⚡l⚡l⚡o⚡)",
        "Heart Decor (♥H♥e♥l♥l♥o♥)",
        "Star Decor (★H★e★l★l★o★)",
        "Fire Decor (🔥H🔥e🔥l🔥l🔥o🔥)",
        "Crown King Decor (👑H👑e👑l👑l👑o👑)",
        "Japanese Brackets (【H】【e】【l】【l】【o】)",
        "Parenthesized (⒣⒠⒧⒧⒪ ⒲⒪⒭⒧⒟)",
        "Curved Brackets (『H』『e』『l』『l』『o』)",
        "Dagger Cross (†H†e†l†l†o†)",
        "Diamond Decor (◈H◈e◈l◈l◈o◈)",
        "Sparkle Star (✧H✧e✧l✧l✧o✧)",
        "Aesthetic Spaced (H · e · l · l · o ·   · W · o · r · l · d)",
        "Glitch Mini (H̷e̸l̶l̶o̸ ̵W̶o̸r̶l̸d̷)"
    };

    private static SharedPreferences getPrefs() {
        try {
            return MessagesController.getGlobalMainSettings();
        } catch (Throwable e) {
            return null;
        }
    }

    public static int getSelectedStyle() {
        try {
            SharedPreferences prefs = getPrefs();
            return prefs != null ? prefs.getInt("fancy_text_style", 0) : 0;
        } catch (Throwable ignored) {}
        return 0;
    }

    public static void setSelectedStyle(int style) {
        try {
            SharedPreferences prefs = getPrefs();
            if (prefs != null) {
                prefs.edit().putInt("fancy_text_style", style).apply();
            }
        } catch (Throwable ignored) {}
    }

    public static void showStyleSelectorDialog(Context context, Runnable onSelected) {
        if (context == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("✨ 40+ Fancy Text Styles & Fonts");
        builder.setItems(STYLE_NAMES, (dialog, which) -> {
            setSelectedStyle(which);
            dialog.dismiss();
            if (onSelected != null) onSelected.run();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    public static String applyTransform(String input) {
        if (TextUtils.isEmpty(input)) return input;
        int style = getSelectedStyle();
        if (style <= 0) return input;

        switch (style) {
            case 1: // ALL CAPS
                return input.toUpperCase();
            case 2: // Small Caps
                return toSmallCaps(input);
            case 3: // Math Serif Bold
                return toMathOffset(input, 0x1D400, 0x1D41A, 0x1D7CE);
            case 4: // Math Serif Italic
                return toMathOffset(input, 0x1D434, 0x1D44E, -1);
            case 5: // Math Bold Italic
                return toMathOffset(input, 0x1D468, 0x1D482, -1);
            case 6: // Math Sans-Serif
                return toMathOffset(input, 0x1D5A0, 0x1D5BA, 0x1D7E2);
            case 7: // Math Sans Bold
                return toMathOffset(input, 0x1D5D4, 0x1D5EE, 0x1D7EC);
            case 8: // Math Sans Italic
                return toMathOffset(input, 0x1D608, 0x1D622, -1);
            case 9: // Math Sans Bold Italic
                return toMathOffset(input, 0x1D63C, 0x1D656, -1);
            case 10: // Monospace
                return toMathOffset(input, 0x1D670, 0x1D68A, 0x1D7F6);
            case 11: // Bold Script
                return toMathOffset(input, 0x1D4D0, 0x1D4EA, -1);
            case 12: // Fraktur
                return toMathOffset(input, 0x1D504, 0x1D51E, -1);
            case 13: // Bold Fraktur
                return toMathOffset(input, 0x1D56C, 0x1D586, -1);
            case 14: // Double-Struck
                return toMathOffset(input, 0x1D538, 0x1D552, 0x1D7D8);
            case 15: // Bubble / Circled
                return toCircled(input);
            case 16: // Dark Bubble
                return toDarkBubble(input);
            case 17: // Squared
                return toSquared(input);
            case 18: // Dark Squared
                return toDarkSquared(input);
            case 19: // Fullwidth
                return toFullwidth(input);
            case 20: // Inverted Flip
                return toInverted(input);
            case 21: // Strikethrough
                return withCombining(input, "\u0336");
            case 22: // Underline Double
                return withCombining(input, "\u0333");
            case 23: // Slashed
                return withCombining(input, "\u0338");
            case 24: // Wave Underline
                return withCombining(input, "\u0330");
            case 25: // Lightning
                return withSurround(input, "⚡");
            case 26: // Heart
                return withSurround(input, "♥");
            case 27: // Star
                return withSurround(input, "★");
            case 28: // Fire
                return withSurround(input, "🔥");
            case 29: // Crown
                return withSurround(input, "👑");
            case 30: // Brackets
                return withWrapper(input, "【", "】");
            case 31: // Parenthesized
                return withWrapper(input, "(", ")");
            case 32: // Curved Brackets
                return withWrapper(input, "『", "』");
            case 33: // Dagger
                return withSurround(input, "†");
            case 34: // Diamond
                return withSurround(input, "◈");
            case 35: // Sparkle Star
                return withSurround(input, "✧");
            case 36: // Aesthetic Spaced
                return toAestheticSpaced(input);
            case 37: // Glitch Mini
                return toGlitch(input);
            default:
                return input;
        }
    }

    private static String toSmallCaps(String text) {
        String normal = "abcdefghijklmnopqrstuvwxyz";
        String[] caps = {"ᴀ","ʙ","ᴄ","ᴅ","ᴇ","ꜰ","ɢ","ʜ","ɪ","ᴊ","ᴋ","ʟ","ᴍ","ɴ","ᴏ","ᴘ","ǫ","ʀ","ꜱ","ᴛ","ᴜ","ᴠ","ᴡ","x","ʏ","ᴢ"};
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            char lower = Character.toLowerCase(c);
            int idx = normal.indexOf(lower);
            if (idx != -1) {
                sb.append(caps[idx]);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toMathOffset(String text, int upperBase, int lowerBase, int digitBase) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 'A' && c <= 'Z' && upperBase != -1) {
                sb.append(new String(Character.toChars(upperBase + (c - 'A'))));
            } else if (c >= 'a' && c <= 'z' && lowerBase != -1) {
                sb.append(new String(Character.toChars(lowerBase + (c - 'a'))));
            } else if (c >= '0' && c <= '9' && digitBase != -1) {
                sb.append(new String(Character.toChars(digitBase + (c - '0'))));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toCircled(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 'A' && c <= 'Z') {
                sb.append((char) (0x24B6 + (c - 'A')));
            } else if (c >= 'a' && c <= 'z') {
                sb.append((char) (0x24D0 + (c - 'a')));
            } else if (c >= '1' && c <= '9') {
                sb.append((char) (0x2460 + (c - '1')));
            } else if (c == '0') {
                sb.append("⓪");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toDarkBubble(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            char upper = Character.toUpperCase(c);
            if (upper >= 'A' && upper <= 'Z') {
                sb.append(new String(Character.toChars(0x1F150 + (upper - 'A'))));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toSquared(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            char upper = Character.toUpperCase(c);
            if (upper >= 'A' && upper <= 'Z') {
                sb.append(new String(Character.toChars(0x1F130 + (upper - 'A'))));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toDarkSquared(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            char upper = Character.toUpperCase(c);
            if (upper >= 'A' && upper <= 'Z') {
                sb.append(new String(Character.toChars(0x1F170 + (upper - 'A'))));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toFullwidth(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 33 && c <= 126) {
                sb.append((char) (c + 65248));
            } else if (c == ' ') {
                sb.append("\u3000");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toInverted(String text) {
        String normal = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String invert = "ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz∀ᗺƆᗡƎℲ⅁HIſʞ˥WNOԀÒᴚS⊥∩ΛMX⅄Z0ƖᄅƐㄣϛ9ㄥ86";
        StringBuilder sb = new StringBuilder();
        for (int i = text.length() - 1; i >= 0; i--) {
            char c = text.charAt(i);
            int idx = normal.indexOf(c);
            sb.append(idx != -1 ? invert.charAt(idx) : c);
        }
        return sb.toString();
    }

    private static String withCombining(String text, String mark) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(c).append(mark);
        }
        return sb.toString();
    }

    private static String withSurround(String text, String symbol) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c != ' ') {
                sb.append(symbol).append(c);
            } else {
                sb.append(" ");
            }
        }
        sb.append(symbol);
        return sb.toString();
    }

    private static String withWrapper(String text, String open, String close) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c != ' ') {
                sb.append(open).append(c).append(close);
            } else {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private static String toAestheticSpaced(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            sb.append(text.charAt(i));
            if (i < text.length() - 1) {
                sb.append(" · ");
            }
        }
        return sb.toString();
    }

    private static String toGlitch(String text) {
        String[] zalgo = {"\u0336", "\u0337", "\u0338", "\u0334", "\u0335", "\u0315", "\u031b", "\u0340"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            sb.append(text.charAt(i));
            if (text.charAt(i) != ' ') {
                sb.append(zalgo[i % zalgo.length]);
            }
        }
        return sb.toString();
    }
}
