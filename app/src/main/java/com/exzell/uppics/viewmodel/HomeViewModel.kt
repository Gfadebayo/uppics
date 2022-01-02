package com.exzell.uppics.viewmodel

import android.app.Application
import android.content.res.Resources
import android.net.Uri
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.AndroidViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.exzell.uppics.UserChangeCallback
import com.exzell.uppics.UserManager
import com.exzell.uppics.model.Post
import com.exzell.uppics.model.User
import com.exzell.uppics.model.Votes
import com.exzell.uppics.utils.deleteFile
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel(application: Application) : AndroidViewModel(application), UserChangeCallback {

    private val context = application.applicationContext

    private val database = Firebase.database

    private val storage = Firebase.storage

    var userManager: UserManager = UserManager

    private val posts: MutableList<Post> = mutableListOf()

    var onUserChange: ((List<User>) -> Unit)? = null

    var fileUri: Uri? = null

    //whether to delete the file after the post has been made
    //true for camera uri and false for uri gotten from the SAF
    var clearFileAfter = false

    init {
        userManager.addUserChangeListener(this)
    }

    fun getPosts(): List<Post> = posts.toList()

    fun fetchAllPosts(onComplete: (MutableList<Post>) -> Unit, onError: (String) -> Unit) {
        database.reference.child("posts").addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snap: DataSnapshot) {
                if (snap.exists() && snap.hasChildren()) {

                    val snapPosts = (snap.children.flatMap { uid ->
                        uid.children.map { number ->
                            number.getValue(Post::class.java)!!
                                    .copy(id = number.key!!.toLong(), uid = uid.key!!)
                        }
                    })


                    posts.clear()
                    posts.addAll(snapPosts)

                    onComplete.invoke(posts)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.d(error.message)
                onError.invoke(error.message)
            }
        })
    }

    fun update(isUpvote: Boolean, postId: Long, onError: (String) -> Unit) {
        val post = posts.find { it.id == postId }!!
        val userId = userManager.user!!.id!!

        post.apply {
            if (votes == null) votes = Votes().apply { addVote(userId, isUpvote) }
            else {
                if (votes!!.upvotes?.contains(userId) == true && isUpvote) votes!!.removeVote(userId, isUpvote)
                else if (votes!!.downvotes?.contains(userId) == true && !isUpvote) votes!!.removeVote(userId, isUpvote)
                else votes!!.addVote(userId, isUpvote)
            }
        }

        //then send it to firebase to update
        //update the available copy iff the firebase update is successful
        database.reference
                .child("posts")
                .child(post.uid)
                .child(post.id.toString())
                .child("votes")
                .setValue(post.votes)
                .addOnFailureListener {

                    onError.invoke(it.localizedMessage)
                }
    }

    fun update(postId: Long, title: String, desc: String, onComplete: (String) -> Unit) {
        val post = posts.find { it.id == postId }!!

        val map = mutableMapOf<String, String>()

        if (title != post.title) map["title"] = title

        if (desc != post.description) map["description"] = desc

        database.reference
                .child("posts")
                .child(post.uid)
                .child(post.id.toString())
                .updateChildren(map as Map<String, Any>)
                .addOnCompleteListener {
                    if (it.isSuccessful) onComplete.invoke(SUCCESS)
                    else onComplete.invoke(it.exception?.localizedMessage ?: "Error")
                }
    }

    fun uploadImageAndCreatePost(title: String, comment: String, onComplete: (String) -> Unit) {
        val locale: Locale = ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0]
        val format = SimpleDateFormat("dd-MM-yyyy,HH:mm:ss", locale).format(Date())

        val metadata = storageMetadata {
            contentType = "image/png"
        }

        storage.getReference("posts/${userManager.user!!.id!!}/UPPICS:${format}.png")
                .putFile(fileUri!!, metadata)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        val time = it.result!!.metadata?.creationTimeMillis ?: 0

                        it.result!!.storage.downloadUrl.addOnSuccessListener {
                            createPost(title, comment, time, it.toString(), onComplete)
                        }


                    } else {
                        Timber.d(it.exception?.localizedMessage ?: "Error")
                        onComplete.invoke(it.exception?.localizedMessage ?: "Error")
                    }
                }
    }

    private fun createPost(title: String, comment: String, uploadTime: Long, url: String, onComplete: (String) -> Unit) {
        val user = Firebase.auth.currentUser!!
        val dispName = user.displayName ?: ""

        database.reference.child("posts").apply {
            addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val last = (if (snapshot.exists()) getLastPostNumber(snapshot) else 0) + 1

                    val post = Post(last, dispName, uploadTime, title, comment, url)

                    child(userManager.user!!.id!!).child(last.toString()).setValue(post)
                            .addOnFailureListener {
                                Timber.d(it.localizedMessage ?: "Error")
                                onComplete.invoke(it.localizedMessage ?: "Error")
                            }.addOnSuccessListener {

                                onComplete.invoke(SUCCESS)

                                //since the temporary file are created just for the post
                                //it should be cleared after
                                if (clearFileAfter) {
                                    if (context.deleteFile(fileUri!!)) {
                                        clearFileAfter = false
                                        fileUri = null
                                    }
                                }
                            }
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.d(error.message)
                    onComplete.invoke(error.message)
                }
            })
        }
    }

    private fun getLastPostNumber(shot: DataSnapshot): Long {
        return shot.children.flatMap {
            it.children.map {
                it.key!!.toLong()
            }
        }.maxOrNull() ?: 0
    }

    fun getAllUsers() = userManager.users

    fun getUserPic(): String? {
        val url = userManager.user?.photoUrl
        return if (url.isNullOrEmpty()) null else url
    }

    fun getCurrentUser(): User? {
        return userManager.user
    }

    fun savePostImage(postId: Long, onComplete: (String?) -> Unit) {
        val parent = context.getExternalFilesDir("downloads")!!
        val file = File(parent, "${parent.list().size}.png")

        val post = posts.find { it.id == postId }!!

        Glide.with(context)
                .downloadOnly()
                .load(post.imageUrl)
                .addListener(object : RequestListener<File> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<File>?, isFirstResource: Boolean): Boolean {
                        onComplete.invoke(null)
                        return false
                    }

                    override fun onResourceReady(resource: File?, model: Any?, target: Target<File>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        return resource?.let {
                            file.outputStream().write(it.inputStream().run {
                                val arr = ByteArray(available())
                                read(arr)
                                arr
                            })

                            onComplete.invoke(file.path)
                            true
                        } ?: false
                    }
                })
                .submit()
                .request!!
                .apply {
                    if (!isRunning) begin()
                }
    }

    fun deletePost(postId: Long) {
        val post = posts.find { it.id == postId }!!

        database.reference
                .child("posts")
                .child(post.uid)
                .child(post.id.toString())
                .removeValue { error, ref ->

                    if (error == null) {
                        storage.getReferenceFromUrl(post.imageUrl)
                                .delete()

                    } else Timber.d(error.details)
                }
    }

    fun signout(onSuccess: () -> Unit) {
        userManager.signoutListener = onSuccess
        userManager.signout()
    }

    override fun invoke(isCurrentUser: Boolean) {
        onUserChange?.invoke(userManager.users)
    }

    companion object {
        const val SUCCESS = "success"
    }
}