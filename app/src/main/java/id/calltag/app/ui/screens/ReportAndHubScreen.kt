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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.calltag.app.data.remote.dto.OfficialChannelDto
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
    modifier: Modifier = Modifier
) {
    val colors = CallTagDesign.colors
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var targetPhone by remember { mutableStateOf(prefilledPhone.ifBlank { "081377665544" }) }

    LaunchedEffect(prefilledPhone) {
        if (prefilledPhone.isNotBlank()) {
            targetPhone = prefilledPhone
            selectedSubTab = 0
        }
    }

    val subTabs = listOf(
        "Beri #Tag",
        "Pengaduan Resmi",
        "Koreksi Nomor"
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
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .background(colors.surfaceSubtle, RoundedCornerShape(CallTagRadius.md))
                    .padding(CallTagSpacing.xxs),
                horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)
            ) {
                subTabs.forEachIndexed { index, title ->
                    val isSelected = selectedSubTab == index
                    val bg = if (isSelected) colors.surfaceCard else Color.Transparent
                    val fg = if (isSelected) colors.brandPrimary else colors.textSecondary

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = CallTagTouch.compact)
                            .clip(RoundedCornerShape(CallTagRadius.md))
                            .background(bg)
                            .clickable { selectedSubTab = index }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = fg,
                            fontSize = CallTagType.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

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

    var selectedCategory by remember { mutableStateOf("APK_SNIFFING") }
    var customTag by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var callType by remember { mutableStateOf("WHATSAPP_CALL") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = CallTagSpacing.md,
            vertical = CallTagSpacing.md
        ),
        verticalArrangement = Arrangement.spacedBy(CallTagSpacing.md)
    ) {
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
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Beri #Tag & Laporkan Nomor",
                            color = colors.textPrimary,
                            fontSize = CallTagType.title,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Bantu pengguna lain mengenali nomor penipu atau pengganggu",
                            color = colors.textSecondary,
                            fontSize = CallTagType.label
                        )
                    }

                    if (recentLogs.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)) {
                            Text(
                                text = "Pilih dari panggilan terakhir:",
                                color = colors.textSecondary,
                                fontSize = CallTagType.label,
                                fontWeight = FontWeight.Medium
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs),
                                verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                            ) {
                                recentLogs.take(4).forEach { log ->
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(CallTagRadius.pill))
                                            .clickable { onPhoneChange(log.phoneE164) },
                                        shape = RoundedCornerShape(CallTagRadius.pill),
                                        color = colors.surfaceSubtle,
                                        border = BorderStroke(1.dp, colors.borderSubtle)
                                    ) {
                                        Text(
                                            text = log.phoneE164,
                                            modifier = Modifier.padding(horizontal = CallTagSpacing.sm, vertical = 6.dp),
                                            style = CallTagDesign.monoStyle(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.brandPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

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
                        label = { Text("Nomor Telepon") },
                        placeholder = { Text("Contoh: 081299887766") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.brandPrimary,
                            unfocusedBorderColor = colors.borderSubtle
                        )
                    )

                    OutlinedTextField(
                        value = customTag,
                        onValueChange = { customTag = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(CallTagRadius.md),
                        label = { Text("Nama #Tag (contoh: Kurir Paket APK)") },
                        placeholder = { Text("Kosongkan untuk memakai nama kategori") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.brandPrimary,
                            unfocusedBorderColor = colors.borderSubtle
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)) {
                        Text(
                            text = "Jenis Gangguan",
                            color = colors.textPrimary,
                            fontSize = CallTagType.bodySmall,
                            fontWeight = FontWeight.Bold
                        )

                        SCAM_CATEGORY_CHOICES.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                            ) {
                                pair.forEach { option ->
                                    val isSelected = selectedCategory == option.code
                                    val borderColor = if (isSelected) colors.brandPrimary else colors.borderSubtle
                                    val bgColor = if (isSelected) colors.brandContainer else colors.backgroundPrimary

                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 68.dp)
                                            .clip(RoundedCornerShape(CallTagRadius.md))
                                            .clickable { selectedCategory = option.code },
                                        shape = RoundedCornerShape(CallTagRadius.md),
                                        colors = CardDefaults.cardColors(containerColor = bgColor),
                                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(CallTagSpacing.sm),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = option.title,
                                                color = if (isSelected) colors.brandPrimary else colors.textPrimary,
                                                fontSize = CallTagType.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = option.example,
                                                color = colors.textSecondary,
                                                fontSize = CallTagType.caption,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)) {
                        Text(
                            text = "Dihubungi Lewat",
                            color = colors.textPrimary,
                            fontSize = CallTagType.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                        ) {
                            CALL_CHANNEL_CHOICES.forEach { (code, label) ->
                                val active = callType == code
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = CallTagTouch.compact)
                                        .clip(RoundedCornerShape(CallTagRadius.md))
                                        .clickable { callType = code },
                                    shape = RoundedCornerShape(CallTagRadius.md),
                                    color = if (active) colors.brandSolid else colors.surfaceSubtle,
                                    border = BorderStroke(
                                        1.dp,
                                        if (active) colors.brandPrimary else colors.borderSubtle
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = CallTagSpacing.xs, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (active) colors.onBrand else colors.textPrimary,
                                            fontSize = CallTagType.label,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(CallTagRadius.md),
                        label = { Text("Cerita Singkat (Opsional)") },
                        placeholder = { Text("Contoh: Mengaku kurir paket dan mengirim file .apk") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.brandPrimary,
                            unfocusedBorderColor = colors.borderSubtle
                        )
                    )

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
                            text = "Simpan #Tag & Laporkan",
                            fontSize = CallTagType.subtitle,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
                                text = "Terima kasih! #Tag berhasil disimpan.",
                                color = colors.cleanEmerald,
                                fontSize = CallTagType.subtitle,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${pt.phoneE164} kini berstatus $humanStatus (Skor ${pt.spamScore.toInt()}/100).",
                                color = colors.textPrimary,
                                fontSize = CallTagType.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
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
    var copyFeedback by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = CallTagSpacing.md,
            vertical = CallTagSpacing.md
        ),
        verticalArrangement = Arrangement.spacedBy(CallTagSpacing.md)
    ) {
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
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Pengaduan Resmi Pemerintah",
                            color = colors.textPrimary,
                            fontSize = CallTagType.title,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Salin nomor pelaku, lalu teruskan ke instansi berwenang bila Anda mengalami kerugian",
                            color = colors.textSecondary,
                            fontSize = CallTagType.label
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(CallTagRadius.md),
                        color = colors.surfaceSubtle,
                        border = BorderStroke(1.dp, colors.borderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = CallTagSpacing.sm, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Nomor Terakhir",
                                    color = colors.textSecondary,
                                    fontSize = CallTagType.caption
                                )
                                Text(
                                    text = perpetratorPhone,
                                    style = CallTagDesign.monoStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.textPrimary
                                    )
                                )
                            }

                            Button(
                                onClick = {
                                    copyToClipboard(context, perpetratorPhone)
                                    copyFeedback = "Nomor berhasil disalin"
                                },
                                modifier = Modifier.heightIn(min = CallTagTouch.compact),
                                shape = RoundedCornerShape(CallTagRadius.md),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.brandSolid,
                                    contentColor = colors.onBrand
                                )
                            ) {
                                Text(
                                    text = "Salin",
                                    fontSize = CallTagType.label,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (copyFeedback.isNotBlank()) {
                        Text(
                            text = copyFeedback,
                            color = colors.cleanEmerald,
                            fontSize = CallTagType.label,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        items(channels, key = { it.channelSlug }) { channel ->
            OfficialChannelCard(
                channel = channel,
                onCopyNumberAndOpenUrl = { url ->
                    copyToClipboard(context, perpetratorPhone)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
            ) {
                Button(
                    onClick = { onCopyNumberAndOpenUrl(channel.actionLinks.websiteUrl) },
                    modifier = Modifier
                        .weight(1f)
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
                            .weight(1f)
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
                            .weight(1f)
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

    var phone by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var contactRef by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = CallTagSpacing.md,
            vertical = CallTagSpacing.md
        ),
        verticalArrangement = Arrangement.spacedBy(CallTagSpacing.md)
    ) {
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
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Ajukan Koreksi Nomor",
                            color = colors.textPrimary,
                            fontSize = CallTagType.title,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Nomor pribadi atau usaha Anda salah diberi #Tag spam? Ajukan peninjauan ulang di sini.",
                            color = colors.textSecondary,
                            fontSize = CallTagType.label,
                            lineHeight = 17.sp
                        )
                    }

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(CallTagRadius.md),
                        textStyle = CallTagDesign.monoStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        label = { Text("Nomor Telepon Anda") },
                        placeholder = { Text("Contoh: 082144556677") }
                    )

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(CallTagRadius.md),
                        label = { Text("Nama Pemilik / Usaha") },
                        placeholder = { Text("Contoh: Toko Logistik Nusantara") }
                    )

                    OutlinedTextField(
                        value = contactRef,
                        onValueChange = { contactRef = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(CallTagRadius.md),
                        label = { Text("Email atau Kontak Verifikasi") },
                        placeholder = { Text("Contoh: admin@usahaanda.id") }
                    )

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(CallTagRadius.md),
                        label = { Text("Keterangan Penggunaan Nomor") },
                        placeholder = { Text("Jelaskan bahwa nomor ini digunakan untuk keperluan resmi...") }
                    )

                    Button(
                        onClick = {
                            viewModel.submitNumberDispute(
                                phone = phone,
                                ownerName = ownerName,
                                reason = reason,
                                contactRef = contactRef
                            )
                        },
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
                            text = "Kirim Pengajuan Koreksi",
                            fontSize = CallTagType.subtitle,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CallTagRadius.lg),
                        colors = CardDefaults.cardColors(containerColor = colors.cleanEmeraldContainer),
                        border = BorderStroke(1.dp, colors.cleanEmerald)
                    ) {
                        Column(
                            modifier = Modifier.padding(CallTagSpacing.lg),
                            verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                        ) {
                            Text(
                                text = "Pengajuan Koreksi Diterima",
                                color = colors.cleanEmerald,
                                fontSize = CallTagType.subtitle,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Nomor Referensi: ${st.data.disputeId} (${st.data.phoneE164})",
                                style = CallTagDesign.monoStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            )
                            Text(
                                text = "Tim kami sedang meninjau kepemilikan atas nama ${st.data.ownerName}.",
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
