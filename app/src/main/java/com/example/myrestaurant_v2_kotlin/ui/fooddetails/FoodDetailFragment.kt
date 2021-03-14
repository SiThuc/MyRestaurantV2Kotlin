package com.example.myrestaurant_v2_kotlin.ui.fooddetails

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.database.CartDataSource
import com.example.myrestaurant_v2_kotlin.database.CartDatabase
import com.example.myrestaurant_v2_kotlin.database.CartItem
import com.example.myrestaurant_v2_kotlin.database.LocalCartDataSource
import com.example.myrestaurant_v2_kotlin.databinding.*
import com.example.myrestaurant_v2_kotlin.eventbus.CountCartEvent
import com.example.myrestaurant_v2_kotlin.eventbus.MenuItemBack
import com.example.myrestaurant_v2_kotlin.model.AddonModel
import com.example.myrestaurant_v2_kotlin.model.CommentModel
import com.example.myrestaurant_v2_kotlin.model.FoodModel
import com.example.myrestaurant_v2_kotlin.ui.comment.CommentFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.*
import com.google.gson.Gson
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import java.lang.StringBuilder

class FoodDetailFragment : Fragment(), TextWatcher {

    private lateinit var viewModel: FoodDetailViewModel
    lateinit var binding: FragmentFoodDetailBinding
    private var waitingDialog: Dialog? = null
    private val compositeDisposable = CompositeDisposable()
    private lateinit var cartDataSource: CartDataSource

    //This is for adding BottomSheetDialog Addon
    private lateinit var addonBottomSheetDialog: BottomSheetDialog
    private lateinit var addonBinding: LayoutAddonDisplayBinding

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        viewModel =
                ViewModelProvider(this).get(FoodDetailViewModel::class.java)
        binding = FragmentFoodDetailBinding.inflate(inflater, container, false)

        initView(binding)

        //Observe current food detail and display
        viewModel.getFoodDetail().observe(viewLifecycleOwner, Observer {
            displayInfo(it)
        })

        //Observe the comment
        viewModel.getComment().observe(viewLifecycleOwner, Observer {
            submitRatingToFirebase(it)
        })

