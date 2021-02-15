package com.example.myrestaurant_v2_kotlin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.asksira.loopingviewpager.LoopingPagerAdapter
import com.bumptech.glide.Glide
import com.example.myrestaurant_v2_kotlin.databinding.LayoutBestDealItemBinding
import com.example.myrestaurant_v2_kotlin.eventbus.BestDealItemClick
import com.example.myrestaurant_v2_kotlin.model.BestDealModel
import org.greenrobot.eventbus.EventBus

class MyBestDealsAdapter(context: Context,
                        itemList:List<BestDealModel>,
                        isInfinite: Boolean): LoopingPagerAdapter<BestDealModel>(context, itemList, isInfinite) {
    private lateinit var binding: LayoutBestDealItemBinding

    override fun bindView(convertView: View, pos: Int, viewType: Int) {
        // Set data
        Glide.with(context).load(itemList!![pos].image).into(binding.imgBestDeal)
        binding.txtBestDeal.text = itemList!![pos].name

        convertView.setOnClickListener {
            EventBus.getDefault().postSticky(BestDealItemClick(itemList!![pos]))
        }
    }

    override fun inflateView(viewType: Int, container: ViewGroup, listPosition: Int): View {
        binding = LayoutBestDealItemBinding.inflate(LayoutInflater.from(context),container, false)

        return binding.root
    }

}