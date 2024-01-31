package com.filerecover.photorecovery.allrecover.restore.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.filerecover.photorecovery.allrecover.restore.R
import com.filerecover.photorecovery.allrecover.restore.databinding.FragmentLanguageBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.ItemLanguagesBinding
import com.filerecover.photorecovery.allrecover.restore.interfaces.AdEventListener
import com.filerecover.photorecovery.allrecover.restore.ui.activities.MainActivity
import com.filerecover.photorecovery.allrecover.restore.ui.activities.PermissionsActivity
import com.filerecover.photorecovery.allrecover.restore.utils.AdsUtils
import com.filerecover.photorecovery.allrecover.restore.utils.Config
import com.filerecover.photorecovery.allrecover.restore.utils.KEY_IS_LANGUAGE_SET
import com.filerecover.photorecovery.allrecover.restore.utils.KEY_SELECTED_LANGUAGE_CODE
import com.filerecover.photorecovery.allrecover.restore.utils.LocaleHelper
import com.filerecover.photorecovery.allrecover.restore.utils.PrefsManager
import com.filerecover.photorecovery.allrecover.restore.utils.StorageHelper.hasStoragePermission
import com.filerecover.photorecovery.allrecover.restore.utils.StorageHelper.isAccessGranted
import com.filerecover.photorecovery.allrecover.restore.utils.dpToPx
import com.filerecover.photorecovery.allrecover.restore.utils.getIconForLang
import com.filerecover.photorecovery.allrecover.restore.utils.getLanguageFromCode
import com.filerecover.photorecovery.allrecover.restore.utils.gone
import com.filerecover.photorecovery.allrecover.restore.utils.isOnline
import com.filerecover.photorecovery.allrecover.restore.utils.isRPlus
import com.filerecover.photorecovery.allrecover.restore.utils.languageCodes
import com.filerecover.photorecovery.allrecover.restore.utils.nextActivity
import com.filerecover.photorecovery.allrecover.restore.utils.visible
import com.google.android.gms.ads.nativead.NativeAd

class LanguageFragment : BaseFragment<FragmentLanguageBinding>(R.layout.fragment_language) {
    val adsUtils by lazy { AdsUtils.newInstance(ctx) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLanguageBinding.inflate(inflater, container, false)
        Config.STATUS_BAR_HEIGHT.observe(viewLifecycleOwner) { top ->
            binding.root.findViewById<RelativeLayout>(R.id.clStatus)
                ?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    height = top
                }
        }
        return getPersistentView(inflater, container, savedInstanceState)
    }

    private val prefs by lazy { PrefsManager.newInstance(ctx) }
    private var selectedLangCode = "en"
    private var isFromSplash = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        isFromSplash = arguments?.getBoolean("isFromSplash", false) ?: false
        selectedLangCode = prefs.getString(KEY_SELECTED_LANGUAGE_CODE, "en") ?: "en"

        setupTitle()
        populateLanguages()
        binding.run {

            if (ctx.isOnline()) {
                adsUtils.loadNative(R.string.native_id, object :
                    AdEventListener {
                    override fun onAdLoaded(nativeAd: NativeAd?) {
                        adsUtils.populateUnifiedNativeAdView(nativeAdFrame, nativeAd!!)
                        shimmerFrame.root.stopShimmer()
                        shimmerFrame.root.gone()
                    }

                    override fun onAdClosed() {

                    }

                    override fun onLoadError(errorCode: String?) {

                    }
                })
            } else shimmerFrame.root.gone()

            btnBack.setOnClickListener {
                onBackBtnPressed()
            }

            btnDone.setOnClickListener {
                prefs.putBoolean(KEY_IS_LANGUAGE_SET, true)
                prefs.putString(KEY_SELECTED_LANGUAGE_CODE, selectedLangCode)
                LocaleHelper.setLocale(ctx, selectedLangCode)
                nextScreen()
            }
        }
    }

    private fun setupTitle() {
        binding.run {
            txtTitle.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginStart = if (isFromSplash) dpToPx(16) else 0
            }

            val lp = txtTitle.layoutParams as RelativeLayout.LayoutParams

            if (isFromSplash) {
                btnBack.gone()
                lp.addRule(RelativeLayout.ALIGN_PARENT_START, txtTitle.id)
            } else {
                lp.addRule(RelativeLayout.CENTER_HORIZONTAL, txtTitle.id)
                btnBack.visible()
            }
            txtTitle.layoutParams = lp
        }
    }

    private fun populateLanguages() {
        binding.run {
            rvLanguages.layoutManager = LinearLayoutManager(ctx).apply {
                orientation = LinearLayoutManager.VERTICAL
            }

            val languagesData = arrayListOf<LanguageData>()
            languageCodes.forEach { code ->
                val language = ctx.getLanguageFromCode(code)
                languagesData.add(
                    LanguageData(
                        language,
                        code,
                        code.getIconForLang
                    )
                )
            }

            rvLanguages.adapter = LanguagesAdapter(
                languagesData,
                languageCodes.indexOf(prefs.getString(KEY_SELECTED_LANGUAGE_CODE, "en"))
            ) { index ->
                selectedLangCode = languagesData[index].langCode
            }
        }
    }

    inner class LanguagesAdapter(
        private val languages: ArrayList<LanguageData>,
        private var selected: Int,
        private val onItemClick: (Int) -> Unit
    ) :
        RecyclerView.Adapter<LanguagesAdapter.VH>() {
        inner class VH(private val binding: ItemLanguagesBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(langData: LanguageData) {
                binding.run {
                    imgLanguageCode.setImageResource(langData.langIcon)
                    txtLanguage.text = langData.lang

                    chkLanguage.isSelected = selected == layoutPosition

                    root.setOnClickListener {
                        chkLanguage.isSelected = true
                        notifyItemChanged(selected)
                        selected = layoutPosition
                        onItemClick.invoke(layoutPosition)
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(
                ItemLanguagesBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun getItemCount(): Int {
            return languages.size
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(languages[holder.layoutPosition])
        }
    }

    data class LanguageData(var lang: String, var langCode: String, var langIcon: Int)

    private fun nextScreen() {
        if (isFromSplash) {
            if (isRPlus()) {
                if (Environment.isExternalStorageManager() && ctx.isAccessGranted()) {
                    requireActivity().nextActivity(MainActivity(), 1000)
                } else requireActivity().nextActivity(PermissionsActivity(), 1000)
            } else {
                if (ctx.hasStoragePermission() && ctx.isAccessGranted()) {
                    requireActivity().nextActivity(MainActivity(), 1000)
                } else requireActivity().nextActivity(PermissionsActivity(), 1000)
            }
        } else {
            applyChanges()
        }
    }

    private fun applyChanges() {
        LocaleHelper.setLocale(ctx, prefs.getString(KEY_SELECTED_LANGUAGE_CODE, "en"))
        Intent(requireActivity(), MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(this)
        }
    }

    override fun onBackBtnPressed() {
        if (isFromSplash)
            requireActivity().finish()
        else
            findNavController().navigateUp()
    }

    override fun onDestroy() {
        adsUtils.destroyNative()
        super.onDestroy()
    }
}