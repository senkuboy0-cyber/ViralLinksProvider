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
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.plugins.Plugin

class MusicbdSettingsFragment(private val plugin: Plugin) : BottomSheetDialogFragment() {

    private fun <T : View> View.findView(name: String): T {
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

    private fun View.makeTvCompatible() {
        val outlineId = plugin.resources!!.getIdentifier("outline", "drawable", BuildConfig.LIBRARY_PACKAGE_NAME)
        this.background = plugin.resources!!.getDrawable(outlineId, null)
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val settings = getLayout("bottom_sheet_layout", inflater, container)

        // Title
        settings.findView<android.widget.TextView>("text1").text = "Musicbd25 Settings"

        // Save button
        val saveIconId = plugin.resources!!.getIdentifier("save_icon", "drawable", BuildConfig.LIBRARY_PACKAGE_NAME)
        val saveBtn = settings.findView<ImageView>("save")
        saveBtn.setImageDrawable(plugin.resources!!.getDrawable(saveIconId, null))
        saveBtn.makeTvCompatible()
        saveBtn.setOnClickListener {
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

        // Cloudflare bypass button
        val bypassBtn = settings.findView<Button>("cf_bypass_btn")
        bypassBtn.setOnClickListener {
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
        bypassBtn.text = if (cfCookies.isNotBlank()) {
            "✅ CF Cookies Saved"
        } else {
            "🛡️ Bypass Cloudflare"
        }

        // Clear CF Cookies button
        val clearBtn = settings.findView<Button>("cf_clear_btn")
        clearBtn.setOnClickListener {
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
                        bypassBtn.text = "🛡️ Bypass Cloudflare"
                        Toast.makeText(ctx, "✅ CF Cookies cleared", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                    .show()
            }
        }

        return settings
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
