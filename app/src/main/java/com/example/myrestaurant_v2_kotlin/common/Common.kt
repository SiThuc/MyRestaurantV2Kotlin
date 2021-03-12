package com.example.myrestaurant_v2_kotlin.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.myrestaurant_v2_kotlin.R
import com.example.myrestaurant_v2_kotlin.model.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.FirebaseDatabase
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

object Common {
    fun updateToken(context: Context, token: String) {
        //Fix Error crash first time
        if (currentUser != null)
            FirebaseDatabase.getInstance()
                    .getReference(TOKEN_REF)
                    .child(currentUser!!.uid)
                    .setValue(TokenModel(currentUser!!.phone!!, token))
                    .addOnFailureListener { e -> Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show() }
    }

    fun getDateOfWeek(i: Int): String {
        when (i) {
            1 -> return "Monday"
            2 -> return "Tuesday"
            3 -> return "Wednesday"
            4 -> return "Thursday"
            5 -> return "Friday"
            6 -> return "Saturday"
            7 -> return "Sunday"
            else -> return "Unknown day"
        }
    }

    fun formatPrice(price: Double): String {
        if (price != 0.0) {
            val df = DecimalFormat("#,##0.00")
            df.roundingMode = RoundingMode.HALF_UP
            val finalPrice = StringBuilder(df.format(price)).toString()
            return finalPrice.replace(".", ",")
        } else
            return "0,00"
    }

    fun calculateExtraPrice(
            selectedSize: SizeModel?,
            selectedAddons: MutableList<AddonModel>?
    ): Double? {
        var result = 0.0
        if (selectedSize != null)
            result += selectedSize!!.price.toDouble()

        if (selectedAddons != null)
            for (addOn in selectedAddons)
                result += addOn.price.toDouble()

        return result
    }


    fun setSpanString(welcome: String, name: String?, txtUser: TextView?) {
        val builder = SpannableStringBuilder()
        builder.append(welcome)
        val txtSpannable = SpannableString(name)
        val boldSpan = StyleSpan(Typeface.BOLD)
        txtSpannable.setSpan(boldSpan, 0, name!!.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.append(txtSpannable)
        txtUser?.setText(builder, TextView.BufferType.SPANNABLE)
    }

    fun createOrderNumber(): String {
        return StringBuilder()
                .append(System.currentTimeMillis())
                .append(Math.abs(Random().nextInt()))
                .toString()
    }

    fun convertStatusToText(orderStatus: Int): String {
        when (orderStatus) {
            0 -> return "Placed"
            1 -> return "Shipping"
            2 -> return "Shipped"
            -1 -> return "Cancelled"
            else -> return "Unknown"
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun showNotification(context: Context, id: Int, title: String?, content: String?, intent: Intent?) {
        Log.d("Notification", "Tittle:$title, Content:$content")
        var pendingIntent: PendingIntent? = null
        if (intent != null) {
            pendingIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            val NOTIFICATION_CHANNEL_ID = "pham.thuc.myrestaurantv2"

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Restaurant V2", NotificationManager.IMPORTANCE_DEFAULT)
            notificationChannel.description = "My Restaurant V2 Channel"
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)

            notificationManager.createNotificationChannel(notificationChannel)

            val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            builder.setContentTitle(title).setContentText(content).setAutoCancel(true)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_restaurant_24))

            if (pendingIntent != null)
                builder.setContentIntent(pendingIntent)

            val notification = builder.build()

            notificationManager.notify(id, notification)
            Log.d("Notification", "Ending notification")
        }
    }

    fun getNewOrderTopic(): String {
        return java.lang.StringBuilder("/topics/new_order").toString()
    }

    fun decodePoly(encoded: String): List<LatLng> {
        val poly: MutableList<LatLng> = ArrayList<LatLng>()
        var index = 0
        var len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0

            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0

            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }

    fun getBearing(begin: LatLng, end: LatLng): Float {
        val lat = Math.abs(begin.latitude - end.latitude)
        val lng = Math.abs(begin.longitude - end.longitude)

        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return Math.toDegrees(Math.atan(lng / lat)).toFloat()
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (90 - Math.toDegrees(Math.atan(lng / lat)) + 90).toFloat()
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return (Math.toDegrees(Math.atan(lng / lat)) + 180).toFloat()
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (90 - Math.toDegrees(Math.atan(lng / lat)) + 270).toFloat()
        return -1.0f

    }

    val REFUND_REQUEST_REF: String = "RefundRequests"
    var currentShipperOrder: ShipperOrderModel? = null
    val SHIPPING_ORDER_REF: String = "ShipperOrders"
    var currentToken: String = ""
    val NOTI_CONTENT: String = "Content"
    val NOTI_TITLE: String = "Title"
    val TOKEN_REF: String = "Tokens"
    val ORDER_REF: String = "Orders"
    val COMMENT_REF: String = "Comments"
    var foodSelected: FoodModel? = null
    var categorySelected: CategoryModel? = null
    val CATEGORY_REF: String = "Category"
    val FULL_WIDTH_COLUMN: Int = 1
    val DEFAULT_COLUMN_COUNT: Int = 1
    val BEST_DEAL_REF: String = "BestDeals"
    val POPULAR_REF: String = "MostPopular"
    var currentUser: UserModel? = null
    val USER_REF: String = "Clients"
}