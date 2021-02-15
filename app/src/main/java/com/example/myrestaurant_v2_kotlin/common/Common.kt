package com.example.myrestaurant_v2_kotlin.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.myrestaurant_v2_kotlin.R
import com.example.myrestaurant_v2_kotlin.model.*
import com.google.firebase.database.FirebaseDatabase
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

object Common {
    fun updateToken(context: Context, token:String){
        FirebaseDatabase.getInstance()
            .getReference(TOKEN_REF)
            .child(currentUser!!.uid)
            .setValue(TokenModel(currentUser!!.phone!!, token))
            .addOnFailureListener { e -> Toast.makeText(context, ""+e.message, Toast.LENGTH_SHORT).show() }
    }

    fun getDateOfWeek(i: Int):String{
        when(i){
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
        when(orderStatus){
            0 -> return "Placed"
            1 -> return "Shipping"
            2 -> return "Shipped"
            -1 -> return "Cancelled"
            else -> return "Unknown"
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun showNotification(context: Context, id: Int, title: String?, content: String?, intent: Intent?) {
        var pendingIntent: PendingIntent?= null
        if(intent != null){
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

            if(pendingIntent != null)
                builder.setContentIntent(pendingIntent)

            val notification = builder.build()

            notificationManager.notify(id, notification)
        }


    }


    var currentToken: String? = null
    val NOTI_CONTENT: String? = "Content"
    val NOTI_TITLE: String? = "Title"
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