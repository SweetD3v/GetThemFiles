package com.filerecover.photorecovery.allrecover.restore.models

data class Result(val status: CallbackStatus, val fileTypes: FileTypes, var size: Long = 0L) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Result) return false

        if (status != other.status) return false
        if (fileTypes != other.fileTypes) return false
        return size == other.size
    }

    override fun hashCode(): Int {
        var result = status.hashCode()
        result = 31 * result + fileTypes.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }
}