package com.example.myrestaurant_v2_kotlin.model

class UserModel() {
    var uid: String = ""
    var name: String? = null
    var address: String? = null
    var phone: String? = null

    constructor(uid: String, name: String?, address: String?, phone: String?) : this() {
        this.uid = uid
        this.name = name
        this.address = address
        this.phone = phone
    }
}