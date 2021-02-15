package com.example.myrestaurant_v2_kotlin.ui.foodlist

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myrestaurant_v2_kotlin.R
import com.example.myrestaurant_v2_kotlin.adapter.MyCategoriesAdapter
import com.example.myrestaurant_v2_kotlin.adapter.MyFoodListAdapter
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.databinding.FragmentCategoryBinding
import com.example.myrestaurant_v2_kotlin.databinding.FragmentFoodlistBinding
import com.example.myrestaurant_v2_kotlin.eventbus.MenuItemBack
import org.greenrobot.eventbus.EventBus

class FoodListFragment : Fragment() {

    private lateinit var viewModel: FoodListViewModel

    private lateinit var binding: FragmentFoodlistBinding
    private var adapter: MyFoodListAdapter? = null
    private var layoutAnimationController: LayoutAnimationController? = null

    override fun onStop() {
        if(adapter != null)
            adapter!!.onStop()
        super.onStop()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewModel = ViewModelProvider(this).get(FoodListViewModel::class.java)

        binding = FragmentFoodlistBinding.inflate(inflater, container, false)
        initView(binding)

        viewModel.getFoodList().observe(viewLifecycleOwner, Observer {
            adapter = MyFoodListAdapter(requireContext(), it)
            binding.recyclerFoodlist.adapter = adapter
            binding.recyclerFoodlist.layoutAnimation = layoutAnimationController
        })
        return(binding.root)
    }

    private fun initView(binding: FragmentFoodlistBinding) {
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)
        binding.recyclerFoodlist.setHasFixedSize(true)
        binding.recyclerFoodlist.layoutManager = LinearLayoutManager(context)

        (activity as AppCompatActivity).supportActionBar!!.title = Common.categorySelected!!.name
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }

}