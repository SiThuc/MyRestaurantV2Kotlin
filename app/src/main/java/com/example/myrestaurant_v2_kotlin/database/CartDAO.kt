package com.example.myrestaurant_v2_kotlin.database

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface CartDAO {
    //Select a cart with uid
    @Query("Select * from Cart where uid=:uid")
    fun getAllCart(uid: String): Flowable<List<CartItem>>

    @Query("Select SUM(foodQuantity) from Cart where uid=:uid")
    fun countItemInCart(uid: String): Single<Int>

    @Query("Select SUM((foodPrice+foodExtraPrice)*foodQuantity) from Cart where uid=:uid")
    fun sumPrice(uid: String): Single<Double>

    @Query("Select * from Cart where foodId=:foodId AND uid=:uid")
    fun getItemInCart(foodId: String, uid: String): Single<CartItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplaceAll(vararg cartItems: CartItem): Completable

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateCart(cart: CartItem): Single<Int>

    @Delete
    fun deleteCart(cart: CartItem): Single<Int>

    @Query("Delete from Cart where uid=:uid")
    fun cleanCart(uid: String): Single<Int>

    @Query("select * from Cart where categoryId=:categoryId AND foodId =:foodId AND uid=:uid AND foodSize=:foodSize AND foodAddon=:foodAddon")
    fun getItemWithAllOptionsInCart(
            uid: String,
            categoryId: String,
            foodId: String,
            foodSize: String,
            foodAddon: String,
    ): Single<CartItem>
}