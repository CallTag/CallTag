package id.calltag.app

import android.app.Application
import id.calltag.app.data.local.CallTagDatabase
import id.calltag.app.data.preferences.CallTagPreferences
import id.calltag.app.data.remote.NetworkModule
import id.calltag.app.data.repository.CallTagRepository
import id.calltag.app.service.CallerIdNotifier

class CallTagApplication : Application() {

    val database: CallTagDatabase by lazy {
        CallTagDatabase.getInstance(this)
    }

    val preferences: CallTagPreferences by lazy {
        CallTagPreferences(this)
    }

    val repository: CallTagRepository by lazy {
        CallTagRepository(
            phoneTagDao = database.phoneTagDao(),
            emergencyContactDao = database.emergencyContactDao(),
            callScreeningLogDao = database.callScreeningLogDao(),
            apiService = NetworkModule.apiService,
            fastScreeningApiService = NetworkModule.fastScreeningApiService,
            preferences = preferences
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Inisialisasi database (memicu offline seed awal) & notification channel
        database
        CallerIdNotifier.ensureNotificationChannel(this)
    }
}
