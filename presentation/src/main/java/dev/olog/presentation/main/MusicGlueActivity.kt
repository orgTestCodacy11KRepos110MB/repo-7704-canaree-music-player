package dev.olog.presentation.main

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.CallSuper
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import dev.olog.analytics.TrackerFacade
import dev.olog.core.MediaId
import dev.olog.core.MediaIdModifier
import dev.olog.core.entity.sort.SortEntity
import dev.olog.intents.MusicServiceAction
import dev.olog.intents.MusicServiceCustomAction
import dev.olog.lib.media.MediaExposer
import dev.olog.lib.media.MediaProvider
import dev.olog.lib.media.connection.OnConnectionChanged
import dev.olog.lib.media.customAction
import dev.olog.lib.media.model.*
import dev.olog.lib.media.playPause
import dev.olog.presentation.base.BaseActivity
import dev.olog.shared.lazyFast
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

abstract class MusicGlueActivity : BaseActivity(),
    MediaProvider,
    OnConnectionChanged {

    @Inject
    internal lateinit var trackerFacade: TrackerFacade

    private val mediaExposer by lazyFast {
        MediaExposer(
            context = this,
            coroutineScope = lifecycleScope,
            onConnectionChanged = this
        )
    }

    fun connect() {
        mediaExposer.connect()
        trackerFacade.trackServiceEvent("connect")

    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        connect()
    }

    @CallSuper
    override fun onStop() {
        super.onStop()
        mediaExposer.disconnect()
        trackerFacade.trackServiceEvent("disconnect")
        unregisterMediaController()
    }

    override fun onConnectedSuccess(
        mediaBrowser: MediaBrowserCompat,
        callback: MediaControllerCompat.Callback
    ) {
        try {
            registerMediaController(mediaBrowser.sessionToken, callback)
            mediaExposer.initialize(MediaControllerCompat.getMediaController(this))
            trackerFacade.trackServiceEvent("onConnectedSuccess")
        } catch (ex: Throwable) {
            ex.printStackTrace()
            onConnectedFailed(mediaBrowser, callback)
        }
    }

    override fun onConnectedFailed(
        mediaBrowser: MediaBrowserCompat,
        callback: MediaControllerCompat.Callback
    ) {
        unregisterMediaController()
        trackerFacade.trackServiceEvent("onConnectedFailed")
    }

    private fun registerMediaController(
        token: MediaSessionCompat.Token,
        callback: MediaControllerCompat.Callback
    ) {
        val mediaController = MediaControllerCompat(this, token)
        mediaController.registerCallback(callback)
        MediaControllerCompat.setMediaController(this, mediaController)
    }

    private fun unregisterMediaController() {
        val mediaController = MediaControllerCompat.getMediaController(this)
        if (mediaController != null) {
            mediaController.unregisterCallback(mediaExposer.callback)
            MediaControllerCompat.setMediaController(this, null)
        }
    }

    override val metadata: Flow<PlayerMetadata>
        get() = mediaExposer.metadata

    override val playbackState: Flow<PlayerPlaybackState>
        get() = mediaExposer.playbackState

    override val repeat: Flow<PlayerRepeatMode>
        get() = mediaExposer.repeat

    override val shuffle: Flow<PlayerShuffleMode>
        get() = mediaExposer.shuffle

    override val queue: Flow<List<PlayerItem>>
        get() = mediaExposer.queue

    private fun mediaController(): MediaControllerCompat? {
        return MediaControllerCompat.getMediaController(this)
    }

    private fun transportControls(): MediaControllerCompat.TransportControls? {
        return mediaController()?.transportControls
    }

    override fun playFromMediaId(mediaId: MediaId, filter: String?, sort: SortEntity?) {
        trackerFacade.trackServiceEvent("playFromMediaId", mediaId, filter, sort)

        val bundle = bundleOf(
            MusicServiceCustomAction.ARGUMENT_FILTER to filter,
            MusicServiceAction.ARGUMENT_SORT_TYPE to sort?.type?.name,
            MusicServiceAction.ARGUMENT_SORT_ARRANGING to sort?.arranging?.name
        )

        transportControls()?.playFromMediaId(mediaId.toString(), bundle)
    }

    override fun skipToQueueItem(idInPlaylist: Int) {
        trackerFacade.trackServiceEvent("skipToQueueItem", idInPlaylist)

        transportControls()?.skipToQueueItem(idInPlaylist.toLong())
    }

    override fun shuffle(mediaId: MediaId, filter: String?) {
        trackerFacade.trackServiceEvent("shuffle", mediaId, filter)

        val bundle = bundleOf(
            MusicServiceCustomAction.ARGUMENT_FILTER to filter
        )

        transportControls()?.playFromMediaId(
            mediaId.copy(modifier = MediaIdModifier.SHUFFLE).toString(),
            bundle
        )
    }

    override fun skipToNext() {
        trackerFacade.trackServiceEvent("skipToNext")

        transportControls()?.skipToNext()
    }

    override fun skipToPrevious() {
        trackerFacade.trackServiceEvent("skipToPrevious")

        transportControls()?.skipToPrevious()
    }

    override fun playPause() {
        trackerFacade.trackServiceEvent("playPause")

        mediaController()?.playPause()
    }

    override fun seekTo(where: Long) {
        trackerFacade.trackServiceEvent("seekTo", where)

        transportControls()?.seekTo(where)
    }

    override fun toggleShuffleMode() {
        trackerFacade.trackServiceEvent("toggleShuffleMode")

        // actual value is cycled internally
        transportControls()?.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_INVALID)
    }

    override fun toggleRepeatMode() {
        trackerFacade.trackServiceEvent("toggleRepeatMode")

        // actual value is cycled internally
        transportControls()?.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_INVALID)
    }

    override fun togglePlayerFavorite() {
        trackerFacade.trackServiceEvent("togglePlayerFavorite")

        // actual value is cycled internally
        transportControls()?.setRating(RatingCompat.newHeartRating(false))
    }

    override fun swap(from: Int, to: Int) {
        trackerFacade.trackServiceEvent("swap", from, to)

        val bundle = bundleOf(
            MusicServiceCustomAction.ARGUMENT_SWAP_FROM to from,
            MusicServiceCustomAction.ARGUMENT_SWAP_TO to to
        )
        transportControls()?.customAction(MusicServiceCustomAction.SWAP, bundle)
    }

    override fun swapRelative(from: Int, to: Int) {
        trackerFacade.trackServiceEvent("swapRelative", from, to)

        val bundle = bundleOf(
            MusicServiceCustomAction.ARGUMENT_SWAP_FROM to from,
            MusicServiceCustomAction.ARGUMENT_SWAP_TO to to
        )
        transportControls()?.customAction(MusicServiceCustomAction.SWAP_RELATIVE, bundle)
    }

    override fun remove(position: Int) {
        trackerFacade.trackServiceEvent("remove", position)

        val bundle = bundleOf(
            MusicServiceCustomAction.ARGUMENT_POSITION to position
        )
        transportControls()?.customAction(MusicServiceCustomAction.REMOVE, bundle)
    }

    override fun removeRelative(position: Int) {
        trackerFacade.trackServiceEvent("removeRelative", position)

        val bundle = bundleOf(
            MusicServiceCustomAction.ARGUMENT_POSITION to position
        )
        transportControls()?.customAction(MusicServiceCustomAction.REMOVE_RELATIVE, bundle)
    }

    override fun moveRelative(position: Int) {
        trackerFacade.trackServiceEvent("moveRelative", position)

        val bundle = bundleOf(
            MusicServiceCustomAction.ARGUMENT_POSITION to position
        )
        transportControls()?.customAction(MusicServiceCustomAction.MOVE_RELATIVE, bundle)
    }

    override fun addToPlayNext(mediaId: MediaId) {
        trackerFacade.trackServiceEvent("addToPlayNext", mediaId)

        transportControls()?.customAction(
            MusicServiceCustomAction.ADD_TO_PLAY_NEXT,
            bundleOf(
                MusicServiceCustomAction.ARGUMENT_MEDIA_ID_LIST to longArrayOf(mediaId.leaf!!),
                MusicServiceCustomAction.ARGUMENT_IS_PODCAST to mediaId.isAnyPodcast
            )

        )
    }

    override fun replayTenSeconds() {
        trackerFacade.trackServiceEvent("replayTenSeconds")

        transportControls()?.customAction(MusicServiceCustomAction.REPLAY_10)
    }

    override fun forwardTenSeconds() {
        trackerFacade.trackServiceEvent("forwardTenSeconds")

        transportControls()?.customAction(MusicServiceCustomAction.FORWARD_10)
    }

    override fun replayThirtySeconds() {
        trackerFacade.trackServiceEvent("replayThirtySeconds")

        transportControls()?.customAction(MusicServiceCustomAction.REPLAY_30)
    }

    override fun forwardThirtySeconds() {
        trackerFacade.trackServiceEvent("forwardThirtySeconds")

        transportControls()?.customAction(MusicServiceCustomAction.FORWARD_30)
    }
}