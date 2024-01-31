package com.filerecover.photorecovery.allrecover.restore.utils

import android.app.Activity
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.database.Cursor
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.os.storage.StorageVolume
import android.provider.MediaStore
import android.text.format.DateUtils
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.EditText
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.filerecover.photorecovery.allrecover.restore.R
import com.filerecover.photorecovery.allrecover.restore.models.DARK_MODE
import com.filerecover.photorecovery.allrecover.restore.models.DARK_MODE.DARK
import com.filerecover.photorecovery.allrecover.restore.models.DARK_MODE.FOLLOW_SYSTEM
import com.filerecover.photorecovery.allrecover.restore.models.DARK_MODE.LIGHT
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt


fun View.gone() {
    this.visibility = View.GONE
}

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.beVisibleIf(predicate: Boolean) {
    if (predicate)
        visible()
    else gone()
}

fun <T : View> Array<T>.allGone() {
    forEach {
        it.gone()
    }
}

fun <T : View> Array<T>.allVisible() {
    forEach {
        it.visible()
    }
}

fun <T : View> Array<T>.allInVisible() {
    forEach {
        it.invisible()
    }
}

fun Long.isYesterday(): Boolean {
    val d = Date(this)
    return DateUtils.isToday(d.time + DateUtils.DAY_IN_MILLIS)
}

fun Long.isToday(): Boolean {
    val d = Date(this)
    return DateUtils.isToday(d.time)
}

fun EditText.toggleKeyboard(show: Boolean = true) {
    if (show) {
        this.requestFocus()
        ViewCompat.getWindowInsetsController(this)
            ?.show(WindowInsetsCompat.Type.ime())
    } else {
        this.clearFocus()
        ViewCompat.getWindowInsetsController(this)
            ?.hide(WindowInsetsCompat.Type.ime())
    }
}

fun <K, V> HashMap<K, V>.reverseMap() = this.toSortedMap(Collections.reverseOrder()).toMap()

fun Long.formatDuration(forceShowHours: Boolean = false): String {
    val sb = StringBuilder(8)
    val hours = this / 3600
    val minutes = this % 3600 / 60
    val seconds = this % 60

    if (this >= 3600) {
        sb.append(String.format(Locale.getDefault(), "%02d", hours)).append(":")
    } else if (forceShowHours) {
        sb.append("0:")
    }

    sb.append(String.format(Locale.getDefault(), "%02d", minutes))
    sb.append(":").append(String.format(Locale.getDefault(), "%02d", seconds))
    return sb.toString()
}

fun Long.formatDurationMinSec(forceShowHours: Boolean = false): String {
    val sb = StringBuilder(8)
    val hours = this / 3600
    val minutes = this % 3600 / 60
    val seconds = this % 60

    if (this >= 3600) {
        sb.append(String.format(Locale.getDefault(), if (hours > 9) "%02d" else "%01d", hours))
            .append(" hours")
    } else if (forceShowHours) {
        sb.append("0").append(" hours")
    }

    sb.append(String.format(Locale.getDefault(), if (minutes > 9) "%02d" else "%01d", minutes))
        .append(" mins ")
        .append(String.format(Locale.getDefault(), if (seconds > 9) "%02d" else "%01d", seconds))
        .append(" secs")
    return sb.toString()
}

fun Long.formatSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.##").format(
        this / 1024.0.pow(digitGroups.toDouble())
    ).replace("٠", "0")
        .replace("١", "1").replace("٢", "2").replace("٣", "3")
        .replace("٤", "4").replace("٥", "5").replace("٦", "6")
        .replace("٧", "7").replace("٨", "8").replace("٩", "9")
        .replace("٫", ".") + " " + units[digitGroups]
}

fun Long.formatSizeCeil(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.##").format(
        ceil(this / 1024.0.pow(digitGroups.toDouble()))
    ).replace("٠", "0")
        .replace("١", "1").replace("٢", "2").replace("٣", "3")
        .replace("٤", "4").replace("٥", "5").replace("٦", "6")
        .replace("٧", "7").replace("٨", "8").replace("٩", "9")
        .replace("٫", ".") + " " + units[digitGroups]
}

fun Float.formatFloat(): Float {
    return "%.2f".format(this).replace("٠", "0")
        .replace("١", "1").replace("٢", "2").replace("٣", "3")
        .replace("٤", "4").replace("٥", "5").replace("٦", "6")
        .replace("٧", "7").replace("٨", "8").replace("٩", "9")
        .replace("٫", ".").toFloat()
}

