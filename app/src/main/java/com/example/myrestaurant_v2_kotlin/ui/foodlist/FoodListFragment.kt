package com.example.myrestaurant_v2_kotlin.ui.foodlist

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
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
import com.example.myrestaurant_v2_kotlin.model.CategoryModel
import com.example.myrestaurant_v2_kotlin.model.FoodModel
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
            if(it != null){
                adapter = MyFoodListAdapter(requireContext(), it)
                binding.recyclerFoodlist.adapter = adapter
                binding.recyclerFoodlist.layoutAnimation = layoutAnimationController
            }
        })
        return(binding.root)
    }

    private fun initView(binding: FragmentFoodlistBinding) {
        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar!!.title = Common.categorySelected!!.name

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)
        binding.recyclerFoodlist.setHasFixedSize(true)
        binding.recyclerFoodlist.layoutManager = LinearLayoutManager(context)

        (activity as AppCompatActivity).supportActionBar!!.title = Common.categorySelected!!.name
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        //super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_menu, menu)
        val menuItem = menu.findItem(R.id.action_search)

        val searchManager = requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menuItem.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))

        //Event
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                startSearch(query!!)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        //Clear Text when user click to clear Button on search view
        val closeButton = searchView.findViewById<View>(R.id.search_close_btn) as ImageView
        closeButton.setOnClickListener {
            val ed = searchView.findViewById<View>(R.id.search_src_text) as EditText
            //Clear Text
            ed.setText("")
            //Clear query
            searchView.setQuery("", false)
            //Collapse the action View
            searchView.onActionViewCollapsed()
            //Collapse the search widget
            menuItem.collapseActionView()
            //Restore result to original
            viewModel.getFoodList()

        }
    }

    private fun startSearch(query: String) {
        var resultFoods = ArrayList<FoodModel>()
        for (item in adapter!!.getFoodList()) {
            if (item.name!!.toLowerCase().contains(query))
                resultFoods.add(item)
        }
        viewModel.foodListLiveData!!.value = resultFoods
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }

}