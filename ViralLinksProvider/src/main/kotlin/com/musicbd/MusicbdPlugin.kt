package com.musicbd

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class MusicbdPlugin : Plugin() {
    
    override val relevantStreams: List<com.lagradost.cloudstream3.plugins.Stream>
        get() = listOf(
            com.lagradost.cloudstream3.plugins.Stream(
                name = "Musicbd25",
                mainUrl = "https://musicbd25.site"
            )
        )
    
    override fun load() {
        // Initialize settings using CloudStream's built-in settings DSL
        initPlugin {
            // Switch setting for Auto WebView Bypass
            switch {
                key = "auto_webview_bypass"
                title = "Auto WebView Bypass"
                default = true
                description = "Automatically solve challenges via WebView"
            }
            
            // Button to clear cookies
            button {
                key = "clear_cookies"
                title = "Clear Cookies"
                description = "Remove saved cookies and force re-authentication"
            }
            
            // Button to trigger manual bypass
            button {
                key = "bypass_now"
                title = "Bypass Now"
                description = "Open WebView and solve challenge manually"
            }
        }
        
        registerMainAPI(MusicbdProvider())
    }
}
