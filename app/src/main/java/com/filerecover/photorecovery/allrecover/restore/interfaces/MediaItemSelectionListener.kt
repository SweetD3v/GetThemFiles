package com.filerecover.photorecovery.allrecover.restore.interfaces

interface MediaItemSelectionListener {
    fun onMediaClick(position: Int, path: String)

    fun onMediaLongPressed(
        position: Int,
        path: String
    ): Boolean
}