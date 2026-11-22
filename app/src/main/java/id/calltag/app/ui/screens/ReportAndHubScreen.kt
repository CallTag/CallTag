package id.calltag.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.calltag.app.data.remote.dto.OfficialChannelDto
import id.calltag.app.ui.components.ScreenIntro
import id.calltag.app.ui.components.AccountAccessGate
import id.calltag.app.ui.theme.CallTagDesign
import id.calltag.app.ui.theme.CallTagRadius
import id.calltag.app.ui.theme.CallTagSpacing
import id.calltag.app.ui.theme.CallTagTouch
import id.calltag.app.ui.theme.CallTagType
import id.calltag.app.ui.viewmodel.CallTagMainViewModel
import id.calltag.app.ui.viewmodel.UiLoadState
import kotlinx.coroutines.launch
import id.calltag.app.util.PhoneNormalizer
import id.calltag.app.work.PendingReportWorker

private data class ScamCategoryChoice(
    val code: String,
    val title: String,
    val example: String
)

private val SCAM_CATEGORY_CHOICES = listOf(
    ScamCategoryChoice(
        code = "APK_SNIFFING",
        title = "Modus File APK",
        example = "Kurir paket, undangan, surat tilang"
    ),
    ScamCategoryChoice(
        code = "PINJOL_DC",
        title = "Pinjol Ilegal & DC",
        example = "Teror tagihan & ancaman sebar data"
    ),
    ScamCategoryChoice(
        code = "IMPERSONATION",
        title = "CS Bank / Instansi Palsu",
        example = "Minta OTP, PIN, atau biaya admin"
    ),
    ScamCategoryChoice(
        code = "JUDOL",
        title = "Promosi Judi Online",
        example = "Pesan atau telepon situs slot"
    ),
    ScamCategoryChoice(
        code = "SOCIAL_ENG",
        title = "Penipuan / Hipnotis",
        example = "Kabar keluarga kecelakaan / hadiah"
    ),
    ScamCategoryChoice(
        code = "TELEMARKETING",
        title = "Spam Promosi",
        example = "Penawaran produk berulang kali"
    )
)

private val CALL_CHANNEL_CHOICES = listOf(
    "CELLULAR_CALL" to "Telepon",
    "WHATSAPP_CALL" to "WA Telepon",
    "WHATSAPP_CHAT" to "WA Chat / SMS"
)

