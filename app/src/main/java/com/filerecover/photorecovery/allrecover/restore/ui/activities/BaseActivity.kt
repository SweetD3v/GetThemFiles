package com.filerecover.photorecovery.allrecover.restore.ui.activities

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.filerecover.photorecovery.allrecover.restore.utils.KEY_SELECTED_LANGUAGE_CODE
import com.filerecover.photorecovery.allrecover.restore.utils.LocaleHelper
import com.filerecover.photorecovery.allrecover.restore.utils.PrefsManager
import com.filerecover.photorecovery.allrecover.restore.utils.isDarkModeEnabled
import com.filerecover.photorecovery.allrecover.restore.utils.isOreoPlus
import com.filerecover.photorecovery.allrecover.restore.utils.isPiePlus
import com.filerecover.photorecovery.allrecover.restore.utils.isRPlus

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {
    abstract val binding: VB
    private val prefsManager by lazy { PrefsManager.newInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        LocaleHelper.setLocale(this, prefsManager.getString(KEY_SELECTED_LANGUAGE_CODE, "en"))
        if (isPiePlus()) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        }

//        if (isTiramisuPlus()) {
//            onBackInvokedDispatcher.registerOnBackInvokedCallback(
//                OnBackInvokedDispatcher.PRIORITY_DEFAULT, object : OnBackInvokedCallback {
//
//                    /**
//                     * onBackPressed logic goes here. For instance:
//                     * Prevents closing the app to go home screen when in the
//                     * middle of entering data to a form
//                     * or from accidentally leaving a fragment with a WebView in it
//                     *
//                     * Unregistering the callback to stop intercepting the back gesture:
//                     * When the user transitions to the topmost screen (activity, fragment)
//                     * in the BackStack, unregister the callback by using
//                     * OnBackInvokeDispatcher.unregisterOnBackInvokedCallback
//                     * (https://developer.android.com/reference/kotlin/android/window/OnBackInvokedDispatcher#unregisteronbackinvokedcallback)
//                     */
//                    override fun onBackInvoked() {
//                        onBackBtnPressed()
//                        onBackInvokedDispatcher.unregisterOnBackInvokedCallback(this)
//                    }
//                })
//        } else {
        onBackPressedDispatcher.addCallback(this) {
            onBackBtnPressed()
//                this.isEnabled = false
        }
//        }
    }

    abstract fun onBackBtnPressed()
}

var inJunk = false
var inSplash = false

@Suppress("DEPRECATION")
fun Activity.hideSystemUI() {
    if (isPiePlus()) {
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
//    if (isRPlus()) {
//        window.insetsController?.let {
//            // Default behavior is that if navigation bar is hidden, the system will "steal" touches
//            // and show it again upon user's touch. We just want the user to be able to show the
//            // navigation bar by swipe, touches are handled by custom code -> change system bar behavior.
//            // Alternative to deprecated SYSTEM_UI_FLAG_IMMERSIVE.
//            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//            // make navigation bar translucent (alternative to deprecated
//            // WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
//            // - do this already in hideSystemUI() so that the bar
//            // is translucent if user swipes it up
////                window.navigationBarColor = getColor(R.color.transperent)
//            // Finally, hide the system bars, alternative to View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//            // and SYSTEM_UI_FLAG_FULLSCREEN.
//            it.hide(WindowInsets.Type.systemBars())
//            if (!isDarkModeEnabled()) {
//                it.setSystemBarsAppearance(
//                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
//                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
//                )
//            }
//        }
//    } else {
//        setFullScreen()
//        setSystemBarsColors()
//    }

    setFullScreen()
    setSystemBarsColors()

    window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
        if (visibility == View.VISIBLE) {
            Handler(Looper.getMainLooper()).postDelayed({
                hideSystemUI()
            }, 1500)
        }
    }
}

fun Activity.setFullScreen() {
    window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
}

fun Activity.setSystemBarsColors() {
    if (isOreoPlus()) {
        val flags = window.decorView.systemUiVisibility
        if (inJunk) {
            window.clearFlags(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            if (!isDarkModeEnabled())
                window.decorView.systemUiVisibility = (flags
                        or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
            else window.clearFlags(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
        } else if (inSplash) {
            window.clearFlags(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            if (isOreoPlus())
                window.clearFlags(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
        } else if (isDarkModeEnabled()) {
            window.clearFlags(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            window.clearFlags(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
        } else {
            window.decorView.systemUiVisibility = (flags
                    or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        }
    }
}

@Suppress("DEPRECATION")
fun Activity.showSystemUI() {
    if (isPiePlus()) {
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
    if (isRPlus()) {
        // show app content in fullscreen, i. e. behind the bars when they are shown (alternative to
        // deprecated View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.setDecorFitsSystemWindows(false)
        // finally, show the system bars
        window.insetsController?.show(WindowInsets.Type.systemBars())
    } else {
        // Shows the system bars by removing all the flags
        // except for the ones that make the content appear under the system bars.
//        val flags = window.decorView.systemUiVisibility
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }
}