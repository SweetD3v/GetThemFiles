package com.filerecover.photorecovery.allrecover.restore

import android.app.Application
import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.Parcel
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.filerecover.photorecovery.allrecover.restore.models.AppModel
import com.filerecover.photorecovery.allrecover.restore.models.CallbackStatus
import com.filerecover.photorecovery.allrecover.restore.models.FileTypes
import com.filerecover.photorecovery.allrecover.restore.models.Result
import com.filerecover.photorecovery.allrecover.restore.utils.StorageHelper
import com.filerecover.photorecovery.allrecover.restore.utils.apkExtensions
import com.filerecover.photorecovery.allrecover.restore.utils.audioExtensions
import com.filerecover.photorecovery.allrecover.restore.utils.documentExtensions
import com.filerecover.photorecovery.allrecover.restore.utils.getTotalStorageSpace
import com.filerecover.photorecovery.allrecover.restore.utils.isOreoPlus
import com.filerecover.photorecovery.allrecover.restore.utils.isRPlus
import com.filerecover.photorecovery.allrecover.restore.utils.photoExtensions
import com.filerecover.photorecovery.allrecover.restore.utils.videoExtensions
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference


class StorageViewModel(private val app: Application) : AndroidViewModel(app) {

    private val contextRef = WeakReference(app.applicationContext)
    private var jobStats: Job? = null

    private var _dataSizes: MutableLiveData<Result> = MutableLiveData()
    val dataSizes: LiveData<Result> get() = _dataSizes

//    private var _dataSizes: MutableLiveData<Result> = MutableLiveData()
//    val dataSizes: LiveData<Result> get() = _dataSizes

    fun getMediaSizes() {
        if (contextRef.get() == null) return
        if (jobStats != null) return

        jobStats = viewModelScope.launch(Dispatchers.IO) {
            try {
                val videoSize: Deferred<Result> = async {
                    Result(CallbackStatus.SUCCESS, FileTypes.VIDEO, getVideosSize())
                }

                val imageSize: Deferred<Result> = async {
                    Result(CallbackStatus.SUCCESS, FileTypes.IMAGE, getImagesSize())
                }

                val audioSize: Deferred<Result> = async {
                    Result(CallbackStatus.SUCCESS, FileTypes.AUDIO, getAudioSize())
                }

                val docSize: Deferred<Result> = async {
                    Result(CallbackStatus.SUCCESS, FileTypes.DOCUMENT, getDocSize())
                }

                val apkSize: Deferred<Result> = async {
                    val apps = getInstalledApps(true)
                    Result(
                        CallbackStatus.SUCCESS,
                        FileTypes.APK,
                        apps.sumOf { it.appSizeLong } + getApkSize()
                    )
                }

                val systemSize: Deferred<Result> = async {
                    Result(CallbackStatus.SUCCESS, FileTypes.SYSTEM, getTotalStorageSize())
                }

                val otherSize: Deferred<Result> = async {
                    Result(CallbackStatus.SUCCESS, FileTypes.OTHER, getOtherSize())
                }

                flow<Result> {
                    emit(videoSize.await())
                    emit(imageSize.await())
                    emit(apkSize.await())
                    emit(docSize.await())
                    emit(audioSize.await())
                    emit(systemSize.await())
                    emit(otherSize.await())
                }.cancellable()
                    .onEach {
                        delay(100)
                    }.collect {
                        _dataSizes.postValue(it)
                    }

//                val projection = arrayOf(
//                    MediaStore.Video.Media.SIZE
//                )
//                val collectionUri = if (isQPlus()) {
//                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
//                } else {
//                    MediaStore.Video.Media.getContentUri("external")
//                }
//
//                val selection = "${MediaStore.Video.Media.SIZE} > ?"
//                val selectionArgs = arrayOf("0")
//
//                var size = 0L
//                contextRef.get()?.let { ctx ->
//                    ctx.contentResolver.query(
//                        collectionUri,
//                        projection,
//                        selection,
//                        selectionArgs,
//                        null
//                    )
//                        ?.use { cursor ->
//                            if (cursor.moveToFirst()) {
//                                do {
//                                    size += cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
//                                } while (cursor.moveToNext())
//                            }
//                        }
//                    callback(size.formatSize())
//                }
            } catch (e: Exception) {
                Log.e("TAG", "getMediaSizesExc: ${e.message}")
                e.printStackTrace()
            } finally {
                jobStats = null
            }
        }
    }

    fun stopAnalyzing() {
        jobStats?.cancel()
    }

    private var videoFilesList: HashSet<File> = hashSetOf()

    private fun getVideoFiles(dir: File): HashSet<File> {
        val listFile: Array<File> = dir.listFiles() ?: arrayOf()
        if (listFile.isNotEmpty()) {
            for (file in listFile) {
                if (file.isDirectory) {
                    getVideoFiles(file)
                } else {
                    if (videoExtensions.any { file.path.endsWith(it, true) }) {
                        val temp = File(file.path)
                        videoFilesList.add(temp)
                    }
                }
            }
        }

        return videoFilesList
    }

