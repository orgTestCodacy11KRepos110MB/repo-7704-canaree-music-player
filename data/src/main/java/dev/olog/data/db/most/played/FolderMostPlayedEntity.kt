package dev.olog.data.db.most.played

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "most_played_folder",
    indices = [(Index("id"))]
)
data class FolderMostPlayedEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val songId: Long,
    val folderPath: String
)