package id.calltag.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.calltag.app.data.local.entity.CachedPhoneTag
import id.calltag.app.ui.theme.CallTagDesign

/**
 * Kartu Popup Caller ID saat telepon berdering bergaya Truecaller / Getcontact:
 * Menampilkan avatar melingkar, nama #Tag utama komunitas, nomor telepon jelas,
 * tingkat bahaya, dan tombol cepat Tolak Panggilan.
 */
@Composable
fun CallerIdOverlayCard(
    phoneTag: CachedPhoneTag,
    lookupLatencyMs: Double,
    source: String,
    autoBlockEnabled: Boolean,
    onDismiss: () -> Unit,
    onRejectSimulatedCall: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val level = phoneTag.reputationLevel.uppercase()

    val headerBg: Color
    val cardBg: Color
    val badgeText: String

    when (level) {
        "DANGER_SCAM" -> {
            headerBg = Color(0xFF7F1D16)
            cardBg = Color(0xFFC4271F)
            badgeText = "PERINGATAN PENIPUAN"
        }
        "SPAM" -> {
            headerBg = Color(0xFF7C3611)
            cardBg = Color(0xFFC0561B)
            badgeText = "TELEPON SPAM"
        }
        "SUSPICIOUS" -> {
            headerBg = Color(0xFF6B4A08)
            cardBg = Color(0xFF9A6A0B)
            badgeText = "PERLU WASPADA"
        }
        else -> {
            headerBg = Color(0xFF0B5A54)
            cardBg = Color(0xFF0F766E)
            badgeText = "NOMOR AMAN"
        }
    }

    val categoryShort = phoneTag.primaryCategoryLabel.substringBefore("(").trim()
    val initials = CallTagDesign.callerInitials(phoneTag.topCommunityLabel, level)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Popup Caller ID
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "CallTag Caller ID • $badgeText",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Panggilan Masuk",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar + Identitas Penelpon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = phoneTag.topCommunityLabel,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 22.sp
                        )
                        Text(
                            text = phoneTag.phoneE164,
                            style = CallTagDesign.monoStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.95f)
                            )
                        )
                        Text(
                            text = "${phoneTag.carrier} • ${phoneTag.uniqueReporters} laporan",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 12.sp
                        )
                    }
                }

                // Baris #Tag & Skor Risiko
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(headerBg, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "#$categoryShort",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Skor ${phoneTag.spamScore.toInt()}/100",
                            style = CallTagDesign.monoStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }

                if (level == "DANGER_SCAM" && autoBlockEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(headerBg, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Proteksi Otomatis Aktif: Panggilan penipu langsung ditolak tanpa berdering.",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Tombol Aksi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (onRejectSimulatedCall != null && level != "CLEAN") {
                        Button(
                            onClick = onRejectSimulatedCall,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = headerBg
                            )
                        ) {
                            Text(
                                text = "Tolak & Blokir",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "  Tutup",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
