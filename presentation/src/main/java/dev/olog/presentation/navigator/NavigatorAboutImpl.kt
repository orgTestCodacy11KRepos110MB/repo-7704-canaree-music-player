package dev.olog.presentation.navigator

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import dev.olog.platform.BottomNavigationFragmentTag
import dev.olog.platform.PlayStoreUtils
import dev.olog.platform.allowed
import dev.olog.platform.superCerealTransition
import dev.olog.presentation.R
import dev.olog.presentation.license.LicensesFragment
import dev.olog.presentation.thanks.SpecialThanksFragment
import dev.olog.presentation.translations.TranslationsFragment
import dev.olog.shared.extension.isIntentSafe
import dev.olog.shared.extension.toast
import dev.olog.ui.colorSurface
import saschpe.android.customtabs.CustomTabsHelper
import javax.inject.Inject

class NavigatorAboutImpl @Inject internal constructor(
    private val activity: FragmentActivity,
    private val tags: Set<@JvmSuppressWildcards BottomNavigationFragmentTag>,
) : NavigatorAbout {

    private val callback = object : CustomTabsHelper.CustomTabFallback {
        override fun openUri(context: Context?, uri: Uri?) {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (activity.packageManager.isIntentSafe(intent)) {
                activity.startActivity(intent)
            } else {
                activity.toast(R.string.common_browser_not_found)
            }
        }
    }

    override fun toHavocPage() {
        if (allowed()) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://play.google.com/store/apps/details?id=dev.olog.havoc")
            if (activity.packageManager.isIntentSafe(intent)) {
                activity.startActivity(intent)
            } else {
                activity.toast(R.string.common_browser_not_found)
            }
        }
    }

    override fun toLicensesFragment() {
        superCerealTransition(
            activity = activity, fragment = LicensesFragment(),
            tag = LicensesFragment.TAG,
            tags = tags,
            transition = FragmentTransaction.TRANSIT_FRAGMENT_CLOSE,
        )
    }

    override fun toChangelog() {
        val customTabIntent = CustomTabsIntent.Builder()
            .enableUrlBarHiding()
            .setToolbarColor(activity.colorSurface())
            .build()
        CustomTabsHelper.addKeepAliveExtra(activity, customTabIntent.intent)

        val uri = Uri.parse("https://github.com/ologe/canaree-music-player/blob/master/CHANGELOG.md")
        CustomTabsHelper.openCustomTab(activity, customTabIntent, uri, callback)
    }

    override fun toGithub() {
        val customTabIntent = CustomTabsIntent.Builder()
            .enableUrlBarHiding()
            .setToolbarColor(activity.colorSurface())
            .build()
        CustomTabsHelper.addKeepAliveExtra(activity, customTabIntent.intent)

        val uri = Uri.parse("https://github.com/ologe/canaree-music-player")
        CustomTabsHelper.openCustomTab(activity, customTabIntent, uri, callback)
    }

    override fun toSpecialThanksFragment() {
        superCerealTransition(
            activity = activity,
            fragment = SpecialThanksFragment(),
            tag = SpecialThanksFragment.TAG,
            tags = tags,
            transition = FragmentTransaction.TRANSIT_FRAGMENT_CLOSE
        )
    }

    override fun toMarket() {
        if (allowed()) {
            PlayStoreUtils.open(activity)
        }
    }

    override fun toPrivacyPolicy() {
        if (allowed()) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://deveugeniuolog.wixsite.com/next/privacy-policy")
            if (activity.packageManager.isIntentSafe(intent)) {
                activity.startActivity(intent)
            } else {
                activity.toast(R.string.common_browser_not_found)
            }
        }
    }

    override fun joinCommunity() {
        if (allowed()) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://www.reddit.com/r/canaree/")
            if (activity.packageManager.isIntentSafe(intent)) {
                activity.startActivity(intent)
            } else {
                activity.toast(R.string.common_browser_not_found)
            }
        }
    }

    override fun joinBeta() {
        if (allowed()) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://play.google.com/apps/testing/dev.olog.msc")
            if (activity.packageManager.isIntentSafe(intent)) {
                activity.startActivity(intent)
            } else {
                activity.toast(R.string.common_browser_not_found)
            }
        }
    }

    override fun toTranslations() {
        superCerealTransition(
            activity = activity,
            fragment = TranslationsFragment(),
            tag = TranslationsFragment.TAG,
            tags = tags,
            transition = FragmentTransaction.TRANSIT_FRAGMENT_CLOSE
        )
    }

    override fun requestTranslation() {
        if (allowed()) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://canaree.oneskyapp.com/collaboration/project/162621")
            if (activity.packageManager.isIntentSafe(intent)) {
                activity.startActivity(intent)
            } else {
                activity.toast(R.string.common_browser_not_found)
            }
        }
    }
}