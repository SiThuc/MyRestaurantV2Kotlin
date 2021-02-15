package com.example.myrestaurant_v2_kotlin.model

class TokenModel() {
    var uid: String? = null
    var token: String? = null

    constructor(uid: String, token: String) : this() {
        this.uid = uid
        this.token = token
    }
}