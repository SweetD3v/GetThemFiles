package com.filerecover.photorecovery.allrecover.restore.interfaces

interface MediaSelectionListener {
    fun onMediaClick(path: String?, groupPosition: Int, childPosition: Int)
    fun onMediaChecked(
        path: String?,
        groupPosition: Int,
        childPosition: Int,
        checked: Boolean
    )
    fun onAllItemsChecked(
        items: MutableList<String>,
        groupPosition: Int,
        checked: Boolean
    )

    fun onMediaLongPressed(
        path: String?,
        groupPosition: Int,
        childPosition: Int,
        checked: Boolean
    )
}