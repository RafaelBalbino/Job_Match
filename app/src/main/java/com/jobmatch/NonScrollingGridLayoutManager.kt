// NonScrollingGridLayoutManager.kt

package com.jobmatch

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager

class NonScrollingGridLayoutManager(
    context: Context,
    spanCount: Int
) : GridLayoutManager(context, spanCount) {

    // Sobrescreve o método canScrollVertically para sempre retornar false.
    // Isso garante que o RecyclerView não tente rolar, forçando a altura total
    // a ser calculada pelo NestedScrollView pai.
    override fun canScrollVertically(): Boolean {
        return false
    }
}