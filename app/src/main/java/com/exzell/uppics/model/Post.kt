package com.exzell.uppics.model

import com.google.firebase.database.Exclude

data class Post(@get:Exclude val id: Long = 0,
                @get:Exclude val uid: String = "",
                val uploadTime: Long = 0,
                val title: String = "",
                val description: String = "",
                val imageUrl: String = "",
                var votes: Votes? = null,)

enum class VoteState{
    UPVOTE,
    DOWNVOTE,
    NO_VOTE
}
