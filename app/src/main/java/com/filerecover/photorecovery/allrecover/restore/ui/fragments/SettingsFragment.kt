package com.filerecover.photorecovery.allrecover.restore.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.findNavController
import com.filerecover.photorecovery.allrecover.restore.R
import com.filerecover.photorecovery.allrecover.restore.databinding.DialogDarkModeBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.FragmentSettingsBinding
import com.filerecover.photorecovery.allrecover.restore.models.DARK_MODE
import com.filerecover.photorecovery.allrecover.restore.models.DARK_MODE.DARK
import com.filerecover.photorecovery.allrecover.restore.models.DARK_MODE.FOLLOW_SYSTEM
import com.filerecover.photorecovery.allrecover.restore.models.DARK_MODE.LIGHT
import com.filerecover.photorecovery.allrecover.restore.ui.activities.PrivacyPolicyActivity
import com.filerecover.photorecovery.allrecover.restore.ui.activities.inJunk
import com.filerecover.photorecovery.allrecover.restore.ui.activities.inSplash
import com.filerecover.photorecovery.allrecover.restore.utils.Config
import com.filerecover.photorecovery.allrecover.restore.utils.KEY_SELECTED_LANGUAGE_CODE
import com.filerecover.photorecovery.allrecover.restore.utils.PrefsManager
import com.filerecover.photorecovery.allrecover.restore.utils.getLanguageFromCode
import com.filerecover.photorecovery.allrecover.restore.utils.getVersionName
import com.filerecover.photorecovery.allrecover.restore.utils.setDarkMode

class SettingsFragment : BaseFragment<FragmentSettingsBinding>(R.layout.fragment_settings) {

    private val prefs by lazy { PrefsManager.newInstance(ctx) }
    private val darkMode: DARK_MODE
        get() = DARK_MODE.fromValue(prefs.getInt("darkMode", FOLLOW_SYSTEM.value()))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        Config.STATUS_BAR_HEIGHT.observe(viewLifecycleOwner) { top ->
            binding.root.findViewById<RelativeLayout>(R.id.clStatus)
                ?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    height = top
                }
        }
        return getPersistentView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.run {
            txtVersion.text = String.format(getString(R.string.version_), ctx.getVersionName())

            btnBack.setOnClickListener {
                onBackBtnPressed()
            }

            llDarkMode.setOnClickListener {
                showDarkModeDialog()
            }

            llLanguage.setOnClickListener {
                navigateToLanguage()
            }

            llShareApp.setOnClickListener {
                shareApp()
            }

            llRateUs.setOnClickListener {
                openPlayStore()
            }

            llPrivacyPolicy.setOnClickListener {
                startActivity(
                    Intent(ctx, PrivacyPolicyActivity::class.java)
                        .putExtra("isPrivacyPolicy", true)
                )
            }

            llTermsOfUse.setOnClickListener {
                startActivity(
                    Intent(ctx, PrivacyPolicyActivity::class.java)
                        .putExtra("isPrivacyPolicy", false)
                )
            }
        }
    }

    private fun openPlayStore() {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(getString(R.string.playstore_url, ctx.packageName))
            )
        )
    }


    private var darkModeDialog: AlertDialog? = null

    private fun showDarkModeDialog() {
        val dialogDarkModeBinding = DialogDarkModeBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(ctx, R.style.RoundedCornersDialogTransp)
            .setCancelable(false)
            .setView(dialogDarkModeBinding.root)
        darkModeDialog = builder.create()
        darkModeDialog?.show()

        var modeSelected = false

        darkModeDialog?.setOnDismissListener {
            if (modeSelected) {
                dialogDarkModeBinding.run {
                    arrayOf(chkFollowSystem, chkLight, chkDark).indexOfFirst {
                        it.isSelected
                    }.let { index ->
                        prefs.putInt("darkMode", DARK_MODE.fromValue(index).value())
                    }
                }

                inJunk = false
                inSplash = false
                darkMode.setDarkMode()
            }
        }

        dialogDarkModeBinding.run {
            arrayOf(chkFollowSystem, chkLight, chkDark).forEachIndexed { index, chk ->
                chk.isSelected = index == darkMode.value()
            }

            btnCancel.setOnClickListener {
                darkModeDialog?.dismiss()
            }

            btnDone.setOnClickListener {
                modeSelected = true
                darkModeDialog?.dismiss()
            }

            llFollowSystem.setOnClickListener {
                chkFollowSystem.isSelected = true
                arrayOf(chkLight, chkDark).forEach { it.isSelected = false }
            }

            llLight.setOnClickListener {
                chkLight.isSelected = true
                arrayOf(chkFollowSystem, chkDark).forEach { it.isSelected = false }
            }

            llDark.setOnClickListener {
                llDark.isSelected = true
                arrayOf(chkFollowSystem, chkLight).forEach { it.isSelected = false }
            }
        }
    }

    private fun shareApp() {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(
            Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=${ctx.packageName}"
        )
        sendIntent.type = "text/plain"
        startActivity(sendIntent)
    }

    override fun onResume() {
        super.onResume()

        val langStr =
            ctx.getLanguageFromCode(prefs.getString(KEY_SELECTED_LANGUAGE_CODE, "en") ?: "en")
        binding.run {
            txtSelectedLang.text = langStr.replaceFirstChar { ch -> ch.uppercase() }
            txtDarkMode.text = when (darkMode) {
                FOLLOW_SYSTEM -> getString(R.string.system)
                LIGHT -> getString(R.string.light)
                DARK -> getString(R.string.dark)
            }
        }
    }

    private fun navigateToLanguage() {
        findNavController().navigate(R.id.action_settings_to_language)
    }

    override fun onBackBtnPressed() {
        findNavController().navigateUp()
    }
}