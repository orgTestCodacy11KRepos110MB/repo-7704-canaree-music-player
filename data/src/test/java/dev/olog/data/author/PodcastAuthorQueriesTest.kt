package dev.olog.data.author

import dev.olog.data.*
import dev.olog.data.QueriesConstants.recentlyAddedMaxTime
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.days

class PodcastAuthorQueriesTest {

    private val db = TestDatabase()
    private val indexedQueries = db.indexedPlayablesQueries
    private val blacklistQueries = db.blacklistQueries
    private val queries = db.podcastAuthorsQueries

    @Before
    fun setup() {
        blacklistQueries.insert("dir")

        indexedQueries.insert(IndexedPlayables(id = 100, author_id = 10, is_podcast = true))
        indexedQueries.insert(IndexedPlayables(id = 101, author_id = 10, is_podcast = true))
        indexedQueries.insert(IndexedPlayables(id = 102, author_id = 10, is_podcast = true))
        indexedQueries.insert(IndexedPlayables(id = 103, author_id = 20, is_podcast = true))
        indexedQueries.insert(IndexedPlayables(id = 104, author_id = 20, is_podcast = true))
        // below should be filtered
        indexedQueries.insert(IndexedPlayables(id = 105, author_id = 30, is_podcast = false))
        indexedQueries.insert(IndexedPlayables(id = 106, author_id = 30, is_podcast = true, directory = "dir"))
    }

    @Test
    fun `test selectAll`() {
        val actual = queries.selectAll().executeAsList()
        val expected = listOf(
            PodcastAuthorView(id = 10, episodes = 3),
            PodcastAuthorView(id = 20, episodes = 2),
        )
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `test selectById`() {
        val actual = queries.selectById(10).executeAsOne()
        val expected = PodcastAuthorView(id = 10, episodes = 3)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `test selectById, should be null when not present`() {
        val actual = queries.selectById(50).executeAsOneOrNull()
        Assert.assertEquals(null, actual)
    }

    @Test
    fun `test selectRecentlyAdded`() {
        var id = 0L
        fun song(authorId: Long, dateAdded: Long, directory: String = "") = IndexedPlayables(
            id = ++id,
            author_id = authorId,
            date_added = dateAdded,
            is_podcast = true,
            directory = directory
        )

        indexedQueries.deleteAll()

        val unixTimestamp = QueriesConstants.unitTimestamp()
        indexedQueries.insert(song(authorId = 10, dateAdded = unixTimestamp))
        indexedQueries.insert(song(authorId = 20, dateAdded = unixTimestamp - recentlyAddedMaxTime + 1.days.inWholeSeconds))
        indexedQueries.insert(song(authorId = 30, dateAdded = unixTimestamp - recentlyAddedMaxTime + 3.days.inWholeSeconds))
        indexedQueries.insert(song(authorId = 40, dateAdded = unixTimestamp - recentlyAddedMaxTime + 2.days.inWholeSeconds))
        // below should be skipped
        indexedQueries.insert(song(authorId = 50, dateAdded = unixTimestamp - recentlyAddedMaxTime - 1.days.inWholeSeconds))
        indexedQueries.insert(song(authorId = 60, dateAdded = unixTimestamp, directory = "dir"))

        val actual = queries.selectRecentlyAdded().executeAsList()
        val expected = listOf(
            PodcastAuthorView(id = 10, episodes = 1, dateAdded = unixTimestamp),
            PodcastAuthorView(id = 30, episodes = 1, dateAdded = unixTimestamp - recentlyAddedMaxTime + 3.days.inWholeSeconds),
            PodcastAuthorView(id = 40, episodes = 1, dateAdded = unixTimestamp - recentlyAddedMaxTime + 2.days.inWholeSeconds),
            PodcastAuthorView(id = 20, episodes = 1, dateAdded = unixTimestamp - recentlyAddedMaxTime + 1.days.inWholeSeconds),
        )
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `test lastPlayed, max 10 items`() {
        indexedQueries.insert(IndexedPlayables(id = 1, author_id = 1, is_podcast = true))
        indexedQueries.insert(IndexedPlayables(id = 2, author_id = 2, is_podcast = true))
        indexedQueries.insert(IndexedPlayables(id = 3, author_id = 3, is_podcast = true))
        indexedQueries.insert(IndexedPlayables(id = 4, author_id = 4, is_podcast = true))
        indexedQueries.insert(IndexedPlayables(id = 5, author_id = 5, is_podcast = true))
        indexedQueries.insert(IndexedPlayables(id = 6, author_id = 6, is_podcast = true))
        indexedQueries.insert(IndexedPlayables(id = 7, author_id = 7, is_podcast = true))
        indexedQueries.insert(IndexedPlayables(id = 8, author_id = 8, is_podcast = true))
        indexedQueries.insert(IndexedPlayables(id = 9, author_id = 9, is_podcast = true))
        indexedQueries.insert(IndexedPlayables(id = 10, author_id = 10, is_podcast = true))
        indexedQueries.insert(IndexedPlayables(id = 11, author_id = 11, is_podcast = true))
        indexedQueries.insert(IndexedPlayables(id = 12, author_id = 12, is_podcast = true))
        indexedQueries.insert(IndexedPlayables(id = 13, author_id = 12, is_podcast = true))

        queries.insertRecentlyPlayed(id = 1, date_played = 10)
        queries.insertRecentlyPlayed(id = 2, date_played = 20)
        queries.insertRecentlyPlayed(id = 3, date_played = 30)
        queries.insertRecentlyPlayed(id = 4, date_played = 40)
        queries.insertRecentlyPlayed(id = 5, date_played = 50)
        queries.insertRecentlyPlayed(id = 6, date_played = 60)
        queries.insertRecentlyPlayed(id = 7, date_played = 70)
        queries.insertRecentlyPlayed(id = 8, date_played = 100)
        queries.insertRecentlyPlayed(id = 9, date_played = 80)
        queries.insertRecentlyPlayed(id = 10, date_played = 5)
        queries.insertRecentlyPlayed(id = 11, date_played = 2)
        queries.insertRecentlyPlayed(id = 12, date_played = 90)

        val actual = queries.selectRecentlyPlayed().executeAsList()
        val expected = listOf(
            PodcastAuthorView(id = 8, episodes = 1),
            PodcastAuthorView(id = 12, episodes = 2),
            PodcastAuthorView(id = 9, episodes = 1),
            PodcastAuthorView(id = 7, episodes = 1),
            PodcastAuthorView(id = 6, episodes = 1),
            PodcastAuthorView(id = 5, episodes = 1),
            PodcastAuthorView(id = 4, episodes = 1),
            PodcastAuthorView(id = 3, episodes = 1),
            PodcastAuthorView(id = 2, episodes = 1),
            PodcastAuthorView(id = 1, episodes = 1),
        )
        Assert.assertEquals(expected, actual)
    }

}