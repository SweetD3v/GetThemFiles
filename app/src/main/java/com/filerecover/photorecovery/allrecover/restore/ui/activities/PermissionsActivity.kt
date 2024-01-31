package com.filerecover.photorecovery.allrecover.restore.ui.activities

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.filerecover.photorecovery.allrecover.restore.R
import com.filerecover.photorecovery.allrecover.restore.databinding.ActivityPermissionsBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.ItemPermissionVpBinding
import com.filerecover.photorecovery.allrecover.restore.interfaces.AdBannerEventListener
import com.filerecover.photorecovery.allrecover.restore.utils.AdsUtils
import com.filerecover.photorecovery.allrecover.restore.utils.Config
import com.filerecover.photorecovery.allrecover.restore.utils.EXTRA_SYSTEM_ALERT_WINDOW
import com.filerecover.photorecovery.allrecover.restore.utils.PrefsManager
import com.filerecover.photorecovery.allrecover.restore.utils.StorageHelper.isAccessGranted
import com.filerecover.photorecovery.allrecover.restore.utils.adjustInsetsBothVisible
import com.filerecover.photorecovery.allrecover.restore.utils.gone
import com.filerecover.photorecovery.allrecover.restore.utils.highlightSettingsTo
import com.filerecover.photorecovery.allrecover.restore.utils.isOnline
import com.filerecover.photorecovery.allrecover.restore.utils.isRPlus
import com.filerecover.photorecovery.allrecover.restore.utils.isTiramisuPlus
import com.google.android.gms.ads.AdView


class PermissionsActivity : BaseActivity<ActivityPermissionsBinding>() {
    override val binding: ActivityPermissionsBinding by lazy {
        ActivityPermissionsBinding.inflate(
            layoutInflater
        )
    }

