package com.example.myrestaurant_v2_kotlin.ui.foodlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.model.FoodModel

class FoodListViewModel : ViewModel() {
    private var foodListLiveData: MutableLiveData<List<FoodModel>>?= null

    fun getFoodList():MutableLiveData<List<FoodModel>>{
        if(foodListLiveData == null)
            foodListLiveData = MutableLiveData()

        foodListLiveData!!.value = Common.categorySelected!!.foods

        return foodListLiveData!!
    }
}