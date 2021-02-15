package com.example.myrestaurant_v2_kotlin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myrestaurant_v2_kotlin.callback.IRecyclerItemClickListener
import com.example.myrestaurant_v2_kotlin.databinding.LayoutPopularCategoriesItemBinding
import com.example.myrestaurant_v2_kotlin.eventbus.PopularCategoryItemClick
import com.example.myrestaurant_v2_kotlin.model.PopularCategoryModel
import org.greenrobot.eventbus.EventBus

class MyPopularCategoriesAdapter(
    var context: Context,
    var popularCategoryModels: List<PopularCategoryModel>
) :
    RecyclerView.Adapter<MyPopularCategoriesAdapter.MyViewHolder>() {

    lateinit var binding: LayoutPopularCategoriesItemBinding



    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        internal var listener: IRecyclerItemClickListener? = null

        fun setListener(listener: IRecyclerItemClickListener){
            this.listener = listener
        }

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            listener!!.onItemClick(v!!,adapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        binding =
            LayoutPopularCategoriesItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        with(holder) {
            with(popularCategoryModels[position]) {
                Glide.with(context).load(this.image).into(binding.categoryImage)
                binding.txtCategoryName.text = this.name

                setListener(object : IRecyclerItemClickListener{
                    override fun onItemClick(view: View, pos: Int) {
                        EventBus.getDefault()
                            .postSticky(PopularCategoryItemClick(popularCategoryModels[pos]))
                    }
                })
            }
        }
    }

    override fun getItemCount(): Int = popularCategoryModels.size
}