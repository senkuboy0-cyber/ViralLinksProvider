package com.musicbd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.mvvm.logError
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MusicbdSettingsFragment(
    private val plugin: MusicbdPlugin,
    private val mainUrl: String
) : BottomSheetDialogFragment() {

    private fun <T : View> View.findViewId(name: String): T {
        val id = plugin.resources!!.getIdentifier(name, "id", BuildConfig.LIBRARY_PACKAGE_NAME)
        return this.findViewById(id)
    }

    private fun getLayout(name: String, inflater: LayoutInflater, container: ViewGroup?): View {
        val id = plugin.resources!!.getIdentifier(name, "layout", BuildConfig.LIBRARY_PACKAGE_NAME)
        val layout = plugin.resources!!.getLayout(id)
        return inflater.inflate(layout, container, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): View? {
        val settingsView = getLayout("settings", inflater, container)

        // Get views
        val autoWebviewSwitch = settingsView.findViewId<Switch>("switch_auto_webview")
        val clearCookiesLayout = settingsView.findViewId<View>("icon_clear_cookies").parent as ViewGroup
        val bypassLayout = settingsView.findViewId<View>("icon_bypass").parent as ViewGroup
        val saveButton = settingsView.findViewId<Button>("button_save")
        val cookieStatus = settingsView.findViewId<TextView>("text_cookie_status")

        // Load saved settings
        val autoWebview = getKey<Boolean>("auto_webview_bypass", defaultValue = true) ?: true
        autoWebviewSwitch.isChecked = autoWebview

        // Update cookie status
        updateCookieStatus(cookieStatus)

        // Set icons
        val webviewIconId = plugin.resources!!.getIdentifier("webview_icon", "drawable", BuildConfig.LIBRARY_PACKAGE_NAME)
        val webviewIcon = plugin.resources!!.getDrawable(webviewIconId, null)
        clearCookiesLayout.findViewById<ImageView>(android.R.id.icon).apply {
            setImageDrawable(webviewIcon)
        }
        bypassLayout.findViewById<ImageView>(android.R.id.icon).apply {
            setImageDrawable(webviewIcon)
        }

        // Clear Cookies Click
        clearCookiesLayout.setOnClickListener {
            clearCookies()
            updateCookieStatus(cookieStatus)
            showToast("Cookies cleared successfully")
        }

        // Bypass Now Click
        bypassLayout.setOnClickListener {
            openWebViewBypass()
        }

        // Save Button Click
        saveButton.setOnClickListener {
            val autoWebviewEnabled = autoWebviewSwitch.isChecked
            setKey("auto_webview_bypass", autoWebviewEnabled)
            showToast("Settings saved")
            dismiss()
        }

        return settingsView
    }

    private fun updateCookieStatus(textView: TextView) {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(mainUrl)
        
        if (cookies.isNullOrEmpty()) {
            textView.text = "No cookies saved"
            textView.setTextColor(Color.GRAY)
        } else {
            val cookieCount = cookies.split(";").filter { it.isNotBlank() }.size
            textView.text = "$cookieCount cookies saved"
            textView.setTextColor(Color.GREEN)
        }
    }

    private fun clearCookies() {
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
            // Also clear saved cookie flag
            setKey("cookies_saved_$mainUrl", false)
        } catch (e: Exception) {
            logError(e)
        }
    }

    private fun openWebViewBypass() {
        context?.let { ctx ->
            try {
                val webView = WebView(ctx)
                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
                webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(webView, true)

                val builder = AlertDialog.Builder(ctx)
                builder.setTitle("WebView Bypass")
                builder.setMessage("Solving challenge... Please wait...")
                
                val progressView = TextView(ctx).apply {
                    text = "Loading $mainUrl"
                    setPadding(32, 32, 32, 32)
                }
                builder.setView(progressView)

                var dialog: AlertDialog? = null
                dialog = builder.create()

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        progressView.text = "Page loaded: ${url?.take(50)}..."
                        
                        // Check if challenge solved (simplified check)
                        Handler(Looper.getMainLooper()).postDelayed({
                            // Try to get cookies after page loads
                            val cookies = cookieManager.getCookie(mainUrl)
                            if (!cookies.isNullOrEmpty()) {
                                setKey("cookies_saved_$mainUrl", true)
                                showToast("Cookies saved successfully!")
                                dialog?.dismiss()
                                webView.destroy()
                            }
                        }, 2000)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        super.onReceivedError(view, errorCode, description, failingUrl)
                        showToast("Error: $description")
                    }
                }

                dialog.show()
                webView.loadUrl(mainUrl)

                // Auto close after 30 seconds
                Handler(Looper.getMainLooper()).postDelayed({
                    if (dialog.isShowing) {
                        val cookies = cookieManager.getCookie(mainUrl)
                        if (!cookies.isNullOrEmpty()) {
                            setKey("cookies_saved_$mainUrl", true)
                            showToast("Cookies captured!")
                        }
                        dialog.dismiss()
                        webView.destroy()
                    }
                }, 30000)

            } catch (e: Exception) {
                logError(e)
                showToast("Failed to open WebView: ${e.message}")
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        val scale = context?.resources?.displayMetrics?.density ?: 1f
        return (dp * scale + 0.5f).toInt()
    }
}
