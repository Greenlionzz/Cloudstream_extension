// 345MovieExtension.kt package com.eww345.movie

import com.lagradost.cloudstream3.* import com.lagradost.cloudstream3.utils.*

class Movie345 : MainAPI() { override var name = "345Movie" override var mainUrl = "https://345movie.net" override var lang = "en" override val hasMainPage = true override val hasSearch = true

override val supportedTypes = setOf(
    TvType.Movie,
    TvType.TvSeries,
    TvType.Anime
)

private fun getTypeFromUrl(url: String): TvType {
    return when {
        "/series/" in url -> TvType.TvSeries
        "/anime/" in url -> TvType.Anime
        else -> TvType.Movie
    }
}

override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    val categories = listOf(
        Pair("Trending", "/home"),
        Pair("Movies", "/movies"),
        Pair("Series", "/series"),
        Pair("Anime", "/anime")
    )

    val home = categories.map { (title, path) ->
        val doc = app.get(mainUrl + path).document
        val items = doc.select(".block a:has(img)").mapNotNull {
            val href = it.attr("href")
            val img = it.selectFirst("img")?.attr("src") ?: return@mapNotNull null
            val name = img.substringAfterLast("/").substringBeforeLast(".").replace("-", " ")
            MovieSearchResponse(
                name = name,
                url = fixUrl(href),
                apiName = this.name,
                type = getTypeFromUrl(href),
                posterUrl = fixUrl(img),
            )
        }
        HomePageList(title, items)
    }

    return HomePageResponse(home)
}

override suspend fun search(query: String): List<SearchResponse> {
    val url = "$mainUrl/search?keyword=$query"
    val doc = app.get(url).document
    return doc.select(".block a:has(img)").mapNotNull {
        val href = it.attr("href")
        val img = it.selectFirst("img")?.attr("src") ?: return@mapNotNull null
        val name = img.substringAfterLast("/").substringBeforeLast(".").replace("-", " ")
        MovieSearchResponse(
            name = name,
            url = fixUrl(href),
            apiName = this.name,
            type = getTypeFromUrl(href),
            posterUrl = fixUrl(img),
        )
    }
}

override suspend fun load(url: String): LoadResponse {
    val doc = app.get(url).document
    val title = doc.selectFirst("h1")?.text()?.trim() ?: "Unknown"
    val poster = doc.select(".w-full.rounded").attr("src")
    val description = doc.selectFirst(".py-2.text-sm")?.text()?.trim()
    val tags = doc.select(".py-2 a.text-xs").map { it.text() }

    val episodes = listOf(
        Episode(fixUrl(url), title)
    )

    return newTvSeriesLoadResponse(title, url, getTypeFromUrl(url)) {
        this.posterUrl = fixUrl(poster)
        this.plot = description
        this.tags = tags
        this.episodes = episodes
    }
}

override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
    val doc = app.get(data).document
    val iframe = doc.selectFirst("iframe")?.attr("src") ?: return false

    callback(
        ExtractorLink(
            source = "345Movie",
            name = "Embedded",
            url = fixUrl(iframe),
            referer = mainUrl,
            quality = Qualities.Unknown.value,
            isM3u8 = false
        )
    )
    return true
}

}


