package org.telegram.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CrashReportActivity extends Activity {

    public static final String EXTRA_ERROR_TITLE = "extra_error_title";
    public static final String EXTRA_ERROR_STACKTRACE = "extra_error_stacktrace";
    public static final String EXTRA_ERROR_DEVICE = "extra_error_device";

    public static void showCrash(Context context, Throwable throwable) {
        try {
            if (context == null) return;

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String stackTrace = sw.toString();

            String errorTitle = throwable.getClass().getSimpleName() + ": " + throwable.getMessage();

            StringBuilder device = new StringBuilder();
            device.append("Brand: ").append(Build.BRAND).append("\n");
            device.append("Model: ").append(Build.MODEL).append(" (").append(Build.DEVICE).append(")\n");
            device.append("Android: ").append(Build.VERSION.RELEASE).append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");
            device.append("CPU ABI: ").append(Build.CPU_ABI).append("\n");

            Intent intent = new Intent(context, CrashReportActivity.class);
            intent.putExtra(EXTRA_ERROR_TITLE, errorTitle);
            intent.putExtra(EXTRA_ERROR_STACKTRACE, stackTrace);
            intent.putExtra(EXTRA_ERROR_DEVICE, device.toString());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);

            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        final String errorTitle = getIntent().getStringExtra(EXTRA_ERROR_TITLE) != null ? getIntent().getStringExtra(EXTRA_ERROR_TITLE) : "Unknown Error";
        final String stackTrace = getIntent().getStringExtra(EXTRA_ERROR_STACKTRACE) != null ? getIntent().getStringExtra(EXTRA_ERROR_STACKTRACE) : "No stack trace available";
        final String deviceInfo = getIntent().getStringExtra(EXTRA_ERROR_DEVICE) != null ? getIntent().getStringExtra(EXTRA_ERROR_DEVICE) : "";

        final String fullReport = "=== BHAICHARA CRASH REPORT ===\n\n" +
                "ERROR: " + errorTitle + "\n\n" +
                "--- DEVICE INFO ---\n" + deviceInfo + "\n" +
                "--- STACK TRACE ---\n" + stackTrace;

        // Root Layout
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF141A21); // Dark background
        root.setPadding(dp(16), dp(32), dp(16), dp(16));

        // Header Title
        TextView titleView = new TextView(this);
        titleView.setText("Bhaichara Crash Guard");
        titleView.setTextColor(0xFFFF5252); // Red
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(titleView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Subtitle
        TextView subView = new TextView(this);
        subView.setText("Telegram encountered an error. Here is the exact reason:");
        subView.setTextColor(0xFF8E9BA8);
        subView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        subView.setGravity(Gravity.CENTER_HORIZONTAL);
        subView.setPadding(0, dp(4), 0, dp(12));
        root.addView(subView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Scroll View for logs
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(0xFF0E1217);
        scrollView.setPadding(dp(12), dp(12), dp(12), dp(12));

        LinearLayout logContainer = new LinearLayout(this);
        logContainer.setOrientation(LinearLayout.VERTICAL);

        // Error reason
        TextView reasonView = new TextView(this);
        reasonView.setText(errorTitle);
        reasonView.setTextColor(0xFFFFB74D); // Orange
        reasonView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        reasonView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        reasonView.setPadding(0, 0, 0, dp(10));
        logContainer.addView(reasonView);

        // Device info
        TextView devView = new TextView(this);
        devView.setText(deviceInfo);
        devView.setTextColor(0xFF4FC3F7); // Cyan
        devView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        devView.setTypeface(Typeface.MONOSPACE);
        devView.setPadding(0, 0, 0, dp(10));
        logContainer.addView(devView);

        // Stack trace
        TextView traceView = new TextView(this);
        traceView.setText(stackTrace);
        traceView.setTextColor(0xFFECEFF1); // White-grey
        traceView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        traceView.setTypeface(Typeface.MONOSPACE);
        logContainer.addView(traceView);

        scrollView.addView(logContainer);

        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollLp.setMargins(0, 0, 0, dp(12));
        root.addView(scrollView, scrollLp);

        // Buttons Layout
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(Gravity.CENTER);

        // Copy Button
        Button copyBtn = new Button(this);
        copyBtn.setText("Copy Error Reason");
        copyBtn.setBackgroundColor(0xFF37474F);
        copyBtn.setTextColor(Color.WHITE);
        copyBtn.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Bhaichara Crash", fullReport));
                Toast.makeText(CrashReportActivity.this, "Crash Reason Copied to Clipboard!", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams copyLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        copyLp.setMargins(0, 0, dp(8), 0);
        buttonLayout.addView(copyBtn, copyLp);

        // Restart Button
        Button restartBtn = new Button(this);
        restartBtn.setText("Restart Bhaichara");
        restartBtn.setBackgroundColor(0xFF2E7D32); // Green
        restartBtn.setTextColor(Color.WHITE);
        restartBtn.setOnClickListener(v -> {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(launchIntent);
            }
            finish();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        });
        LinearLayout.LayoutParams restartLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        buttonLayout.addView(restartBtn, restartLp);

        root.addView(buttonLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private int dp(float value) {
        return (int) Math.ceil(getResources().getDisplayMetrics().density * value);
    }
}
