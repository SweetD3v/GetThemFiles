package com.filerecover.photorecovery.allrecover.restore.expandables.data

abstract class AbstractExpandableDataProvider {
    abstract class BaseData {
        abstract val text: String
        abstract var isPinned: Boolean
    }

    abstract class GroupData : BaseData() {
        abstract val isSectionHeader: Boolean
        abstract val groupId: Long
    }

    abstract class GridData : BaseData() {
        abstract val gridId: Long
    }

    abstract val groupCount: Int
    abstract fun getGridCount(groupPosition: Int): Int
    abstract fun getGroupItem(groupPosition: Int): GroupData
    abstract fun getGridItem(groupPosition: Int, gridPosition: Int): GridData
}