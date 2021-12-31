package com.exzell.uppics.model

import com.google.firebase.database.Exclude

data class User(@get:Exclude val id: String? = "",
                val name: String? = "",
                val photoUrl: String? = "",
                @get:Exclude val email: String? = "",
                @get:Exclude val password: String? = "",
                @get:Exclude val phoneNumber: String? = "",)