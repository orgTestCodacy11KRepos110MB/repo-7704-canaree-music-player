package dev.olog.feature.library

import android.content.Context
import dev.olog.core.MediaIdCategory

data class LibraryCategoryBehavior(
    val category: MediaIdCategory,
    var visible: Boolean,
    var order: Int
) {

    fun asString(context: Context): String {
        val stringId = when (category) {
            MediaIdCategory.FOLDERS -> localization.R.string.category_folders
            MediaIdCategory.PLAYLISTS,
            MediaIdCategory.PODCASTS_PLAYLIST -> localization.R.string.category_playlists
            MediaIdCategory.SONGS -> localization.R.string.category_songs
            MediaIdCategory.ALBUMS,
            MediaIdCategory.PODCASTS_ALBUMS -> localization.R.string.category_albums
            MediaIdCategory.ARTISTS,
            MediaIdCategory.PODCASTS_ARTISTS -> localization.R.string.category_artists
            MediaIdCategory.GENRES -> localization.R.string.category_genres
            MediaIdCategory.PODCASTS -> localization.R.string.category_podcasts
            else -> 0 //will throw an exception
        }
        return context.getString(stringId)
    }

}
