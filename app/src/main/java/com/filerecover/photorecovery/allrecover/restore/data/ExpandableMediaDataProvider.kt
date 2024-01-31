package com.filerecover.photorecovery.allrecover.restore.expandables.data

import android.os.Build
import com.filerecover.photorecovery.allrecover.restore.utils.ListChopper
import com.filerecover.photorecovery.allrecover.restore.models.RecoveryMedia
import com.filerecover.photorecovery.allrecover.restore.utils.MEDIA_GRID_SIZE
import java.io.File

class ExpandableMediaDataProvider(
    var mData: MutableList<Pair<RecoveryMedia, MutableList<RecoveryMedia>>> = mutableListOf()
) : AbstractExpandableDataProvider() {
    private var concreteGroupList: MutableList<ConcreteGroupData> = mutableListOf()

    fun updateData(mData: MutableList<Pair<RecoveryMedia, MutableList<RecoveryMedia>>> = mutableListOf()) {
        this.mData = mData
        val groupItems = mData
        concreteGroupList = mutableListOf()
        for (i in groupItems.indices) {
            val groupId = i.toLong()
            var groupText = (File(groupItems[i].first.path).parentFile?.name ?: "0").toString()
            if (groupText == "0") {
                val brand = Build.MANUFACTURER
                val name = Build.DEVICE
                groupText = "${
                    brand.replaceFirst(
                        brand.first(),
                        brand.first().uppercaseChar()
                    )
                } ${
                    name.replaceFirst(
                        name.first(),
                        name.first().uppercaseChar()
                    )
                }"
            }
            val group = ConcreteGroupData(groupId, groupText, groupItems[i].second)
            // add group item to groups' list
            concreteGroupList.add(group)
        }
    }

    override val groupCount: Int
        get() = concreteGroupList.size

    override fun getGridCount(groupPosition: Int): Int {
        return concreteGroupList[groupPosition].concreteGridDataList.size
    }

    override fun getGroupItem(groupPosition: Int): GroupData {
        if (groupPosition < 0 || groupPosition >= groupCount) {
            throw IndexOutOfBoundsException("groupPosition = $groupPosition")
        }
        return concreteGroupList[groupPosition]
    }

    override fun getGridItem(groupPosition: Int, gridPosition: Int): GridData {
        if (groupPosition < 0 || groupPosition >= groupCount) {
            throw IndexOutOfBoundsException("groupPosition = $groupPosition")
        }
        if (gridPosition < 0 || gridPosition >= (getGroupItem(groupPosition) as ConcreteGroupData?)!!.concreteGridDataList.size) {
            throw IndexOutOfBoundsException("gridPosition = $gridPosition")
        }
        return (getGroupItem(groupPosition) as ConcreteGroupData?)!!.concreteGridDataList[gridPosition]
    }

    /**
     * A concrete group data. It splits its children data into small arrays.
     */
    inner class ConcreteGroupData(
        override val groupId: Long,
        override val text: String,
        recoveryMedia: MutableList<RecoveryMedia>
    ) : GroupData() {
        override var isPinned = false
        private val gridChildDataList: MutableList<ConcreteGridData> = mutableListOf()

        init {
            // each group manages its children data
            val childrenData: MutableList<RecoveryMedia> = mutableListOf()
            childrenData.addAll(recoveryMedia)
            // split whole children data into small arrays of data, each array must have at most the same size
            val childrenDataArrays =
                ListChopper.splitListBySize(childrenData, MEDIA_GRID_SIZE)

            // Then each grid holds an array of children data
            for (k in childrenDataArrays.indices) {
                val gridData = ConcreteGridData(k, childrenDataArrays[k])
                gridChildDataList.add(gridData)
            }
        }

        val concreteGridDataList: List<ConcreteGridData>
            get() = gridChildDataList
        override val isSectionHeader: Boolean
            get() = false
    }

    /**
     * An ARRAY of concrete child data
     */
    class ConcreteGridData internal constructor(
        private var mId: Int, // would lay more than one child data per row
        val gridDataArray: MutableList<RecoveryMedia>
    ) : GridData() {
        override var isPinned = false

        override val gridId: Long
            get() = mId.toLong()
        override val text: String
            get() = ""

        fun setGridId(id: Int) {
            mId = id
        }
    }
}