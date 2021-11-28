package dev.olog.msc.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.color.ColorCallback
import com.afollestad.materialdialogs.color.colorChooser
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.olog.core.MediaIdCategory
import dev.olog.core.prefs.TutorialPreferenceGateway
import dev.olog.feature.lastm.fm.LastFmCredentialsFragment
import dev.olog.feature.library.LibraryPage
import dev.olog.feature.library.LibraryPrefs
import dev.olog.feature.library.blacklist.BlacklistFragment
import dev.olog.feature.library.category.LibraryCategoriesFragment
import dev.olog.feature.main.MainPrefs
import dev.olog.image.provider.GlideApp
import dev.olog.image.provider.creator.ImagesFolderUtils
import dev.olog.msc.R
import dev.olog.scrollhelper.layoutmanagers.OverScrollLinearLayoutManager
import dev.olog.shared.android.extensions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat(),
    ColorCallback,
    SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        @JvmStatic
        val TAG = SettingsFragment::class.java.name
    }

    @Inject
    internal lateinit var tutorialPrefsUseCase: TutorialPreferenceGateway

    @Inject
    lateinit var libraryPrefs: LibraryPrefs
    @Inject
    lateinit var mainPrefs: MainPrefs

    private lateinit var libraryCategories: Preference
    private lateinit var podcastCategories: Preference
    private lateinit var blacklist: Preference
    private lateinit var iconShape: Preference
    private lateinit var deleteCache: Preference
    private lateinit var lastFmCredentials: Preference
    private lateinit var autoCreateImages: Preference
    private lateinit var accentColorChooser: Preference
    private lateinit var resetTutorial: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs, rootKey)
        libraryCategories = preferenceScreen.findPreference(getString(prefs.R.string.prefs_library_categories_key))!!
        podcastCategories = preferenceScreen.findPreference(getString(prefs.R.string.prefs_podcast_library_categories_key))!!
        blacklist = preferenceScreen.findPreference(getString(prefs.R.string.prefs_blacklist_key))!!
        iconShape = preferenceScreen.findPreference(getString(prefs.R.string.prefs_icon_shape_key))!!
        deleteCache = preferenceScreen.findPreference(getString(prefs.R.string.prefs_delete_cached_images_key))!!
        lastFmCredentials = preferenceScreen.findPreference(getString(prefs.R.string.prefs_last_fm_credentials_key))!!
        autoCreateImages = preferenceScreen.findPreference(getString(prefs.R.string.prefs_auto_create_images_key))!!
        accentColorChooser = preferenceScreen.findPreference(getString(prefs.R.string.prefs_color_accent_key))!!
        resetTutorial = preferenceScreen.findPreference(getString(prefs.R.string.prefs_reset_tutorial_key))!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = view.findViewById<RecyclerView>(androidx.preference.R.id.recycler_view)
        list.layoutManager = OverScrollLinearLayoutManager(list)
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        libraryCategories.setOnPreferenceClickListener {
            LibraryCategoriesFragment.newInstance(MediaIdCategory.SONGS)
                .show(activity!!.supportFragmentManager, LibraryCategoriesFragment.TAG)
            true
        }
        podcastCategories.setOnPreferenceClickListener {
            LibraryCategoriesFragment.newInstance(MediaIdCategory.PODCASTS)
                .show(activity!!.supportFragmentManager, LibraryCategoriesFragment.TAG)
            true
        }
        blacklist.setOnPreferenceClickListener {
            act.fragmentTransaction {
                setReorderingAllowed(true)
                add(BlacklistFragment(), BlacklistFragment.TAG)
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            }
            true
        }

        deleteCache.setOnPreferenceClickListener {
            showDeleteAllCacheDialog()
            true
        }
        lastFmCredentials.setOnPreferenceClickListener {
            act.fragmentTransaction {
                setReorderingAllowed(true)
                add(LastFmCredentialsFragment.newInstance(), LastFmCredentialsFragment.TAG)
            }
            true
        }
        accentColorChooser.setOnPreferenceClickListener {
            MaterialDialog(act)
                .colorChooser(
                    colors = ColorPalette.getAccentColors(ctx.isDarkMode()),
                    subColors = ColorPalette.getAccentColorsSub(ctx.isDarkMode()),
                    initialSelection = mainPrefs.accentColor.get(),
                    selection = this
                ).show()
            true
        }
        resetTutorial.setOnPreferenceClickListener {
            showResetTutorialDialog()
            true
        }
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        libraryCategories.onPreferenceClickListener = null
        podcastCategories.onPreferenceClickListener = null
        blacklist.onPreferenceClickListener = null
        deleteCache.onPreferenceClickListener = null
        lastFmCredentials.onPreferenceClickListener = null
        accentColorChooser.onPreferenceClickListener = null
        resetTutorial.onPreferenceClickListener = null
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            getString(prefs.R.string.prefs_folder_tree_view_key) -> {
                act.recreate()
            }
            getString(prefs.R.string.prefs_show_podcasts_key) -> {
                libraryPrefs.setLibraryPage(LibraryPage.TRACKS)
                act.recreate()
            }
        }
    }

    private fun showDeleteAllCacheDialog() {
        MaterialAlertDialogBuilder(ctx)
            .setTitle(localization.R.string.prefs_delete_cached_images_title)
            .setMessage(localization.R.string.are_you_sure)
            .setPositiveButton(localization.R.string.popup_positive_ok) { _, _ -> lifecycleScope.launch { clearGlideCache() } }
            .setNegativeButton(localization.R.string.popup_negative_no, null)
            .show()
    }

    private suspend fun clearGlideCache() {
        GlideApp.get(ctx.applicationContext).clearMemory()

        withContext(Dispatchers.IO) {
            GlideApp.get(ctx.applicationContext).clearDiskCache()
            ImagesFolderUtils.getImageFolderFor(ctx, ImagesFolderUtils.FOLDER).listFiles()
                ?.forEach { it.delete() }
            ImagesFolderUtils.getImageFolderFor(ctx, ImagesFolderUtils.PLAYLIST).listFiles()
                ?.forEach { it.delete() }
            ImagesFolderUtils.getImageFolderFor(ctx, ImagesFolderUtils.GENRE).listFiles()
                ?.forEach { it.delete() }
        }
        ctx.applicationContext.toast(localization.R.string.prefs_delete_cached_images_success)
    }

    private fun showResetTutorialDialog() {
        MaterialAlertDialogBuilder(ctx)
            .setTitle(localization.R.string.prefs_reset_tutorial_title)
            .setMessage(localization.R.string.are_you_sure)
            .setPositiveButton(localization.R.string.popup_positive_ok) { _, _ -> tutorialPrefsUseCase.reset() }
            .setNegativeButton(localization.R.string.popup_negative_no, null)
            .show()
    }

    override fun invoke(dialog: MaterialDialog, color: Int) {
        val realColor = ColorPalette.getRealAccentSubColor(ctx.isDarkMode(), color)
        mainPrefs.accentColor.set(realColor)
        act.recreate()
    }
}