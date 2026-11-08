package id.calltag.app.util

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Helper terpusat untuk mengelola izin RoleManager.ROLE_CALL_SCREENING (Android 10+)
 * serta runtime permissions untuk Caller ID, blokir otomatis panggilan, notifikasi, dan overlay.
 */
object RolePermissionManager {

    /**
     * Memeriksa apakah aplikasi CallTag sudah ditetapkan sebagai Default Call Screening & Caller ID App
     * di tingkat sistem operasi Android.
     */
    fun isCallScreeningRoleHeld(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true &&
                roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        } else {
            hasRuntimePermissions(context)
        }
    }

    /**
     * Membuat Intent resmi sistem Android 10+ (API 29+) untuk meminta RoleManager.ROLE_CALL_SCREENING
     * agar CallTag disetujui pengguna sebagai pendeteksi Caller ID & Spam bawaan.
     */
    fun createCallScreeningRoleIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            }
        }
        return null
    }

    /**
     * Daftar runtime permission wajib berdasarkan versi SDK Android perangkat.
     */
    fun getRequiredRuntimePermissions(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return list.toTypedArray()
    }

    fun hasRuntimePermissions(context: Context): Boolean {
        return getRequiredRuntimePermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun createOverlayPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun openAppSettings(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", activity.packageName, null)
        )
        activity.startActivity(intent)
    }
}
