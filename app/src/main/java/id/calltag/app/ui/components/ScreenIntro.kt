package id.calltag.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.calltag.app.ui.theme.CallTagDesign
import id.calltag.app.ui.theme.CallTagSpacing
import id.calltag.app.ui.theme.CallTagType

/** Pengantar bersama agar judul dan hierarki empat tab tetap konsisten. */
@Composable
fun ScreenIntro(
    eyebrow: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val colors = CallTagDesign.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
    ) {
        Text(
            text = eyebrow.uppercase(),
            color = colors.brandPrimary,
            fontSize = CallTagType.label,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = CallTagType.display,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.8).sp,
            lineHeight = 36.sp
        )
        Text(
            text = description,
            color = colors.textSecondary,
            fontSize = CallTagType.body,
            lineHeight = 21.sp
        )
    }
}
