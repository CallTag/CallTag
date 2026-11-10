package id.calltag.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.calltag.app.ui.screens.EmergencyScreen
import id.calltag.app.ui.screens.HomeScreen
import id.calltag.app.ui.screens.ReportAndHubScreen
import id.calltag.app.ui.screens.SpamGuardScreen
import id.calltag.app.ui.theme.CallTagDesign
import id.calltag.app.ui.theme.CallTagRadius
import id.calltag.app.ui.theme.CallTagSpacing
import id.calltag.app.ui.theme.CallTagTouch
import id.calltag.app.ui.theme.CallTagType
import id.calltag.app.ui.viewmodel.CallTagMainViewModel

private data class MainNavTab(
    val index: Int,
    val title: String,
    val icon: ImageVector
)

private val MAIN_NAV_TABS = listOf(
    MainNavTab(0, "Cari", Icons.Default.Search),
    MainNavTab(1, "Darurat", Icons.Default.Call),
    MainNavTab(2, "Beri #Tag", Icons.Default.AddCircle),
    MainNavTab(3, "Proteksi", Icons.Default.Lock)
)

/**
 * Shell navigasi utama bergaya Getcontact & Truecaller:
 * Header ringkas & bersih di bagian atas, serta Bottom Navigation Bar berikon intuitif.
 */
@Composable
fun MainScaffold(
    viewModel: CallTagMainViewModel
) {
    val colors = CallTagDesign.colors
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var prefilledReportPhone by rememberSaveable { mutableStateOf("") }

    var reportRequestId by rememberSaveable { mutableIntStateOf(0) }
    val tabStateHolder = rememberSaveableStateHolder()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.backgroundPrimary,
        topBar = {
            Surface(
                color = colors.backgroundPrimary
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CallTagSpacing.lg, vertical = CallTagSpacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.sm)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(CallTagRadius.sm))
                                .background(colors.brandSolid),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#",
                                color = colors.onBrand,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = "CallTag",
                            color = colors.textPrimary,
                            fontSize = CallTagType.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .heightIn(min = CallTagTouch.compact)
                            .clip(RoundedCornerShape(CallTagRadius.pill))
                            .clickable { viewModel.toggleDarkMode() },
                        shape = RoundedCornerShape(CallTagRadius.pill),
                        color = colors.surfaceSubtle,
                        border = BorderStroke(1.dp, colors.borderSubtle)
                    ) {
                        Box(
                            modifier = Modifier.padding(
                                horizontal = CallTagSpacing.md,
                                vertical = CallTagSpacing.xs
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isDarkMode) "Terang" else "Gelap",
                                color = colors.textPrimary,
                                fontSize = CallTagType.label,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = colors.surfaceCard,
                border = BorderStroke(1.dp, colors.borderSubtle)
            ) {
                NavigationBar(
                    containerColor = colors.surfaceCard,
                    tonalElevation = 0.dp
                ) {
                    MAIN_NAV_TABS.forEach { tab ->
                        val selected = selectedTabIndex == tab.index
                        val activeAccent = if (tab.index == 1) colors.dangerScam else colors.brandPrimary
                        val activeContainer = if (tab.index == 1) {
                            colors.dangerScamContainer
                        } else {
                            colors.brandContainer
                        }

                        NavigationBarItem(
                            selected = selected,
                            onClick = { selectedTabIndex = tab.index },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = CallTagType.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = activeAccent,
                                selectedTextColor = activeAccent,
                                indicatorColor = activeContainer,
                                unselectedIconColor = colors.textSecondary,
                                unselectedTextColor = colors.textSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            tabStateHolder.SaveableStateProvider(selectedTabIndex) {
                when (selectedTabIndex) {
                    0 -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToReportWithPhone = { phoneE164 ->
                            prefilledReportPhone = phoneE164
                            reportRequestId += 1
                            selectedTabIndex = 2
                        }
                    )
                    1 -> EmergencyScreen(
                        viewModel = viewModel
                    )
                    2 -> ReportAndHubScreen(
                        viewModel = viewModel,
                        prefilledPhone = prefilledReportPhone,
                        prefillRequestId = reportRequestId
                    )
                    3 -> SpamGuardScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
