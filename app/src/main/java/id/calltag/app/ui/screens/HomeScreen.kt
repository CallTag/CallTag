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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.calltag.app.data.local.entity.CallScreeningLog
import id.calltag.app.data.repository.FullPhoneLookupResult
import id.calltag.app.ui.components.CallerIdOverlayCard
import id.calltag.app.ui.components.ScreenIntro
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
                isLoading = lookupState is UiLoadState.Loading,
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

        if (lookupState !is UiLoadState.Idle) item {
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
// Bidang dan tombol kini disusun vertikal agar nomor tetap terbaca pada layar kecil.
@Composable
private fun SearchPanel(
    searchQuery: String,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSearch: () -> Unit,
    onSampleChipClick: (String) -> Unit
) {
    val colors = CallTagDesign.colors
    val canSearch = searchQuery.isNotBlank() && !isLoading

    Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.sm)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CallTagRadius.lg),
            color = colors.brandContainer
        ) {
            Column(
                modifier = Modifier.padding(CallTagSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(CallTagSpacing.lg)
            ) {
                ScreenIntro(
                    eyebrow = "CEK NOMOR",
                    title = "Siapa yang\nmenelepon?",
                    description = "Kenali reputasi nomor asing dari laporan komunitas."
                )
                Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.sm)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(CallTagRadius.md),
                        label = { Text("Nomor telepon") },
                        placeholder = {
                            Text(
                                text = "08xx atau +628xx",
                                style = CallTagDesign.monoStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = colors.textSecondary
                                )
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(onSearch = { if (canSearch) onSearch() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.brandPrimary,
                            unfocusedBorderColor = colors.borderStrong,
                            focusedLabelColor = colors.brandPrimary,
                            unfocusedLabelColor = colors.textSecondary,
                            focusedContainerColor = colors.surfaceCard,
                            unfocusedContainerColor = colors.surfaceCard
                        )
                    )
                    Button(
                        onClick = onSearch,
                        enabled = canSearch,
                        modifier = Modifier.fillMaxWidth().heightIn(min = CallTagTouch.action),
                        shape = RoundedCornerShape(CallTagRadius.md),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.brandSolid,
                            contentColor = colors.onBrand
                        )
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(CallTagSpacing.xs))
                        Text(
                            text = if (isLoading) "Memeriksa nomor…" else "Cek nomor",
                            fontSize = CallTagType.subtitle,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)) {
            Text(
                text = "Coba nomor contoh",
                color = colors.textSecondary,
                fontSize = CallTagType.label,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)) {
                items(QUICK_SAMPLE_CHIPS) { chip ->
                    SampleChip(label = chip.tagLabel, onClick = { onSampleChipClick(chip.phone) })
                }
            }
        }
    }
}

