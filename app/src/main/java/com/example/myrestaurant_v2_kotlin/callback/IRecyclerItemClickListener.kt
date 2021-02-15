package com.example.myrestaurant_v2_kotlin.callback

import android.view.View

interface IRecyclerItemClickListener {
    fun onItemClick(view: View, pos: Int)
}