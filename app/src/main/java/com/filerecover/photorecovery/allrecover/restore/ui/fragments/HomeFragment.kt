package com.filerecover.photorecovery.allrecover.restore.ui.fragments

import android.Manifest
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.findNavController
import com.filerecover.photorecovery.allrecover.restore.R
import com.filerecover.photorecovery.allrecover.restore.RecoveryApp
import com.filerecover.photorecovery.allrecover.restore.databinding.FragmentHomeBinding
import com.filerecover.photorecovery.allrecover.restore.interfaces.AdBannerEventListener
import com.filerecover.photorecovery.allrecover.restore.interfaces.AdEventListener
import com.filerecover.photorecovery.allrecover.restore.ui.activities.MainActivity
import com.filerecover.photorecovery.allrecover.restore.ui.activities.SplashActivity.Companion.splashAdsUtils
import com.filerecover.photorecovery.allrecover.restore.utils.AdsUtils
import com.filerecover.photorecovery.allrecover.restore.utils.Config
import com.filerecover.photorecovery.allrecover.restore.utils.PrefsManager
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE_AUDIOS
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE_DOCS
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE_IMAGES
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE_VIDEOS
import com.filerecover.photorecovery.allrecover.restore.utils.Utils
import com.filerecover.photorecovery.allrecover.restore.utils.formatSize
import com.filerecover.photorecovery.allrecover.restore.utils.getFreeStorageSpace
import com.filerecover.photorecovery.allrecover.restore.utils.getTotalStorageSpace
import com.filerecover.photorecovery.allrecover.restore.utils.gone
import com.filerecover.photorecovery.allrecover.restore.utils.hasPermission
import com.filerecover.photorecovery.allrecover.restore.utils.isOnline
import com.filerecover.photorecovery.allrecover.restore.utils.isTiramisuPlus
import com.filerecover.photorecovery.allrecover.restore.utils.visible
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.nativead.NativeAd
import java.text.DecimalFormat


class HomeFragment : BaseFragment<FragmentHomeBinding>(R.layout.fragment_home) {

    private val adsUtils: AdsUtils by lazy { splashAdsUtils ?: AdsUtils.newInstance(ctx) }
    private var adView: AdView? = null

    private val prefs: PrefsManager by lazy { PrefsManager.newInstance(ctx) }
    private var askCount = 0

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))
                    prefs.getBoolean("canAskNotification", false)
