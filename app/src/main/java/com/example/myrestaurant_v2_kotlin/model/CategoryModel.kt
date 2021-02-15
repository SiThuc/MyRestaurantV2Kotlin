package com.example.myrestaurant_v2_kotlin.model

data class CategoryModel(
    var menu_id: String? = null,
    var name: String? = null,
    var image: String? = null,
    var foods: List<FoodModel>? = null
)