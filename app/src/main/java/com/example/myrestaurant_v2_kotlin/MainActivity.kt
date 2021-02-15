package com.example.myrestaurant_v2_kotlin

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.example.myrestaurant_v2_kotlin.common.Common
import com.example.myrestaurant_v2_kotlin.databinding.ActivityHomeBinding
import com.example.myrestaurant_v2_kotlin.databinding.ActivityMainBinding
import com.example.myrestaurant_v2_kotlin.databinding.LayoutRegisterBinding
import com.example.myrestaurant_v2_kotlin.model.UserModel
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dmax.dialog.SpotsDialog
import io.reactivex.disposables.CompositeDisposable

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener
    private lateinit var dialog: AlertDialog
    private lateinit var userRef: DatabaseReference
    private lateinit var binding: ActivityMainBinding

    private val compositeDisposable = CompositeDisposable()

    companion object {
        private val APP_REQUEST_CODE = 2018
    }


    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(listener)
    }

    override fun onStop() {
        if (listener != null)
            firebaseAuth.removeAuthStateListener(listener)
        compositeDisposable.clear()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
    }

    private fun init() {
        firebaseAuth = FirebaseAuth.getInstance()
        userRef = FirebaseDatabase.getInstance().getReference(Common.USER_REF)
        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()

        listener = FirebaseAuth.AuthStateListener { firebaseAuth ->

            Dexter.withContext(this@MainActivity)
                .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                        val user = firebaseAuth.currentUser
                        if (user != null) {

                            // Already login
                            Toast.makeText(this@MainActivity, "Already login", Toast.LENGTH_SHORT)
                                .show()
                            checkUserFromFirebase(user)

                        } else {
                            //Not login
                            phoneLogin()
                        }
                    }

                    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {

                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: PermissionRequest?,
                        p1: PermissionToken?
                    ) {
                        Toast.makeText(
                            this@MainActivity,
                            "You have to accept this permission to continue the app",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }).check()
        }

    }

    private fun checkUserFromFirebase(user: FirebaseUser) {
        dialog.show()
        userRef.child(user.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userModel = snapshot.getValue(UserModel::class.java)
                        goToHomeActivity(userModel)
                    } else
                        showRegisterDialog(user)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "" + error.message, Toast.LENGTH_SHORT).show()
                }

            })
        dialog.dismiss()
    }


    private fun showRegisterDialog(user: FirebaseUser) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Register")
        builder.setMessage("Please fill information")

        val dialog_binding = LayoutRegisterBinding.inflate(layoutInflater)

        //Set phone number for editText
        dialog_binding.edtPhone.setText(user.phoneNumber.toString())

        builder.setView(dialog_binding.root)
        builder.setNegativeButton("CANCEL") { dialogInterface, i -> dialogInterface.dismiss() }

        builder.setPositiveButton("REGISTER") { dialogInterface, i ->
            if (TextUtils.isDigitsOnly(dialog_binding.edtName.text.toString())) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            } else if (TextUtils.isDigitsOnly(dialog_binding.edtAddress.text.toString())) {
                Toast.makeText(this, "Please enter your address", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val userModel = UserModel()
            userModel.uid = user.uid
            userModel.name = dialog_binding.edtName.text.toString()
            userModel.address = dialog_binding.edtAddress.text.toString()
            userModel.phone = dialog_binding.edtPhone.text.toString()

            userRef.child(user.uid)
                .setValue(userModel)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        dialogInterface.dismiss()
                        Toast.makeText(
                            this,
                            "Congratulation! Regisrer success!",
                            Toast.LENGTH_SHORT
                        ).show()

                        goToHomeActivity(userModel)
                    }
                }
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun goToHomeActivity(userModel: UserModel?) {

        FirebaseMessaging.getInstance().token
            .addOnFailureListener { e ->
                Toast.makeText(this@MainActivity, ""+e.message, Toast.LENGTH_SHORT).show()
                Common.currentUser = userModel!!
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    Common.currentUser = userModel!!
                    Common.updateToken(this@MainActivity, task.result)
                    startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                    finish()
                }
            }



    }


    private fun phoneLogin() {
        val providers = arrayListOf(AuthUI.IdpConfig.PhoneBuilder().build())
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            APP_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
            } else {
                Toast.makeText(this, "Failed to sign in", Toast.LENGTH_SHORT).show()
            }
        }
    }
}