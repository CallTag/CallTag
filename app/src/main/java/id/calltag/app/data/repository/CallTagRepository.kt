package id.calltag.app.data.repository

import id.calltag.app.data.local.OfflineSeedData
import id.calltag.app.data.local.dao.CallScreeningLogDao
import id.calltag.app.data.local.dao.EmergencyContactDao
import id.calltag.app.data.local.dao.PhoneTagDao
import id.calltag.app.data.local.entity.CachedPhoneTag
import id.calltag.app.data.local.entity.CallScreeningLog
import id.calltag.app.data.local.entity.EmergencyContact
import id.calltag.app.data.preferences.CallTagPreferences
import id.calltag.app.data.remote.ApiService
import id.calltag.app.data.remote.dto.AuthLoginRequestDto
import id.calltag.app.data.remote.dto.DisputeRequestDto
import id.calltag.app.data.remote.dto.DisputeResponseDto
import id.calltag.app.data.remote.dto.EmergencyDirectoryDto
import id.calltag.app.data.remote.dto.LookupDataDto
import id.calltag.app.data.remote.dto.OfficialChannelDto
import id.calltag.app.data.remote.dto.PhoneTagDto
import id.calltag.app.data.remote.dto.ReportRequestDto
import id.calltag.app.data.remote.dto.ReportResponseDto
import id.calltag.app.data.remote.dto.ReputationLevels
import id.calltag.app.data.remote.dto.ScamCategories
import id.calltag.app.data.remote.dto.SpamReportItemDto
import id.calltag.app.util.PhoneNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.system.measureNanoTime

data class ScreeningLookupResult(
    val phoneTag: CachedPhoneTag,
    val source: String, // LOCAL_CACHE atau LIVE_API
    val lookupLatencyMs: Double = 0.0
)

data class FullPhoneLookupResult(
    val phoneTag: CachedPhoneTag,
    val categoryBreakdown: Map<String, Int>,
    val recentReports: List<SpamReportItemDto>,
    val source: String, // LOCAL_CACHE atau LIVE_API
    val lookupLatencyMs: Double
)

data class OfflineCacheStats(
    val syncVersion: Long,
    val cachedPhoneCount: Int,
    val emergencyContactsCount: Int
)

/**
 * Repository utama berarsitektur Offline-First yang menggabungkan Room Database (Local Cache)
 * dan Retrofit `ApiService` sesuai kontrak resmi Bagian 5.3 `CallTag/PRD.md`.
 */
