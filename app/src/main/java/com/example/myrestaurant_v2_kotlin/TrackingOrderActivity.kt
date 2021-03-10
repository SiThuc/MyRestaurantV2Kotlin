package com.example.myrestaurant_v2_kotlin

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.remote.IGoogleAPI
import com.example.myrestaurant_v2_kotlin.remote.RetrofitGoogleAPIClient
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import java.lang.Exception
import kotlin.text.StringBuilder

class TrackingOrderActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var shipperMarker: Marker? = null
    private var polylineOptions: PolylineOptions? = null
    private var blackPolylineOptions: PolylineOptions? = null
    private var yellowPolyline: Polyline? = null
    private var polylineList: List<LatLng> = ArrayList()

    private lateinit var iGoogleAPI: IGoogleAPI
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking_order)

        iGoogleAPI = RetrofitGoogleAPIClient.instance!!.create(IGoogleAPI::class.java)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

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
                    .title(Common.currentShipperOrder!!.shipperName)
                    .snippet(Common.currentShipperOrder!!.shipperPhone)
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
}