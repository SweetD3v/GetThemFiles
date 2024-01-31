package com.filerecover.photorecovery.allrecover.restore.ui.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import com.filerecover.photorecovery.allrecover.restore.databinding.ActivityLanguageBinding
import com.filerecover.photorecovery.allrecover.restore.utils.Config
import com.filerecover.photorecovery.allrecover.restore.utils.adjustInsetsBothVisible

class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {
    override val binding: ActivityLanguageBinding by lazy {
        ActivityLanguageBinding.inflate(
            layoutInflater
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setInsetMargins()
    }

    private fun setInsetMargins() {
        showSystemUI()
        binding.root.adjustInsetsBothVisible(this, { top ->
            Config._STATUS_BAR_HEIGHT.postValue(top)
        }, { bottom ->
//            binding.fragmentContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
//                bottomMargin = bottom
//            }
        })
        Handler(Looper.getMainLooper()).postDelayed({
            hideSystemUI()
        }, 150)
    }


    override fun onBackBtnPressed() {
        finish()
    }
}