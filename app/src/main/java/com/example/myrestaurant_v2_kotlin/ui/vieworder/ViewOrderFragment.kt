package com.example.myrestaurant_v2_kotlin.ui.vieworder

import android.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myrestaurant_v2_kotlin.R
import com.example.myrestaurant_v2_kotlin.adapter.MyOrderAdapter
import com.example.myrestaurant_v2_kotlin.callback.ILoadOrderCallbackListener
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.databinding.FragmentViewOrderBinding
import com.example.myrestaurant_v2_kotlin.eventbus.MenuItemBack
import com.example.myrestaurant_v2_kotlin.model.Order
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList

class ViewOrderFragment : Fragment(), ILoadOrderCallbackListener {

    private lateinit var viewModel: ViewOrderViewModel
    private lateinit var binding: FragmentViewOrderBinding

    lateinit var dialog: AlertDialog
    lateinit var listener: ILoadOrderCallbackListener


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewModel = ViewModelProvider(this).get(ViewOrderViewModel::class.java)
        binding = FragmentViewOrderBinding.inflate(inflater, container, false)
        initViews()
        loadOrderFromFirebase()

        viewModel.orderLiveDataMutableList.observe(viewLifecycleOwner, Observer {
            Collections.reverse(it)
            val adapter = MyOrderAdapter(requireContext(), it)
            binding.recyclerOrder.adapter = adapter
        })

        return binding.root
    }

    private fun loadOrderFromFirebase() {
        dialog.show()

        val orderList = ArrayList<Order>()
        FirebaseDatabase.getInstance().getReference(Common.ORDER_REF)
            .orderByChild("userId")
            .equalTo(Common.currentUser!!.uid)
            .limitToLast(100)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (orderSnapshot in snapshot.children) {
                        val order = orderSnapshot.getValue(Order::class.java)
                        order!!.orderNumber = orderSnapshot.key
                        orderList.add(order)
                    }
                    listener.onLoadOrdersSuccess(orderList)
                }

                override fun onCancelled(error: DatabaseError) {
                    listener.onLoadOrdersFailed(error.message)
                }
            })
    }

    private fun initViews() {
        listener = this
        dialog = SpotsDialog.Builder().setContext(context).setCancelable(false).build()

        binding.recyclerOrder.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        binding.recyclerOrder.layoutManager = layoutManager
        binding.recyclerOrder.addItemDecoration(
            DividerItemDecoration(
                context,
                layoutManager.orientation
            )
        )

    }

    override fun onLoadOrdersSuccess(orderList: List<Order>) {
        dialog.dismiss()
        viewModel.setOrderLiveDataMutableList(orderList)
    }

    override fun onLoadOrdersFailed(message: String) {
        dialog.dismiss()
        Toast.makeText(context, "" + message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }

}