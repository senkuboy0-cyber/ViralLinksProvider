package com.musicbd

import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class MusicbdPlugin : Plugin() {
    
    override fun load() {
        registerMainAPI(MusicbdProvider())

        this.openSettings = { ctx ->
            val activity = ctx as AppCompatActivity
            val frag = MusicbdSettingsFragment(this)
            frag.show(activity.supportFragmentManager, "")
        }
    }

    companion object {
        /** Full cookie string saved after a successful WebView CF bypass */
        var cfCookies: String
            get() = getKey("MUSICBD_CF_COOKIES") ?: ""
            set(value) {
                setKey("MUSICBD_CF_COOKIES", value)
            }

        /** The exact User-Agent string used by the WebView to solve the challenge. */
        var cfUserAgent: String
            get() = getKey("MUSICBD_CF_USER_AGENT") ?: ""
            set(value) {
                setKey("MUSICBD_CF_USER_AGENT", value)
            }

        /** The host for which cfCookies were captured */
        var cfCookieHost: String
            get() = getKey("MUSICBD_CF_COOKIE_HOST") ?: ""
            set(value) {
                setKey("MUSICBD_CF_COOKIE_HOST", value)
            }
    }
}
