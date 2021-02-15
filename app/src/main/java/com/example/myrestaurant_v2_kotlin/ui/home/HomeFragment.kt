package com.example.myrestaurant_v2_kotlin.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myrestaurant_v2_kotlin.R
import com.example.myrestaurant_v2_kotlin.adapter.MyBestDealsAdapter
import com.example.myrestaurant_v2_kotlin.adapter.MyPopularCategoriesAdapter
import com.example.myrestaurant_v2_kotlin.databinding.FragmentHomeBinding
import com.example.myrestaurant_v2_kotlin.eventbus.MenuItemBack
import org.greenrobot.eventbus.EventBus

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var homeViewModel: HomeViewModel
    var layoutAnimationController: LayoutAnimationController? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        initView(binding)

        // Binding data
        //For popular category
        homeViewModel.popularList.observe(viewLifecycleOwner, Observer {
            val adapter = MyPopularCategoriesAdapter(requireContext(), it)
            binding.recyclerPopular.adapter = adapter
            binding.recyclerPopular.layoutAnimation = layoutAnimationController
        })

        //For Best deals
        homeViewModel.bestDealList.observe(viewLifecycleOwner, Observer {
            val adapter = MyBestDealsAdapter(requireContext(), it, false)
            binding.viewpager.adapter = adapter
        })
        return binding.root
    }

    private fun initView(binding: FragmentHomeBinding) {
        layoutAnimationController =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)
        binding.recyclerPopular.setHasFixedSize(true)
        binding.recyclerPopular.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
    }

    override fun onResume() {
        super.onResume()
        binding.viewpager.resumeAutoScroll()
    }

    override fun onPause() {
        binding.viewpager.pauseAutoScroll()
        super.onPause()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }
}