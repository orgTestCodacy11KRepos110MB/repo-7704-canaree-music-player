package dev.olog.presentation.service_music

import android.media.session.PlaybackState.STATE_PAUSED
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import dev.olog.presentation.dagger.PerActivity
import dev.olog.shared.MediaId
import dev.olog.shared.constants.MusicConstants
import dev.olog.shared.constants.MusicConstants.ACTION_PLAY_SHUFFLE
import javax.inject.Inject

@PerActivity
class MusicController @Inject constructor(
        private val mediaControllerProvider: MediaControllerProvider
) {

    fun playPause() {
        val transportControls = getTransportControls() ?: return

        getMediaController()?.playbackState?.let {
            val state = it.state
            if (state == PlaybackStateCompat.STATE_PLAYING) {
                transportControls.pause()
            } else if (state == STATE_PAUSED) {
                transportControls.play()
            }
        }
    }

    fun skipToNext() {
        getTransportControls()?.skipToNext()
    }

    fun skipToPrevious() {
        getTransportControls()?.skipToPrevious()
    }

    fun toggleRepeatMode() {
        getTransportControls()?.setRepeatMode(-1)
    }

    fun toggleShuffleMode() {
        getTransportControls()?.setShuffleMode(-1)
    }


    fun seekTo(pos: Long){
        getTransportControls()?.seekTo(pos)
    }

    fun playFromMediaId(mediaId: MediaId) {
        getTransportControls()?.playFromMediaId(mediaId.toString(), null)
    }

    fun playRecentlyPlayedFromMediaId(mediaId: MediaId){
        val bundle = Bundle()
        bundle.putBoolean(MusicConstants.BUNDLE_RECENTLY_PLAYED, true)
        getTransportControls()?.playFromMediaId(mediaId.toString(), bundle)
    }

    fun playMostPlayedFromMediaId(mediaId: MediaId){
        val bundle = Bundle()
        bundle.putBoolean(MusicConstants.BUNDLE_MOST_PLAYED, true)
        getTransportControls()?.playFromMediaId(mediaId.toString(), bundle)
    }

    fun playShuffle(mediaId: MediaId) {
        val bundle = Bundle()
        bundle.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId.toString())
        getTransportControls()?.sendCustomAction(ACTION_PLAY_SHUFFLE, bundle)
    }

    fun skipToQueueItem(mediaId: MediaId) {
        getTransportControls()?.skipToQueueItem(mediaId.leaf!!)
    }

    fun togglePlayerFavorite() {
        val mediaController = getMediaController() ?: return

        val playbackState = mediaController.playbackState
        if (playbackState != null) {
            val activeQueueItemId = playbackState.activeQueueItemId
            toggleFavorite(activeQueueItemId)
        }
    }

    private fun toggleFavorite(songId: Long) {
        val transportControls = getTransportControls() ?: return
        val bundle = Bundle()
        bundle.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, songId.toString())
        transportControls.setRating(RatingCompat.newHeartRating(false), bundle)
    }

    fun swap(from: Int, to: Int){
        val bundle = Bundle()
        bundle.putInt(MusicConstants.ARGUMENT_SWAP_FROM, from)
        bundle.putInt(MusicConstants.ARGUMENT_SWAP_TO, to)
        getTransportControls()?.sendCustomAction(MusicConstants.ACTION_SWAP, bundle)
    }

    fun swapRelative(from: Int, to: Int){
        val bundle = Bundle()
        bundle.putInt(MusicConstants.ARGUMENT_SWAP_FROM, from)
        bundle.putInt(MusicConstants.ARGUMENT_SWAP_TO, to)
        getTransportControls()?.sendCustomAction(MusicConstants.ACTION_SWAP_RELATIVE, bundle)
    }

    private fun getTransportControls(): MediaControllerCompat.TransportControls? {
        return mediaControllerProvider.getSupportMediaController()?.transportControls
    }

    private fun getMediaController(): MediaControllerCompat? {
        return mediaControllerProvider.getSupportMediaController()
    }

}