package com.musicbd

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.cloudstream3.plugins.Plugin

class MusicbdSettingsFragment(private val plugin: Plugin) : BottomSheetDialogFragment() {

    private lateinit var bypassBtn: Button
    private lateinit var ctx: Context

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        
        // Title
        val title = TextView(ctx).apply {
            text = "Musicbd25 Settings"
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dpToPx(12))
        }
        mainLayout.addView(title)
        
        // Divider
        val divider = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply { bottomMargin = dpToPx(12) }
            setBackgroundColor(Color.parseColor("#333333"))
        }
        mainLayout.addView(divider)
        
        // Section Title
        val sectionTitle = TextView(ctx).apply {
            text = "Cloudflare Protection"
            textSize = 17f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dpToPx(8))
        }
        mainLayout.addView(sectionTitle)
        
        // Description
        val descText = TextView(ctx).apply {
            text = "If Musicbd25 shows challenge screen, use WebView to bypass."
            textSize = 13f
            setTextColor(Color.parseColor("#888888"))
            setPadding(0, 0, 0, dpToPx(12))
        }
        mainLayout.addView(descText)
        
        // Bypass Button
        bypassBtn = Button(ctx).apply {
            text = if (MusicbdPlugin.cfCookies.isNotBlank()) {
                "✅ CF Cookies Saved"
            } else {
                "🛡️ Bypass Cloudflare"
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
                            bypassBtn.text = "✅ CF Cookies Saved"
                        }
                    }
                )
                dialog.show(parentFragmentManager, "musicbd_cf_bypass")
            }
        }
        mainLayout.addView(bypassBtn)
        
        // Clear Button
        val clearBtn = Button(ctx).apply {
            text = "Clear CF Cookies"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(16) }
            setOnClickListener {
                AlertDialog.Builder(ctx)
                    .setTitle("Clear CF Cookies?")
                    .setMessage("This will remove saved cookies. You need to bypass again.")
                    .setPositiveButton("Clear") { _, _ ->
                        val host = MusicbdPlugin.cfCookieHost
                        if (host.isNotBlank()) {
                            val cm = CookieManager.getInstance()
                            listOf("cf_clearance", "__ddg1_", "__ddg2_", "__cfruid").forEach { name ->
                                cm.setCookie(host, "$name=; Max-Age=0")
                            }
                            cm.flush()
                        }
                        MusicbdPlugin.cfCookies = ""
                        MusicbdPlugin.cfUserAgent = ""
                        MusicbdPlugin.cfCookieHost = ""
                        bypassBtn.text = "🛡️ Bypass Cloudflare"
                        Toast.makeText(ctx, "✅ CF Cookies cleared", Toast.LENGTH_SHORT).show()
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
}
