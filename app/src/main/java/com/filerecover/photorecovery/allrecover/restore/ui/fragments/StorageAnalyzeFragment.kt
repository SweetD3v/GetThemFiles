package com.filerecover.photorecovery.allrecover.restore.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.filerecover.photorecovery.allrecover.restore.R
import com.filerecover.photorecovery.allrecover.restore.StorageViewModel
import com.filerecover.photorecovery.allrecover.restore.adapters.StorageStatsAdapter
import com.filerecover.photorecovery.allrecover.restore.databinding.FragmentStorageAnalyzeBinding
import com.filerecover.photorecovery.allrecover.restore.interfaces.AdBannerEventListener
import com.filerecover.photorecovery.allrecover.restore.models.CallbackStatus
import com.filerecover.photorecovery.allrecover.restore.models.FileTypes
import com.filerecover.photorecovery.allrecover.restore.models.Result
import com.filerecover.photorecovery.allrecover.restore.models.StatsModel
import com.filerecover.photorecovery.allrecover.restore.utils.AdsUtils
import com.filerecover.photorecovery.allrecover.restore.utils.Config
import com.filerecover.photorecovery.allrecover.restore.utils.PrefsManager
import com.filerecover.photorecovery.allrecover.restore.utils.StorageHelper.getRelativeStorageSize
import com.filerecover.photorecovery.allrecover.restore.utils.formatSize
import com.filerecover.photorecovery.allrecover.restore.utils.getFreeStorageSpace
import com.filerecover.photorecovery.allrecover.restore.utils.getTotalStorageSpace
import com.filerecover.photorecovery.allrecover.restore.utils.gone
import com.filerecover.photorecovery.allrecover.restore.utils.isOnline
import com.filerecover.photorecovery.allrecover.restore.utils.setOnSingleClickListener
import com.google.android.gms.ads.AdView
import java.text.DecimalFormat

