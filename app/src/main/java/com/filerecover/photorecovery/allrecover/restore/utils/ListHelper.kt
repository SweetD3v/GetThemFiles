package com.filerecover.photorecovery.allrecover.restore.utils

fun <T> Collection<T>.containsAny(other: Collection<T>): Boolean {
    // Use HashSet instead of #toSet which uses a LinkedHashSet
    val set = if (this is Set) this else HashSet(this)
    for (item in other)
        if (set.contains(item)) // early return
            return true
    return false
}