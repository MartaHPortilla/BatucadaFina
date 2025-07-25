package com.martahp.batucadafina.model.entities

data class User(
    val id: Long,
    val username: String,
    val email: String,
    //val password: String,
    val profilePhotoPath: String? = null


)
