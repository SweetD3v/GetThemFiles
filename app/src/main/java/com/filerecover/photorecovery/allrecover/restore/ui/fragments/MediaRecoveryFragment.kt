package com.filerecover.photorecovery.allrecover.restore.ui.fragments

import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import com.filerecover.photorecovery.allrecover.restore.R
import com.filerecover.photorecovery.allrecover.restore.adapters.MediaListAdapter
import com.filerecover.photorecovery.allrecover.restore.databinding.BottomSheetMoreOptionsBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.DialogDeleteBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.DialogDetailsBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.DialogRestoreBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.DialogRestoreProgressBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.FragmentMediaRecoveryBinding
import com.filerecover.photorecovery.allrecover.restore.db_helper.RecFilesDao
import com.filerecover.photorecovery.allrecover.restore.db_helper.RecoveredFiles
import com.filerecover.photorecovery.allrecover.restore.db_helper.RecoveredFilesDBClient
import com.filerecover.photorecovery.allrecover.restore.interfaces.AdEventListener
import com.filerecover.photorecovery.allrecover.restore.interfaces.MediaClickListener
import com.filerecover.photorecovery.allrecover.restore.models.RecoveryMedia
import com.filerecover.photorecovery.allrecover.restore.utils.ARGS_MEDIA_LIST_DETAILS
import com.filerecover.photorecovery.allrecover.restore.utils.AdsUtils
import com.filerecover.photorecovery.allrecover.restore.utils.Config
import com.filerecover.photorecovery.allrecover.restore.utils.ITEM_TYPE_HEADER
import com.filerecover.photorecovery.allrecover.restore.utils.ITEM_TYPE_MEDIA
import com.filerecover.photorecovery.allrecover.restore.utils.ImageHelper
import com.filerecover.photorecovery.allrecover.restore.utils.MEDIA_GRID_SIZE
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE_IMAGES
import com.filerecover.photorecovery.allrecover.restore.utils.StorageHelper
import com.filerecover.photorecovery.allrecover.restore.utils.Utils
import com.filerecover.photorecovery.allrecover.restore.utils.convertToDateStr
import com.filerecover.photorecovery.allrecover.restore.utils.formatSize
import com.filerecover.photorecovery.allrecover.restore.utils.getMimeType
import com.filerecover.photorecovery.allrecover.restore.utils.gone
import com.filerecover.photorecovery.allrecover.restore.utils.isOnline
import com.filerecover.photorecovery.allrecover.restore.utils.openFile
import com.filerecover.photorecovery.allrecover.restore.utils.photoExtensions
import com.filerecover.photorecovery.allrecover.restore.utils.setOnSingleClickListener
import com.filerecover.photorecovery.allrecover.restore.utils.shareFile
import com.filerecover.photorecovery.allrecover.restore.utils.videoExtensions
import com.filerecover.photorecovery.allrecover.restore.utils.visible
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MediaRecoveryFragment :
    BaseFragment<FragmentMediaRecoveryBinding>(R.layout.fragment_media_recovery),
    MediaClickListener {

    val adsUtils by lazy { AdsUtils.newInstance(ctx) }

    private val recFilesDao: RecFilesDao by lazy { RecoveredFilesDBClient.getInstance(ctx).recFilesDao }

    private val mediaAdapter: MediaListAdapter by lazy { MediaListAdapter() }

    private var imagesList: MutableList<RecoveryMedia> = mutableListOf()
    private var recoveredList: MutableList<RecoveryMedia> = mutableListOf()
    private var recoveredMediaList: MutableList<RecoveryMedia> = mutableListOf()

    private var jobRecovery: Job? = null
    private var jobRestore: Job? = null
    var numPhoto = 0
    private var recoveryType = RECOVERY_TYPE_IMAGES

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMediaRecoveryBinding.inflate(layoutInflater)
        Config.STATUS_BAR_HEIGHT.observe(viewLifecycleOwner) { top ->
            binding.root.findViewById<RelativeLayout>(R.id.clStatus)
                ?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    height = top
                }
        }
        return getPersistentView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recoveryType =
            arguments?.getInt(RECOVERY_TYPE, RECOVERY_TYPE_IMAGES) ?: RECOVERY_TYPE_IMAGES
        setupRVMedia()

        lifecycleScope.launch(Dispatchers.IO) {
            val imagesDB = (if (recoveryType == RECOVERY_TYPE_IMAGES) recFilesDao.getAllImages()
                ?: mutableListOf() else recFilesDao.getAllVideos()
                ?: mutableListOf()).filter { File(it.mediaPath).exists() }

            if (imagesDB.isEmpty()) {
                showScanBtn()
            } else startRecovering(false, imagesDB.map {
                RecoveryMedia(
                    path = it.mediaPath,
                    dateModified = File(it.mediaPath).lastModified(),
                    size = File(it.mediaPath).length()
                )
            }.toMutableList())
        }

        binding.run {
            txtTitle.text =
                if (recoveryType == RECOVERY_TYPE_IMAGES) getString(R.string.image_recovery)
                else getString(R.string.video_recovery)

            btnBack.setOnClickListener {
                onBackBtnPressed()
            }

            if (ctx.isOnline()) {
                adsUtils.loadNative(R.string.native_id, object :
                    AdEventListener {
                    override fun onAdLoaded(nativeAd: NativeAd?) {
                        adsUtils.populateUnifiedNativeAdView(nativeAdFrame, nativeAd!!)
                        shimmerFrame.root.stopShimmer()
                        shimmerFrame.root.gone()
                    }

                    override fun onAdClosed() {

                    }

                    override fun onLoadError(errorCode: String?) {

                    }
                })
            } else shimmerFrame.root.gone()

            btnBack.setOnClickListener {
                onBackBtnPressed()
            }

            clDeepScan.btnDeepScan.setOnSingleClickListener {
                startRecovering()
            }

            btnRescan.setOnSingleClickListener {
                startRecovering(rescanning = true)
            }
        }
    }

    private fun setupRVMedia() {
        binding.run {
            rvImages.setHasFixedSize(true)
            rvImages.layoutManager = GridLayoutManager(ctx, MEDIA_GRID_SIZE).apply {
                spanSizeLookup = object : SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return if (recoveredMediaList[position].itemType == ITEM_TYPE_HEADER) MEDIA_GRID_SIZE else 1
                    }
                }
            }
            rvImages.adapter = mediaAdapter
            mediaAdapter.recoveryType = recoveryType
            mediaAdapter.mediaClickListener = this@MediaRecoveryFragment

            // additional decorations
