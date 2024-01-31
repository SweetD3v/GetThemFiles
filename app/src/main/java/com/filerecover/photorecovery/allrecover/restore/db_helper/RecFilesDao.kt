package com.filerecover.photorecovery.allrecover.restore.db_helper

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE_AUDIOS
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE_DOCS
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE_IMAGES
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE_VIDEOS

@Dao
interface RecFilesDao {
    @Query("SELECT * FROM recovered_files")
    fun getAllMedia(): MutableList<RecoveredFiles>?

    @Query("SELECT * FROM recovered_files WHERE mediaType LIKE $RECOVERY_TYPE_IMAGES")
    fun getAllImages(): MutableList<RecoveredFiles>?

    @Query("SELECT * FROM recovered_files WHERE mediaType LIKE $RECOVERY_TYPE_VIDEOS")
    fun getAllVideos(): MutableList<RecoveredFiles>?

    @Query("SELECT * FROM recovered_files WHERE mediaType LIKE $RECOVERY_TYPE_AUDIOS")
    fun getAllAudios(): MutableList<RecoveredFiles>?

    @Query("SELECT * FROM recovered_files WHERE mediaType LIKE $RECOVERY_TYPE_DOCS")
    fun getAllDocs(): MutableList<RecoveredFiles>?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFile(images: RecoveredFiles)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(images: MutableList<RecoveredFiles>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateFile(item: RecoveredFiles)

    @Delete
    fun deleteFile(file: RecoveredFiles)

    @Delete
    fun deleteFiles(files: MutableList<RecoveredFiles>)

    @Query("DELETE FROM recovered_files WHERE mediaPath LIKE :filePath")
    fun deleteByPath(filePath: String)
}