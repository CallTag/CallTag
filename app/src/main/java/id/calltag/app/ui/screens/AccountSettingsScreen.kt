package id.calltag.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.ClearCredentialException
import androidx.lifecycle.Lifecycle
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch
import androidx.lifecycle.LifecycleEventObserver
import id.calltag.app.BuildConfig
import id.calltag.app.ui.theme.*
import id.calltag.app.ui.viewmodel.CallTagMainViewModel
import id.calltag.app.ui.viewmodel.UiLoadState
import id.calltag.app.util.FeedbackDiagnostics
import id.calltag.app.util.RolePermissionManager

private enum class AccountPage(val title: String) {
    Overview("Akun"),
    Profile("Profil"),
    Permissions("Izin perlindungan"),
    Exceptions("Daftar pengecualian"),
    Appearance("Tampilan & penyimpanan"),
    Help("Bantuan & masukan"),
    Guide("Panduan CallTag"),
    Feedback("Kirim masukan");

    val parent: AccountPage
        get() = when (this) {
            Guide, Feedback -> Help
            else -> Overview
        }
}

@Composable
fun AccountSettingsScreen(
    viewModel: CallTagMainViewModel,
    modifier: Modifier = Modifier,
    onOpenProtection: () -> Unit = {}
) {
    val colors = CallTagDesign.colors
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val loggedIn by viewModel.isLoggedIn.collectAsState()
    val displayName by viewModel.sessionDisplayName.collectAsState()
    val dark by viewModel.isDarkMode.collectAsState()
    val profile by viewModel.accountProfileState.collectAsState()
    val message by viewModel.accountActionMessage.collectAsState()
    val health by viewModel.backendHealthState.collectAsState()
    val whitelist by viewModel.personalWhitelist.collectAsState(initial = emptySet())
    var page by rememberSaveable { mutableStateOf(AccountPage.Overview) }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()
    var choosingGoogleAccount by remember { mutableStateOf(false) }
    var googleError by rememberSaveable { mutableStateOf<String?>(null) }

    fun signInWithGoogle() {
        if (choosingGoogleAccount || profile is UiLoadState.Loading) return
        choosingGoogleAccount = true
        googleError = null
        coroutineScope.launch {
            try {
                val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_CLIENT_ID).build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val credential = credentialManager.getCredential(context, request).credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    viewModel.loginWithGoogle(GoogleIdTokenCredential.createFrom(credential.data).idToken)
                } else {
                    googleError = "Akun Google tidak dapat dibaca."
                }
            } catch (_: GetCredentialCancellationException) {
                // Pengguna membatalkan pemilihan akun.
            } catch (_: GetCredentialException) {
                googleError = "Tidak bisa membuka akun Google. Pastikan akun ditambahkan di pengaturan Android."
            } catch (_: GoogleIdTokenParsingException) {
                googleError = "Identitas Google tidak dapat dibaca. Coba masuk lagi."
            } finally {
                choosingGoogleAccount = false
            }
        }
    }
    var whitelistPhone by rememberSaveable { mutableStateOf("") }
    var deleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf("Lapor Bug") }
    var description by rememberSaveable { mutableStateOf("") }
    var copied by rememberSaveable { mutableStateOf(false) }
    var role by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(false) }
    BackHandler(enabled = page != AccountPage.Overview) { page = page.parent }
    LaunchedEffect(loggedIn, page) {
        if (page == AccountPage.Profile) {
            if (!loggedIn) page = AccountPage.Overview
            else if (profile is UiLoadState.Idle) viewModel.refreshAccountProfile()
        }
    }
    LaunchedEffect(loggedIn) {
        if (!loggedIn) {
            whitelistPhone = ""
            description = ""
            deleteConfirmation = false
            copied = false
        }
    }
    fun refreshPermissions() {
        role = RolePermissionManager.isCallScreeningRoleHeld(context)
        overlay = RolePermissionManager.canDrawOverlays(context)
        notifications = RolePermissionManager.areNotificationsEnabled(context)
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshPermissions()
    }
    val onPermissionsPage by rememberUpdatedState(page == AccountPage.Permissions)
    LaunchedEffect(page) {
        if (page == AccountPage.Permissions) viewModel.checkBackendHealth()
    }
    DisposableEffect(lifecycleOwner) {
        refreshPermissions()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
                if (onPermissionsPage) viewModel.checkBackendHealth()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun openSettings(intent: Intent) {
        runCatching { context.startActivity(intent) }.onFailure {
            runCatching { context.startActivity(RolePermissionManager.createAppSettingsIntent(context)) }
                .onFailure { Toast.makeText(context, "Pengaturan tidak tersedia di perangkat ini.", Toast.LENGTH_SHORT).show() }
        }
    }
    key(page) {
        LazyColumn(
            modifier = modifier.fillMaxSize().background(colors.backgroundPrimary),
            contentPadding = PaddingValues(CallTagSpacing.md),
            verticalArrangement = Arrangement.spacedBy(CallTagSpacing.md)
        ) {
            item { SettingsHeading(page.title, if (page == AccountPage.Overview) null else ({ page = page.parent })) }
            when (page) {
                AccountPage.Overview -> {
                    item {
                        AccountSection(if (loggedIn) displayName.ifBlank { "Akun aktif" } else "Belum masuk") {
                            if (loggedIn) {
                                SettingsMenuRow(AccountPage.Profile.title) { page = AccountPage.Profile }
                            } else {
                                AccountBody("Masuk untuk mengirim #Tag dan mengelola pengecualian.")
                                AccountButton(
                                    text = if (profile is UiLoadState.Loading) "Sedang masuk…" else "Masuk dengan Google",
                                    enabled = !choosingGoogleAccount && profile !is UiLoadState.Loading,
                                    onClick = ::signInWithGoogle
                                )
                                (googleError ?: (profile as? UiLoadState.Error)?.message)?.let { AccountBody(it) }
                            }
                            message?.takeIf { !loggedIn && profile !is UiLoadState.Error }?.let { AccountBody(it) }
                        }
                    }
                    item {
                        SettingsMenu {
                            SettingsMenuRow(AccountPage.Permissions.title, if (role) "Caller ID aktif" else "Caller ID belum aktif") {
                                page = AccountPage.Permissions
                            }
                            HorizontalDivider(color = colors.borderSubtle)
                            SettingsMenuRow(AccountPage.Exceptions.title) { page = AccountPage.Exceptions }
                            HorizontalDivider(color = colors.borderSubtle)
                            SettingsMenuRow(AccountPage.Appearance.title) { page = AccountPage.Appearance }
                            HorizontalDivider(color = colors.borderSubtle)
                            SettingsMenuRow(AccountPage.Help.title) { page = AccountPage.Help }
                        }
                    }
                }
                AccountPage.Profile -> if (loggedIn) item {
                    AccountSection((profile as? UiLoadState.Success)?.data?.displayName.orEmpty().ifBlank { displayName.ifBlank { "Akun aktif" } }) {
                        when (val state = profile) {
                            UiLoadState.Loading -> AccountBody("Memuat profil…")
                            is UiLoadState.Error -> AccountBody(state.message)
                            is UiLoadState.Success -> {
                                state.data.email.takeIf { it.isNotBlank() }?.let { AccountBody(it) }
                            }
                            else -> AccountBody("Profil akun belum dimuat.")
                        }
                        AccountButton(
                            text = if (profile is UiLoadState.Loading) "Memuat profil…" else "Muat ulang profil",
                            outlined = true,
                            enabled = profile !is UiLoadState.Loading
                        ) { viewModel.refreshAccountProfile(showConfirmation = true) }
                        AccountButton("Keluar akun", outlined = true) {
                            viewModel.logout()
                            coroutineScope.launch {
                                try {
                                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                } catch (_: ClearCredentialException) {
                                    // Sesi CallTag sudah ditutup meski penyedia kredensial gagal dibersihkan.
                                }
                            }
                        }
                        TextButton(onClick = { deleteConfirmation = true }, modifier = Modifier.heightIn(min = CallTagTouch.action)) {
                            Text("Hapus data akun", color = colors.dangerScam)
                        }
                        message?.let { AccountBody(it) }
                    }
                }
                AccountPage.Appearance -> item {
                    AccountSection("Preferensi perangkat") {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = CallTagTouch.action)) {
                            Text("Tema gelap", color = colors.textPrimary, modifier = Modifier.weight(1f))
                            Switch(
                                checked = dark,
                                onCheckedChange = viewModel::setDarkMode,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.onBrand,
                                    checkedTrackColor = colors.brandSolid,
                                    checkedBorderColor = colors.brandSolid,
                                    uncheckedThumbColor = colors.textSecondary,
                                    uncheckedTrackColor = colors.surfaceSubtle,
                                    uncheckedBorderColor = colors.borderStrong
                                )
                            )
                        }
                        AccountBody("Pilihan tema tersimpan di perangkat ini. Bersihkan cache pencarian tanpa menghapus data bawaan darurat.")
                        AccountButton("Bersihkan cache pencarian", outlined = true) { viewModel.clearCache() }
                        message?.let { AccountBody(it) }
                    }
                }
                AccountPage.Permissions -> item {
                    AccountSection("Izin Android") {
                        AccountBody("Pilih CallTag sebagai aplikasi Caller ID di Android. Izin popup dan notifikasi diatur terpisah.")
                        PermissionSetting("Caller ID / penyaring panggilan", role) {
                            val roleIntent = RolePermissionManager.createCallScreeningRoleIntent(context)
                            if (roleIntent != null) {
                                runCatching { roleLauncher.launch(roleIntent) }
                                    .onFailure { openSettings(RolePermissionManager.createDefaultAppsSettingsIntent()) }
                            } else {
                                openSettings(RolePermissionManager.createDefaultAppsSettingsIntent())
                            }
                        }
                        PermissionSetting("Popup di atas aplikasi lain", overlay) { openSettings(RolePermissionManager.createOverlayPermissionIntent(context)) }
                        PermissionSetting("Notifikasi", notifications) { openSettings(RolePermissionManager.createNotificationSettingsIntent(context)) }
                        AccountBody(when (val state = health) {
                            is UiLoadState.Success -> state.data
                            is UiLoadState.Error -> "Backend tidak terhubung: ${state.message}. Data offline tetap tersedia."
                            UiLoadState.Loading -> "Memeriksa koneksi backend…"
                            else -> "Backend belum diperiksa."
                        })
                        AccountButton("Periksa koneksi backend", outlined = true, enabled = health !is UiLoadState.Loading) { viewModel.checkBackendHealth() }
                        AccountButton("Buka tab Proteksi", outlined = true) {
                            page = AccountPage.Overview
                            onOpenProtection()
                        }
                    }
                }
                AccountPage.Exceptions -> item {
                    AccountSection("Nomor yang dikecualikan") {
                        AccountBody("Nomor dalam daftar ini melewati blokir otomatis. Tersimpan lokal di perangkat, bukan tanda terverifikasi resmi.")
                        if (!loggedIn) {
                            AccountBody("Masuk untuk menambahkan nomor ke daftar pengecualian.")
                            AccountButton("Masuk dengan Google", enabled = !choosingGoogleAccount && profile !is UiLoadState.Loading, onClick = ::signInWithGoogle)
                        (googleError ?: (profile as? UiLoadState.Error)?.message)?.let { AccountBody(it) }
                        } else {
                            OutlinedTextField(value = whitelistPhone, onValueChange = { whitelistPhone = it }, label = { Text("Nomor kenalan, kurir, atau kantor") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, shape = RoundedCornerShape(CallTagRadius.md), modifier = Modifier.fillMaxWidth())
                            AccountButton("Tambah pengecualian", enabled = whitelistPhone.isNotBlank()) { viewModel.addToWhitelist(whitelistPhone); whitelistPhone = "" }
                            if (whitelist.isEmpty()) AccountBody("Belum ada nomor dalam daftar pengecualian.")
                            message?.let { AccountBody(it) }
                            whitelist.sorted().forEach { number ->
                                Column {
                                    Text(number, style = CallTagDesign.monoStyle(CallTagType.body), color = colors.textPrimary)
                                    AccountButton("Hapus dari whitelist", outlined = true) { viewModel.removeFromWhitelist(number) }
                                }
                            }
                        }
                    }
                }
                AccountPage.Help -> item {
                    SettingsMenu {
                        SettingsMenuRow(AccountPage.Guide.title) { page = AccountPage.Guide }
                        HorizontalDivider(color = colors.borderSubtle)
                        SettingsMenuRow(AccountPage.Feedback.title, "Saran, kritik, atau laporan bug") { page = AccountPage.Feedback }
                    }
                }
                AccountPage.Guide -> item {
                    AccountSection("Langkah penggunaan") {
                        AccountBody("1. Aktifkan Caller ID / penyaring panggilan di halaman Izin perlindungan.")
                        AccountBody("2. Izinkan popup dan notifikasi agar identitas panggilan dapat tampil saat telepon masuk.")
                        AccountBody("3. Buka Proteksi untuk mengatur blokir otomatis dan memperbarui data offline.")
                        AccountBody("4. Cari nomor sebelum membalas. Kirim #Tag setelah masuk akun, atau ajukan sanggahan jika data keliru.")
                        Text("Arti reputasi nomor", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                        ReputationGuide("CLEAN · Risiko rendah, bukan jaminan aman.", colors.cleanEmerald)
                        ReputationGuide("SUSPICIOUS · Dilaporkan komunitas, berhati-hati.", colors.suspiciousAmber)
                        ReputationGuide("SPAM · Indikasi panggilan mengganggu.", colors.spamOrange)
                        ReputationGuide("DANGER_SCAM · Indikasi risiko penipuan tinggi.", colors.dangerScam)
                        AccountBody("VERIFIED berarti cocok dengan sumber resmi. UNKNOWN berarti data belum tersedia, bukan berarti aman.")
                    }
                }
                AccountPage.Feedback -> item {
                    val feedback = FeedbackDiagnostics.create(category, description, BuildConfig.VERSION_NAME, Build.MODEL, Build.VERSION.RELEASE)
                    AccountSection("Kategori dan deskripsi") {
                        AccountBody("Pilih kategori dan jelaskan masalah. Versi aplikasi, model HP, serta versi Android ikut dilampirkan. Hindari menulis data pribadi atau token.")
                        listOf("Saran Fitur", "Kritik Tampilan", "Lapor Bug").forEach { option ->
                            OutlinedButton(onClick = { category = option; copied = false }, shape = RoundedCornerShape(CallTagRadius.md), modifier = Modifier.fillMaxWidth().heightIn(min = CallTagTouch.action), colors = ButtonDefaults.outlinedButtonColors(contentColor = if (category == option) colors.brandPrimary else colors.textSecondary)) {
                                Text(if (category == option) "✓ $option" else option)
                            }
                        }
                        OutlinedTextField(value = description, onValueChange = { description = it.take(3000); copied = false }, label = { Text("Deskripsi dan langkah kejadian") }, minLines = 3, shape = RoundedCornerShape(CallTagRadius.md), modifier = Modifier.fillMaxWidth())
                        AccountBody("CallTag ${BuildConfig.VERSION_NAME} · ${Build.MODEL} · Android ${Build.VERSION.RELEASE}")
                        AccountButton(if (copied) "Template tersalin" else "Salin template") {
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Feedback CallTag", feedback))
                            copied = true
                        }
                        AccountButton("Bagikan via Email / WhatsApp", outlined = true, enabled = description.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "CallTag — $category")
                                putExtra(Intent.EXTRA_TEXT, feedback)
                            }
                            runCatching { context.startActivity(Intent.createChooser(intent, "Bagikan feedback CallTag")) }
                                .onFailure { Toast.makeText(context, "Aplikasi berbagi tidak tersedia. Salin template untuk mengirim manual.", Toast.LENGTH_LONG).show() }
                        }
                        AccountBody("Pilih aplikasi dan penerima sendiri. Feedback belum terkirim sampai Anda mengirimkannya di aplikasi tujuan.")
                    }
                }
            }
        }
    }
    if (deleteConfirmation && loggedIn && page == AccountPage.Profile) AlertDialog(
        onDismissRequest = { deleteConfirmation = false },
        title = { Text("Hapus data akun?") },
        text = { Text("Permintaan penghapusan dikirim ke server untuk akun aktif. Tindakan ini tidak dapat dibatalkan. Data yang wajib disimpan dapat mengikuti kebijakan retensi layanan.") },
        confirmButton = { TextButton(onClick = { deleteConfirmation = false; viewModel.requestAccountDeletion() }) { Text("Hapus data", color = colors.dangerScam) } },
        dismissButton = { TextButton(onClick = { deleteConfirmation = false }) { Text("Batal") } }
    )
}

