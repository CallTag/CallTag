package id.calltag.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.calltag.app.ui.components.CallerIdOverlayCard
import id.calltag.app.ui.theme.CallTagDesign
import id.calltag.app.ui.theme.CallTagRadius
import id.calltag.app.ui.theme.CallTagSpacing
import id.calltag.app.ui.theme.CallTagTouch
import id.calltag.app.ui.theme.CallTagType
import id.calltag.app.ui.viewmodel.CallTagMainViewModel

@Composable
fun SpamGuardScreen(
    viewModel: CallTagMainViewModel,
    modifier: Modifier = Modifier
) {
    val colors = CallTagDesign.colors

    val autoBlockEnabled by viewModel.autoBlockScammer.collectAsState()
    val showHeadsUpEnabled by viewModel.showHeadsUpAlert.collectAsState()
    val cacheStats by viewModel.cacheStats.collectAsState()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val dangerScammers by viewModel.dangerScammers.collectAsState()
    val overlayPreview by viewModel.activeOverlayPreview.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundPrimary),
        contentPadding = PaddingValues(
            horizontal = CallTagSpacing.md,
            vertical = CallTagSpacing.md
        ),
        verticalArrangement = Arrangement.spacedBy(CallTagSpacing.md)
    ) {
        // 1. Kartu Utama Proteksi Panggilan (Gaya Truecaller Shield)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CallTagRadius.lg),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                border = BorderStroke(1.dp, colors.borderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(CallTagSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(CallTagSpacing.md)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.sm)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (autoBlockEnabled) colors.cleanEmeraldContainer else colors.surfaceSubtle
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (autoBlockEnabled) colors.cleanEmerald else colors.textSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)) {
                            Text(
                                text = if (autoBlockEnabled) {
                                    "Proteksi Penipuan Aktif"
                                } else {
                                    "Proteksi Otomatis Nonaktif"
                                },
                                color = colors.textPrimary,
                                fontSize = CallTagType.title,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Atur cara CallTag melindungi telepon masuk Anda",
                                color = colors.textSecondary,
                                fontSize = CallTagType.label
                            )
                        }
                    }

                    HorizontalDivider(color = colors.borderSubtle)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = CallTagSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)
                        ) {
                            Text(
                                text = "Blokir Otomatis Penipu",
                                color = colors.textPrimary,
                                fontSize = CallTagType.subtitle,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tolak otomatis nomor berstatus Bahaya Penipuan sebelum HP berdering",
                                color = colors.textSecondary,
                                fontSize = CallTagType.label,
                                lineHeight = 17.sp
                            )
                        }
                        Switch(
                            checked = autoBlockEnabled,
                            onCheckedChange = { viewModel.setAutoBlockScammer(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.onBrand,
                                checkedTrackColor = colors.brandSolid
                            )
                        )
                    }

                    HorizontalDivider(color = colors.borderSubtle)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = CallTagSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)
                        ) {
                            Text(
                                text = "Notifikasi Caller ID",
                                color = colors.textPrimary,
                                fontSize = CallTagType.subtitle,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tampilkan kartu nama #Tag saat nomor asing menelepon",
                                color = colors.textSecondary,
                                fontSize = CallTagType.label,
                                lineHeight = 17.sp
                            )
                        }
                        Switch(
                            checked = showHeadsUpEnabled,
                            onCheckedChange = { viewModel.setShowHeadsUpAlert(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.onBrand,
                                checkedTrackColor = colors.brandSolid
                            )
                        )
                    }
                }
            }
        }

        // 2. Kartu Database Perlindungan Offline
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CallTagRadius.lg),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                border = BorderStroke(1.dp, colors.borderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(CallTagSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(CallTagSpacing.sm)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)) {
                        Text(
                            text = "Database Perlindungan",
                            color = colors.textPrimary,
                            fontSize = CallTagType.title,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tersimpan di HP Anda agar tetap mengenali penipu walau tanpa sinyal",
                            color = colors.textSecondary,
                            fontSize = CallTagType.label
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                    ) {
                        CleanMetricBox(
                            label = "Nomor Dikenali",
                            value = "${cacheStats.cachedPhoneCount}",
                            modifier = Modifier.weight(1f)
                        )
                        CleanMetricBox(
                            label = "Nomor Darurat",
                            value = "${cacheStats.emergencyContactsCount}",
                            modifier = Modifier.weight(1f)
                        )
                        CleanMetricBox(
                            label = "Versi Data",
                            value = "v${cacheStats.syncVersion}",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = syncStatusMessage,
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )

                    Button(
                        onClick = { viewModel.runOfflineDeltaSync() },
                        enabled = !isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = CallTagTouch.action),
                        shape = RoundedCornerShape(CallTagRadius.md),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.brandSolid,
                            contentColor = colors.onBrand
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(CallTagSpacing.xs))
                        Text(
                            text = if (isSyncing) "Memperbarui Database..." else "Perbarui Database",
                            fontSize = CallTagType.body,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Pratinjau Kartu Caller ID saat Telepon Masuk
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CallTagRadius.lg),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                border = BorderStroke(1.dp, colors.borderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(CallTagSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(CallTagSpacing.sm)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)) {
                        Text(
                            text = "Contoh Tampilan Telepon Masuk",
                            color = colors.textPrimary,
                            fontSize = CallTagType.subtitle,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Lihat tampilan kartu Caller ID berdasarkan tingkat risiko nomor",
                            color = colors.textSecondary,
                            fontSize = CallTagType.label
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.simulateOverlayByLevel("SUSPICIOUS") },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = CallTagTouch.action),
                            shape = RoundedCornerShape(CallTagRadius.md),
                            border = BorderStroke(1.dp, colors.suspiciousAmber)
                        ) {
                            Text(
                                text = "Waspada",
                                color = colors.suspiciousAmber,
                                fontSize = CallTagType.label,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.simulateOverlayByLevel("SPAM") },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = CallTagTouch.action),
                            shape = RoundedCornerShape(CallTagRadius.md),
                            border = BorderStroke(1.dp, colors.spamOrange)
                        ) {
                            Text(
                                text = "Spam",
                                color = colors.spamOrange,
                                fontSize = CallTagType.label,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { viewModel.simulateOverlayByLevel("DANGER_SCAM") },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = CallTagTouch.action),
                            shape = RoundedCornerShape(CallTagRadius.md),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.dangerSolid,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Penipuan",
                                fontSize = CallTagType.label,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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
                    onRejectSimulatedCall = { viewModel.recordSimulatedBlock(preview.phoneTag) }
                )
            }
        }

        // 4. Daftar Nomor Berbahaya yang Dikenali
        item {
            Text(
                text = "Daftar Nomor Berbahaya (${dangerScammers.size})",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = CallTagSpacing.xxs)
            )
        }

        items(dangerScammers, key = { it.phoneE164 }) { scammer ->
            val badgeColor = CallTagDesign.reputationBadgeColor(scammer.reputationLevel)
            val containerColor = CallTagDesign.reputationContainerColor(scammer.reputationLevel)
            val statusLabel = CallTagDesign.reputationLabel(scammer.reputationLevel)
            val initials = CallTagDesign.callerInitials(scammer.topCommunityLabel, scammer.reputationLevel)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CallTagRadius.lg),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                border = BorderStroke(1.dp, colors.borderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(CallTagSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(containerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = badgeColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.width(CallTagSpacing.sm))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)
                    ) {
                        Text(
                            text = scammer.topCommunityLabel,
                            color = colors.textPrimary,
                            fontSize = CallTagType.body,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = scammer.phoneE164,
                            style = CallTagDesign.monoStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(CallTagSpacing.xs))

                    Surface(
                        shape = RoundedCornerShape(CallTagRadius.md),
                        color = containerColor
                    ) {
                        Text(
                            text = statusLabel,
                            modifier = Modifier.padding(
                                horizontal = CallTagSpacing.xs,
                                vertical = 4.dp
                            ),
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
