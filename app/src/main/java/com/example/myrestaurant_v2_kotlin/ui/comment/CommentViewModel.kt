package com.example.myrestaurant_v2_kotlin.ui.comment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myrestaurant_v2_kotlin.model.CommentModel

class CommentViewModel : ViewModel() {
    private var listCommentLiveData: MutableLiveData<List<CommentModel>>?= null
    init {
        listCommentLiveData = MutableLiveData()
    }

    fun getCommentList(): MutableLiveData<List<CommentModel>>{
        return listCommentLiveData!!
    }

    fun setCommentList(listComments: List<CommentModel>){
        listCommentLiveData!!.value = listComments
    }
}