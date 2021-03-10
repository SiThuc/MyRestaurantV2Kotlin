package com.example.myrestaurant_v2_kotlin.callback

import com.example.myrestaurant_v2_kotlin.model.OrderModel

interface ILoadOrderCallbackListener {

    fun onLoadOrdersSuccess(orderList: List<OrderModel>)
    fun onLoadOrdersFailed(message: String)
}