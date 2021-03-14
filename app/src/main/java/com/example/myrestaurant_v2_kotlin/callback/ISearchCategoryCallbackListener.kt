package com.example.myrestaurant_v2_kotlin.callback

import com.example.myrestaurant_v2_kotlin.database.CartItem
import com.example.myrestaurant_v2_kotlin.model.CategoryModel

interface ISearchCategoryCallbackListener {
    fun onSearchFound(category: CategoryModel, cartItem: CartItem)
    fun onSearchNotFound(message: String)
}