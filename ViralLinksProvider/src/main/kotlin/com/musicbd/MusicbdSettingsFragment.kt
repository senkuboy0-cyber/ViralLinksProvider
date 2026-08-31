package com.musicbd

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.cloudstream3.plugins.Plugin

class MusicbdSettingsFragment(private val plugin: Plugin) : BottomSheetDialogFragment() {

    // Get package name from plugin
    private fun getPackageName(): String {
        return try {
            val context = plugin.javaClass.classLoader?.loadClass("com.musicbd.BuildConfig")
            val field = context?.getField("LIBRARY_PACKAGE_NAME")
            field?.get(null) as? String ?: "com.musicbd"
        } catch (e: Exception) {
            "com.musicbd"
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val packageName = getPackageName()
        
        val layoutId = plugin.resources!!.getIdentifier(
            "bottom_sheet_layout",
            "layout",
            packageName
        )
        val view = inflater.inflate(layoutId, container, false)

        // Get drawable IDs
        val outlineId = plugin.resources!!.getIdentifier(
            "outline",
            "drawable",
            packageName
        )
        val saveIconId = plugin.resources!!.getIdentifier(
            "save_icon",
            "drawable",
            packageName
        )

        // Get view IDs
        val saveViewId = plugin.resources!!.getIdentifier(
            "save",
            "id",
            packageName
        )
        val bypassBtnId = plugin.resources!!.getIdentifier(
            "cf_bypass_btn",
            "id",
            packageName
        )
        val clearBtnId = plugin.resources!!.getIdentifier(
            "cf_clear_btn",
            "id",
            packageName
        )

        // Save button
        val saveBtn = view.findViewById<ImageView>(saveViewId)
        saveBtn?.setImageDrawable(plugin.resources!!.getDrawable(saveIconId, null))
        saveBtn?.background = plugin.resources!!.getDrawable(outlineId, null)
        saveBtn?.setOnClickListener {
            context?.let { ctx ->
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

        // ---- Cloudflare bypass button ----------------------------------------
        val bypassBtn = view.findViewById<Button>(bypassBtnId)
        bypassBtn.setOnClickListener {
            val dialog = CloudflareWebViewDialog(
                targetUrl = "https://musicbd25.site",
                onFinished = { saved ->
                    if (saved) bypassBtn.text = "✅ CF Cookies Saved"
                }
            )
            dialog.show(parentFragmentManager, "musicbd_cf_bypass")
        }

        // Update button label to show current cookie status
        val cfCookies = MusicbdPlugin.cfCookies
        if (cfCookies.isNotBlank()) {
            bypassBtn.text = "✅ CF Cookies Saved"
        } else {
            bypassBtn.text = "🛡️ Bypass Cloudflare"
        }

        // ---- Clear CF Cookies button -----------------------------------------
        val clearBtn = view.findViewById<Button>(clearBtnId)
        clearBtn.setOnClickListener {
            context?.let { ctx ->
                AlertDialog.Builder(ctx)
                    .setTitle("Clear CF Cookies?")
                    .setMessage("This will remove the saved Cloudflare cookies and User-Agent.")
                    .setPositiveButton("Clear") { _, _ ->
                        // Remove from Android's CookieManager
                        val host = MusicbdPlugin.cfCookieHost
                        if (host.isNotBlank()) {
                            val cm = CookieManager.getInstance()
                            listOf("cf_clearance", "__ddg1_", "__ddg2_", "__cfruid").forEach { name ->
                                cm.setCookie(host, "$name=; Max-Age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT")
                            }
                            cm.flush()
                        }
                        // Clear plugin store
                        MusicbdPlugin.cfCookies = ""
                        MusicbdPlugin.cfUserAgent = ""
                        MusicbdPlugin.cfCookieHost = ""
                        // Reset bypass button label
                        bypassBtn.text = "🛡️ Bypass Cloudflare"
                        Toast.makeText(ctx, "✅ CF Cookies cleared", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                    .show()
            }
        }

        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
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
