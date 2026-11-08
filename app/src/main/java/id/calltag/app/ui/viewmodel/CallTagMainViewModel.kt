package id.calltag.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.calltag.app.data.local.OfflineSeedData
import id.calltag.app.data.local.entity.CachedPhoneTag
import id.calltag.app.data.local.entity.CallScreeningLog
import id.calltag.app.data.local.entity.EmergencyContact
import id.calltag.app.data.preferences.CallTagPreferences
import id.calltag.app.data.remote.dto.DisputeResponseDto
import id.calltag.app.data.remote.dto.OfficialChannelDto
import id.calltag.app.data.remote.dto.ReportResponseDto
import id.calltag.app.data.repository.CallTagRepository
import id.calltag.app.data.repository.FullPhoneLookupResult
import id.calltag.app.data.repository.OfflineCacheStats
import id.calltag.app.util.PhoneNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiLoadState<out T> {
    data object Idle : UiLoadState<Nothing>
    data object Loading : UiLoadState<Nothing>
    data class Success<T>(val data: T) : UiLoadState<T>
    data class Error(val message: String) : UiLoadState<Nothing>
}

class CallTagMainViewModel(
    private val repository: CallTagRepository,
    private val preferences: CallTagPreferences
) : ViewModel() {

    // Pengaturan Tema & Spam Guard
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    val autoBlockScammer: StateFlow<Boolean> = preferences.autoBlockScammerFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showHeadsUpAlert: StateFlow<Boolean> = preferences.showHeadsUpAlertFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Layar 1: Home Lookup & Riwayat Panggilan
    private val _searchQuery = MutableStateFlow("+6281299887766")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _lookupState = MutableStateFlow<UiLoadState<FullPhoneLookupResult>>(UiLoadState.Idle)
    val lookupState: StateFlow<UiLoadState<FullPhoneLookupResult>> = _lookupState.asStateFlow()

    private val _activeOverlayPreview = MutableStateFlow<FullPhoneLookupResult?>(null)
    val activeOverlayPreview: StateFlow<FullPhoneLookupResult?> = _activeOverlayPreview.asStateFlow()

    val recentScreeningLogs: StateFlow<List<CallScreeningLog>> = repository.observeRecentScreeningLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dangerScammers: StateFlow<List<CachedPhoneTag>> = repository.observeDangerScammers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Layar 2: Direktori Darurat Nasional & Lokal
    val nationalEmergencyContacts: StateFlow<List<EmergencyContact>> =
        repository.observeNationalEmergencyContacts()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                OfflineSeedData.getInitialEmergencyContacts().filter { it.scope == "NATIONAL" }
            )

    private val _selectedProvince = MutableStateFlow("")
    val selectedProvince: StateFlow<String> = _selectedProvince.asStateFlow()

    private val _selectedCity = MutableStateFlow("")
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    private val _selectedAgencyType = MutableStateFlow("")
    val selectedAgencyType: StateFlow<String> = _selectedAgencyType.asStateFlow()

    private val _allLocalEmergency = repository.observeLocalEmergencyContacts()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            OfflineSeedData.getInitialEmergencyContacts().filter { it.scope == "LOCAL" }
        )

    val filteredLocalEmergencyContacts: StateFlow<List<EmergencyContact>> = combine(
        _allLocalEmergency,
        _selectedProvince,
        _selectedCity,
        _selectedAgencyType
    ) { list, prov, city, agency ->
        list.filter { item ->
            val matchProv = prov.isBlank() || item.province.contains(prov, ignoreCase = true)
            val matchCity = city.isBlank() || item.cityRegency.contains(city, ignoreCase = true)
            val matchAgency = agency.isBlank() || item.agencyType.equals(agency, ignoreCase = true)
            matchProv && matchCity && matchAgency
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Layar 3: Lapor Komunitas, Official Reporting Hub, & Dispute
    private val _reportSubmissionState = MutableStateFlow<UiLoadState<ReportResponseDto>>(UiLoadState.Idle)
    val reportSubmissionState: StateFlow<UiLoadState<ReportResponseDto>> = _reportSubmissionState.asStateFlow()

    private val _disputeSubmissionState = MutableStateFlow<UiLoadState<DisputeResponseDto>>(UiLoadState.Idle)
    val disputeSubmissionState: StateFlow<UiLoadState<DisputeResponseDto>> = _disputeSubmissionState.asStateFlow()

    private val _officialChannels = MutableStateFlow(OfflineSeedData.getInitialOfficialChannels())
    val officialChannels: StateFlow<List<OfficialChannelDto>> = _officialChannels.asStateFlow()

    private val _lastReportedPhoneForCopy = MutableStateFlow("+6281299887766")
    val lastReportedPhoneForCopy: StateFlow<String> = _lastReportedPhoneForCopy.asStateFlow()

    // Layar 4: Spam Guard & Offline Delta Sync
    private val _cacheStats = MutableStateFlow(
        OfflineCacheStats(
            syncVersion = 6L,
            cachedPhoneCount = OfflineSeedData.getInitialTopScammers().size,
            emergencyContactsCount = OfflineSeedData.getInitialEmergencyContacts().size
        )
    )
    val cacheStats: StateFlow<OfflineCacheStats> = _cacheStats.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow("Database perlindungan nomor siap digunakan tanpa internet.")
    val syncStatusMessage: StateFlow<String> = _syncStatusMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        performPhoneLookup("+6281299887766")
        refreshCacheStats()
        loadOfficialChannels()
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun performPhoneLookup(rawPhone: String = _searchQuery.value) {
        val cleaned = rawPhone.trim()
        if (cleaned.isEmpty()) {
            _lookupState.value = UiLoadState.Error("Masukkan nomor telepon terlebih dahulu (contoh: 081299887766).")
            return
        }
        _searchQuery.value = cleaned
        _lookupState.value = UiLoadState.Loading

        viewModelScope.launch {
            val result = repository.lookupPhoneFull(cleaned)
            if (result != null) {
                _lookupState.value = UiLoadState.Success(result)
                _lastReportedPhoneForCopy.value = result.phoneTag.phoneE164
            } else {
                _lookupState.value = UiLoadState.Error("Format nomor telepon tidak valid.")
            }
        }
    }

    fun triggerOverlayPreview(lookupResult: FullPhoneLookupResult) {
        _activeOverlayPreview.value = lookupResult
    }

    fun simulateOverlayByLevel(level: String) {
        val sampleTag = when (level.uppercase()) {
            "DANGER_SCAM" -> OfflineSeedData.getInitialTopScammers().first { it.reputationLevel == "DANGER_SCAM" }
            "SPAM" -> OfflineSeedData.getInitialTopScammers().first { it.reputationLevel == "SPAM" }
            else -> OfflineSeedData.getInitialTopScammers().first { it.reputationLevel == "SUSPICIOUS" }
        }
        _activeOverlayPreview.value = FullPhoneLookupResult(
            phoneTag = sampleTag,
            categoryBreakdown = OfflineSeedData.getOfflineBreakdownForPhone(sampleTag),
            recentReports = OfflineSeedData.getOfflineCommentsForPhone(sampleTag),
            source = "LOCAL_CACHE",
            lookupLatencyMs = 4.2
        )
    }

    fun dismissOverlayPreview() {
        _activeOverlayPreview.value = null
    }

    fun recordSimulatedBlock(phoneTag: CachedPhoneTag) {
        viewModelScope.launch {
            repository.recordScreeningLog(
                CallScreeningLog(
                    phoneE164 = phoneTag.phoneE164,
                    tagLabel = phoneTag.topCommunityLabel,
                    categoryCode = phoneTag.primaryCategory,
                    reputationLevel = phoneTag.reputationLevel,
                    spamScore = phoneTag.spamScore,
                    wasBlocked = true,
                    detectionSource = "LOCAL_CACHE"
                )
            )
            _activeOverlayPreview.value = null
        }
    }

    fun setEmergencyFilter(
        province: String = _selectedProvince.value,
        city: String = _selectedCity.value,
        agencyType: String = _selectedAgencyType.value
    ) {
        _selectedProvince.value = province
        _selectedCity.value = city
        _selectedAgencyType.value = agencyType
    }

    fun submitCommunityReport(
        phone: String,
        categoryCode: String,
        customTag: String,
        comment: String,
        callType: String
    ) {
        if (phone.isBlank()) {
            _reportSubmissionState.value = UiLoadState.Error("Nomor telepon wajib diisi.")
            return
        }
        val effectiveTag = customTag.trim().ifBlank {
            PhoneNormalizer.getCategoryDisplayLabel(categoryCode)
        }
        _reportSubmissionState.value = UiLoadState.Loading

        viewModelScope.launch {
            val result = repository.submitSpamReport(
                phone = phone,
                categoryCode = categoryCode,
                tagName = effectiveTag,
                comment = comment.trim(),
                callType = callType
            )
            result.fold(
                onSuccess = { resp ->
                    _lastReportedPhoneForCopy.value = resp.phoneTag.phoneE164
                    _reportSubmissionState.value = UiLoadState.Success(resp)
                    refreshCacheStats()
                    performPhoneLookup(resp.phoneTag.phoneE164)
                },
                onFailure = { err ->
                    _reportSubmissionState.value = UiLoadState.Error(
                        err.message ?: "Gagal mengirim laporan"
                    )
                }
            )
        }
    }

    fun resetReportState() {
        _reportSubmissionState.value = UiLoadState.Idle
    }

    fun submitNumberDispute(
        phone: String,
        ownerName: String,
        reason: String,
        contactRef: String
    ) {
        if (phone.isBlank() || ownerName.isBlank() || reason.isBlank()) {
            _disputeSubmissionState.value = UiLoadState.Error(
                "Nomor telepon, nama pemilik, dan alasan sanggahan wajib diisi lengkap."
            )
            return
        }
        _disputeSubmissionState.value = UiLoadState.Loading

        viewModelScope.launch {
            val result = repository.submitDispute(
                phone = phone,
                ownerName = ownerName,
                reason = reason,
                contactRef = contactRef
            )
            result.fold(
                onSuccess = { resp ->
                    _disputeSubmissionState.value = UiLoadState.Success(resp)
                },
                onFailure = { err ->
                    _disputeSubmissionState.value = UiLoadState.Error(
                        err.message ?: "Gagal mengajukan sanggahan"
                    )
                }
            )
        }
    }

    fun resetDisputeState() {
        _disputeSubmissionState.value = UiLoadState.Idle
    }

    fun setAutoBlockScammer(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setAutoBlockScammer(enabled)
        }
    }

    fun setShowHeadsUpAlert(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setShowHeadsUpAlert(enabled)
        }
    }

    fun runOfflineDeltaSync() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        _syncStatusMessage.value = "Memperbarui database nomor penipu terbaru..."

        viewModelScope.launch {
            val res = repository.syncTopScammersToLocalCache()
            repository.syncEmergencyDirectory()
            val stats = repository.getLocalCacheStats()
            _cacheStats.value = stats
            _isSyncing.value = false

            res.fold(
                onSuccess = {
                    _syncStatusMessage.value =
                        "Database perlindungan berhasil diperbarui (versi ${stats.syncVersion})."
                },
                onFailure = {
                    _syncStatusMessage.value =
                        "Menggunakan database perlindungan lokal di HP (versi ${stats.syncVersion})."
                }
            )
        }
    }

    private fun refreshCacheStats() {
        viewModelScope.launch {
            _cacheStats.value = repository.getLocalCacheStats()
        }
    }

    private fun loadOfficialChannels() {
        viewModelScope.launch {
            _officialChannels.value = repository.getOfficialChannels()
        }
    }

    companion object {
        fun provideFactory(
            repository: CallTagRepository,
            preferences: CallTagPreferences
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CallTagMainViewModel(repository, preferences) as T
            }
        }
    }
}
