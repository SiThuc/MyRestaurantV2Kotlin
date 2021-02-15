package com.example.myrestaurant_v2_kotlin.model

data class FoodModel(
    var id: String? = null,
    var name: String? = null,
    var image: String? = null,
    var price: Long = 0,
    var description: String? = null,
    var addon: List<AddonModel> = ArrayList<AddonModel>(),
    var size: List<SizeModel> = ArrayList<SizeModel>(),
    var key: String? = null,

    var ratingValue: Double = 0.0,
    var ratingCount: Long = 0L,
    var averageRating: Double = 0.0,

    var userSelectedAddon: MutableList<AddonModel>? = null,
    var userSelectedSize: SizeModel? = null
)