        return binding.root
    }

    private fun initView(binding: FragmentFoodDetailBinding) {

        (activity as AppCompatActivity).supportActionBar!!.title = Common.foodSelected!!.name
        //Initiate the cartDataSource
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(requireContext()).cartDao())

        // Initiate the addonBottomSheetDialog
        addonBottomSheetDialog = BottomSheetDialog(requireContext())
        addonBinding = LayoutAddonDisplayBinding.inflate(layoutInflater)
        addonBottomSheetDialog.setContentView(addonBinding.root)

        addonBottomSheetDialog.setOnDismissListener {
            displayUserSelectedAddon()
            calculateTotalPrice()
        }

        // Set up action Bar title
        (activity as AppCompatActivity).supportActionBar!!.title = Common.foodSelected!!.name

        //Init the waiting dialog
        waitingDialog = SpotsDialog.Builder().setContext(requireContext()).setCancelable(false).build()

        //When user click on the rating button
        binding.btnRating.setOnClickListener {
            showDialogComment()
        }

        //When user click on the show comment button
        binding.btnShowComment.setOnClickListener {
            val commentFragment = CommentFragment.newInstance()
            commentFragment.show(requireActivity().supportFragmentManager, "CommentFragment")
        }

        //When user click on add AddonButton
        binding.imgAddAddon.setOnClickListener {
            if (Common.foodSelected!!.addon != null) {
                displayAllAddon()
                addonBottomSheetDialog.show()
            }
        }

        //When user click on the cart button
        binding.btnCart.setOnClickListener {
            addCartItemIntoShoppingCart()

        }
    }

    private fun addCartItemIntoShoppingCart() {
        var cartItem = CartItem()

        cartItem.uid = Common.currentUser!!.uid
        cartItem.userPhone = Common.currentUser!!.phone

        cartItem.categoryId = Common.categorySelected!!.menu_id!!
        cartItem.foodId = Common.foodSelected!!.id!!
        cartItem.foodName = Common.foodSelected!!.name
        cartItem.foodImage = Common.foodSelected!!.image
        cartItem.foodPrice = Common.foodSelected!!.price.toDouble()
        cartItem.foodQuantity = binding.numberButton.number.toInt()
        cartItem.foodExtraPrice = Common.calculateExtraPrice(Common.foodSelected!!.userSelectedSize, Common.foodSelected!!.userSelectedAddon)
        if (Common.foodSelected!!.userSelectedAddon != null)
            cartItem.foodAddon = Gson().toJson(Common.foodSelected!!.userSelectedAddon)
        else
            cartItem.foodAddon = "Default"

        if (Common.foodSelected!!.userSelectedSize != null)
            cartItem.foodSize = Gson().toJson(Common.foodSelected!!.userSelectedSize)
        else
            cartItem.foodSize = "Default"

        cartDataSource.getItemWithAllOptionsInCart(
                Common.currentUser!!.uid,
                cartItem.categoryId,
                cartItem.foodId,
                cartItem.foodSize,
                cartItem.foodAddon
        )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<CartItem> {
                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onSuccess(t: CartItem) {
                        if (t.equals(cartItem)) {
                            //If item already in database, just update
                            t.foodExtraPrice = cartItem.foodExtraPrice
                            t.foodAddon = cartItem.foodAddon
                            t.foodSize = cartItem.foodSize
                            t.foodQuantity = t.foodQuantity!!.plus(cartItem.foodQuantity!!)

                            cartDataSource.updateCart(t)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<Int> {
                                        override fun onSubscribe(d: Disposable) {
                                        }

                                        override fun onSuccess(t: Int) {
                                            Toast.makeText(
                                                    context,
                                                    "Update Data success!",
                                                    Toast.LENGTH_SHORT
                                            ).show()
                                            EventBus.getDefault().postSticky(CountCartEvent(true))
                                        }

                                        override fun onError(e: Throwable) {
                                            Toast.makeText(
                                                    context,
                                                    "[UPDATE CART] " + e.message,
                                                    Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                        } else {
                            // If item is not avaiable in database, just insert
                            compositeDisposable.add(
                                    cartDataSource.insertOrReplaceAll(cartItem)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe({
                                                Toast.makeText(
                                                        context,
                                                        "Add to cart success",
                                                        Toast.LENGTH_SHORT
                                                ).show()
                                                EventBus.getDefault().postSticky(CountCartEvent(true))
                                            }, { t: Throwable ->
                                                Toast.makeText(
                                                        context,
                                                        "[INSERT CART] " + t.message,
                                                        Toast.LENGTH_SHORT
                                                ).show()
                                            })
                            )
                        }

                    }

                    override fun onError(e: Throwable) {
                        if (e.message!!.contains("empty")) {
                            compositeDisposable.add(
                                    cartDataSource.insertOrReplaceAll(cartItem)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe({
                                                Toast.makeText(
                                                        context,
                                                        "Add to cart Success",
                                                        Toast.LENGTH_SHORT
                                                ).show()
                                                EventBus.getDefault().postSticky(CountCartEvent(true))
                                            }, { t: Throwable ->
                                                Toast.makeText(
                                                        context,
                                                        "[INSERT CART] " + t.message,
                                                        Toast.LENGTH_SHORT
                                                ).show()
                                            })
                            )
                        } else
                            Toast.makeText(context, "[CART ERROR]!! ", Toast.LENGTH_SHORT).show()
                    }

                })
    }

    private fun submitRatingToFirebase(commentModel: CommentModel?) {
        waitingDialog!!.show()

        //Firstly, we will submit to Comment Ref
        FirebaseDatabase.getInstance().getReference(Common.COMMENT_REF)
                .child(Common.foodSelected!!.id!!)
                .push()
                .setValue(commentModel)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // We Upload the value of Rating to foodModel, then update it
                        addRatingToFood(commentModel!!.ratingValue.toDouble())
                    }
                    waitingDialog!!.dismiss()
                }
    }

    private fun addRatingToFood(ratingValue: Double) {
        FirebaseDatabase.getInstance().getReference(Common.CATEGORY_REF)
                .child(Common.categorySelected!!.menu_id!!)
                .child("foods")
                .child(Common.foodSelected!!.key!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val foodModel = snapshot.getValue(FoodModel::class.java)
                            foodModel!!.key = Common.foodSelected!!.key

                            //Apply Rating
                            val sumRating = foodModel.ratingValue + ratingValue
                            val ratingCount = foodModel.ratingCount + 1
                            val result = sumRating / ratingCount

                            val updateData = HashMap<String, Any>()
                            updateData["ratingValue"] = sumRating
                            updateData["ratingCount"] = ratingCount
                            updateData["averageRating"] = result

                            //Update data to the foodModel
                            foodModel.ratingValue = sumRating
                            foodModel.ratingCount = ratingCount
                            foodModel.averageRating = result

                            snapshot.ref
                                    .updateChildren(updateData)
                                    .addOnCompleteListener { task ->
                                        Common.foodSelected = foodModel
                                        viewModel.setFoodModel(foodModel)
                                        Toast.makeText(
                                                requireContext(),
                                                "Successfully! Thank you for commenting",
                                                Toast.LENGTH_SHORT
                                        ).show()
                                    }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), "" + error.message, Toast.LENGTH_SHORT).show()
                    }
                })
    }

    private fun displayInfo(foodModel: FoodModel?) {
        Glide.with(requireContext()).load(foodModel!!.image).into(binding.imgFoodDetail)
        binding.foodName.text = StringBuilder(foodModel.name)
        binding.foodDecripstion.text = StringBuilder(foodModel.description)
        binding.foodPrice.text = StringBuilder(foodModel.price.toString())
        binding.ratingBar.rating = foodModel.averageRating.toFloat()

        //Create RadioButtons and adding them into Radio Group Size

        for (size in foodModel.size) {
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
            val radioButton = RadioButton(context)
            radioButton.layoutParams = params
            radioButton.text = size.name
            radioButton.tag = size.price
            binding.rdiGroupSize.addView(radioButton)

            //Events
            radioButton.setOnCheckedChangeListener { cB, b ->
                if (b)
                    Common.foodSelected!!.userSelectedSize = size
                calculateTotalPrice()
            }
        }

        //Set Default first radionbutton
        if (binding.rdiGroupSize.childCount >= 0) {
            val radioButton = binding.rdiGroupSize.getChildAt(0) as RadioButton
            radioButton.isChecked = true
        }
    }

    private fun calculateTotalPrice() {
        var totalPrice = Common.foodSelected!!.price.toDouble()
        var displayPrice = totalPrice

        //Addon
        if (Common.foodSelected!!.userSelectedAddon != null && Common.foodSelected!!.userSelectedAddon!!.size > 0) {
            for (addOn in Common.foodSelected!!.userSelectedAddon!!) {
                totalPrice += addOn.price.toDouble()
            }
        }

        //Size
        totalPrice += Common.foodSelected!!.userSelectedSize!!.price.toDouble()
        displayPrice = totalPrice * binding.numberButton.number.toInt()
        displayPrice = Math.round(displayPrice * 100.0) / 100.0

        binding.foodPrice.text = StringBuilder("").append(Common.formatPrice(displayPrice)).toString()

    }

    private fun displayAllAddon() {
        if (Common.foodSelected!!.addon.isNotEmpty()) {
            addonBinding.chipGroupAddon.clearCheck()
            addonBinding.chipGroupAddon.removeAllViews()
            addonBinding.edtSearch.addTextChangedListener(this)

            //Add addOn Chip into ChipGroup
            for (addonModel in Common.foodSelected!!.addon!!) {
                val chipBinding = LayoutChipBinding.inflate(layoutInflater)
                chipBinding.chip.text = StringBuilder(addonModel.name!!).append("(+€").append(addonModel.price).append(")").toString()

                chipBinding.chip.setOnCheckedChangeListener { compoundButton, b ->
                    if (b) {
                        if (Common.foodSelected!!.userSelectedAddon == null)
                            Common.foodSelected!!.userSelectedAddon = ArrayList()
                        Toast.makeText(requireContext(), chipBinding.chip.text.toString() + " Seleted!", Toast.LENGTH_SHORT).show()
                        Common.foodSelected!!.userSelectedAddon!!.add(addonModel)
                    }
                }

                addonBinding.chipGroupAddon!!.addView(chipBinding.chip)
            }
        }
    }

    private fun displayUserSelectedAddon() {
        if (Common.foodSelected!!.userSelectedAddon != null && Common.foodSelected!!.userSelectedAddon!!.size > 0) {
            binding.chipGroupUserSelectedAddon.removeAllViews()
            for (addOn in Common.foodSelected!!.userSelectedAddon!!) {
                val chipDelBinding = LayoutChipWithDeleteBinding.inflate(layoutInflater)
                chipDelBinding.chipDel.text = StringBuilder(addOn.name)
                        .append("(€+")
                        .append(addOn.price)
                        .append(")").toString()
                chipDelBinding.chipDel.isCheckable = false

                chipDelBinding.chipDel.setOnCloseIconClickListener { view ->
                    binding.chipGroupUserSelectedAddon.removeView(view)
                    Common.foodSelected!!.userSelectedAddon!!.remove(addOn)
                    calculateTotalPrice()
                }
                binding.chipGroupUserSelectedAddon.addView(chipDelBinding.chipDel)
            }
        } else
            binding.chipGroupUserSelectedAddon.removeAllViews()
    }

    private fun showDialogComment() {
        // Show dialog comment to input rating and comments

        //Set up a builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Rating this Food")
        builder.setMessage("Please leave a comment here...")

        //rending layout
        var dialog_binding = LayoutRatingCommentBinding.inflate(LayoutInflater.from(requireContext()))

        //Set layout for builder
        builder.setView(dialog_binding.root)

        builder.setNegativeButton("CANCEL") { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() }
        builder.setPositiveButton("OK") { dialogInterface: DialogInterface, i: Int ->
            val commentModel = CommentModel()
            commentModel.name = Common.currentUser!!.name
            commentModel.uid = Common.currentUser!!.uid
            commentModel.comment = dialog_binding.edtComment.text.toString()
            commentModel.ratingValue = dialog_binding.ratingBar.rating

            val serverTimestamp = HashMap<String, Any>()
            serverTimestamp["timestamp"] = ServerValue.TIMESTAMP
            commentModel.commentTimestamp = serverTimestamp

            viewModel.setCommentModel(commentModel)
        }

        builder.create()
        builder.show()

    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        addonBinding.chipGroupAddon.clearCheck()
        addonBinding.chipGroupAddon.removeAllViews()
        for (addOn in Common.foodSelected!!.addon) {
            if (addOn.name!!.toLowerCase().contains(s.toString().toLowerCase())) {
                val chipBinding = LayoutChipBinding.inflate(layoutInflater)
                chipBinding.chip.text = StringBuilder(addOn.name!!).append("(+€").append(addOn.price).append(")").toString()

                chipBinding.chip.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        if (Common.foodSelected!!.userSelectedAddon == null)
                            Common.foodSelected!!.userSelectedAddon = ArrayList()
                        Common.foodSelected!!.userSelectedAddon!!.add(addOn)
                    }
                }
                addonBinding.chipGroupAddon.addView(chipBinding.chip)
            }
        }
    }

    override fun afterTextChanged(s: Editable?) {

    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }
}