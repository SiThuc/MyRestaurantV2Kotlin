package com.example.myrestaurant_v2_kotlin.ui.vieworder

import android.app.AlertDialog
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.Layout
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myrestaurant_v2_kotlin.TrackingOrderActivity
import com.example.myrestaurant_v2_kotlin.adapter.MyOrderAdapter
import com.example.myrestaurant_v2_kotlin.callback.ILoadOrderCallbackListener
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.databinding.FragmentViewOrderBinding
import com.example.myrestaurant_v2_kotlin.databinding.LayoutRefundRequestBinding
import com.example.myrestaurant_v2_kotlin.eventbus.CancelOrderEvent
import com.example.myrestaurant_v2_kotlin.eventbus.MenuItemBack
import com.example.myrestaurant_v2_kotlin.eventbus.TrackingOrderEvent
import com.example.myrestaurant_v2_kotlin.model.OrderModel
import com.example.myrestaurant_v2_kotlin.model.RefundRequestModel
import com.example.myrestaurant_v2_kotlin.model.ShipperOrderModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

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
            val adapter = MyOrderAdapter(requireContext(), it.toMutableList())
            binding.recyclerOrder.adapter = adapter
        })

        return binding.root
    }

    private fun loadOrderFromFirebase() {
        dialog.show()

        val orderList = ArrayList<OrderModel>()
        FirebaseDatabase.getInstance().getReference(Common.ORDER_REF)
            .orderByChild("userId")
            .equalTo(Common.currentUser!!.uid)
            .limitToLast(100)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (orderSnapshot in snapshot.children) {
                        val order = orderSnapshot.getValue(OrderModel::class.java)
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

    override fun onLoadOrdersSuccess(orderList: List<OrderModel>) {
        dialog.dismiss()
        viewModel.setOrderLiveDataMutableList(orderList)
    }

    override fun onLoadOrdersFailed(message: String) {
        dialog.dismiss()
        Toast.makeText(context, "" + message, Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())

        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCancelOrderEvent(event: CancelOrderEvent) {
        val order =
            (binding.recyclerOrder.adapter as MyOrderAdapter).getItemAtPosition(event.position)
        if (order.orderStatus == 0) {

            //If payment == COD
            if (order.cod) {
                val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                builder.setTitle("Cancel Order")
                    .setMessage("Do you really want to cancel this order?")
                    .setNegativeButton("NO") { dialogInterface, i ->
                        dialogInterface.dismiss()
                    }
                    .setPositiveButton("YES") { dialogInterface, i ->
                        val updateData = HashMap<String, Any>()
                        updateData["orderStatus"] = -1
                        FirebaseDatabase.getInstance()
                            .getReference(Common.ORDER_REF)
                            .child(order.orderNumber!!)
                            .updateChildren(updateData)
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT)
                                    .show()
                            }
                            .addOnSuccessListener {
                                order.orderStatus = -1
                                (binding.recyclerOrder.adapter as MyOrderAdapter).setItemAtPosition(
                                    event.position,
                                    order
                                )
                                (binding.recyclerOrder.adapter as MyOrderAdapter).notifyItemChanged(
                                    event.position
                                )
                                Toast.makeText(
                                    requireContext(),
                                    "Cancel order successful",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }

                val dialogCancel = builder.create()
                dialogCancel.show()
            }
            else{  //Payment != COD
                val dialogBinding = LayoutRefundRequestBinding.inflate(LayoutInflater.from(context))

                //Set format
                dialogBinding.edtCardNumber.setFormat("---- ---- ---- ----")
                dialogBinding.edtExp.setFormat("--/--")

                val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                builder.setTitle("Cancel Order")
                    .setMessage("Do you really want to cancel this order?")
                    .setView(dialogBinding.root)
                    .setNegativeButton("NO"){dialogInterface, i -> dialogInterface.dismiss() }
                    .setPositiveButton("YES"){dialogInterface, i ->

                        val refundRequest = RefundRequestModel()
                        refundRequest.name = Common.currentUser!!.name!!
                        refundRequest.phone = Common.currentUser!!.phone!!
                        refundRequest.cardName = dialogBinding.edtCardName.text.toString()
                        refundRequest.cardNumber = dialogBinding.edtCardNumber.text.toString()
                        refundRequest.cardExp = dialogBinding.edtExp.text.toString()
                        refundRequest.amount = order.finalPayment


                        FirebaseDatabase.getInstance()
                            .getReference(Common.REFUND_REQUEST_REF)
                            .child(order.orderNumber!!)
                            .setValue(refundRequest)
                            .addOnFailureListener { e -> Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show() }
                            .addOnSuccessListener {
                                //Update data to Firebase
                                val updateData = HashMap<String, Any>()
                                updateData["orderStatus"] = -1  //Cancel order
                                FirebaseDatabase.getInstance()
                                    .getReference(Common.ORDER_REF)
                                    .child(order.orderNumber!!)
                                    .updateChildren(updateData)
                                    .addOnFailureListener { e ->
                                        Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                    .addOnSuccessListener {
                                        order.orderStatus = -1
                                        (binding.recyclerOrder.adapter as MyOrderAdapter).setItemAtPosition(
                                            event.position,
                                            order
                                        )
                                        (binding.recyclerOrder.adapter as MyOrderAdapter).notifyItemChanged(
                                            event.position
                                        )
                                        Toast.makeText(
                                            requireContext(),
                                            "Cancel order successful",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                    }

                val dialogCancel = builder.create()
                dialogCancel.show()

            }

        } else {
            Toast.makeText(
                requireContext(), StringBuilder("Your order status was changed to ")
                    .append(Common.convertStatusToText(order.orderStatus))
                    .append(", so you can't cancel it"), Toast.LENGTH_SHORT
            ).show()
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onTrackingOrderEvent(event: TrackingOrderEvent) {
        val orderModel = (binding.recyclerOrder.adapter as MyOrderAdapter).getItemAtPosition(event.position)
        Log.d("DEBUG",orderModel.orderNumber!! )

        //Fetch data from Firebase
        FirebaseDatabase.getInstance()
            .getReference(Common.SHIPPING_ORDER_REF)
            .child(orderModel.orderNumber!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) { // If there are a shipperOrder
                        Common.currentShipperOrder = snapshot.getValue(ShipperOrderModel::class.java)
                        if (Common.currentShipperOrder!!.currentLat != -1.0 &&
                            Common.currentShipperOrder!!.currentLng != -1.0) {
                            startActivity(Intent(requireContext(), TrackingOrderActivity::class.java))
                        } else {
                            Toast.makeText(requireContext(), "Your order has not been ship, please wait", Toast.LENGTH_SHORT).show()
                        }

                    } else { //If there is no shipperOrder
                        Toast.makeText(requireContext(), "You have just placed order, please wait until it will be shipping", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

}