package com.example.myrestaurant_v2_kotlin

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.common.MyCustomInfoWindow
import com.example.myrestaurant_v2_kotlin.model.ShipperOrderModel
import com.example.myrestaurant_v2_kotlin.remote.IGoogleAPI
import com.example.myrestaurant_v2_kotlin.remote.RetrofitGoogleAPIClient
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import java.lang.Exception
import kotlin.text.StringBuilder

class TrackingOrderActivity : AppCompatActivity(), OnMapReadyCallback, ValueEventListener {

    private lateinit var mMap: GoogleMap
    private var shipperMarker: Marker? = null
    private var polylineOptions: PolylineOptions? = null
    private var blackPolylineOptions: PolylineOptions? = null
    private var blackPolyline: Polyline? = null
    private var grayPolyline: Polyline? = null
    private var yellowPolyline: Polyline? = null
    private var polylineList: List<LatLng> = ArrayList()

    private lateinit var iGoogleAPI: IGoogleAPI
    private val compositeDisposable = CompositeDisposable()

    private lateinit var shipperRef: DatabaseReference
    private var isInit = false  //isInist must be false for first time

    private lateinit var btnCall: MaterialButton

    //Move maker
    private var handler: Handler? = null
    private var index = 0
    private var next: Int = 0
    private var v = 0f
    private var lat = 0.0
    private var lng = 0.0
    private var startPosition = LatLng(0.0, 0.0)
    private var endPosition = LatLng(0.0, 0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking_order)

        iGoogleAPI = RetrofitGoogleAPIClient.instance!!.create(IGoogleAPI::class.java)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        subscribeShipperMove()

