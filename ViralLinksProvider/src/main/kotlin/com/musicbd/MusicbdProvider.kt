package com.musicbd

import android.webkit.WebSettings
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.Response
import kotlin.coroutines.resume

object MusicbdCFBypassInterceptor : Interceptor {
    var systemUserAgent: String = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
            .removeHeader("X-Requested-With")
            .header("sec-ch-ua-mobile", "?1")
            .header("sec-ch-ua-platform", "\"Android\"")
            .header("Cache-Control", "no-cache") 

        val savedUa = MusicbdPlugin.cfUserAgent
        if (savedUa.isNotEmpty()) {
            builder.header("User-Agent", savedUa)
        } else {
            builder.header("User-Agent", systemUserAgent)
        }

        val savedCookies = MusicbdPlugin.cfCookies
        if (savedCookies.isNotEmpty()) {
            val existingCookie = original.header("Cookie") ?: ""
            val cookieMap = mutableMapOf<String, String>()
            
            if (existingCookie.isNotEmpty()) {
                existingCookie.split(";").forEach {
                    val parts = it.trim().split("=", limit = 2)
                    if (parts.isNotEmpty() && parts[0].isNotEmpty()) {
                        cookieMap[parts[0]] = if (parts.size > 1) parts[1] else ""
                    }
                }
            }
            
            savedCookies.split(";").forEach {
                val parts = it.trim().split("=", limit = 2)
                if (parts.isNotEmpty() && parts[0].isNotEmpty()) {
                    cookieMap[parts[0]] = if (parts.size > 1) parts[1] else ""
                }
            }
            
            val finalCookie = cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
            builder.header("Cookie", finalCookie)
        }

        return chain.proceed(builder.build())
    }
}

suspend fun showMusicbdCFBypassDialogAndWait(url: String): Boolean =
    withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val activity = CommonActivity.activity as? AppCompatActivity
            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                MusicbdLogger.log("Error: Activity is null, cannot open WebView.")
                cont.resume(false)
                return@suspendCancellableCoroutine
            }
            var resumed = false
            fun safeResume(success: Boolean) {
                if (!resumed) { 
                    resumed = true
                    cont.resume(success) 
                }
            }
            val dialog = CloudflareWebViewDialog(
                targetUrl = url,
                onFinished = { success -> safeResume(success) }
            )
            cont.invokeOnCancellation {
                activity.runOnUiThread { runCatching { dialog.dismissAllowingStateLoss() } }
            }
            dialog.show(activity.supportFragmentManager, "musicbd_cf_bypass_auto")
        }
    }

