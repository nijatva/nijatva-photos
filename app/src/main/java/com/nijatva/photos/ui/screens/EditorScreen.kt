package com.nijatva.photos.ui.screens

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader

/**
 * In-app photo editor — the Photalius web editor (fabric.js) merged into Nijatva Photos and
 * hosted in a WebView, so editing happens entirely inside this app with no separate install.
 *
 * Wiring that makes it fully self-contained:
 *  - assets are served by [WebViewAssetLoader] over `https://appassets.androidplatform.net/`
 *    (an https origin, so canvas export doesn't hit a "tainted canvas" error);
 *  - the selected photo is streamed on that same origin at `/photo/current`, and after the
 *    page loads we call `photalius.open({ image: … })` to open it;
 *  - the editor's "Open Photo" button is backed by a real file chooser ([WebChromeClient]);
 *  - the editor's "Done"/export is captured by the `onSave` hook in index.html and handed to
 *    the [EditorBridge], which writes it into the device gallery.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(photoUri: String?, onBack: () -> Unit) {
    val context = LocalContext.current
    var pendingChooser by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        pendingChooser?.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        )
        pendingChooser = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PhotaliusWebView(
                context = context,
                photoUri = photoUri,
                onPickFile = { callback ->
                    pendingChooser = callback
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "image/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    filePicker.launch(intent)
                }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PhotaliusWebView(
    context: Context,
    photoUri: String?,
    onPickFile: (ValueCallback<Array<Uri>>) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val uri = photoUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                .addPathHandler("/photo/", PhotoPathHandler(ctx, uri))
                .build()

            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT

                addJavascriptInterface(EditorBridge(ctx), "AndroidEditor")

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        if (uri != null) {
                            // window.photalius is exposed by index.html. open({image}) is the
                            // public entry point that loads the image as the main image.
                            view.evaluateJavascript(
                                "(function t(n){try{if(window.photalius&&window.photalius.open){" +
                                    "window.photalius.open({image:'https://appassets.androidplatform.net/photo/current'});" +
                                    "return;}}catch(e){}if(n>0)setTimeout(function(){t(n-1)},300);})(40);",
                                null
                            )
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView,
                        filePathCallback: ValueCallback<Array<Uri>>,
                        fileChooserParams: FileChooserParams
                    ): Boolean {
                        onPickFile(filePathCallback)
                        return true
                    }
                }

                // Fallback for any http(s)/data download the editor triggers directly.
                setDownloadListener(
                    DownloadListener { url, _, _, _, _ ->
                        EditorBridge(ctx).saveUrl(url)
                    }
                )

                loadUrl("https://appassets.androidplatform.net/assets/photalius/index.html")
            }
        }
    )
}

/** Streams the selected photo on the asset-loader origin so the editor can read it same-origin. */
private class PhotoPathHandler(
    private val context: Context,
    private val uri: Uri?
) : WebViewAssetLoader.PathHandler {

    override fun handle(path: String): WebResourceResponse? {
        val source = uri ?: return null
        return try {
            val stream = context.contentResolver.openInputStream(source) ?: return null
            val mime = context.contentResolver.getType(source) ?: "image/*"
            val headers = HashMap<String, String>()
            headers["Access-Control-Allow-Origin"] = "*"
            headers["Cache-Control"] = "no-store"
            WebResourceResponse(mime, null, 200, "OK", headers, stream)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * JS bridge: the editor's `onSave` hook hands exported images (data URLs) or project files
 * (JSON) here, and we persist them on-device — images into the gallery, projects into the
 * app's own files directory.
 */
private class EditorBridge(private val context: Context) {

    @JavascriptInterface
    fun save(data: String, filename: String, format: String) {
        try {
            if (format.equals("json", ignoreCase = true) || data.startsWith("{")) {
                val name = filename.ifBlank { "photalius_${System.currentTimeMillis()}.json" }
                val file = java.io.File(context.filesDir, name)
                file.writeText(data)
                toast("Project saved: ${file.name}")
                return
            }
            val bytes = decodeDataUrl(data) ?: run {
                toast("Nothing to save")
                return
            }
            val mime = if (data.startsWith("data:image/svg")) "image/svg+xml" else "image/png"
            val ext = when {
                mime.contains("svg") -> "svg"
                format.isNotBlank() -> format.lowercase()
                else -> "png"
            }
            val displayName =
                if (filename.contains('.')) filename else "${filename.ifBlank { "edited_${System.currentTimeMillis()}" }}.$ext"
            val uri = writeToGallery(displayName, mime, bytes)
            toast(if (uri != null) "Saved to Pictures/Nijatva Photos" else "Could not save")
        } catch (e: Exception) {
            toast("Save failed: ${e.message}")
        }
    }

    /** Fallback used by the DownloadListener for plain http(s)/data URLs. */
    fun saveUrl(url: String) {
        try {
            if (url.startsWith("data:")) {
                val bytes = decodeDataUrl(url) ?: return
                writeToGallery("edited_${System.currentTimeMillis()}.png", "image/png", bytes)
                toast("Saved to Pictures/Nijatva Photos")
            } else {
                toast("Download blocked — use the editor's Done button to save")
            }
        } catch (e: Exception) {
            toast("Save failed: ${e.message}")
        }
    }

    private fun writeToGallery(displayName: String, mime: String, bytes: ByteArray): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Nijatva Photos")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null
        resolver.openOutputStream(uri)?.use { it.write(bytes) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }

    private fun decodeDataUrl(data: String): ByteArray? {
        val comma = data.indexOf(',')
        if (!data.startsWith("data:") || comma < 0) return null
        val meta = data.substring(0, comma)
        val payload = data.substring(comma + 1)
        return if (meta.contains("base64")) {
            Base64.decode(payload, Base64.DEFAULT)
        } else {
            Uri.decode(payload).toByteArray()
        }
    }

    private fun toast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
