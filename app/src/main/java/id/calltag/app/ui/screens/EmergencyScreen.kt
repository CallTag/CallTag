package id.calltag.app.ui.screens

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.calltag.app.data.local.entity.EmergencyContact
import id.calltag.app.ui.components.ScreenIntro
import id.calltag.app.ui.theme.CallTagDesign
import id.calltag.app.ui.theme.CallTagRadius
import id.calltag.app.ui.theme.CallTagSpacing
import id.calltag.app.ui.theme.CallTagTouch
import id.calltag.app.ui.theme.CallTagType
import id.calltag.app.ui.viewmodel.CallTagMainViewModel

/**
 * Layar direktori darurat: panggilan cepat nasional, filter posko daerah, dan daftar posko.
 *
 * Hierarki mengikuti `DESIGN.md` Bagian 4: tombol darurat nasional adalah elemen paling
 * besar di layar (minimal 64dp) karena dipakai dalam kondisi panik, sedangkan tombol posko
 * daerah memakai target 56dp standar. Seluruh nomor telepon memakai `FontFamily.Monospace`
 * agar tidak salah baca.
 */
@Composable
fun EmergencyScreen(
    viewModel: CallTagMainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CallTagDesign.colors

    val nationalNumbers by viewModel.nationalEmergencyContacts.collectAsState()
    val localDirectory by viewModel.filteredLocalEmergencyContacts.collectAsState()
    val selectedProvince by viewModel.selectedProvince.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val selectedAgencyType by viewModel.selectedAgencyType.collectAsState()

    val cityOptions = listOf(
        "" to "Semua Kota",
        "Jakarta Pusat" to "Jakarta",
        "Kota Bandung" to "Bandung",
        "Kota Surabaya" to "Surabaya",
        "Kota Semarang" to "Semarang",
        "Kota Yogyakarta" to "Yogyakarta"
    )

    val agencyOptions = listOf(
        "" to "Semua Posko",
        "DAMKAR" to "Damkar",
        "POLISI" to "Kepolisian",
        "AMBULANS_RS" to "RS & IGD"
    )

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
            ScreenIntro(
                eyebrow = "Direktori darurat",
                title = "Bantuan saat dibutuhkan.",
                description = "Pilih layanan untuk membuka nomor di aplikasi telepon."
            )
        }

        // 1. Panggilan cepat nomor darurat nasional.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)) {
                nationalNumbers.firstOrNull { it.shortCode == "112" }?.let { contact ->
                    NationalHotlineButton(
                        contact = contact,
                        onClick = { dialNumber(context, contact.primaryPhone) },
                        modifier = Modifier.fillMaxWidth(),
                        prominent = true
                    )
                }
                nationalNumbers.filterNot { it.shortCode == "112" }.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
                    ) {
                        rowItems.forEach { contact ->
                            NationalHotlineButton(
                                contact = contact,
                                onClick = { dialNumber(context, contact.primaryPhone) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) Box(modifier = Modifier.weight(1f))
                    }
                }
                // Lencana status offline — direktori tetap bisa dibaca tanpa internet.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = colors.brandPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Direktori tersedia offline",
                        color = colors.textSecondary,
                        fontSize = CallTagType.label
                    )
                }
            }
        }

        // 2. Filter posko daerah (Damkar, Kepolisian, IGD RS).
        item {
            Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.sm)) {
                Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)) {
                    Text(
                        text = "Cari posko daerah",
                        color = colors.textPrimary,
                        fontSize = CallTagType.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Telepon atau WhatsApp layanan di kota Anda.",
                        color = colors.textSecondary,
                        fontSize = CallTagType.bodySmall
                    )
                }
                Text("Kota", color = colors.textPrimary, fontSize = CallTagType.label, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)) {
                    items(cityOptions) { (cityKey, cityLabel) ->
                        FilterChipPill(
                            label = cityLabel,
                            selected = selectedCity == cityKey,
                            onClick = {
                                viewModel.setEmergencyFilter(
                                    province = selectedProvince,
                                    city = cityKey,
                                    agencyType = selectedAgencyType
                                )
                            }
                        )
                    }
                }
                Text("Jenis layanan", color = colors.textPrimary, fontSize = CallTagType.label, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)) {
                    items(agencyOptions) { (agencyKey, agencyLabel) ->
                        FilterChipPill(
                            label = agencyLabel,
                            selected = selectedAgencyType == agencyKey,
                            onClick = {
                                viewModel.setEmergencyFilter(
                                    province = selectedProvince,
                                    city = selectedCity,
                                    agencyType = agencyKey
                                )
                            }
                        )
                    }
                }
                Text(
                    text = "${localDirectory.size} posko dalam direktori",
                    color = colors.textSecondary,
                    fontSize = CallTagType.label
                )
            }
        }

        // 3. Daftar posko daerah.
        if (localDirectory.isEmpty()) {
            item { EmptyDirectoryCard() }
        } else {
            items(localDirectory, key = { it.code }) { localContact ->
                LocalEmergencyAgencyCard(
                    contact = localContact,
                    onDialLocal = { dialNumber(context, localContact.primaryPhone) },
                    onOpenWhatsApp = {
                        openWhatsAppEmergency(
                            context = context,
                            whatsappNumber = localContact.whatsappNumber,
                            template = localContact.whatsappTemplate
                        )
                    }
                )
            }
        }
    }
}