@Composable
internal fun SettingsHeading(title: String, onBack: (() -> Unit)? = null) {
    val colors = CallTagDesign.colors
    Row(
        modifier = Modifier.heightIn(min = CallTagTouch.compact),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.xs)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = colors.textPrimary)
            }
        }
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = if (onBack == null) CallTagType.display else CallTagType.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun SettingsMenu(content: @Composable ColumnScope.() -> Unit) {
    val colors = CallTagDesign.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceCard,
        shape = RoundedCornerShape(CallTagRadius.lg),
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(content = content)
    }
}

@Composable
internal fun SettingsMenuRow(title: String, summary: String? = null, onClick: () -> Unit) {
    val colors = CallTagDesign.colors
    Surface(
        onClick = onClick,
        color = colors.surfaceCard,
        shape = RoundedCornerShape(CallTagRadius.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = CallTagTouch.action)
                .padding(horizontal = CallTagSpacing.md, vertical = CallTagSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CallTagSpacing.sm)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)) {
                Text(title, color = colors.textPrimary, fontSize = CallTagType.subtitle, fontWeight = FontWeight.SemiBold)
                if (summary != null) Text(summary, color = colors.textSecondary, fontSize = CallTagType.bodySmall)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary)
        }
    }
}

@Composable
private fun AccountSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = CallTagDesign.colors
    Surface(color = colors.surfaceCard, shape = RoundedCornerShape(CallTagRadius.lg), border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(CallTagSpacing.md), verticalArrangement = Arrangement.spacedBy(CallTagSpacing.sm)) {
            Text(title, color = colors.textPrimary, fontSize = CallTagType.title, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun AccountBody(text: String) = Text(text, color = CallTagDesign.colors.textSecondary, fontSize = CallTagType.body)

@Composable
private fun AccountButton(text: String, outlined: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = CallTagDesign.colors
    val modifier = Modifier.fillMaxWidth().heightIn(min = CallTagTouch.action)
    if (outlined) OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier, shape = RoundedCornerShape(CallTagRadius.md), colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.brandPrimary)) { Text(text) }
    else Button(onClick = onClick, enabled = enabled, modifier = modifier, shape = RoundedCornerShape(CallTagRadius.md), colors = ButtonDefaults.buttonColors(containerColor = colors.brandSolid, contentColor = colors.onBrand)) { Text(text) }
}

@Composable
private fun PermissionSetting(label: String, active: Boolean, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CallTagSpacing.xxs)) {
        Text("$label · ${if (active) "Aktif" else "Belum aktif"}", color = if (active) CallTagDesign.colors.brandPrimary else CallTagDesign.colors.textPrimary, fontSize = CallTagType.body, fontWeight = FontWeight.SemiBold)
        AccountButton("Atur $label", outlined = true, onClick = onClick)
    }
}

@Composable
private fun ReputationGuide(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text, color = color, fontSize = CallTagType.body)
}
