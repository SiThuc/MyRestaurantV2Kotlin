package com.example.myrestaurant_v2_kotlin.callback

import com.example.myrestaurant_v2_kotlin.model.CommentModel

interface ICommentCallBackListener {
    fun onLoadCommentSuccess(commentList: List<CommentModel>)
    fun onCommentLoadFailed(message: String)
}