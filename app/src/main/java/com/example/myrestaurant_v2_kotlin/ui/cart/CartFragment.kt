package com.example.myrestaurant_v2_kotlin.ui.cart

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Geocoder
import android.location.Location
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myrestaurant_v2_kotlin.R
import com.example.myrestaurant_v2_kotlin.adapter.MyCartAdapter
import com.example.myrestaurant_v2_kotlin.callback.ILoadTimeFromFirebaseCallback
import com.example.myrestaurant_v2_kotlin.callback.ISearchCategoryCallbackListener
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.database.CartDataSource
import com.example.myrestaurant_v2_kotlin.database.CartDatabase
import com.example.myrestaurant_v2_kotlin.database.CartItem
import com.example.myrestaurant_v2_kotlin.database.LocalCartDataSource
import com.example.myrestaurant_v2_kotlin.databinding.FragmentCartBinding
import com.example.myrestaurant_v2_kotlin.databinding.LayoutAddonDisplayBinding
import com.example.myrestaurant_v2_kotlin.databinding.LayoutPlaceOrderBinding
import com.example.myrestaurant_v2_kotlin.eventbus.*
import com.example.myrestaurant_v2_kotlin.model.*
import com.example.myrestaurant_v2_kotlin.service.IFCMService
import com.example.myrestaurant_v2_kotlin.service.RetrofitFCMClient
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CartFragment : Fragment(), ILoadTimeFromFirebaseCallback, ISearchCategoryCallbackListener, TextWatcher {
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
    private var searchCategorylistener: ISearchCategoryCallbackListener = this

    private var placeSelected: Place? = null
    private var placesFragment: AutocompleteSupportFragment? = null
    private lateinit var placeClient: PlacesClient
    private val placeFields = Arrays.asList(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
    )

    private lateinit var addonBottomSheetDialog: BottomSheetDialog
    private lateinit var addonBinding: LayoutAddonDisplayBinding


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
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 3000
        locationRequest.smallestDisplacement = 10f
    }

    private fun initViews() {
        initPlacesClient()

        setHasOptionsMenu(true)

        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService::class.java)

        listener = this

        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(requireContext()).cartDao())

        // Initiate the addonBottomSheetDialog
        addonBottomSheetDialog = BottomSheetDialog(requireContext())
        addonBinding = LayoutAddonDisplayBinding.inflate(layoutInflater)
        addonBottomSheetDialog.setContentView(addonBinding.root)

        addonBottomSheetDialog.setOnDismissListener {
            displayUserSelectedAddon(addonBinding.chipGroupAddon)
            calculateTotalPrice()
        }

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

            placesFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.places_fragment) as? AutocompleteSupportFragment

            //places_fragment = requireActivity().supportFragmentManager.findFragmentById(R.id.places_fragment) as AutocompleteSupportFragment

            placesFragment!!.setPlaceFields(placeFields)
            placesFragment!!.setOnPlaceSelectedListener(object : PlaceSelectionListener {
                override fun onPlaceSelected(p0: Place) {
                    placeSelected = p0
                    alertDialogBinding.txtDetailAddress.text = placeSelected!!.address
                }

                override fun onError(p0: Status) {
                    Toast.makeText(requireContext(), "" + p0.statusMessage, Toast.LENGTH_SHORT).show()
                }
            })

            //By default, we select rdi_home => show the user's address
            alertDialogBinding.txtDetailAddress.text = Common.currentUser!!.address

            // If the RadioButton Home is checked
            alertDialogBinding.rdiDeliHome.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    alertDialogBinding.txtDetailAddress.text = Common.currentUser!!.address
                }
            }

            //If the RadioButton Other is checked
            alertDialogBinding.rdiDeliOther.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    alertDialogBinding.txtDetailAddress.text = ""
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
                                        alertDialogBinding.txtDetailAddress.text = t
                                    }

                                    override fun onError(e: Throwable) {
                                        alertDialogBinding.txtDetailAddress.text = e.message
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
                                    alertDialogBinding.txtDetailAddress.text.toString(),
                                    alertDialogBinding.edtComment.text.toString()
                            )
                    })
            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun displayUserSelectedAddon(chipGroupAddon: ChipGroup) {
        if (Common.foodSelected!!.userSelectedAddon != null && Common.foodSelected!!.userSelectedAddon!!.size > 0) {
            chipGroupAddon.removeAllViews()
            for (addOnModel in Common.foodSelected!!.userSelectedAddon!!) {
                val chip = layoutInflater.inflate(R.layout.layout_chip_with_delete, null) as Chip
                chip.text = StringBuilder(addOnModel.name).append("+(€").append(addOnModel.price).append(")")
                //Event
                chip.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        if (Common.foodSelected!!.userSelectedAddon == null)
                            Common.foodSelected!!.userSelectedAddon = ArrayList()
                        Common.foodSelected!!.userSelectedAddon!!.add(addOnModel)
                    }
                }
                chipGroupAddon.addView(chip)

            }
        }else
            chipGroupAddon.removeAllViews()

    }

    private fun initPlacesClient() {
        Places.initialize(requireContext(), getString(R.string.google_maps_key))
        placeClient = Places.createClient(requireContext())
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
                                            val order = OrderModel()
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
                                            order.cod = true
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

    private fun syncLocalTimeWithServerTime(order: OrderModel) {
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

    private fun writeOrderToFirebase(order: OrderModel) {
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

    //EventBus for listening when user click on Update Button
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onUpdateAddonSize(event: UpdateAddonSizeEvent){
        var cartItem = (fragmentBinding.recyclerCart.adapter as MyCartAdapter).getItemAtPosition(event.position)
        FirebaseDatabase.getInstance()
                .getReference(Common.CATEGORY_REF)
                .child(cartItem.categoryId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if(snapshot.exists()){
                            val categoryModel = snapshot.getValue(CategoryModel::class.java)
                            searchCategorylistener.onSearchFound(categoryModel!!, cartItem)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        searchCategorylistener.onSearchNotFound(error.message)
                    }
                })

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

    override fun onLoadTimeSuccess(order: OrderModel, estimatedTimeMs: Long) {
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

    override fun onSearchFound(category: CategoryModel, cartItem: CartItem) {
        val foodModel: FoodModel = Common.findFoodInListById(category, cartItem.foodId)!!
        showUpdateDialog(cartItem, foodModel)
    }

    override fun onSearchNotFound(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun showUpdateDialog(cartItem: CartItem, foodModel: FoodModel) {
        Common.foodSelected = foodModel
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_dialog_update_cart, null)
        builder.setView(itemView)

        //View
        val btnOk = itemView.findViewById<View>(R.id.btn_ok) as Button
        val btnCancel = itemView.findViewById<View>(R.id.btn_cancel) as Button

        val rdiGroupSize = itemView.findViewById<View>(R.id.rdi_group_size) as RadioGroup
        val chipGroupUserSelectedAddon = itemView.findViewById<View>(R.id.chip_group_user_selected_addon) as ChipGroup
        val imgAddon = itemView.findViewById<View>(R.id.img_addon) as ImageView

        imgAddon.setOnClickListener {
            if(foodModel.addon != null){
                displayAddonList()
                addonBottomSheetDialog.show()
            }
        }

        //Size
        if(foodModel.size != null){
            for(sizeModel in foodModel.size){
                val radioButton = RadioButton(requireContext())
                radioButton.setOnCheckedChangeListener { buttonView, isChecked ->
                    if(isChecked)
                        Common.foodSelected!!.userSelectedSize = sizeModel
                    calculateTotalPrice()
                }

                val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
                radioButton.layoutParams = params
                radioButton.text = sizeModel.name
                radioButton.tag = sizeModel.price
                rdiGroupSize.addView(radioButton)
            }

            if(rdiGroupSize.childCount > 0){
                val radioButton = rdiGroupSize.getChildAt(0) as RadioButton
                radioButton.isChecked = true // Default check

            }
        }

        //Addon
        displayAlreadySelectedAddon(chipGroupUserSelectedAddon, cartItem)

        //Show dialog
        val dialog = builder.create()
        dialog.show()

        //Custom
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setGravity(Gravity.CENTER)

        //Event
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnOk.setOnClickListener {
            //Delete Item first
            cartDataSource!!.deleteCart(cartItem)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : SingleObserver<Int> {
                        override fun onSubscribe(d: Disposable) {

                        }

                        override fun onSuccess(t: Int) {
                           //After delete success, we will update new information and add to cart again
                            if(Common.foodSelected!!.userSelectedAddon != null)
                                cartItem.foodAddon = Gson().toJson(Common.foodSelected!!.userSelectedAddon)
                            else
                                cartItem.foodAddon = "Default"
                            if(Common.foodSelected!!.userSelectedSize != null)
                                cartItem.foodSize = Gson().toJson(Common.foodSelected!!.userSelectedSize)
                            else
                                cartItem.foodSize = "Default"

                            cartItem.foodExtraPrice = Common.calculateExtraPrice(Common.foodSelected!!.userSelectedSize,
                            Common.foodSelected!!.userSelectedAddon)

                            //Insert
                            compositeDisposable.add(cartDataSource!!.insertOrReplaceAll(cartItem)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe({
                                                EventBus.getDefault().postSticky(CountCartEvent(true))
                                                dialog.dismiss()
                                                calculateTotalPrice()
                                                Toast.makeText(context, "Update cart success", Toast.LENGTH_SHORT).show()

                                            }, { t: Throwable ->
                                                Toast.makeText(context, "[INSERT CART] " + t.message, Toast.LENGTH_SHORT).show()
                                            })
                            )
                        }

                        override fun onError(e: Throwable) {
                            Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                        }
                    })
        }
    }

    private fun displayAlreadySelectedAddon(chipGroupUserSelectedAddon: ChipGroup, cartItem: CartItem) {
        //This function will display all addon we already selected before add to cart and display on layout
        if(!cartItem.equals("Default")){
            val addonModels: List<AddonModel> = Gson().fromJson(cartItem.foodAddon, object : TypeToken<List<AddonModel>>(){}.type)
            Common.foodSelected!!.userSelectedAddon = addonModels.toMutableList()
            chipGroupUserSelectedAddon.removeAllViews()
            //Add all view
            for(addonModel in addonModels){
                val chip = layoutInflater.inflate(R.layout.layout_chip_with_delete, null) as Chip
                chip.text = StringBuilder(addonModel.name).append("(+€").append(addonModel.price).append(")")
                chip.isClickable = false
                chip.setOnCloseIconClickListener {
                    chipGroupUserSelectedAddon.removeView(it)
                    Common.foodSelected!!.userSelectedAddon!!.remove(addonModel)
                    calculateTotalPrice()
                }
                addonBinding.chipGroupAddon.addView(chip)
            }
        }
    }

    private fun displayAddonList() {
        if(Common.foodSelected!!.addon.isNotEmpty()){
            addonBinding.chipGroupAddon.clearCheck()
            addonBinding.chipGroupAddon.removeAllViews()
            addonBinding.edtSearch.addTextChangedListener(this)

            // Add all view
            for(addonModel in Common.foodSelected!!.addon){
                val chip = layoutInflater.inflate(R.layout.layout_chip, null) as Chip
                chip.text = StringBuilder(addonModel.name).append("(+€").append(addonModel.price).append(")")
                chip.setOnCheckedChangeListener { buttonView, isChecked ->
                    if(isChecked)
                        if(Common.foodSelected!!.userSelectedAddon == null)
                            Common.foodSelected!!.userSelectedAddon = ArrayList()
                    Common.foodSelected!!.userSelectedAddon!!.add(addonModel)

                }
                addonBinding.chipGroupAddon.addView(chip)
            }
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
       addonBinding.chipGroupAddon.clearCheck()
        addonBinding.chipGroupAddon.removeAllViews()

        for(addonModel in Common.foodSelected!!.addon){
           if(addonModel.name!!.toLowerCase().contains(s.toString().toLowerCase())){
               val chip = layoutInflater.inflate(R.layout.layout_chip, null) as Chip
               chip.text = StringBuilder(addonModel.name).append("(+€").append(addonModel.price).append(")")
               chip.setOnCheckedChangeListener { buttonView, isChecked ->
                   if(isChecked)
                       if(Common.foodSelected!!.userSelectedAddon == null)
                           Common.foodSelected!!.userSelectedAddon = ArrayList()
                   Common.foodSelected!!.userSelectedAddon!!.add(addonModel)

               }
               addonBinding.chipGroupAddon.addView(chip)
           }
        }
    }

    override fun afterTextChanged(s: Editable?) {

    }

}