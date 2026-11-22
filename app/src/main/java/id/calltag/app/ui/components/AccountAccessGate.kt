package id.calltag.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.calltag.app.ui.theme.CallTagDesign
import id.calltag.app.ui.theme.CallTagRadius
import id.calltag.app.ui.theme.CallTagSpacing
import id.calltag.app.ui.theme.CallTagTouch
import id.calltag.app.ui.theme.CallTagType

/** Ajakan masuk yang konsisten untuk fitur pribadi. */
@Composable
fun AccountAccessGate(onOpenAccount: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CallTagDesign.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CallTagRadius.lg),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(Modifier.padding(CallTagSpacing.lg), verticalArrangement = Arrangement.spacedBy(CallTagSpacing.sm)) {
            Text("Masuk Akun untuk Membuka Fitur Ini", color = colors.textPrimary,
                fontSize = CallTagType.title, fontWeight = FontWeight.Bold)
            Text("Simpan #Tag, akses riwayat pribadi, dan kelola pengecualian nomor dari tab Akun.",
                color = colors.textSecondary, fontSize = CallTagType.body)
            Button(onClick = onOpenAccount,
                modifier = Modifier.fillMaxWidth().heightIn(min = CallTagTouch.action),
                shape = RoundedCornerShape(CallTagRadius.md),
                colors = ButtonDefaults.buttonColors(containerColor = colors.brandSolid, contentColor = colors.onBrand)) {
                Text("Buka tab Akun", fontWeight = FontWeight.Bold)
            }
        }
    }
}
