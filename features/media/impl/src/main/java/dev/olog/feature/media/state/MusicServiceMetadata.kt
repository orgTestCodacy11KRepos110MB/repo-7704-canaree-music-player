package dev.olog.feature.media.state

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import dev.olog.core.ServiceScope
import dev.olog.core.schedulers.Schedulers
import dev.olog.feature.media.api.MusicConstants
import dev.olog.feature.media.api.MusicPreferencesGateway
import dev.olog.feature.media.api.extensions.putBoolean
import dev.olog.feature.media.interfaces.IPlayerLifecycle
import dev.olog.feature.media.api.model.MediaEntity
import dev.olog.feature.media.api.model.MetadataEntity
import dev.olog.feature.media.api.model.SkipType
import dev.olog.feature.widget.api.FeatureWidgetNavigator
import dev.olog.feature.widget.api.WidgetConstants
import dev.olog.image.provider.getCachedBitmap
import dev.olog.shared.extension.getAppWidgetsIdsFor
import dev.olog.ui.GlideUtils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject

@ServiceScoped
internal class MusicServiceMetadata @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaSession: MediaSessionCompat,
    playerLifecycle: IPlayerLifecycle,
    musicPrefs: MusicPreferencesGateway,
    private val serviceScope: ServiceScope,
    private val schedulers: Schedulers,
    private val featureWidgetNavigator: FeatureWidgetNavigator,
) : IPlayerLifecycle.Listener {

    private val builder = MediaMetadataCompat.Builder()

    private var showLockScreenArtwork = false

    init {
        playerLifecycle.addListener(this)

        musicPrefs.observeShowLockscreenArtwork()
            .onEach { showLockScreenArtwork = it }
            .launchIn(serviceScope)
    }

    override fun onPrepare(metadata: MetadataEntity) {
        onMetadataChanged(metadata)
    }

    override fun onMetadataChanged(metadata: MetadataEntity) {
        update(metadata)
        notifyWidgets(metadata.entity)
    }

    private fun update(metadata: MetadataEntity) {
        serviceScope.launch(schedulers.io) {

            val entity = metadata.entity

            builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, entity.mediaId.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, entity.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, entity.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, entity.album)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, entity.title)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, entity.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, entity.album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, entity.duration)
                .putString(MusicConstants.PATH, entity.path)
                .putBoolean(MusicConstants.IS_PODCAST, entity.isPodcast)
                .putBoolean(MusicConstants.SKIP_NEXT, metadata.skipType == SkipType.SKIP_NEXT)
                .putBoolean(MusicConstants.SKIP_PREVIOUS, metadata.skipType == SkipType.SKIP_PREVIOUS)

            yield()

            if (showLockScreenArtwork) {
                val bitmap = context.getCachedBitmap(entity.mediaId, GlideUtils.OVERRIDE_BIG)
                yield()
                builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
            }
            mediaSession.setMetadata(builder.build())
        }
    }

    private fun notifyWidgets(entity: MediaEntity) {
        for (clazz in featureWidgetNavigator.widgetClasses()) {
            val ids = context.getAppWidgetsIdsFor(clazz)

            val intent = Intent(context, clazz).apply {
                action = WidgetConstants.METADATA_CHANGED
                putExtra(WidgetConstants.ARGUMENT_SONG_ID, entity.id)
                putExtra(WidgetConstants.ARGUMENT_TITLE, entity.title)
                putExtra(WidgetConstants.ARGUMENT_SUBTITLE, entity.artist)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }

            context.sendBroadcast(intent)
        }

    }

}