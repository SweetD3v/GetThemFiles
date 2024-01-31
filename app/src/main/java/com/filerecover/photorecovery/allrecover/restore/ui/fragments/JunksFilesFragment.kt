package com.filerecover.photorecovery.allrecover.restore.ui.fragments

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.filerecover.photorecovery.allrecover.restore.R
import com.filerecover.photorecovery.allrecover.restore.adapters.JunksAdapter
import com.filerecover.photorecovery.allrecover.restore.databinding.BottomSheetMoreOptionsBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.DialogDeleteBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.DialogDetailsBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.DialogRestoreBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.FragmentJunkFilesBinding
import com.filerecover.photorecovery.allrecover.restore.interfaces.AdEventListener
import com.filerecover.photorecovery.allrecover.restore.interfaces.MediaItemSelectionListener
import com.filerecover.photorecovery.allrecover.restore.models.JunkMedia
import com.filerecover.photorecovery.allrecover.restore.rv_touch_helper.DragSelectTouchListener
import com.filerecover.photorecovery.allrecover.restore.rv_touch_helper.DragSelectionProcessor
import com.filerecover.photorecovery.allrecover.restore.ui.activities.MainActivity
import com.filerecover.photorecovery.allrecover.restore.utils.AdsUtils
import com.filerecover.photorecovery.allrecover.restore.utils.Config
import com.filerecover.photorecovery.allrecover.restore.utils.ITEM_TYPE_MEDIA
import com.filerecover.photorecovery.allrecover.restore.utils.ImageHelper
import com.filerecover.photorecovery.allrecover.restore.utils.Utils
import com.filerecover.photorecovery.allrecover.restore.utils.apkExtensions
import com.filerecover.photorecovery.allrecover.restore.utils.beVisibleIf
import com.filerecover.photorecovery.allrecover.restore.utils.convertToDateStr
import com.filerecover.photorecovery.allrecover.restore.utils.formatSize
import com.filerecover.photorecovery.allrecover.restore.utils.formatSizeOnly
import com.filerecover.photorecovery.allrecover.restore.utils.formatSizeUnit
import com.filerecover.photorecovery.allrecover.restore.utils.getMimeType
import com.filerecover.photorecovery.allrecover.restore.utils.gone
import com.filerecover.photorecovery.allrecover.restore.utils.isOnline
import com.filerecover.photorecovery.allrecover.restore.utils.isOreoPlus
import com.filerecover.photorecovery.allrecover.restore.utils.shareFile
import com.filerecover.photorecovery.allrecover.restore.utils.tempExtensions
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

