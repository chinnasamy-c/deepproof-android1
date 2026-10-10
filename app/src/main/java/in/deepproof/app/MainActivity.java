package in.deepproof.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.graphics.Color;
import android.view.View;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_PDFS = 701;
    private final ArrayList<Uri> selected = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinearLayout root, fileList, resultsList;
    private TextView status, counts;
    private EditText apiUrl;
    private Button verifyButton, csvButton, zipButton;
    private JSONArray lastResults = new JSONArray();

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(11,18,32));
        getWindow().setNavigationBarColor(Color.rgb(11,18,32));
        buildUi();
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private void addSpace(LinearLayout parent, int h) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(h)));
    }

    private TextView cardTitle(String s) {
        TextView t = text(s, 18, Color.rgb(237,244,255), true);
        return t;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(11,18,32));
        root = column();
        root.setPadding(dp(18), dp(24), dp(18), dp(28));
        scroll.addView(root);
        setContentView(scroll);

        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("✓", 28, Color.rgb(11,18,32), true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackgroundColor(Color.rgb(204,228,255));
        brand.addView(logo, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout titles = column();
        titles.setPadding(dp(12), 0, 0, 0);
        titles.addView(text("DeepProof", 25, Color.WHITE, true));
        titles.addView(text("PDF SIGNATURE VERIFICATION", 11, Color.rgb(158,176,200), false));
        brand.addView(titles);
        root.addView(brand);
        addSpace(root, 24);
        root.addView(text("Check your PDF signatures.", 27, Color.WHITE, true));
        addSpace(root, 5);
        root.addView(text("Select one or more PDFs, send them to your DeepProof verification backend, and review the real validation result.", 14, Color.rgb(158,176,200), false));
        addSpace(root, 22);

        LinearLayout panel = column();
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackgroundColor(Color.rgb(18,29,48));
        root.addView(panel, new LinearLayout.LayoutParams(-1, -2));
        panel.addView(cardTitle("Backend URL"));
        addSpace(panel, 6);
        panel.addView(text("Paste your deployed HTTPS backend URL. Example: https://your-service.onrender.com", 12, Color.rgb(158,176,200), false));
        apiUrl = new EditText(this);
        apiUrl.setSingleLine(true);
        apiUrl.setText("https://YOUR-BACKEND-URL");
        apiUrl.setHint("https://your-service.onrender.com");
        apiUrl.setTextColor(Color.WHITE);
        apiUrl.setHintTextColor(Color.GRAY);
        apiUrl.setPadding(dp(10), dp(10), dp(10), dp(10));
        apiUrl.setBackgroundColor(Color.rgb(9,19,33));
        panel.addView(apiUrl, new LinearLayout.LayoutParams(-1, -2));
        addSpace(panel, 14);

        Button pick = button("Choose PDF files", true);
        panel.addView(pick, matchWrap());
        pick.setOnClickListener(v -> pickPdfs());
        addSpace(panel, 10);
        fileList = column();
        panel.addView(fileList);
        addSpace(panel, 10);
        verifyButton = button("Verify selected PDFs", true);
        verifyButton.setEnabled(false);
        panel.addView(verifyButton, matchWrap());
        verifyButton.setOnClickListener(v -> verify());
        addSpace(panel, 8);
        status = text("No files selected.", 13, Color.rgb(158,176,200), false);
        panel.addView(status);
        addSpace(root, 18);

        LinearLayout report = column();
        report.setPadding(dp(16), dp(16), dp(16), dp(16));
        report.setBackgroundColor(Color.rgb(18,29,48));
        root.addView(report, matchWrap());
        report.addView(cardTitle("Batch report"));
        addSpace(report, 5);
        counts = text("Files: 0  •  Passed checks: 0  •  Need review: 0", 13, Color.rgb(158,176,200), false);
        report.addView(counts);
        addSpace(report, 10);
        LinearLayout exports = new LinearLayout(this);
        csvButton = button("Export CSV", false);
        zipButton = button("Export ZIP", false);
        csvButton.setEnabled(false); zipButton.setEnabled(false);
        exports.addView(csvButton, new LinearLayout.LayoutParams(0, dp(46), 1));
        Space gap = new Space(this); exports.addView(gap, new LinearLayout.LayoutParams(dp(8), 1));
        exports.addView(zipButton, new LinearLayout.LayoutParams(0, dp(46), 1));
        report.addView(exports);
        csvButton.setOnClickListener(v -> exportReport("/api/export/csv", "deepproof-report.csv"));
        zipButton.setOnClickListener(v -> exportReport("/api/export/zip", "deepproof-reports.zip"));
        addSpace(report, 14);
        resultsList = column();
        report.addView(resultsList);
        addSpace(root, 16);
        root.addView(text("Privacy: PDFs are sent to the backend you configure. Use only a backend you trust. This app is not an official government service.", 12, Color.rgb(158,176,200), false));
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private Button button(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(null, android.graphics.Typeface.BOLD);
        b.setTextColor(primary ? Color.rgb(16,36,59) : Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
            primary ? Color.rgb(204,228,255) : Color.rgb(24,38,59)));
        return b;
    }

    private void pickPdfs() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_PDFS);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 702 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri destination = data.getData();
            try (OutputStream out = getContentResolver().openOutputStream(destination)) {
                if (out == null || saveBytes == null) throw new IOException("Could not save export");
                out.write(saveBytes);
                status.setText("Saved report: " + displayName(destination));
            } catch (Exception e) {
                status.setText("Could not save report: " + e.getMessage());
            } finally {
                saveBytes = null;
            }
            return;
        }
        if (requestCode != PICK_PDFS || resultCode != RESULT_OK || data == null) return;
        selected.clear();
        if (data.getClipData() != null) {
            int n = Math.min(data.getClipData().getItemCount(), 25);
            for (int i = 0; i < n; i++) selected.add(data.getClipData().getItemAt(i).getUri());
        } else if (data.getData() != null) selected.add(data.getData());
        renderFiles();
        verifyButton.setEnabled(!selected.isEmpty());
        status.setText(selected.size() + " PDF file(s) selected. Maximum 25 per batch.");
    }

    private String displayName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return c.getString(i);
            }
        } catch (Exception ignored) {}
        return "document.pdf";
    }

    private void renderFiles() {
        fileList.removeAllViews();
        for (Uri uri : selected) {
            TextView row = text("• " + displayName(uri), 13, Color.rgb(205,218,237), false);
            row.setPadding(0, dp(4), 0, dp(4));
            fileList.addView(row);
        }
    }

    private void verify() {
        String base = apiUrl.getText().toString().trim().replaceAll("/+$", "");
        if (!base.startsWith("https://") || base.contains("YOUR-BACKEND-URL")) {
            status.setText("Enter your real HTTPS backend URL first.");
            return;
        }
        if (selected.isEmpty()) { status.setText("Choose at least one PDF."); return; }
        verifyButton.setEnabled(false);
        status.setText("Uploading and validating PDFs…");
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String boundary = "----DeepProof" + System.currentTimeMillis();
                URL url = new URL(base + "/api/validate");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(120000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                try (OutputStream out = conn.getOutputStream()) {
                    for (Uri uri : selected) {
                        String name = displayName(uri).replace("\"", "_").replace("\r", "_").replace("\n", "_");
                        write(out, "--" + boundary + "\r\n");
                        write(out, "Content-Disposition: form-data; name=\"files\"; filename=\"" + name + "\"\r\n");
                        write(out, "Content-Type: application/pdf\r\n\r\n");
                        try (InputStream in = getContentResolver().openInputStream(uri)) {
                            if (in == null) throw new IOException("Could not open " + name);
                            byte[] buf = new byte[8192]; int read;
                            while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
                        }
                        write(out, "\r\n");
                    }
                    write(out, "--" + boundary + "--\r\n");
                }
                int code = conn.getResponseCode();
                InputStream response = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                String body = readAll(response);
                if (code < 200 || code >= 300) throw new IOException("Backend HTTP " + code + ": " + body);
                JSONObject json = new JSONObject(body);
                JSONArray got = json.optJSONArray("results");
                runOnUiThread(() -> {
                    lastResults = got == null ? new JSONArray() : got;
                    renderResults();
                    status.setText("Validation complete. Batch ID: " + json.optString("batch_id", "n/a"));
                    verifyButton.setEnabled(!selected.isEmpty());
                    csvButton.setEnabled(lastResults.length() > 0);
                    zipButton.setEnabled(lastResults.length() > 0);
                });
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "Unknown error" : e.getMessage();
                runOnUiThread(() -> {
                    status.setText("Could not validate: " + msg + ". Check backend URL and deployment logs.");
                    verifyButton.setEnabled(!selected.isEmpty());
                });
            } finally { if (conn != null) conn.disconnect(); }
        });
    }

    private void renderResults() {
        resultsList.removeAllViews();
        int valid = 0;
        for (int i = 0; i < lastResults.length(); i++) {
            JSONObject r = lastResults.optJSONObject(i);
            if (r == null) continue;
            String state = r.optString("status", "ERROR");
            if ("VALID".equals(state)) valid++;
            LinearLayout card = column();
            card.setPadding(dp(10), dp(10), dp(10), dp(10));
            card.setBackgroundColor(Color.rgb(9,19,33));
            TextView name = text(r.optString("filename", "PDF"), 14, Color.WHITE, true);
            card.addView(name);
            TextView badge = text(state.replace('_', ' '), 12,
                "VALID".equals(state) ? Color.rgb(117,214,160) : Color.rgb(255,213,138), true);
            card.addView(badge);
            TextView message = text(r.optString("message", ""), 12, Color.rgb(158,176,200), false);
            card.addView(message);
            JSONArray signatures = r.optJSONArray("signatures");
            if (signatures != null) {
                for (int j = 0; j < signatures.length(); j++) {
                    JSONObject sig = signatures.optJSONObject(j);
                    if (sig == null) continue;
                    TextView detail = text("Signature " + sig.optInt("signature_number") + ": "
                        + sig.optString("status") + "\n" + sig.optString("details"), 11,
                        Color.rgb(189,203,224), false);
                    detail.setPadding(0, dp(6), 0, 0);
                    card.addView(detail);
                }
            }
            LinearLayout.LayoutParams lp = matchWrap(); lp.bottomMargin = dp(8);
            resultsList.addView(card, lp);
        }
        counts.setText("Files: " + lastResults.length() + "  •  Passed checks: " + valid
            + "  •  Need review: " + (lastResults.length() - valid));
    }

    private void exportReport(String endpoint, String filename) {
        String base = apiUrl.getText().toString().trim().replaceAll("/+$", "");
        if (!base.startsWith("https://") || base.contains("YOUR-BACKEND-URL")) {
            status.setText("Enter your real HTTPS backend URL first."); return;
        }
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(base + endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                JSONObject payload = new JSONObject(); payload.put("results", lastResults);
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) throw new IOException("Export HTTP " + code);
                byte[] bytes;
                try (InputStream in = conn.getInputStream(); ByteArrayOutputStream b = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[8192]; int n;
                    while ((n = in.read(buf)) != -1) b.write(buf, 0, n);
                    bytes = b.toByteArray();
                }
                String mime = endpoint.endsWith("/csv") ? "text/csv" : "application/zip";
                runOnUiThread(() -> saveFile(bytes, mime, filename));
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "Export failed" : e.getMessage();
                runOnUiThread(() -> status.setText(msg));
            } finally { if (conn != null) conn.disconnect(); }
        });
    }

    private void saveFile(byte[] bytes, String mime, String name) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime);
        intent.putExtra(Intent.EXTRA_TITLE, name);
        saveBytes = bytes;
        saveMime = mime;
        startActivityForResult(intent, 702);
    }

    private byte[] saveBytes;
    private String saveMime;

    @Override protected void onResume() { super.onResume(); }

    private void write(OutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.UTF_8));
    }

    private String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        try (InputStream stream = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192]; int n;
            while ((n = stream.read(b)) != -1) out.write(b, 0, n);
            return out.toString("UTF-8");
        }
    }

}