class StorageAnalyzeFragment :
    BaseFragment<FragmentStorageAnalyzeBinding>(R.layout.fragment_storage_analyze) {

    private val adsUtils by lazy { AdsUtils.newInstance(ctx) }
    private var adView: AdView? = null

    private val statsAdapter: StorageStatsAdapter by lazy { StorageStatsAdapter() }
    private var statsList: MutableList<StatsModel> = mutableListOf()

    private val storageVM: StorageViewModel by activityViewModels()
    private val prefs: PrefsManager by lazy { PrefsManager.newInstance(ctx) }

    private var videoSize = -1L
    private var imageSize = -1L
    private var audioSize = -1L
    private var docSize = -1L
    private var apkSize = -1L
    private var systemSize = -1L
    private var otherSize = -1L
    private var availableSize = -1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStorageAnalyzeBinding.inflate(inflater, container, false)
        Config.STATUS_BAR_HEIGHT.observe(viewLifecycleOwner) { top ->
            binding.root.findViewById<RelativeLayout>(R.id.clStatus)
                ?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    height = top
                }
        }
        return getPersistentView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        adsUtils.loadInterstitialAd(R.string.interstitial_id)

        storageVM.dataSizes.observe(viewLifecycleOwner) { value ->
            handleResult(value)
        }

        videoSize = prefs.getLong("videoSize", -1L)
        imageSize = prefs.getLong("imageSize", -1L)
        audioSize = prefs.getLong("audioSize", -1L)
        docSize = prefs.getLong("docSize", -1L)
        apkSize = prefs.getLong("apkSize", -1L)
        systemSize = prefs.getLong("systemSize", -1L)
        otherSize = prefs.getLong("otherSize", -1L)
        availableSize = prefs.getLong("availableSize", -1L)

        binding.run {
            if (adView == null) {
                if (ctx.isOnline())
                    adsUtils.loadBanner(
                        requireActivity() as AppCompatActivity,
                        bannerContainer,
                        R.string.banner_id,
                        object : AdBannerEventListener {
                            override fun onAdLoaded(adView: AdView?) {
                                this@StorageAnalyzeFragment.adView = adView
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

            rvStorageList.layoutManager = LinearLayoutManager(ctx)
            rvStorageList.adapter = statsAdapter
            initDummyData()

            btnBack.setOnClickListener {
                onBackBtnPressed()
            }
            btnCleanUp.setOnClickListener {
                navigateToJunkFiles()
            }
            btnRescan.setOnSingleClickListener {
                videoSize = -1L
                imageSize = -1L
                apkSize = -1L
                audioSize = -1L
                docSize = -1L
                systemSize = -1L
                otherSize = -1L
                availableSize = -1L

                storageVM.stopAnalyzing()
                initDummyData()
                storageVM.getMediaSizes()
            }

            storageVM.getMediaSizes()
        }
    }

    private fun navigateToJunkFiles() {
        Log.e("TAG", "navigateToJunkFiles: ")
        findNavController().navigate(R.id.action_storage_analyze_to_junk)
    }

    private fun handleResult(result: Result?) {
        if (result != null && result.status == CallbackStatus.SUCCESS) {
            when (result.fileTypes) {
                FileTypes.VIDEO -> {
                    val statsModel = StatsModel(
                        getString(R.string.videos),
                        ContextCompat.getColor(ctx, R.color.color_videos),
                        result
                    )
                    statsList.find { it.result.fileTypes == FileTypes.VIDEO }?.let { stat ->
                        stat.apply {
                            this.result = result
                        }
                    } ?: statsList.add(statsModel)

                    videoSize = result.size
                    prefs.putLong("videoSize", videoSize)
                    populateVideoPerc(result)
                }

                FileTypes.IMAGE -> {
                    val statsModel = StatsModel(
                        getString(R.string.images),
                        ContextCompat.getColor(ctx, R.color.color_images),
                        result
                    )
                    statsList.find { it.result.fileTypes == FileTypes.IMAGE }?.let { stat ->
                        stat.apply {
                            this.result = result
                        }
                    } ?: statsList.add(statsModel)

                    imageSize = result.size
                    prefs.putLong("imageSize", imageSize)
                    populateImagePerc(result)
                }

                FileTypes.AUDIO -> {
                    val statsModel = StatsModel(
                        getString(R.string.audio),
                        ContextCompat.getColor(ctx, R.color.color_audio_files),
                        result
                    )
                    statsList.find { it.result.fileTypes == FileTypes.AUDIO }?.let { stat ->
                        stat.apply {
                            this.result = result
                        }
                    } ?: statsList.add(statsModel)

                    audioSize = result.size
                    prefs.putLong("audioSize", audioSize)
                    populateAudioPerc(result)
                }

                FileTypes.DOCUMENT -> {
                    val statsModel = StatsModel(
                        getString(R.string.documents),
                        ContextCompat.getColor(ctx, R.color.color_documents),
                        result
                    )
                    statsList.find { it.result.fileTypes == FileTypes.DOCUMENT }?.let { stat ->
                        stat.apply {
                            this.result = result
                        }
                    } ?: statsList.add(statsModel)

                    docSize = result.size
                    prefs.putLong("docSize", docSize)
                    populateDocsPerc(result)
                }

                FileTypes.APK -> {
                    val statsModel = StatsModel(
                        getString(R.string.apps),
                        ContextCompat.getColor(ctx, R.color.color_apps),
                        result
                    )

                    statsList.find { it.result.fileTypes == FileTypes.APK }?.let { stat ->
                        stat.apply {
                            this.result = result
                        }
                    } ?: statsList.add(statsModel)

                    apkSize = result.size
                    prefs.putLong("apkSize", apkSize)
                    populateApkPerc(result)
                }

                FileTypes.SYSTEM -> {
                    val statsModel = StatsModel(
                        getString(R.string.system),
                        ContextCompat.getColor(ctx, R.color.color_systems),
                        result
                    )

                    statsList.find { it.result.fileTypes == FileTypes.SYSTEM }?.let { stat ->
                        stat.apply {
                            this.result = result
                        }
                    } ?: statsList.add(statsModel)

                    systemSize = result.size
                    prefs.putLong("systemSize", systemSize)
                    populateSystemPerc(result)
                }

                FileTypes.OTHER -> {
                    val statsModel = StatsModel(
                        getString(R.string.other),
                        ContextCompat.getColor(ctx, R.color.color_other),
                        result
                    )

                    statsList.find { it.result.fileTypes == FileTypes.OTHER }?.let { stat ->
                        stat.apply {
                            this.result = result
                        }
                    } ?: statsList.add(statsModel)

                    otherSize = result.size
                    prefs.putLong("otherSize", otherSize)
                    populateOtherPerc(result)

                    availableSize = statsList.sumOf { it.result.size }
                    prefs.putLong("availableSize", availableSize)
                    populateAvailablePerc()
                }
            }

            val model = statsList.find { it.result.fileTypes == result.fileTypes }
            statsAdapter.updateStat(model)
            statsAdapter.updateList(statsList, index = statsList.indexOf(model))
        }
    }

    private fun populateVideoPerc(result: Result) {
        val videoPerc = result.size.getRelativeStorageSize()
        binding.run {
            val lp = imgVideos.layoutParams as LinearLayout.LayoutParams
            lp.weight = videoPerc
            imgVideos.layoutParams = lp
        }
    }

    private fun populateImagePerc(result: Result) {
        val imagePerc = result.size.getRelativeStorageSize()

        binding.run {
            val lp = imgImages.layoutParams as LinearLayout.LayoutParams
            lp.weight = imagePerc
            imgImages.layoutParams = lp
        }
    }

    private fun populateAudioPerc(result: Result) {
        val audioPerc = result.size.getRelativeStorageSize()

        binding.run {
            val lp = imgAudioFiles.layoutParams as LinearLayout.LayoutParams
            lp.weight = audioPerc
            imgAudioFiles.layoutParams = lp
        }
    }

    private fun populateDocsPerc(result: Result) {
        val docPerc = result.size.getRelativeStorageSize()

        binding.run {
            val lp = imgDocuments.layoutParams as LinearLayout.LayoutParams
            lp.weight = docPerc
            imgDocuments.layoutParams = lp
        }
    }

    private fun populateApkPerc(result: Result) {
        val appPerc = result.size.getRelativeStorageSize()

        binding.run {
            val lp = imgApps.layoutParams as LinearLayout.LayoutParams
            lp.weight = appPerc
            imgApps.layoutParams = lp
        }
    }

    private fun populateSystemPerc(result: Result) {
        val systemPerc = result.size.getRelativeStorageSize()

        binding.run {
            val lp = imgSystem.layoutParams as LinearLayout.LayoutParams
            lp.weight = systemPerc
            imgSystem.layoutParams = lp
        }
    }

    private fun populateOtherPerc(result: Result) {
        val otherPerc = result.size.getRelativeStorageSize()

        binding.run {
            val lp = imgOther.layoutParams as LinearLayout.LayoutParams
            lp.weight = otherPerc
            imgOther.layoutParams = lp
        }
    }

    private fun populateAvailablePerc() {
        val availablePerc = if (availableSize != -1L) availableSize.getRelativeStorageSize()
        else statsList.sumOf { it.result.size }.getRelativeStorageSize()

        binding.run {
            val lp = imgAvailable.layoutParams as LinearLayout.LayoutParams
            lp.weight = availablePerc
            imgAvailable.layoutParams = lp
        }
    }

    private fun initDummyData() {
        val resultVideo = if (videoSize != -1L) {
            Result(CallbackStatus.SUCCESS, FileTypes.VIDEO).apply {
                this.size = videoSize
            }
        } else Result(CallbackStatus.LOADING, FileTypes.VIDEO)
        populateVideoPerc(resultVideo)

        val resultImage = if (imageSize != -1L) {
            Result(CallbackStatus.SUCCESS, FileTypes.IMAGE).apply {
                this.size = imageSize
            }
        } else Result(CallbackStatus.LOADING, FileTypes.IMAGE)
        populateImagePerc(resultImage)

        val resultApk = if (apkSize != -1L) {
            Result(CallbackStatus.SUCCESS, FileTypes.APK).apply {
                this.size = apkSize
            }
        } else Result(CallbackStatus.LOADING, FileTypes.APK)
        populateApkPerc(resultApk)

        val resultAudio = if (audioSize != -1L) {
            Result(CallbackStatus.SUCCESS, FileTypes.AUDIO).apply {
                this.size = audioSize
            }
        } else Result(CallbackStatus.LOADING, FileTypes.AUDIO)
        populateAudioPerc(resultAudio)

        val resultDoc = if (docSize != -1L) {
            Result(CallbackStatus.SUCCESS, FileTypes.DOCUMENT).apply {
                this.size = docSize
            }
        } else Result(CallbackStatus.LOADING, FileTypes.DOCUMENT)
        populateDocsPerc(resultDoc)

        val resultSystem = if (systemSize != -1L) {
            Result(CallbackStatus.SUCCESS, FileTypes.SYSTEM).apply {
                this.size = systemSize
            }
        } else Result(CallbackStatus.LOADING, FileTypes.SYSTEM)
        populateSystemPerc(resultSystem)

        val resultOther = if (otherSize != -1L) {
            Result(CallbackStatus.SUCCESS, FileTypes.OTHER).apply {
                this.size = otherSize
            }
        } else Result(CallbackStatus.LOADING, FileTypes.OTHER)
        populateOtherPerc(resultOther)
        populateAvailablePerc()

        statsList = mutableListOf(
            StatsModel(
                getString(R.string.videos),
                ContextCompat.getColor(ctx, R.color.color_videos),
                resultVideo
            ),
            StatsModel(
                getString(R.string.images),
                ContextCompat.getColor(ctx, R.color.color_images),
                resultImage
            ), StatsModel(
                getString(R.string.apps),
                ContextCompat.getColor(ctx, R.color.color_apps),
                resultApk
            ),
            StatsModel(
                getString(R.string.audio),
                ContextCompat.getColor(
                    ctx,
                    R.color.color_audio_files
                ),
                resultAudio
            ), StatsModel(
                getString(R.string.documents),
                ContextCompat.getColor(ctx, R.color.color_documents),
                resultDoc
            ),
            StatsModel(
                getString(R.string.system),
                ContextCompat.getColor(
                    ctx,
                    R.color.color_systems
                ),
                resultSystem
            ), StatsModel(
                getString(R.string.other),
                ContextCompat.getColor(
                    ctx,
                    R.color.color_other
                ),
                resultOther
            )
        )
        statsAdapter.updateList(statsList)
    }

    override fun onResume() {
        super.onResume()

        val usedSpace = getTotalStorageSpace() - getFreeStorageSpace()
        val spacePercent = usedSpace * 100 / getTotalStorageSpace()

        binding.run {
            txtAvailableStoragePerc.text = "${DecimalFormat("##").format(spacePercent)}%"
            txtSpaceAvailable.text = String.format(
                getString(R.string._of_space_used),
                usedSpace.formatSize(),
                getTotalStorageSpace().formatSize()
            )
        }
    }

    override fun onBackBtnPressed() {
        if (adsUtils.isShown10SecsAgo()) {
            adsUtils.showInterstitialAd(
                R.string.interstitial_id,
                object : AdsUtils.OnAdClosedListener {
                    override fun onAdClosed() {
                        findNavController().navigateUp()
                    }
                }, true
            )
        } else
            findNavController().navigateUp()
    }

    override fun onDestroy() {
        storageVM.stopAnalyzing()
        adsUtils.destroyBanner()
        super.onDestroy()
    }
}