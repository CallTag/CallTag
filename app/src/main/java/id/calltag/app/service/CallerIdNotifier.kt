package id.calltag.app.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import id.calltag.app.MainActivity
import id.calltag.app.R
import id.calltag.app.data.local.entity.CachedPhoneTag

/**
 * Menampilkan Heads-Up Notification berprioritas tinggi serta Overlay Caller ID di layar
 * saat CallTagScreeningService mendeteksi nomor mencurigakan, spam, atau scam.
 */
object CallerIdNotifier {

    const val CHANNEL_ID_CALLER_ALERT = "calltag_high_priority_caller_id"
    private const val NOTIFICATION_ID_BASE = 7000

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_ID_CALLER_ALERT,
                context.getString(R.string.notification_channel_caller_id),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun showCallerAlert(
        context: Context,
        phoneTag: CachedPhoneTag,
        wasAutoBlocked: Boolean
    ) {
        ensureNotificationChannel(context)
        showHeadsUpNotification(context, phoneTag, wasAutoBlocked)
        if (Settings.canDrawOverlays(context)) {
            showOverlayBanner(context, phoneTag, wasAutoBlocked)
        }
    }

    private fun showHeadsUpNotification(
        context: Context,
        phoneTag: CachedPhoneTag,
        wasAutoBlocked: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("screened_phone", phoneTag.phoneE164)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            phoneTag.phoneE164.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusPrefix = when {
            wasAutoBlocked -> "DIBLOKIR OTOMATIS"
            phoneTag.reputationLevel == "DANGER_SCAM" -> "BAHAYA SCAM"
            phoneTag.reputationLevel == "SPAM" -> "PERINGATAN SPAM"
            else -> "NOMOR DILAPORKAN"
        }

        val title = "$statusPrefix: ${phoneTag.topCommunityLabel}"
        val body = buildString {
            append("Nomor: ${phoneTag.phoneE164}\n")
            append("Kategori: ${phoneTag.primaryCategoryLabel}\n")
            append("Skor Bahaya: ${phoneTag.spamScore}/100 (${phoneTag.uniqueReporters} pelapor)")
        }

        val accentColor = when (phoneTag.reputationLevel) {
            "DANGER_SCAM" -> Color.parseColor("#C4271F")
            "SPAM" -> Color.parseColor("#C0561B")
            else -> Color.parseColor("#9A6A0B")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CALLER_ALERT)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText("${phoneTag.primaryCategoryLabel} • Skor ${phoneTag.spamScore}/100")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setColor(accentColor)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID_BASE + (phoneTag.phoneE164.hashCode() and 0x0FFF),
            notification
        )
    }

    private fun showOverlayBanner(
        context: Context,
        phoneTag: CachedPhoneTag,
        wasAutoBlocked: Boolean
    ) {
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            runCatching {
                val windowManager =
                    context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return@post

                val bgColor = when (phoneTag.reputationLevel) {
                    "DANGER_SCAM" -> Color.parseColor("#7F1D16")
                    "SPAM" -> Color.parseColor("#7C3611")
                    else -> Color.parseColor("#6B4A08")
                }

                val container = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(48, 36, 48, 36)
                    background = GradientDrawable().apply {
                        setColor(bgColor)
                        cornerRadius = 28f
                    }
                }

                val badgeText = if (wasAutoBlocked) {
                    "CALLTAG SPAM GUARD • PANGGILAN DITOLAK OTOMATIS"
                } else {
                    "CALLTAG CALLER ID • ${phoneTag.reputationLevel}"
                }

                val headerView = TextView(context).apply {
                    text = badgeText
                    setTextColor(Color.parseColor("#FCEDEA"))
                    textSize = 11f
                    setTypeface(typeface, Typeface.BOLD)
                }

                val titleView = TextView(context).apply {
                    text = phoneTag.topCommunityLabel
                    setTextColor(Color.WHITE)
                    textSize = 17f
                    setTypeface(typeface, Typeface.BOLD)
                }

                val detailView = TextView(context).apply {
                    text = "${phoneTag.phoneE164} • ${phoneTag.primaryCategoryLabel} (Skor: ${phoneTag.spamScore}/100)"
                    setTextColor(Color.WHITE)
                    textSize = 13f
                }

                container.addView(headerView)
                container.addView(titleView)
                container.addView(detailView)

                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = 96
                }

                container.setOnClickListener {
                    runCatching { windowManager.removeView(container) }
                }

                windowManager.addView(container, params)

                // Tutup overlay otomatis setelah 8 detik
                mainHandler.postDelayed({
                    runCatching { windowManager.removeView(container) }
                }, 8000L)
            }
        }
    }
}
