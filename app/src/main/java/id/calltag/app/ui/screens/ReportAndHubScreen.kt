package id.calltag.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import id.calltag.app.ui.theme.CallTagDesign
import id.calltag.app.ui.theme.CallTagRadius
import id.calltag.app.ui.theme.CallTagSpacing
import id.calltag.app.ui.theme.CallTagTouch
import id.calltag.app.ui.theme.CallTagType
import id.calltag.app.ui.viewmodel.CallTagMainViewModel
import id.calltag.app.ui.viewmodel.UiLoadState

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
    modifier: Modifier = Modifier
) {
    val colors = CallTagDesign.colors
    var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }
    var targetPhone by rememberSaveable { mutableStateOf(prefilledPhone) }

    var appliedPrefillRequest by rememberSaveable { mutableIntStateOf(-1) }
    val draftState = rememberSaveableStateHolder()

    LaunchedEffect(prefilledPhone, prefillRequestId) {
        if (prefilledPhone.isNotBlank() && prefillRequestId != appliedPrefillRequest) {
            appliedPrefillRequest = prefillRequestId
            targetPhone = prefilledPhone
            selectedSubTab = 0
        }
    }

    val subTabs = listOf(
        "Beri #Tag",
        "Kanal resmi",
        "Koreksi"
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
            0 -> AddTagReportSubScreen(
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
    val recentLogs by viewModel.recentScreeningLogs.collectAsState()
    val submissionState by viewModel.reportSubmissionState.collectAsState()

    var selectedCategory by rememberSaveable { mutableStateOf("APK_SNIFFING") }
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
                description = "Bagikan pengalaman Anda agar pengguna lain mengenali modus gangguan."
            )
        }
        item {
            ReportFormSection(title = "Nomor yang dilaporkan") {
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
                if (recentLogs.isNotEmpty()) {
                    Text(
                        text = "Atau pilih panggilan terakhir",
                        color = colors.textSecondary,
                        fontSize = CallTagType.bodySmall
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                    ) {
                        recentLogs.take(4).forEach { log ->
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
        item {
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
            ReportFormSection(title = "Ceritakan kejadiannya") {
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
                    placeholder = { Text("Contoh: Kurir Paket APK") },
                    supportingText = { Text("Jika kosong, nama kategori akan dipakai.") }
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(CallTagRadius.md),
                    label = { Text("Cerita singkat (opsional)") },
                    placeholder = { Text("Contoh: Mengaku kurir dan mengirim file .apk") }
                )
            }
        }
        item {
            Button(
                onClick = {
                    viewModel.submitCommunityReport(
                        phone = phoneInput,
                        categoryCode = selectedCategory,
                        customTag = customTag,
                        comment = comment,
                        callType = callType
                    )
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = CallTagTouch.action),
                shape = RoundedCornerShape(CallTagRadius.md),
                colors = ButtonDefaults.buttonColors(containerColor = colors.brandSolid, contentColor = colors.onBrand)
            ) {
                Text("Simpan #Tag & laporkan", fontSize = CallTagType.subtitle, fontWeight = FontWeight.Bold)
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
                    val humanStatus = CallTagDesign.reputationLabel(pt.reputationLevel)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CallTagRadius.lg),
                        colors = CardDefaults.cardColors(containerColor = colors.cleanEmeraldContainer),
                        border = BorderStroke(1.dp, colors.cleanEmerald)
                    ) {
                        Column(
                            modifier = Modifier.padding(CallTagSpacing.lg),
                            verticalArrangement = Arrangement.spacedBy(CallTagSpacing.sm)
                        ) {
                            Text(
                                text = if (st.data.reportId.contains("_local_")) "#Tag tersimpan di perangkat" else "#Tag berhasil dikirim",
                                color = colors.cleanEmerald,
                                fontSize = CallTagType.subtitle,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${pt.phoneE164} · $humanStatus\nSkor ${pt.spamScore.toInt()}/100",
                                style = CallTagDesign.monoStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            )
                            if (st.data.reportId.contains("_local_")) {
                                Text(
                                    text = "Layanan belum mengonfirmasi penerimaan laporan ini.",
                                    color = colors.textSecondary,
                                    fontSize = CallTagType.bodySmall
                                )
                            }
                            Button(
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

@Composable
private fun NumberDisputeSubScreen(
    viewModel: CallTagMainViewModel
) {
    val colors = CallTagDesign.colors
    val disputeState by viewModel.disputeSubmissionState.collectAsState()

    var phone by rememberSaveable { mutableStateOf("") }
    var ownerName by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    var contactRef by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(CallTagSpacing.md),
        verticalArrangement = Arrangement.spacedBy(CallTagSpacing.lg)
    ) {
        item {
            ScreenIntro(
                eyebrow = "Koreksi nomor",
                title = "Ajukan peninjauan",
                description = "Nomor pribadi atau usaha Anda salah diberi #Tag? Jelaskan penggunaan nomor untuk mengajukan koreksi."
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
                    val isLocalFallback = st.data.submittedAt.startsWith("Tersimpan di Antrean")
                    val statusColor = if (isLocalFallback) colors.suspiciousAmber else colors.brandPrimary
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CallTagRadius.lg),
                        colors = CardDefaults.cardColors(containerColor = if (isLocalFallback) colors.suspiciousAmberContainer else colors.brandContainer),
                        border = BorderStroke(1.dp, statusColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(CallTagSpacing.lg),
                            verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                        ) {
                            Text(
                                text = if (isLocalFallback) "Pengajuan belum terkirim" else "Respons pengajuan koreksi",
                                color = statusColor,
                                fontSize = CallTagType.subtitle,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isLocalFallback) st.data.phoneE164 else "Referensi: ${st.data.disputeId}",
                                style = CallTagDesign.monoStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            )
                            Text(
                                text = if (isLocalFallback) {
                                    "Koneksi tidak tersedia. Kirim kembali saat online; pengajuan belum masuk antrean peninjauan."
                                } else {
                                    "Status layanan: ${st.data.status}. Status ini belum berarti reputasi nomor telah dikoreksi."
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
