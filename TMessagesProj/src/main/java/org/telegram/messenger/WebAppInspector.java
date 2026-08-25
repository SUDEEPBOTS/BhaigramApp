package org.telegram.messenger;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import java.net.URLDecoder;

public class WebAppInspector {

    public static void showInspectorDialog(Context context, String fullUrl) {
        if (context == null) return;
        if (TextUtils.isEmpty(fullUrl)) {
            Toast.makeText(context, "WebApp URL not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        String initData = "";
        try {
            if (fullUrl.contains("#tgWebAppData=")) {
                String hashPart = fullUrl.substring(fullUrl.indexOf("#tgWebAppData=") + "#tgWebAppData=".length());
                if (hashPart.contains("&")) {
                    hashPart = hashPart.substring(0, hashPart.indexOf("&"));
                }
                initData = URLDecoder.decode(hashPart, "UTF-8");
            } else if (fullUrl.contains("tgWebAppData=")) {
                String queryPart = fullUrl.substring(fullUrl.indexOf("tgWebAppData=") + "tgWebAppData=".length());
                if (queryPart.contains("&")) {
                    queryPart = queryPart.substring(0, queryPart.indexOf("&"));
                }
                initData = URLDecoder.decode(queryPart, "UTF-8");
            }
        } catch (Throwable ignored) {}

        ScrollView scrollView = new ScrollView(context);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        scrollView.addView(layout);

        TextView titleView = new TextView(context);
        titleView.setText("🔍 WebApp / Mini App Inspector");
        titleView.setTextSize(18);
        titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        layout.addView(titleView);

        TextView urlHeader = new TextView(context);
        urlHeader.setText("\n🌐 Full WebApp URL:");
        urlHeader.setTextSize(14);
        urlHeader.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        layout.addView(urlHeader);

        TextView urlBody = new TextView(context);
        urlBody.setText(fullUrl);
        urlBody.setTextSize(12);
        urlBody.setTextIsSelectable(true);
        layout.addView(urlBody);

        if (!TextUtils.isEmpty(initData)) {
            TextView initHeader = new TextView(context);
            initHeader.setText("\n🔑 Extracted InitData (Auth Signature / Hash):");
            initHeader.setTextSize(14);
            initHeader.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            layout.addView(initHeader);

            TextView initBody = new TextView(context);
            initBody.setText(initData);
            initBody.setTextSize(12);
            initBody.setTextIsSelectable(true);
            layout.addView(initBody);
        }

        final String finalInitData = initData;
        final String finalUrl = fullUrl;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Mini App Inspector");
        builder.setView(scrollView);

        builder.setPositiveButton("Copy Full URL", (dialog, which) -> {
            copyToClipboard(context, "WebApp URL", finalUrl);
            Toast.makeText(context, "Full WebApp URL Copied to Clipboard!", Toast.LENGTH_SHORT).show();
        });

        if (!TextUtils.isEmpty(finalInitData)) {
            builder.setNeutralButton("Copy InitData", (dialog, which) -> {
                copyToClipboard(context, "InitData", finalInitData);
                Toast.makeText(context, "InitData (Auth Hash) Copied to Clipboard!", Toast.LENGTH_SHORT).show();
            });
        }

        builder.setNegativeButton("Open in Browser", (dialog, which) -> {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl));
                context.startActivity(browserIntent);
            } catch (Exception e) {
                Toast.makeText(context, "Cannot open external browser: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        builder.show();
    }

    private static void copyToClipboard(Context context, String label, String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(label, text);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
            }
        } catch (Throwable ignored) {}
    }
}
