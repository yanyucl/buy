@file:OptIn(ExperimentalMaterial3Api::class)

package com.waimai.compare.ui.web

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.waimai.compare.data.Platform
import com.waimai.compare.data.PlatformLinks
import com.waimai.compare.data.openPlatformSearch

/**
 * 内嵌网页版比价：三个平台各一个标签页，WebView 加载 H5 搜索页（关键词自动带入）。
 * 各平台在 WebView 里登录一次后 Cookie 持久化（CookieManager 全局保存），下次无需再登。
 */
@Composable
fun WebScreen(keyword: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loadedUrl by remember { mutableStateOf<String?>(null) }
    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { locationGranted = it }

    // 美团 H5 依赖定位（门店定价），切到该标签时请求定位权限
    LaunchedEffect(tab) {
        if (Platform.entries[tab] == Platform.MEITUAN && !locationGranted) {
            locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // 网页内有上级页面时先回退网页，否则退出本页
    BackHandler {
        val wv = webView
        if (wv != null && wv.canGoBack()) wv.goBack() else onBack()
    }

    val url = PlatformLinks.h5SearchUrl(Platform.entries[tab], keyword)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("网页版：$keyword", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 用当前标签平台的唤端 scheme 拉起本机 App（未安装则回退浏览器 H5）
                    IconButton(
                        onClick = {
                            context.openPlatformSearch(Platform.entries[tab], keyword)
                        }
                    ) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = "唤起本机 App")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = tab) {
                Platform.entries.forEachIndexed { i, p ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(p.label) })
                }
            }
            if (progress in 0.01f..0.99f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            // 去掉 WebView 标记，减少部分 H5 的拦截/降级
                            userAgentString = userAgentString.replace("; wv", "")
                        }
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean {
                                val u = request.url
                                // http(s) 留在 WebView；其他 scheme（唤端链接）交给系统
                                if (u.scheme == "http" || u.scheme == "https") return false
                                return try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, u)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                    true
                                } catch (_: Exception) {
                                    true
                                }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress / 100f
                            }

                            override fun onGeolocationPermissionsShowPrompt(
                                origin: String?,
                                callback: GeolocationPermissions.Callback?,
                            ) {
                                callback?.invoke(origin, locationGranted, false)
                            }
                        }
                        webView = this
                    }
                },
                update = { wv ->
                    // 只在标签/关键词变化时加载，避免重组导致反复刷新
                    if (loadedUrl != url) {
                        loadedUrl = url
                        wv.loadUrl(url)
                    }
                }
            )
        }
    }
}
