package dev.olog.media

import androidx.lifecycle.LiveData
import dev.olog.core.MediaId
import dev.olog.core.entity.sort.SortEntity
import dev.olog.media.model.*
import kotlinx.coroutines.flow.Flow

interface MediaProvider {

    fun observeMetadata(): LiveData<PlayerMetadata>
    fun observePlaybackState(): LiveData<PlayerPlaybackState>
    fun observeRepeat(): LiveData<PlayerRepeatMode>
    fun observeShuffle(): LiveData<PlayerShuffleMode>
    // is a flow instead of livedata because list operations may be expensive, so they can be
    // moved to a background thread
    fun observeQueue(): Flow<List<PlayerItem>>
    fun observeQueueTitle(): LiveData<String>

    fun playFromMediaId(mediaId: MediaId, sort: SortEntity? = null)
    fun playMostPlayed(mediaId: MediaId)
    fun playRecentlyAdded(mediaId: MediaId)

    fun skipToQueueItem(idInPlaylist: Long)
    fun shuffle(mediaId: MediaId)
    fun skipToNext()
    fun skipToPrevious()
    fun playPause()
    fun seekTo(where: Long)
    fun toggleShuffleMode()
    fun toggleRepeatMode()

    fun addToPlayNext(mediaId: MediaId)
    fun moveToPlayNext(mediaId: MediaId)

    fun togglePlayerFavorite()

    fun swap(from: Int, to: Int)
    fun swapRelative(from: Int, to: Int)

    fun remove(position: Int)
    fun removeRelative(position: Int)

    fun replayTenSeconds()
    fun forwardTenSeconds()

    fun replayThirtySeconds()
    fun forwardThirtySeconds()

}