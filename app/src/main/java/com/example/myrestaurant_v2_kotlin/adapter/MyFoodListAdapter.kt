package com.example.myrestaurant_v2_kotlin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myrestaurant_v2_kotlin.callback.IRecyclerItemClickListener
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.database.CartDataSource
import com.example.myrestaurant_v2_kotlin.database.CartDatabase
import com.example.myrestaurant_v2_kotlin.database.CartItem
import com.example.myrestaurant_v2_kotlin.database.LocalCartDataSource
import com.example.myrestaurant_v2_kotlin.databinding.LayoutFoodlistItemBinding
import com.example.myrestaurant_v2_kotlin.eventbus.CountCartEvent
import com.example.myrestaurant_v2_kotlin.eventbus.FoodItemClick
import com.example.myrestaurant_v2_kotlin.model.FoodModel
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus

class MyFoodListAdapter(
    var context: Context,
    var foodList: List<FoodModel>
) :
    RecyclerView.Adapter<MyFoodListAdapter.MyViewHolder>() {

    lateinit var binding: LayoutFoodlistItemBinding
    private val compositeDisposable: CompositeDisposable
    private val cartDataSource: CartDataSource

    init {
        compositeDisposable = CompositeDisposable()
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context).cartDao())
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        private var listener: IRecyclerItemClickListener? = null

        fun setListener(listener: IRecyclerItemClickListener){
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
        binding = LayoutFoodlistItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        with(holder) {
            Glide.with(context).load(foodList.get(position).image)
                .into(binding.imgFood)
            binding.txtFoodName.text = foodList.get(position).name
            binding.txtFoodPrice.text = "€ " + foodList.get(position).price.toString()

            setListener(object : IRecyclerItemClickListener{
                override fun onItemClick(view: View, pos: Int) {
                    Common.foodSelected = foodList[pos]
                    Common.foodSelected!!.key = pos.toString()
                    EventBus.getDefault().postSticky(FoodItemClick(true, foodList[pos]))
                }
            })

            //Adding event when user click on CartImage to add cartitem
            binding.imgAddCart.setOnClickListener {
                var cartItem = CartItem()

                cartItem.uid = Common.currentUser!!.uid.toString()
                cartItem.userPhone = Common.currentUser!!.phone

                cartItem.foodId = foodList[position].id!!
                cartItem.foodName = foodList[position].name
                cartItem.foodImage = foodList[position].image
                cartItem.foodPrice = foodList[position].price.toDouble()
                cartItem.foodQuantity = 1
                cartItem.foodExtraPrice = 0.0
                cartItem.foodAddon = "Default"
                cartItem.foodSize = "Default"

                cartDataSource.getItemWithAllOptionsInCart(
                    Common.currentUser!!.uid,
                    cartItem.foodId,
                    cartItem.foodSize,
                    cartItem.foodAddon
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : SingleObserver<CartItem> {
                        override fun onSubscribe(d: Disposable) {

                        }

                        override fun onSuccess(t: CartItem) {
                            if (t.equals(cartItem)) {
                                //If item already in database, just update
                                t.foodExtraPrice = cartItem.foodExtraPrice
                                t.foodAddon = cartItem.foodAddon
                                t.foodSize = cartItem.foodSize
                                t.foodQuantity = t.foodQuantity!!.plus(cartItem.foodQuantity!!)

                                cartDataSource.updateCart(t)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<Int> {
                                        override fun onSubscribe(d: Disposable) {
                                        }

                                        override fun onSuccess(t: Int) {
                                            Toast.makeText(
                                                context,
                                                "Update Data success!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            EventBus.getDefault().postSticky(CountCartEvent(true))
                                        }

                                        override fun onError(e: Throwable) {
                                            Toast.makeText(
                                                context,
                                                "[UPDATE CART] " + e.message,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                            } else {
                                // If item is not avaiable in database, just insert
                                compositeDisposable.add(
                                    cartDataSource.insertOrReplaceAll(cartItem)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe({
                                            Toast.makeText(
                                                context,
                                                "Add to cart success",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            EventBus.getDefault().postSticky(CountCartEvent(true))
                                        }, { t: Throwable ->
                                            Toast.makeText(
                                                context,
                                                "[INSERT CART] " + t.message,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        })
                                )
                            }

                        }

                        override fun onError(e: Throwable) {
                            if (e.message!!.contains("empty")) {
                                compositeDisposable.add(
                                    cartDataSource.insertOrReplaceAll(cartItem)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe({
                                            Toast.makeText(
                                                context,
                                                "Add to cart Success",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            EventBus.getDefault().postSticky(CountCartEvent(true))
                                        }, { t: Throwable ->
                                            Toast.makeText(
                                                context,
                                                "[INSERT CART] " + t.message,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        })
                                )
                            } else
                                Toast.makeText(context, "[CART ERROR]!! ", Toast.LENGTH_SHORT).show()
                        }

                    })
            }
        }
    }

    override fun getItemCount(): Int = foodList.size

    fun onStop() {
        if (compositeDisposable != null)
            compositeDisposable.clear()
    }
}

