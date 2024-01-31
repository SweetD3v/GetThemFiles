package com.filerecover.photorecovery.allrecover.restore.utils

import com.filerecover.photorecovery.allrecover.restore.models.GroupMedia
import com.filerecover.photorecovery.allrecover.restore.models.JunkMedia
import com.filerecover.photorecovery.allrecover.restore.models.RecoveryMedia
import java.io.File

object ImageHelper {
    fun getGroupedMedia(
        list: MutableList<RecoveryMedia>,
        recoveryType: Int = RECOVERY_TYPE_IMAGES
    ): MutableList<RecoveryMedia> {
        val mediaList: MutableList<RecoveryMedia> = mutableListOf()
        val grouped = list.sortedByDescending { it.dateModified }
            .groupBy { File(it.path).parent }

        grouped.forEach { imageList ->
            val groupedList = imageList.value

            mediaList.add(
                RecoveryMedia(
                    imageList.key ?: "Other",
                    itemType = ITEM_TYPE_HEADER
                )
            )

            if (recoveryType == RECOVERY_TYPE_IMAGES || recoveryType == RECOVERY_TYPE_VIDEOS) {
                if (groupedList.size > (MEDIA_GRID_SIZE - 1)) {
                    mediaList.addAll(groupedList.subList(0, (MEDIA_GRID_SIZE - 1)))
                    mediaList.add(
                        RecoveryMedia(
                            group = groupedList.subList((MEDIA_GRID_SIZE - 1), groupedList.size)
                                .map {
                                    GroupMedia(it.path)
                                }.toMutableList(),
                            itemType = ITEM_TYPE_GROUP
                        )
                    )
                } else {
                    mediaList.addAll(groupedList)
                }
            } else {
                mediaList.addAll(groupedList)
            }
        }

        return mediaList
    }

    fun getGroupedJunks(
        list: MutableList<JunkMedia>
    ): MutableList<JunkMedia> {
        val junkList: MutableList<JunkMedia> = mutableListOf()
//        val grouped = list.sortedByDescending { it.dateModified }
//            .groupBy { File(it.path).parent }

//        grouped.forEach { imageList ->
//            val groupedList = imageList.value
//
//            junkList.add(
//                JunkMedia(
//                    imageList.key ?: "Other",
//                    itemType = ITEM_TYPE_HEADER
//                )
//            )
//
//            junkList.addAll(groupedList)
//        }

        junkList.add(
            JunkMedia(
                "Apks",
                itemType = ITEM_TYPE_HEADER
            )
        )

        junkList.addAll(list)
        return junkList
    }
}