//            rvImages.addItemDecoration(
//                SimpleListDividerDecoratorAdv(
//                    ContextCompat.getDrawable(
//                        ctx, R.drawable.list_divider_h
//                    ), true, true
//                )
//            )
        }
    }

    private fun navigateToMediaDetails(mediaPath: String) {
        val bundle = Bundle()
        bundle.putString(ARGS_MEDIA_LIST_DETAILS, mediaPath)
        findNavController().navigate(R.id.action_media_to_media_details, bundle)
    }

    private fun showLoading(showLoad: Boolean = false) {
        binding.run {
            root.post {
                clEmpty.root.gone()
                clDeepScan.root.gone()
                if (showLoad) {
                    arrayOf(txtFoundCounts, viewDivider1, rvImages).forEach { it.gone() }
                    clScanning.root.visible()
                    flStatus.visible()
                }
            }
        }
    }

    private fun showScanBtn() {
        binding.run {
            root.post {
                clEmpty.root.gone()
                clScanning.root.gone()
                clDeepScan.root.visible()
            }
        }
    }

    private fun startRecovering(
        showLoad: Boolean = true,
        imagesListDB: MutableList<RecoveryMedia> = mutableListOf(),
        rescanning: Boolean = false
    ) {
        if (jobRecovery?.isActive == true || !isAdded) return
        showLoading(showLoad)

        binding.run {
            btnRescan.gone()
        }
        jobRecovery = lifecycleScope.launch(Dispatchers.IO) {
            try {
                numPhoto = 0
                imagesList = mutableListOf()
                if (imagesListDB.isNotEmpty() && !rescanning) {
                    imagesList = imagesListDB.onEach { it.itemType = ITEM_TYPE_MEDIA }
                } else {
                    try {
                        val internalPath = Environment.getExternalStorageDirectory().absolutePath
                        checkFileOfDirectory(
                            internalPath, Utils.getFileList(internalPath)
                        )

                        val recoveredFilesList = imagesList.map {
                            RecoveredFiles().apply {
                                this.mediaPath = it.path
                                this.mediaType = recoveryType
                            }
                        }.toMutableList()

                        recFilesDao.insertFiles(recoveredFilesList)
                        recFilesDao.deleteFiles(recoveredFilesList.filter { !File(it.mediaPath).exists() }
                            .toMutableList())

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                imagesList = imagesList.sortedByDescending { it.dateModified }.toMutableList()

                recoveredList.clear()
                recoveredList = mutableListOf()
                recoveredList.addAll(imagesList)

                recoveredMediaList = ImageHelper.getGroupedMedia(recoveredList)
                StorageHelper._recoveredMediaList.postValue(recoveredList)

                withContext(Dispatchers.Main) {
                    if (showLoad) Toast.makeText(
                        ctx, getString(R.string.all_files_have_been_scanned), Toast.LENGTH_SHORT
                    ).show()
                    mediaAdapter.updateList(recoveredMediaList)
                    processData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                jobRecovery = null
            }
        }
    }

    private fun processData() {
        binding.run {
            clScanning.root.gone()
            btnRescan.visible()

            if (imagesList.isEmpty()) {
                arrayOf(txtFoundCounts, viewDivider1, rvImages).forEach { it.gone() }
                flStatus.visible()
                clEmpty.root.visible()
            } else {
                txtFoundCounts.text = String.format(
                    if (recoveryType == RECOVERY_TYPE_IMAGES) getString(R.string._images_found) else getString(
                        R.string._videos_found
                    ), imagesList.size
                )
                arrayOf(txtFoundCounts, viewDivider1, rvImages).forEach { it.visible() }
                clEmpty.root.gone()
                flStatus.gone()
            }
        }
    }

    private suspend fun checkFileOfDirectory(temp: String?, fileArr: Array<File?>) {
        for (i in fileArr.indices) {
            if (fileArr[i]?.isDirectory == true) {
                val temp_sub = fileArr[i]?.path
                checkFileOfDirectory(temp_sub, Utils.getFileList(fileArr[i]?.path))
            } else if (StorageHelper.isWithinScanFolders(
                    fileArr[i]?.path.toString(),
                    recoveryType
                )
            ) {
                val file = File(fileArr[i]?.path.toString())
                if (recoveryType == RECOVERY_TYPE_IMAGES) {
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeFile(fileArr[i]?.path, options)
                    if (!(options.outWidth == -1 || options.outHeight == -1)) {
                        if (photoExtensions.any { file.path.endsWith(it, true) }) {
                            imagesList.add(
                                RecoveryMedia(
                                    file.path, file.lastModified(), file.length(),
                                    itemType = ITEM_TYPE_MEDIA
                                )
                            )
                            numPhoto += 1
                        }
                    }
                } else {
                    if (videoExtensions.any { file.path.endsWith(it, true) }) {
                        imagesList.add(
                            RecoveryMedia(
                                file.path, file.lastModified(), file.length(),
                                itemType = ITEM_TYPE_MEDIA
                            )
                        )
                        numPhoto += 1
                    }
                }
            }
            withContext(Dispatchers.Main) {
                updateScannedText(numPhoto, fileArr[i]?.path.toString())
            }
        }
        if (imagesList.isNotEmpty() && temp?.contains(Config.RECOVER_DIRECTORY!!) == false) {
            imagesList.sortByDescending { it.dateModified }
        }
    }

    private fun updateScannedText(numPhoto: Int, path: String) {
        if (!isAdded) return
        binding.clScanning.run {
            txtNoOfItems.post {
                txtNoOfItems.text = String.format(
                    if (recoveryType == RECOVERY_TYPE_IMAGES) getString(R.string.s_photos) else getString(
                        R.string.s_videos
                    ), numPhoto
                )
            }
            txtPaths.post {
                txtPaths.text = File(path).parentFile?.name
            }
        }
    }

//    private suspend fun getSDCard() {
//            val externalStoragePaths = externalStorageDirectories
//            if (externalStoragePaths.isNotEmpty()) {
//                for (path in externalStoragePaths) {
//                    val file = File(path.toString())
//                    if (file.exists()) {
//                        val subFiles = file.listFiles()
//                        checkFileOfDirectory(path, subFiles)
//                    }
//                }
//            }
//        }
//    private val externalStorageDirectories: Array<String?>
//        get() {
//            val results: MutableList<String?> = ArrayList()
//            val externalDirs = ContextCompat.getExternalFilesDirs(ctx, null)
//            if (externalDirs.isNotEmpty()) {
//                for (file in externalDirs) {
//                    if (file != null) {
//                        val paths =
//                            file.path.split("/Android".toRegex()).dropLastWhile { it.isEmpty() }
//                                .toTypedArray()
//                        if (paths.isNotEmpty()) {
//                            val path = paths[0]
//                            var addPath = Environment.isExternalStorageRemovable(file)
//                            if (addPath) {
//                                results.add(path)
//                            }
//                        }
//                    }
//                }
//            }
//            if (results.isEmpty()) {
//                var output = ""
//                var ins: InputStream? = null
//                try {
//                    val process =
//                        ProcessBuilder(*arrayOfNulls(0)).command(*arrayOf("mount | grep /dev/block/vold"))
//                            .redirectErrorStream(true).start()
//                    process.waitFor()
//                    ins = process.inputStream
//                    val buffer = ByteArray(1024)
//                    while (ins.read(buffer) != -1) {
//                        output += String(buffer)
//                    }
//                    ins.close()
//                } catch (e: Exception) {
//                    if (ins != null) {
//                        try {
//                            ins.close()
//                        } catch (e2: IOException) {
//                        }
//                    }
//                }
//                if (!output.trim { it <= ' ' }.isEmpty()) {
//                    val devicePoints: Array<String> =
//                        output.split(IOUtils.LINE_SEPARATOR_UNIX.toRegex())
//                            .dropLastWhile { it.isEmpty() }.toTypedArray()
//                    if (devicePoints.isNotEmpty()) {
//                        for (voldPoint in devicePoints) {
//                            results.add(
//                                voldPoint.split(" ".toRegex())
//                                    .dropLastWhile { it.isEmpty() }.toTypedArray()[2]
//                            )
//                        }
//                    }
//                }
//            }
//            val storageDirectories = arrayOfNulls<String>(results.size)
//            for (i in results.indices) {
//                storageDirectories[i] = results[i]
//            }
//            return storageDirectories
//        }

    private var detailsDialog: AlertDialog? = null

    private fun showDetailsDialog(path: String) {
        val file = File(path)

        val dialogDetailsBinding = DialogDetailsBinding.inflate(layoutInflater)
        val builder =
            AlertDialog.Builder(ctx, R.style.RoundedCornersDialogTransp).setCancelable(false)
                .setView(dialogDetailsBinding.root)
        detailsDialog = builder.create()
        detailsDialog?.show()

        dialogDetailsBinding.run {
            txtName.text = file.name
            txtPath.text = file.path
            txtSize.text = file.length().formatSize()
            txtType.text = getMimeType(file)
            txtCreated.text =
                file.lastModified().convertToDateStr("dd MMM yyyy HH:MM").replaceFirst("0", "")

            btnOk.setOnClickListener {
                detailsDialog?.dismiss()
            }
        }
    }

    private var deleteDialog: AlertDialog? = null

    private fun showDeleteMultipleConfirmation(
        paths: List<String>
    ) {
        val dialogDeleteBinding = DialogDeleteBinding.inflate(layoutInflater)
        val builder =
            AlertDialog.Builder(ctx, R.style.RoundedCornersDialogTransp).setCancelable(false)
                .setView(dialogDeleteBinding.root)
        deleteDialog = builder.create()
        deleteDialog?.show()

        var deleted = false
        deleteDialog?.setOnDismissListener {
            if (deleted) {
                paths.forEach { path ->
                    imagesList.find { it.path == path }?.let { rMedia ->
                        imagesList.remove(rMedia)
                        recoveredList.remove(rMedia)
                    }
                }

                recoveredList.clear()
                recoveredList = mutableListOf()
                recoveredList.addAll(imagesList)

                recoveredMediaList = ImageHelper.getGroupedMedia(recoveredList)
                StorageHelper._recoveredMediaList.postValue(recoveredList)

                mediaAdapter.updateList(recoveredMediaList)

                binding.rvImages.post {
                    Toast.makeText(
                        ctx, getString(R.string.deleted_selected_files), Toast.LENGTH_SHORT
                    ).show()

                    processData()
                }
            }
        }

        dialogDeleteBinding.run {
            btnCancel.setOnClickListener {
                deleteDialog?.dismiss()
            }

            btnDelete.setOnClickListener {
                deleted = paths.all { File(it).delete() }
                MediaScannerConnection.scanFile(
                    ctx, paths.toTypedArray(), null
                ) { _, _ ->
                }
                deleted = true
                deleteDialog?.dismiss()
            }
        }
    }

    private var restoreDialog: AlertDialog? = null

    private fun showRestoreMultipleConfirmation(
        paths: List<String>
    ) {
        val dialogRestoreBinding = DialogRestoreBinding.inflate(layoutInflater)
        val builder =
            AlertDialog.Builder(ctx, R.style.RoundedCornersDialogTransp).setCancelable(false)
                .setView(dialogRestoreBinding.root)
        restoreDialog = builder.create()
        restoreDialog?.show()

        var restore = false
        restoreDialog?.setOnDismissListener {
            if (restore) {
                startRestoring(paths)
            }
        }

        dialogRestoreBinding.run {
            btnCancel.setOnClickListener {
                restoreDialog?.dismiss()
            }

            btnRestore.setOnClickListener {
                restore = true
                restoreDialog?.dismiss()
            }
        }
    }

    private var stopDialog: AlertDialog? = null

    private fun showStopTaskDialog(job: Job?) {
        val dialogRestoreBinding = DialogRestoreBinding.inflate(layoutInflater)
        val builder =
            AlertDialog.Builder(ctx, R.style.RoundedCornersDialogTransp).setCancelable(false)
                .setView(dialogRestoreBinding.root)
        stopDialog = builder.create()
        stopDialog?.show()

        var stop = false
        stopDialog?.setOnDismissListener {
            if (stop) {
                job?.cancel()
                onBackBtnPressed()
            }
        }

        dialogRestoreBinding.run {
            txtTitle.text = getString(R.string.stop_scanning_)
            btnRestore.text = getString(R.string.stop)
            btnCancel.text = getString(R.string.continue_)

            btnCancel.setOnClickListener {
                stopDialog?.dismiss()
            }

            btnRestore.setOnClickListener {
                stop = true
                stopDialog?.dismiss()
            }
        }
    }

    private var restorePD: AlertDialog? = null
    private var dialogRestoreBinding: DialogRestoreProgressBinding? = null

    private fun showRestorePD(paths: List<String>) {
        dialogRestoreBinding = DialogRestoreProgressBinding.inflate(layoutInflater)
        val builder =
            AlertDialog.Builder(ctx, R.style.RoundedCornersDialogTransp).setCancelable(false)
                .setView(dialogRestoreBinding?.root)
        restorePD = builder.create()
        restorePD?.show()

        restorePD?.setOnDismissListener {
            paths.forEach { path ->
                imagesList.find { it.path == path }?.let { rMedia ->
                    imagesList.remove(rMedia)
                    recoveredList.remove(rMedia)
                }
            }

            recoveredList.clear()
            recoveredList = mutableListOf()
            recoveredList.addAll(imagesList)

            recoveredMediaList = ImageHelper.getGroupedMedia(recoveredList)
            StorageHelper._recoveredMediaList.postValue(recoveredList)

            mediaAdapter.updateList(recoveredMediaList)

            binding.rvImages.post {
                Toast.makeText(
                    ctx, getString(R.string.restored_selected_files), Toast.LENGTH_SHORT
                ).show()

                processData()
            }
        }

        dialogRestoreBinding?.run {
            btnCancel.setOnClickListener {
                dismissRestorePD()
                jobRestore?.cancel()
            }
        }
    }

    private fun dismissRestorePD() {
        restorePD?.dismiss()
    }

    private fun publishProgress(progress: Int, total: Int) {
        if (total > 0) dialogRestoreBinding?.run {
            val progressInt = (progress * 100 / total)
            progressBar.setProgress(progressInt, true)
            txtProgress.post {
                txtProgress.text = "$progress/$total"
            }
            txtProgressPerc.post {
                txtProgressPerc.text = "$progressInt%"
            }
        }
    }

    private fun startRestoring(paths: List<String>) {
        if (jobRestore?.isActive == true || !isAdded) return

        var progress = 0

        val rootFile = File(
            Environment.getExternalStorageDirectory().path + File.separator + getString(R.string.app_name),
            getString(R.string.restored)
        )
        val restoreDir = File(
            rootFile,
            if (recoveryType == RECOVERY_TYPE_IMAGES) getString(R.string.images) else getString(R.string.videos)
        )
        if (!restoreDir.exists()) restoreDir.mkdirs()
        showRestorePD(paths)
        dialogRestoreBinding?.progressBar?.max = paths.size
        jobRestore = lifecycleScope.launch(Dispatchers.IO) {
            try {
                paths.forEach { path ->
                    val restoreFile = File(restoreDir, File(path).name)
                    val restored = File(path).renameTo(restoreFile)
                    if (restored) {
                        MediaScannerConnection.scanFile(
                            ctx, arrayOf(path, restoreFile.path), null
                        ) { _, _ ->
                        }
                        progress++
                        withContext(Dispatchers.Main) {
                            publishProgress(progress, paths.size)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                jobRestore = null
            }
        }.apply {
            invokeOnCompletion {
                binding.root.post {
                    dismissRestorePD()
                }
            }
        }
    }

    override fun onDestroy() {
        jobRecovery?.cancel()
        adsUtils.destroyNative()
        super.onDestroy()
    }

    override fun onBackBtnPressed() {
        if (jobRecovery?.isActive == true) {
            showStopTaskDialog(jobRecovery)
            return
        }
        if (jobRestore?.isActive == true) {
            showStopTaskDialog(jobRestore)
            return
        }
        findNavController().navigateUp()
    }

    override fun onMediaClick(mediaPath: String) {
        var selection = -1

        val bsMoreBinding = BottomSheetMoreOptionsBinding.inflate(layoutInflater)
        val bsDialog = BottomSheetDialog(ctx, R.style.BottomSheetDialogTheme)
        bsDialog.setContentView(bsMoreBinding.root)

        bsMoreBinding.run {
            llOpenWith.setOnClickListener {
                selection = 0
                bsDialog.dismiss()
            }

            llRestore.setOnClickListener {
                selection = 1
                bsDialog.dismiss()
            }

            llShare.setOnClickListener {
                selection = 2
                bsDialog.dismiss()
            }

            llDelete.setOnClickListener {
                selection = 3
                bsDialog.dismiss()
            }

            llDetails.setOnClickListener {
                selection = 4
                bsDialog.dismiss()
            }
        }

        bsDialog.show()

        bsDialog.setOnDismissListener {
            when (selection) {
                0 -> {
                    File(mediaPath).openFile(ctx)
                }

                1 -> {
                    showRestoreMultipleConfirmation(listOf(mediaPath))
                }

                2 -> {
                    File(mediaPath).shareFile(ctx)
                }

                3 -> {
                    showDeleteMultipleConfirmation(listOf(mediaPath))
                }

                4 -> {
                    Log.e("TAG", "onImageClick: $mediaPath")
                    showDetailsDialog(mediaPath)
                }
            }
        }
    }

    override fun onGroupClick(parentPath: String) {
        navigateToMediaDetails(parentPath)
    }
}