    private fun getVideosSize(): Long {
        videoFilesList = hashSetOf()
        getVideoFiles(Environment.getExternalStorageDirectory())
        return videoFilesList.sumOf { it.length() }
    }

    private var imageFilesList: HashSet<File> = hashSetOf()

    private fun getImageFiles(dir: File): HashSet<File> {
        val listFile: Array<File> = dir.listFiles() ?: arrayOf()
        if (listFile.isNotEmpty()) {
            for (file in listFile) {
                if (file.isDirectory) {
                    getImageFiles(file)
                } else {
                    if (photoExtensions.any { file.path.endsWith(it, true) }) {
                        val temp = File(file.path)
                        imageFilesList.add(temp)
                    }
                }
            }
        }

        return imageFilesList
    }

    private fun getImagesSize(): Long {
        imageFilesList = hashSetOf()
        getImageFiles(Environment.getExternalStorageDirectory())
        return imageFilesList.sumOf { it.length() }
    }

    private var audioFilesList: HashSet<File> = hashSetOf()

    private fun getAudioFiles(dir: File): HashSet<File> {
        val listFile: Array<File> = dir.listFiles() ?: arrayOf()
        if (listFile.isNotEmpty()) {
            for (file in listFile) {
                if (file.isDirectory) {
                    getAudioFiles(file)
                } else {
                    if (audioExtensions.any { file.path.endsWith(it, true) }) {
                        val temp = File(file.path)
                        audioFilesList.add(temp)
                    }
                }
            }
        }

        return audioFilesList
    }

    private fun getAudioSize(): Long {
        audioFilesList = hashSetOf()
        getAudioFiles(Environment.getExternalStorageDirectory())
        return audioFilesList.sumOf { it.length() }
    }

    private var docFilesList: HashSet<File> = hashSetOf()

    private fun getDocFiles(dir: File): HashSet<File> {
        val listFile: Array<File> = dir.listFiles() ?: arrayOf()
        if (listFile.isNotEmpty()) {
            for (file in listFile) {
                if (file.isDirectory) {
                    getDocFiles(file)
                } else {
                    if (documentExtensions.any { file.path.endsWith(it, true) }) {
                        val temp = File(file.path)
                        docFilesList.add(temp)
                    }
                }
            }
        }

        return docFilesList
    }

    private fun getDocSize(): Long {
        docFilesList = hashSetOf()
        getDocFiles(Environment.getExternalStorageDirectory())
        return docFilesList.sumOf { it.length() }
    }

    private var otherFilesList: HashSet<File> = hashSetOf()

    private fun getOtherFiles(dir: File): HashSet<File> {
        val listFile: Array<File> = dir.listFiles() ?: arrayOf()
        if (listFile.isNotEmpty()) {
            for (file in listFile) {
                if (file.isDirectory) {
                    getOtherFiles(file)
                } else {
                    val extList = listOf(
                        videoExtensions.asList(),
                        photoExtensions.asList(),
                        audioExtensions.asList(),
                        documentExtensions.asList(),
                        apkExtensions.asList(),
                    ).flatten()
                    if (!extList.any { file.path.endsWith(it, true) }) {
                        Log.e("TAG", "getOtherFiles: ${file.path}")
                        val temp = File(file.path)
                        otherFilesList.add(temp)
                    }
                }
            }
        }

        return otherFilesList
    }

    private fun getOtherSize(): Long {
        otherFilesList = hashSetOf()
        getOtherFiles(Environment.getExternalStorageDirectory())
        return otherFilesList.sumOf { it.length() }
    }

    private fun getTotalStorageSize(): Long {

//        if (isOreoPlus()) {
//            if (contextRef.get() == null) return ""
//            val storageManager =
//                contextRef.get()!!.getSystemService(Context.STORAGE_SERVICE) as StorageManager
//            val storageVolumes = storageManager.storageVolumes
//            val storageStatsManager = contextRef.get()!!
//                .getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
//
//            var totalSpace = 0L
//            var freeSpace = 0L
//            for (storageVolume in storageVolumes) {
//                val path = getPath(contextRef.get()!!, storageVolume)
//                if (storageVolume.isPrimary) {
//                    totalSpace += storageStatsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
//                    freeSpace += storageStatsManager.getFreeBytes(StorageManager.UUID_DEFAULT)
//                } else if (path != null) {
//                    val file = File(path)
//                    totalSpace += file.totalSpace
//                    freeSpace += file.freeSpace
//                }
//            }
//
//            Log.e("TAG", "getTotalStorageSize: ${freeSpace.formatSize()}")
//        }

        return (StorageHelper.totalCapacity - getTotalStorageSpace())
    }

