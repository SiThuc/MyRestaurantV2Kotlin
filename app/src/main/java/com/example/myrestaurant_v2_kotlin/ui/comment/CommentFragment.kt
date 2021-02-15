package com.example.myrestaurant_v2_kotlin.ui.comment

import android.app.AlertDialog
import android.app.Dialog
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myrestaurant_v2_kotlin.R
import com.example.myrestaurant_v2_kotlin.adapter.MyCommentAdapter
import com.example.myrestaurant_v2_kotlin.callback.ICommentCallBackListener
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.databinding.FragmentCommentBinding
import com.example.myrestaurant_v2_kotlin.eventbus.MenuItemBack
import com.example.myrestaurant_v2_kotlin.model.CommentModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus

class CommentFragment : BottomSheetDialogFragment(), ICommentCallBackListener {

    companion object {
        private var instance: CommentFragment? = null

        fun newInstance(): CommentFragment {
            if (instance == null)
                instance = CommentFragment()
            return instance!!
        }

    }


    lateinit var binding: FragmentCommentBinding
    private var viewModel: CommentViewModel? = null
    private var listener: ICommentCallBackListener? = null
    private lateinit var adapter: MyCommentAdapter

    private lateinit var dialog: AlertDialog

    init {
        listener = this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentCommentBinding.inflate(inflater, container, false)

        initViews(binding)

        loadCommentFromFirebase()

        //Observe data from viewmodel into fragment
        viewModel!!.getCommentList().observe(viewLifecycleOwner, Observer {
            adapter = MyCommentAdapter(requireContext(), it)
            binding.recyclerComment.adapter = adapter
        })

        return binding.root
    }

    private fun loadCommentFromFirebase() {
        dialog.show()
        val commentList = ArrayList<CommentModel>()

        FirebaseDatabase.getInstance().getReference(Common.COMMENT_REF)
                .child(Common.foodSelected!!.id!!)
                .orderByChild("commentTimeStamp")
                .limitToLast(100)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (item in snapshot.children) {
                            val comment = item.getValue(CommentModel::class.java)
                            commentList.add(comment!!)
                        }
                        listener!!.onLoadCommentSuccess(commentList)
                        dialog.dismiss()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        listener!!.onCommentLoadFailed(error.message)
                        dialog.dismiss()
                    }
                })
    }

    private fun initViews(binding: FragmentCommentBinding) {
        viewModel = ViewModelProvider(this).get(CommentViewModel::class.java)
        dialog = SpotsDialog.Builder().setContext(requireContext()).setCancelable(false).build()

        //Setup recyclerView
        binding.recyclerComment.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
        binding.recyclerComment.layoutManager = layoutManager
        binding.recyclerComment.addItemDecoration(
                DividerItemDecoration(requireContext(), layoutManager.orientation)
        )
    }

    override fun onLoadCommentSuccess(commentList: List<CommentModel>) {
        viewModel!!.setCommentList(commentList)
    }

    override fun onCommentLoadFailed(message: String) {
        Toast.makeText(requireContext(), "" + message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }

}