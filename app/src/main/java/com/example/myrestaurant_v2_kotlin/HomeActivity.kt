package com.example.myrestaurant_v2_kotlin

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.andremion.counterfab.CounterFab
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.database.CartDataSource
import com.example.myrestaurant_v2_kotlin.database.CartDatabase
import com.example.myrestaurant_v2_kotlin.database.LocalCartDataSource
import com.example.myrestaurant_v2_kotlin.databinding.ActivityHomeBinding
import com.example.myrestaurant_v2_kotlin.databinding.AppBarMainBinding
import com.example.myrestaurant_v2_kotlin.eventbus.*
import com.example.myrestaurant_v2_kotlin.model.BestDealModel
import com.example.myrestaurant_v2_kotlin.model.CategoryModel
import com.example.myrestaurant_v2_kotlin.model.FoodModel
import com.example.myrestaurant_v2_kotlin.model.PopularCategoryModel
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class HomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityHomeBinding
    private lateinit var cartDataSource: CartDataSource
    private lateinit var counterFab: CounterFab

    private lateinit var drawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    private var menuItemClick = -1

    private var dialog: AlertDialog? = null

    override fun onResume() {
        super.onResume()
        countCartItem()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()

        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(this).cartDao())

        counterFab = findViewById(R.id.counterFab)
        counterFab.setOnClickListener {
            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_cart)
        }


        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawer = binding.drawerLayout
        toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)

        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_home, R.id.nav_category, R.id.nav_food_list, R.id.nav_cart),
            drawer)

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        var headerView = navView.getHeaderView(0)
        var txtUser = headerView.findViewById<TextView>(R.id.txt_username)
        Common.setSpanString("Hey", Common.currentUser!!.name, txtUser)

        navView.setNavigationItemSelectedListener(object :
            NavigationView.OnNavigationItemSelectedListener {
            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                item.isChecked = true
                drawer!!.closeDrawers()

                if (item.itemId == R.id.nav_sign_out) {
                    signOut()
                } else if (item.itemId == R.id.nav_home) {
                    if (menuItemClick != item.itemId)
                        navController.navigate(R.id.nav_home)
                } else if (item.itemId == R.id.nav_category) {
                    if (menuItemClick != item.itemId)
                        navController.navigate(R.id.nav_category)
                } else if (item.itemId == R.id.nav_cart) {
                    if (menuItemClick != item.itemId)
                        navController.navigate(R.id.nav_cart)
                }else if(item.itemId == R.id.nav_view_order)
                    if (menuItemClick != item.itemId)
                        navController.navigate(R.id.nav_view_order)

                menuItemClick = item.itemId
                return true
            }
        })
    }

    private fun signOut() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Sign Out")
            .setMessage("Do you really want to exit")
            .setNegativeButton("CANCEL", { dialogInterface, _ -> dialogInterface.dismiss() })
            .setPositiveButton("OK") { dialogInterface, _ ->
                Common.foodSelected = null
                Common.categorySelected = null
                Common.currentUser = null
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        builder.create()
        builder.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        toggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggle.onOptionsItemSelected(item))
            return true

        return super.onOptionsItemSelected(item)

    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    //Event Bus receive
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCategorySelected(event: CategoryClick) {
        if (event.isSuccess) {
            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_food_list)
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onFoodSelected(event: FoodItemClick) {
        if (event.isSuccess) {
            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_food_detail)
        }
    }

    //When user click on Item in PopularCategory List
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onPopularCategorySelected(event: PopularCategoryItemClick) {
        if (event.popularCategoryModel != null) {
            dialog!!.show()

            FirebaseDatabase.getInstance()
                .getReference("Category")
                .child(event.popularCategoryModel.menu_id!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            Common.categorySelected = snapshot.getValue(CategoryModel::class.java)
                            Common.categorySelected!!.menu_id = snapshot.key

                            //Load food
                            FirebaseDatabase.getInstance()
                                .getReference("Category")
                                .child(event.popularCategoryModel!!.menu_id!!)
                                .child("foods")
                                .orderByChild("id")
                                .equalTo(event.popularCategoryModel.food_id)
                                .limitToLast(1)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
                                            dialog!!.dismiss()

                                            for (foodSnapShot in snapshot.children) {
                                                Common.foodSelected =
                                                    foodSnapShot.getValue(FoodModel::class.java)
                                                Common.foodSelected!!.key = foodSnapShot.key
                                            }
                                            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_food_detail)
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        dialog!!.dismiss()
                                        Toast.makeText(
                                            this@HomeActivity,
                                            "" + error.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                })

                        } else {
                            dialog!!.dismiss()
                            Toast.makeText(
                                this@HomeActivity,
                                "Item doesn't exist!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        dialog!!.dismiss()
                        Toast.makeText(this@HomeActivity, "" + error.message, Toast.LENGTH_SHORT)
                            .show()
                    }
                })
        }
    }

    //When user click on Item in BestDeal List
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onBestDealSelected(event: BestDealItemClick) {
        if (event.bestDeal != null) {
            dialog!!.show()

            FirebaseDatabase.getInstance()
                .getReference("Category")
                .child(event.bestDeal.menu_id!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            Common.categorySelected = snapshot.getValue(CategoryModel::class.java)
                            Common.categorySelected!!.menu_id = snapshot.key

                            //Load food
                            FirebaseDatabase.getInstance()
                                .getReference("Category")
                                .child(event.bestDeal!!.menu_id!!)
                                .child("foods")
                                .orderByChild("id")
                                .equalTo(event.bestDeal.food_id)
                                .limitToLast(1)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
                                            dialog!!.dismiss()

                                            for (foodSnapShot in snapshot.children) {
                                                Common.foodSelected =
                                                    foodSnapShot.getValue(FoodModel::class.java)
                                                Common.foodSelected!!.key = foodSnapShot.key
                                            }
                                            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_food_detail)
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        dialog!!.dismiss()
                                        Toast.makeText(
                                            this@HomeActivity,
                                            "" + error.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                })

                        } else {
                            dialog!!.dismiss()
                            Toast.makeText(
                                this@HomeActivity,
                                "Item doesn't exist!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        dialog!!.dismiss()
                        Toast.makeText(this@HomeActivity, "" + error.message, Toast.LENGTH_SHORT)
                            .show()
                    }
                })
        }
    }


    //EventBus which listens when user click on the Cart Icon in FoodListFragment
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCountCartEvent(event: CountCartEvent) {
        if (event.isSuccess) {
            countCartItem()
        }
    }

    private fun countCartItem() {
        cartDataSource.countItemInCart(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Int> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Int) {
                    counterFab.count = t
                }

                override fun onError(e: Throwable) {
                    if (!e.message!!.contains("Query returned empty"))
                        Toast.makeText(
                            this@HomeActivity,
                            "[COUNT CART]" + e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    else
                        counterFab.count = 0
                }
            })
    }

    //EventBus which listens when user click on the Cart Icon in FoodListFragment
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onHideFAB(event: HideFABCart) {
        if (event.isHide) {
            counterFab.hide()
        } else {
            counterFab.show()
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onMenuItemBack(event: MenuItemBack) {
        menuItemClick = -1
        if(supportFragmentManager.backStackEntryCount > 0)
            supportFragmentManager.popBackStack()
    }

}