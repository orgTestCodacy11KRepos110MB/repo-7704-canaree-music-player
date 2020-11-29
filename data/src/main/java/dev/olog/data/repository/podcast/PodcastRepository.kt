package dev.olog.data.repository.podcast

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.olog.core.entity.track.Song
import dev.olog.core.gateway.base.Id
import dev.olog.core.gateway.podcast.PodcastGateway
import dev.olog.core.prefs.BlacklistPreferences
import dev.olog.core.prefs.SortPreferences
import dev.olog.core.schedulers.Schedulers
import dev.olog.data.local.podcast.PodcastPositionDao
import dev.olog.data.local.podcast.PodcastPositionEntity
import dev.olog.data.mapper.toSong
import dev.olog.data.queries.TrackQueries
import dev.olog.data.repository.BaseRepository
import dev.olog.data.repository.ContentUri
import dev.olog.data.utils.queryAll
import dev.olog.data.utils.queryOne
import dev.olog.shared.value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import javax.inject.Inject

internal class PodcastRepository @Inject constructor(
    @ApplicationContext context: Context,
    sortPrefs: SortPreferences,
    blacklistPrefs: BlacklistPreferences,
    private val podcastPositionDao: PodcastPositionDao,
    schedulers: Schedulers
) : BaseRepository<Song, Id>(context, schedulers), PodcastGateway {

    private val queries = TrackQueries(
        schedulers = schedulers,
        contentResolver = context.contentResolver,
        blacklistPrefs = blacklistPrefs,
        sortPrefs = sortPrefs,
        isPodcast = true
    )

    init {
        firstQuery()
    }

    override fun registerMainContentUri(): ContentUri {
        return ContentUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true)
    }

    override suspend fun queryAll(): List<Song> {
        val cursor = queries.getAll()
        return contentResolver.queryAll(cursor, Cursor::toSong)
    }

    override suspend fun getByParam(param: Id): Song? {
        return publisher.value?.find { it.id == param }
            ?: contentResolver.queryOne(queries.getByParam(param), Cursor::toSong)
    }

    override fun observeByParam(param: Id): Flow<Song?> {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, param)
        val contentUri = ContentUri(uri, true)
        return observeByParamInternal(contentUri) { getByParam(param) }
            .distinctUntilChanged()
    }

    override suspend fun deleteSingle(id: Id) {
        return deleteInternal(id)
    }

    override suspend fun deleteGroup(podcastList: List<Song>) {
        for (id in podcastList) {
            deleteInternal(id.id)
        }
    }

    private suspend fun deleteInternal(id: Id) {
        val path = getByParam(id)!!.path
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
        val deleted = contentResolver.delete(uri, null, null)

        if (deleted < 1) {
            Log.w("PodcastRepo", "podcast not found $id")
            return
        }
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
    }

    override suspend fun getCurrentPosition(podcastId: Long, duration: Long): Long {
        val position = podcastPositionDao.getPosition(podcastId) ?: 0L
        if (position > duration - 1000 * 5) {
            // if last 5 sec, restart
            return 0L
        }
        return position
    }

    override suspend fun saveCurrentPosition(podcastId: Long, position: Long) {
        podcastPositionDao.setPosition(PodcastPositionEntity(podcastId, position))
    }

    override suspend fun getByAlbumId(albumId: Id): Song? {
        return publisher.value?.find { it.albumId == albumId }
            ?: contentResolver.queryOne(queries.getByAlbumId(albumId), Cursor::toSong)
    }
}