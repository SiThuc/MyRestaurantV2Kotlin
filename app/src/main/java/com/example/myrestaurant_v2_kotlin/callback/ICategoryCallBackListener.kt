package com.example.myrestaurant_v2_kotlin.callback

import com.example.myrestaurant_v2_kotlin.model.CategoryModel

interface ICategoryCallBackListener {
    fun onCategoryLoadSuccess(categoryList: List<CategoryModel>)
    fun onCategoryLoadFailed(message: String)
}