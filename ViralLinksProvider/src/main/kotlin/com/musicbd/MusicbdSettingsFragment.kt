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

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Use the plugin's class loader to get the correct package name
        val pluginPackageName = plugin.javaClass.`package`.name
        
        // Use requireContext().resources for runtime resource access
        val resources = requireContext().resources
        val packageName = requireContext().packageName
        
        // Get layout ID
        val layoutId = resources.getIdentifier(
            "bottom_sheet_layout",
            "layout",
            packageName
        )
        
        if (layoutId == 0) {
            // Fallback: try direct R class
            return try {
                val rClass = Class.forName("$packageName.R\$layout")
                val field = rClass.getField("bottom_sheet_layout")
                val id = field.getInt(null)
                inflater.inflate(id, container, false)
            } catch (e: Exception) {
                android.widget.TextView(context).apply {
                    text = "Settings Error: Layout not found"
                }
            }
        }
        
        val view = inflater.inflate(layoutId, container, false)

        // Get drawable IDs
        val outlineId = resources.getIdentifier(
            "outline",
            "drawable",
            packageName
        )
        val saveIconId = resources.getIdentifier(
            "save_icon",
            "drawable",
            packageName
        )

        // Get view IDs
        val saveViewId = resources.getIdentifier("save", "id", packageName)
        val bypassBtnId = resources.getIdentifier("cf_bypass_btn", "id", packageName)
        val clearBtnId = resources.getIdentifier("cf_clear_btn", "id", packageName)

        // Save button
        if (saveViewId != 0) {
            val saveBtn = view.findViewById<ImageView>(saveViewId)
            if (saveIconId != 0) {
                saveBtn?.setImageDrawable(resources.getDrawable(saveIconId, null))
            }
            if (outlineId != 0) {
                saveBtn?.background = resources.getDrawable(outlineId, null)
            }
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
        }

        // Cloudflare bypass button
        if (bypassBtnId != 0) {
            val bypassBtn = view.findViewById<Button>(bypassBtnId)
            bypassBtn?.setOnClickListener {
                val dialog = CloudflareWebViewDialog(
                    targetUrl = "https://musicbd25.site",
                    onFinished = { saved ->
                        if (saved) bypassBtn.text = "✅ CF Cookies Saved"
                    }
                )
                dialog.show(parentFragmentManager, "musicbd_cf_bypass")
            }

            // Update button label
            val cfCookies = MusicbdPlugin.cfCookies
            bypassBtn?.text = if (cfCookies.isNotBlank()) {
                "✅ CF Cookies Saved"
            } else {
                "🛡️ Bypass Cloudflare"
            }
        }

        // Clear CF Cookies button
        if (clearBtnId != 0) {
            val clearBtn = view.findViewById<Button>(clearBtnId)
            clearBtn?.setOnClickListener {
                context?.let { ctx ->
                    AlertDialog.Builder(ctx)
                        .setTitle("Clear CF Cookies?")
                        .setMessage("This will remove the saved Cloudflare cookies.")
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
                            
                            if (bypassBtnId != 0) {
                                view.findViewById<Button>(bypassBtnId)?.text = "🛡️ Bypass Cloudflare"
                            }
                            Toast.makeText(ctx, "✅ CF Cookies cleared", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                        .show()
                }
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
