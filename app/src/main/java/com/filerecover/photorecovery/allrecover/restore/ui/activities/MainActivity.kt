package com.filerecover.photorecovery.allrecover.restore.ui.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.allViews
import com.filerecover.photorecovery.allrecover.restore.R
import com.filerecover.photorecovery.allrecover.restore.databinding.ActivityMainBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.DialogDeleteBinding
import com.filerecover.photorecovery.allrecover.restore.notification_helper.NotiReceiver
import com.filerecover.photorecovery.allrecover.restore.utils.Config
import com.filerecover.photorecovery.allrecover.restore.utils.adjustInsetsBothVisible
import java.util.Calendar


class MainActivity : BaseActivity<ActivityMainBinding>() {
    override val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val notiReceiver = NotiReceiver()
        val notiTime = Calendar.getInstance().apply {
            this.set(Calendar.HOUR_OF_DAY, 21)
            this.set(Calendar.MINUTE, 0)
            this.set(Calendar.SECOND, 0)
            this.set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        notiReceiver.setupNotification(this, notiTime)
        setInsetMargins()
    }

    private fun setInsetMargins() {
        showSystemUI()
        binding.root.adjustInsetsBothVisible(this, { top ->
            Config._STATUS_BAR_HEIGHT.postValue(top)
        }, { bottom ->
        })
        Handler(Looper.getMainLooper()).postDelayed({
            setFullScreen()
        }, 150)
    }

    private fun setFullScreen() {
        setStatusBarColor()
        binding.root.setBackgroundColor(
            ContextCompat.getColor(
                this@MainActivity,
                R.color.bg_default
            )
        )
    }

    fun setStatusBarColor(fromJunk: Boolean = false) {
        inJunk = fromJunk
        hideSystemUI()
    }

    private var exitDialog: AlertDialog? = null

    private fun showExitDialog() {
        val dialogDeleteBinding = DialogDeleteBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(this, R.style.RoundedCornersDialogTransp)
            .setCancelable(false)
            .setView(dialogDeleteBinding.root)
        exitDialog = builder.create()
        exitDialog?.show()

        binding.root.allViews.forEach {
            it.isEnabled = false
        }

        var exited = false
        exitDialog?.setOnDismissListener {
            if (exited)
                finish()
            else binding.root.allViews.forEach {
                it.isEnabled = true
            }
        }

        dialogDeleteBinding.run {
            txtTitle.text = getString(R.string.exit_app)
            txtSubTitle.text = getString(R.string.are_you_sure_want_to_exit)

            btnDelete.text = getString(R.string.exit)

            btnDelete.setOnClickListener {
                exited = true
                exitDialog?.dismiss()
            }

            btnCancel.setOnClickListener {
                exitDialog?.dismiss()
            }
        }
    }

    override fun onBackBtnPressed() {
        showExitDialog()
    }
}