fun Long.formatSizeLong(): Double {
    if (this <= 0) return 0.0
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.##").format(
        this / 1024.0.pow(digitGroups.toDouble())
    ).replace("٠", "0")
        .replace("١", "1").replace("٢", "2").replace("٣", "3")
        .replace("٤", "4").replace("٥", "5").replace("٦", "6")
        .replace("٧", "7").replace("٨", "8").replace("٩", "9")
        .replace("٫", ".").toDouble()
}

fun Long.formatSizeOnly(): String {
    if (this <= 0) return "0"
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.##").format(
        this / 1024.0.pow(digitGroups.toDouble())
    ).replace("٠", "0")
        .replace("١", "1").replace("٢", "2").replace("٣", "3")
        .replace("٤", "4").replace("٥", "5").replace("٦", "6")
        .replace("٧", "7").replace("٨", "8").replace("٩", "9")
        .replace("٫", ".")
}

fun Long.formatSizeUnit(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    return units[digitGroups]
}

fun Context.getVersionName(): String {
    val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
    return pInfo.versionName
}

fun Context.getLanguageFromCode(code: String): String {
    when (code) {
        "en" -> {
            return getString(R.string.english)
        }

        "es" -> {
            return getString(R.string.spanish)
        }

        "hi" -> {
            return getString(R.string.hindi)
        }

        "fr" -> {
            return getString(R.string.french)
        }

        "zh" -> {
            return getString(R.string.chinese)
        }

        "ar" -> {
            return getString(R.string.arabic)
        }

        "ru" -> {
            return getString(R.string.russian)
        }

        "pt" -> {
            return getString(R.string.portuguese)
        }

        "it" -> {
            return getString(R.string.italian)
        }

        "bn" -> {
            return getString(R.string.bengali)
        }

        "de" -> {
            return getString(R.string.german)
        }

        "ja" -> {
            return getString(R.string.japanese)
        }

        "ur" -> {
            return getString(R.string.urdu)
        }

        "fa" -> {
            return getString(R.string.persian)
        }

        "ms" -> {
            return getString(R.string.malaysian)
        }

        else -> return getString(R.string.english)
    }
}

val String.getIconForLang: Int
    get() = when (this) {
        "en" -> {
            R.drawable.ic_usa_flag
        }

        "es" -> {
            R.drawable.ic_spanish_flag
        }

        "hi" -> {
            R.drawable.ic_india_flag
        }

        "fr" -> {
            R.drawable.ic_frans_flag
        }

        "zh" -> {
            R.drawable.ic_china_flag
        }

        "ar" -> {
            R.drawable.ic_arabic_flag
        }

        "ru" -> {
            R.drawable.ic_russia_flag
        }

        "pt" -> {
            R.drawable.ic_portuguese_flag
        }

        "it" -> {
            R.drawable.ic_italy_flag
        }

        "bn" -> {
            R.drawable.ic_bangladesh_flag
        }

        "de" -> {
            R.drawable.ic_germany_flag
        }

        "ja" -> {
            R.drawable.ic_japan_flag
        }

        "ur" -> {
            R.drawable.ic_pakistan_flag
        }

        "fa" -> {
            R.drawable.ic_iran_flag
        }

        "ms" -> {
            R.drawable.ic_malaysia_flag
        }

        else -> R.drawable.ic_usa_flag
    }

fun Long.convertToDateStr(format: String? = "hh:mm a"): String =
    SimpleDateFormat(format, Locale.ENGLISH).format(Date(this))

fun String.convertToDate(format: String? = "dd MM yyyy") =
    SimpleDateFormat(format, Locale.ENGLISH).parse(this) ?: Date(System.currentTimeMillis())

fun <A : Activity> A.nextActivity(
    activity: Activity,
    timeInMilliS: Long,
    finishIt: Boolean = true
) {
    val handler = Handler(Looper.getMainLooper())
    handler.postDelayed({
        if (!this.isDestroyed) {
            startActivity(Intent(this, activity::class.java))
            if (finishIt)
                finish()
        }
    }, timeInMilliS)
}

fun setLightStatusBar(activity: AppCompatActivity, colorId: Int) {
    var flags = activity.window.decorView.systemUiVisibility
    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    activity.window.decorView.systemUiVisibility = flags
    activity.window.statusBarColor = ResourcesCompat.getColor(
        activity.resources, colorId, null
    )
}