class JunksFilesFragment : BaseFragment<FragmentJunkFilesBinding>(R.layout.fragment_junk_files),
    MediaItemSelectionListener {

    private val adsUtils by lazy { AdsUtils.newInstance(ctx) }

    private val junksAdapter: JunksAdapter by lazy { JunksAdapter() }

    private var apksList: MutableList<JunkMedia> = mutableListOf()
    private var recoveredList: MutableList<JunkMedia> = mutableListOf()
    private var recoveredJunkList: MutableList<JunkMedia> = mutableListOf()

    private var isSelecting = false
    private var selectedList: MutableList<String> = mutableListOf()

    private var jobRecovery: Job? = null
    private var numApks = 0
    private var totalSize: MutableList<AppSizeModel> = mutableListOf()
    private val pm by lazy { ctx.packageManager }

    private val dragSelectionProcessor: DragSelectionProcessor by lazy {
        DragSelectionProcessor(object : DragSelectionProcessor.ISelectionHandler {
            override val selection: Set<Int>
                get() = junksAdapter.selected

            override fun isSelected(index: Int): Boolean {
                return junksAdapter.getSelection().contains(index)
            }

            override fun updateSelection(
                start: Int, end: Int, isSelected: Boolean, calledFromOnStart: Boolean
            ) {
                junksAdapter.selectRange(start, end, isSelected)
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
        binding = FragmentJunkFilesBinding.inflate(inflater, container, false)
        Config.STATUS_BAR_HEIGHT.observe(viewLifecycleOwner) { top ->
            binding.root.findViewById<RelativeLayout>(R.id.clStatus)
                ?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    height = top
                }
        }
        return getPersistentView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRVMedia()

        binding.run {
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

            root.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    root.postDelayed({
                        (requireActivity() as MainActivity).setStatusBarColor(fromJunk = true)
                    }, 150)
                    root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })

            btnBack.setOnClickListener {
                onBackBtnPressed()
            }

            startRecovering()

            btnClean.setOnClickListener {
                showDeleteMultipleConfirmation(selectedList)
            }
        }
    }

    private fun setupRVMedia() {
        binding.run {
            rvJunkFiles.setHasFixedSize(true)
            rvJunkFiles.layoutManager = LinearLayoutManager(ctx)
            rvJunkFiles.addOnItemTouchListener(mDragSelectTouchListener)
            rvJunkFiles.isMotionEventSplittingEnabled = false
            rvJunkFiles.adapter = junksAdapter
            junksAdapter.mediaSelectionListener = this@JunksFilesFragment

            junksAdapter.junksSelection.observe(viewLifecycleOwner) { list ->
                selectedList = list.map { it.path }.toMutableList()
                handleResult(selectedList)
            }
        }
    }

    private fun startRecovering() {
        if (jobRecovery?.isActive == true || !isAdded) return

        showCleaning(true)

        jobRecovery = lifecycleScope.launch(Dispatchers.IO) {
            numApks = 0
            totalSize.clear()
            apksList = mutableListOf()

//            try {
//                getInstalledApps().forEach { app ->
//                    apksList.add(
//                        JunkMedia(
//                            app.appName,
//                            app.path,
//                            size = app.appSizeLong,
//                            dateModified = app.dateModified,
//                            appIcon = app.icon,
//                            junkType = "Cache files"
//                        )
//                    )
//                    totalSize.add(AppSizeModel().apply {
//                        this.path = app.path
//                        this.appSizeLong = app.appSizeLong
//                    })
//                    updateScannedText(
//                        totalSize.sumOf { it.appSizeLong }.formatSizeOnly(),
//                        totalSize.sumOf { it.appSizeLong }.formatSizeUnit()
//                    )
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }

//            getJunkSize()

            try {
                val rootPath = Environment.getExternalStorageDirectory().absolutePath
                checkFileOfDirectory(
                    rootPath, Utils.getFileList(rootPath)
                )
                Log.e("TAG", "startRecovering: $apksList")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            apksList.removeAll { it.size == 0L }
            apksList = apksList.sortedByDescending { it.dateModified }.toMutableList()

            recoveredList.clear()
            recoveredList = mutableListOf()
            recoveredList.addAll(apksList)

            recoveredJunkList = ImageHelper.getGroupedJunks(recoveredList)

            withContext(Dispatchers.Main) {
                showCleaning(false)

                Toast.makeText(
                    ctx, getString(R.string.all_files_have_been_scanned), Toast.LENGTH_SHORT
                ).show()

                junksAdapter.updateList(recoveredJunkList)
                junksAdapter.selectAll()
                processData()
            }
        }
    }

    private fun showCleaning(show: Boolean) {
        binding.run {
            if (show) {
                arrayOf(txtTotalSize, txtSizeUnit, txtFiles).forEach { it.alpha = 0f }
            } else {
                flScanning.post {
                    flScanning.animate().alpha(0f).apply {
                        duration = 450
                        interpolator = DecelerateInterpolator()
                        withEndAction {
                            flScanning.gone()
                        }
                    }.start()
                }

                txtTotalSize.post {
                    arrayOf(txtTotalSize, txtSizeUnit, txtFiles).forEach {
                        it.animate().alpha(1f).apply {
                            duration = 500
                            interpolator = AccelerateInterpolator()
                            withEndAction {
                                txtTotalSize.visible()
                                txtSizeUnit.visible()
                                txtFiles.visible()
                            }
                        }.start()
                    }
                }
            }
        }
    }

    private suspend fun getJunkSize() {
        val tempsList: MutableList<File> = mutableListOf()
        val rootPath = Environment.getExternalStorageDirectory().absolutePath
        checkTempFile(tempsList, Utils.getFileList(rootPath)).forEach { file ->
            apksList.add(
                JunkMedia(
                    file.name,
                    file.path,
                    file.lastModified(),
                    file.length(),
                    junkType = "Cache files"
                )
            )

            withContext(Dispatchers.Main) {
                updateScannedText(
                    totalSize.count().toLong().formatSizeOnly(),
                    totalSize.count().toLong().formatSizeUnit()
                )
            }

            numApks += 1
            totalSize.add(AppSizeModel().apply {
                this.path = file.path
                this.appSizeLong = file.length()
            })
        }

        if (apksList.isNotEmpty()) {
            apksList.sortByDescending { it.dateModified }
        }
    }

    private fun checkTempFile(
        tempsList: MutableList<File>,
        fileArr: Array<File?>
    ): MutableList<File> {
        for (i in fileArr.indices) {
            if (fileArr[i]?.isDirectory == true) {
                checkTempFile(tempsList, Utils.getFileList(fileArr[i]?.path))
            } else {
                val file = File(fileArr[i]?.path.toString())
                if (tempExtensions.any { file.path.endsWith(it, true) }) {
                    tempsList.add(file)
                }
            }
        }
        return tempsList
    }

    private fun getInstalledApps(): List<AppFileModel> {
        val arrayList: MutableList<AppFileModel> = ArrayList()
        val packageManager = ctx.packageManager
        val installedPackages: List<PackageInfo> =
            packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        val appsCount = installedPackages.size
        var i = 0
        while (i < appsCount) {
            val packageInfo: PackageInfo = installedPackages[i]
            val applicationInfo = packageManager.getApplicationInfo(packageInfo.packageName, 0)
            val cacheDir = applicationInfo.dataDir
            if (packageManager.getLaunchIntentForPackage(packageInfo.packageName) != null) {
                if (cacheDir.isNotEmpty() && !cacheDir.startsWith("/vendor/", false)) {
                    val appModel = AppFileModel().apply {
                        this.appName =
                            packageInfo.applicationInfo.loadLabel(packageManager).toString()
                        this.path = cacheDir
                        this.dateModified = packageInfo.lastUpdateTime
                        this.icon = packageInfo.applicationInfo.loadIcon(packageManager)

                        val file = File(cacheDir)

                        val cacheSize: Long = if (isOreoPlus()) {
                            calculateCacheSize(applicationInfo)
                        } else {
                            file.length()
                        }

                        Log.e(
                            "TAG",
                            "getInstalledApps: ${file.path} - ${file.length().formatSize()}"
                        )

                        this.appSizeLong = cacheSize
                    }
                    arrayList.add(appModel)
                }
            }
            i++
        }
        return arrayList.sortedByDescending { it.appSizeLong }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateCacheSize(applicationInfo: ApplicationInfo): Long {
        val storageStatsManager =
            ctx.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
        val packageName = applicationInfo.packageName
        val storageStats =
            storageStatsManager.queryStatsForUid(applicationInfo.storageUuid, applicationInfo.uid)
        return storageStats.cacheBytes
    }

    private suspend fun checkFileOfDirectory(temp: String?, fileArr: Array<File?>) {
        for (i in fileArr.indices) {
            if (fileArr[i]?.isDirectory == true) {
                val tempSub = fileArr[i]?.path
                checkFileOfDirectory(tempSub, Utils.getFileList(fileArr[i]?.path))
            } else {
                val file = File(fileArr[i]?.path.toString())
                if (apkExtensions.any { file.path.endsWith(it, true) }) {
                    val info = pm.getPackageArchiveInfo(file.path, 0)
                    info?.let { packageInfo ->
                        packageInfo.applicationInfo?.sourceDir = file.path
                        packageInfo.applicationInfo?.publicSourceDir = file.path
                        apksList.add(
                            JunkMedia(
                                file.name,
                                file.path,
                                file.lastModified(),
                                file.length(),
                                appIcon = packageInfo.applicationInfo.loadIcon(pm),
                                junkType = "APKs",
                                itemType = ITEM_TYPE_MEDIA
                            )
                        )
                        numApks += 1
                        totalSize.add(AppSizeModel().apply {
                            this.path = file.path
                            this.appSizeLong = file.length()
                        })
                    }
                }
            }
            withContext(Dispatchers.Main) {
                updateScannedText(
                    totalSize.count().toLong().formatSizeOnly(),
                    totalSize.count().toLong().formatSizeUnit()
                )
            }
        }
        if (apksList.isNotEmpty() && temp?.contains(Config.RECOVER_DIRECTORY.toString()) == false) {
            apksList.sortByDescending { it.dateModified }
        }
    }

    private fun updateScannedText(size: String, unit: String) {
        binding.run {
            txtTotalSize.post {
                txtTotalSize.text = "$size"
                txtSizeUnit.text = "$unit"
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
                    apksList.find { it.path == path }?.let { rMedia ->
                        apksList.remove(rMedia)
                        recoveredList.remove(rMedia)
                    }
                }
                recoveredList.clear()
                recoveredList = mutableListOf()
                recoveredList.addAll(apksList)

                recoveredJunkList = ImageHelper.getGroupedJunks(recoveredList)
                junksAdapter.updateList(recoveredJunkList)

                binding.rvJunkFiles.post {
                    Toast.makeText(
                        ctx,
                        getString(R.string.deleted_selected_files),
                        Toast.LENGTH_SHORT
                    ).show()

                    processData()
                    if (isSelecting) {
                        clearSelection()
                        navigateToHome()
                    }
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

    private fun navigateToHome() {
        adsUtils.destroyNative()
        Intent(ctx, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ctx.startActivity(this)
        }
    }

    private fun processData() {
        binding.run {
            if (apksList.isEmpty()) {
                rvJunkFiles.gone()
                clEmptyParent.visible()
                clEmpty.root.visible()
            } else {
                rvJunkFiles.visible()
                clEmptyParent.gone()
                clEmpty.root.gone()
            }
        }
    }

    private fun handleResult(selectedPaths: MutableList<String>) {
        isSelecting = selectedPaths.isNotEmpty()
        binding.btnClean.beVisibleIf(isSelecting)

        totalSize.retainAll { selectedPaths.contains(it.path) }
        selectedPaths.forEach { path ->
            if (!totalSize.map { ts -> ts.path }.contains(path))
                totalSize.add(AppSizeModel().apply {
                    this.path = path
                    this.appSizeLong = File(path).length()
                })
        }

        updateScannedText(
            totalSize.sumOf { it.appSizeLong }.formatSizeOnly(),
            totalSize.sumOf { it.appSizeLong }.formatSizeUnit()
        )
    }

    private fun clearSelection() {
        isSelecting = false
        selectedList.clear()
        junksAdapter.clearSelection()
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

                }

                1 -> {

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
            junksAdapter.notifyItemRangeChanged(
                0,
                junksAdapter.itemCount,
                isSelecting
            )
        }
        mDragSelectTouchListener.startDragSelection(position)
        return true
    }

    override fun onBackBtnPressed() {
        if (jobRecovery?.isActive == true) {
            showStopTaskDialog(jobRecovery)
            return
        }
        if (isSelecting) {
            clearSelection()
            return
        }
        (requireActivity() as MainActivity).setStatusBarColor()
        findNavController().navigateUp()
    }
}

class AppFileModel {
    var appName = ""
    var path = ""
    var appSizeLong = 0L
    var dateModified = 0L
    var icon: Drawable? = null
}

class AppSizeModel {
    var path = ""
    var appSizeLong = 0L
}