    private fun getPath(context: Context, storageVolume: StorageVolume): String? {
        if (isRPlus())
            storageVolume.directory?.absolutePath?.let { return it }
        try {
            return storageVolume.javaClass.getMethod("getPath").invoke(storageVolume) as String
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            return (storageVolume.javaClass.getMethod("getPathFile")
                .invoke(storageVolume) as File).absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val extDirs = context.getExternalFilesDirs(null)
        for (extDir in extDirs) {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val fileStorageVolume: StorageVolume = storageManager.getStorageVolume(extDir)
                ?: continue
            if (fileStorageVolume == storageVolume) {
                var file = extDir
                while (true) {
                    val parent = file.parentFile ?: return file.absolutePath
                    val parentStorageVolume = storageManager.getStorageVolume(parent)
                        ?: return file.absolutePath
                    if (parentStorageVolume != storageVolume)
                        return file.absolutePath
                    file = parent
                }
            }
        }
        try {
            val parcel = Parcel.obtain()
            storageVolume.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            parcel.readString()
            return parcel.readString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private var apkFilesList: HashSet<File> = hashSetOf()

    private fun getApkFiles(dir: File): HashSet<File> {
        val listFile: Array<File> = dir.listFiles() ?: arrayOf()
        if (listFile.isNotEmpty()) {
            for (file in listFile) {
                if (file.isDirectory) {
                    getApkFiles(file)
                } else {
                    if (apkExtensions.any { file.path.endsWith(it, true) }) {
                        val temp = File(file.path)
                        apkFilesList.add(temp)
                    }
                }
            }
        }

        return apkFilesList
    }

    private fun getApkSize(): Long {
        apkFilesList = hashSetOf()
        getApkFiles(Environment.getExternalStorageDirectory())
        val apkSizes = apkFilesList.sumOf { it.length() }
        return apkSizes
    }

    private fun getInstalledApps(showSystem: Boolean): List<AppModel> {
        val arrayList: MutableList<AppModel> = ArrayList()
        val packageManager = contextRef.get()!!.packageManager
        val installedPackages: List<PackageInfo> =
            packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        val appsCount = installedPackages.size
        var i = 0
        while (i < appsCount) {
            val packageInfo: PackageInfo = installedPackages[i]
            val applicationInfo = packageManager.getApplicationInfo(packageInfo.packageName, 0)
            val sourceDir = applicationInfo.sourceDir
            if (!showSystem) {
                if (packageManager.getLaunchIntentForPackage(packageInfo.packageName) != null) {
                    if (!sourceDir.startsWith("/vendor/", false)) {
                        val appModel = AppModel().apply {
                            this.appName =
                                packageInfo.applicationInfo.loadLabel(packageManager).toString()
                            val packageName: String = packageInfo.packageName
                            if (packageName.isNotEmpty()) {
                                this.packageName = packageName
                                packageInfo.versionName?.let { versionName ->
                                    this.versionName = versionName
                                }
                                this.icon = packageInfo.applicationInfo.loadIcon(packageManager)

                                val file = File(applicationInfo.sourceDir)

                                this.appSizeLong = file.length()

//                            val permInfo: PackageInfo = packageManager.getPackageInfo(
//                                applicationInfo.packageName,
//                                PackageManager.GET_PERMISSIONS
//                            )
                            }
                        }
                        arrayList.add(appModel)
                    }
                }
            } else {
                val appModel = AppModel().apply {
                    this.appName =
                        packageInfo.applicationInfo.loadLabel(packageManager).toString()
                    val packageName: String = packageInfo.packageName
                    if (packageName.isNotEmpty()) {
                        this.packageName = packageName
                        packageInfo.versionName?.let { versionName ->
                            this.versionName = versionName
                        }
                        this.icon = packageInfo.applicationInfo.loadIcon(packageManager)

                        if (isOreoPlus()) {
                            val storageStatsManager = contextRef.get()!!
                                .getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                            val storageStats: StorageStats = storageStatsManager.queryStatsForUid(
                                applicationInfo.storageUuid,
                                applicationInfo.uid
                            )
                            val apkSize = storageStats.appBytes
                            val dataSize = storageStats.dataBytes
                            val cacheSize = storageStats.cacheBytes

                            val totalSize = apkSize + dataSize
//                            if (packageManager.getLaunchIntentForPackage(packageInfo.packageName) != null) {
//                                this.appSizeLong =
//                                    totalSize + File(applicationInfo.sourceDir).length() + File(
//                                        applicationInfo.publicSourceDir
//                                    ).length()
//                            } else
                            this.appSizeLong = totalSize
                        } else {
                            this.appSizeLong = File(applicationInfo.sourceDir).length()
                        }
                    }
                }
                arrayList.add(appModel)
            }
            i++
        }
        return arrayList.sortedByDescending { it.appSizeLong }
    }
}
