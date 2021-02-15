package com.example.myrestaurant_v2_kotlin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.databinding.LayoutOrderItemBinding
import com.example.myrestaurant_v2_kotlin.model.Order
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

class MyOrderAdapter(var context: Context, var orderList: List<Order>):
    RecyclerView.Adapter<MyOrderAdapter.MyViewHolder>() {

    internal var calendar: Calendar
    internal var simpleDateFormat: SimpleDateFormat

    lateinit var layoutBinding: LayoutOrderItemBinding

    init {
        calendar = Calendar.getInstance()
        simpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
    }

    inner class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        layoutBinding = LayoutOrderItemBinding.inflate(LayoutInflater.from(context))

        return MyViewHolder(layoutBinding.root)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        with(holder){
            val order = orderList[position]

            Glide.with(context).load(order.cartItemList!![0].foodImage).into(layoutBinding.imgFoodImage)
            calendar.timeInMillis = order.createDate
            val date = Date(order.createDate)
            layoutBinding.txtTime.setText(StringBuilder(Common.getDateOfWeek(calendar.get(Calendar.DAY_OF_WEEK)))
                .append(" ")
                .append(simpleDateFormat.format(date)))
            layoutBinding.txtOrderNumber.setText(StringBuilder("Order No.: ").append(order.orderNumber))
            layoutBinding.txtComment.setText(StringBuilder("Comment: ").append(order.comment))
            layoutBinding.txtNumItem.setText(StringBuilder("No. Items: ").append(order.cartItemList!!.size))
            layoutBinding.txtOrderStatus.setText(StringBuilder("Status: ")
                .append(Common.convertStatusToText(order.orderStatus)))
        }
    }

    override fun getItemCount(): Int = orderList.size
}