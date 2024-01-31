package com.filerecover.photorecovery.allrecover.restore.ui.fragments

import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.filerecover.photorecovery.allrecover.restore.R
import com.filerecover.photorecovery.allrecover.restore.adapters.DocsAdapter
import com.filerecover.photorecovery.allrecover.restore.databinding.BottomSheetMoreOptionsBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.DialogDeleteBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.DialogDetailsBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.DialogRestoreBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.DialogRestoreProgressBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.FragmentDocRecoveryBinding
import com.filerecover.photorecovery.allrecover.restore.db_helper.RecFilesDao
import com.filerecover.photorecovery.allrecover.restore.db_helper.RecoveredFiles
import com.filerecover.photorecovery.allrecover.restore.db_helper.RecoveredFilesDBClient
import com.filerecover.photorecovery.allrecover.restore.interfaces.AdEventListener
import com.filerecover.photorecovery.allrecover.restore.interfaces.MediaItemSelectionListener
import com.filerecover.photorecovery.allrecover.restore.models.RecoveryMedia
import com.filerecover.photorecovery.allrecover.restore.rv_touch_helper.DragSelectTouchListener
import com.filerecover.photorecovery.allrecover.restore.rv_touch_helper.DragSelectionProcessor
import com.filerecover.photorecovery.allrecover.restore.utils.AdsUtils
import com.filerecover.photorecovery.allrecover.restore.utils.Config
import com.filerecover.photorecovery.allrecover.restore.utils.ITEM_TYPE_MEDIA
import com.filerecover.photorecovery.allrecover.restore.utils.ImageHelper
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE_AUDIOS
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE_IMAGES
import com.filerecover.photorecovery.allrecover.restore.utils.StorageHelper
import com.filerecover.photorecovery.allrecover.restore.utils.Utils
import com.filerecover.photorecovery.allrecover.restore.utils.apkExtensions
import com.filerecover.photorecovery.allrecover.restore.utils.audioExtensions
import com.filerecover.photorecovery.allrecover.restore.utils.convertToDateStr
import com.filerecover.photorecovery.allrecover.restore.utils.documentExtensions
import com.filerecover.photorecovery.allrecover.restore.utils.dpToPx
import com.filerecover.photorecovery.allrecover.restore.utils.formatSize
import com.filerecover.photorecovery.allrecover.restore.utils.getAudioIcon
import com.filerecover.photorecovery.allrecover.restore.utils.getDocIcon
import com.filerecover.photorecovery.allrecover.restore.utils.getMimeType
import com.filerecover.photorecovery.allrecover.restore.utils.gone
import com.filerecover.photorecovery.allrecover.restore.utils.isOnline
import com.filerecover.photorecovery.allrecover.restore.utils.openFile
import com.filerecover.photorecovery.allrecover.restore.utils.setOnSingleClickListener
import com.filerecover.photorecovery.allrecover.restore.utils.shareFile
import com.filerecover.photorecovery.allrecover.restore.utils.visible
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.IOException
import java.io.InputStream

