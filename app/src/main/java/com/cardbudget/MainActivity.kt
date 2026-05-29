package com.cardbudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.cardbudget.ui.card.CardScreen
import com.cardbudget.ui.common.PermissionSetupScreen
import com.cardbudget.ui.home.HomeScreen
import com.cardbudget.ui.notification.NotificationScreen
import com.cardbudget.ui.theme.CardBudgetTheme
import com.cardbudget.ui.transaction.TransactionScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CardBudgetTheme {
                CardBudgetApp()
            }
        }
    }
}

// ─── Navigation ────────────────────────────────────────────
sealed class Screen(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Home : Screen("home", "홈", Icons.Filled.Home, Icons.Outlined.Home)
    object Transactions : Screen("transactions", "내역", Icons.Filled.List, Icons.Outlined.List)
    object Cards : Screen("cards", "카드", Icons.Filled.CreditCard, Icons.Outlined.CreditCard)
    object Notifications : Screen("notifications", "알림", Icons.Filled.Notifications, Icons.Outlined.Notifications)
}

val bottomNavItems = listOf(Screen.Home, Screen.Transactions, Screen.Cards, Screen.Notifications)

@Composable
fun CardBudgetApp() {
    var permissionsGranted by remember { mutableStateOf(false) }

    if (!permissionsGranted) {
        PermissionSetupScreen(onComplete = { permissionsGranted = true })
        return
    }

    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = { Icon(if (selected) screen.selectedIcon else screen.unselectedIcon, screen.label) },
                        label = { Text(screen.label) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Home.route, Modifier.padding(innerPadding)) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                    onNavigateToCards = { navController.navigate(Screen.Cards.route) }
                )
            }
            composable(Screen.Transactions.route) { TransactionScreen() }
            composable(Screen.Cards.route) { CardScreen() }
            composable(Screen.Notifications.route) { NotificationScreen() }
        }
    }
}
