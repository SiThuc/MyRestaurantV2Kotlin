package com.example.myrestaurant_v2_kotlin.callback

import com.example.myrestaurant_v2_kotlin.model.OrderModel

interface ILoadTimeFromFirebaseCallback {
    fun onLoadTimeSuccess(order: OrderModel, estimatedTimeMs: Long)
    fun onLoadTimeFailed(message: String)
}