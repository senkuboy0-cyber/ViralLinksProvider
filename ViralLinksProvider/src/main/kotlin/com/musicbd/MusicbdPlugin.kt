package com.musicbd

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class MusicbdPlugin : BasePlugin() {
    
    override fun load(context: Context) {
        registerMainAPI(MusicbdProvider())
    }

    init {
        this.openSettings = { activity ->
            try {
                val act = activity as? AppCompatActivity
                if (act != null) {
                    val frag = MusicbdSettingsFragment(this)
                    frag.show(act.supportFragmentManager, "Musicbd25")
                }
            } catch (e: Exception) {
            }
        }
    }
}
