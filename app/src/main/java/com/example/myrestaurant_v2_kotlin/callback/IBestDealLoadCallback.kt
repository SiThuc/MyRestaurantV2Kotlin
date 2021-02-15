package com.example.myrestaurant_v2_kotlin.callback

import com.example.myrestaurant_v2_kotlin.model.BestDealModel
import com.example.myrestaurant_v2_kotlin.model.PopularCategoryModel

interface IBestDealLoadCallback {
    fun onBestDealLoadSuccess(bestDealList: List<BestDealModel>)
    fun onBestDealLoadFailed(message: String)
}