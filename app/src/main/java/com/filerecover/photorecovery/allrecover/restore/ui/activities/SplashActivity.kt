package com.filerecover.photorecovery.allrecover.restore.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.filerecover.photorecovery.allrecover.restore.R
import com.filerecover.photorecovery.allrecover.restore.databinding.ActivitySplashBinding
import com.filerecover.photorecovery.allrecover.restore.interfaces.AdEventListener
import com.filerecover.photorecovery.allrecover.restore.utils.AdsUtils
import com.filerecover.photorecovery.allrecover.restore.utils.AdsUtils.Companion.fromSplash
import com.filerecover.photorecovery.allrecover.restore.utils.KEY_IS_LANGUAGE_SET
import com.filerecover.photorecovery.allrecover.restore.utils.KEY_IS_POLICY_ACCEPTED
import com.filerecover.photorecovery.allrecover.restore.utils.PrefsManager
import com.filerecover.photorecovery.allrecover.restore.utils.StorageHelper.hasStoragePermission
import com.filerecover.photorecovery.allrecover.restore.utils.StorageHelper.isAccessGranted
import com.filerecover.photorecovery.allrecover.restore.utils.Utils
import com.filerecover.photorecovery.allrecover.restore.utils.adjustInsetsBothVisible
import com.filerecover.photorecovery.allrecover.restore.utils.gone
import com.filerecover.photorecovery.allrecover.restore.utils.hasPermission
import com.filerecover.photorecovery.allrecover.restore.utils.isOnline
import com.filerecover.photorecovery.allrecover.restore.utils.isRPlus
import com.filerecover.photorecovery.allrecover.restore.utils.isTiramisuPlus
import com.filerecover.photorecovery.allrecover.restore.utils.nextActivity
import com.filerecover.photorecovery.allrecover.restore.utils.visible
import com.google.android.gms.ads.nativead.NativeAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    override val binding by lazy {
        ActivitySplashBinding.inflate(
            layoutInflater
        )
    }
    private val prefs by lazy { PrefsManager.newInstance(this) }

    var progressVal = 0


    private var handlerP: Handler = Handler(Looper.getMainLooper())
    private var runnableP: Runnable = object : Runnable {
        override fun run() {
            progressVal += 10
            handlerP.postDelayed(this, 100)
            binding.run {
                progressBar.post {
                    progressBar.setProgressCompat(progressVal, true)

                    if (binding.progressBar.max == progressVal) {
                        continueExecution()
                    }
                }
            }
        }
    }

    private fun updateProgress() {
        binding.progressBar.visible()

        lifecycleScope.launch(Dispatchers.Main) {
            while (binding.progressBar.progress < 300) {
                delay(100)
                progressVal += 10
                binding.progressBar.post {
                    binding.progressBar.setProgressCompat(progressVal, true)
                }
            }
            continueExecution()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inSplash = true
        setContentView(binding.root)
        Utils.SHOW_OPEN_ADS = false
        onBackPressedDispatcher.addCallback(this) {
            onBackBtnPressed()
        }

        if (isTiramisuPlus() && !hasPermission(Manifest.permission.POST_NOTIFICATIONS))
            prefs.putBoolean("canAskNotification", true)
        setInsetMargins()
        setPrivacyText()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        )
            prefs.putBoolean("askNotification", true)

        splashAdsUtils = AdsUtils.newInstance(this)

        if (isOnline()) {
            Utils.fromSplash = true
            fromSplash = true

            splashAdsUtils?.loadInterstitialAd(R.string.interstitial_id)

            splashAdsUtils?.loadNative(R.string.native_id, object :
                AdEventListener {
                override fun onAdLoaded(nativeAd: NativeAd?) {
                    splashAdsUtils?.nativeAdHome = nativeAd
                }

                override fun onAdClosed() {

                }

                override fun onLoadError(errorCode: String?) {

                }
            })
        }

        binding.run {
            policyCheck.isSelected = true
            policyCheck.setOnClickListener {
                policyCheck.isSelected = !policyCheck.isSelected
            }

            btnStart.setOnClickListener {
                if (!policyCheck.isSelected) {
                    Toast.makeText(
                        this@SplashActivity,
                        getString(R.string.click_checkbox_toast),
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                prefs.putBoolean(KEY_IS_POLICY_ACCEPTED, true)
                continueExecution()
            }
        }

        if (!prefs.getBoolean(KEY_IS_POLICY_ACCEPTED, false)) {
            binding.imgLogo.gone()
            binding.splashAnim.gone()
            showPolicyScreen()
        } else {
            updateProgress()
        }
    }

    private fun setInsetMargins() {
        showSystemUI()
        binding.run {
            root.adjustInsetsBothVisible(this@SplashActivity, { top ->
                clGetStarted.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = top
                }
            }, { bottom ->
                progressBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = bottom
                }
                clGetStarted.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = bottom
                }
            })
        }
        Handler(Looper.getMainLooper()).postDelayed({
            hideSystemUI()
            inSplash = false
        }, 150)
    }

    private fun showPolicyScreen() {
        binding.run {
            imgLogo.animate().alpha(0.0f).apply {
                duration = 350
                interpolator = DecelerateInterpolator()
            }
                .withEndAction {
                    clGetStarted.animate().alpha(1.0f).apply {
                        duration = 500
                        interpolator = AccelerateInterpolator()
                    }.start()
                }
                .start()
        }
    }

    private fun continueExecution() {
        if (!prefs.getBoolean(KEY_IS_LANGUAGE_SET, false)) {
            nextActivity(LanguageActivity(), 100)
        } else {
            if (isRPlus()) {
                if (Environment.isExternalStorageManager() && isAccessGranted()) {
                    nextActivity(MainActivity(), 100)
                } else nextActivity(PermissionsActivity(), 100)
            } else {
                if (hasStoragePermission() && isAccessGranted()) {
                    nextActivity(MainActivity(), 100)
                } else nextActivity(PermissionsActivity(), 100)
            }
        }
    }

    private fun setPrivacyText() {
        val s1 = getString(R.string.bottom_text1) + "\u0020"
        val s2 = getString(R.string.bottom_text2)
        val s3 = "\u0020" + getString(R.string.bottom_text3) + "\u0020"
        val s4 = getString(R.string.bottom_text4)
        val s5 = "\u0020" + getString(R.string.bottom_text5)
        val spannableString = SpannableString(s1 + s2 + s3 + s4 + s5)
        val clickableSpan1: ClickableSpan = object : ClickableSpan() {
            override fun updateDrawState(ds: TextPaint) {
                ds.linkColor = Color.WHITE
                super.updateDrawState(ds)
            }

            override fun onClick(widget: View) {
                val intent = Intent(this@SplashActivity, PrivacyPolicyActivity::class.java)
                intent.putExtra("isPrivacyPolicy", true)
                startActivity(intent)
            }
        }
        val clickableSpan2: ClickableSpan = object : ClickableSpan() {
            override fun updateDrawState(ds: TextPaint) {
                ds.linkColor = Color.WHITE
                super.updateDrawState(ds)
            }

            override fun onClick(widget: View) {
                val intent = Intent(this@SplashActivity, PrivacyPolicyActivity::class.java)
                intent.putExtra("isPrivacyPolicy", false)
                startActivity(intent)
            }
        }
        spannableString.setSpan(
            clickableSpan1, s1.length, s1.length + s2.length, Spanned.SPAN_POINT_MARK
        )
        spannableString.setSpan(
            clickableSpan2,
            s1.length + s2.length + s3.length,
            s1.length + s2.length + s3.length + s4.length,
            Spanned.SPAN_POINT_MARK
        )
        binding.txtPrivacy.text = spannableString
        binding.txtPrivacy.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onBackBtnPressed() {
        finish()
    }

    companion object {
        var splashAdsUtils: AdsUtils? = null
        const val SPLASH_DELAY = 3000L
    }
}