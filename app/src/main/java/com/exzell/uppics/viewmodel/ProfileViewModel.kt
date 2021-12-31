package com.exzell.uppics.viewmodel

import android.net.Uri
import androidx.core.os.UserManagerCompat
import androidx.lifecycle.ViewModel
import com.exzell.uppics.UserManager
import com.exzell.uppics.model.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import timber.log.Timber

class ProfileViewModel: ViewModel() {

    var userManager: UserManager = UserManager

    var picUri: Uri? = null

    private val auth = Firebase.auth

    private val database = Firebase.database

    fun getUserDetails(callback: (User) -> Unit) {
        UserManager.user?.let { callback.invoke(it) }
    }

    fun updateUser(name: String, email: String, password: String, onComplete: (Boolean) -> Unit) {

        userManager.user?.let {
        val map = mutableMapOf<String, String>()
            if(it.name?.equals(name) != true){

                map["name"] = name
            }

            if(it.password?.equals(password) != true){
                map["password"] = password
            }

            userManager.updateUser(map, email = if(email == it.email) email else null, ){
                picUri?.let {

                    userManager.updatePicture(it){
                        onComplete.invoke(it)
                    }
                } ?: onComplete.invoke(it)
            }
        }

    }
}