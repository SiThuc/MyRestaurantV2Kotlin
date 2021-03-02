package com.example.myrestaurant_v2_kotlin.ui.cart

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myrestaurant_v2_kotlin.R
import com.example.myrestaurant_v2_kotlin.adapter.MyCartAdapter
import com.example.myrestaurant_v2_kotlin.callback.ILoadOrderCallbackListener
import com.example.myrestaurant_v2_kotlin.callback.ILoadTimeFromFirebaseCallback
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.database.CartDataSource
import com.example.myrestaurant_v2_kotlin.database.CartDatabase
import com.example.myrestaurant_v2_kotlin.database.LocalCartDataSource
import com.example.myrestaurant_v2_kotlin.databinding.FragmentCartBinding
import com.example.myrestaurant_v2_kotlin.databinding.LayoutPlaceOrderBinding
import com.example.myrestaurant_v2_kotlin.eventbus.CountCartEvent
import com.example.myrestaurant_v2_kotlin.eventbus.HideFABCart
import com.example.myrestaurant_v2_kotlin.eventbus.MenuItemBack
import com.example.myrestaurant_v2_kotlin.eventbus.UpdateItemInCart
import com.example.myrestaurant_v2_kotlin.model.FCMSendData
import com.example.myrestaurant_v2_kotlin.model.Order
import com.example.myrestaurant_v2_kotlin.service.IFCMService
import com.example.myrestaurant_v2_kotlin.service.RetrofitFCMClient
import com.google.android.gms.location.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class CartFragment : Fragment(), ILoadTimeFromFirebaseCallback {
    private var cartDataSource: CartDataSource? = null
    private var compositeDisposable: CompositeDisposable = CompositeDisposable()
    private lateinit var fragmentBinding: FragmentCartBinding
    private var recyclerViewState: Parcelable? = null
    private lateinit var adapter: MyCartAdapter

    private lateinit var viewModel: CartViewModel

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallBack: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentLocation: Location

    lateinit var ifcmService: IFCMService

    lateinit var listener: ILoadTimeFromFirebaseCallback


    override fun onResume() {
        super.onResume()
        calculateTotalPrice()

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallBack,
                Looper.getMainLooper()
            )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        EventBus.getDefault().postSticky(HideFABCart(true))
        viewModel = ViewModelProvider(this).get(CartViewModel::class.java)
        viewModel.initCartDataSource(requireContext())

        fragmentBinding = FragmentCartBinding.inflate(inflater, container, false)

        initViews()
        initLocation()

        viewModel.getCartMutableListLiveData().observe(viewLifecycleOwner, Observer {
            if (it == null || it.isEmpty()) {
                fragmentBinding.recyclerCart.visibility = View.GONE
                fragmentBinding.groupPlaceHolder.visibility = View.GONE
                fragmentBinding.txtEmpty.visibility = View.VISIBLE
            } else {
                fragmentBinding.recyclerCart.visibility = View.VISIBLE
                fragmentBinding.groupPlaceHolder.visibility = View.VISIBLE
                fragmentBinding.txtEmpty.visibility = View.GONE

                adapter = MyCartAdapter(requireContext(), it, cartDataSource!!)
                calculateTotalPrice()
                fragmentBinding.recyclerCart.adapter = adapter
            }
        })

        return fragmentBinding.root
    }

    private fun initLocation() {
        buildLocationRequest()
        buildLocationCallback()
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest, locationCallBack,
            Looper.getMainLooper()
        )
    }

    private fun buildLocationCallback() {
        locationCallBack = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                currentLocation = p0!!.lastLocation
            }
        }
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setInterval(5000)
        locationRequest.setFastestInterval(3000)
        locationRequest.setSmallestDisplacement(10f)
    }

    private fun initViews() {
        setHasOptionsMenu(true)

        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService::class.java)

        listener = this
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(requireContext()).cartDao())
        fragmentBinding.recyclerCart.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        fragmentBinding.recyclerCart.layoutManager = layoutManager
        fragmentBinding.recyclerCart.addItemDecoration(
            DividerItemDecoration(
                context,
                layoutManager.orientation
            )
        )


        //Set Event when user press the place order button
        fragmentBinding.btnPlaceOrder.setOnClickListener {
            var builder = AlertDialog.Builder(context)
            builder.setTitle("One more step")

            val alertDialogBinding = LayoutPlaceOrderBinding.inflate(LayoutInflater.from(context))

            //By default, we select rdi_home => show the user's address
            alertDialogBinding.edtAddress.setText(Common.currentUser!!.address)

            // If the RadioButton Home is checked
            alertDialogBinding.rdiDeliHome.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    alertDialogBinding.edtAddress.setText(Common.currentUser!!.address)
                }
            }

            //If the RadioButton Other is checked
            alertDialogBinding.rdiDeliOther.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    alertDialogBinding.edtAddress.setText("")
                    alertDialogBinding.edtAddress.setHint("Enter your address")
                    alertDialogBinding.txtDetailAddress.visibility = View.GONE
                }
            }

            //If the RadioButton Specific is checked
            alertDialogBinding.rdiDeliThisAddress.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                    }
                    fusedLocationProviderClient.lastLocation
                        .addOnFailureListener { e ->
                            alertDialogBinding.txtDetailAddress.visibility = View.GONE
                            Toast.makeText(requireContext(), "" + e.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                        .addOnCompleteListener { task ->
                            val coordinates = java.lang.StringBuilder()
                                .append(task.result.latitude)
                                .append("/")
                                .append(task.result.longitude)
                                .toString()

                            val singleAddress = Single.just(
                                getAddressFromLatLng(
                                    task.result.latitude,
                                    task.result.longitude
                                )
                            )
                            val disposable = singleAddress.subscribeWith(object :
                                DisposableSingleObserver<String>() {
                                override fun onSuccess(t: String) {
                                    alertDialogBinding.edtAddress.setText(coordinates)
                                    alertDialogBinding.txtDetailAddress.visibility = View.VISIBLE
                                    alertDialogBinding.txtDetailAddress.setText(t)
                                }

                                override fun onError(e: Throwable) {
                                    alertDialogBinding.edtAddress.setText(coordinates)
                                    alertDialogBinding.txtDetailAddress.visibility = View.VISIBLE
                                    alertDialogBinding.txtDetailAddress.setText(e.message)
                                }
                            })
                        }
                }
            }

            builder.setView(alertDialogBinding.root)
            builder.setNegativeButton("NO", { dialog, _ -> dialog.dismiss() })
                .setPositiveButton("YES", { dialog, _ ->
                    if (alertDialogBinding.rdiPayCod.isChecked)
                        paymentCOD(
                            alertDialogBinding.edtAddress.text.toString(),
                            alertDialogBinding.edtComment.text.toString()
                        )
                })
            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun getAddressFromLatLng(lat: Double, lng: Double): String {
        val geoCOder = Geocoder(context, Locale.getDefault())
        var result: String? = null
        try {
            val addressList = geoCOder.getFromLocation(lat, lng, 1)
            if (addressList != null && addressList.size > 0) {
                val address = addressList[0]
                val sb = java.lang.StringBuilder(address.getAddressLine(0))
                result = sb.toString()
            } else
                result = "Address not found!"
            return result
        } catch (e: IOException) {
            return e.message!!
        }
    }

    private fun paymentCOD(address: String, comment: String) {
        compositeDisposable.add(
            cartDataSource!!.getAllCart(Common.currentUser!!.uid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ cartItemList ->
                    // When we have all cartItems, we will get total price
                    cartDataSource!!.sumPrice(Common.currentUser!!.uid)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : SingleObserver<Double> {
                            override fun onSubscribe(d: Disposable) {
                            }

                            override fun onSuccess(t: Double) {
                                val finalPrice = t
                                val order = Order()
                                order.userId = Common.currentUser!!.uid
                                order.userName = Common.currentUser!!.name
                                order.userPhone = Common.currentUser!!.phone
                                order.shippingAddress = address
                                order.comment = comment

                                if (currentLocation != null) {
                                    order.lat = currentLocation.latitude
                                    order.lng = currentLocation.longitude
                                }

                                order.cartItemList = cartItemList
                                order.totalPayment = t
                                order.finalPayment = finalPrice
                                order.discount = 0
                                order.isCod = true
                                order.transactionId = "Cash On Delivery"

                                // Submit to FIrebase
                                syncLocalTimeWithServerTime(order)
                                //writeOrderToFirebase(order)
                            }

                            override fun onError(t: Throwable) {
                                Toast.makeText(
                                    context,
                                    "[SUM CART]" + t.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                }, { throwable ->
                    Toast.makeText(context, "" + throwable.message, Toast.LENGTH_SHORT).show()
                })
        )
    }

    private fun syncLocalTimeWithServerTime(order: Order) {
        val offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
        offsetRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val offset = snapshot.getValue(Long::class.java)
                val estimatedServerTimeInMs = System.currentTimeMillis() + offset!!
                val sdf = SimpleDateFormat("MM dd yyy, HH:mm")
                val date = Date(estimatedServerTimeInMs)
                Log.d("EDMT_DEV", "" + sdf.format(date))
                listener.onLoadTimeSuccess(order, estimatedServerTimeInMs)
            }

            override fun onCancelled(error: DatabaseError) {
                listener.onLoadTimeFailed(error.message)
            }
        })

    }

    private fun writeOrderToFirebase(order: Order) {
        FirebaseDatabase.getInstance()
            .getReference(Common.ORDER_REF)
            .child(Common.createOrderNumber())
            .setValue(order)
            .addOnFailureListener { e ->
                Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener { task ->
                //Clean cart
                if (task.isSuccessful) {
                    cartDataSource!!.cleanCart(Common.currentUser!!.uid)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : SingleObserver<Int> {
                            override fun onSubscribe(d: Disposable) {

                            }

                            override fun onSuccess(t: Int) {
                                val data2Send = HashMap<String, String>()
                                data2Send[Common.NOTI_TITLE] = "New Order"
                                data2Send[Common.NOTI_CONTENT] = "You have new order from " + Common.currentUser!!.phone
                                val sendData = FCMSendData(Common.getNewOrderTopic(), data2Send)
                                compositeDisposable.add(
                                    ifcmService.sendNotification(sendData)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe({ fcmResponse ->
                                            if (fcmResponse.success != 0)
                                                Toast.makeText(
                                                    requireContext(),
                                                    "Order placed successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                        }, { throwable ->
                                            Toast.makeText(
                                                requireContext(),
                                                "Order was sent but notification failed",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        )
                                )

                                Toast.makeText(
                                    context,
                                    "Order placed successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            override fun onError(e: Throwable) {
                                Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
                            }

                        })
                }
            }
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onStop() {
        // Send EventBus to HomeActivity to show the fabCart before fragment goes into Stopped State
        EventBus.getDefault().postSticky(HideFABCart(false))

        //Clear the compositeDispose from MyCartAdapter
        viewModel.onStop()

        // Clear the compositeDispose from this fragment
        compositeDisposable!!.clear()

        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this)

        super.onStop()
    }

    //EventBus for listening when user click on the elegant Button
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onUpdateItemInCart(event: UpdateItemInCart) {
        if (event.cartItem != null) {
            recyclerViewState = fragmentBinding.recyclerCart.layoutManager!!.onSaveInstanceState()
            cartDataSource!!.updateCart(event.cartItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<Int> {
                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onSuccess(t: Int) {
                        calculateTotalPrice();
                        fragmentBinding.recyclerCart.layoutManager!!.onRestoreInstanceState(
                            recyclerViewState
                        )
                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context, "[UPDATE CART] " + e.message, Toast.LENGTH_SHORT)
                            .show()
                    }

                })
        }
    }

    private fun calculateTotalPrice() {
        cartDataSource!!.sumPrice(Common.currentUser!!.uid)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Double> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Double) {
                    fragmentBinding.txtTotalPrice.text = StringBuilder("€")
                        .append(Common.formatPrice(t))
                }

                override fun onError(e: Throwable) {
                    if (!e.message!!.contains("Query returned empty"))
                        Toast.makeText(context, "[SUM CART]" + e.message, Toast.LENGTH_SHORT).show()

                }

            })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.cart_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_clear_cart) {
            cartDataSource!!.cleanCart(Common.currentUser!!.uid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<Int> {
                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onSuccess(t: Int) {
                        Toast.makeText(context, "Clear Cart Success", Toast.LENGTH_SHORT)
                        EventBus.getDefault().postSticky(CountCartEvent(true))
                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
                    }

                })
        }
        return true
        return super.onOptionsItemSelected(item)
    }


    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_settings).setVisible(false)
        super.onPrepareOptionsMenu(menu)
    }

    override fun onLoadTimeSuccess(order: Order, estimatedTimeMs: Long) {
        order.createDate = (estimatedTimeMs)
        order.orderStatus = 0
        writeOrderToFirebase(order)

    }

    override fun onLoadTimeFailed(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }

}