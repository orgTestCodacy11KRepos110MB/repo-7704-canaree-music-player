package dev.olog.feature.media.impl

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import dev.olog.core.MediaId
import dev.olog.core.entity.LastMetadata
import dev.olog.core.entity.favorite.FavoriteEnum
import dev.olog.core.entity.favorite.FavoriteStateEntity
import dev.olog.core.entity.favorite.FavoriteType
import dev.olog.core.interactor.*
import dev.olog.core.interactor.favorite.IsFavoriteSongUseCase
import dev.olog.core.interactor.favorite.UpdateFavoriteStateUseCase
import dev.olog.core.interactor.lastplayed.InsertLastPlayedAlbumUseCase
import dev.olog.core.interactor.lastplayed.InsertLastPlayedArtistUseCase
import dev.olog.core.prefs.MusicPreferencesGateway
import dagger.hilt.android.scopes.ServiceScoped
import dev.olog.feature.media.impl.interfaces.IPlayerLifecycle
import dev.olog.feature.media.impl.model.MediaEntity
import dev.olog.feature.media.impl.model.MetadataEntity
import dev.olog.injection.dagger.ServiceLifecycle
import dev.olog.shared.CustomScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject

@ServiceScoped
internal class CurrentSong @Inject constructor(
    insertMostPlayedUseCase: InsertMostPlayedUseCase,
    insertHistorySongUseCase: InsertHistorySongUseCase,
    private val musicPreferencesUseCase: MusicPreferencesGateway,
    private val isFavoriteSongUseCase: IsFavoriteSongUseCase,
    private val updateFavoriteStateUseCase: UpdateFavoriteStateUseCase,
    insertLastPlayedAlbumUseCase: InsertLastPlayedAlbumUseCase,
    insertLastPlayedArtistUseCase: InsertLastPlayedArtistUseCase,
    playerLifecycle: IPlayerLifecycle,
    @ServiceLifecycle private val lifecycle: Lifecycle,
) : IPlayerLifecycle.Listener {

    companion object {
        @JvmStatic
        private val TAG = "SM:${CurrentSong::class.java.simpleName}"
    }

    private var isFavoriteJob: Job? = null

    private val channel = Channel<MediaEntity>(Channel.UNLIMITED)

    init {
        playerLifecycle.addListener(this)

        lifecycle.coroutineScope.launch(Dispatchers.IO) {
            for (entity in channel) {
                Log.v(TAG, "on new item ${entity.title}")
                if (entity.mediaId.isArtist || entity.mediaId.isPodcastArtist) {
                    Log.v(TAG, "insert last played artist ${entity.title}")
                    insertLastPlayedArtistUseCase(entity.mediaId)
                } else if (entity.mediaId.isAlbum || entity.mediaId.isPodcastAlbum) {
                    Log.v(TAG, "insert last played album ${entity.title}")
                    insertLastPlayedAlbumUseCase(entity.mediaId)
                }

                Log.v(TAG, "insert most played ${entity.title}")
                MediaId.playableItem(entity.mediaId, entity.id)
                insertMostPlayedUseCase(entity.mediaId)

                Log.v(TAG, "insert to history ${entity.title}")
                insertHistorySongUseCase(
                    InsertHistorySongUseCase.Input(
                        entity.id,
                        entity.isPodcast
                    )
                )
            }
        }

    }

    override fun onPrepare(metadata: MetadataEntity) {
        updateFavorite(metadata.entity)
        saveLastMetadata(metadata.entity)
    }

    override fun onMetadataChanged(metadata: MetadataEntity) {
        channel.trySend(metadata.entity)
        updateFavorite(metadata.entity)
        saveLastMetadata(metadata.entity)
    }

    private fun updateFavorite(mediaEntity: MediaEntity) {
        Log.v(TAG, "updateFavorite ${mediaEntity.title}")

        isFavoriteJob?.cancel()
        isFavoriteJob = lifecycle.coroutineScope.launch(Dispatchers.Default) {
            val type = if (mediaEntity.isPodcast) FavoriteType.PODCAST else FavoriteType.TRACK
            val isFavorite =
                isFavoriteSongUseCase(IsFavoriteSongUseCase.Input(mediaEntity.id, type))
            val isFavoriteEnum =
                if (isFavorite) FavoriteEnum.FAVORITE else FavoriteEnum.NOT_FAVORITE
            updateFavoriteStateUseCase(FavoriteStateEntity(mediaEntity.id, isFavoriteEnum, type))
        }
    }

    private fun saveLastMetadata(entity: MediaEntity) {
        Log.v(TAG, "saveLastMetadata ${entity.title}")
        lifecycle.coroutineScope.launch(Dispatchers.Default) {
            musicPreferencesUseCase.setLastMetadata(
                LastMetadata(
                    entity.title, entity.artist, entity.id
                )
            )
        }
    }

}