@Composable
fun ReportAndHubScreen(
    viewModel: CallTagMainViewModel,
    prefilledPhone: String = "",
    prefillRequestId: Int = 0,
    modifier: Modifier = Modifier,
    onNavigateToAccount: () -> Unit = {}
) {
    val colors = CallTagDesign.colors
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }
    var targetPhone by rememberSaveable { mutableStateOf(prefilledPhone) }

    var appliedPrefillRequest by rememberSaveable { mutableIntStateOf(-1) }
    val draftState = rememberSaveableStateHolder()

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            draftState.removeState(0)
            targetPhone = ""
        }
    }

    LaunchedEffect(prefilledPhone, prefillRequestId) {
        if (prefilledPhone.isNotBlank() && prefillRequestId != appliedPrefillRequest) {
            appliedPrefillRequest = prefillRequestId
            targetPhone = prefilledPhone
            viewModel.resetReportState()
            selectedSubTab = 0
        }
    }

    val subTabs = listOf(
        "Laporkan nomor",
        "Kanal resmi",
        "Koreksi nomor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundPrimary)
    ) {
        // Segmented Pill Bar yang Bersih & Modern
        Surface(
            color = colors.surfaceCard,
            border = BorderStroke(1.dp, colors.borderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CallTagSpacing.md, vertical = CallTagSpacing.xs)
                    .background(colors.surfaceSubtle, RoundedCornerShape(CallTagRadius.md))
                    .padding(CallTagSpacing.xxs)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)
            ) {
                subTabs.forEachIndexed { index, title ->
                    val isSelected = selectedSubTab == index
                    val bg = if (isSelected) colors.surfaceCard else Color.Transparent
                    val fg = if (isSelected) colors.brandPrimary else colors.textSecondary

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = CallTagTouch.action)
                            .clip(RoundedCornerShape(CallTagRadius.md))
                            .background(bg)
                            .selectable(selected = isSelected, role = Role.Tab, onClick = { selectedSubTab = index })
                            .padding(horizontal = CallTagSpacing.xxs, vertical = CallTagSpacing.xs),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = fg,
                            fontSize = CallTagType.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        draftState.SaveableStateProvider(selectedSubTab) {
        when (selectedSubTab) {
            0 -> if (!isLoggedIn) {
                AccountAccessGate(onOpenAccount = onNavigateToAccount, modifier = Modifier.padding(CallTagSpacing.md))
            } else AddTagReportSubScreen(
                viewModel = viewModel,
                phoneInput = targetPhone,
                onPhoneChange = { targetPhone = it },
                onSwitchToOfficialHub = { selectedSubTab = 1 }
            )
            1 -> OfficialReportingHubSubScreen(
                viewModel = viewModel
            )
            2 -> NumberDisputeSubScreen(
                viewModel = viewModel
            )
        }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTagReportSubScreen(
    viewModel: CallTagMainViewModel,
    phoneInput: String,
    onPhoneChange: (String) -> Unit,
    onSwitchToOfficialHub: () -> Unit
) {
    val colors = CallTagDesign.colors
    val context = LocalContext.current
    val recentLogs by viewModel.recentScreeningLogs.collectAsState(initial = emptyList())
    val pendingReports by viewModel.pendingReports.collectAsState(initial = emptyList())
    val submissionState by viewModel.reportSubmissionState.collectAsState()

    LaunchedEffect(submissionState) {
        val st = submissionState
        if (st is UiLoadState.Success && st.data.reportId.startsWith("PENDING:")) {
            PendingReportWorker.enqueue(context)
        }
    }

    var selectedCategory by rememberSaveable { mutableStateOf("APK_SNIFFING") }
    var isInfoTag by rememberSaveable { mutableStateOf(false) }
    var customTag by rememberSaveable { mutableStateOf("") }
    var comment by rememberSaveable { mutableStateOf("") }
    var callType by rememberSaveable { mutableStateOf("WHATSAPP_CALL") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(CallTagSpacing.md),
        verticalArrangement = Arrangement.spacedBy(CallTagSpacing.lg)
    ) {
        item {
            ScreenIntro(
                eyebrow = "Laporan komunitas",
                title = "Beri #Tag nomor",
                description = "Tambahkan konteks layanan atau laporkan gangguan berdasarkan pengalaman Anda."
            )
        }
        item {
            ReportFormSection(title = "Jenis #Tag") {
                listOf(false to "Laporkan Gangguan/Penipuan", true to "Beri #Tag Info Umum").forEach { (info, title) ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().heightIn(min = CallTagTouch.action)
                            .selectable(selected = isInfoTag == info, role = Role.RadioButton, onClick = { isInfoTag = info }),
                        shape = RoundedCornerShape(CallTagRadius.md),
                        color = if (isInfoTag == info) colors.brandContainer else colors.surfaceSubtle
                    ) {
                        Row(Modifier.padding(CallTagSpacing.sm), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)) {
                            RadioButton(selected = isInfoTag == info, onClick = null)
                            Text(title, color = colors.textPrimary, fontSize = CallTagType.body, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (isInfoTag) Text("Tag layanan/peran tidak menaikkan skor spam. Hindari nama pribadi, nomor rekening, dan isi buku kontak.",
                    color = colors.textSecondary, fontSize = CallTagType.bodySmall)
            }
        }
        item {
            ReportFormSection(title = "Nomor yang diberi #Tag") {
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = onPhoneChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(CallTagRadius.md),
                    textStyle = CallTagDesign.monoStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    label = { Text("Nomor telepon") },
                    placeholder = { Text("Contoh: 081299887766") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.brandPrimary,
                        unfocusedBorderColor = colors.borderSubtle
                    )
                )
                val unmaskedLogs = recentLogs.filter { !it.phoneE164.contains('*') }.take(4)
                if (unmaskedLogs.isNotEmpty()) {
                    Text(
                        text = "Atau pilih panggilan terakhir",
                        color = colors.textSecondary,
                        fontSize = CallTagType.bodySmall
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                    ) {
                        unmaskedLogs.forEach { log ->
                            Surface(
                                modifier = Modifier
                                    .heightIn(min = CallTagTouch.action)
                                    .clip(RoundedCornerShape(CallTagRadius.md))
                                    .clickable { onPhoneChange(log.phoneE164) },
                                shape = RoundedCornerShape(CallTagRadius.md),
                                color = colors.surfaceSubtle,
                                border = BorderStroke(1.dp, colors.borderSubtle)
                            ) {
                                Box(
                                    modifier = Modifier.padding(CallTagSpacing.sm),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = log.phoneE164,
                                        style = CallTagDesign.monoStyle(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.brandPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!isInfoTag) item {
            Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.sm)) {
                ReportSectionTitle("Pilih jenis gangguan")
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                ) {
                    SCAM_CATEGORY_CHOICES.forEach { option ->
                        val selected = selectedCategory == option.code
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = CallTagTouch.action)
                                .clip(RoundedCornerShape(CallTagRadius.md))
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { selectedCategory = option.code }
                                ),
                            shape = RoundedCornerShape(CallTagRadius.md),
                            color = if (selected) colors.brandContainer else colors.surfaceCard,
                            border = BorderStroke(1.dp, if (selected) colors.brandPrimary else colors.borderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(CallTagSpacing.sm),
                                horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selected, onClick = null)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)
                                ) {
                                    Text(
                                        text = option.title,
                                        color = if (selected) colors.brandPrimary else colors.textPrimary,
                                        fontSize = CallTagType.body,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = option.example,
                                        color = colors.textSecondary,
                                        fontSize = CallTagType.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            ReportFormSection(title = if (isInfoTag) "Konteks layanan" else "Ceritakan kejadiannya") {
                Text(
                    text = "Dihubungi lewat",
                    color = colors.textSecondary,
                    fontSize = CallTagType.bodySmall
                )
                FlowRow(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                ) {
                    CALL_CHANNEL_CHOICES.forEach { (code, label) ->
                        val selected = callType == code
                        Surface(
                            modifier = Modifier
                                .heightIn(min = CallTagTouch.action)
                                .clip(RoundedCornerShape(CallTagRadius.md))
                                .selectable(selected = selected, role = Role.RadioButton, onClick = { callType = code }),
                            shape = RoundedCornerShape(CallTagRadius.md),
                            color = if (selected) colors.brandSolid else colors.surfaceSubtle,
                            border = BorderStroke(1.dp, if (selected) colors.brandSolid else colors.borderSubtle)
                        ) {
                            Box(
                                modifier = Modifier.padding(CallTagSpacing.sm),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) colors.onBrand else colors.textPrimary,
                                    fontSize = CallTagType.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = customTag,
                    onValueChange = { customTag = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(CallTagRadius.md),
                    label = { Text("Nama #Tag (opsional)") },
                    placeholder = { Text(if (isInfoTag) "Contoh: Kurir Ekspedisi" else "Contoh: Kurir Paket APK") },
                    supportingText = { Text("Jika kosong, nama kategori akan dipakai.") }
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(CallTagRadius.md),
                    label = { Text("Cerita singkat (opsional)") },
                    placeholder = { Text(if (isInfoTag) "Contoh: Layanan pengiriman paket" else "Contoh: Mengaku kurir dan mengirim file .apk") }
                )
            }
        }
        item {
            Button(
                onClick = {
                    viewModel.submitCommunityReport(
                        phone = phoneInput,
                        categoryCode = if (isInfoTag) "INFO_TAG" else selectedCategory,
                        customTag = customTag,
                        comment = comment,
                        callType = callType
                    )
                },
                enabled = submissionState !is UiLoadState.Loading && phoneInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = CallTagTouch.action),
                shape = RoundedCornerShape(CallTagRadius.md),
                colors = ButtonDefaults.buttonColors(containerColor = colors.brandSolid, contentColor = colors.onBrand)
            ) {
                Text(if (isInfoTag) "Simpan #Tag Info" else "Simpan #Tag & laporkan", fontSize = CallTagType.subtitle, fontWeight = FontWeight.Bold)
            }
        }

        // Status Pengiriman Laporan
        item {
            when (val st = submissionState) {
                is UiLoadState.Idle -> Unit
                is UiLoadState.Loading -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CallTagRadius.lg),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                        border = BorderStroke(1.dp, colors.borderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(CallTagSpacing.lg),
                            horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = colors.brandPrimary,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                text = "Menyimpan #Tag komunitas...",
                                color = colors.textPrimary,
                                fontSize = CallTagType.bodySmall
                            )
                        }
                    }
                }
                is UiLoadState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CallTagRadius.lg),
                        colors = CardDefaults.cardColors(containerColor = colors.dangerScamContainer),
                        border = BorderStroke(1.dp, colors.dangerScam)
                    ) {
                        Text(
                            text = st.message,
                            modifier = Modifier.padding(CallTagSpacing.md),
                            color = colors.dangerScam,
                            fontSize = CallTagType.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                is UiLoadState.Success -> {
                    val pt = st.data.phoneTag
                    val isQueuedOffline = st.data.reportId.startsWith("PENDING:")
                    val humanStatus = if (isQueuedOffline) "Antrean Offline" else CallTagDesign.reputationLabel(pt.reputationLevel)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CallTagRadius.lg),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isQueuedOffline) colors.suspiciousAmberContainer else colors.cleanEmeraldContainer
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isQueuedOffline) colors.suspiciousAmber else colors.cleanEmerald
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(CallTagSpacing.lg),
                            verticalArrangement = Arrangement.spacedBy(CallTagSpacing.sm)
                        ) {
                            Text(
                                text = if (isQueuedOffline) "Laporan masuk antrean offline" else "#Tag berhasil dikirim",
                                color = if (isQueuedOffline) colors.suspiciousAmber else colors.cleanEmerald,
                                fontSize = CallTagType.subtitle,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${PhoneNormalizer.maskPhone(pt.phoneE164)} · $humanStatus",
                                style = CallTagDesign.monoStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            )
                            Text(
                                text = if (isQueuedOffline) {
                                    "Skor risiko lokal tidak berubah. Laporan dikirim otomatis saat koneksi tersedia."
                                } else if (pt.primaryCategory == "INFO_TAG") {
                                    "Tag konteks tersimpan. Skor spam tidak berubah."
                                } else {
                                    "Skor agregat server: ${pt.spamScore.toInt()}/100 (${pt.uniqueReporters} pelapor)"
                                },
                                color = colors.textSecondary,
                                fontSize = CallTagType.bodySmall
                            )
                            if (pt.primaryCategory != "INFO_TAG") Button(
                                onClick = onSwitchToOfficialHub,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = CallTagTouch.action),
                                shape = RoundedCornerShape(CallTagRadius.md),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.brandSolid,
                                    contentColor = colors.onBrand
                                )
                            ) {
                                Text(
                                    text = "Lanjut ke Kanal Pengaduan Resmi",
                                    fontSize = CallTagType.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
        if (pendingReports.isNotEmpty()) {
            item {
                ReportSectionTitle("Antrean Laporan Offline (Outbox)")
            }
            items(pendingReports, key = { it.localId }) { item ->
                val statusColor = when (item.status) {
                    "SENT" -> colors.cleanEmerald
                    "FAILED" -> colors.dangerScam
                    else -> colors.suspiciousAmber
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CallTagRadius.md),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                    border = BorderStroke(1.dp, colors.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(CallTagSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = PhoneNormalizer.maskPhone(item.phoneE164),
                                style = CallTagDesign.monoStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            )
                            Text(
                                text = item.status,
                                color = statusColor,
                                fontSize = CallTagType.caption,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = "#${item.customTag} (${item.categoryCode})",
                            color = colors.textSecondary,
                            fontSize = CallTagType.bodySmall
                        )
                        if (item.lastError.isNotBlank()) {
                            Text(
                                text = item.lastError,
                                color = colors.textSecondary,
                                fontSize = CallTagType.label
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                        ) {
                            if (item.status != "SENT") {
                                OutlinedButton(
                                    onClick = { viewModel.retryPendingReport(item.localId) },
                                    enabled = !item.lastError.startsWith("Laporan lama belum memiliki pemilik sesi"),
                                    modifier = Modifier.weight(1f).heightIn(min = CallTagTouch.compact),
                                    shape = RoundedCornerShape(CallTagRadius.md)
                                ) {
                                    Text("Kirim ulang", fontSize = CallTagType.bodySmall)
                                }
                            }
                            OutlinedButton(
                                onClick = { viewModel.deletePendingReport(item.localId) },
                                modifier = Modifier.weight(1f).heightIn(min = CallTagTouch.compact),
                                shape = RoundedCornerShape(CallTagRadius.md)
                            ) {
                                Text("Hapus", fontSize = CallTagType.bodySmall, color = colors.dangerScam)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfficialReportingHubSubScreen(
    viewModel: CallTagMainViewModel
) {
    val context = LocalContext.current
    val colors = CallTagDesign.colors
    val channels by viewModel.officialChannels.collectAsState()
    val perpetratorPhone by viewModel.lastReportedPhoneForCopy.collectAsState()
    var copyFeedback by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(CallTagSpacing.md),
        verticalArrangement = Arrangement.spacedBy(CallTagSpacing.lg)
    ) {
        item {
            ScreenIntro(
                eyebrow = "Kanal resmi",
                title = "Lanjutkan pengaduan",
                description = "Hubungi instansi berwenang bila Anda mengalami kerugian. Siapkan bukti sebelum membuka kanal pengaduan."
            )
        }
        if (perpetratorPhone.isNotBlank()) {
            item {
                ReportFormSection(title = "Nomor terakhir dilaporkan") {
                    Text(
                        text = perpetratorPhone,
                        style = CallTagDesign.monoStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    )
                    OutlinedButton(
                        onClick = {
                            copyToClipboard(context, perpetratorPhone)
                            copyFeedback = "Nomor berhasil disalin"
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = CallTagTouch.action),
                        shape = RoundedCornerShape(CallTagRadius.md)
                    ) {
                        Text("Salin nomor", fontSize = CallTagType.body, fontWeight = FontWeight.Bold)
                    }
                    if (copyFeedback.isNotBlank()) {
                        Text(copyFeedback, color = colors.cleanEmerald, fontSize = CallTagType.bodySmall)
                    }
                }
            }
        }

        items(channels, key = { it.channelSlug }) { channel ->
            OfficialChannelCard(
                channel = channel,
                onCopyNumberAndOpenUrl = { url ->
                    if (perpetratorPhone.isNotBlank()) copyToClipboard(context, perpetratorPhone)
                    openExternalUrl(context, url)
                },
                onDialAuthority = { dial ->
                    openDialer(context, dial)
                }
            )
        }
    }
}

@Composable
private fun OfficialChannelCard(
    channel: OfficialChannelDto,
    onCopyNumberAndOpenUrl: (String) -> Unit,
    onDialAuthority: (String) -> Unit
) {
    val colors = CallTagDesign.colors

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
                    text = channel.authorityName,
                    color = colors.brandPrimary,
                    fontSize = CallTagType.label,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = channel.title,
                    color = colors.textPrimary,
                    fontSize = CallTagType.title,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = channel.description,
                    color = colors.textSecondary,
                    fontSize = CallTagType.bodySmall,
                    lineHeight = 18.sp
                )
            }

            if (channel.requiredEvidenceChecklist.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.backgroundPrimary, RoundedCornerShape(CallTagRadius.md))
                        .padding(CallTagSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)
                ) {
                    Text(
                        text = "Siapkan Bukti Berikut:",
                        color = colors.textPrimary,
                        fontSize = CallTagType.label,
                        fontWeight = FontWeight.Bold
                    )
                    channel.requiredEvidenceChecklist.forEach { item ->
                        Text(
                            text = "• $item",
                            color = colors.textSecondary,
                            fontSize = CallTagType.label
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
            ) {
                Button(
                    onClick = { onCopyNumberAndOpenUrl(channel.actionLinks.websiteUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = CallTagTouch.action),
                    shape = RoundedCornerShape(CallTagRadius.md),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.brandSolid,
                        contentColor = colors.onBrand
                    )
                ) {
                    Text(
                        text = "Buka Portal",
                        fontSize = CallTagType.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!channel.actionLinks.whatsappUrl.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = { onCopyNumberAndOpenUrl(channel.actionLinks.whatsappUrl) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = CallTagTouch.action),
                        shape = RoundedCornerShape(CallTagRadius.md),
                        border = BorderStroke(1.dp, colors.cleanEmerald)
                    ) {
                        Text(
                            text = "WhatsApp OJK",
                            color = colors.cleanEmerald,
                            fontSize = CallTagType.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (!channel.actionLinks.phoneDial.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = { onDialAuthority(channel.actionLinks.phoneDial) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = CallTagTouch.action),
                        shape = RoundedCornerShape(CallTagRadius.md),
                        border = BorderStroke(1.dp, colors.borderSubtle)
                    ) {
                        Text(
                            text = "Telepon ${channel.actionLinks.phoneDial}",
                            style = CallTagDesign.monoStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

private enum class DisputePage(val title: String) {
    Overview("Koreksi nomor"),
    Submit("Ajukan koreksi"),
    Track("Lacak pengajuan")
}

@Composable
private fun NumberDisputeSubScreen(
    viewModel: CallTagMainViewModel
) {
    val colors = CallTagDesign.colors
    val disputeState by viewModel.disputeSubmissionState.collectAsState()
    val pendingDisputes by viewModel.pendingDisputes.collectAsState(initial = emptyList())
    val disputeLookupState by viewModel.disputeLookupState.collectAsState()

    var phone by rememberSaveable { mutableStateOf("") }
    var ownerName by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    var contactRef by rememberSaveable { mutableStateOf("") }
    var trackDisputeId by rememberSaveable { mutableStateOf("") }
    var trackToken by rememberSaveable { mutableStateOf("") }

    var page by rememberSaveable { mutableStateOf(DisputePage.Overview) }
    val goBack: () -> Unit = { page = DisputePage.Overview }
    BackHandler(enabled = page != DisputePage.Overview, onBack = goBack)

    key(page) {
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(CallTagSpacing.md),
            verticalArrangement = Arrangement.spacedBy(CallTagSpacing.lg)
        ) {
            item {
                SettingsHeading(page.title, if (page == DisputePage.Overview) null else goBack)
            }
            when (page) {
                DisputePage.Overview -> item {
                    SettingsMenu {
                        SettingsMenuRow(DisputePage.Submit.title, "Laporkan #Tag yang keliru pada nomor Anda") {
                            page = DisputePage.Submit
                        }
                        HorizontalDivider(color = colors.borderSubtle)
                        SettingsMenuRow(DisputePage.Track.title, "Cek status dengan ID sanggahan") {
                            page = DisputePage.Track
                        }
                    }
                }
                DisputePage.Submit -> {
                    item {
                        Text(
                            "Jelaskan penggunaan nomor pribadi atau usaha yang salah diberi #Tag.",
                            color = colors.textSecondary,
                            fontSize = CallTagType.body
                        )
                    }
                    item {
                        ReportFormSection(title = "Identitas pemilik") {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(CallTagRadius.md),
                                textStyle = CallTagDesign.monoStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                label = { Text("Nomor telepon Anda") },
                                placeholder = { Text("Contoh: 082144556677") }
                            )
                            OutlinedTextField(
                                value = ownerName,
                                onValueChange = { ownerName = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(CallTagRadius.md),
                                label = { Text("Nama pemilik / usaha") },
                                placeholder = { Text("Contoh: Toko Logistik Nusantara") }
                            )
                            OutlinedTextField(
                                value = contactRef,
                                onValueChange = { contactRef = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(CallTagRadius.md),
                                label = { Text("Kontak verifikasi") },
                                placeholder = { Text("Email atau nomor yang bisa dihubungi") }
                            )
                        }
                    }
                    item {
                        ReportFormSection(title = "Alasan koreksi") {
                            OutlinedTextField(
                                value = reason,
                                onValueChange = { reason = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                shape = RoundedCornerShape(CallTagRadius.md),
                                label = { Text("Penggunaan nomor") },
                                placeholder = { Text("Jelaskan keperluan penggunaan nomor ini…") }
                            )
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                viewModel.submitNumberDispute(
                                    phone = phone,
                                    ownerName = ownerName,
                                    reason = reason,
                                    contactRef = contactRef
                                )
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = CallTagTouch.action),
                            shape = RoundedCornerShape(CallTagRadius.md),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.brandSolid, contentColor = colors.onBrand)
                        ) {
                            Text("Kirim pengajuan koreksi", fontSize = CallTagType.subtitle, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        when (val st = disputeState) {
                            is UiLoadState.Idle -> Unit
                            is UiLoadState.Loading -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(CallTagRadius.lg),
                                    colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                                    border = BorderStroke(1.dp, colors.borderSubtle)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(CallTagSpacing.md),
                                        horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.sm),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            color = colors.brandPrimary,
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                        Text(
                                            text = "Mengirim pengajuan koreksi...",
                                            color = colors.textPrimary,
                                            fontSize = CallTagType.bodySmall
                                        )
                                    }
                                }
                            }
                            is UiLoadState.Error -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(CallTagRadius.lg),
                                    colors = CardDefaults.cardColors(containerColor = colors.dangerScamContainer),
                                    border = BorderStroke(1.dp, colors.dangerScam)
                                ) {
                                    Text(
                                        text = st.message,
                                        modifier = Modifier.padding(CallTagSpacing.md),
                                        color = colors.dangerScam,
                                        fontSize = CallTagType.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            is UiLoadState.Success -> {
                                val isLocalQueue = st.data.status == "PENDING"
                                val statusColor = if (isLocalQueue) colors.suspiciousAmber else colors.brandPrimary
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(CallTagRadius.lg),
                                    colors = CardDefaults.cardColors(containerColor = if (isLocalQueue) colors.suspiciousAmberContainer else colors.brandContainer),
                                    border = BorderStroke(1.dp, statusColor)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(CallTagSpacing.lg),
                                        verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                                    ) {
                                        Text(
                                            text = if (isLocalQueue) "Pengajuan masuk antrean offline" else "Sanggahan tercatat di server",
                                            color = statusColor,
                                            fontSize = CallTagType.subtitle,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "ID Sanggahan: ${st.data.disputeId}",
                                            style = CallTagDesign.monoStyle(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                        )
                                        if (st.data.trackingToken.isNotBlank()) {
                                            Text(
                                                text = "Tracking Token: ${st.data.trackingToken}",
                                                style = CallTagDesign.monoStyle(
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.brandPrimary
                                                )
                                            )
                                        }
                                        Text(
                                            text = if (isLocalQueue) {
                                                "Koneksi tidak tersedia. Pengajuan akan dikirim ulang otomatis saat online."
                                            } else {
                                                "Status: ${st.data.status}. Simpan Tracking Token di atas untuk mengecek hasil peninjauan."
                                            },
                                            color = colors.textSecondary,
                                            fontSize = CallTagType.label
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                DisputePage.Track -> {
                    item {
                        ReportFormSection(title = "Informasi pengajuan") {
                            OutlinedTextField(
                                value = trackDisputeId,
                                onValueChange = { trackDisputeId = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(CallTagRadius.md),
                                label = { Text("ID Sanggahan") },
                                placeholder = { Text("Contoh: DSP_674...") }
                            )
                            OutlinedTextField(
                                value = trackToken,
                                onValueChange = { trackToken = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(CallTagRadius.md),
                                label = { Text("Token pelacak (opsional)") },
                                placeholder = { Text("Token pelacak 32 karakter") }
                            )
                            Button(
                                onClick = { viewModel.checkDisputeStatus(trackDisputeId, trackToken) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = CallTagTouch.action),
                                shape = RoundedCornerShape(CallTagRadius.md),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.brandSolid, contentColor = colors.onBrand)
                            ) {
                                Text("Periksa Status Sanggahan", fontSize = CallTagType.body, fontWeight = FontWeight.Bold)
                            }
                            when (val ls = disputeLookupState) {
                                is UiLoadState.Idle -> Unit
                                is UiLoadState.Loading -> Text("Memeriksa status sanggahan...", color = colors.textSecondary, fontSize = CallTagType.bodySmall)
                                is UiLoadState.Error -> Text(ls.message, color = colors.dangerScam, fontSize = CallTagType.bodySmall, fontWeight = FontWeight.SemiBold)
                                is UiLoadState.Success -> {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(CallTagRadius.md),
                                        color = colors.surfaceSubtle,
                                        border = BorderStroke(1.dp, colors.borderSubtle)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(CallTagSpacing.md),
                                            verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)
                                        ) {
                                            Text(
                                                text = "${ls.data.disputeId} · Status: ${ls.data.status}",
                                                style = CallTagDesign.monoStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.brandPrimary)
                                            )
                                            if (ls.data.resolutionNotes.isNotBlank()) {
                                                Text(
                                                    text = "Catatan moderator: ${ls.data.resolutionNotes}",
                                                    color = colors.textPrimary,
                                                    fontSize = CallTagType.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (pendingDisputes.isNotEmpty()) {
                        item {
                            ReportSectionTitle("Riwayat Sanggahan Perangkat")
                        }
                        items(pendingDisputes, key = { it.localId }) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(CallTagRadius.md),
                                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                                border = BorderStroke(1.dp, colors.borderSubtle)
                            ) {
                                Column(
                                    modifier = Modifier.padding(CallTagSpacing.md),
                                    verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                                ) {
                                    Text(
                                        text = "${PhoneNormalizer.maskPhone(item.phoneE164)} · ${item.status}",
                                        style = CallTagDesign.monoStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    )
                                    if (item.disputeId.isNotBlank()) {
                                        Text(
                                            text = "ID: ${item.disputeId}",
                                            style = CallTagDesign.monoStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)
                                        )
                                    }
                                    if (item.trackingToken.isNotBlank()) {
                                        Text(
                                            text = "Token: ${item.trackingToken}",
                                            style = CallTagDesign.monoStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)
                                        )
                                    }
                                    if (item.disputeId.isNotBlank() && !item.disputeId.startsWith("PENDING_")) {
                                        OutlinedButton(
                                            onClick = {
                                                trackDisputeId = item.disputeId
                                                trackToken = item.trackingToken
                                                viewModel.checkDisputeStatus(item.disputeId, item.trackingToken)
                                                scope.launch { listState.scrollToItem(0) }
                                            },
                                            modifier = Modifier.fillMaxWidth().heightIn(min = CallTagTouch.compact),
                                            shape = RoundedCornerShape(CallTagRadius.md)
                                        ) {
                                            Text("Cek Status Terkini", fontSize = CallTagType.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportSectionTitle(title: String) {
    Text(
        text = title,
        color = CallTagDesign.colors.textPrimary,
        fontSize = CallTagType.title,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ReportFormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = CallTagDesign.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CallTagRadius.lg),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(CallTagSpacing.md),
            verticalArrangement = Arrangement.spacedBy(CallTagSpacing.sm)
        ) {
            ReportSectionTitle(title)
            content()
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText("Nomor Telepon CallTag", text)
    clipboard?.setPrimaryClip(clip)
}

private fun openExternalUrl(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

private fun openDialer(context: Context, number: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${number.trim()}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
