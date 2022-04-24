package dev.olog.presentation.rateapp

import android.content.Context
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.olog.platform.PlayStoreUtils
import dev.olog.presentation.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject

private var counterAlreadyIncreased = false

private const val PREFS_APP_STARTED_COUNT = "prefs.app.started.count"
private const val PREFS_APP_RATE_NEVER_SHOW_AGAIN = "prefs.app.rate.never.show"

// todo replace with in-app rate
class RateAppDialog @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activity: FragmentActivity,
) {

    init {
        activity.lifecycleScope.launchWhenResumed {
            val show = updateCounter(activity)
            delay(2000)
            if (show) {
                showAlert()
            }
        }
    }

    private suspend fun showAlert() = withContext(Dispatchers.Main) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.rate_app_title)
            .setMessage(R.string.rate_app_message)
            .setPositiveButton(R.string.rate_app_positive_button) { _, _ ->
                setNeverShowAgain()
                PlayStoreUtils.open(activity)
            }
            .setNegativeButton(R.string.rate_app_negative_button) { _, _ -> setNeverShowAgain() }
            .setNeutralButton(R.string.rate_app_neutral_button) { _, _ -> }
            .setCancelable(false)
            .show()
    }

    /**
     * @return true when is requested to show rate dialog
     */
    private suspend fun updateCounter(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (!counterAlreadyIncreased) {
            counterAlreadyIncreased = true

            val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

            val oldValue = prefs.getInt(PREFS_APP_STARTED_COUNT, 0)
            val newValue = oldValue + 1
            prefs.edit { putInt(PREFS_APP_STARTED_COUNT, newValue) }

            newValue.rem(20) == 0 && !isNeverShowAgain()
        } else {
            false
        }
    }

    private fun isNeverShowAgain(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(PREFS_APP_RATE_NEVER_SHOW_AGAIN, false)
    }

    private fun setNeverShowAgain() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit { putBoolean(PREFS_APP_RATE_NEVER_SHOW_AGAIN, true) }
    }

}