package com.filerecover.photorecovery.allrecover.restore

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.filerecover.photorecovery.allrecover.restore.models.DARK_MODE
import com.filerecover.photorecovery.allrecover.restore.models.DARK_MODE.DARK
import com.filerecover.photorecovery.allrecover.restore.models.DARK_MODE.FOLLOW_SYSTEM
import com.filerecover.photorecovery.allrecover.restore.models.DARK_MODE.LIGHT
import com.filerecover.photorecovery.allrecover.restore.ui.activities.PermissionsActivity
import com.filerecover.photorecovery.allrecover.restore.ui.activities.PrivacyPolicyActivity
import com.filerecover.photorecovery.allrecover.restore.ui.activities.SplashActivity
import com.filerecover.photorecovery.allrecover.restore.utils.PrefsManager
import com.filerecover.photorecovery.allrecover.restore.utils.Utils.SHOW_OPEN_ADS
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.onesignal.OneSignal
import java.util.Date


class RecoveryApp : MultiDexApplication(), ActivityLifecycleCallbacks, LifecycleObserver {
    private lateinit var appOpenAdManager: AppOpenAdManager
    private var currentActivity: Activity? = null
    private var loadTime: Long = 0

    private val prefs by lazy { PrefsManager.newInstance(this) }
    private val darkMode: DARK_MODE
        get() = DARK_MODE.fromValue(prefs.getInt("darkMode", FOLLOW_SYSTEM.value()))

    companion object {
        var mInstance: RecoveryApp? = null
        var isShowingAd = false

        @Synchronized
        @JvmStatic
        fun getInstance(): RecoveryApp {
            return mInstance ?: RecoveryApp()
        }
    }

    override fun onCreate() {
        super.onCreate()

        when (darkMode) {
            FOLLOW_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        mInstance = this
        registerActivityLifecycleCallbacks(this)
        MobileAds.initialize(this) {}
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager()
        Firebase.analytics

        OneSignal.initWithContext(this, getString(R.string.onesignal_app_id))
    }

    /** Interface definition for a callback to be invoked when an app open ad is complete. */
    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    private inner class AppOpenAdManager {
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false

        /** Request an ad. */
        fun loadAd(context: Context) {
            // Do not load ad if there is an unused ad or one is already loading.
            if (isLoadingAd || isAdAvailable()) {
                return
            }

            isLoadingAd = true
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context, context.getString(R.string.app_open_id), request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAdLoadCallback() {

                    override fun onAdLoaded(ad: AppOpenAd) {
                        // Called when an app open ad has loaded.
                        Log.e("LOG_TAG", "Ad was loaded.")
                        appOpenAd = ad
                        isLoadingAd = false
                        loadTime = Date().time
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Called when an app open ad has failed to load.
                        Log.e("LOG_TAG", loadAdError.message)
                        isLoadingAd = false
                    }
                })
        }

        /** Shows the ad if one isn't already showing. */
        fun showAdIfAvailable(
            activity: Activity,
            onShowAdCompleteListener: OnShowAdCompleteListener
        ) {
            // If the app open ad is already showing, do not show the ad again.
            if (isShowingAd) {
                Log.e("LOG_TAG", "The app open ad is already showing.")
                return
            }

            // If the app open ad is not available yet, invoke the callback then load the ad.
            if (!isAdAvailable()) {
                Log.e("LOG_TAG", "The app open ad is not ready yet.")
                onShowAdCompleteListener.onShowAdComplete()
                loadAd(activity)
                return
            }

            if (activity is SplashActivity || activity is PermissionsActivity || activity is PrivacyPolicyActivity)
                return

            if (!SHOW_OPEN_ADS)
                return

            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Called when full screen content is dismissed.
                    // Set the reference to null so isAdAvailable() returns false.
                    Log.e("LOG_TAG", "Ad dismissed fullscreen content.")
                    appOpenAd = null
                    isShowingAd = false

                    onShowAdCompleteListener.onShowAdComplete()
                    loadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Called when fullscreen content failed to show.
                    // Set the reference to null so isAdAvailable() returns false.
                    Log.d("LOG_TAG Error", adError.message)
                    appOpenAd = null
                    isShowingAd = false

                    onShowAdCompleteListener.onShowAdComplete()
                    loadAd(activity)
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when fullscreen content is shown.
                    isShowingAd = true
                    Log.e("LOG_TAG", "Ad showed fullscreen content.")
                }
            }
            Handler(Looper.getMainLooper()).post {
                appOpenAd?.show(activity)
            }
        }

        /** Show the ad if one isn't already showing. */
        fun showAdIfAvailable(activity: Activity) {
            showAdIfAvailable(
                activity,
                object : OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                        // Empty because the user will go back to the activity that shows the ad.
                        SHOW_OPEN_ADS = true
                        isShowingAd = false
                    }
                })
        }

        /** Check if ad exists and can be shown. */
        private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
            val dateDifference: Long = Date().time - loadTime
            val numMilliSecondsPerHour: Long = 3600000
            return dateDifference < numMilliSecondsPerHour * numHours
        }

        private fun isAdAvailable(): Boolean {
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        // Updating the currentActivity only when an ad is not showing.
        if (!isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        // Show the ad (if available) when the app moves to foreground.
        currentActivity?.let {
            Log.e("LOG_TAG", "onMoveToForeground: ${it.javaClass.name}")
            appOpenAdManager.showAdIfAvailable(it)
        }
    }
}