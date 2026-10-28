package com.mylens.kit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;

/**
 * Web visual search: opens the visual-search camera page in a WebView and
 * attaches the photo into its file input via DataTransfer (no button click,
 * no file chooser), with the WebView's onShowFileChooser as automatic
 * fallback. CSS hides everything except image search; sign-in/account pages
 * are hard-blocked. Internal — use MyLens.openVisualMatches().
 */
public class BingVisionActivity extends Activity {

    private static final String[] BLOCKED_HOSTS = {
            "login.live.com", "account.live.com", "signup.live.com",
            "login.microsoftonline.com", "account.microsoft.com",
            "login.microsoft.com", "outlook.live.com/owa"
    };

    private WebView web;
    private String photoPath;
    private ValueCallback<Uri[]> pendingFileChooser;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        photoPath = getIntent().getStringExtra("photo");

        web = new WebView(this);
        web.setBackgroundColor(0xFF121212);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setUserAgentString(s.getUserAgentString().replace("; wv", "") + " Mobile");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String u = request.getUrl().toString().toLowerCase();
                for (String h : BLOCKED_HOSTS) {
                    if (u.contains(h)) {
                        Toast.makeText(BingVisionActivity.this,
                                "Sign-in pages are blocked", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // Serve the captured photo to page JavaScript without a giant base64 string.
                Uri u = request.getUrl();
                if ("__mylens_photo__".equals(u.getLastPathSegment()) && photoPath != null) {
                    try {
                        File f = new File(photoPath);
                        if (f.exists()) {
                            return new WebResourceResponse("image/jpeg", null, new FileInputStream(f));
                        }
                    } catch (Throwable ignored) {}
                }
                return null;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                injectCss(view);
                if (photoPath != null) {
                    try {
                        Toast.makeText(BingVisionActivity.this,
                                "Attaching photo to visual search…", Toast.LENGTH_SHORT).show();
                    } catch (Throwable ignored) {}
                    injectPhoto(view);
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                              FileChooserParams params) {
                if (pendingFileChooser != null) {
                    try { pendingFileChooser.onReceiveValue(null); } catch (Throwable ignored) {}
                }
                pendingFileChooser = callback;
                if (photoPath != null) {
                    try {
                        Uri uri = androidx.core.content.FileProvider.getUriForFile(
                                BingVisionActivity.this, MyLens.fileProviderAuthority,
                                new File(photoPath));
                        callback.onReceiveValue(new Uri[]{uri});
                        pendingFileChooser = null;
                        return true;
                    } catch (Throwable ignored) {}
                }
                return false; // let the system chooser open
            }
        });

        web.loadUrl("https://www.bing.com/camera");
    }

    private static void injectCss(WebView view) {
        String css = ".b_logoArea,#mHamburger,#HBMenu,#navs_toT,li[id^='b-scopeListItem'],"
                + "nav.b_scopebar,a[href*='login.live.com'],a[href*='login.microsoftonline.com'],"
                + "a[href*='account.microsoft.com'],.id_button,#id_l,.id_avatar,.b_signin,"
                + "#bepsv,.mic_cont,#sb_form_q{display:none!important;}";
        String js = "(function(){try{var s=document.createElement('style');s.textContent="
                + jsString(css) + ";(document.head||document.documentElement).appendChild(s);}catch(e){}})();";
        try { view.evaluateJavascript(js, null); } catch (Throwable ignored) {}
    }

    private static void injectPhoto(WebView view) {
        // Waits for the visual-search file input, builds a File from the intercepted
        // photo stream, assigns it via DataTransfer and fires 'change' directly.
        // Retries for up to ~40s; the done flag is set only on success.
        String js =
                "(function(){try{"
                + "if(window.__mylensDone)return;"
                + "var n=80;"
                + "var go=function(){"
                + "  try{"
                + "    if(window.__mylensDone)return;"
                + "    var inp=document.getElementById('bcam_sbi_file');"
                + "    if(!inp){var l=document.querySelectorAll('input[type=file]');"
                + "      for(var i=0;i<l.length;i++){if((l[i].accept||'').indexOf('image')>=0){inp=l[i];break;}}"
                + "      if(!inp&&l.length)inp=l[0];}"
                + "    if(!inp){if(n-->0){setTimeout(go,500);}return;}"
                + "    fetch('/__mylens_photo__').then(function(r){if(!r.ok)throw 0;return r.arrayBuffer()})"
                + "      .then(function(b){"
                + "        var f=new File([b],'photo.jpg',{type:'image/jpeg'});"
                + "        var dt=new DataTransfer();dt.items.add(f);"
                + "        inp.files=dt.files;"
                + "        inp.dispatchEvent(new Event('change',{bubbles:true}));"
                + "        window.__mylensDone=true;"
                + "      }).catch(function(){if(n-->0){setTimeout(go,500);}});"
                + "  }catch(e){if(n-->0){setTimeout(go,500);}}"
                + "};"
                + "go();"
                + "}catch(e){}})();";
        try { view.evaluateJavascript(js, null); } catch (Throwable ignored) {}
    }

    /** JSON-escape a string for embedding in JS. */
    private static String jsString(String s) {
        StringBuilder sb = new StringBuilder("'");
        for (char c : s.toCharArray()) {
            if (c == '\'') sb.append("\\'");
            else if (c == '\\') sb.append("\\\\");
            else if (c == '\n') sb.append("\\n");
            else sb.append(c);
        }
        return sb.append('\'').toString();
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.loadUrl("about:blank");
            web.destroy();
        }
        super.onDestroy();
    }
}