@Composable
private fun SampleChip(label: String, onClick: () -> Unit) {
    val colors = CallTagDesign.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(CallTagRadius.md),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Box(
            modifier = Modifier.heightIn(min = CallTagTouch.action)
                .padding(horizontal = CallTagSpacing.sm, vertical = CallTagSpacing.xs),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = colors.textPrimary,
                fontSize = CallTagType.label,
                fontWeight = FontWeight.SemiBold
            )
        }
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
                        containerColor = colors.brandSolid,
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
    var detailsExpanded by rememberSaveable(pt.phoneE164) { mutableStateOf(false) }

    val hashtags = remember(pt.tagsSummary, pt.topCommunityLabel, result.categoryBreakdown) {
        CallTagDesign.extractHashtagList(
            tagsSummary = pt.tagsSummary,
            topLabel = pt.topCommunityLabel,
            categoryBreakdown = result.categoryBreakdown
        )
    }
    val isCleanNoReports = pt.reputationLevel.equals("CLEAN", ignoreCase = true) && pt.uniqueReporters == 0
    val displayTitle = if (isCleanNoReports) "Belum ada laporan spam" else pt.topCommunityLabel

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CallTagRadius.lg),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier
                .drawBehind { drawRect(color = badgeColor, size = Size(4.dp.toPx(), size.height)) }
                .padding(CallTagSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(CallTagSpacing.md)
        ) {
            // Baris status ringkas di atas.
            Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)) {
                Text(
                    text = "HASIL PEMERIKSAAN",
                    color = colors.textSecondary,
                    fontSize = CallTagType.caption,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                // Identitas nomor.
                Text(
                    text = pt.phoneE164,
                    style = CallTagDesign.monoStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Text(
                    text = displayTitle,
                    color = colors.textPrimary,
                    fontSize = CallTagType.title,
                    fontWeight = FontWeight.Bold
                )
                if (pt.carrier.isNotBlank()) {
                    Text(text = pt.carrier, color = colors.textSecondary, fontSize = CallTagType.bodySmall)
                }
                TagChip(text = if (isCleanNoReports) "Belum ada laporan" else statusText, fg = badgeColor, bg = containerColor)
            }

            // Skor risiko — angka wajib monospace.
            Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Skor risiko", color = colors.textPrimary, fontSize = CallTagType.body, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${pt.uniqueReporters} pelapor",
                            color = colors.textSecondary,
                            fontSize = CallTagType.label
                        )
                    }
                    Text(
                        text = "${pt.spamScore.toInt()} / 100",
                        style = CallTagDesign.monoStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                    )
                }
                LinearProgressIndicator(
                    progress = { (pt.spamScore.toFloat() / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(CallTagSpacing.xs).clip(RoundedCornerShape(CallTagRadius.pill)),
                    color = badgeColor,
                    trackColor = colors.surfaceSubtle
                )
                if (isCleanNoReports) {
                    Text(
                        text = "Belum ada laporan bukan jaminan nomor aman.",
                        color = colors.textSecondary,
                        fontSize = CallTagType.bodySmall
                    )
                }
            }

            // Aksi: satu primer, satu sekunder.
            Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)) {
                Button(
                    onClick = onReportThisPhone,
                    modifier = Modifier.fillMaxWidth().heightIn(min = CallTagTouch.action),
                    shape = RoundedCornerShape(CallTagRadius.md),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandSolid, contentColor = colors.onBrand)
                ) {
                    Text(text = "Beri #Tag / lapor nomor", fontSize = CallTagType.body, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onShowOverlayPreview,
                    modifier = Modifier.fillMaxWidth().heightIn(min = CallTagTouch.action),
                    shape = RoundedCornerShape(CallTagRadius.md),
                    border = BorderStroke(1.dp, colors.borderStrong)
                ) {
                    Text(text = "Lihat tampilan telepon", color = colors.textPrimary, fontSize = CallTagType.body, fontWeight = FontWeight.SemiBold)
                }
            }
            HorizontalDivider(color = colors.borderSubtle)
            Surface(
                onClick = { detailsExpanded = !detailsExpanded },
                color = colors.surfaceCard,
                shape = RoundedCornerShape(CallTagRadius.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = CallTagTouch.action),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)) {
                        Text(
                            text = if (detailsExpanded) "Tutup rincian komunitas" else "Lihat rincian komunitas",
                            color = colors.textPrimary,
                            fontSize = CallTagType.body,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${hashtags.size} #Tag · ${result.recentReports.size} catatan",
                            color = colors.textSecondary,
                            fontSize = CallTagType.label
                        )
                    }
                    Icon(
                        imageVector = if (detailsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = colors.brandPrimary
                    )
                }
            }
            if (detailsExpanded) {
                // Awan #Tag komunitas.
                Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)) {
                    Text(text = "#Tag komunitas", color = colors.textPrimary, fontSize = CallTagType.subtitle, fontWeight = FontWeight.Bold)
                    if (hashtags.isEmpty()) {
                        Text(text = "Belum ada #Tag untuk nomor ini.", color = colors.textSecondary, fontSize = CallTagType.bodySmall)
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs),
                            verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                        ) {
                            hashtags.forEach { tag -> TagChip(text = tag, fg = badgeColor, bg = containerColor) }
                        }
                    }
                }
                // Catatan pengguna lain.
                Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.sm)) {
                    Text(text = "Catatan pengguna lain", color = colors.textPrimary, fontSize = CallTagType.subtitle, fontWeight = FontWeight.Bold)
                    if (result.recentReports.isEmpty()) {
                        Text(text = "Belum ada catatan untuk nomor ini.", color = colors.textSecondary, fontSize = CallTagType.bodySmall)
                    }
                    result.recentReports.forEach { rep ->
                        ReportNote(tag = rep.tagName, callType = friendlyCallType(rep.callType), comment = rep.comment)
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(text: String, fg: Color, bg: Color) {
    Surface(shape = RoundedCornerShape(CallTagRadius.sm), color = bg) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = CallTagSpacing.sm, vertical = CallTagSpacing.xs),
            color = fg,
            fontSize = CallTagType.label,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ReportNote(tag: String, callType: String, comment: String) {
    val colors = CallTagDesign.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)
    ) {
        Text(text = "#$tag", color = colors.textPrimary, fontSize = CallTagType.body, fontWeight = FontWeight.Bold)
        Text(text = callType, color = colors.textSecondary, fontSize = CallTagType.label)
        if (comment.isNotBlank()) {
            Text(text = comment, color = colors.textSecondary, fontSize = CallTagType.body, lineHeight = 21.sp)
        }
        HorizontalDivider(modifier = Modifier.padding(top = CallTagSpacing.xs), color = colors.borderSubtle)
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
