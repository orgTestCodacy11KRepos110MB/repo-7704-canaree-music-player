package dev.olog.feature.main.dialog.favorite

import android.content.Context
import androidx.core.text.parseAsHtml
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.olog.core.MediaId
import dev.olog.platform.fragment.BaseDialog
import dev.olog.platform.navigation.FragmentTagFactory
import dev.olog.shared.extension.argument
import dev.olog.shared.extension.launchWhenResumed
import dev.olog.shared.extension.toast
import dev.olog.shared.extension.withArguments

@AndroidEntryPoint
class AddFavoriteDialog : BaseDialog() {

    companion object {
        val TAG = FragmentTagFactory.create(AddFavoriteDialog::class)
        val ARGUMENTS_MEDIA_ID = "$TAG.arguments.media_id"
        val ARGUMENTS_LIST_SIZE = "$TAG.arguments.list_size"
        val ARGUMENTS_ITEM_TITLE = "$TAG.arguments.item_title"

        fun newInstance(mediaId: MediaId, listSize: Int, itemTitle: String): AddFavoriteDialog {
            return AddFavoriteDialog().withArguments(
                    ARGUMENTS_MEDIA_ID to mediaId,
                    ARGUMENTS_LIST_SIZE to listSize,
                    ARGUMENTS_ITEM_TITLE to itemTitle
            )
        }
    }

    private val mediaId by argument<MediaId>(ARGUMENTS_MEDIA_ID)
    private val title by argument<String>(ARGUMENTS_ITEM_TITLE)
    private val listSize by argument<Int>(ARGUMENTS_LIST_SIZE)

    private val viewModel by viewModels<AddFavoriteDialogViewModel>()

    override fun extendBuilder(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        return builder.setTitle(localization.R.string.popup_add_to_favorites)
            .setMessage(createMessage().parseAsHtml())
            .setPositiveButton(localization.R.string.popup_positive_ok, null)
            .setNegativeButton(localization.R.string.popup_negative_cancel, null)
    }

    override fun positionButtonAction(context: Context) {
        launchWhenResumed {
            var message: String
            try {
                viewModel.execute(mediaId)
                message = successMessage(requireContext())
            } catch (ex: Throwable) {
                ex.printStackTrace()
                message = failMessage(requireContext())
            }
            toast(message)
            dismiss()
        }
    }

    private fun successMessage(context: Context): String {
        if (mediaId.isLeaf){
            return context.getString(localization.R.string.song_x_added_to_favorites, title)
        }
        return context.resources.getQuantityString(localization.R.plurals.xx_songs_added_to_favorites, listSize, listSize)
    }

    private fun failMessage(context: Context): String {
        return context.getString(localization.R.string.popup_error_message)
    }

    private fun createMessage() : String {
        return if (mediaId.isLeaf) {
            getString(localization.R.string.add_song_x_to_favorite, title)
        } else {
            context!!.resources.getQuantityString(
                    localization.R.plurals.add_xx_songs_to_favorite, listSize, listSize)
        }
    }

}