package com.example.myrestaurant_v2_kotlin.ui.cart

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.database.CartDatabase
import com.example.myrestaurant_v2_kotlin.database.CartItem
import com.example.myrestaurant_v2_kotlin.database.LocalCartDataSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class CartViewModel : ViewModel() {
    private var compositeDisposable: CompositeDisposable = CompositeDisposable()
    private lateinit var cartDataSource: LocalCartDataSource
    private var cartMutableListLiveData: MutableLiveData<List<CartItem>>? = null

    fun initCartDataSource(context: Context) {
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context).cartDao())
    }

    fun getCartMutableListLiveData(): MutableLiveData<List<CartItem>> {
        if (cartMutableListLiveData == null)
            cartMutableListLiveData = MutableLiveData()
        getCartItems()
        return cartMutableListLiveData!!
    }

    private fun getCartItems() {
        compositeDisposable.addAll(
                cartDataSource!!.getAllCart(Common.currentUser!!.uid!!)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ cartItems ->
                            cartMutableListLiveData!!.value = cartItems
                        }, { t: Throwable? -> cartMutableListLiveData!!.value == null })
        )
    }

    fun onStop() {
        compositeDisposable.clear()
    }
}