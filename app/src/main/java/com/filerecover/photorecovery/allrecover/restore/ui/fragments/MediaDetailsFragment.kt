package com.filerecover.photorecovery.allrecover.restore.ui.fragments

import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.filerecover.photorecovery.allrecover.restore.R
import com.filerecover.photorecovery.allrecover.restore.adapters.MediaDetailsListAdapter
import com.filerecover.photorecovery.allrecover.restore.databinding.BottomSheetMoreOptionsBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.DialogDeleteBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.DialogDetailsBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.DialogRestoreBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.DialogRestoreProgressBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.FragmentMediaDetailsBinding
import com.filerecover.photorecovery.allrecover.restore.interfaces.AdBannerEventListener
import com.filerecover.photorecovery.allrecover.restore.interfaces.MediaItemSelectionListener
import com.filerecover.photorecovery.allrecover.restore.models.RecoveryMedia
import com.filerecover.photorecovery.allrecover.restore.rv_touch_helper.DragSelectTouchListener
import com.filerecover.photorecovery.allrecover.restore.rv_touch_helper.DragSelectionProcessor
import com.filerecover.photorecovery.allrecover.restore.utils.ARGS_MEDIA_LIST_DETAILS
import com.filerecover.photorecovery.allrecover.restore.utils.AdsUtils
import com.filerecover.photorecovery.allrecover.restore.utils.Config
import com.filerecover.photorecovery.allrecover.restore.utils.MEDIA_GRID_SIZE
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE_IMAGES
import com.filerecover.photorecovery.allrecover.restore.utils.StorageHelper
import com.filerecover.photorecovery.allrecover.restore.utils.convertToDateStr
import com.filerecover.photorecovery.allrecover.restore.utils.dpToPx
import com.filerecover.photorecovery.allrecover.restore.utils.formatSize
import com.filerecover.photorecovery.allrecover.restore.utils.getMimeType
import com.filerecover.photorecovery.allrecover.restore.utils.gone
import com.filerecover.photorecovery.allrecover.restore.utils.isOnline
import com.filerecover.photorecovery.allrecover.restore.utils.openFile
import com.filerecover.photorecovery.allrecover.restore.utils.shareFile
import com.filerecover.photorecovery.allrecover.restore.utils.visible
import com.google.android.gms.ads.AdView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MediaDetailsFragment :
    BaseFragment<FragmentMediaDetailsBinding>(R.layout.fragment_media_details),
    MediaItemSelectionListener {

    private var jobRestore: Job? = null

    private val adsUtils by lazy { AdsUtils.newInstance(ctx) }
    private var adView: AdView? = null

    private var recoveredMediaList: MutableList<RecoveryMedia> = mutableListOf()
    private var selectedList: MutableList<String> = mutableListOf()
    private val mediaDetailsAdapter by lazy { MediaDetailsListAdapter() }
    private var recoveryType: Int = RECOVERY_TYPE_IMAGES
    private var isSelecting = false

    private val dragSelectionProcessor: DragSelectionProcessor by lazy {
        DragSelectionProcessor(object : DragSelectionProcessor.ISelectionHandler {
            override val selection: Set<Int>
                get() = mediaDetailsAdapter.selected

            override fun isSelected(index: Int): Boolean {
                return mediaDetailsAdapter.getSelection().contains(index)
            }

            override fun updateSelection(
                start: Int, end: Int, isSelected: Boolean, calledFromOnStart: Boolean
            ) {
                mediaDetailsAdapter.selectRange(start, end, isSelected)
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMediaDetailsBinding.inflate(layoutInflater)
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

        binding.run {
            txtTitle.text =
                if (recoveryType == RECOVERY_TYPE_IMAGES) getString(R.string.image_recovery)
                else getString(R.string.video_recovery)
        }

        setupRVMedia()
        updateMediaList()

        binding.run {
            if (adView == null) {
                if (ctx.isOnline())
                    adsUtils.loadBanner(
                        requireActivity() as AppCompatActivity,
                        bannerContainer,
                        R.string.banner_id,
                        object : AdBannerEventListener {
                            override fun onAdLoaded(adView: AdView?) {
                                this@MediaDetailsFragment.adView = adView
                                shimmerFrameBanner.root.stopShimmer()
                                shimmerFrameBanner.root.gone()
                            }

                            override fun onAdClosed() {
                            }

                            override fun onLoadError(errorCode: String?) {
                                Log.e("TAG", "onLoadError: $errorCode")
                            }
                        })
                else shimmerFrameBanner.root.gone()
            } else {
                shimmerFrameBanner.root.stopShimmer()
                shimmerFrameBanner.root.gone()
                adView?.apply {
                    if (parent != null) {
                        (parent as ViewGroup).removeView(this)
                        bannerContainer.addView(this)
                    }
                }
            }

            btnRestore.animate()
                .translationY((dpToPx(100) + shimmerFrameBanner.root.height).toFloat())
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

            btnBack.setOnClickListener {
                onBackBtnPressed()
            }
            btnRestore.setOnClickListener {
                showRestoreMultipleConfirmation(selectedList)
            }
        }
    }

    private fun setupRVMedia() {
        binding.run {

            rvImages.setHasFixedSize(true)
            rvImages.layoutManager = GridLayoutManager(ctx, MEDIA_GRID_SIZE)
            rvImages.addOnItemTouchListener(mDragSelectTouchListener)
            rvImages.isMotionEventSplittingEnabled = false
            rvImages.adapter = mediaDetailsAdapter
            mediaDetailsAdapter.recoveryType = recoveryType
            mediaDetailsAdapter.mediaSelectionListener = this@MediaDetailsFragment

            mediaDetailsAdapter.mediaSelection.observe(viewLifecycleOwner) {
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

    private fun updateMediaList() {
        val parentPath = arguments?.getString(ARGS_MEDIA_LIST_DETAILS) ?: ""

        StorageHelper.recoveredMediaList.observe(viewLifecycleOwner) { mediaList ->
            recoveredMediaList =
                mediaList.filter { File(it.path).parent == parentPath }.toMutableList()

            mediaDetailsAdapter.updateList(recoveredMediaList)
        }
    }

    private fun clearSelection() {
        isSelecting = false
        selectedList.clear()
        mediaDetailsAdapter.clearSelection()
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
                    Log.e("TAG", "onImageClick: $path")
                    showDetailsDialog(path)
                }
            }
        }
    }

    override fun onMediaLongPressed(position: Int, path: String): Boolean {
        if (!isSelecting) {
            isSelecting = true
            mediaDetailsAdapter.notifyItemRangeChanged(
                0,
                mediaDetailsAdapter.itemCount,
                isSelecting
            )
        }
        mDragSelectTouchListener.startDragSelection(position)
        return true
    }

    private fun processData() {
        binding.run {
            if (recoveredMediaList.isEmpty()) {
                clEmpty.root.visible()
            } else {
                clEmpty.root.gone()
            }
        }
    }

    private fun showRestoreBtn() {
        binding.run {
            btnDelete.visible()
            txtRestore.text = String.format(
                getString(R.string.restore_count),
                mediaDetailsAdapter.getSelection().size
            )
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
            btnDelete.gone()
            txtRestore.text = getString(R.string.restore)
            btnRestore.animate()
                .translationY((dpToPx(100) + shimmerFrameBanner.root.height).toFloat())
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
//                paths.forEach { path ->
//                    recoveredMediaList.find { it.path == path }?.let { rMedia ->
//                        recoveredMediaList.remove(rMedia)
//                    }
//                }

                val newList = StorageHelper.recoveredMediaList.value
                newList?.removeAll { paths.any { path -> it.path == path } }
                StorageHelper._recoveredMediaList.postValue(newList)

                binding.rvImages.post {
                    Toast.makeText(
                        ctx, getString(R.string.deleted_selected_files), Toast.LENGTH_SHORT
                    ).show()

                    processData()
                    if (isSelecting) clearSelection()
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
            Log.e("TAG", "showRestorePD: ${recoveredMediaList.size}")
//            paths.forEach { path ->
//                recoveredMediaList.find { it.path == path }?.let { rMedia ->
//                    recoveredMediaList.remove(rMedia)
//                }
//            }
            val newList = StorageHelper._recoveredMediaList.value
            newList?.removeAll { paths.any { path -> it.path == path } }
            StorageHelper._recoveredMediaList.postValue(newList)

            binding.rvImages.post {
                Toast.makeText(
                    ctx, getString(R.string.restored_selected_files), Toast.LENGTH_SHORT
                ).show()

                processData()
                if (isSelecting) clearSelection()
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

    override fun onBackBtnPressed() {
        if (isSelecting) {
            mediaDetailsAdapter.clearSelection()
            return
        }
        findNavController().navigateUp()
    }

    override fun onDestroy() {
        adsUtils.destroyBanner()
        super.onDestroy()
    }
}