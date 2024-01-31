package com.filerecover.photorecovery.allrecover.restore.db_helper

import android.content.Context
import androidx.room.Room

class RecoveredFilesDBClient {
    lateinit var recFilesDao: RecFilesDao

    companion object {
        fun getInstance(ctx: Context): RecoveredFilesDBClient {
            val recFilesDBClient = RecoveredFilesDBClient()
            recFilesDBClient.recFilesDao = Room.databaseBuilder(
                ctx,
                RecoveredFilesDB::class.java,
                "rec_files.db"
            )
                .build().recFilesDao()
            return recFilesDBClient
        }
    }
}