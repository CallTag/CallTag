package id.calltag.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.calltag.app.data.local.entity.CallScreeningLog
import id.calltag.app.data.repository.FullPhoneLookupResult
import id.calltag.app.ui.components.CallerIdOverlayCard
import id.calltag.app.ui.theme.CallTagDesign
import id.calltag.app.ui.theme.CallTagRadius
import id.calltag.app.ui.theme.CallTagSpacing
import id.calltag.app.ui.theme.CallTagTouch
import id.calltag.app.ui.theme.CallTagType
import id.calltag.app.ui.viewmodel.CallTagMainViewModel
import id.calltag.app.ui.viewmodel.UiLoadState
import id.calltag.app.util.RolePermissionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class SampleLookupChip(
    val tagLabel: String,
    val phone: String
)

private val QUICK_SAMPLE_CHIPS = listOf(
    SampleLookupChip("#KurirAPK", "081299887766"),
    SampleLookupChip("#PinjolIlegal", "085711223344"),
    SampleLookupChip("#CSBankPalsu", "081377665544"),
    SampleLookupChip("#SpamJudol", "089655443322"),
    SampleLookupChip("#Waspada", "082144556677"),
    SampleLookupChip("#NomorAman", "081100001111")
)

/**
 * Layar utama.
 *
 * Alur baca dirancang untuk dibaca < 1 detik (DESIGN.md Bagian 1):
 * 1. Judul besar + satu kalimat penjelas.
 * 2. Kolom pencarian nomor — satu-satunya aksi utama di layar ini.
 * 3. Bilah status Caller ID yang tenang (bukan kartu besar yang bersaing).
 * 4. Hasil pencarian: kartu reputasi dengan pita warna di tepi kiri.
 * 5. Riwayat panggilan.
 *
 * Semua angka nomor memakai monospace; semua pemisahan memakai border 1dp, tanpa bayangan.
 */