        initView()
    }

    private fun initView() {
        btnCall = findViewById<MaterialButton>(R.id.btn_call)
        btnCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = (Uri.parse(StringBuilder("tel:").append(Common.currentShipperOrder!!.shipperPhone).toString()))

            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
                //Request permission
                Dexter.withContext(this)
                        .withPermission(Manifest.permission.CALL_PHONE)
                        .withListener(object : PermissionListener {
                            override fun onPermissionGranted(p0: PermissionGrantedResponse?) {

                            }

                            override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                                Toast.makeText(this@TrackingOrderActivity, "You must enable this permission to CALL", Toast.LENGTH_SHORT).show()
                            }

                            override fun onPermissionRationaleShouldBeShown(p0: PermissionRequest?, p1: PermissionToken?) {

                            }
                        }).check()
                return@setOnClickListener
            }
            startActivity(intent)
        }

    }

    private fun subscribeShipperMove() {
        shipperRef = FirebaseDatabase.getInstance()
                .getReference(Common.SHIPPING_ORDER_REF)
                .child(Common.currentShipperOrder!!.key!!)
        shipperRef.addValueEventListener(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setInfoWindowAdapter(MyCustomInfoWindow(layoutInflater))

        mMap.uiSettings.isZoomControlsEnabled = true
        try {
            val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.uber_light_with_label))
            if (!success)
                Log.d("DEBUG", "Failed to load map style")
        } catch (ex: Resources.NotFoundException) {
            Log.d("DEBUG", "Not found json string for map style")
        }

        drawRoutes()
    }

    private fun drawRoutes() {
        val locationOrder = LatLng(Common.currentShipperOrder!!.orderModel!!.lat, Common.currentShipperOrder!!.orderModel!!.lng)
        val locationShipper = LatLng(Common.currentShipperOrder!!.currentLat, Common.currentShipperOrder!!.currentLng)

        //Add Box
        mMap.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.box))
                .title(Common.currentShipperOrder!!.orderModel!!.userName)
                .snippet(Common.currentShipperOrder!!.orderModel!!.shippingAddress)
                .position(locationOrder))

        //Add Shipper
        if (shipperMarker == null) {
            val height = 80
            val width = 80
            val bitmapDrawable = ContextCompat.getDrawable(this@TrackingOrderActivity, R.drawable.shippernew) as BitmapDrawable
            val resized = Bitmap.createScaledBitmap(bitmapDrawable.bitmap, width, height, false)

            shipperMarker = mMap.addMarker(MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(resized))
                    .title(StringBuilder("Shipper: ").append(Common.currentShipperOrder!!.shipperName).toString())
                    .snippet(StringBuilder("Phone: ").append(Common.currentShipperOrder!!.shipperPhone)
                            .append("\n")
                            .append("Estimate Delivery Time:")
                            .append(Common.currentShipperOrder!!.estimateTime).toString())
                    .position(locationShipper))

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 18.0f))
        } else {
            shipperMarker!!.position = locationShipper
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 18.0f))
        }

        //Draw Routes
        val to = StringBuilder().append(Common.currentShipperOrder!!.orderModel!!.lat)
                .append(",")
                .append(Common.currentShipperOrder!!.orderModel!!.lng)
                .toString()

        val from = StringBuilder().append(Common.currentShipperOrder!!.currentLat)
                .append(",")
                .append(Common.currentShipperOrder!!.currentLng)
                .toString()

        compositeDisposable.add(iGoogleAPI!!.getDirections(
                "driving", "less_driving", from, to,
                getString(R.string.google_maps_key))!!
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ s ->
                    try {
                        val jsonObject = JSONObject(s)
                        val jsonArray = jsonObject.getJSONArray("routes")
                        for (i in 0 until jsonArray.length()) {
                            val route = jsonArray.getJSONObject(i)
                            val poly = route.getJSONObject("overview_polyline")
                            val polyline = poly.getString("points")
                            polylineList = Common.decodePoly(polyline)
                        }

                        polylineOptions = PolylineOptions()
                        polylineOptions!!.color(Color.YELLOW)
                        polylineOptions!!.width(12.0f)
                        polylineOptions!!.startCap(SquareCap())
                        polylineOptions!!.endCap(SquareCap())
                        polylineOptions!!.jointType(JointType.ROUND)
                        polylineOptions!!.addAll(polylineList)
                        yellowPolyline = mMap.addPolyline(polylineOptions)
                    } catch (e: Exception) {
                        Log.d("DEBUG", e.message.toString())
                    }

                }, { t ->
                    Toast.makeText(this@TrackingOrderActivity, t.message.toString(), Toast.LENGTH_SHORT).show()
                })
        )

    }

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }

    override fun onDestroy() {
        shipperRef.removeEventListener(this)
        isInit = false
        super.onDestroy()
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        //Save old position
        val from = StringBuilder().append(Common.currentShipperOrder!!.currentLat)
                .append(",")
                .append(Common.currentShipperOrder!!.currentLng).toString()

        //Update position
        Common.currentShipperOrder = snapshot.getValue(ShipperOrderModel::class.java)
        Common.currentShipperOrder!!.key = snapshot.key

        //Save new position
        val to = StringBuilder().append(Common.currentShipperOrder!!.currentLat)
                .append(",")
                .append(Common.currentShipperOrder!!.currentLng).toString()

        if(snapshot.exists())
            if(isInit)
                moveMarkerAnimation(shipperMarker, from, to)
            else
                isInit = true
    }

    private fun moveMarkerAnimation(shipperMarker: Marker?, from: String, to: String) {
        compositeDisposable.addAll(
                iGoogleAPI!!.getDirections("driving", "less_driving", from, to, getString(R.string.google_maps_key))!!
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ s ->
                            Log.d("DEBUG", s!!)
                            try {
                                val jsonObject = JSONObject(s)
                                val jsonArray = jsonObject.getJSONArray("routes")
                                for (i in 0 until jsonArray.length()) {
                                    val route = jsonArray.getJSONObject(i)
                                    val poly = route.getJSONObject("overview_polyline")
                                    val polyline = poly.getString("points")

                                    polylineList = Common.decodePoly(polyline)
                                }

                                polylineOptions = PolylineOptions()
                                polylineOptions!!.color(Color.GRAY)
                                polylineOptions!!.width(5.0f)
                                polylineOptions!!.startCap(SquareCap())
                                polylineOptions!!.endCap(SquareCap())
                                polylineOptions!!.jointType(JointType.ROUND)
                                polylineOptions!!.addAll(polylineList)
                                grayPolyline = mMap.addPolyline(polylineOptions)

                                blackPolylineOptions = PolylineOptions()
                                blackPolylineOptions!!.color(Color.BLACK)
                                blackPolylineOptions!!.width(5.0f)
                                blackPolylineOptions!!.startCap(SquareCap())
                                blackPolylineOptions!!.endCap(SquareCap())
                                blackPolylineOptions!!.jointType(JointType.ROUND)
                                blackPolylineOptions!!.addAll(polylineList)
                                blackPolyline = mMap.addPolyline(blackPolylineOptions)


                                //Animator
                                val polylineAnimator = ValueAnimator.ofInt(0, 100)
                                polylineAnimator.duration = 2000
                                polylineAnimator.interpolator = LinearInterpolator()
                                polylineAnimator.addUpdateListener { valueAnimator ->
                                    val points = grayPolyline!!.points
                                    val percentValue =
                                            Integer.parseInt(valueAnimator.animatedValue.toString())
                                    val size = points.size
                                    val newPoints = (size * (percentValue / 100.0f).toInt())
                                    val p = points.subList(0, newPoints)
                                    blackPolyline!!.points = p
                                }
                                polylineAnimator.start()

                                //Car moving
                                index = -1
                                next = 1
                                val r = object : Runnable {
                                    override fun run() {
                                        if (index < polylineList.size - 1) {
                                            index++
                                            next = index + 1
                                            startPosition = polylineList[index]
                                            endPosition = polylineList[next]
                                        }

                                        val valueAnimator = ValueAnimator.ofInt(0, 1)
                                        valueAnimator.duration = 1500
                                        valueAnimator.interpolator = LinearInterpolator()
                                        valueAnimator.addUpdateListener { valueAnimator ->
                                            v = valueAnimator.animatedFraction
                                            lat =
                                                    v * endPosition!!.latitude + (1 - v) * startPosition!!.latitude
                                            lng =
                                                    v * endPosition!!.longitude + (1 - v) * startPosition!!.longitude

                                            val newPos = LatLng(lat, lng)
                                            shipperMarker!!.position = newPos
                                            shipperMarker.setAnchor(0.5f, 0.5f)
                                            shipperMarker.rotation = Common.getBearing(startPosition!!, newPos)

                                            mMap.moveCamera(CameraUpdateFactory.newLatLng(shipperMarker.position)) //Fixed
                                        }

                                        valueAnimator.start()
                                        if (index < polylineList.size - 2)
                                            handler!!.postDelayed(this, 1500)
                                    }

                                }

                                handler = Handler()
                                handler!!.postDelayed(r, 1500)

                            } catch (e: Exception) {
                                Log.d("DEBUG", e.message.toString())
                            }

                        }, { throwable ->
                            Toast.makeText(this@TrackingOrderActivity, throwable.message, Toast.LENGTH_SHORT)
                                    .show()
                        })
        )
    }

    override fun onCancelled(error: DatabaseError) {

    }
}