package com.sigmaflux.market

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sigmaflux.market.ui.BottomBarBehavior
import com.sigmaflux.market.ui.detail.AssetDetailScreen
import com.sigmaflux.market.ui.home.HomeScreen
import com.sigmaflux.market.ui.market.MarketScreen
import com.sigmaflux.market.ui.more.MoreScreen
import com.sigmaflux.market.ui.news.NewsDetailScreen
import com.sigmaflux.market.ui.news.NewsScreen
import com.sigmaflux.market.ui.portfolio.PortfolioScreen
import com.sigmaflux.market.ui.theme.Graphite
import com.sigmaflux.market.ui.theme.SigmaFluxTheme

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Главная", Icons.Filled.Home),
    Tab("news", "Новости", Icons.Filled.Article),
    Tab("market", "Рынок", Icons.Filled.Insights),
    Tab("portfolio", "Портфель", Icons.Filled.PieChart),
    Tab("more", "Ещё", Icons.Filled.MoreHoriz)
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SigmaFluxTheme {
                SigmaFluxApp()
            }
        }
    }
}

@Composable
private fun SigmaFluxApp() {
    val navController = rememberNavController()
    val behavior = remember { BottomBarBehavior() }
    var pendingSymbol by remember { mutableStateOf<String?>(null) }

    // deep link: sigmaflux://asset/{symbol}
    LaunchedEffect(Unit) {
        val intent = (LocalContext.current as? ComponentActivity)?.intent
        intent?.data?.let { uri ->
            if (uri.scheme == "sigmaflux" && uri.host == "asset") {
                val sym = uri.pathSegments.firstOrNull()?.uppercase()
                if (sym != null) pendingSymbol = sym
            }
        }
    }
    pendingSymbol?.let { sym ->
        LaunchedEffect(sym) {
            navController.navigate("detail/$sym") { launchSingleTop = true }
            pendingSymbol = null
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val isDetail = currentRoute?.startsWith("detail/") == true
    // на графике/detail — immersive: bottom bar скрыт
    LaunchedEffect(isDetail) {
        if (isDetail) behavior.forceHide() else behavior.forceShow()
    }

    Scaffold(
        containerColor = Graphite.Background,
        bottomBar = {
            AnimatedVisibility(
                visible = !behavior.hidden && !isDetail,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                NavigationBar(containerColor = Graphite.Surface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    behavior = behavior,
                    onOpenInstrument = { navController.navigate("detail/$it") },
                    onOpenNews = { newsId -> navController.navigate("news/$newsId") },
                    onShowAllNews = { navController.navigate("news") }
                )
            }
            composable("news") {
                NewsScreen(
                    behavior = behavior,
                    onOpenNews = { newsId -> navController.navigate("news/$newsId") }
                )
            }
            composable(
                route = "news/{newsId}",
                arguments = listOf(navArgument("newsId") { type = NavType.StringType })
            ) { entry ->
                val newsId = entry.arguments?.getString("newsId") ?: return@composable
                NewsDetailScreen(
                    newsId = newsId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("market") {
                MarketScreen(
                    behavior = behavior,
                    onOpenInstrument = { navController.navigate("detail/$it") }
                )
            }
            composable("portfolio") {
                PortfolioScreen(
                    behavior = behavior,
                    onOpenInstrument = { navController.navigate("detail/$it") }
                )
            }
            composable("more") {
                MoreScreen(
                    behavior = behavior,
                    onOpenInstrument = { navController.navigate("detail/$it") }
                )
            }
            composable(
                route = "detail/{symbol}",
                arguments = listOf(navArgument("symbol") { type = NavType.StringType })
            ) { entry ->
                val symbol = entry.arguments?.getString("symbol")?.uppercase() ?: return@composable
                AssetDetailScreen(
                    symbol = symbol,
                    onBack = { navController.popBackStack() },
                    onCreateAlert = { sym ->
                        navController.navigate("more")
                    }
                )
            }
        }
    }
}
