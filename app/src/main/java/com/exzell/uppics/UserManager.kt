package com.exzell.uppics

import android.net.Uri
import com.exzell.uppics.model.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import timber.log.Timber

/**
 * A function that will be called anytime a user data changes
 * The Boolean tells if the user currently using the app
 * is the one that has changed
 */
typealias UserChangeCallback = (Boolean) -> Unit

object UserManager: ValueEventListener {

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

            if (auth.currentUser == null) {
                signoutListener?.invoke()
                user = null

            }
            //a different uid means user has changed
            else if (user == null || it.uid != user?.id) {
                user = null
                setUser()
            }
        }
    }

    fun init() {
        if(auth.uid != null) setUser()
    }

    fun addUserChangeListener(listener: UserChangeCallback) {
        userChangeListener.add(listener)
    }

    fun removeUserChangeListener(listener: UserChangeCallback) {
        userChangeListener.remove(listener)
    }

    /**
     * The callbacks are in case the call fails in init (maybe due to network)
     * and the result is needed immediately
     */
    private fun setUser(newData: User? = null) {
        if (user == null) {

            val currentUser = users.find {
                it.id == auth.uid
            }

            if (currentUser == null) {
                //Auth has some data so we can use that till the DB is ready
                user = User(id = auth.uid, name = auth.currentUser!!.displayName, email = auth.currentUser!!.email)
                addUserToDb()
            } else {
                user = auth.currentUser?.run {
                    currentUser.copy(id = uid, email = email, phoneNumber = phoneNumber)
                }
            }
        }

        if (newData != null) {
            user = auth.currentUser?.run {
                newData.copy(id = uid, email = email, phoneNumber = phoneNumber)
            }
        }
    }

    private fun getAllUser() {
        database.reference.child("users").addValueEventListener(this)
    }

    fun addUserToDb() {
        auth.currentUser!!.let {

            database.reference.child("users").apply {
                addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.hasChild(it.uid)) Timber.d("User already in the database")
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

                        } else onComplete.invoke(false)
                    }

        } else onComplete.invoke(true)
    }

    private fun updateMailAndPhone(email: String? = null, phone: String? = null, onComplete: (Boolean) -> Unit) {
        email?.let {
            auth.currentUser!!.updateEmail(email).addOnCompleteListener {
                if (it.isSuccessful) updatePhone(phone, onComplete)
                else onComplete.invoke(false)
            }
        } ?: updatePhone(phone, onComplete)
    }

    private fun updatePhone(phone: String?, onComplete: (Boolean) -> Unit) {
        onComplete.invoke(true)
    }

    fun signout() {
        auth.signOut()
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        if (snapshot.exists() && snapshot.hasChildren()) {

            val userCopy = snapshot.children.map {
                it.getValue(User::class.java)!!.copy(id = it.key!!)
            }

            val changedUser = userCopy.filter { !users.contains(it) }
            val changedCurrentUser = changedUser.find { it.id == user?.id }

            users.clear()
            users.addAll(userCopy)

            if (user == null) setUser()
            else if (changedCurrentUser != null) setUser(changedCurrentUser)

            if (changedUser.isNotEmpty()) userChangeListener.forEach { it.invoke(changedCurrentUser != null) }
        }
    }

    override fun onCancelled(error: DatabaseError) {
        Timber.d(error.message)
    }
}