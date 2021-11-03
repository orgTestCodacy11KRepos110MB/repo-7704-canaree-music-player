package dev.olog.feature.library.tab

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.olog.core.MediaId
import dev.olog.core.entity.sort.SortEntity
import dev.olog.core.prefs.SortPreferences
import dev.olog.feature.base.model.DisplayableItem
import dev.olog.feature.library.LibraryPrefs
import dev.olog.feature.library.TabCategory
import dev.olog.shared.android.extensions.asLiveData
import javax.inject.Inject

@HiltViewModel
class TabFragmentViewModel @Inject constructor(
    private val dataProvider: TabDataProvider,
    private val appPreferencesUseCase: SortPreferences,
    private val libraryPrefs: LibraryPrefs,

    ) : ViewModel() {

    private val liveDataMap: MutableMap<TabCategory, LiveData<List<DisplayableItem>>> =
        mutableMapOf()

    fun observeData(category: TabCategory): LiveData<List<DisplayableItem>> {
        var liveData = liveDataMap[category]
        if (liveData == null) {
            liveData = dataProvider.get(category).asLiveData()
        }
        return liveData
    }

    fun getAllTracksSortOrder(mediaId: MediaId): SortEntity? {
        if (mediaId.isAnyPodcast) {
            return null
        }
        return appPreferencesUseCase.getAllTracksSort()
    }

    fun getAllAlbumsSortOrder(): SortEntity {
        return appPreferencesUseCase.getAllAlbumsSort()
    }

    fun getAllArtistsSortOrder(): SortEntity {
        return appPreferencesUseCase.getAllArtistsSort()
    }

    fun getSpanCount(category: TabCategory) = libraryPrefs.spanCount(category).get()
    fun observeSpanCount(category: TabCategory) = libraryPrefs.spanCount(category).observe()

}