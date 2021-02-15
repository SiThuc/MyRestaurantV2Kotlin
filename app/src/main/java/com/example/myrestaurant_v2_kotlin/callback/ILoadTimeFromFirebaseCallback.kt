package com.example.myrestaurant_v2_kotlin.callback

import com.example.myrestaurant_v2_kotlin.model.Order

interface ILoadTimeFromFirebaseCallback {
    fun onLoadTimeSuccess(order: Order, estimatedTimeMs: Long)
    fun onLoadTimeFailed(message: String)
}