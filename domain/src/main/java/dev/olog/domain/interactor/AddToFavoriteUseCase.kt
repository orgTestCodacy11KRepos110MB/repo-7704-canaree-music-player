package dev.olog.domain.interactor

import dev.olog.domain.mediaid.MediaId
import dev.olog.domain.entity.favorite.FavoriteType
import dev.olog.domain.gateway.FavoriteGateway
import dev.olog.domain.interactor.songlist.GetSongListByParamUseCase
import javax.inject.Inject

class AddToFavoriteUseCase @Inject constructor(
    private val favoriteGateway: FavoriteGateway,
    private val getSongListByParamUseCase: GetSongListByParamUseCase

) {

    suspend operator fun invoke(param: Input) {
        val mediaId = param.mediaId
        val type = param.type
        if (mediaId.isLeaf) {
            val songId = mediaId.leaf!!
            return favoriteGateway.addSingle(type, songId)
        }

        val ids = getSongListByParamUseCase(mediaId).map { it.id }
        return favoriteGateway.addGroup(type, ids)
    }

    class Input(
        val mediaId: MediaId,
        val type: FavoriteType
    )

}