class DocsRecoveryFragment :
    BaseFragment<FragmentDocRecoveryBinding>(R.layout.fragment_doc_recovery),
    MediaItemSelectionListener {

    val adsUtils by lazy { AdsUtils.newInstance(ctx) }

    private val recFilesDao: RecFilesDao by lazy { RecoveredFilesDBClient.getInstance(ctx).recFilesDao }

    private val docsAdapter: DocsAdapter by lazy { DocsAdapter() }

    private var docsList: MutableList<RecoveryMedia> = mutableListOf()
    private var recoveredList: MutableList<RecoveryMedia> = mutableListOf()
    private var recoveredDocsList: MutableList<RecoveryMedia> = mutableListOf()

    private var isSelecting = false
    private var selectedList: MutableList<String> = mutableListOf()

    private var jobRecovery: Job? = null
    private var jobRestore: Job? = null
    var numDocs = 0
    private var recoveryType = RECOVERY_TYPE_AUDIOS

    private val dragSelectionProcessor: DragSelectionProcessor by lazy {
        DragSelectionProcessor(object : DragSelectionProcessor.ISelectionHandler {
            override val selection: Set<Int>
                get() = docsAdapter.selected

            override fun isSelected(index: Int): Boolean {
                return docsAdapter.getSelection().contains(index)
            }

            override fun updateSelection(
                start: Int, end: Int, isSelected: Boolean, calledFromOnStart: Boolean
            ) {
                docsAdapter.selectRange(start, end, isSelected)
            }
        })
    }
    private val onDragSelectionListener: DragSelectTouchListener.OnDragSelectListener by lazy { dragSelectionProcessor }
    private val mDragSelectTouchListener: DragSelectTouchListener by lazy {
        DragSelectTouchListener().withSelectListener(
            onDragSelectionListener
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDocRecoveryBinding.inflate(inflater, container, false)
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
            arguments?.getInt(RECOVERY_TYPE, RECOVERY_TYPE_AUDIOS) ?: RECOVERY_TYPE_AUDIOS

        setupRVMedia()

        lifecycleScope.launch(Dispatchers.IO) {
            val docsDB =
                (if (recoveryType == RECOVERY_TYPE_AUDIOS) recFilesDao.getAllAudios()
                    ?: mutableListOf() else recFilesDao.getAllDocs()
                    ?: mutableListOf()).filter { File(it.mediaPath).exists() }

            if (docsDB.isEmpty()) {
                showScanBtn()
            } else startRecovering(false, docsDB.map {
                val file = File(it.mediaPath)
                RecoveryMedia(
                    path = file.path,
                    dateModified = file.lastModified(),
                    size = file.length(),
                    appIcon = if (recoveryType == RECOVERY_TYPE_AUDIOS) getAudioIcon(".${file.extension}")
                    else getDocIcon(".${file.extension}"),
                    itemType = 2,
                    group = mutableListOf()
                )
            }.toMutableList())
        }

        binding.run {
            txtTitle.text =
                if (recoveryType == RECOVERY_TYPE_AUDIOS) getString(R.string.audio_recovery)
                else getString(R.string.document_recovery)

            btnRestore.animate().translationY((dpToPx(100) + shimmerFrame.root.height).toFloat())
                .apply {
                    interpolator = AccelerateInterpolator()
                    duration = 10
                    withEndAction {
                        btnRestore.post {
                            btnRestore.isEnabled = false
                            btnRestore.visible()
                        }
                    }
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
                startRecovering()
            }

            btnRestore.setOnClickListener {
                showRestoreMultipleConfirmation(selectedList)
            }

            btnDelete.setOnClickListener {
                showDeleteMultipleConfirmation(selectedList)
            }
        }
    }

    private fun setupRVMedia() {
        binding.run {
            rvDocs.setHasFixedSize(true)
            rvDocs.layoutManager = LinearLayoutManager(ctx)
            rvDocs.addOnItemTouchListener(mDragSelectTouchListener)
            rvDocs.isMotionEventSplittingEnabled = false
            rvDocs.adapter = docsAdapter
            docsAdapter.recoveryType = recoveryType
            docsAdapter.mediaSelectionListener = this@DocsRecoveryFragment

            docsAdapter.docsSelection.observe(viewLifecycleOwner) {
                handleResult(it)
            }
        }
    }

    private fun handleResult(list: MutableList<RecoveryMedia>) {
        isSelecting = list.isNotEmpty()
        selectedList = list.map { it.path }.toMutableList()
        if (isSelecting) {
            showRestoreBtn()
        } else {
            hideRestoreBtn()
        }
    }

    private fun clearSelection() {
        isSelecting = false
        selectedList.clear()
        docsAdapter.clearSelection()
    }

    private fun showLoading(showLoad: Boolean = false) {
        binding.run {
            root.post {
                clEmpty.root.gone()
                clDeepScan.root.gone()
                if (showLoad) {
                    arrayOf(txtFoundCounts, viewDivider1, rvDocs).forEach { it.gone() }
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
        docsListDB: MutableList<RecoveryMedia> = mutableListOf()
    ) {
        if (jobRecovery?.isActive == true || !isAdded) return
        showLoading(showLoad)
        if (isSelecting)
            clearSelection()
        binding.btnDelete.gone()
        binding.btnRescan.gone()
        jobRecovery = lifecycleScope.launch(Dispatchers.IO) {
            try {
                numDocs = 0
                docsList = mutableListOf()
                if (docsListDB.isNotEmpty()) {
                    docsList = docsListDB.onEach { it.itemType = ITEM_TYPE_MEDIA }
                } else {
                    try {
                        val internalPath = Environment.getExternalStorageDirectory().absolutePath
                        checkFileOfDirectory(
                            internalPath, Utils.getFileList(internalPath)
                        )

                        val recoveredFilesList = docsList.map {
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
                docsList = docsList.sortedByDescending { it.dateModified }.toMutableList()

                recoveredList.clear()
                recoveredList = mutableListOf()
                recoveredList.addAll(docsList)

                recoveredDocsList = ImageHelper.getGroupedMedia(recoveredList, recoveryType)

                withContext(Dispatchers.Main) {
                    if (showLoad) Toast.makeText(
                        ctx, getString(R.string.all_files_have_been_scanned), Toast.LENGTH_SHORT
                    ).show()
                    docsAdapter.updateList(recoveredDocsList)
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
            btnDelete.gone()

            if (docsList.isEmpty()) {
                arrayOf(txtFoundCounts, viewDivider1, rvDocs).forEach { it.gone() }
                clEmpty.root.visible()
            } else {
                txtFoundCounts.text = String.format(
                    if (recoveryType == RECOVERY_TYPE_IMAGES) getString(R.string._audios_found) else getString(
                        R.string._docs_found
                    ), docsList.size
                )
                arrayOf(txtFoundCounts, viewDivider1, rvDocs).forEach { it.visible() }
                clEmpty.root.gone()
            }
        }
    }

    private suspend fun checkFileOfDirectory(temp: String?, fileArr: Array<File?>) {
        for (i in fileArr.indices) {
            if (fileArr[i]?.isDirectory == true) {
                val tempSub = fileArr[i]?.path
                checkFileOfDirectory(tempSub, Utils.getFileList(fileArr[i]?.path))
            } else if (StorageHelper.isWithinScanFolders(fileArr[i]?.path, recoveryType)) {
                val file = File(fileArr[i]?.path.toString())
                if (if (recoveryType == RECOVERY_TYPE_AUDIOS)
                        audioExtensions.any { file.path.endsWith(it, true) }
                    else {
                        documentExtensions.any { file.path.endsWith(it, true) }
                                || apkExtensions.any { file.path.endsWith(it, true) }
                    }
                ) {
                    docsList.add(
                        RecoveryMedia(
                            file.path,
                            file.lastModified(),
                            file.length(),
                            appIcon = if (recoveryType == RECOVERY_TYPE_AUDIOS) getAudioIcon(".${file.extension}")
                            else getDocIcon(".${file.extension}"),
                            itemType = ITEM_TYPE_MEDIA,
                            group = mutableListOf()
                        )
                    )
                    numDocs += 1
                }
            }
            withContext(Dispatchers.Main) {
                updateScannedText(numDocs, fileArr[i]?.path.toString())
            }
        }
        if (docsList.isNotEmpty() && temp?.contains(Config.RECOVER_DIRECTORY.toString()) == false) {
            docsList.sortByDescending { it.dateModified }
        }
    }

    private fun updateScannedText(numDocs: Int, path: String) {
        if (!isAdded) return
        binding.clScanning.run {
            txtNoOfItems.post {
                txtNoOfItems.text = String.format(
                    if (recoveryType == RECOVERY_TYPE_AUDIOS) getString(R.string.s_audio) else getString(
                        R.string.s_documents
                    ), numDocs
                )
            }
            txtPaths.post {
                txtPaths.text = File(path).parentFile?.name
            }
        }
    }

    //    private val sdCard: Unit
//        get() {
//            val externalStoragePaths = externalStorageDirectories
//            if (externalStoragePaths.isNotEmpty()) {
//                for (path in externalStoragePaths) {
//                    val file = File(path)
//                    if (file.exists()) {
//                        val subFiles = file.listFiles()
//                        checkFileOfDirectory(path, subFiles)
//                    }
//                }
//            }
//        }
    private val externalStorageDirectories: Array<String?>
        get() {
            val results: MutableList<String?> = ArrayList<String?>()
            val externalDirs = ContextCompat.getExternalFilesDirs(ctx, null)
            if (externalDirs.isNotEmpty()) {
                for (file in externalDirs) {
                    if (file != null) {
                        val paths =
                            file.path.split("/Android".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        if (paths.isNotEmpty()) {
                            val path = paths[0]
                            var addPath = Environment.isExternalStorageRemovable(file)
                            if (addPath) {
                                results.add(path)
                            }
                        }
                    }
                }
            }
            if (results.isEmpty()) {
                var output = ""
                var ins: InputStream? = null
                try {
                    val process =
                        ProcessBuilder(*arrayOfNulls(0)).command(*arrayOf("mount | grep /dev/block/vold"))
                            .redirectErrorStream(true).start()
                    process.waitFor()
                    ins = process.inputStream
                    val buffer = ByteArray(1024)
                    while (ins.read(buffer) != -1) {
                        output += String(buffer)
                    }
                    ins.close()
                } catch (e: Exception) {
                    if (ins != null) {
                        try {
                            ins.close()
                        } catch (e2: IOException) {
                        }
                    }
                }
                if (!output.trim { it <= ' ' }.isEmpty()) {
                    val devicePoints: Array<String> =
                        output.split(IOUtils.LINE_SEPARATOR_UNIX.toRegex())
                            .dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (devicePoints.isNotEmpty()) {
                        for (voldPoint in devicePoints) {
                            results.add(
                                voldPoint.split(" ".toRegex())
                                    .dropLastWhile { it.isEmpty() }.toTypedArray()[2]
                            )
                        }
                    }
                }
            }
            val storageDirectories = arrayOfNulls<String>(results.size)
            for (i in results.indices) {
                storageDirectories[i] = results[i]
            }
            return storageDirectories
        }

    private var detailsDialog: AlertDialog? = null

    private fun showDetailsDialog(path: String) {
        val file = File(path)

        val dialogDetailsBinding = DialogDetailsBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(ctx, R.style.RoundedCornersDialogTransp)
            .setCancelable(false)
            .setView(dialogDetailsBinding.root)
        if (detailsDialog == null)
            detailsDialog = builder.create()
        if (detailsDialog?.isShowing == false)
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
        val builder = AlertDialog.Builder(ctx, R.style.RoundedCornersDialogTransp)
            .setCancelable(false)
            .setView(dialogDeleteBinding.root)
        deleteDialog = builder.create()
        deleteDialog?.show()

        var deleted = false
        deleteDialog?.setOnDismissListener {
            if (deleted) {
                paths.forEach { path ->
                    docsList.find { it.path == path }?.let { rMedia ->
                        docsList.remove(rMedia)
                        recoveredList.remove(rMedia)
                    }
                }
                recoveredList.clear()
                recoveredList = mutableListOf()
                recoveredList.addAll(docsList)

                recoveredDocsList = ImageHelper.getGroupedMedia(recoveredList, recoveryType)
                docsAdapter.updateList(recoveredDocsList)

                binding.rvDocs.post {
                    Toast.makeText(
                        ctx,
                        getString(R.string.deleted_selected_files),
                        Toast.LENGTH_SHORT
                    ).show()
                    processData()

                    if (isSelecting)
                        clearSelection()
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
                    ctx,
                    paths.toTypedArray(),
                    null
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
        val builder = AlertDialog.Builder(ctx, R.style.RoundedCornersDialogTransp)
            .setCancelable(false)
            .setView(dialogRestoreBinding.root)
        restoreDialog = builder.create()
        restoreDialog?.show()

        var restore = false
        restoreDialog?.setOnDismissListener {
            if (restore) {
                hideRestoreBtn()
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
        val builder = AlertDialog.Builder(ctx, R.style.RoundedCornersDialogTransp)
            .setCancelable(false)
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
        val builder = AlertDialog.Builder(ctx, R.style.RoundedCornersDialogTransp)
            .setCancelable(false)
            .setView(dialogRestoreBinding?.root)
        restorePD = builder.create()
        restorePD?.show()

        restorePD?.setOnDismissListener {
            paths.forEach { path ->
                docsList.find { it.path == path }?.let { rMedia ->
                    docsList.remove(rMedia)
                    recoveredList.remove(rMedia)
                }
            }

            recoveredList.clear()
            recoveredList = mutableListOf()
            recoveredList.addAll(docsList)

            recoveredDocsList = ImageHelper.getGroupedMedia(recoveredList, recoveryType)

            docsAdapter.updateList(recoveredDocsList)

            binding.rvDocs.post {
                Toast.makeText(
                    ctx,
                    getString(R.string.restored_selected_files),
                    Toast.LENGTH_SHORT
                ).show()
                processData()

                if (isSelecting)
                    clearSelection()
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
        if (total > 0)
            dialogRestoreBinding?.run {
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
            if (recoveryType == RECOVERY_TYPE_AUDIOS) getString(R.string.audio) else getString(R.string.documents)
        )
        if (!restoreDir.exists())
            restoreDir.mkdirs()
        showRestorePD(paths)
        dialogRestoreBinding?.progressBar?.max = paths.size
        jobRestore = lifecycleScope.launch(Dispatchers.IO) {
            try {
                paths.forEach { path ->
                    val restoreFile = File(restoreDir, File(path).name)
                    val restored = File(path).renameTo(restoreFile)
                    if (restored) {
                        MediaScannerConnection.scanFile(
                            ctx,
                            arrayOf(path, restoreFile.path),
                            null
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

    private fun showRestoreBtn() {
        binding.run {
            txtRestore.text = String.format(getString(R.string.restore_count), selectedList.size)
            btnRestore.animate().translationY(0f).apply {
                interpolator = AccelerateInterpolator()
                duration = 200
                withEndAction {
                    btnRestore.post {
                        btnRestore.isEnabled = true
                    }
                }
            }
        }
    }

    private fun hideRestoreBtn() {
        binding.run {
            txtRestore.text = getString(R.string.restore)
            btnRestore.animate().translationY((dpToPx(100) + shimmerFrame.root.height).toFloat())
                .apply {
                    interpolator = AccelerateInterpolator()
                    duration = 150
                    withEndAction {
                        btnRestore.post {
                            btnRestore.isEnabled = false
                        }
                    }
                }
        }
    }

    override fun onMediaClick(position: Int, path: String) {
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
                    File(path).openFile(ctx)
                }

                1 -> {
                    showRestoreMultipleConfirmation(listOf(path))
                }

                2 -> {
                    File(path).shareFile(ctx)
                }

                3 -> {
                    showDeleteMultipleConfirmation(listOf(path))
                }

                4 -> {
                    showDetailsDialog(path)
                }
            }
        }
    }

    override fun onMediaLongPressed(position: Int, path: String): Boolean {
        if (!isSelecting) {
            isSelecting = true
            docsAdapter.notifyItemRangeChanged(
                0,
                docsAdapter.itemCount,
                isSelecting
            )
        }
        mDragSelectTouchListener.startDragSelection(position)
        return true
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
        if (isSelecting) {
            clearSelection()
            return
        }
        findNavController().navigateUp()
    }
}