//                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
//                    askCount++
//                    askForNotificationPermission()
//                } else {
//                    askCount = 2
//                    showPermissionDialog()
//                }
            }
        }

    private fun checkAndAskNotificationPermission() {
        if (ctx.hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            askCount = 2
            return
        } else {
            if (askCount == 0)
                askForNotificationPermission()
            else if (askCount == 2)
                showPermissionDialog()
            return
        }
    }

    private fun askForNotificationPermission() {
        if (isTiramisuPlus()) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private var permissionDialog: AlertDialog? = null

    private fun showPermissionDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(ctx, R.style.RoundedCornersDialog)
        builder.setTitle(R.string.permissions_required)
            .setCancelable(false)
            .setMessage(R.string.you_need_to_give_some_required_permissions_to_run_this_app_smoothly)
            .setPositiveButton(R.string.settings) { dialog, _ ->
                dialog.dismiss()
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts(
                    "package", ctx.packageName,
                    null
                )
                intent.data = uri
                startActivity(intent)
            }
        permissionDialog ?: let {
            permissionDialog = builder.create()
        }

        if (permissionDialog?.isShowing == false) {
            permissionDialog?.show()
            permissionDialog?.getButton(AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor(ctx.getColor(R.color.colorPrimary))
        }
    }

    private var notificationDialog: AlertDialog? = null

    private fun showNotificationDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(ctx, R.style.RoundedCornersDialog)
        builder.setTitle(R.string.permissions_required)
            .setCancelable(false)
            .setMessage(R.string.you_need_to_give_notification_permission_to_run_this_app_smoothly)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                prefs.putBoolean("askNotification", false)
                dialog.dismiss()
                checkAndAskNotificationPermission()
            }
        notificationDialog ?: let {
            notificationDialog = builder.create()
        }
        if (notificationDialog?.isShowing == false) {
            notificationDialog?.show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        Config.STATUS_BAR_HEIGHT.observe(viewLifecycleOwner) { top ->
            binding.root.findViewById<RelativeLayout>(R.id.clStatus)
                ?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    height = top
                }
        }
        return getPersistentView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))
            prefs.putBoolean("canAskNotification", false)

        if (!RecoveryApp.isShowingAd
        ) {
            if (!prefs.getBoolean(
                    "canAskNotification",
                    true
                ) && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
                && !ctx.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
            )
                showPermissionDialog()
            else {
                if (prefs.getBoolean("canAskNotification", true))
                    askForNotificationPermission()
            }
        }

        if (adsUtils.isAdLoad && Utils.fromSplash) {
            Utils.fromSplash = false
            adsUtils.showInterstitialAd(
                R.string.interstitial_id, object : AdsUtils.OnAdClosedListener {
                    override fun onAdClosed() {
                        Utils.SHOW_OPEN_ADS = true
                    }
                })
        }

        binding.run {
            if (adView == null) {
                if (ctx.isOnline())
                    adsUtils.loadBanner(
                        requireActivity() as AppCompatActivity,
                        bannerContainer,
                        R.string.banner_id,
                        object : AdBannerEventListener {
                            override fun onAdLoaded(adView: AdView?) {
                                this@HomeFragment.adView = adView
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

            shimmerFrame.root.background = ColorDrawable(ctx.getColor(R.color.bg_admob_native_home))
            adsUtils.nativeAdHome?.let { nativeAdHome ->
                nativeAdFrame.visible()
                shimmerFrame.root.stopShimmer()
                shimmerFrame.root.gone()
                adsUtils.populateUnifiedNativeAdView(
                    nativeAdFrame,
                    nativeAdHome,
                    true,
                    ctx
                )
            } ?: let {
                if (ctx.isOnline())
                    adsUtils.loadNative(R.string.native_id, object :
                        AdEventListener {
                        override fun onAdLoaded(nativeAd: NativeAd?) {
                            nativeAd?.let {
                                adsUtils.nativeAdHome = it
                                adsUtils.populateUnifiedNativeAdView(nativeAdFrame, it, true, ctx)
                                nativeAdFrame.visible()
                                shimmerFrame.root.stopShimmer()
                                shimmerFrame.root.gone()
                            }
                        }

                        override fun onAdClosed() {
                        }

                        override fun onLoadError(errorCode: String?) {
                        }
                    })
                else {
                    shimmerFrame.root.gone()
                }
            }

            storageCard.setOnClickListener {
                navigateToStorageAnalyze()
            }

            llImageRecovery.setOnClickListener {
                navigateToImageRecovery()
            }

            llVideoRecovery.setOnClickListener {
                navigateToVideoRecovery()
            }

            llAudioRecovery.setOnClickListener {
                navigateToAudioRecovery()
            }

            llDocumentRecovery.setOnClickListener {
                navigateToDocsRecovery()
            }

            llRecycleBin.setOnClickListener {
                navigateToRecycleBin()
            }

            btnSettings.setOnClickListener {
                navigateToSettings()
            }
        }
    }

    private fun navigateToStorageAnalyze() {
        findNavController().navigate(R.id.action_home_to_storage_analyze)
    }

    private fun navigateToImageRecovery() {
        val bundle = Bundle()
        bundle.putInt(RECOVERY_TYPE, RECOVERY_TYPE_IMAGES)
        findNavController().navigate(R.id.action_home_to_image_recovery, bundle)
    }

    private fun navigateToVideoRecovery() {
        val bundle = Bundle()
        bundle.putInt(RECOVERY_TYPE, RECOVERY_TYPE_VIDEOS)
        findNavController().navigate(R.id.action_home_to_image_recovery, bundle)
    }

    private fun navigateToAudioRecovery() {
        val bundle = Bundle()
        bundle.putInt(RECOVERY_TYPE, RECOVERY_TYPE_AUDIOS)
        findNavController().navigate(R.id.action_home_to_doc_recovery, bundle)
    }

    private fun navigateToDocsRecovery() {
        val bundle = Bundle()
        bundle.putInt(RECOVERY_TYPE, RECOVERY_TYPE_DOCS)
        findNavController().navigate(R.id.action_home_to_doc_recovery, bundle)
    }

    private fun navigateToRecycleBin() {
        findNavController().navigate(R.id.action_home_to_doc_recovery)
    }

    private fun navigateToSettings() {
        findNavController().navigate(R.id.action_home_to_settings)
    }

    override fun onResume() {
        super.onResume()

        val usedSpace = getTotalStorageSpace() - getFreeStorageSpace()
        val spacePercent = usedSpace * 100 / getTotalStorageSpace()

        binding.run {
            progressStorage.setProgressWithAnimation(spacePercent.toFloat(), 500)
            txtPercents.text = "${DecimalFormat("##").format(spacePercent)}%"
            txtSpaceUsed.text = String.format(
                getString(R.string._of_space_used),
                usedSpace.formatSize(),
                getTotalStorageSpace().formatSize()
            )
        }
    }

    override fun onDestroy() {
        adsUtils.destroyNativeHome()
        super.onDestroy()
    }

    override fun onBackBtnPressed() {
        if (activity is MainActivity) {
            (activity as MainActivity).onBackBtnPressed()
        }
    }
}