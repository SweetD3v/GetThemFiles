package com.filerecover.photorecovery.allrecover.restore.utils

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.filerecover.photorecovery.allrecover.restore.models.RecoveryMedia


object StorageHelper {
    val spaceLong get() = getTotalStorageSpace().formatSizeLong()
    val totalCapacity: Long
        get() = if (spaceLong > 8.0 && spaceLong <= 16.0) {
            16 * 1024 * 1024 * 1024L
        } else if (spaceLong > 16.0 && spaceLong <= 32.0) {
            32 * 1024 * 1024 * 1024L
        } else if (spaceLong > 32.0 && spaceLong <= 64.0) {
            64 * 1024 * 1024 * 1024L
        } else if (spaceLong > 64.0 && spaceLong < 128.0) {
            128 * 1024 * 1024 * 1024L
        } else if (spaceLong > 128.0 && spaceLong < 256.0) {
            256 * 1024 * 1024 * 1024L
        } else if (spaceLong > 256.0 && spaceLong < 512.0) {
            512 * 1024 * 1024 * 1024L
        } else if (spaceLong > 512.0 && spaceLong < 1024.0) {
            1024 * 1024 * 1024 * 1024L
        } else 8 * 1024 * 1024 * 1024L

    fun Long.getRelativeStorageSize(): Float {
        return (this * 100f / getTotalStorageSpace()).formatFloat()
    }

    fun Context.hasStoragePermission() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED

    fun Context.isAccessGranted(): Boolean {
        return try {
            val packageManager = packageManager
            val applicationInfo = packageManager.getApplicationInfo(
                packageName, 0
            )
            val appOpsManager = getSystemService(AppCompatActivity.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                applicationInfo.uid, applicationInfo.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isWithinScanFolders(path: String?, recoveryType: Int): Boolean {
        val mediaPath = path ?: ""
        return when (recoveryType) {
            RECOVERY_TYPE_IMAGES -> {
                mediaPath.contains("Pictures/.thumbnails")
                        || mediaPath.contains("Movies/.thumbnails")
                        || mediaPath.contains("WhatsApp/.Shared")
                        || mediaPath.contains("WhatsApp Images/Sent")
                        || mediaPath.contains("WhatsApp Stickers")
                        || mediaPath.contains("Telegram")
                        || getFolderName(mediaPath).contains("cache")
                        || getFolderName(mediaPath).contains("trash")
                        || getFolderName(mediaPath).startsWith(".watermark")
                        || getFolderName(mediaPath).startsWith(".")
            }

            RECOVERY_TYPE_VIDEOS -> {
                mediaPath.contains("WhatsApp Video/Sent")
                        || mediaPath.contains("WhatsApp/.Shared")
                        || mediaPath.contains("WhatsApp Stickers")
                        || mediaPath.contains("Telegram")
                        || getFolderName(mediaPath).contains("cache")
                        || getFolderName(mediaPath).contains("trash")
                        || getFolderName(mediaPath).startsWith(".")
            }

            RECOVERY_TYPE_AUDIOS -> {
                mediaPath.contains("WhatsApp Audio/Sent")
                        || mediaPath.contains("Telegram")
                        || getFolderName(mediaPath).contains("cache")
                        || getFolderName(mediaPath).contains("trash")
                        || getFolderName(mediaPath).startsWith(".")
            }

            RECOVERY_TYPE_DOCS -> {
                mediaPath.contains("WhatsApp Documents/Sent")
                        || mediaPath.contains("Telegram")
                        || getFolderName(mediaPath).contains("cache")
                        || getFolderName(mediaPath).contains("trash")
                        || getFolderName(mediaPath).startsWith(".")
            }

            else -> false
        }
    }

    var _recoveredMediaList: MutableLiveData<MutableList<RecoveryMedia>> = MutableLiveData()
    val recoveredMediaList: LiveData<MutableList<RecoveryMedia>> get() = _recoveredMediaList
}