class MusicbdProvider : MainAPI() {
    override var mainUrl = "https://musicbd25.site"
    override var name = "Musicbd25"
    override var lang = "en"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie)

    private val defaultPosterUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEhQvNXfZt7ctszD6Fy_FwU7NfcyxIEZ6uW6asTw_5cMPS38hkm65bQdzb2bCD-86XfOUVmp5xjOANaefT4ZdWSCf_picqYtsAN5McX_3gVEfdVa5EA4h9e2noiaNLwUhMK8VaGx1mQGI_7TCnpmEI3LxtgNPeVpKsojjSbqSZh50VbyrTiP7_2KOIusBBsC/s1024/1000073990.png"

    private val excludedSrcs = listOf(
        "1000016877",
        "wp-1674077227462",
        "r4.png",
        "rssn.png",
        "new-news",
        "1000073990",
        "1000025339",
        ".gif"
    )

    private val ua = mapOf(
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "Accept-Language" to "en-US,en;q=0.5"
    )

    companion object {
        private val cfMutex = Mutex()
        
        private val CF_BLOCKER_PHRASES = listOf(
            "just a moment",
            "checking your browser",
            "ddos-guard",
            "attention required",
            "verify you are human",
            "cloudflare"
        )

        fun isCloudflareBlocked(response: com.lagradost.nicehttp.NiceResponse): Boolean {
            if (response.code == 403 || response.code == 503 || response.code == 202) return true
            return CF_BLOCKER_PHRASES.any { response.text.lowercase().contains(it) }
        }
        
        fun isAutoWebviewEnabled(): Boolean {
            return getKey<Boolean>("musicbd_auto_bypass_v2") ?: true
        }

        suspend fun appGet(
            url: String,
            headers: Map<String, String> = emptyMap()
        ): com.lagradost.nicehttp.NiceResponse {
            
            try {
                val sysUa = withContext(Dispatchers.Main) {
                    CommonActivity.activity?.let { WebSettings.getDefaultUserAgent(it) }
                }
                if (!sysUa.isNullOrEmpty()) {
                    MusicbdCFBypassInterceptor.systemUserAgent = sysUa
                }
            } catch (e: Exception) {
                MusicbdLogger.log("Failed to fetch system User-Agent")
            }

            val noCacheHeaders = headers.toMutableMap()
            noCacheHeaders["Cache-Control"] = "no-cache"
            
            MusicbdLogger.log("Requesting URL: $url")
            var rawResponse = app.get(url, headers = noCacheHeaders, interceptor = MusicbdCFBypassInterceptor)
            
            if (isCloudflareBlocked(rawResponse)) {
                MusicbdLogger.log("Blocked by Protection (Code: ${rawResponse.code})")
                
                if (isAutoWebviewEnabled()) {
                    cfMutex.withLock {
                        var checkResp = app.get(url, headers = noCacheHeaders, interceptor = MusicbdCFBypassInterceptor)
                        
                        if (isCloudflareBlocked(checkResp)) {
                            MusicbdLogger.log("Opening WebView to bypass protection...")
                            
                            val success = showMusicbdCFBypassDialogAndWait("https://musicbd25.site")
                            if (success) {
                                val cookieManager = android.webkit.CookieManager.getInstance()
                                cookieManager.flush() 
                                val newCookies = cookieManager.getCookie("https://musicbd25.site") ?: ""
                                if (newCookies.isNotEmpty()) {
                                    MusicbdPlugin.cfCookies = newCookies
                                    MusicbdLogger.log("Cookies fetched successfully: ${newCookies.take(20)}...")
                                } else {
                                    MusicbdLogger.log("Warning: WebView closed but cookies are empty.")
                                }
                                
                                MusicbdLogger.log("Retrying request after bypass...")
                                
                                val bypassCacheHeaders = headers.toMutableMap()
                                bypassCacheHeaders["Cache-Control"] = "no-store, no-cache, must-revalidate, max-age=0"
                                bypassCacheHeaders["Pragma"] = "no-cache"
                                
                                checkResp = app.get(url, headers = bypassCacheHeaders, interceptor = MusicbdCFBypassInterceptor)
                                
                                if (isCloudflareBlocked(checkResp)) {
                                    MusicbdLogger.log("Retry failed! Still blocked.")
                                } else {
                                    MusicbdLogger.log("Bypass successful! Page loaded.")
                                }
                            } else {
                                MusicbdLogger.log("WebView dialog was closed or failed.")
                            }
                        }
                        rawResponse = checkResp
                    }
                } else {
                    MusicbdLogger.log("Auto WebView is disabled in settings.")
                }
            }
            return rawResponse
        }
    }

    private fun upgradeBloggerImageSize(url: String): String {
        return url.replace(Regex("/s\\d+/"), "/s1600/")
    }

    private fun isValidPoster(src: String): Boolean {
        if (src.isBlank()) return false
        for (excluded in excludedSrcs) {
            if (src.contains(excluded)) return false
        }
        return true
    }

    override val mainPage = mainPageOf(
        "$mainUrl/" to "Latest",
        "$mainUrl/site-0.html" to "All Categories"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        var listUrl = request.data
        if (page > 1) {
            listUrl = "${request.data}?to-page=$page"
        }
        
        val listDoc = appGet(listUrl, headers = ua).document

        var linkElements = listDoc.select("div.catlistblock a[href*=/page-download/]")
        if (linkElements.isEmpty()) {
            linkElements = listDoc.select("div.post a[href*=/page-download/]")
        }
        
        if (linkElements.isEmpty()) {
            MusicbdLogger.log("MainPage: No items found for ${request.name}")
            return newHomePageResponse(request.name, emptyList(), false)
        }

        val items = coroutineScope {
            linkElements.map { el ->
                async {
                    var href = el.attr("href").trim()
                    if (href.isBlank()) return@async null
                    
                    if (href.startsWith("/")) {
                        href = "$mainUrl$href"
                    }

                    var title = el.text().trim()
                    var poster = defaultPosterUrl
                    
                    val parent = el.closest("div.catlistblock") ?: el.closest("div.post") ?: el.parent()
                    val imgEl = parent?.selectFirst("img")
                    
                    if (imgEl != null) {
                        val src = imgEl.attr("src").trim()
                        if (title.isBlank()) {
                            title = imgEl.attr("alt").trim()
                        }
                        if (isValidPoster(src)) {
                            poster = upgradeBloggerImageSize(src)
                        }
                    }
                    
                    if (title.isBlank()) {
                        val parts = href.split("/")
                        title = parts.last().replace(".html", "").replace("-", " ")
                    }

                    newMovieSearchResponse(title, href, TvType.Movie) {
                        this.posterUrl = poster
                    }
                }
            }.awaitAll().filterNotNull()
        }

        return newHomePageResponse(request.name, items, items.isNotEmpty())
    }

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        var url = "$mainUrl/site-1.html?to-search=$encoded"
        if (page > 1) {
            url = "$mainUrl/site-1.html?to-search=$encoded&to-page=$page"
        }
        
        val doc = appGet(url, headers = ua).document

        var linkElements = doc.select("div.catlistblock a[href*=/page-download/]")
        if (linkElements.isEmpty()) {
            linkElements = doc.select("div.post a[href*=/page-download/]")
        }
        
        if (linkElements.isEmpty()) {
            return newSearchResponseList(emptyList(), false)
        }

        val items = coroutineScope {
            linkElements.map { el ->
                async {
                    var href = el.attr("href").trim()
                    if (href.isBlank()) return@async null
                    
                    if (href.startsWith("/")) {
                        href = "$mainUrl$href"
                    }

                    var title = el.text().trim()
                    var poster = defaultPosterUrl
                    
                    val parent = el.closest("div.catlistblock") ?: el.closest("div.post") ?: el.parent()
                    val imgEl = parent?.selectFirst("img")
                    
                    if (imgEl != null) {
                        val src = imgEl.attr("src").trim()
                        if (title.isBlank()) {
                            title = imgEl.attr("alt").trim()
                        }
                        if (isValidPoster(src)) {
                            poster = upgradeBloggerImageSize(src)
                        }
                    }
                    
                    if (title.isBlank()) {
                        val parts = href.split("/")
                        title = parts.last().replace(".html", "").replace("-", " ")
                    }

                    newMovieSearchResponse(title, href, TvType.Movie) {
                        this.posterUrl = poster
                    }
                }
            }.awaitAll().filterNotNull()
        }

        return newSearchResponseList(items, items.isNotEmpty())
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = appGet(url, headers = ua).document
        doc.select("div.updates").remove()

        var title = ""
        val h2Element = doc.selectFirst("div.hh h2")
        if (h2Element != null) {
            title = h2Element.text().trim()
        } else {
            title = doc.title().trim()
        }

        var poster = defaultPosterUrl
        
        val elements = ArrayList<org.jsoup.nodes.Element>()
        elements.addAll(doc.select("div.thumb img"))
        elements.addAll(doc.select("div.finfo img"))
        elements.addAll(doc.select("img[alt][title][src*=blogger.googleusercontent.com]"))
        elements.addAll(doc.select("img[src*=blogger.googleusercontent.com]"))
        
        for (element in elements) {
            val src = element.attr("src").trim()
            if (isValidPoster(src)) {
                poster = upgradeBloggerImageSize(src)
                break
            }
        }

        val downloadA = doc.selectFirst("a[href*=filedownload]")
        if (downloadA != null) {
            var downloadUrl = downloadA.attr("href").trim()
            if (downloadUrl.startsWith("//")) {
                downloadUrl = "https:$downloadUrl"
            } else if (downloadUrl.startsWith("/")) {
                downloadUrl = "$mainUrl$downloadUrl"
            }

            return newMovieLoadResponse(title, url, TvType.Movie, downloadUrl) {
                this.posterUrl = poster
            }
        }

        return newMovieLoadResponse(title, url, TvType.Movie, "") {
            this.posterUrl = poster
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if (data.isBlank() || !data.contains("filedownload")) return false
        try {
            val requestHeaders = ua + mapOf("Referer" to "$mainUrl/")
            val doc = appGet(data, headers = requestHeaders).document
            var finalUrl = ""

            val link1 = doc.selectFirst("a[href$=.mp4]")
            val link2 = doc.selectFirst("a[href*=.mp4]")
            val link3 = doc.selectFirst("a:contains(Start Download)")
            val link4 = doc.selectFirst("a:contains(Download Now)")

            if (link1 != null) {
                finalUrl = link1.attr("href")
            } else if (link2 != null) {
                finalUrl = link2.attr("href")
            } else if (link3 != null) {
                finalUrl = link3.attr("href")
            } else if (link4 != null) {
                finalUrl = link4.attr("href")
            }

            finalUrl = finalUrl.trim()
            if (finalUrl.isNotEmpty()) {
                var normalized = finalUrl
                if (normalized.startsWith("//")) {
                    normalized = "https:$normalized"
                } else if (normalized.startsWith("/")) {
                    normalized = "$mainUrl$normalized"
                }

                callback.invoke(
                    newExtractorLink(
                        this.name,
                        "Direct Stream",
                        normalized,
                        ExtractorLinkType.VIDEO
                    ) {
                        quality = Qualities.Unknown.value
                        referer = "$mainUrl/"
                    }
                )
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}