fun Activity.setLightStatusBar() {
    if (isMarshmallowPlus()) {
        var flags = window.decorView.systemUiVisibility
        flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (isOreoPlus()) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        window.decorView.systemUiVisibility = flags
    }
}

fun setDarkStatusBar(activity: AppCompatActivity) {
    activity.window.statusBarColor = ResourcesCompat.getColor(
        activity.resources, R.color.black, null
    )
}

fun AppCompatActivity.setDarkStatusBar(color: Int? = null) {
    window?.clearFlags(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
    window.statusBarColor = color?.let {
        ResourcesCompat.getColor(
            resources, it, null
        )
    } ?: Color.TRANSPARENT
}

fun Context.isDarkModeEnabled(): Boolean {
    val resources = resources
    val configuration = resources.configuration
    val currentUiMode = configuration.uiMode
    val darkMode = DARK_MODE.fromValue(
        PrefsManager.newInstance(this).getInt("darkMode", FOLLOW_SYSTEM.value())
    )
    return when (darkMode) {
        FOLLOW_SYSTEM -> currentUiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        else -> darkMode != LIGHT
    }
}

fun DARK_MODE.setDarkMode() {
    when (this) {
        FOLLOW_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}

fun dpToPx(dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), Resources.getSystem().displayMetrics
    ).roundToInt()
}

fun pxToDp(px: Int): Int {
    return (px / Resources.getSystem().displayMetrics.density).roundToInt()
}

fun View.adjustInsets(activity: Activity) {
    ViewCompat.setOnApplyWindowInsetsListener(
        activity.window.decorView
    ) { _, insets ->
        val statusbarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        Log.e("TAG", "adjustInsets: ${statusbarHeight}")
        (this.layoutParams as ViewGroup.MarginLayoutParams).topMargin = statusbarHeight
        insets
    }
}

fun View.adjustInsetsBothVisible(
    activity: Activity,
    topMargin: (Int) -> Unit,
    bottomMargin: (Int) -> Unit
) {
    ViewCompat.setOnApplyWindowInsetsListener(
        activity.window.decorView
    ) { _, insets ->
        val statusbarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        val navbarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
//        Log.e("TAG", "adjustInsetsTop: ${statusbarHeight}")
//        Log.e("TAG", "adjustInsetsBottom: ${navbarHeight}")
//        Log.e("TAG", "insetsVisible: ${insets.isVisible(WindowInsetsCompat.Type.systemBars())}")
        if (statusbarHeight > 0) {
            topMargin(statusbarHeight)
        }
        if (navbarHeight > 0) {
            bottomMargin(navbarHeight)
        }
        setOnApplyWindowInsetsListener(null)
        WindowInsetsCompat.CONSUMED
    }
}

fun getFreeStorageSpace(): Long {
    val root = Environment.getExternalStorageDirectory()
    val statFs = StatFs(root.path)
    val availBlocks: Long = statFs.availableBlocksLong
    val blockSize: Long = statFs.blockSizeLong
    return availBlocks * blockSize
}

fun getUsedStorageSpace(): Long {
    val root = Environment.getExternalStorageDirectory()
    val statFs = StatFs(root.path)
    val usedBlocks: Long = statFs.availableBlocksLong
    val blockSize: Long = statFs.blockSizeLong
    return getTotalStorageSpace() - (usedBlocks * blockSize)
}

fun getVolumeDirectory(volume: StorageVolume?): File? {
    return try {
        val f = StorageVolume::class.java.getDeclaredField("mPath")
        f.isAccessible = true
        f[volume] as File
    } catch (e: Exception) {
        // This shouldn't fail, as mPath has been there in every version
        throw RuntimeException(e)
    }
}

fun getTotalStorageSpace(): Long {
    val root = Environment.getExternalStorageDirectory()
    val statFs = StatFs(root.path)
    return statFs.totalBytes
}

fun getMimeType(file: File): String? {
    var mimeType: String? = ""
    val extension = getExtension(file.name)
    if (MimeTypeMap.getSingleton().hasExtension(extension)) {
        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
    return mimeType
}

private fun getExtension(fileName: String): String {
    val arrayOfFilename = fileName.toCharArray()
    for (i in arrayOfFilename.size - 1 downTo 1) {
        if (arrayOfFilename[i] == '.') {
            return fileName.substring(i + 1, fileName.length)
        }
    }
    return ""
}

fun File.shareFile(ctx: Context) {
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", this)
    Intent(Intent.ACTION_SEND).apply {
        type = ctx.contentResolver.getType(uri)
        ctx.grantUriPermission("android", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_STREAM, uri)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(this, ctx.getString(R.string.share_using)))
    }
}

