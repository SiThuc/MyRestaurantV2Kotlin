package com.example.myrestaurant_v2_kotlin.adapter

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.example.myrestaurant_v2_kotlin.callback.IRecyclerItemClickListener
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.database.CartItem
import com.example.myrestaurant_v2_kotlin.databinding.LayoutDialogOrderDetailBinding
import com.example.myrestaurant_v2_kotlin.databinding.LayoutOrderItemBinding
import com.example.myrestaurant_v2_kotlin.eventbus.CancelOrderEvent
import com.example.myrestaurant_v2_kotlin.model.Order
import org.greenrobot.eventbus.EventBus
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

class MyOrderAdapter(var context: Context, var orderList: MutableList<Order>):
    RecyclerView.Adapter<MyOrderAdapter.MyViewHolder>() {

    private var calendar: Calendar = Calendar.getInstance()
    private var simpleDateFormat: SimpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

    lateinit var layoutBinding: LayoutOrderItemBinding
    private val viewBinderHelper = ViewBinderHelper()

    init {
        viewBinderHelper.setOpenOnlyOne(true)
    }

    inner class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private var listener: IRecyclerItemClickListener? = null

        fun setListener(listener: IRecyclerItemClickListener) {
            this.listener = listener
        }

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            listener!!.onItemClick(v!!, adapterPosition)
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        layoutBinding = LayoutOrderItemBinding.inflate(LayoutInflater.from(context))
        return MyViewHolder(layoutBinding.root)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        with(holder){
            val order = orderList[position]
            viewBinderHelper.bind(layoutBinding.root, order.transactionId)

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

            //Event
            layoutBinding.btnCancel.setOnClickListener {
                EventBus.getDefault().postSticky(CancelOrderEvent(position))
            }

            layoutBinding.imgFoodImage.setOnClickListener {
                Log.d("DEBUG", "Clicked")
                showDialog(order.cartItemList!!)
            }

            /*setListener(object : IRecyclerItemClickListener {
                override fun onItemClick(view: View, pos: Int) {
                    Log.d("DEBUG", "Clicked")
                    showDialog(order.cartItemList!!)
                }

            })*/
        }
    }

    private fun showDialog(cartItemList: List<CartItem>) {
        val dialogBinding = LayoutDialogOrderDetailBinding.inflate(LayoutInflater.from(context))

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogBinding.root)

        dialogBinding.recyclerOrderDetail.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        dialogBinding.recyclerOrderDetail.layoutManager = layoutManager
        dialogBinding.recyclerOrderDetail.addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))
        val adapter = MyOrderDetailAdapter(context, cartItemList!!.toMutableList())
        dialogBinding.recyclerOrderDetail.adapter = adapter

        //Show dialog
        val dialog = builder.create()
        dialog.show()

        //Custom dialog
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setGravity(Gravity.CENTER)

        dialogBinding.btnOk.setOnClickListener { dialog.dismiss() }
    }

    override fun getItemCount(): Int = orderList.size

    fun getItemAtPosition(position: Int): Order{
        return orderList[position]
    }

    fun setItemAtPosition(position: Int, order: Order){
        orderList[position] = order
    }
}