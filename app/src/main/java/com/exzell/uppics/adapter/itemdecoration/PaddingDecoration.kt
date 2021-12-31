package com.exzell.uppics.adapter.itemdecoration

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PaddingDecoration(private val offset: Int): RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val currentPosition = parent.getChildAdapterPosition(view)
        val numItems = parent.adapter!!.itemCount

        if(numItems-1 == currentPosition) outRect.set(0, 0, 0, offset)
    }
}