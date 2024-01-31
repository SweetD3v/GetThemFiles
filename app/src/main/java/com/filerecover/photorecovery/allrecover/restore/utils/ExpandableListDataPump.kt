package com.filerecover.photorecovery.allrecover.restore.utils

import android.content.Context
import com.google.common.io.ByteStreams
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


object ExpandableListDataPump {
    fun Context.getAllPics(): HashMap<String, MutableList<String>> {
        val list = assets.list("pics")?.toMutableList() ?: mutableListOf()
        val pics = mutableListOf<String>()

        val data: HashMap<String, MutableList<String>> = HashMap()
        list.forEach {
            val file = assets.open("pics/$it").createFileFromInputStream(
                File(
                    cacheDir,
                    "IMG_${System.currentTimeMillis()}${it.substring(it.lastIndexOf("."))}"
                ).path
            )
            pics.add(file?.path.toString())
        }
        val lists = pics.chunked(5)
        lists.forEachIndexed { index, picsList ->
            data["Groups_${index + 1}"] = picsList.toMutableList()
        }

        return data
    }

    fun Context.getAllPicsArr(): MutableList<ByteArray?> {
        val list = assets.list("pics")?.toMutableList() ?: mutableListOf()
        val pics = mutableListOf<ByteArray?>()
        list.forEach {
            val byteArr = assets.open("pics/$it").createByteArrayInputStream()
            pics.add(byteArr)
        }

        return pics
    }

    private fun InputStream.createByteArrayInputStream(): ByteArray? {
        return ByteStreams.toByteArray(this)
    }

    private fun InputStream.createFileFromInputStream(path: String): File? {
        try {
            val file = File(path)
            val outputStream: OutputStream = FileOutputStream(file)
            val buffer = ByteArray(1024)
            var length = 0
            while (read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.close()
            close()
            return file
        } catch (e: IOException) {
            //Logging exception
        }
        return null
    }

    val data: HashMap<String, List<String>>
        get() {
            val expandableListDetail = HashMap<String, List<String>>()
            val cricket: MutableList<String> = ArrayList()
            cricket.add("India")
            cricket.add("Pakistan")
            cricket.add("Australia")
            cricket.add("England")
            cricket.add("South Africa")
            val football: MutableList<String> = ArrayList()
            football.add("Brazil")
            football.add("Spain")
            football.add("Germany")
            football.add("Netherlands")
            football.add("Italy")
            val basketball: MutableList<String> = ArrayList()
            basketball.add("United States")
            basketball.add("Spain")
            basketball.add("Argentina")
            basketball.add("France")
            basketball.add("Russia")
            expandableListDetail["CRICKET TEAMS"] = cricket
            expandableListDetail["FOOTBALL TEAMS"] = football
            expandableListDetail["BASKETBALL TEAMS"] = basketball
            return expandableListDetail
        }
}