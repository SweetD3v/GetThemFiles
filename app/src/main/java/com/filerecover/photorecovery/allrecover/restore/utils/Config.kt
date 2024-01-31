package com.filerecover.photorecovery.allrecover.restore.utils

import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.File

object Config {
    const val DATA = 1000
    const val REPAIR = 2000
    const val UPDATE = 3000
    var RECOVER_DIRECTORY: String? = null
    var IMAGE_RECOVER_DIRECTORY: String? = null
    var VIDEO_RECOVER_DIRECTORY: String? = null
    var AUDIO_RECOVER_DIRECTORY: String? = null
    var OTHER_RECOVER_DIRECTORY: String? = null
    const val APP_NAME = "DataRecovery"
    var _STATUS_BAR_HEIGHT : MutableLiveData<Int> = MutableLiveData()
    val STATUS_BAR_HEIGHT : LiveData<Int> get() = _STATUS_BAR_HEIGHT
    var _NAVIGARION_BAR_HEIGHT : MutableLiveData<Int> = MutableLiveData()
    val NAVIGATION_BAR_HEIGHT : LiveData<Int> get() = _NAVIGARION_BAR_HEIGHT

    init {
        val sbDirectory = StringBuilder()
        sbDirectory.append(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
        )
        sbDirectory.append(File.separator)
        sbDirectory.append(APP_NAME)
        RECOVER_DIRECTORY = sbDirectory.toString()
    }

    init {
        val sbDirectory = StringBuilder()
        sbDirectory.append(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
        )
        sbDirectory.append(File.separator)
        sbDirectory.append(APP_NAME)
        sbDirectory.append(File.separator)
        sbDirectory.append("Audios")
        AUDIO_RECOVER_DIRECTORY = sbDirectory.toString()
    }

    init {
        val sbDirectory = StringBuilder()
        sbDirectory.append(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
        )
        sbDirectory.append(File.separator)
        sbDirectory.append(APP_NAME)
        sbDirectory.append(File.separator)
        sbDirectory.append("Images")
        IMAGE_RECOVER_DIRECTORY = sbDirectory.toString()
    }

    init {
        val sbDirectory = StringBuilder()
        sbDirectory.append(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
        )
        sbDirectory.append(File.separator)
        sbDirectory.append(APP_NAME)
        sbDirectory.append(File.separator)
        sbDirectory.append("Videos")
        VIDEO_RECOVER_DIRECTORY = sbDirectory.toString()
    }

    init {
        val sbDirectory = StringBuilder()
        sbDirectory.append(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
        )
        sbDirectory.append(File.separator)
        sbDirectory.append(APP_NAME)
        sbDirectory.append(File.separator)
        sbDirectory.append("Others")
        OTHER_RECOVER_DIRECTORY = sbDirectory.toString()
    }
}