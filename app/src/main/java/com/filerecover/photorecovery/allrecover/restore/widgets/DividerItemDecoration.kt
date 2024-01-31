package com.filerecover.photorecovery.allrecover.restore.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.filerecover.photorecovery.allrecover.restore.utils.dpToPx

class DividerItemDecoration(context: Context, resId: Int) : ItemDecoration() {
    private var divider: Drawable?
    private var marginStart = 0
    private var marginEnd = 0

    init {
        divider = ContextCompat.getDrawable(context, resId)

        marginStart = dpToPx(20)
        marginEnd = dpToPx(16)
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft + marginStart
        val right = parent.width - marginEnd
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + (divider?.intrinsicHeight ?: 0)
            divider?.setBounds(left, top, right, bottom)
            divider?.draw(c)
        }
    }
}