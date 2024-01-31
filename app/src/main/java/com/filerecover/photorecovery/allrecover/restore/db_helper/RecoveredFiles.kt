package com.filerecover.photorecovery.allrecover.restore.db_helper

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE_IMAGES

@Entity(tableName = "recovered_files", indices = [(Index(value = ["mediaPath"], unique = true))])
data class RecoveredFiles(
    @PrimaryKey(autoGenerate = true) var pid: Int,
    @ColumnInfo(name = "mediaPath") var mediaPath: String = "",
    @ColumnInfo(name = "mediaType") var mediaType: Int = RECOVERY_TYPE_IMAGES,
) {
    constructor() : this(0)

    override fun toString(): String {
        return "path: $mediaPath\n"
    }
}