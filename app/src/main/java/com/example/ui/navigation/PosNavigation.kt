package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.ui.MainViewModel
import com.example.ui.components.RoleBadge
import com.example.ui.screens.categories.CategoriesScreen
import com.example.ui.screens.customers.CustomersScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.expenses.ExpensesScreen
import com.example.ui.screens.invoice.InvoiceScreen
import com.example.ui.screens.login.LoginScreen
import com.example.ui.screens.pos.PosScreen
import com.example.ui.screens.products.ProductsScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.stock.StockScreen
import com.example.ui.screens.users.UsersScreen
import com.example.ui.screens.sales.SalesScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Login : Screen("login", "Login", Icons.Default.Lock)
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Pos : Screen("pos", "POS Billing", Icons.Default.PointOfSale)
    object Products : Screen("products", "Products", Icons.Default.Inventory2)
    object Categories : Screen("categories", "Categories", Icons.Default.Category)
    object Stock : Screen("stock", "Stock Management", Icons.Default.Warehouse)
    object Customers : Screen("customers", "Customers", Icons.Default.People)
    object Sales : Screen("sales", "Sales History", Icons.Default.ReceiptLong)
    object Expenses : Screen("expenses", "Expenses", Icons.Default.MoneyOff)
    object Reports : Screen("reports", "Reports & Analytics", Icons.Default.BarChart)
    object Users : Screen("users", "Staff & Users", Icons.Default.ManageAccounts)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Invoice : Screen("invoice", "Invoice / Receipt", Icons.Default.Receipt)
}

val drawerScreens = listOf(
    Screen.Dashboard,
    Screen.Pos,
    Screen.Products,
    Screen.Categories,
    Screen.Stock,
    Screen.Customers,
    Screen.Sales,
    Screen.Expenses,
    Screen.Reports,
    Screen.Users,
    Screen.Settings
)

val bottomBarScreens = listOf(
    Screen.Dashboard,
    Screen.Pos,
    Screen.Products,
    Screen.Sales,
    Screen.Reports
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosAppRoot(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Listen for snackbar messages
    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (currentUser == null) {
        // Fullscreen Login
        LoginScreen(
            viewModel = viewModel,
            onLoginSuccess = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            },
            modifier = modifier
        )
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 840.dp

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !isWideScreen && currentRoute != Screen.Login.route,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.widthIn(max = 300.dp)) {
                    DrawerHeader(
                        shopName = settings.shopName.ifBlank { "GK POS" },
                        userName = currentUser?.fullName ?: "User",
                        userRole = currentUser?.role ?: "CASHIER"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    drawerScreens.forEach { screen ->
                        NavigationDrawerItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, fontWeight = FontWeight.SemiBold) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                scope.launch { drawerState.close() }
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .testTag("drawer_item_${screen.route}")
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error) },
                        label = { Text("Logout", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.logout()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Wide Screen Navigation Rail (Tablet / Desktop)
                if (isWideScreen) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PointOfSale, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        drawerScreens.forEach { screen ->
                            NavigationRailItem(
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("nav_rail_${screen.route}")
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        NavigationRailItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error) },
                            label = { Text("Logout", color = MaterialTheme.colorScheme.error) },
                            selected = false,
                            onClick = { viewModel.logout() }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Main Content Screen Area
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                val currentTitle = drawerScreens.find { it.route == currentRoute }?.title
                                    ?: if (currentRoute == Screen.Invoice.route) "Invoice / Receipt" else "GK POS"
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentTitle,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    currentUser?.let { RoleBadge(role = it.role) }
                                }
                            },
                            navigationIcon = {
                                if (!isWideScreen) {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("app_bar_menu_button")
                                    ) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                }
                            },
                            actions = {
                                if (currentRoute != Screen.Pos.route) {
                                    IconButton(onClick = { navController.navigate(Screen.Pos.route) }) {
                                        Icon(Icons.Default.PointOfSale, contentDescription = "POS Terminal", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                IconButton(onClick = { viewModel.logout() }) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Switch Account / Logout")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        if (!isWideScreen && currentRoute != Screen.Login.route) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                bottomBarScreens.forEach { screen ->
                                    NavigationBarItem(
                                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                                        label = { Text(screen.title) },
                                        selected = currentRoute == screen.route,
                                        onClick = {
                                            if (currentRoute != screen.route) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("bottom_nav_${screen.route}")
                                    )
                                }
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToPos = { navController.navigate(Screen.Pos.route) },
                                onNavigateToProducts = { navController.navigate(Screen.Products.route) },
                                onNavigateToStock = { navController.navigate(Screen.Stock.route) },
                                onNavigateToCustomers = { navController.navigate(Screen.Customers.route) },
                                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                                onNavigateToExpenses = { navController.navigate(Screen.Expenses.route) },
                                onViewSale = { sale ->
                                    viewModel.setViewingInvoice(sale)
                                    navController.navigate(Screen.Invoice.route)
                                }
                            )
                        }

                        composable(Screen.Pos.route) {
                            PosScreen(
                                viewModel = viewModel,
                                onSaleCompleted = {
                                    navController.navigate(Screen.Invoice.route)
                                }
                            )
                        }

                        composable(Screen.Products.route) {
                            ProductsScreen(viewModel = viewModel)
                        }

                        composable(Screen.Categories.route) {
                            CategoriesScreen(viewModel = viewModel)
                        }

                        composable(Screen.Stock.route) {
                            StockScreen(viewModel = viewModel)
                        }

                        composable(Screen.Customers.route) {
                            CustomersScreen(
                                viewModel = viewModel,
                                onViewSale = { sale ->
                                    viewModel.setViewingInvoice(sale)
                                    navController.navigate(Screen.Invoice.route)
                                }
                            )
                        }

                        composable(Screen.Sales.route) {
                            SalesScreen(
                                viewModel = viewModel,
                                onViewSale = { sale ->
                                    viewModel.setViewingInvoice(sale)
                                    navController.navigate(Screen.Invoice.route)
                                }
                            )
                        }

                        composable(Screen.Expenses.route) {
                            ExpensesScreen(viewModel = viewModel)
                        }

                        composable(Screen.Reports.route) {
                            ReportsScreen(viewModel = viewModel)
                        }

                        composable(Screen.Users.route) {
                            UsersScreen(viewModel = viewModel)
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(viewModel = viewModel)
                        }

                        composable(Screen.Invoice.route) {
                            InvoiceScreen(
                                viewModel = viewModel,
                                onBackToPos = { navController.navigate(Screen.Pos.route) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerHeader(
    shopName: String,
    userName: String,
    userRole: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PointOfSale,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = shopName,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(
                text = userName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            RoleBadge(role = userRole)
        }
    }
}
