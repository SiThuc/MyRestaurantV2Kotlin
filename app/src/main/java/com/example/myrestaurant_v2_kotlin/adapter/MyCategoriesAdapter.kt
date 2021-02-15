package com.example.myrestaurant_v2_kotlin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myrestaurant_v2_kotlin.callback.IRecyclerItemClickListener
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.databinding.LayoutCategoryItemBinding
import com.example.myrestaurant_v2_kotlin.eventbus.CategoryClick
import com.example.myrestaurant_v2_kotlin.model.CategoryModel
import org.greenrobot.eventbus.EventBus

class MyCategoriesAdapter(
    var context: Context,
    var categoryList: List<CategoryModel>
) : RecyclerView.Adapter<MyCategoriesAdapter.MyViewHolder>() {

    lateinit var binding: LayoutCategoryItemBinding


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
        binding =
            LayoutCategoryItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        with(holder){
            with(categoryList[position]){
                Glide.with(context).load(this.image).into(binding.categoryImage)
                binding.categoryName.text = this.name

                setListener(object : IRecyclerItemClickListener{
                    override fun onItemClick(view: View, pos: Int) {
                        Common.categorySelected = categoryList[pos]
                        EventBus.getDefault().postSticky(CategoryClick(true, categoryList[pos]))
                    }
                })
            }
        }
    }

    override fun getItemCount(): Int = categoryList.size

    override fun getItemViewType(position: Int): Int {
        return if (categoryList.size == 1)
            Common.DEFAULT_COLUMN_COUNT
        else {
            if (categoryList.size % 2 == 0)
                Common.DEFAULT_COLUMN_COUNT
            else
                if (position > 1 && position == categoryList.size - 1)
                    Common.FULL_WIDTH_COLUMN
                else
                    Common.DEFAULT_COLUMN_COUNT
        }
    }
}