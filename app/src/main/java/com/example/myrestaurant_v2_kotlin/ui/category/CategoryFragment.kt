package com.example.myrestaurant_v2_kotlin.ui.category

import android.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myrestaurant_v2_kotlin.R
import com.example.myrestaurant_v2_kotlin.adapter.MyCategoriesAdapter
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.common.SpacesDecoration
import com.example.myrestaurant_v2_kotlin.databinding.FragmentCategoryBinding
import com.example.myrestaurant_v2_kotlin.eventbus.MenuItemBack
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus

class CategoryFragment : Fragment() {
    private lateinit var viewModel: CategoryViewModel

    private lateinit var binding: FragmentCategoryBinding

    private lateinit var dialog: AlertDialog
    private var layoutAnimationController: LayoutAnimationController? = null
    private var adapter: MyCategoriesAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentCategoryBinding.inflate(inflater,container, false)
        initView(binding)

        return binding.root
    }

    private fun initView(binding: FragmentCategoryBinding) {
        dialog = SpotsDialog.Builder().setContext(context)
            .setCancelable(false)
            .build()

        layoutAnimationController =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)

        binding.recyclerMenu.setHasFixedSize(true)

        val layoutManager = GridLayoutManager(context, 2)
        layoutManager.orientation = RecyclerView.VERTICAL
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {

                return if (adapter != null) {
                    when (adapter!!.getItemViewType(position)) {
                        Common.DEFAULT_COLUMN_COUNT -> 1
                        Common.FULL_WIDTH_COLUMN -> 2
                        else -> -1
                    }
                } else
                    -1
            }
        }
        binding.recyclerMenu.layoutManager = layoutManager
        binding.recyclerMenu.addItemDecoration(SpacesDecoration(8))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(CategoryViewModel::class.java)

        viewModel.getMessageError().observe(viewLifecycleOwner, Observer {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        })

        viewModel.getCategoryList().observe(viewLifecycleOwner, Observer {
            dialog.dismiss()
            adapter = MyCategoriesAdapter(requireContext(), it)

            binding.recyclerMenu.adapter = adapter
            binding.recyclerMenu.layoutAnimation = layoutAnimationController
        })
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }


}