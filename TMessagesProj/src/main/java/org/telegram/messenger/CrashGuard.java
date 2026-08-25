package org.telegram.messenger;

import android.text.TextUtils;

public class CrashGuard {

    private static final int MAX_SAFE_CONSECUTIVE_COMBINING = 10;
    private static final int MAX_SAFE_RTL_OVERFLOW = 20;

    public static boolean isAntiFreezeEnabled() {
        return MessagesController.getGlobalMainSettings().getBoolean("anti_freeze_guard", true);
    }

    public static CharSequence sanitizeText(CharSequence text) {
        if (!isAntiFreezeEnabled() || TextUtils.isEmpty(text)) {
            return text;
        }

        int length = text.length();
        if (length > 20000) {
            text = text.subSequence(0, 20000);
            length = 20000;
        }

        StringBuilder clean = new StringBuilder(length);
        int consecutiveCombining = 0;
        int rtlCount = 0;

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            // Filter out excessive zero-width / bidirectional override spam
            if (c >= '\u202A' && c <= '\u202E' || c >= '\u2066' && c <= '\u2069') {
                rtlCount++;
                if (rtlCount > MAX_SAFE_RTL_OVERFLOW) {
                    continue;
                }
            }

            // Filter out stacked zalgo combining characters (causes Android TextView measure crash)
            int type = Character.getType(c);
            if (type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK || type == Character.ENCLOSING_MARK) {
                consecutiveCombining++;
                if (consecutiveCombining > MAX_SAFE_CONSECUTIVE_COMBINING) {
                    continue;
                }
            } else {
                consecutiveCombining = 0;
            }

            // Unpaired surrogates check
            if (Character.isHighSurrogate(c)) {
                if (i + 1 < length && Character.isLowSurrogate(text.charAt(i + 1))) {
                    clean.append(c);
                    clean.append(text.charAt(i + 1));
                    i++;
                    continue;
                } else {
                    continue; // Skip orphan high surrogate
                }
            } else if (Character.isLowSurrogate(c)) {
                continue; // Skip orphan low surrogate
            }

            clean.append(c);
        }

        return clean.toString();
    }
}
