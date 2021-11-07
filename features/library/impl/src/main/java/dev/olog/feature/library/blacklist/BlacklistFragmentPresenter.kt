package dev.olog.feature.library.blacklist

import android.os.Environment
import dev.olog.core.MediaId
import dev.olog.core.entity.track.Folder
import dev.olog.core.gateway.track.FolderGateway
import dev.olog.feature.base.model.BaseModel
import dev.olog.feature.library.LibraryPrefs
import dev.olog.feature.library.R
import dev.olog.shared.lazyFast
import java.util.*
import javax.inject.Inject

class BlacklistFragmentPresenter @Inject constructor(
    folderGateway: FolderGateway,
    private val libraryPrefs: LibraryPrefs
) {

    val data : List<BlacklistModel> by lazyFast {
        val blacklisted = libraryPrefs.blacklist.get().map { it.toLowerCase(Locale.getDefault()) }
        folderGateway.getAllBlacklistedIncluded().map { it.toDisplayableItem(blacklisted) }
    }

    private fun Folder.toDisplayableItem(blacklisted: List<String>): BlacklistModel {
        return BlacklistModel(
            R.layout.dialog_blacklist_item,
            getMediaId(),
            this.title,
            this.path,
            blacklisted.contains(this.path.toLowerCase(Locale.getDefault()))
        )
    }

    fun saveBlacklisted(data: List<BlacklistModel>) {
        val blacklisted = data.filter { it.isBlacklisted }
            .map { it.path }
            .toSet()
        libraryPrefs.blacklist.set(blacklisted)
    }


}

data class BlacklistModel(
    override val type: Int,
    override val mediaId: MediaId,
    val title: String,
    val path: String,
    var isBlacklisted: Boolean
) : BaseModel {

    companion object {
        @Suppress("DEPRECATION")
        @JvmStatic
        private val defaultStorageDir = Environment.getExternalStorageDirectory().path ?: "/storage/emulated/0/"
    }

    // show the path without "/storage/emulated/0"
    val displayablePath : String
        get() {
            return try {
                path.substring(defaultStorageDir.length)
            } catch (ex: StringIndexOutOfBoundsException){
                ex.printStackTrace()
                path
            }
        }

}