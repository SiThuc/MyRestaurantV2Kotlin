package com.example.myrestaurant_v2_kotlin.model

data class CommentModel(var ratingValue: Float = 0.0f,
                        var comment: String? = null,
                        var name: String? = null,
                        var uid: String? = null,
                        var commentTimestamp: HashMap<String, Any>? = null)
