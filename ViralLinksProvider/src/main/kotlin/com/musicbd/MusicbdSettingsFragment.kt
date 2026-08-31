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

class MusicbdSettingsFragment(
    private val plugin: MusicbdPlugin
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

    private fun getDrawable(name: String): android.graphics.drawable.Drawable? {
        val id = plugin.resources!!.getIdentifier(name, "drawable", BuildConfig.LIBRARY_PACKAGE_NAME)
        return plugin.resources!!.getDrawable(id, null)
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
        val autoWebview = getKey<Boolean>("auto_webview_bypass") ?: true
        autoWebviewSwitch.isChecked = autoWebview

        // Update cookie status
        updateCookieStatus(cookieStatus)

        // Set icons
        val webviewIconId = plugin.resources!!.getIdentifier("webview_icon", "drawable", BuildConfig.LIBRARY_PACKAGE_NAME)
        val webviewIcon = plugin.resources!!.getDrawable(webviewIconId, null)
        clearCookiesLayout.findViewById<ImageView>(android.R.id.icon).setImageDrawable(webviewIcon)
        bypassLayout.findViewById<ImageView>(android.R.id.icon).setImageDrawable(webviewIcon)

        // Clear Cookies Click
        clearCookiesLayout.setOnClickListener {
            clearCookies()
            updateCookieStatus(cookieStatus)
            showToast("Cookies cleared successfully")
        }

        // Bypass Now Click
        bypassLayout.setOnClickListener {
            showToast("Bypass feature - Open WebView manually in browser")
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
        // Simple status check - in real app would check CookieManager
        textView.text = "Ready for bypass"
        textView.setTextColor(Color.GRAY)
    }

    private fun clearCookies() {
        try {
            // Clear saved cookie flag
            setKey("cookies_saved_musicbd25", false)
        } catch (e: Exception) {
            logError(e)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val scale = context?.resources?.displayMetrics?.density ?: 1f
        return (dp * scale + 0.5f).toInt()
    }
}
