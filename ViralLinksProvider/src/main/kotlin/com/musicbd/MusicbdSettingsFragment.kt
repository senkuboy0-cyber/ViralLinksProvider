package com.musicbd

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey

class MusicbdSettingsFragment(private val plugin: Plugin) : BottomSheetDialogFragment() {

    private lateinit var bypassBtn: Button
    private lateinit var ctx: Context

    private fun getPluginDrawable(name: String): Drawable? {
        val id = plugin.resources?.getIdentifier(name, "drawable", "com.musicbd") ?: 0
        if (id == 0) return null
        return ResourcesCompat.getDrawable(plugin.resources!!, id, null)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ctx = requireContext()
        
        val scrollView = ScrollView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        val mainLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.TOP
        }

        val headerLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(12) }
            gravity = Gravity.CENTER_VERTICAL
        }
        
        val title = TextView(ctx).apply {
            text = "Musicbd25 Settings"
            textSize = 20f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerLayout.addView(title)

        val saveBtn = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(24), dpToPx(24))
            
            val icon = getPluginDrawable("save_icon")
            if (icon != null) {
                setImageDrawable(icon)
            } else {
                setImageResource(android.R.drawable.ic_menu_save)
            }
            
            setOnClickListener {
                AlertDialog.Builder(ctx)
                    .setTitle("Restart App?")
                    .setMessage("Save changes and restart the app?")
                    .setPositiveButton("Yes") { _, _ ->
                        restartApp(ctx)
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                        Toast.makeText(ctx, "Changes saved", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                    .show()
            }
        }
        headerLayout.addView(saveBtn)
        
        mainLayout.addView(headerLayout)
        
        val divider = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1))
                .apply { bottomMargin = dpToPx(12) }
            setBackgroundColor(Color.parseColor("#333333"))
        }
        mainLayout.addView(divider)
        
        val sectionTitle = TextView(ctx).apply {
            text = "Protection Bypass"
            textSize = 17f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dpToPx(8))
        }
        mainLayout.addView(sectionTitle)
        
        val descText = TextView(ctx).apply {
            text = "If the site shows a verification screen, use the bypass below. Auto Bypass will do this automatically."
            textSize = 13f
            setTextColor(Color.parseColor("#888888"))
            setPadding(0, 0, 0, dpToPx(12))
        }
        mainLayout.addView(descText)

        val switchLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(16) }
            gravity = Gravity.CENTER_VERTICAL
        }

        val switchTitle = TextView(ctx).apply {
            text = "Auto WebView Bypass"
            textSize = 15f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        switchLayout.addView(switchTitle)

        val autoBypassSwitch = Switch(ctx).apply {
            isChecked = getKey("musicbd_auto_bypass_v2") ?: true
            setOnCheckedChangeListener { _, isChecked ->
                setKey("musicbd_auto_bypass_v2", isChecked)
            }
        }
        switchLayout.addView(autoBypassSwitch)
        
        mainLayout.addView(switchLayout)
        
        bypassBtn = Button(ctx).apply {
            text = if (MusicbdPlugin.cfCookies.isNotBlank()) {
                "✅ Cookies Saved - Refresh"
            } else {
                "🛡️ Bypass Protection"
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(8) }
            setOnClickListener {
                val dialog = CloudflareWebViewDialog(
                    targetUrl = "https://musicbd25.site",
                    onFinished = { saved ->
                        if (saved) {
                            bypassBtn.text = "✅ Cookies Saved - Refresh"
                            Toast.makeText(ctx, "Done! Cookies saved", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                dialog.show(parentFragmentManager, "musicbd_cf_bypass")
            }
        }
        mainLayout.addView(bypassBtn)
        
        val clearBtn = Button(ctx).apply {
            text = "Clear Cookies"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(16) }
            setOnClickListener {
                AlertDialog.Builder(ctx)
                    .setTitle("Clear Cookies?")
                    .setMessage("This will remove all saved cookies. You need to bypass again.")
                    .setPositiveButton("Clear") { _, _ ->
                        
                        val cm = CookieManager.getInstance()
                        val url = "https://musicbd25.site"
                        val domain = "musicbd25.site"
                        
                        val cookieString = cm.getCookie(url)
                        if (cookieString != null) {
                            val cookies = cookieString.split(";")
                            for (cookie in cookies) {
                                val cookieName = cookie.substringBefore("=").trim()
                                if (cookieName.isNotBlank()) {
                                    cm.setCookie(url, "$cookieName=; Max-Age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT; domain=$domain; path=/")
                                    cm.setCookie(url, "$cookieName=; Max-Age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT; domain=.$domain; path=/")
                                    cm.setCookie(url, "$cookieName=; Max-Age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/")
                                }
                            }
                        }
                        cm.flush()
                        
                        MusicbdPlugin.cfCookies = ""
                        MusicbdPlugin.cfUserAgent = ""
                        MusicbdPlugin.cfCookieHost = ""
                        
                        bypassBtn.text = "🛡️ Bypass Protection"
                        Toast.makeText(ctx, "All Cookies cleared completely", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                    .show()
            }
        }
        mainLayout.addView(clearBtn)
        
        scrollView.addView(mainLayout)
        return scrollView
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }
    
    private fun dpToPx(dp: Int): Int {
        val scale = ctx.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    private fun restartApp(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component

        if (componentName != null) {
            val restartIntent = Intent.makeRestartActivityTask(componentName)
            context.startActivity(restartIntent)
            Runtime.getRuntime().exit(0)
        }
    }
}
