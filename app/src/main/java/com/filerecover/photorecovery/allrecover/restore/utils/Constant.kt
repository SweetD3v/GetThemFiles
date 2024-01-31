package com.filerecover.photorecovery.allrecover.restore.utils

import com.filerecover.photorecovery.allrecover.restore.R

const val RECOVERY_TYPE = "recovery_type"
const val ARGS_MEDIA_LIST_DETAILS = "args_media_list_details"

const val RECOVERY_TYPE_IMAGES = 0
const val RECOVERY_TYPE_VIDEOS = 1
const val RECOVERY_TYPE_AUDIOS = 2
const val RECOVERY_TYPE_DOCS = 3
const val RECOVERY_TYPE_JUNK = 4

const val ITEM_TYPE_HEADER = 0
const val ITEM_TYPE_MEDIA = 1
const val ITEM_TYPE_GROUP = 2

const val MEDIA_GRID_SIZE = 4

val CELLS_IDS = intArrayOf(
    R.id.cell_1,
    R.id.cell_2,
    R.id.cell_3
)

val photoExtensions: Array<String> = arrayOf(
    ".jpg",
    ".png",
    ".jpeg",
    ".bmp",
    ".webp",
    ".heic",
    ".heif",
    ".apng",
    ".avif"
)
val videoExtensions: Array<String> = arrayOf(
    ".mp4",
    ".mkv",
    ".webm",
    ".avi",
    ".3gp",
    ".mov",
    ".m4v",
    ".3gpp"
)
val audioExtensions: Array<String> = arrayOf(
    ".mp3",
    ".wav",
    ".wma",
    ".ogg",
    ".m4a",
    ".opus",
    ".flac",
    ".aac",
    ".m4b"
)

fun getAudioIcon(extension: String): Int = if (audioExtensions.any { it == extension }) {
    when (extension) {
        audioExtensions[0] -> R.drawable.ic_mp3_ext
        audioExtensions[1] -> R.drawable.ic_wav_ext
        audioExtensions[3] -> R.drawable.ic_ogg_ext
        audioExtensions[4] -> R.drawable.ic_m4a_ext
        audioExtensions[7] -> R.drawable.ic_acc_ext
        else -> R.drawable.ic_other_ext
    }
} else R.drawable.ic_other_ext

val rawExtensions: Array<String> = arrayOf(
    ".dng",
    ".orf",
    ".nef",
    ".arw",
    ".rw2",
    ".cr2",
    ".cr3"
)

val documentExtensions: Array<String> = arrayOf(
    ".doc",
    ".docx",
    ".htm",
    ".html",
    ".odt",
    ".pdf",
    ".xls",
    ".xlsx",
    ".ods",
    ".ppt",
    ".pptx",
    ".txt",
    ".zip",
    ".rar",
    ".xml",
    ".apk"
)

fun getDocIcon(extension: String): Int = if (documentExtensions.any { it == extension }) {
    when (extension) {
        documentExtensions[0], documentExtensions[1] -> R.drawable.ic_doc_ext
        documentExtensions[2], documentExtensions[3] -> R.drawable.ic_html_ext
        documentExtensions[5] -> R.drawable.ic_pdf_ext
        documentExtensions[6], documentExtensions[7] -> R.drawable.ic_xls_ext
        documentExtensions[9], documentExtensions[10] -> R.drawable.ic_ppt_ext
        documentExtensions[11] -> R.drawable.ic_txt_ext
        documentExtensions[12] -> R.drawable.ic_zip_ext
        documentExtensions[13] -> R.drawable.ic_rar_ext
        documentExtensions[14] -> R.drawable.ic_xml_ext
        documentExtensions[15] -> R.drawable.ic_apk_ext
        else -> R.drawable.ic_other_ext
    }
} else R.drawable.ic_other_ext

val apkExtensions: Array<String> = arrayOf(
    ".apk",
)

val tempExtensions: Array<String> = arrayOf(
    ".tmp",
    ".cache"
)

const val KEY_IS_POLICY_ACCEPTED = "is_policy_accepted"
const val KEY_IS_LANGUAGE_SET = "is_language_set"
const val KEY_SELECTED_LANGUAGE_CODE = "selected_language_code"
val languageCodes = arrayListOf("en", "es", "hi", "fr", "zh", "ar", "ru", "pt", "it", "bn", "de", "ja", "ur", "fa", "ms")