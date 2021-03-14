package com.example.myrestaurant_v2_kotlin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.example.myrestaurant_v2_kotlin.R
import com.example.myrestaurant_v2_kotlin.callback.ISearchCategoryCallbackListener
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.database.CartDataSource
import com.example.myrestaurant_v2_kotlin.database.CartDatabase
import com.example.myrestaurant_v2_kotlin.database.CartItem
import com.example.myrestaurant_v2_kotlin.database.LocalCartDataSource
import com.example.myrestaurant_v2_kotlin.databinding.LayoutCartItemBinding
import com.example.myrestaurant_v2_kotlin.eventbus.CountCartEvent
import com.example.myrestaurant_v2_kotlin.eventbus.UpdateAddonSizeEvent
import com.example.myrestaurant_v2_kotlin.eventbus.UpdateItemInCart
import com.example.myrestaurant_v2_kotlin.model.AddonModel
import com.example.myrestaurant_v2_kotlin.model.CategoryModel
import com.example.myrestaurant_v2_kotlin.model.FoodModel
import com.example.myrestaurant_v2_kotlin.model.SizeModel
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus


class MyCartAdapter(
        var context: Context,
        var cartList: List<CartItem>,
        var cartDataSource: CartDataSource
) : RecyclerView.Adapter<MyCartAdapter.MyViewHolder>(){
    lateinit var binding: LayoutCartItemBinding

    private val viewBinderHelper = ViewBinderHelper()
    internal var compositeDisposable:CompositeDisposable
    val gson: Gson


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    init {
        viewBinderHelper.setOpenOnlyOne(true)
        compositeDisposable = CompositeDisposable()
        gson = Gson()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        binding = LayoutCartItemBinding.inflate(LayoutInflater.from(context), parent, false)

        return MyViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        with(holder) {

            viewBinderHelper.bind(binding.swipeLayout, cartList[position].foodId)
            Glide.with(context).load(cartList[position].foodImage).into(binding.imgFood)
            binding.txtFoodName.text = cartList[position].foodName
            binding.txtFoodPrice.text = StringBuilder("€").append(
                    cartList[position].foodPrice!! + cartList[position].foodExtraPrice!!
            )

            //Size
            if(cartList[position].foodSize != null){
                if(cartList[position].foodSize == "Default")
                    binding.txtFoodSize.text = StringBuilder("Size: Default")
                else{
                    val sizeModel = gson.fromJson<SizeModel>(cartList[position].foodSize, object : TypeToken<SizeModel>(){}.type)
                    binding.txtFoodSize.text = StringBuilder("Size:").append(sizeModel.name)
                }
            }

            //Addon
            if(cartList[position].foodAddon != null){
                if(cartList[position].foodAddon == "Default")
                    binding.txtFoodAddon.text = StringBuilder("Addon: Default")
                else{
                    val addonModels = gson.fromJson<List<AddonModel>>(cartList[position].foodAddon, object : TypeToken<List<AddonModel>>(){}.type)
                    binding.txtFoodAddon.text = StringBuilder("Addon:").append(Common.getListAddon(addonModels))
                }
            }


            binding.btnNumFood.number = cartList[position].foodQuantity.toString()

            // When user press Elegant Number Button to increase or decrease the number of cartItem
            binding.btnNumFood.setOnValueChangeListener { view, oldValue, newValue ->
                //When user increase the number of cart down to 0, it will delete the item
                if (newValue == 0)
                    deleteCartItem(cartList[position])
                else {
                    cartList[position].foodQuantity = newValue
                }

                EventBus.getDefault().postSticky(UpdateItemInCart(cartList[position]))
                EventBus.getDefault().postSticky(CountCartEvent(true))

            }

            //When user click on Delete button
            binding.btnDelete.setOnClickListener {
                deleteCartItem(cartList[position])
            }

            //When user click on Update button
            binding.btnUpdate.setOnClickListener {
                EventBus.getDefault().postSticky(UpdateAddonSizeEvent(position))
            }
        }
    }

    private fun deleteCartItem(cartItem: CartItem) {
        //Delete on database
        cartDataSource.deleteCart(cartItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<Int> {
                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onSuccess(t: Int) {
                        EventBus.getDefault().postSticky(UpdateItemInCart(cartItem))
                        EventBus.getDefault().postSticky(CountCartEvent(true))
                        Toast.makeText(context, "Delete Item success", Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
                    }
                })
    }

    override fun getItemCount(): Int = cartList.size

    fun getItemAtPosition(position: Int): CartItem {
        return cartList[position]
    }
}