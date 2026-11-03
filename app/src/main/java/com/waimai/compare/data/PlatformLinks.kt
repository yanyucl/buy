package com.waimai.compare.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * 跳转式比价 + 内嵌网页版的链接配置。
 * 唤端 scheme 失效或美团搜索路径变化时，只需改这里。
 */
object PlatformLinks {

    /** H5 搜索页（内嵌 WebView 用，也是唤端失败时的回退） */
    fun h5SearchUrl(platform: Platform, keyword: String): String {
        val q = Uri.encode(keyword)
        return when (platform) {
            Platform.JD -> "https://so.m.jd.com/ware/search.action?keyword=$q"
            Platform.TAOBAO -> "https://s.taobao.com/search?q=$q"
            Platform.MEITUAN -> "https://h5.waimai.meituan.com/waimai/mindex/search?query=$q"
        }
    }

    /** 唤端链接列表：App scheme 优先，H5 兜底 */
    fun searchUrls(platform: Platform, keyword: String): List<String> {
        return when (platform) {
            Platform.JD -> listOf(
                "openapp.jdmobile://virtual?params=" + Uri.encode(
                    """{"category":"jump","des":"search","keyword":"$keyword"}"""
                ),
                h5SearchUrl(platform, keyword),
            )
            Platform.TAOBAO -> listOf(
                "taobao://s.taobao.com/search?q=${Uri.encode(keyword)}",
                h5SearchUrl(platform, keyword),
            )
            Platform.MEITUAN -> listOf(
                "imeituan://www.meituan.com/search?query=${Uri.encode(keyword)}",
                h5SearchUrl(platform, keyword),
            )
        }
    }
}

/** 依次尝试唤端（App scheme → H5），有一个成功即返回 true */
fun Context.openPlatformSearch(platform: Platform, keyword: String): Boolean =
    PlatformLinks.searchUrls(platform, keyword).any { url ->
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