class CallTagRepository(
    private val phoneTagDao: PhoneTagDao,
    private val emergencyContactDao: EmergencyContactDao,
    private val callScreeningLogDao: CallScreeningLogDao,
    private val apiService: ApiService,
    private val fastScreeningApiService: ApiService,
    private val preferences: CallTagPreferences
) {

    /**
     * Digunakan oleh CallTagScreeningService saat panggilan masuk berdering:
     * 1. Cek Room Database lokal terlebih dahulu (< 20 ms).
     * 2. Jika belum ada di Room DB dan perangkat online, panggil `GET /api/v1/phones/{e164}`
     *    dengan batas timeout ketat 800 ms, lalu simpan hasilnya ke Room DB.
     */
    suspend fun screenIncomingPhone(rawPhone: String): ScreeningLookupResult? {
        val e164 = PhoneNormalizer.normalizeToE164(rawPhone)
        if (e164.isEmpty()) return null

        var cached: CachedPhoneTag? = null
        val elapsedNanos = measureNanoTime {
            cached = phoneTagDao.getByPhone(e164)
        }
        val localMs = (elapsedNanos / 1_000_000.0)
        if (cached != null) {
            return ScreeningLookupResult(
                phoneTag = cached!!,
                source = "LOCAL_CACHE",
                lookupLatencyMs = localMs
            )
        }

        // Batas timeout ketat 800 ms ke endpoint resmi GET /api/v1/phones/{e164}
        val remoteDto = withTimeoutOrNull(800L) {
            runCatching {
                fastScreeningApiService.getPhoneDetails(e164).data
            }.getOrNull()
        } ?: return null

        val entity = mapLookupDtoToEntity(e164, remoteDto)
        phoneTagDao.upsert(entity)
        return ScreeningLookupResult(
            phoneTag = entity,
            source = "LIVE_API",
            lookupLatencyMs = localMs
        )
    }

    /**
     * Pencarian manual nomor telepon dari HomeScreen:
     * Menormalisasi input `08...` ke `+628...`, membaca cache lokal Room SQLite (< 20 ms),
     * serta memanggil `GET /api/v1/phones/{e164}` dan `GET /api/v1/phones/{e164}/reports` bila online.
     */
    suspend fun lookupPhoneFull(rawPhone: String): FullPhoneLookupResult? {
        val e164 = PhoneNormalizer.normalizeToE164(rawPhone)
        if (e164.isEmpty()) return null

        var localTag: CachedPhoneTag? = null
        val localNanos = measureNanoTime {
            localTag = phoneTagDao.getByPhone(e164)
        }
        val localLatencyMs = (localNanos / 1_000_000.0)

        val remoteDetails = withTimeoutOrNull(1800L) {
            runCatching {
                apiService.getPhoneDetails(e164).data
            }.getOrNull()
        }
        val remoteReports = withTimeoutOrNull(1800L) {
            runCatching {
                apiService.getPhoneReports(e164).data
            }.getOrNull()
        }

        if (remoteDetails != null && remoteDetails.found) {
            val entity = mapLookupDtoToEntity(e164, remoteDetails)
            phoneTagDao.upsert(entity)
            val reportsList = remoteReports?.reports?.ifEmpty { remoteDetails.recentReports }
                ?: remoteDetails.recentReports
            return FullPhoneLookupResult(
                phoneTag = entity,
                categoryBreakdown = remoteDetails.categoryBreakdown.ifEmpty {
                    OfflineSeedData.getOfflineBreakdownForPhone(entity)
                },
                recentReports = reportsList.ifEmpty {
                    OfflineSeedData.getOfflineCommentsForPhone(entity)
                },
                source = "LIVE_API",
                lookupLatencyMs = localLatencyMs
            )
        }

        val currentLocal = localTag
        if (currentLocal != null) {
            return FullPhoneLookupResult(
                phoneTag = currentLocal,
                categoryBreakdown = OfflineSeedData.getOfflineBreakdownForPhone(currentLocal),
                recentReports = OfflineSeedData.getOfflineCommentsForPhone(currentLocal),
                source = "LOCAL_CACHE",
                lookupLatencyMs = localLatencyMs
            )
        }

        val cleanTag = CachedPhoneTag(
            phoneE164 = e164,
            carrier = "Operator Telekomunikasi Indonesia",
            reputationLevel = ReputationLevels.CLEAN,
            spamScore = 0.0,
            isDanger = false,
            primaryCategory = ScamCategories.TELEMARKETING,
            primaryCategoryLabel = "Nomor Bersih / Belum Ada Laporan",
            topCommunityLabel = "Tidak Ditemukan Indikasi Scam",
            tagsSummary = "",
            uniqueReporters = 0,
            syncVersion = 0L
        )
        return FullPhoneLookupResult(
            phoneTag = cleanTag,
            categoryBreakdown = mapOf(
                ScamCategories.PINJOL_DC to 0,
                ScamCategories.APK_SNIFFING to 0,
                ScamCategories.IMPERSONATION to 0,
                ScamCategories.JUDOL to 0,
                ScamCategories.SOCIAL_ENG to 0,
                ScamCategories.TELEMARKETING to 0
            ),
            recentReports = emptyList(),
            source = "LOCAL_CACHE",
            lookupLatencyMs = localLatencyMs
        )
    }

    /**
     * Mengirim laporan spam/scam & tag nama baru ke `POST /api/v1/reports`.
     */
    suspend fun submitSpamReport(
        phone: String,
        categoryCode: String,
        tagName: String,
        comment: String,
        callType: String = "CELLULAR_CALL",
        deviceId: String = "android-calltag"
    ): Result<ReportResponseDto> = runCatching {
        val normalizedE164 = PhoneNormalizer.normalizeToE164(phone)
        require(normalizedE164.length >= 8) { "Nomor telepon tidak valid (gunakan format 08xx atau +628xx)" }

        val validCat = if (categoryCode.uppercase() in ScamCategories.ALL) {
            categoryCode.uppercase()
        } else {
            ScamCategories.TELEMARKETING
        }

        val token = preferences.getAuthToken()
        val bearer = if (token.isNotBlank()) "Bearer $token" else null
        val req = ReportRequestDto(
            phone = normalizedE164,
            categoryCode = validCat,
            customTag = tagName,
            comment = comment,
            callType = callType
        )

        val remoteResult = runCatching {
            apiService.submitReport(
                bearerToken = bearer,
                deviceId = deviceId,
                request = req
            ).data
        }.getOrNull()

        if (remoteResult != null) {
            val pt = remoteResult.phoneTag
            val cached = CachedPhoneTag(
                phoneE164 = pt.phoneE164.ifBlank { normalizedE164 },
                carrier = pt.carrier.ifBlank { "Operator Telekomunikasi Indonesia" },
                reputationLevel = pt.reputationLevel,
                spamScore = pt.spamScore,
                isDanger = pt.isDanger,
                primaryCategory = pt.primaryCategory,
                primaryCategoryLabel = pt.primaryCategoryLabel.ifBlank {
                    PhoneNormalizer.getCategoryDisplayLabel(pt.primaryCategory)
                },
                topCommunityLabel = pt.topCommunityLabel.ifBlank { tagName },
                tagsSummary = pt.tags.joinToString(", ") { "${it.tag} (${it.count})" },
                uniqueReporters = pt.uniqueReporters,
                syncVersion = pt.syncVersion
            )
            phoneTagDao.upsert(cached)
            return@runCatching remoteResult
        }

        // Offline Fallback: Hitung agregasi lokal di Room SQLite
        val existing = phoneTagDao.getByPhone(normalizedE164)
        val newReporters = (existing?.uniqueReporters ?: 0) + 1
        val addedPoints = when (validCat) {
            ScamCategories.APK_SNIFFING -> 16.0
            ScamCategories.IMPERSONATION, ScamCategories.SOCIAL_ENG -> 14.4
            ScamCategories.PINJOL_DC -> 12.0
            ScamCategories.JUDOL -> 10.4
            else -> 8.0
        }
        val newScore = ((existing?.spamScore ?: 0.0) + addedPoints).coerceAtMost(100.0)
        val newLevel = when {
            newScore >= 70.0 && newReporters >= 5 -> ReputationLevels.DANGER_SCAM
            newScore >= 40.0 && newReporters >= 3 -> ReputationLevels.SPAM
            newScore >= 15.0 -> ReputationLevels.SUSPICIOUS
            else -> ReputationLevels.SUSPICIOUS
        }
        val nextSync = phoneTagDao.getLatestSyncVersion() + 1L
        val localUpdated = CachedPhoneTag(
            phoneE164 = normalizedE164,
            carrier = existing?.carrier ?: "Operator Telekomunikasi Indonesia",
            reputationLevel = newLevel,
            spamScore = newScore,
            isDanger = newLevel == ReputationLevels.DANGER_SCAM || newLevel == ReputationLevels.SPAM,
            primaryCategory = validCat,
            primaryCategoryLabel = PhoneNormalizer.getCategoryDisplayLabel(validCat),
            topCommunityLabel = tagName,
            tagsSummary = "$tagName ($newReporters)",
            uniqueReporters = newReporters,
            syncVersion = nextSync
        )
        phoneTagDao.upsert(localUpdated)

        val recommended = OfflineSeedData.getInitialOfficialChannels().filter {
            validCat in it.relatedCategories
        }.ifEmpty { OfflineSeedData.getInitialOfficialChannels() }

        ReportResponseDto(
            reportId = "${normalizedE164}_local_$nextSync",
            phoneTag = PhoneTagDto(
                phoneE164 = localUpdated.phoneE164,
                carrier = localUpdated.carrier,
                reputationLevel = localUpdated.reputationLevel,
                spamScore = localUpdated.spamScore,
                primaryCategory = localUpdated.primaryCategory,
                primaryCategoryLabel = localUpdated.primaryCategoryLabel,
                isDanger = localUpdated.isDanger,
                topCommunityLabel = localUpdated.topCommunityLabel,
                uniqueReporters = localUpdated.uniqueReporters,
                totalReports = localUpdated.uniqueReporters,
                syncVersion = localUpdated.syncVersion
            ),
            recommendedOfficialHub = recommended
        )
    }

    /**
     * Mengajukan sanggahan (dispute) nomor telepon ke `POST /api/v1/disputes` (US-05).
     */
    suspend fun submitDispute(
        phone: String,
        ownerName: String,
        reason: String,
        contactRef: String
    ): Result<DisputeResponseDto> = runCatching {
        val e164 = PhoneNormalizer.normalizeToE164(phone)
        require(e164.length >= 8) { "Nomor telepon yang diajukan tidak valid" }
        require(reason.isNotBlank()) { "Alasan sanggahan wajib diisi" }

        val token = preferences.getAuthToken()
        val bearer = if (token.isNotBlank()) "Bearer $token" else null
        val req = DisputeRequestDto(
            phone = e164,
            ownerName = ownerName,
            reason = reason,
            contactRef = contactRef
        )

        val remote = runCatching {
            apiService.submitDispute(bearerToken = bearer, request = req).data
        }.getOrNull()

        remote ?: DisputeResponseDto(
            disputeId = "DSP_${e164.removePrefix("+")}",
            phoneE164 = e164,
            ownerName = ownerName,
            status = "UNDER_REVIEW",
            submittedAt = "Tersimpan di Antrean Peninjauan CallTag"
        )
    }

    /**
     * Sinkronisasi Delta Offline Bundle dari `GET /api/v1/sync/offline-bundle?since_version=...`
     */
    suspend fun syncTopScammersToLocalCache(): Result<OfflineCacheStats> = runCatching {
        val sinceVersion = phoneTagDao.getLatestSyncVersion()
        val resp = apiService.syncOfflineBundle(sinceVersion = sinceVersion, limit = 250)
        val data = resp.data

        val items = data?.items.orEmpty()
        if (items.isNotEmpty()) {
            val entities = items.map { dto ->
                CachedPhoneTag(
                    phoneE164 = dto.phoneE164,
                    carrier = "Operator Telekomunikasi Indonesia",
                    reputationLevel = dto.reputationLevel,
                    spamScore = dto.spamScore,
                    isDanger = dto.isDanger,
                    primaryCategory = dto.primaryCategory,
                    primaryCategoryLabel = dto.primaryCategoryLabel.ifBlank {
                        PhoneNormalizer.getCategoryDisplayLabel(dto.primaryCategory)
                    },
                    topCommunityLabel = dto.topCommunityLabel,
                    tagsSummary = dto.topCommunityLabel,
                    uniqueReporters = 5,
                    syncVersion = dto.syncVersion
                )
            }
            phoneTagDao.upsertAll(entities)
        }

        val natItems = data?.nationalEmergency.orEmpty()
        if (natItems.isNotEmpty()) {
            emergencyContactDao.upsertAll(natItems.map { mapEmergencyDtoToEntity(it) })
        }

        getLocalCacheStats()
    }

    suspend fun getLocalCacheStats(): OfflineCacheStats {
        if (emergencyContactDao.count() == 0) {
            emergencyContactDao.upsertAll(OfflineSeedData.getInitialEmergencyContacts())
        }
        if (phoneTagDao.count() == 0) {
            phoneTagDao.upsertAll(OfflineSeedData.getInitialTopScammers())
        }
        return OfflineCacheStats(
            syncVersion = phoneTagDao.getLatestSyncVersion(),
            cachedPhoneCount = phoneTagDao.count(),
            emergencyContactsCount = emergencyContactDao.count()
        )
    }

    /**
     * Mengambil daftar kanal pelaporan resmi pemerintah (`GET /api/v1/official-channels`)
     */
    suspend fun getOfficialChannels(category: String? = null): List<OfficialChannelDto> {
        val remote = withTimeoutOrNull(1500L) {
            runCatching {
                apiService.getOfficialChannels(category = category).data
            }.getOrNull()
        }
        if (!remote.isNullOrEmpty()) return remote

        val seeded = OfflineSeedData.getInitialOfficialChannels()
        if (category.isNullOrBlank()) return seeded
        return seeded.filter { category in it.relatedCategories }.ifEmpty { seeded }
    }

    /**
     * Sinkronisasi direktori darurat Nasional (`GET /api/v1/emergency/national`)
     * & Lokal (`GET /api/v1/emergency/local`) ke Room DB.
     */
    suspend fun syncEmergencyDirectory(
        city: String? = null,
        province: String? = null
    ): Result<List<OfficialChannelDto>> = runCatching {
        val natResp = runCatching { apiService.getNationalEmergencyDirectory().data }.getOrNull()
        val locResp = apiService.getLocalEmergencyDirectory(city = city, province = province).data
            ?: error("Data direktori darurat kosong")

        val allContacts = buildList {
            addAll(natResp?.nationalNumbers.orEmpty().map { mapEmergencyDtoToEntity(it) })
            addAll(locResp.nationalNumbers.map { mapEmergencyDtoToEntity(it) })
            addAll(locResp.localDirectory.map { mapEmergencyDtoToEntity(it) })
        }
        if (allContacts.isNotEmpty()) {
            emergencyContactDao.upsertAll(allContacts)
        }
        locResp.officialChannels.ifEmpty { OfflineSeedData.getInitialOfficialChannels() }
    }

    /**
     * Registrasi perangkat & autentikasi JWT ke `POST /api/v1/auth/device`.
     */
    suspend fun authenticateDevice(
        phone: String,
        displayName: String,
        deviceId: String
    ): Result<String> = runCatching {
        val resp = apiService.registerOrAuthenticateDevice(
            AuthLoginRequestDto(
                phone = PhoneNormalizer.normalizeToE164(phone),
                displayName = displayName,
                deviceId = deviceId
            )
        )
        val token = resp.data?.token.orEmpty()
        if (token.isNotBlank()) {
            preferences.saveAuthToken(token)
        }
        token
    }

    fun observeNationalEmergencyContacts(): Flow<List<EmergencyContact>> =
        emergencyContactDao.observeNationalContacts()

    fun observeLocalEmergencyContacts(
        province: String = "",
        city: String = "",
        agencyType: String = ""
    ): Flow<List<EmergencyContact>> =
        emergencyContactDao.observeLocalContacts(province, city, agencyType)

    fun observeDangerScammers(): Flow<List<CachedPhoneTag>> =
        phoneTagDao.observeDangerPhoneTags()

    fun observeRecentScreeningLogs(): Flow<List<CallScreeningLog>> =
        callScreeningLogDao.observeRecentLogs()

    suspend fun recordScreeningLog(log: CallScreeningLog) {
        callScreeningLogDao.insert(log)
    }

    private fun mapLookupDtoToEntity(e164: String, dto: LookupDataDto): CachedPhoneTag {
        val label = dto.topCommunityLabel.ifBlank {
            PhoneNormalizer.getCategoryDisplayLabel(dto.primaryCategory)
        }
        return CachedPhoneTag(
            phoneE164 = dto.phoneE164.ifBlank { e164 },
            carrier = dto.carrier.ifBlank { "Operator Telekomunikasi Indonesia" },
            reputationLevel = dto.reputationLevel.ifBlank { ReputationLevels.CLEAN },
            spamScore = dto.spamScore,
            isDanger = dto.isDanger,
            primaryCategory = dto.primaryCategory,
            primaryCategoryLabel = dto.primaryCategoryLabel.ifBlank {
                PhoneNormalizer.getCategoryDisplayLabel(dto.primaryCategory)
            },
            topCommunityLabel = label,
            tagsSummary = dto.tags.joinToString(", ") { "${it.tag} (${it.count})" },
            uniqueReporters = dto.uniqueReporters,
            syncVersion = 0L
        )
    }

    private fun mapEmergencyDtoToEntity(dto: EmergencyDirectoryDto): EmergencyContact {
        val phones = dto.contacts.phoneNumbers
        val primary = phones.firstOrNull() ?: dto.contacts.shortCode.orEmpty()
        val secondary = phones.drop(1).firstOrNull().orEmpty()
        return EmergencyContact(
            code = dto.code,
            scope = dto.scope,
            agencyType = dto.agencyType,
            name = dto.name,
            province = dto.province.orEmpty(),
            cityRegency = dto.cityRegency.orEmpty(),
            shortCode = dto.contacts.shortCode.orEmpty(),
            primaryPhone = primary,
            secondaryPhone = secondary,
            whatsappNumber = dto.contacts.whatsappNumber.orEmpty(),
            whatsappTemplate = dto.contacts.whatsappTemplate.orEmpty(),
            capabilitiesSummary = dto.capabilities.joinToString(" • "),
            address = dto.address.orEmpty(),
            is24Hours = dto.is24Hours
        )
    }
}
