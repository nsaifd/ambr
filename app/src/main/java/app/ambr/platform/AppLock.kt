package app.ambr.platform

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom

/** Locks the app behind a PIN (and fingerprint, when the phone has one). */
class AppLock(private val settings: Settings) {
    private val _locked = MutableStateFlow(settings.appLock.value && settings.pinHash != null)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()
    private var backgroundAt = 0L
    private var ownTrip = false

    val enabled get() = settings.appLock.value && settings.pinHash != null

    /** The app is opening a picker, share sheet or dialer itself; coming back from it doesn't lock. */
    fun leavingForResult() { ownTrip = true }

    fun onBackground() { backgroundAt = SystemClock.elapsedRealtime() }

    fun onForeground() {
        val trip = ownTrip
        ownTrip = false
        if (!enabled || backgroundAt == 0L) return
        // Still lock after a long trip away, even one the app started.
        if (trip && SystemClock.elapsedRealtime() - backgroundAt < 10 * 60_000L) return
        if (SystemClock.elapsedRealtime() - backgroundAt >= settings.lockDelaySeconds * 1000L) _locked.value = true
    }

    fun check(pin: String): Boolean = (hash(pin, salt()) == settings.pinHash?.substringAfter(':')).also { if (it) _locked.value = false }

    fun unlock() { _locked.value = false }

    fun turnOn(pin: String, biometric: Boolean, delaySeconds: Int) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        settings.pinHash = "$salt:${hash(pin, salt)}"
        settings.useBiometric = biometric
        settings.lockDelaySeconds = delaySeconds
        settings.setAppLock(true)
    }

    fun turnOff() {
        settings.setAppLock(false)
        settings.pinHash = null
        _locked.value = false
    }

    private fun salt() = settings.pinHash?.substringBefore(':').orEmpty()

    private fun hash(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).joinToString("") { "%02x".format(it) }
}
