package id.calltag.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.calltag.app.ui.components.CallerIdOverlayCard
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import id.calltag.app.ui.components.AccountAccessGate
import id.calltag.app.ui.theme.CallTagDesign
import id.calltag.app.ui.theme.CallTagRadius
import id.calltag.app.ui.theme.CallTagSpacing
import id.calltag.app.ui.theme.CallTagTouch
import id.calltag.app.ui.theme.CallTagType
import id.calltag.app.ui.viewmodel.CallTagMainViewModel
import id.calltag.app.util.RolePermissionManager

private enum class ProtectionPage(val title: String) {
    Overview("Proteksi"),
    Offline("Data offline"),
    Preview("Pratinjau Caller ID"),
    History("Riwayat panggilan")
}

@Composable
fun SpamGuardScreen(
    viewModel: CallTagMainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToAccount: () -> Unit = {}
) {
    val colors = CallTagDesign.colors

    val autoBlockEnabled by viewModel.autoBlockScammer.collectAsState()
    val showHeadsUpEnabled by viewModel.showHeadsUpAlert.collectAsState()
    val cacheStats by viewModel.cacheStats.collectAsState()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val dangerScammers by viewModel.dangerScammers.collectAsState()
    val overlayPreview by viewModel.activeOverlayPreview.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val loggedIn by viewModel.isLoggedIn.collectAsState()
    val history by viewModel.recentScreeningLogs.collectAsState(initial = emptyList())
    var page by rememberSaveable { mutableStateOf(ProtectionPage.Overview) }
    var callerIdActive by remember { mutableStateOf(RolePermissionManager.isCallScreeningRoleHeld(context)) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                callerIdActive = RolePermissionManager.isCallScreeningRoleHeld(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun goBack() {
        if (page == ProtectionPage.Preview) viewModel.dismissOverlayPreview()
        page = ProtectionPage.Overview
    }
    BackHandler(enabled = page != ProtectionPage.Overview, onBack = ::goBack)

    key(page) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(colors.backgroundPrimary),
            contentPadding = PaddingValues(
                start = CallTagSpacing.md,
                end = CallTagSpacing.md,
                top = CallTagSpacing.md,
                bottom = CallTagSpacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(CallTagSpacing.md)
        ) {
            item {
                SettingsHeading(page.title, if (page == ProtectionPage.Overview) null else ::goBack)
            }
            if (page == ProtectionPage.Overview) {
                item {
                    SettingsMenu {
                        SettingsMenuRow(
                            title = if (callerIdActive) "Caller ID aktif" else "Caller ID belum aktif",
                            summary = "Lihat izin perlindungan di Akun",
                            onClick = onNavigateToAccount
                        )
                    }
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CallTagRadius.lg),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                        border = BorderStroke(1.dp, colors.borderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = CallTagSpacing.md)) {
                            ProtectionSetting(
                                title = "Blokir otomatis penipu",
                                description = "Menolak nomor berstatus Bahaya Penipuan jika data server berumur maksimal 14 hari.",
                                checked = autoBlockEnabled,
                                onCheckedChange = { viewModel.setAutoBlockScammer(it) }
                            )
                            HorizontalDivider(color = colors.borderSubtle)
                            ProtectionSetting(
                                title = "Notifikasi Caller ID",
                                description = "Tampilkan identitas #Tag saat panggilan masuk.",
                                checked = showHeadsUpEnabled,
                                onCheckedChange = { viewModel.setShowHeadsUpAlert(it) }
                            )
                        }
                    }
                }

                item {
                    SettingsMenu {
                        SettingsMenuRow(ProtectionPage.Offline.title) { page = ProtectionPage.Offline }
                        HorizontalDivider(color = colors.borderSubtle)
                        SettingsMenuRow(ProtectionPage.Preview.title) { page = ProtectionPage.Preview }
                        HorizontalDivider(color = colors.borderSubtle)
                        SettingsMenuRow(ProtectionPage.History.title) { page = ProtectionPage.History }
                    }
                }
            }
            if (page == ProtectionPage.Offline) {

                item {
                    ProtectionSectionTitle("Data offline", "Data di perangkat dapat dibaca tanpa koneksi.")
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CallTagRadius.lg),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                        border = BorderStroke(1.dp, colors.borderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(CallTagSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(CallTagSpacing.md)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                            ) {
                                CleanMetricBox(
                                    label = "Nomor dalam cache",
                                    value = "${cacheStats.cachedPhoneCount}",
                                    modifier = Modifier.weight(1f)
                                )
                                CleanMetricBox(
                                    label = "Kontak darurat",
                                    value = "${cacheStats.emergencyContactsCount}",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)) {
                                Text(
                                    text = "Versi sinkronisasi: ${cacheStats.syncVersion}",
                                    color = colors.textPrimary,
                                    fontSize = CallTagType.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = syncStatusMessage,
                                    color = colors.textSecondary,
                                    fontSize = CallTagType.bodySmall
                                )
                            }
                            Button(
                                onClick = { viewModel.runOfflineDeltaSync() },
                                enabled = !isSyncing,
                                modifier = Modifier.fillMaxWidth().heightIn(min = CallTagTouch.action),
                                shape = RoundedCornerShape(CallTagRadius.md),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.brandSolid,
                                    contentColor = colors.onBrand
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(CallTagSpacing.xs))
                                Text(
                                    text = if (isSyncing) "Menyinkronkan…" else "Sinkronkan data",
                                    fontSize = CallTagType.body,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    ProtectionSectionTitle(
                        "Nomor berbahaya (${dangerScammers.size})",
                        "Nomor berstatus Bahaya Penipuan dalam data lokal."
                    )
                }
                if (dangerScammers.isEmpty()) {
                    item { Text("Belum ada nomor berbahaya dalam data lokal.", color = colors.textSecondary) }
                }
                items(dangerScammers, key = { it.phoneE164 }) { scammer ->
                    val badgeColor = CallTagDesign.reputationBadgeColor(scammer.reputationLevel)
                    val containerColor = CallTagDesign.reputationContainerColor(scammer.reputationLevel)
                    val statusLabel = CallTagDesign.reputationLabel(scammer.reputationLevel)
                    val initials = CallTagDesign.callerInitials(scammer.topCommunityLabel, scammer.reputationLevel)

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = CallTagSpacing.xs),
                            horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(CallTagTouch.compact).clip(CircleShape).background(containerColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(initials, color = badgeColor, fontSize = CallTagType.subtitle, fontWeight = FontWeight.Bold)
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)
                            ) {
                                Text(
                                    text = scammer.topCommunityLabel,
                                    color = colors.textPrimary,
                                    fontSize = CallTagType.body,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = scammer.phoneE164,
                                    style = CallTagDesign.monoStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textSecondary
                                    )
                                )
                                Text(statusLabel, color = badgeColor, fontSize = CallTagType.label, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        HorizontalDivider(color = colors.borderSubtle)
                    }
                }

            }
            if (page == ProtectionPage.Preview) {
                item {
                    ProtectionSectionTitle(
                        "Coba kartu Caller ID",
                        "Simulasi tampilan kartu; tidak melakukan panggilan."
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.simulateOverlayByLevel("SUSPICIOUS") },
                                modifier = Modifier.weight(1f).heightIn(min = CallTagTouch.action),
                                shape = RoundedCornerShape(CallTagRadius.md),
                                border = BorderStroke(1.dp, colors.suspiciousAmber),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.suspiciousAmber)
                            ) {
                                Text("Waspada", fontSize = CallTagType.body, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { viewModel.simulateOverlayByLevel("SPAM") },
                                modifier = Modifier.weight(1f).heightIn(min = CallTagTouch.action),
                                shape = RoundedCornerShape(CallTagRadius.md),
                                border = BorderStroke(1.dp, colors.spamOrange),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.spamOrange)
                            ) {
                                Text("Spam", fontSize = CallTagType.body, fontWeight = FontWeight.Bold)
                            }
                        }
                        Button(
                            onClick = { viewModel.simulateOverlayByLevel("DANGER_SCAM") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = CallTagTouch.action),
                            shape = RoundedCornerShape(CallTagRadius.md),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.dangerSolid,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Bahaya penipuan", fontSize = CallTagType.body, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (overlayPreview != null) {
                    item {
                        val preview = overlayPreview!!
                        CallerIdOverlayCard(
                            phoneTag = preview.phoneTag,
                            lookupLatencyMs = preview.lookupLatencyMs,
                            source = preview.source,
                            autoBlockEnabled = autoBlockEnabled,
                            onDismiss = { viewModel.dismissOverlayPreview() },
                            onRejectSimulatedCall = viewModel::dismissOverlayPreview
                        )
                    }
                }

            }
            if (page == ProtectionPage.History) {
                when {
                    !loggedIn -> item { AccountAccessGate(onOpenAccount = onNavigateToAccount) }
                    history.isEmpty() -> item {
                        Text("Belum ada riwayat panggilan.", color = colors.textSecondary, fontSize = CallTagType.body)
                    }
                    else -> items(history, key = { it.id }) { entry ->
                        Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)) {
                            Text(entry.tagLabel.ifBlank { "Panggilan masuk" }, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                            Text(entry.phoneE164, style = CallTagDesign.monoStyle(CallTagType.body, color = colors.textSecondary))
                            Text(
                                if (entry.wasBlocked) "Diblokir" else CallTagDesign.reputationLabel(entry.reputationLevel),
                                color = colors.textSecondary,
                                fontSize = CallTagType.bodySmall
                            )
                            HorizontalDivider(color = colors.borderSubtle)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanMetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = CallTagDesign.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(CallTagRadius.md),
        color = colors.surfaceSubtle
    ) {
        Column(
            modifier = Modifier.padding(CallTagSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = value,
                style = CallTagDesign.monoStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary
                )
            )
            Text(
                text = label,
                color = colors.textSecondary,
                fontSize = CallTagType.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


@Composable
private fun ProtectionSectionTitle(title: String, description: String) {
    val colors = CallTagDesign.colors
    Column(
        modifier = Modifier.padding(top = CallTagSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)
    ) {
        Text(title, color = colors.textPrimary, fontSize = CallTagType.title, fontWeight = FontWeight.Bold)
        Text(description, color = colors.textSecondary, fontSize = CallTagType.bodySmall)
    }
}

@Composable
private fun ProtectionSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = CallTagDesign.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = CallTagTouch.action)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = CallTagSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)
        ) {
            Text(title, color = colors.textPrimary, fontSize = CallTagType.subtitle, fontWeight = FontWeight.Bold)
            Text(description, color = colors.textSecondary, fontSize = CallTagType.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onBrand,
                checkedTrackColor = colors.brandSolid
            )
        )
    }
}
