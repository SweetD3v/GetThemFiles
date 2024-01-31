package com.filerecover.photorecovery.allrecover.restore.models

import android.graphics.drawable.Drawable

data class JunkMedia(
    var name: String = "",
    var path: String = "",
    var dateModified: Long = 0L,
    var size: Long = 0L,
    var isHeader: Boolean = false,
    var appIcon: Drawable? = null,
    var junkType: String = "",
    var itemType: Int = 0
)