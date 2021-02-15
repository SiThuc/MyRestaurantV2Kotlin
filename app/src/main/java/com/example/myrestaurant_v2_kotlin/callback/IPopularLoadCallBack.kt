package com.example.myrestaurant_v2_kotlin.callback

import com.example.myrestaurant_v2_kotlin.model.PopularCategoryModel

interface IPopularLoadCallBack {
    fun onPopularLoadSuccess(popularCategoryList: List<PopularCategoryModel>)
    fun onPopularLoadFailed(message: String)
}