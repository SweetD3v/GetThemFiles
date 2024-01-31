package com.filerecover.photorecovery.allrecover.restore.models

enum class DARK_MODE(var position: Int) {
    FOLLOW_SYSTEM(0),
    LIGHT(1),
    DARK(2);

    fun value(): Int {
        return position
    }

    companion object {
        fun fromValue(value: Int): DARK_MODE {
            val list = values()
            for (pos in list) {
                if (pos.value() == value) {
                    return pos
                }
            }
            return FOLLOW_SYSTEM
        }
    }
}