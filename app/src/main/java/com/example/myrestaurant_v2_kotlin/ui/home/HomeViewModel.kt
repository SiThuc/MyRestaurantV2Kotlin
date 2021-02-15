package com.example.myrestaurant_v2_kotlin.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myrestaurant_v2_kotlin.callback.IBestDealLoadCallback
import com.example.myrestaurant_v2_kotlin.callback.IPopularLoadCallBack
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.model.BestDealModel
import com.example.myrestaurant_v2_kotlin.model.PopularCategoryModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeViewModel : ViewModel(), IPopularLoadCallBack, IBestDealLoadCallback {
    private var popularListMutableLiveData: MutableLiveData<List<PopularCategoryModel>>? = null
    private var bestDealMutableLiveData: MutableLiveData<List<BestDealModel>>? = null

    private lateinit var messageError: MutableLiveData<String>

    private var popularLoadCallBackListener: IPopularLoadCallBack
    private var bestDealCallbackListener: IBestDealLoadCallback

    val popularList: LiveData<List<PopularCategoryModel>>
        get() {
            if(popularListMutableLiveData == null){
                popularListMutableLiveData = MutableLiveData()
                messageError = MutableLiveData()
                loadPopularList()
            }
            return popularListMutableLiveData!!
        }


    val bestDealList: LiveData<List<BestDealModel>>
        get() {
            if(bestDealMutableLiveData == null) {
                bestDealMutableLiveData = MutableLiveData()
                messageError = MutableLiveData()
                loadBestDealList()
            }
            return bestDealMutableLiveData!!

        }

    private fun loadBestDealList() {
        val tempList = ArrayList<BestDealModel>()
        val bestDealRef = FirebaseDatabase.getInstance().getReference(Common.BEST_DEAL_REF)

        bestDealRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for(itemSnapShot in snapshot.children){
                    val model = itemSnapShot.getValue<BestDealModel>(BestDealModel::class.java)
                    tempList.add(model!!)
                }
                bestDealCallbackListener.onBestDealLoadSuccess(tempList)
            }

            override fun onCancelled(error: DatabaseError) {
                bestDealCallbackListener.onBestDealLoadFailed(error.message)
            }
        })
    }

    init {
        popularLoadCallBackListener = this
        bestDealCallbackListener = this
    }

    private fun loadPopularList() {
        val tempList = ArrayList<PopularCategoryModel>()
        val popularRef = FirebaseDatabase.getInstance().getReference(Common.POPULAR_REF)
        popularRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for(itemSnapShot in snapshot.children){
                    val model = itemSnapShot.getValue<PopularCategoryModel>(PopularCategoryModel::class.java)

                    tempList.add(model!!)
                }
                popularLoadCallBackListener.onPopularLoadSuccess(tempList)
            }

            override fun onCancelled(error: DatabaseError) {
                popularLoadCallBackListener.onPopularLoadFailed(error.message)
            }

        })
    }

    override fun onPopularLoadSuccess(popularCategoryList: List<PopularCategoryModel>) {
        popularListMutableLiveData!!.value = popularCategoryList
    }

    override fun onPopularLoadFailed(message: String) {
        messageError.value = message
    }

    override fun onBestDealLoadSuccess(bestDealList: List<BestDealModel>) {
        bestDealMutableLiveData!!.value = bestDealList
    }

    override fun onBestDealLoadFailed(message: String) {
        messageError.value = message
    }


}