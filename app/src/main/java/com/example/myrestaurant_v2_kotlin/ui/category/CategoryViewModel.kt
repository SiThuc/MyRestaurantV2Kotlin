package com.example.myrestaurant_v2_kotlin.ui.category

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myrestaurant_v2_kotlin.callback.ICategoryCallBackListener
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.model.CategoryModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CategoryViewModel : ViewModel(), ICategoryCallBackListener {
    var categoryListMutable: MutableLiveData<List<CategoryModel>>? = null
    private var messageError: MutableLiveData<String> = MutableLiveData()

    private var categoryCallBackListener: ICategoryCallBackListener = this


    fun getCategoryList(): LiveData<List<CategoryModel>> {
        if (categoryListMutable == null) {
            categoryListMutable = MutableLiveData()
            messageError = MutableLiveData()
            loadCategoryList()
        }
        return categoryListMutable!!
    }

    fun getMessageError(): MutableLiveData<String> {
        return messageError!!
    }

    fun loadCategoryList() {
        var tempList = ArrayList<CategoryModel>()

        var categoryRef = FirebaseDatabase.getInstance().getReference(Common.CATEGORY_REF)

        categoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
           override fun onDataChange(snapshot: DataSnapshot) {
              for (item in snapshot.children) {
                 val model = item.getValue<CategoryModel>(CategoryModel::class.java)
                 model!!.menu_id = item.key
                 tempList.add(model)
              }
              categoryCallBackListener.onCategoryLoadSuccess(tempList)
           }

           override fun onCancelled(error: DatabaseError) {
              categoryCallBackListener.onCategoryLoadFailed(error.message)
           }
        })
    }

    override fun onCategoryLoadSuccess(categoryList: List<CategoryModel>) {
        categoryListMutable!!.value = categoryList
    }

    override fun onCategoryLoadFailed(message: String) {
        messageError!!.value = message
    }
}