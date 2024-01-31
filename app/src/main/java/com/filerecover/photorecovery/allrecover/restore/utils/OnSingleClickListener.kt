package com.filerecover.photorecovery.allrecover.restore.utils

import android.os.SystemClock
import android.view.View

class OnSingleClickListener(private val click: (View) -> Unit) : View.OnClickListener {
    private var lastClickTime = 0L

    override fun onClick(view: View) {
        if (SystemClock.elapsedRealtime() - lastClickTime < 700) {
            return
        }
        lastClickTime = SystemClock.elapsedRealtime()

        click(view)
    }
}

fun View.setOnSingleClickListener(block: (View) -> Unit) {
    setOnClickListener(OnSingleClickListener(block))
}