package com.filerecover.photorecovery.allrecover.restore.utils

import java.io.File

object Utils {
    var SHOW_OPEN_ADS = true
    var fromSplash = false

    fun getFileName(path: String): String {
        return path.substring(path.lastIndexOf("/") + 1)
    }

    fun getFileList(str: String?): Array<File?> {
        val file = File(str.toString())
        if (!file.isDirectory) {
            return arrayOfNulls(0)
        }
        return file.listFiles() ?: arrayOfNulls(0)
    }
}