    private val prefs by lazy { PrefsManager.newInstance(this) }
    private val adsUtils: AdsUtils by lazy { AdsUtils.newInstance(this) }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                if (isTiramisuPlus()) {
                    prefs.putBoolean("canAskNotification", false)
                }
            }
        }

    private val allFilesAccessLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results: Map<String, Boolean?> ->
            val granted = results.all { it.value == true }

            if (granted) {
                if (isAccessGranted()) {
                    val intent = Intent(this@PermissionsActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    applicationContext.startActivity(intent)
                } else {
                    binding.viewPager.currentItem = 1
                }
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
                    && shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ) {
                    requestStoragePermission()
                } else {
                    showPermissionDialog()
                }
            }
        }

    private var permissionDialog: AlertDialog? = null

    private fun showPermissionDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this, R.style.RoundedCornersDialog)
        builder.setTitle(R.string.permissions_required)
            .setCancelable(false)
            .setMessage(R.string.you_need_to_give_some_required_permissions_to_run_this_app_smoothly)
            .setPositiveButton(R.string.settings) { dialog, _ ->
                dialog.dismiss()
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts(
                    "package", packageName,
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
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setInsetMargins()
        setUpViewPager()

        askForNotificationPermission()

        binding.run {
            if (isOnline())
                if (AdsUtils.adViewPermission == null) {
                    adsUtils.loadBanner(
                        this@PermissionsActivity,
                        bannerContainer,
                        R.string.banner_id,
                        object : AdBannerEventListener {
                            override fun onAdLoaded(adView: AdView?) {
                                AdsUtils.adViewPermission = adView
                                shimmerFrameBanner.root.stopShimmer()
                                shimmerFrameBanner.root.gone()
                            }

                            override fun onAdClosed() {
                            }

                            override fun onLoadError(errorCode: String?) {
                                Log.e("TAG", "onLoadError: $errorCode")
                            }
                        })
                } else {
                    shimmerFrameBanner.root.stopShimmer()
                    shimmerFrameBanner.root.gone()
                    AdsUtils.adViewPermission?.apply {
                        if (parent != null) {
                            (parent as ViewGroup).removeView(this)
                            bannerContainer.addView(this)
                        }
                    }
                }
            else shimmerFrameBanner.root.gone()

            if (isRPlus() && Environment.isExternalStorageManager()) {
                viewPager.currentItem = 1
            } else viewPager.currentItem = 0

            btnAllowPerm.setOnClickListener {
                when (viewPager.currentItem) {
                    0 -> {
                        if (isRPlus())
                            manageFilesPermission()
                        else
                            requestStoragePermission()
                    }

                    1 -> usageAccessPermission()
                }
            }
        }
    }

    private fun setInsetMargins() {
        showSystemUI()
        binding.run {
            root.adjustInsetsBothVisible(this@PermissionsActivity, { top ->
                rlMain.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = top
                }
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

    private fun setUpViewPager() {
        binding.run {
            viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
            viewPager.adapter = PermissionScreensAdapter(
                arrayListOf(
                    if (isRPlus())
                        PermissionsModel(
                            getString(R.string.all_files_access),
                            getString(R.string.you_need_to_give_all_files_access),
                            R.drawable.ic_perm_all_files
                        ) else PermissionsModel(
                        getString(R.string.storage_permission),
                        getString(R.string.you_need_to_give_storage_access),
                        R.drawable.ic_perm_all_files
                    ),
                    PermissionsModel(
                        getString(R.string.analyze_storage),
                        getString(R.string.you_need_to_give_usage_access),
                        R.drawable.ic_perm_usage
                    )
                )
            )
            viewPager.isUserInputEnabled = false
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    when (position) {
                        0 -> {
                            llIndicator.children.first().backgroundTintList =
                                ColorStateList.valueOf(getColor(R.color.colorPrimary))
                            llIndicator.children.last().backgroundTintList =
                                ColorStateList.valueOf(getColor(R.color._cbd1d9))
                        }

                        1 -> {
                            llIndicator.children.first().backgroundTintList =
                                ColorStateList.valueOf(getColor(R.color._cbd1d9))
                            llIndicator.children.last().backgroundTintList =
                                ColorStateList.valueOf(getColor(R.color.colorPrimary))
                        }
                    }
                }
            })
        }
    }

    private fun manageFilesPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            "android:manage_external_storage",
            Process.myUid(), packageName
        )
        if (mode == AppOpsManager.MODE_ALLOWED) {
            return true
        }
        appOps.startWatchingMode("android:manage_external_storage",
            applicationContext.packageName,
            object : AppOpsManager.OnOpChangedListener {
                override fun onOpChanged(op: String, packageName: String) {
                    val modeStatus = appOps.checkOpNoThrow(
                        "android:manage_external_storage",
                        Process.myUid(), getPackageName()
                    )
                    if (modeStatus != AppOpsManager.MODE_ALLOWED) {
                        return
                    }
                    appOps.stopWatchingMode(this)
                    val intent = Intent(this@PermissionsActivity, PermissionsActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    applicationContext.startActivity(intent)
                }
            })
        if (isRPlus())
            requestManageFilesAccess()
        return false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestManageFilesAccess() {
        if (!Environment.isExternalStorageManager())
            allFilesAccessLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
    }

    private fun requestStoragePermission() {
        storagePermissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
    }

    private fun usageAccessPermission() {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), packageName
        )
        if (mode == AppOpsManager.MODE_ALLOWED) {
            val intent = Intent(this@PermissionsActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            applicationContext.startActivity(intent)
            return
        }
        appOps.startWatchingMode(AppOpsManager.OPSTR_GET_USAGE_STATS,
            applicationContext.packageName,
            object : AppOpsManager.OnOpChangedListener {
                override fun onOpChanged(op: String, packageName: String) {
                    val modeStatus = appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(), getPackageName()
                    )
                    if (modeStatus != AppOpsManager.MODE_ALLOWED) {
                        return
                    }
                    appOps.stopWatchingMode(this)
                    val intent = Intent(this@PermissionsActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    applicationContext.startActivity(intent)
                }
            })
        requestUsageAccess()
    }

    private fun requestUsageAccess() {
        val intent = Intent(
            Settings.ACTION_USAGE_ACCESS_SETTINGS,
            Uri.parse("package:$packageName")
        ).highlightSettingsTo(EXTRA_SYSTEM_ALERT_WINDOW)
        startActivity(intent)
    }

    class PermissionScreensAdapter(val list: ArrayList<PermissionsModel>) :
        RecyclerView.Adapter<PermissionScreensAdapter.VH>() {
        inner class VH(val binding: ItemPermissionVpBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(
                ItemPermissionVpBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val permissionModel = list[holder.bindingAdapterPosition]
            holder.binding.run {
                txtTitle.text = permissionModel.title
                txtSubTitle.text = permissionModel.subTitle
                imgPermission.setImageResource(permissionModel.image)
            }
        }

        override fun getItemCount() = list.size
    }

    data class PermissionsModel(var title: String, var subTitle: String, var image: Int)

    private fun askForNotificationPermission() {
        if (isTiramisuPlus() && prefs.getBoolean("canAskNotification", true)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onBackBtnPressed() {
        finish()
    }
}