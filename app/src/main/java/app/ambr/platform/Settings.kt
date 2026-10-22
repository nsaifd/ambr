package app.ambr.platform

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small app preferences, exposed as flows so screens update when they change. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("ambr", Context.MODE_PRIVATE)

    private val _isPro = MutableStateFlow(prefs.getBoolean(KEY_PRO, false))
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _firstReminderDays = MutableStateFlow(prefs.getInt(KEY_FIRST, 30))
    val firstReminderDays: StateFlow<Int> = _firstReminderDays.asStateFlow()

    private val _secondReminder = MutableStateFlow(prefs.getBoolean(KEY_SECOND, true))
    val secondReminder: StateFlow<Boolean> = _secondReminder.asStateFlow()

    private val _appLock = MutableStateFlow(prefs.getBoolean(KEY_LOCK, false))
    val appLock: StateFlow<Boolean> = _appLock.asStateFlow()

    private val _backupOn = MutableStateFlow(prefs.getBoolean(KEY_BACKUP, false))
    val backupOn: StateFlow<Boolean> = _backupOn.asStateFlow()

    private val _lastBackup = MutableStateFlow(prefs.getLong(KEY_LAST_BACKUP, 0L))
    val lastBackup: StateFlow<Long> = _lastBackup.asStateFlow()

    var welcomed: Boolean
        get() = prefs.getBoolean(KEY_WELCOMED, false)
        set(v) = prefs.edit().putBoolean(KEY_WELCOMED, v).apply()

    fun setPro(v: Boolean) { prefs.edit().putBoolean(KEY_PRO, v).apply(); _isPro.value = v }
    fun setFirstReminderDays(v: Int) { prefs.edit().putInt(KEY_FIRST, v).apply(); _firstReminderDays.value = v }
    fun setSecondReminder(v: Boolean) { prefs.edit().putBoolean(KEY_SECOND, v).apply(); _secondReminder.value = v }
    fun setAppLock(v: Boolean) { prefs.edit().putBoolean(KEY_LOCK, v).apply(); _appLock.value = v }
    fun setBackupOn(v: Boolean) { prefs.edit().putBoolean(KEY_BACKUP, v).apply(); _backupOn.value = v }
    fun setLastBackup(v: Long) { prefs.edit().putLong(KEY_LAST_BACKUP, v).apply(); _lastBackup.value = v }

    companion object {
        const val FREE_LIMIT = 10
        private const val KEY_PRO = "pro"
        private const val KEY_FIRST = "reminder_first_days"
        private const val KEY_SECOND = "reminder_second"
        private const val KEY_LOCK = "app_lock"
        private const val KEY_BACKUP = "backup_on"
        private const val KEY_LAST_BACKUP = "last_backup"
        private const val KEY_WELCOMED = "welcomed"
    }
}
