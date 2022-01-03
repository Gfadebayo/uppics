package com.exzell.uppics.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.exzell.uppics.UserManager
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginViewModel: ViewModel() {

    private val auth = Firebase.auth

    fun signup(email: String, password: String, onComplete: (String) -> Unit){
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    onComplete.invoke(if(!it.isSuccessful) it.exception?.message!! else MESSAGE_SUCCESS)
                    UserManager.addUserToDb()
                }
    }

    fun signin(email: String, password: String, onComplete: (String) -> Unit){
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    onComplete.invoke(if(!it.isSuccessful) it.exception?.message!! else MESSAGE_SUCCESS)
                    UserManager.addUserToDb()
                }
    }

    fun fixForgotPassword(email: String, onComplete: (String) -> Unit) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener {
                    onComplete.invoke(if(it.isSuccessful) MESSAGE_SUCCESS else it.exception?.message!!)

                }
    }

    fun isSignedin(): Boolean{
        return auth.currentUser != null
    }

    companion object{
        const val MESSAGE_SUCCESS = "UpPics:SUCCESS"
    }
}