@Composable
fun HomeScreen(
    viewModel: CallTagMainViewModel,
    onNavigateToReportWithPhone: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val colors = CallTagDesign.colors

    val searchQuery by viewModel.searchQuery.collectAsState()
    val lookupState by viewModel.lookupState.collectAsState()
    val overlayPreview by viewModel.activeOverlayPreview.collectAsState()
    val screeningLogs by viewModel.recentScreeningLogs.collectAsState()
    val autoBlockEnabled by viewModel.autoBlockScammer.collectAsState()

    var isRoleHeld by remember {
        mutableStateOf(RolePermissionManager.isCallScreeningRoleHeld(context))
    }
    var hasPermissions by remember {
        mutableStateOf(RolePermissionManager.hasRuntimePermissions(context))
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isRoleHeld = RolePermissionManager.isCallScreeningRoleHeld(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermissions = RolePermissionManager.hasRuntimePermissions(context)
        isRoleHeld = RolePermissionManager.isCallScreeningRoleHeld(context)
    }

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
            SearchPanel(
                searchQuery = searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                onClearQuery = { viewModel.updateSearchQuery("") },
                onSearch = {
                    focusManager.clearFocus()
                    viewModel.performPhoneLookup()
                },
                onSampleChipClick = { phone ->
                    focusManager.clearFocus()
                    viewModel.performPhoneLookup(phone)
                }
            )
        }

        item {
            CallerIdStatusBar(
                isRoleHeld = isRoleHeld,
                hasPermissions = hasPermissions,
                autoBlockEnabled = autoBlockEnabled,
                onActivateCallerId = {
                    if (!hasPermissions) {
                        permissionLauncher.launch(RolePermissionManager.getRequiredRuntimePermissions())
                    } else {
                        val intent = RolePermissionManager.createCallScreeningRoleIntent(context)
                        if (intent != null) {
                            roleLauncher.launch(intent)
                        } else {
                            permissionLauncher.launch(RolePermissionManager.getRequiredRuntimePermissions())
                        }
                    }
                }
            )
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

        item {
            when (val state = lookupState) {
                is UiLoadState.Idle -> Unit
                is UiLoadState.Loading -> LookupLoadingCard()
                is UiLoadState.Error -> LookupErrorCard(message = state.message)
                is UiLoadState.Success -> CallerProfileResultCard(
                    result = state.data,
                    onShowOverlayPreview = { viewModel.triggerOverlayPreview(state.data) },
                    onReportThisPhone = {
                        onNavigateToReportWithPhone(state.data.phoneTag.phoneE164)
                    }
                )
            }
        }

        item {
            SectionHeader(
                title = "Panggilan Terakhir",
                meta = if (screeningLogs.isNotEmpty()) "${screeningLogs.size} aktivitas" else null
            )
        }

        if (screeningLogs.isEmpty()) {
            item { EmptyHistoryCard() }
        } else {
            items(screeningLogs, key = { it.id }) { logItem ->
                RecentCallRowCard(
                    log = logItem,
                    onClick = { viewModel.performPhoneLookup(logItem.phoneE164) }
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Panel pencarian                                                     */
/* ------------------------------------------------------------------ */

/**
 * Panel pencarian. Kolom input digabung dengan tombol cari di dalam satu bidang
 * tegas, sehingga mata membaca satu kontrol bukan dua. Nomor contoh dipisah
 * dengan garis putus agar jelas itu bukan input.
 */
@Composable
private fun SearchPanel(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSearch: () -> Unit,
    onSampleChipClick: (String) -> Unit
) {
    val colors = CallTagDesign.colors

    Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.sm)) {
        Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)) {
            Text(
                text = "Siapa yang menelepon?",
                color = colors.textPrimary,
                fontSize = CallTagType.display,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                lineHeight = 29.sp
            )
            Text(
                text = "Cek reputasi nomor asing sebelum Anda angkat.",
                color = colors.textSecondary,
                fontSize = CallTagType.body
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(CallTagRadius.md),
                placeholder = {
                    Text(
                        text = "08xx atau +628xx",
                        color = colors.textSecondary,
                        fontSize = CallTagType.body
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.textSecondary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearQuery) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus nomor",
                                tint = colors.textSecondary
                            )
                        }
                    }
                },
                textStyle = CallTagDesign.monoStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.brandPrimary,
                    unfocusedBorderColor = colors.borderStrong,
                    focusedContainerColor = colors.surfaceCard,
                    unfocusedContainerColor = colors.surfaceCard
                )
            )

            Button(
                onClick = onSearch,
                modifier = Modifier
                    .heightIn(min = CallTagTouch.action)
                    .height(CallTagTouch.action),
                shape = RoundedCornerShape(CallTagRadius.md),
                contentPadding = PaddingValues(horizontal = CallTagSpacing.lg),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.brandPrimary,
                    contentColor = colors.onBrand
                )
            ) {
                Text(
                    text = "Cari",
                    fontSize = CallTagType.subtitle,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs),
            contentPadding = PaddingValues(top = CallTagSpacing.xxs)
        ) {
            items(QUICK_SAMPLE_CHIPS) { chip ->
                SampleChip(
                    label = chip.tagLabel,
                    onClick = { onSampleChipClick(chip.phone) }
                )
            }
        }
    }
}

@Composable
private fun SampleChip(label: String, onClick: () -> Unit) {
    val colors = CallTagDesign.colors
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(CallTagRadius.pill))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CallTagRadius.pill),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(
                horizontal = CallTagSpacing.sm,
                vertical = CallTagSpacing.xs
            ),
            color = colors.textSecondary,
            fontSize = CallTagType.label,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/* ------------------------------------------------------------------ */
/* Bilah status Caller ID                                              */
/* ------------------------------------------------------------------ */

/**
 * Bilah status tipis. Saat aktif hanya satu baris tenang agar pencarian tetap fokus.
 * Saat belum aktif, bilah berubah warna amber dan menampilkan satu tombol aksi.
 */
