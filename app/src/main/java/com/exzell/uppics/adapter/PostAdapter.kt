package com.exzell.uppics.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.exzell.uppics.R
import com.exzell.uppics.UserManager
import com.exzell.uppics.databinding.ItemPostBinding
import com.exzell.uppics.model.Post
import com.exzell.uppics.model.Sort
import com.exzell.uppics.model.User
import com.exzell.uppics.model.VoteState
import com.exzell.uppics.utils.shrink
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class PostAdapter(private val context: Context,
                  posts: List<Post>,
                  private val currentUser: User?): ListAdapter<Post, PostAdapter.ViewHolder>(callback) {
    var users = mutableListOf<User>()

    companion object{
        val callback = object: DiffUtil.ItemCallback<Post>(){
            override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem

            override fun getChangePayload(oldItem: Post, newItem: Post): Any? {
                return determinePayload(oldItem, newItem)
            }
        }

        private fun determinePayload(oldItem: Post, newItem: Post): Any?{
            val payload = mutableListOf<String>()

            if(oldItem.title != newItem.title) payload.add(PAYLOAD_TITLE)

            if(oldItem.description != newItem.description) payload.add(PAYLOAD_DESC)

            if(oldItem.votes != newItem.votes) payload.add(PAYLOAD_VOTE_COUNT)

            if(newItem.votes == null || oldItem.votes == null) payload.add(PAYLOAD_VOTE)
            else if(oldItem.votes != newItem.votes) payload.add(PAYLOAD_VOTE)

            return if(payload.size == 1) payload[0]
            else if(payload.isEmpty()) null
            else payload
        }

        const val PAYLOAD_VOTE = "vote none"
        const val PAYLOAD_TITLE = "title change"
        const val PAYLOAD_DESC = "description change"
        const val PAYLOAD_VOTE_COUNT = "vote count change"
        const val PAYLOAD_USER = "user data change"
    }

    /**
     * A listener for when the vote buttons are clicked
     * The parameter talks about whether an upvote or downvote should be performed
     */
    var onVoteClicked: ((Boolean, Long) -> Unit)? = null

    var onMoreClicked: ((Long, View) -> Unit)? = null

    init {
        submitList(posts)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return LayoutInflater.from(context).inflate(R.layout.item_post,
                parent, false).run {
            ViewHolder(this)
                }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        val post = currentList[position]

        if(payloads.isNotEmpty()){
            val innerList = (payloads.find {
                it is List<*>
            } ?: mutableListOf<Any>()) as MutableList<Any>

            innerList.addAll(payloads)

            holder.itemPostBinding.apply {

                if(innerList.contains(PAYLOAD_VOTE_COUNT))
                    layoutVote.textVote.text = (post.votes?.voteSum() ?: 0).toString()

                if(innerList.contains(PAYLOAD_VOTE)) {

                    layoutVote.imageUpvote.isSelected = post.votes?.upvotes?.contains(currentUser?.id) ?: false
                    layoutVote.imageDownvote.isSelected = post.votes?.downvotes?.contains(currentUser?.id) ?: false
                }

                if(innerList.contains(PAYLOAD_TITLE)) {
                    textTitle.text = post.title
                }

                if(innerList.contains(PAYLOAD_DESC)) {
                    textDesc.text = post.description
                }

                if(innerList.contains(PAYLOAD_USER)) {
                    users.find { it.id == post.uid }?.let {
                        bindUserData(this, it, post) }
                }

            }

        } else onBindViewHolder(holder, position)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = currentList[position]
        val user = users.find { it.id == post.uid }

        holder.itemPostBinding.apply {
            layoutVote.textVote.text = (post.votes?.voteSum() ?: 0).toString()

            textTitle.text = post.title

            textTime.text = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT).format(Date(post.uploadTime))

            textDesc.text = post.description

            Glide.with(root)
                    .load(post.imageUrl)
                    .into(imagePost)
                    .request!!.apply {
                        if(!isRunning) begin()
                    }

            user?.let { bindUserData(this, user, post) }

            val upv = post.votes?.upvotes?.contains(currentUser?.id) ?: false
            layoutVote.imageUpvote.isSelected = upv
            layoutVote.imageDownvote.isSelected = post.votes?.downvotes?.contains(currentUser?.id) ?: false
        }
    }

    private fun bindUserData(binding: ItemPostBinding, user: User, post: Post){
        binding.apply {
            user.let {
                if(post.uid == currentUser?.id) textUser.setText(R.string.you)
                else {
                    textUser.text = if (it.name?.isEmpty() == true) "No Title" else it.name
                }

                if(it.photoUrl?.isNotEmpty() == true){
                    Glide.with(root)
                            .load(it.photoUrl)
                            .into(imageUserPic)
                            .request!!.apply {
                                if(!isRunning) begin()
                            }
                }
            }
        }
    }

    fun rearrangeAndSubmitList(sort: Sort, isAscending: Boolean, list: MutableList<Post>){
        ArrayList(list).apply {

            sortWith { p1, p2 ->
                when (sort) {
                    Sort.TITLE -> p1.title.compareTo(p2.title)
                    Sort.CREATED_TIME -> p1.uploadTime.compareTo(p2.uploadTime)
                    else -> {
                        val p1Vote = p1.votes?.voteSum() ?: 0
                        val p2Vote = p2.votes?.voteSum() ?: 0
                        p1Vote.compareTo(p2Vote)
                    }
                }
            }

            //find out if the list is in ascending or descending order
            // by comparing the first and last element according to the sort criteria
            //a negative number means it is in ascending and positive means descending
            val order = when (sort) {
                Sort.TITLE -> this[0].title.compareTo(this[size-1].title)
                Sort.CREATED_TIME -> this[0].uploadTime.compareTo(this[size-1].uploadTime)
                else -> {
                    val p1Vote = this[0].votes?.voteSum() ?: 0
                    val p2Vote = this[size-1].votes?.voteSum() ?: 0
                    p1Vote.compareTo(p2Vote)
                }
            }

            //if order is positive and isAscending is true, we reverse
            if(order > 0 && isAscending) reverse()
            else if(order < 0 && !isAscending) reverse()

            submitList(this)
        }
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val itemPostBinding = ItemPostBinding.bind(itemView)

        init {
            itemPostBinding.layoutVote.imageUpvote.setOnClickListener {
                val post = currentList[absoluteAdapterPosition]

                onVoteClicked?.invoke(true, post.id)
            }

            itemPostBinding.layoutVote.imageDownvote.setOnClickListener {
                val post = currentList[absoluteAdapterPosition]

                onVoteClicked?.invoke(false, post.id)
            }

            itemPostBinding.buttonMore.setOnClickListener {
                val post = currentList[absoluteAdapterPosition]

                onMoreClicked?.invoke(post.id, it)
            }

            itemPostBinding.textDesc.shrink(itemPostBinding.textDesc.maxLines)
        }
    }

}