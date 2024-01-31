package com.filerecover.photorecovery.allrecover.restore.models

data class RecoveryMedia(
    var path: String = "",
    var dateModified: Long = 0L,
    var size: Long = 0L,
    var appIcon: Int? = null,
    var itemType: Int = 0,
    var group: MutableList<GroupMedia> = mutableListOf()
)

data class GroupMedia(var path: String = "")