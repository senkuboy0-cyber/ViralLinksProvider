plugins {
    id("com.lagradost.cloudstream3.gradle")
}

version = 1

android {
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("com.google.android.material:material:1.14.0")
}

cloudstream {
    language = "en"
    authors = listOf("Cloudstream Extension")
    description = "ViralLinksProvider"
    status = 1
    tvTypes = listOf(
        "TvSeries",
        "Movie"
    )
    isCrossPlatform = true
    requiresResources = true
    iconUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEhQvNXfZt7ctszD6Fy_FwU7NfcyxIEZ6uW6asTw_5cMPS38hkm65bQdzb2bCD-86XfOUVmp5xjOANaefT4ZdWSCf_picqYtsAN5McX_3gVEfdVa5EA4h9e2noiaNLwUhMK8VaGx1mQGI_7TCnpmEI3LxtgNPeVpKsojjSbqSZh50VbyrTiP7_2KOIusBBsC/s1024/1000073990.png"
}
