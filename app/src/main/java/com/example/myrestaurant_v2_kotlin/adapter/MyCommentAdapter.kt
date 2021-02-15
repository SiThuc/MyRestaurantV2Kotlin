package com.example.myrestaurant_v2_kotlin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myrestaurant_v2_kotlin.databinding.LayoutCommentItemBinding
import com.example.myrestaurant_v2_kotlin.model.CommentModel

class MyCommentAdapter(var context: Context, var listComment: List<CommentModel>): RecyclerView.Adapter<MyCommentAdapter.MyViewHolder>() {

    lateinit var binding: LayoutCommentItemBinding

    inner class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        binding =  LayoutCommentItemBinding.inflate(LayoutInflater.from(context))
        return MyViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        with(holder){
            binding.txtUsername.text = listComment[position].name
            binding.txtCommentDate.text = listComment[position].commentTimestamp.toString()
            binding.txtComment.text = listComment[position].comment
            binding.ratingBarCm.rating = listComment[position].ratingValue
        }
    }

    override fun getItemCount(): Int = listComment.size
}