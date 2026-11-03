package com.waimai.compare

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.waimai.compare.ui.search.SearchScreen
import com.waimai.compare.ui.theme.WaiMaiTheme
import com.waimai.compare.ui.web.WebScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WaiMaiTheme {
                AppNavHost()
            }
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController = rememberNavController()) {
    NavHost(navController = nav, startDestination = "search") {
        composable("search") {
            SearchScreen(onSearch = { kw -> nav.navigate("web?keyword=" + Uri.encode(kw)) })
        }
        composable(
            "web?keyword={keyword}",
            arguments = listOf(navArgument("keyword") { type = NavType.StringType; defaultValue = "" }),
        ) { entry ->
            WebScreen(
                keyword = entry.arguments?.getString("keyword").orEmpty(),
                onBack = { nav.popBackStack() },
            )
        }
    }
}
