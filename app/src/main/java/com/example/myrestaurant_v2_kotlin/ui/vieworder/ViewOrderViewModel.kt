package com.example.myrestaurant_v2_kotlin.ui.vieworder

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myrestaurant_v2_kotlin.model.OrderModel

class ViewOrderViewModel : ViewModel() {

    val orderLiveDataMutableList: MutableLiveData<List<OrderModel>>
    init {
        orderLiveDataMutableList = MutableLiveData()
    }

    fun setOrderLiveDataMutableList(orderList: List<OrderModel>){
        orderLiveDataMutableList.value = orderList
    }
}