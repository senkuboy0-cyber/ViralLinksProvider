package com.musicbd

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class MusicbdPlugin : com.lagradost.cloudstream3.plugins.BasePlugin() {
    
    override fun load() {
        registerMainAPI(MusicbdProvider())
    }

    init {
        // Enable settings button in CloudStream app
        this.openSettings = { activity ->
            try {
                val act = activity as? AppCompatActivity
                if (act != null) {
                    val mainUrl = "https://musicbd25.site"
                    val settingsFragment = MusicbdSettingsFragment(this, mainUrl)
                    settingsFragment.show(act.supportFragmentManager, "Musicbd25Settings")
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
}
