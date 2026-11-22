package id.calltag.app

import android.app.Application
import id.calltag.app.data.local.CallTagDatabase
import id.calltag.app.data.preferences.CallTagPreferences
import id.calltag.app.data.remote.NetworkModule
import id.calltag.app.data.repository.CallTagRepository
import id.calltag.app.service.CallerIdNotifier
import id.calltag.app.work.PendingReportWorker
import id.calltag.app.work.SyncWorker

class CallTagApplication : Application() {

    val database: CallTagDatabase by lazy {
        CallTagDatabase.getInstance(this)
    }

    val preferences: CallTagPreferences by lazy {
        CallTagPreferences(this)
    }

    val repository: CallTagRepository by lazy {
        NetworkModule.init(preferences)
        CallTagRepository(
            phoneTagDao = database.phoneTagDao(),
            emergencyContactDao = database.emergencyContactDao(),
            callScreeningLogDao = database.callScreeningLogDao(),
            apiService = NetworkModule.apiService,
            fastScreeningApiService = NetworkModule.fastScreeningApiService,
            preferences = preferences,
            pendingReportDao = database.pendingReportDao(),
            pendingDisputeDao = database.pendingDisputeDao(),
            syncStateDao = database.syncStateDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        NetworkModule.init(preferences)
        // Inisialisasi database (memicu offline seed awal) & notification channel
        database
        CallerIdNotifier.ensureNotificationChannel(this)
        SyncWorker.schedulePeriodic(this)
        PendingReportWorker.enqueue(this)
    }
}
