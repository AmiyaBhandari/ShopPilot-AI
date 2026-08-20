package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.ActionConfirmDialog
import com.example.ui.dialogs.QuickSaleDialog
import com.example.ui.dialogs.RecordPurchaseDialog
import com.example.ui.dialogs.VoiceInputDialog
import com.example.ui.screens.activity.ActivityScreen
import com.example.ui.screens.ai.AiInvoiceScannerSheet
import com.example.ui.screens.ai.AiShopAssistantScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.more.MoreScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.products.AddEditProductDialog
import com.example.ui.screens.products.ProductsScreen
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class AppDestination(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    DASHBOARD("Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    PRODUCTS("Products", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    ACTIVITY("Activity", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    AI_ASSISTANT("AI Advisor", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    MORE("More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ShopPilotApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ShopPilotApp(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val activeInsights by viewModel.activeInsights.collectAsState()
    val proposedAction by viewModel.proposedAction.collectAsState()
    val notification by viewModel.notification.collectAsState()

    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }

    // Dialog & Sheet states
    var showQuickSale by remember { mutableStateOf(false) }
    var showAddProduct by remember { mutableStateOf(false) }
    var showRecordPurchase by remember { mutableStateOf(false) }
    var showVoiceInput by remember { mutableStateOf(false) }
    var showScanInvoice by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notification) {
        notification?.let {
            snackbarHostState.showSnackbar(
                message = it.message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearNotification()
        }
    }

    // Check if onboarding is needed
    if (settings != null && !settings!!.setupCompleted) {
        OnboardingScreen(
            viewModel = viewModel,
            onFinish = {
                currentDestination = AppDestination.DASHBOARD
            }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, AppBorderLight),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("main_bottom_nav"),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Home
                        val isHome = currentDestination == AppDestination.DASHBOARD
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .clickable { currentDestination = AppDestination.DASHBOARD }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isHome) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "Home",
                                tint = if (isHome) BrandPrimary else TextSubtleLight,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Home",
                                fontSize = 10.sp,
                                fontWeight = if (isHome) FontWeight.Bold else FontWeight.Medium,
                                color = if (isHome) BrandPrimary else TextSubtleLight
                            )
                        }

                        // Products
                        val isProducts = currentDestination == AppDestination.PRODUCTS
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .clickable { currentDestination = AppDestination.PRODUCTS }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (metrics.lowStockCount > 0) {
                                        Badge(containerColor = StatusWarning) {
                                            Text("${metrics.lowStockCount}", fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isProducts) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                                    contentDescription = "Products",
                                    tint = if (isProducts) BrandPrimary else TextSubtleLight,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "Products",
                                fontSize = 10.sp,
                                fontWeight = if (isProducts) FontWeight.Bold else FontWeight.Medium,
                                color = if (isProducts) BrandPrimary else TextSubtleLight
                            )
                        }

                        // Central Elevated Floating Mic Button (Clean Utility signature style)
                        Box(
                            modifier = Modifier
                                .offset(y = (-8).dp)
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(BrandPrimary)
                                .border(3.dp, AppBackgroundLight, CircleShape)
                                .clickable { showVoiceInput = true }
                                .testTag("floating_mic_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // AI Assistant / Activity
                        val isAi = currentDestination == AppDestination.AI_ASSISTANT
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .clickable { currentDestination = AppDestination.AI_ASSISTANT }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (activeInsights.isNotEmpty()) {
                                        Badge(containerColor = BrandSecondary) {
                                            Text("${activeInsights.size}", fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isAi) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                                    contentDescription = "AI Assistant",
                                    tint = if (isAi) BrandPrimary else TextSubtleLight,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "AI Advisor",
                                fontSize = 10.sp,
                                fontWeight = if (isAi) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAi) BrandPrimary else TextSubtleLight
                            )
                        }

                        // More
                        val isMore = currentDestination == AppDestination.MORE
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .clickable { currentDestination = AppDestination.MORE }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isMore) Icons.Filled.Menu else Icons.Outlined.Menu,
                                contentDescription = "More",
                                tint = if (isMore) BrandPrimary else TextSubtleLight,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "More",
                                fontSize = 10.sp,
                                fontWeight = if (isMore) FontWeight.Bold else FontWeight.Medium,
                                color = if (isMore) BrandPrimary else TextSubtleLight
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                when (currentDestination) {
                    AppDestination.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToProducts = { currentDestination = AppDestination.PRODUCTS },
                        onNavigateToAiAssistant = { currentDestination = AppDestination.AI_ASSISTANT },
                        onNavigateToActivity = { currentDestination = AppDestination.ACTIVITY },
                        onOpenQuickSale = { showQuickSale = true },
                        onOpenAddProduct = { showAddProduct = true },
                        onOpenRecordPurchase = { showRecordPurchase = true },
                        onOpenVoiceInput = { showVoiceInput = true },
                        onOpenScanInvoice = { showScanInvoice = true }
                    )
                    AppDestination.PRODUCTS -> ProductsScreen(
                        viewModel = viewModel,
                        onOpenVoiceInput = { showVoiceInput = true }
                    )
                    AppDestination.ACTIVITY -> ActivityScreen(
                        viewModel = viewModel,
                        onOpenQuickSale = { showQuickSale = true },
                        onOpenRecordPurchase = { showRecordPurchase = true },
                        onOpenVoiceInput = { showVoiceInput = true }
                    )
                    AppDestination.AI_ASSISTANT -> AiShopAssistantScreen(
                        viewModel = viewModel,
                        onOpenVoiceInput = { showVoiceInput = true },
                        onOpenScanInvoice = { showScanInvoice = true },
                        onOpenRecordPurchase = { showRecordPurchase = true }
                    )
                    AppDestination.MORE -> MoreScreen(
                        viewModel = viewModel,
                        onNavigateToAssistant = { currentDestination = AppDestination.AI_ASSISTANT }
                    )
                }
            }
        }
    }

    // Modal Overlays
    if (showQuickSale) {
        QuickSaleDialog(
            viewModel = viewModel,
            onDismiss = { showQuickSale = false }
        )
    }

    if (showAddProduct) {
        AddEditProductDialog(
            onDismiss = { showAddProduct = false },
            onSave = { newProd ->
                viewModel.saveProduct(newProd)
                showAddProduct = false
            }
        )
    }

    if (showRecordPurchase) {
        RecordPurchaseDialog(
            viewModel = viewModel,
            onDismiss = { showRecordPurchase = false }
        )
    }

    if (showVoiceInput) {
        VoiceInputDialog(
            viewModel = viewModel,
            onDismiss = { showVoiceInput = false }
        )
    }

    if (showScanInvoice) {
        AiInvoiceScannerSheet(
            viewModel = viewModel,
            onDismiss = { showScanInvoice = false }
        )
    }

    // AI Safety Action Confirmation Dialog
    proposedAction?.let { action ->
        ActionConfirmDialog(
            action = action,
            onConfirm = { viewModel.executeProposedAction(action) },
            onDismiss = { viewModel.dismissProposedAction() }
        )
    }
}
