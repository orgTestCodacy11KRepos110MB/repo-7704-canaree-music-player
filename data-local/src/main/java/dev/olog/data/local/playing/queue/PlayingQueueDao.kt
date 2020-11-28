package dev.olog.data.local.playing.queue

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import dev.olog.core.MediaId
import dev.olog.core.MediaIdCategory
import dev.olog.core.entity.PlayingQueueSong
import dev.olog.core.entity.track.Song
import dev.olog.core.gateway.podcast.PodcastGateway
import dev.olog.core.gateway.track.SongGateway
import dev.olog.core.interactor.UpdatePlayingQueueUseCaseRequest
import dev.olog.shared.android.utils.assertBackgroundThread
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
internal abstract class PlayingQueueDao {

    @Query(
        """
        SELECT * FROM playing_queue
        ORDER BY progressive
    """
    )
    abstract suspend fun getAllImpl(): List<PlayingQueueEntity>

    @Query(
        """
        SELECT * FROM playing_queue
        ORDER BY progressive
    """
    )
    abstract fun observeAllImpl(): Flow<List<PlayingQueueEntity>>

    @Query("DELETE FROM playing_queue")
    abstract suspend fun deleteAllImpl()

    @Insert
    abstract suspend fun insertAllImpl(list: List<PlayingQueueEntity>)

    private fun makePlayingQueue(
        playingQueue: List<PlayingQueueEntity>,
        songList: List<Song>,
        podcastList: List<Song>
    ): List<PlayingQueueSong> {
        // mapping to avoid O(n^2) iteration
        val mappedSongList = songList.groupBy { it.id }
        val mappedPodcastList = podcastList.groupBy { it.id }

        val result = mutableListOf<PlayingQueueSong>()

        for (playingQueueEntity in playingQueue) {
            val id = playingQueueEntity.songId

            val fakeSongList = mappedSongList[id]
                ?: mappedPodcastList[id]
                ?: continue

            val song = fakeSongList[0] // only one song
            val playingQueueSong = song.toPlayingQueueSong(
                idInPlaylist = playingQueueEntity.idInPlaylist,
                category = playingQueueEntity.category,
                categoryValue = playingQueueEntity.categoryValue
            )
            result.add(playingQueueSong)
        }
        return result
    }

    suspend fun getAllAsSongs(
        songList: List<Song>,
        podcastList: List<Song>
    ): List<PlayingQueueSong> {
        val queueEntityList = getAllImpl()
        return makePlayingQueue(queueEntityList, songList, podcastList)
    }

    fun observeAllAsSongs(
        songGateway: SongGateway,
        podcastGateway: PodcastGateway
    ): Flow<List<PlayingQueueSong>> {
        return this.observeAllImpl()
            .map {
                makePlayingQueue(it, songGateway.getAll(), podcastGateway.getAll())
            }
    }

    @Transaction
    open suspend fun insert(list: List<UpdatePlayingQueueUseCaseRequest>) {
        assertBackgroundThread()

        deleteAllImpl()
        val result = list.map {
            PlayingQueueEntity(
                songId = it.songId,
                category = it.mediaId.category.toString(),
                categoryValue = it.mediaId.categoryValue,
                idInPlaylist = it.idInPlaylist
            )
        }
        insertAllImpl(result)
    }

    private fun Song.toPlayingQueueSong(idInPlaylist: Int, category: String, categoryValue: String)
            : PlayingQueueSong {

        val parentMediaId = MediaId.createCategoryValue(MediaIdCategory.valueOf(category), categoryValue)

        return PlayingQueueSong(
            this.copy(idInPlaylist = idInPlaylist),
            MediaId.playableItem(parentMediaId, this.id)
        )
    }


}