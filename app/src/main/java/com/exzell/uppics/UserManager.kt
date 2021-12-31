package com.exzell.uppics

import android.net.Uri
import com.exzell.uppics.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import timber.log.Timber

typealias UserChangeCallback = (User) -> Unit

object UserManager {

    var user: User? = null
        private set

    val users = mutableListOf<User>()

    private val database = Firebase.database

    private val auth = Firebase.auth

    private val storage = Firebase.storage

    var signoutListener: (() -> Unit)? = null

    private var userChangeListener: MutableList<UserChangeCallback> = mutableListOf()

    init {
        getAllUser()

        auth.addAuthStateListener {
            //a different uid means user has changed


            if(auth.currentUser == null){
                signoutListener?.invoke()
                user = null

            }else if(user == null || it.uid != user?.id){
                user = null
                setUser()
            }
        }
    }

    fun init(){}

    fun addUserChangeListener(listener: UserChangeCallback){
        userChangeListener.add(listener)
    }

    fun removeUserChangeListener(listener: UserChangeCallback){
        userChangeListener.remove(listener)
    }

    /**
     * The callbacks are in case the call fails in init (maybe due to network)
     * and the result is needed immediately
     */
    fun setUser(){
        if(user == null){
            database.reference.child("users").child(auth.uid!!).addValueEventListener(object: ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()){
                        val snapUser = snapshot.getValue(User::class.java)

                        user = if(user == null) snapUser!!.copy(id = snapshot.key!!)
                        else snapUser!!.copy(id = user!!.id, email = user!!.email, phoneNumber = user!!.phoneNumber)

                        userChangeListener.forEach { it.invoke(user!!) }
                    } else addUserToDb()
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.i(error.message)
                }
            })

            auth.currentUser?.apply {
                user = if(user == null) User(id = uid, email = email, phoneNumber = phoneNumber)
                else user!!.copy(id = uid, email = email, phoneNumber = phoneNumber)
            }
        }
    }

    private fun getAllUser(){
        database.reference.child("users").addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists() && snapshot.hasChildren()){

                    users.clear()

                    users.addAll(snapshot.children.map {
                        it.getValue(User::class.java)!!.copy(id = it.key!!)
                    })

//                    if(user == null) user = users.find { it.id == auth.uid }?.copy(email = auth.currentUser!!.email)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.d(error.message)
            }
        })
    }

//    fun getUserDetails(onSuccess: (User) -> Unit, onFailed: (String) -> Unit){
//       if(user != null) onSuccess.invoke(user!!)
//        else {
//            setUser(onSuccess, onFailed)
//        }
//    }

    fun addUserToDb(){
        auth.currentUser!!.let {

            database.reference.child("users").apply {
                addListenerForSingleValueEvent(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if(snapshot.hasChild(it.uid)) Timber.d("User already in the database")
                        else {
                            val user = User(id = it.uid, name = it.displayName, email = it.email, phoneNumber = it.phoneNumber)
                            child(it.uid).setValue(user)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Timber.d(error.message)
                    }
                })
            }
        }
    }

    fun updatePicture(picUri: Uri, onComplete: (Boolean) -> Unit) {

        storage.reference.child("profiles/${user!!.id}/profile.png")
                .putFile(picUri)
                .addOnCompleteListener {
                    it.result!!.storage.downloadUrl.addOnSuccessListener {

                        database.reference.child("users")
                                .child(user!!.id!!)
                                .child("photoUrl")
                                .setValue(it.toString())
                                .addOnCompleteListener {
//                                if(it.isSuccessful){
//                                    user = user!!.copy(photoUrl = url)
//                                }
//
                                    onComplete.invoke(it.isSuccessful)
                                }
                    }
                }
    }

    fun updateUser(changeMap: Map<String, String>,
                   email: String? = null,
                   phone: String? = null,
                   onComplete: (Boolean) -> Unit) {
//            phone?.let { updatePhoneNumber(PhoneAuthCredential()) }

        if (changeMap.isNotEmpty()) {
            database.reference.child("users")
                    .child(user!!.id!!)
                    .updateChildren(changeMap)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            updateMailAndPhone(email, phone, onComplete)

                        }else onComplete.invoke(false)
                    }

        }
        else onComplete.invoke(true)
    }

    private fun updateMailAndPhone(email: String? = null, phone: String? = null, onComplete: (Boolean) -> Unit){
        email?.let {
            auth.currentUser!!.updateEmail(email).addOnCompleteListener {
                if(it.isSuccessful) updatePhone(phone, onComplete)
                else onComplete.invoke(false)
            }
        } ?: updatePhone(phone, onComplete)
    }

    private fun updatePhone(phone: String?, onComplete: (Boolean) -> Unit){
        onComplete.invoke(true)
    }

    fun signout() {
        auth.signOut()
    }
}