fun File.openFile(ctx: Context) {
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", this)
    Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, ctx.contentResolver.getType(uri))
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(this, ctx.getString(R.string.open_with)))
    }
}

fun Context.getRealPathFromURI(path: String?): Uri? {
    var cursor: Cursor? = null
    return try {
        val proj = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME
        )
        cursor = contentResolver.query(
            MediaStore.Images.Media.getContentUri("external"),
            proj,
            "${MediaStore.Images.Media.DATA} LIKE ?",
            arrayOf(path.toString()),
            null
        )
//        val colIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getInt(it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
            } else null
        }
    } finally {
        cursor?.close()
    }
}

fun Context.getImageContentUri(imageFile: File): Uri? {
    val filePath = imageFile.absolutePath
    val cursor = contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media._ID),
        MediaStore.Images.Media.DATA + "=? ", arrayOf(filePath), null
    )
    return if (cursor != null && cursor.moveToFirst()) {
        val id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
        cursor.close()
        Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id)
    } else {
        if (imageFile.exists()) {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DATA, filePath)
            contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            )
        } else {
            null
        }
    }
}

fun Context.getImageContentUri10(imageFile: File): Uri? {
    val filePath = imageFile.absolutePath
    val cursor = contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media._ID),
        MediaStore.Images.Media.DATA + "=? ", arrayOf(filePath), null
    )
    return if (cursor != null && cursor.moveToFirst()) {
        val id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
        cursor.close()
        Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id)
    } else {
        if (imageFile.exists()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val picCollection = MediaStore.Images.Media
                    .getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val picDetail = ContentValues()
                picDetail.put(MediaStore.Images.Media.DISPLAY_NAME, imageFile.name)
                picDetail.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
                picDetail.put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "DCIM/" + UUID.randomUUID().toString()
                )
                picDetail.put(MediaStore.Images.Media.IS_PENDING, 1)
                val finaluri = resolver.insert(picCollection, picDetail)
                picDetail.clear()
                picDetail.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(picCollection, picDetail, null, null)
                finaluri
            } else {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.DATA, filePath)
                contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                )
            }
        } else {
            null
        }
    }
}

fun Context.shareMultipleFiles(
    filesList: ArrayList<File>
) {
    if (filesList.isEmpty())
        return
    val uriList = ArrayList(filesList.map {
        FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            it
        )
    })
    uriList.forEach { uri ->
        grantUriPermission("android", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    var fileURI: Uri
    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = filesList.let {
            fileURI = uriList.firstOrNull() ?: Uri.parse("")
            contentResolver.getType(fileURI)
        }
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
    }
    startActivity(
        Intent.createChooser(
            shareIntent,
            getString(R.string.share_using)
        )
    )
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.M)
fun isMarshmallowPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
fun isNougatPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N_MR1)
fun isNougatMR1Plus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
fun isOreoPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O_MR1)
fun isOreoMr1Plus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
fun isPiePlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
fun isQPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
fun isRPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun isSPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
fun isTiramisuPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
fun isUpsideDownCakePlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

fun getFolderName(absolutePath: String?): String {
    if (!absolutePath.isNullOrEmpty()) {
        if (absolutePath.contains(File.separator)) {
            val path = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator))
            val file = File(path)

            return file.name
        }
    }
    return ""
}

fun Context.isAppInstalled(packageName: String): Boolean {
    val pm = packageManager

    try {
        pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
        return true
    } catch (e: Exception) {
    }
    return false
}

fun Context.isOnline(): Boolean {
    val cm = getSystemService(Service.CONNECTIVITY_SERVICE) as ConnectivityManager
    val net = cm.activeNetwork ?: return false
    val actNw = cm.getNetworkCapabilities(net) ?: return false
    return when {
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        //for other device how are able to connect with Ethernet
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        //for check internet over Bluetooth
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
        else -> false
    }
}

private val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
private val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"
val EXTRA_SYSTEM_ALERT_WINDOW = "system_alert_window"

fun Intent.highlightSettingsTo(string: String): Intent {
    putExtra(EXTRA_FRAGMENT_ARG_KEY, string)
    val bundle = bundleOf(EXTRA_FRAGMENT_ARG_KEY to string)
    putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle)
    return this
}

fun Context.hasPermission(permission: String) =
    ContextCompat.checkSelfPermission(
        this,
        permission
    ) == PackageManager.PERMISSION_GRANTED