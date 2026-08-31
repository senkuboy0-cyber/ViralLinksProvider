package com.musicbd

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
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
        // Try multiple package names for resources
        val packageNames = listOf(
            "com.musicbd",
            plugin.javaClass.`package`.name,
            requireContext().packageName
        )
        
        var layoutId = 0
        var foundPackage = ""
        
        for (pkg in packageNames) {
            val id = plugin.resources?.getIdentifier(
                "bottom_sheet_layout",
                "layout",
                pkg
            ) ?: 0
            Log.d("MusicbdSettings", "Trying package: $pkg, layoutId: $id")
            if (id != 0) {
                layoutId = id
                foundPackage = pkg
                break
            }
        }
        
        Log.d("MusicbdSettings", "Found package: $foundPackage, layoutId: $layoutId")
        
        if (layoutId == 0) {
            // Create a simple debug view
            return TextView(context).apply {
                text = "Error: Layout not found\n\nPackages tried: ${packageNames.joinToString()}\n\nPlugin class: ${plugin.javaClass.name}\nPlugin package: ${plugin.javaClass.`package`.name}"
                setPadding(32, 32, 32, 32)
            }
        }
        
        val view = inflater.inflate(layoutId, container, false)

        // Get drawable IDs using found package
        val outlineId = plugin.resources?.getIdentifier("outline", "drawable", foundPackage) ?: 0
        val saveIconId = plugin.resources?.getIdentifier("save_icon", "drawable", foundPackage) ?: 0

        // Get view IDs
        val saveViewId = plugin.resources?.getIdentifier("save", "id", foundPackage) ?: 0
        val bypassBtnId = plugin.resources?.getIdentifier("cf_bypass_btn", "id", foundPackage) ?: 0
        val clearBtnId = plugin.resources?.getIdentifier("cf_clear_btn", "id", foundPackage) ?: 0

        Log.d("MusicbdSettings", "IDs - outline: $outlineId, saveIcon: $saveIconId, saveView: $saveViewId, bypass: $bypassBtnId, clear: $clearBtnId")

        // Save button
        if (saveViewId != 0) {
            val saveBtn = view.findViewById<ImageView>(saveViewId)
            if (saveIconId != 0) {
                try {
                    saveBtn?.setImageDrawable(plugin.resources?.getDrawable(saveIconId, null))
                } catch (e: Exception) {
                    Log.e("MusicbdSettings", "Error setting icon: ${e.message}")
                }
            }
            if (outlineId != 0) {
                try {
                    saveBtn?.background = plugin.resources?.getDrawable(outlineId, null)
                } catch (e: Exception) {
                    Log.e("MusicbdSettings", "Error setting background: ${e.message}")
                }
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