@Composable
private fun CallerIdStatusBar(
    isRoleHeld: Boolean,
    hasPermissions: Boolean,
    autoBlockEnabled: Boolean,
    onActivateCallerId: () -> Unit
) {
    val colors = CallTagDesign.colors
    val isActive = isRoleHeld && hasPermissions

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CallTagRadius.md),
        color = if (isActive) colors.surfaceCard else colors.suspiciousAmberContainer,
        border = BorderStroke(
            1.dp,
            if (isActive) colors.borderSubtle else colors.suspiciousAmber.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = CallTagSpacing.sm,
                vertical = CallTagSpacing.xs
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isActive) colors.cleanEmerald else colors.suspiciousAmber,
                modifier = Modifier.size(18.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = if (isActive) "Caller ID aktif" else "Caller ID belum aktif",
                    color = colors.textPrimary,
                    fontSize = CallTagType.label,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isActive) {
                        if (autoBlockEnabled) "Blokir penipu otomatis: aktif"
                        else "Blokir penipu otomatis: nonaktif"
                    } else {
                        "Kenali penipu saat telepon berdering."
                    },
                    color = colors.textSecondary,
                    fontSize = CallTagType.caption
                )
            }

            if (!isActive) {
                Button(
                    onClick = onActivateCallerId,
                    modifier = Modifier.heightIn(min = CallTagTouch.compact),
                    shape = RoundedCornerShape(CallTagRadius.sm),
                    contentPadding = PaddingValues(horizontal = CallTagSpacing.md),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.brandPrimary,
                        contentColor = colors.onBrand
                    )
                ) {
                    Text(
                        text = "Aktifkan",
                        fontSize = CallTagType.label,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Kartu profil hasil pencarian                                        */
/* ------------------------------------------------------------------ */

/**
 * Kartu reputasi nomor. Pita warna 4dp di tepi kiri memberi sinyal bahaya
 * yang terbaca dari sudut mata tanpa harus membaca teks.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CallerProfileResultCard(
    result: FullPhoneLookupResult,
    onShowOverlayPreview: () -> Unit,
    onReportThisPhone: () -> Unit
) {
    val colors = CallTagDesign.colors
    val pt = result.phoneTag
    val badgeColor = CallTagDesign.reputationBadgeColor(pt.reputationLevel)
    val containerColor = CallTagDesign.reputationContainerColor(pt.reputationLevel)
    val statusText = CallTagDesign.reputationLabel(pt.reputationLevel)
    val initials = CallTagDesign.callerInitials(pt.topCommunityLabel, pt.reputationLevel)

    val hashtags = remember(pt.tagsSummary, pt.topCommunityLabel, result.categoryBreakdown) {
        CallTagDesign.extractHashtagList(
            tagsSummary = pt.tagsSummary,
            topLabel = pt.topCommunityLabel,
            categoryBreakdown = result.categoryBreakdown
        )
    }

    val isCleanNoReports = pt.reputationLevel.equals("CLEAN", ignoreCase = true) && pt.uniqueReporters == 0
    val displayTitle = if (isCleanNoReports) "Belum Ada Laporan Spam" else pt.topCommunityLabel

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CallTagRadius.lg),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        color = badgeColor,
                        size = Size(4.dp.toPx(), size.height)
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Baris status ringkas di atas.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = CallTagSpacing.lg + 4.dp,
                            end = CallTagSpacing.lg,
                            top = CallTagSpacing.sm,
                            bottom = CallTagSpacing.sm
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText.uppercase(),
                        color = badgeColor,
                        fontSize = CallTagType.caption,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = if (pt.uniqueReporters > 0) {
                            "${pt.uniqueReporters} pelapor"
                        } else {
                            "Tidak ada laporan"
                        },
                        color = colors.textSecondary,
                        fontSize = CallTagType.caption,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(
                    modifier = Modifier.padding(
                        start = CallTagSpacing.lg + 4.dp,
                        end = CallTagSpacing.lg,
                        bottom = CallTagSpacing.lg
                    ),
                    verticalArrangement = Arrangement.spacedBy(CallTagSpacing.lg)
                ) {
                    // Identitas nomor.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.sm)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(containerColor)
                                .border(1.dp, badgeColor.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                color = badgeColor,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = displayTitle,
                                color = colors.textPrimary,
                                fontSize = CallTagType.title,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 21.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = pt.phoneE164,
                                style = CallTagDesign.monoStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            )
                            Text(
                                text = pt.carrier,
                                color = colors.textSecondary,
                                fontSize = CallTagType.caption,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Skor risiko — angka wajib monospace.
                    Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "Skor risiko",
                                color = colors.textSecondary,
                                fontSize = CallTagType.label,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${pt.spamScore.toInt()}",
                                style = CallTagDesign.monoStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = badgeColor
                                )
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (pt.spamScore.toFloat() / 100f).coerceIn(0.03f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = badgeColor,
                            trackColor = colors.surfaceSubtle
                        )
                    }

                    HorizontalDivider(color = colors.borderSubtle)

                    // Awan #Tag komunitas.
                    Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)) {
                        Text(
                            text = "#Tag Komunitas · ${hashtags.size}",
                            color = colors.textPrimary,
                            fontSize = CallTagType.label,
                            fontWeight = FontWeight.Bold
                        )

                        if (hashtags.isEmpty()) {
                            TagChip(
                                text = "#BelumAdaLaporanSpam",
                                fg = colors.cleanEmerald,
                                bg = colors.cleanEmeraldContainer
                            )
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs),
                                verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                            ) {
                                hashtags.forEach { tag ->
                                    TagChip(
                                        text = tag,
                                        fg = badgeColor,
                                        bg = containerColor,
                                        maxWidth = 210.dp
                                    )
                                }
                            }
                        }
                    }

                    // Catatan pengguna lain.
                    if (result.recentReports.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)) {
                            Text(
                                text = "Catatan Pengguna Lain",
                                color = colors.textPrimary,
                                fontSize = CallTagType.label,
                                fontWeight = FontWeight.Bold
                            )
                            result.recentReports.take(3).forEach { rep ->
                                ReportNote(
                                    tag = rep.tagName,
                                    callType = friendlyCallType(rep.callType),
                                    comment = rep.comment
                                )
                            }
                        }
                    }

                    // Aksi: satu primer, satu sekunder.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                    ) {
                        Button(
                            onClick = onReportThisPhone,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = CallTagTouch.action),
                            shape = RoundedCornerShape(CallTagRadius.md),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.brandPrimary,
                                contentColor = colors.onBrand
                            )
                        ) {
                            Text(
                                text = "Beri #Tag / Lapor",
                                fontSize = CallTagType.label,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onShowOverlayPreview,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = CallTagTouch.action),
                            shape = RoundedCornerShape(CallTagRadius.md),
                            border = BorderStroke(1.dp, colors.borderStrong)
                        ) {
                            Text(
                                text = "Tampilan Telepon",
                                color = colors.textPrimary,
                                fontSize = CallTagType.label,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(text: String, fg: Color, bg: Color, maxWidth: Dp = Dp.Unspecified) {
    Surface(
        modifier = if (maxWidth == Dp.Unspecified) Modifier else Modifier.widthIn(max = maxWidth),
        shape = RoundedCornerShape(CallTagRadius.pill),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = CallTagSpacing.sm,
                vertical = 6.dp
            ),
            color = fg,
            fontSize = CallTagType.label,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReportNote(tag: String, callType: String, comment: String) {
    val colors = CallTagDesign.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(CallTagRadius.sm))
            .padding(CallTagSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "#$tag",
                modifier = Modifier.weight(1f),
                color = colors.textPrimary,
                fontSize = CallTagType.label,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = callType,
                color = colors.textSecondary,
                fontSize = CallTagType.caption
            )
        }
        if (comment.isNotBlank()) {
            Text(
                text = comment,
                color = colors.textSecondary,
                fontSize = CallTagType.label,
                lineHeight = 17.sp
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* Status memuat / galat / kosong                                      */
/* ------------------------------------------------------------------ */

@Composable
private fun LookupLoadingCard() {
    val colors = CallTagDesign.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CallTagRadius.lg),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(CallTagSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.sm)
        ) {
            CircularProgressIndicator(
                color = colors.brandPrimary,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.5.dp
            )
            Text(
                text = "Mencari #Tag dan reputasi nomor…",
                color = colors.textPrimary,
                fontSize = CallTagType.body,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LookupErrorCard(message: String) {
    val colors = CallTagDesign.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CallTagRadius.md),
        colors = CardDefaults.cardColors(containerColor = colors.dangerScamContainer),
        border = BorderStroke(1.dp, colors.dangerScam.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(CallTagSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = colors.dangerScam,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = message,
                color = colors.dangerScam,
                fontSize = CallTagType.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmptyHistoryCard() {
    val colors = CallTagDesign.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CallTagRadius.md),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(CallTagSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)
        ) {
            Text(
                text = "Belum ada riwayat",
                color = colors.textPrimary,
                fontSize = CallTagType.body,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Panggilan asing yang tersaring akan muncul di sini.",
                color = colors.textSecondary,
                fontSize = CallTagType.bodySmall
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, meta: String?) {
    val colors = CallTagDesign.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = CallTagSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = CallTagType.title,
            fontWeight = FontWeight.ExtraBold
        )
        if (meta != null) {
            Text(
                text = meta,
                color = colors.textSecondary,
                fontSize = CallTagType.caption
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* Baris riwayat                                                       */
/* ------------------------------------------------------------------ */

@Composable
private fun RecentCallRowCard(
    log: CallScreeningLog,
    onClick: () -> Unit
) {
    val colors = CallTagDesign.colors
    val badgeColor = CallTagDesign.reputationBadgeColor(log.reputationLevel)
    val containerColor = CallTagDesign.reputationContainerColor(log.reputationLevel)
    val statusLabel = if (log.wasBlocked) {
        "Diblokir"
    } else {
        CallTagDesign.reputationShortLabel(log.reputationLevel)
    }
    val initials = CallTagDesign.callerInitials(log.tagLabel, log.reputationLevel)

    val timeFormatted = remember(log.screenedAtEpochMs) {
        SimpleDateFormat("dd MMM · HH:mm", Locale.forLanguageTag("id-ID"))
            .format(Date(log.screenedAtEpochMs))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CallTagRadius.md))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CallTagRadius.md),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = CallTagTouch.action)
                .padding(horizontal = CallTagSpacing.sm, vertical = CallTagSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = badgeColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.width(CallTagSpacing.sm))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = log.tagLabel,
                    color = colors.textPrimary,
                    fontSize = CallTagType.body,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = log.phoneE164,
                    style = CallTagDesign.monoStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.width(CallTagSpacing.xs))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(CallTagRadius.pill),
                    color = containerColor
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = CallTagSpacing.xs, vertical = 3.dp),
                        color = badgeColor,
                        fontSize = CallTagType.caption,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = timeFormatted,
                    color = colors.textSecondary,
                    fontSize = CallTagType.caption
                )
            }
        }
    }
}

private fun friendlyCallType(raw: String): String {
    return when (raw.uppercase()) {
        "WHATSAPP_CALL" -> "Telepon WhatsApp"
        "WHATSAPP_CHAT" -> "Chat WhatsApp"
        "SMS" -> "Pesan SMS"
        else -> "Telepon Seluler"
    }
}
