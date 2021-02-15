package com.example.myrestaurant_v2_kotlin.callback

import com.example.myrestaurant_v2_kotlin.model.Order

interface ILoadOrderCallbackListener {

    fun onLoadOrdersSuccess(orderList: List<Order>)
    fun onLoadOrdersFailed(message: String)
}