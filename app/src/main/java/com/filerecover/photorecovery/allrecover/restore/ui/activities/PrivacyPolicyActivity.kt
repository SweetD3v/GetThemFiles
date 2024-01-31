package com.filerecover.photorecovery.allrecover.restore.ui.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.view.updateLayoutParams
import com.filerecover.photorecovery.allrecover.restore.R
import com.filerecover.photorecovery.allrecover.restore.databinding.ActivityPrivacyPolicyBinding
import com.filerecover.photorecovery.allrecover.restore.utils.Config
import com.filerecover.photorecovery.allrecover.restore.utils.adjustInsetsBothVisible

class PrivacyPolicyActivity : BaseActivity<ActivityPrivacyPolicyBinding>() {
    override val binding by lazy {
        ActivityPrivacyPolicyBinding.inflate(
            layoutInflater
        )
    }
    private var isPrivacyPolicy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setInsetMargins()

        isPrivacyPolicy = intent.getBooleanExtra("isPrivacyPolicy", false)

        binding.root.adjustInsetsBothVisible(this, { top ->
            binding.rlMain.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = top
            }
        }, { bottom ->
            binding.rlMain.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = bottom
            }
        })

        binding.run {
            if (isPrivacyPolicy) {
                txtTitle.text = getString(R.string.title_privacy_policy)
            } else {
                txtTitle.text = getString(R.string.title_terms_service)
            }

            btnBack.setOnClickListener {
                onBackBtnPressed()
            }
        }
        loadWebView()
    }

    private fun setInsetMargins() {
        showSystemUI()
        binding.run {
            root.adjustInsetsBothVisible(this@PrivacyPolicyActivity, { top ->
                Config._STATUS_BAR_HEIGHT.postValue(top)
            }, { bottom ->
                rlMain.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = bottom
                }
            })
        }
        Handler(Looper.getMainLooper()).postDelayed({
            hideSystemUI()
        }, 150)
    }

    private fun loadWebView() {
        try {
            binding.run {
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                webView.settings.javaScriptEnabled = true
                webView.webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        progress.progress = newProgress
                        if (newProgress == 100) progress.visibility = View.GONE
                    }
                }
                if (isPrivacyPolicy) {
                    webView.loadUrl(resources.getString(R.string.privacy_policy_link))
                } else {
                    webView.loadUrl(resources.getString(R.string.terms_service_link))
                }
            }
        } catch (e: Exception) {
            Log.e("error", "loadWebView: " + e.message)
        }
    }

    override fun onBackBtnPressed() {
        finish()
    }
}