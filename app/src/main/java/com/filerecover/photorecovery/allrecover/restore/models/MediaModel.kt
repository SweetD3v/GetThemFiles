package com.filerecover.photorecovery.allrecover.restore.models

class MediaModel(var pathPhoto: String, var lastModified: Long, var sizePhoto: Long) {
    var isChecked = false

    fun isCheck(): Boolean {
        return isChecked
    }

    fun setCheck(check: Boolean) {
        isChecked = check
    }
}