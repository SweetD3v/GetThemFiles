package com.filerecover.photorecovery.allrecover.restore.utils

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.filerecover.photorecovery.allrecover.restore.R
import com.filerecover.photorecovery.allrecover.restore.RecoveryApp
import com.filerecover.photorecovery.allrecover.restore.interfaces.AdBannerEventListener
import com.filerecover.photorecovery.allrecover.restore.interfaces.AdEventListener
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoController.VideoLifecycleCallbacks
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import java.util.concurrent.TimeUnit

class AdsUtils private constructor(val ctx: Context) {
    private var adView: AdView? = null

    var nativeAd: NativeAd? = null
    var nativeAdHome: NativeAd? = null
    var nativeAdView: NativeAdView? = null
    var interstitialAd: InterstitialAd? = null
    var isAdLoad = false
    var isAdLoadProcessing = false
    var isAdLoadFailed = false

    fun isShown10SecsAgo() =
        lastShownTime == 0L || TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastShownTime) >= 10

    fun loadInterstitialAd(@StringRes interstitialAdID: Int) {
        if (interstitialAd == null && !isAdLoadProcessing) {
            isAdLoadProcessing = true
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(ctx,
                ctx.getString(interstitialAdID),
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAds: InterstitialAd) {
                        isAdLoad = true
                        isAdLoadFailed = false
                        isAdLoadProcessing = false
                        interstitialAd = interstitialAds
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.e("TAG", "onAdFailedToLoad: " + loadAdError.message)
                        isAdLoad = false
                        isAdLoadFailed = true
                        isAdLoadProcessing = false
                    }
                })
        }
    }

    fun showInterstitialAd(
        @StringRes interstitialAdID: Int,
        onAdClosedListener: OnAdClosedListener?,
        fromJunk: Boolean = false
    ) {
        if (!RecoveryApp.isShowingAd) {
            if (isAdLoad && !isAdLoadFailed && !isAdLoadProcessing) {
                interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        super.onAdFailedToShowFullScreenContent(adError)
                        interstitialAd = null
                        isAdLoad = false
                        isAdLoadProcessing = false
                        isAdLoadFailed = false
                        Utils.SHOW_OPEN_ADS = true
                        RecoveryApp.isShowingAd = false
                        onAdClosedListener?.onAdClosed()
                    }

                    override fun onAdShowedFullScreenContent() {
                        super.onAdShowedFullScreenContent()
                        Utils.SHOW_OPEN_ADS = false
                        RecoveryApp.isShowingAd = true
                        if (fromJunk)
                            lastShownTime = System.currentTimeMillis()
                    }

                    override fun onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent()
                        Utils.SHOW_OPEN_ADS = true
                        RecoveryApp.isShowingAd = false
                        isAdLoad = false
                        isAdLoadProcessing = false
                        isAdLoadFailed = false
                        interstitialAd = null
                        loadInterstitialAd(interstitialAdID)
                        onAdClosedListener?.onAdClosed()
                    }
                }
                RecoveryApp.isShowingAd = true
                interstitialAd?.show((ctx as Activity))
            } else {
                onAdClosedListener?.onAdClosed()
            }
        } else {
            onAdClosedListener?.onAdClosed()
        }
    }

    fun loadNative(@StringRes adId: Int, adEventListener: AdEventListener?) {
        val builder = AdLoader.Builder(ctx, ctx.getString(adId))
        builder.forNativeAd { unifiedNativeAd: NativeAd ->
            if (adEventListener != null) {
                nativeAd = unifiedNativeAd
                adEventListener.onAdLoaded(unifiedNativeAd)
            }
        }.withAdListener(object : AdListener() {

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                run {
                    adEventListener?.onLoadError(loadAdError.message)
                }
            }

        })
        val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
        val adOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
        builder.withNativeAdOptions(adOptions)
        val adLoader = builder.build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun populateUnifiedNativeAdView(
        nativeAdFrame: FrameLayout? = null,
        unifiedNativeAd: NativeAd,
        isHome: Boolean = false,
        ctx: Context = this.ctx
    ) {
        val nativeAdView =
            (ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
                if (isHome) R.layout.admob_native_medium_home else R.layout.admob_native_medium,
                null
            ) as NativeAdView
        nativeAdFrame?.removeAllViews()
        nativeAdFrame?.addView(nativeAdView)

        nativeAdView.headlineView = nativeAdView.findViewById(R.id.adTitle)
        nativeAdView.bodyView = nativeAdView.findViewById(R.id.ad_body)
        nativeAdView.iconView = nativeAdView.findViewById(R.id.adIcon)

        val mediaView = nativeAdView.findViewById(R.id.mediaView) as MediaView?
        mediaView?.mediaContent = nativeAd?.mediaContent
        mediaView?.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View, child: View) {
                if (child is ImageView) {
                    child.adjustViewBounds = true
                    child.scaleType = ImageView.ScaleType.FIT_CENTER
                }
            }

            override fun onChildViewRemoved(parent: View, child: View) {}
        })
        nativeAdView.mediaView = mediaView

        nativeAdView.advertiserView = nativeAdView.findViewById(R.id.ad_advertiser)
        nativeAdView.callToActionView = nativeAdView.findViewById(R.id.callToAction)

        (nativeAdView.headlineView as AppCompatTextView).text = unifiedNativeAd.headline

        if (unifiedNativeAd.body == null) {
            (nativeAdView.bodyView as AppCompatTextView).invisible()
        } else {
            (nativeAdView.bodyView as AppCompatTextView).visible()
            (nativeAdView.bodyView as AppCompatTextView).text = unifiedNativeAd.body
        }

        if (unifiedNativeAd.callToAction == null) {
            (nativeAdView.callToActionView as AppCompatButton).invisible()
        } else {
            (nativeAdView.callToActionView as AppCompatButton).visible()
            (nativeAdView.callToActionView as AppCompatButton).text = unifiedNativeAd.callToAction
        }
        if (unifiedNativeAd.icon == null) {
            (nativeAdView.iconView as AppCompatImageView).gone()
        } else {
            (nativeAdView.iconView as AppCompatImageView).setImageDrawable(unifiedNativeAd.icon!!.drawable)
            (nativeAdView.iconView as AppCompatImageView).visible()
        }

        if (unifiedNativeAd.advertiser == null) {
            (nativeAdView.advertiserView as AppCompatTextView).gone()
        } else {
            (nativeAdView.advertiserView as AppCompatTextView).text = unifiedNativeAd.advertiser
            (nativeAdView.advertiserView as AppCompatTextView).visible()
        }
        val vc = unifiedNativeAd.mediaContent!!.videoController
        vc.mute(true)
        if (vc.hasVideoContent()) {
            vc.videoLifecycleCallbacks = object : VideoLifecycleCallbacks() {}
        }
        nativeAdView.setNativeAd(unifiedNativeAd)

        (nativeAdView.headlineView as TextView).text = unifiedNativeAd.headline
        (nativeAdView.bodyView as TextView).text = unifiedNativeAd.body
        (nativeAdView.callToActionView as TextView).text = unifiedNativeAd.callToAction
        val icon = unifiedNativeAd.icon
        if (icon == null) {
            nativeAdView.iconView?.invisible()
        } else {
            if (nativeAdView.iconView != null) {
                (nativeAdView.iconView as ImageView).setImageDrawable(icon.drawable)
                nativeAdView.iconView?.visible()
            }
        }
        if (unifiedNativeAd.advertiser == null) {
            nativeAdView.advertiserView?.invisible()
        } else {
            (nativeAdView.advertiserView as TextView).text = unifiedNativeAd.advertiser
            nativeAdView.advertiserView?.visible()
        }
        nativeAdView.setNativeAd(unifiedNativeAd)
    }

    fun destroyNative() {
        nativeAdView?.destroy()
        nativeAdView = null
        nativeAd?.destroy()
        nativeAd = null
    }

    fun destroyNativeHome() {
        nativeAdHome?.destroy()
        nativeAdHome = null
    }

    fun destroyBanner() {
        adView?.destroy()
    }

    fun loadBanner(
        activity: AppCompatActivity,
        bannerContainer: ViewGroup,
        @StringRes adId: Int,
        adEventListener: AdBannerEventListener? = null
    ) {
        adView = AdView(activity)
        adView?.adUnitId = activity.getString(adId)
        if (bannerContainer.childCount > 0) bannerContainer.removeAllViews()
        bannerContainer.addView(adView)
        val adRequest = AdRequest.Builder().build()
        val adSize = getAdSize(activity, bannerContainer)
        adView?.setAdSize(adSize)
        adView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                adEventListener?.onAdLoaded(adView)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                adEventListener?.onLoadError(error.message)
            }
        }
        adView?.loadAd(adRequest)
    }

    fun getAdSize(activity: AppCompatActivity, bannerContainer: ViewGroup): AdSize {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val density = outMetrics.density
        var adWidthPixels = bannerContainer.width.toFloat()
        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }
        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    companion object {
        @Synchronized
        fun newInstance(context: Context) = AdsUtils(context)

        var fromSplash: Boolean = false
        var lastShownTime = 0L
        var adViewPermission: AdView? = null
    }

    interface OnAdClosedListener {
        fun onAdClosed()
    }
}