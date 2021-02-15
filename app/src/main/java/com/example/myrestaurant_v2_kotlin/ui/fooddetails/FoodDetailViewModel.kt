package com.example.myrestaurant_v2_kotlin.ui.fooddetails

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.model.CommentModel
import com.example.myrestaurant_v2_kotlin.model.FoodModel

class FoodDetailViewModel : ViewModel() {
    private var foodDetailData: MutableLiveData<FoodModel>?= null
    private var commentLiveData: MutableLiveData<CommentModel>? = null
    init {
        commentLiveData = MutableLiveData()
    }

    fun getFoodDetail(): MutableLiveData<FoodModel>{
        if(foodDetailData == null){
            foodDetailData = MutableLiveData()
        }

        foodDetailData!!.value = Common.foodSelected

        return foodDetailData!!
    }

    fun getComment(): MutableLiveData<CommentModel>{
        if(commentLiveData == null)
            commentLiveData = MutableLiveData()
        return commentLiveData!!
    }

    fun setCommentModel(commentModel: CommentModel) {
        if(commentLiveData != null)
            commentLiveData!!.value = commentModel
    }

    fun setFoodModel(foodModel: FoodModel) {
        if(foodDetailData != null)
            foodDetailData!!.value = foodModel
    }
}