/**
 * Tombol nomor darurat nasional. Tinggi minimal 64dp sesuai `DESIGN.md` Bagian 4
 * agar mudah ditekan ibu jari dalam kondisi panik. Nomor tampil monospace besar.
 */
@Composable
private fun NationalHotlineButton(
    contact: EmergencyContact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false
) {
    val colors = CallTagDesign.colors
    val isUrgentRed = contact.shortCode in listOf("112", "113", "119")
    // Latar tombol memakai token *Solid, bukan warna teks semantik: warna teks
    // dibuat terang di mode gelap agar terbaca, sehingga tidak layak jadi latar.
    val bgColor = if (isUrgentRed) colors.dangerSolid else colors.surfaceCard
    val fgColor = if (isUrgentRed) Color.White else colors.textPrimary
    val shortTitle = when (contact.shortCode) {
        "112" -> "Darurat Terpadu"
        "110" -> "Kepolisian"
        "113" -> "Damkar"
        "119" -> "Ambulans"
        "115" -> "SAR / Bencana"
        "123" -> "Gangguan PLN"
        "14080" -> "Jalan tol"
        else -> contact.name.substringBefore("(").trim()
    }

    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = CallTagTouch.emergency),
        shape = RoundedCornerShape(CallTagRadius.md),
        border = if (isUrgentRed) null else BorderStroke(1.dp, colors.borderSubtle),
        contentPadding = PaddingValues(
            horizontal = CallTagSpacing.sm,
            vertical = CallTagSpacing.xs
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            contentColor = fgColor
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (contact.shortCode == "119") "118/119" else contact.shortCode,
                    style = CallTagDesign.monoStyle(
                        fontSize = if (prominent) 30.sp else 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = fgColor
                    )
                )
                Text(
                    text = shortTitle,
                    color = fgColor.copy(alpha = 0.92f),
                    fontSize = if (prominent) CallTagType.body else CallTagType.label,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                tint = fgColor.copy(alpha = 0.9f),
                modifier = Modifier.size(if (prominent) 26.dp else 18.dp)
            )
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = CallTagDesign.colors
    val bg = if (selected) colors.brandSolid else colors.surfaceCard
    val fg = if (selected) colors.onBrand else colors.textPrimary

    Surface(
        modifier = Modifier
            .heightIn(min = CallTagTouch.action)
            .clip(RoundedCornerShape(CallTagRadius.pill))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CallTagRadius.pill),
        color = bg,
        border = BorderStroke(1.dp, if (selected) colors.brandPrimary else colors.borderSubtle)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = CallTagSpacing.md, vertical = CallTagSpacing.xs),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = fg,
                fontSize = CallTagType.label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocalEmergencyAgencyCard(
    contact: EmergencyContact,
    onDialLocal: () -> Unit,
    onOpenWhatsApp: () -> Unit
) {
    val colors = CallTagDesign.colors
    val agencyIsDamkar = contact.agencyType.equals("DAMKAR", ignoreCase = true)
    val (badgeColor, badgeContainer, agencyLabel) = when (contact.agencyType.uppercase()) {
        "DAMKAR" -> Triple(colors.dangerScam, colors.dangerScamContainer, "Damkar")
        "POLISI" -> Triple(colors.brandPrimary, colors.brandContainer, "Kepolisian")
        else -> Triple(colors.cleanEmerald, colors.cleanEmeraldContainer, "IGD & Ambulans")
    }

    val capabilitiesList = contact.capabilitiesSummary
        .split("•", ",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

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
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)
            ) {
                Surface(
                    shape = RoundedCornerShape(CallTagRadius.pill),
                    color = badgeContainer
                ) {
                    Text(
                        text = "$agencyLabel • ${contact.cityRegency}",
                        modifier = Modifier.padding(
                            horizontal = CallTagSpacing.xs,
                            vertical = 4.dp
                        ),
                        color = badgeColor,
                        fontSize = CallTagType.caption,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (contact.is24Hours) "Siaga 24 jam" else "Jam kerja",
                    color = if (contact.is24Hours) colors.cleanEmerald else colors.textSecondary,
                    fontSize = CallTagType.caption,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)) {
                Text(
                    text = contact.name,
                    color = colors.textPrimary,
                    fontSize = CallTagType.title,
                    fontWeight = FontWeight.Bold
                )
                if (contact.address.isNotBlank()) {
                    Text(
                        text = contact.address,
                        color = colors.textSecondary,
                        fontSize = CallTagType.label
                    )
                }
            }

            if (capabilitiesList.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    capabilitiesList.forEach { cap ->
                        Surface(
                            shape = RoundedCornerShape(CallTagRadius.sm),
                            color = colors.surfaceSubtle
                        ) {
                            Text(
                                text = cap,
                                modifier = Modifier.padding(
                                    horizontal = CallTagSpacing.xs,
                                    vertical = 4.dp
                                ),
                                color = colors.textSecondary,
                                fontSize = CallTagType.caption,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
            ) {
                Button(
                    onClick = onDialLocal,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = CallTagTouch.action),
                    shape = RoundedCornerShape(CallTagRadius.md),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (agencyIsDamkar) colors.dangerSolid else colors.brandSolid,
                        contentColor = if (agencyIsDamkar) Color.White else colors.onBrand
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(CallTagSpacing.xxs))
                    Text(
                        text = "Telepon",
                        fontSize = CallTagType.label,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (contact.whatsappNumber.isNotBlank()) {
                    OutlinedButton(
                        onClick = onOpenWhatsApp,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = CallTagTouch.action),
                        shape = RoundedCornerShape(CallTagRadius.md),
                        border = BorderStroke(1.dp, colors.cleanEmerald)
                    ) {
                        Text(
                            text = "WhatsApp",
                            color = colors.cleanEmerald,
                            fontSize = CallTagType.label,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDirectoryCard() {
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
                text = "Posko tidak ditemukan",
                color = colors.textPrimary,
                fontSize = CallTagType.body,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Coba pilih Semua Kota atau Semua Posko.",
                color = colors.textSecondary,
                fontSize = CallTagType.bodySmall
            )
        }
    }
}

private fun dialNumber(context: Context, phoneNumber: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phoneNumber.trim()}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

private fun openWhatsAppEmergency(
    context: Context,
    whatsappNumber: String,
    template: String
) {
    runCatching {
        val cleanDigits = whatsappNumber.filter { it.isDigit() }
        val encodedText = Uri.encode(
            template.ifBlank { "Halo Petugas Darurat, saya membutuhkan bantuan darurat di lokasi: " }
        )
        val uri = Uri.parse("https://wa.me/$cleanDigits?text=$encodedText")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
