package com.filerecover.photorecovery.allrecover.restore.db_helper

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RecoveredFiles::class],
    version = 1
)
abstract class RecoveredFilesDB : RoomDatabase() {
    abstract fun recFilesDao(): RecFilesDao
}