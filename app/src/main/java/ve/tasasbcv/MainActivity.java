package ve.tasasbcv;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    // Solo se acepta una descarga que sea realmente la app (descarta portales Wi-Fi, páginas de error, etc.)
    private static final String MARK = "<title>Tasas BCV</title>";
    private WebView web;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.setBackgroundColor(getColor(R.color.bg));
        // Cualquier enlace (p. ej. descargar el APK nuevo) se abre en el navegador:
        // el puente "Android" solo queda expuesto a nuestra propia interfaz
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                startActivity(new Intent(Intent.ACTION_VIEW, req.getUrl()));
                return true;
            }
        });
        web.addJavascriptInterface(new Object() {
            // El portapapeles web no es fiable dentro de WebView; se copia con la API nativa
            @JavascriptInterface
            public void copy(String text) {
                ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
                        .setPrimaryClip(ClipData.newPlainText("monto", text));
            }

            // Barra de estado y de navegación a juego con el tema elegido en la app
            @JavascriptInterface
            public void theme(boolean dark) {
                runOnUiThread(() -> {
                    int bg = dark ? 0xFF0C0D10 : 0xFFF5F6F8;
                    getWindow().setStatusBarColor(bg);
                    getWindow().setNavigationBarColor(bg);
                    web.setBackgroundColor(bg);
                    View decor = getWindow().getDecorView();
                    int light = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    int flags = decor.getSystemUiVisibility();
                    decor.setSystemUiVisibility(dark ? flags & ~light : flags | light);
                });
            }

            @JavascriptInterface
            public int versionCode() { return BuildConfig.VERSION_CODE; }

            @JavascriptInterface
            public String otaBase() { return BuildConfig.OTA_BASE; }
        }, "Android");

        // Misma base que antes (file:///android_asset/) para conservar lo guardado en localStorage
        web.loadDataWithBaseURL("file:///android_asset/", localHtml(), "text/html", "utf-8", null);
        setContentView(web);
        new Thread(this::downloadUpdate).start();
    }

    // Interfaz descargada por OTA; ligada a esta versión del APK para que un APK nuevo no cargue una interfaz vieja
    private File otaFile() {
        return new File(getFilesDir(), "ota-" + BuildConfig.VERSION_CODE + ".html");
    }

    private String localHtml() {
        try {
            File f = otaFile();
            try (InputStream in = f.exists() ? new java.io.FileInputStream(f) : getAssets().open("index.html")) {
                return read(in);
            }
        } catch (Exception e) {
            return "<p>Error al cargar la app</p>";
        }
    }

    // OTA: baja la interfaz publicada y la deja lista para la próxima apertura
    private void downloadUpdate() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(BuildConfig.OTA_BASE + "index.html?t=" + System.currentTimeMillis()).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            if (c.getResponseCode() != 200) return;
            String html;
            try (InputStream in = c.getInputStream()) { html = read(in); }
            if (!html.contains(MARK)) return;
            File tmp = new File(getFilesDir(), "ota.tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) { out.write(html.getBytes("UTF-8")); }
            tmp.renameTo(otaFile()); // reemplazo atómico: nunca queda un archivo a medias
        } catch (Exception ignored) {
            // Sin internet: se sigue usando la última interfaz guardada
        }
    }

    private static String read(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        for (int n; (n = in.read(b)) > 0; ) buf.write(b, 0, n);
        return buf.toString("UTF-8");
    }

    // "Atrás" cierra el gráfico/historial antes de salir de la app
    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
