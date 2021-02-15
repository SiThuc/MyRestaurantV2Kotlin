package com.example.myrestaurant_v2_kotlin.ui.vieworder

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myrestaurant_v2_kotlin.model.Order

class ViewOrderViewModel : ViewModel() {

    val orderLiveDataMutableList: MutableLiveData<List<Order>>
    init {
        orderLiveDataMutableList = MutableLiveData()
    }

    fun setOrderLiveDataMutableList(orderList: List<Order>){
        orderLiveDataMutableList.value = orderList
    }
}