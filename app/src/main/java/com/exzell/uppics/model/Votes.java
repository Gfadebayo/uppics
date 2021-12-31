package com.exzell.uppics.model;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Votes {

    private List<String> upvotes;

    private List<String> downvotes;

    public List<String> getUpvotes() {
        return upvotes;
    }

    public List<String> getDownvotes() {
        return downvotes;
    }

    /**
     * Adding to one list automatically means the id MUST NOT be in
     * the other list ie remove it from the other list
     * @param userId The user id to put into the list
     * @param isUpvote whether the id should be added to the upvote list
     *                or the downvote list
     */
    public void addVote(String userId, boolean isUpvote){

        if(isUpvote){
            if(upvotes == null) upvotes = new ArrayList<>();

            upvotes.add(userId);

        }else {
            if(downvotes == null) downvotes = new ArrayList<>();

            downvotes.add(userId);
        }

        removeVote(userId, !isUpvote);
    }

    public void removeVote(String userId, boolean isUpvote){

        if(isUpvote && upvotes != null){
            upvotes.remove(userId);

        }else if(!isUpvote && downvotes != null) {
            downvotes.remove(userId);
        }
    }

    public int voteSum(){
        int upSum = upvotes == null ? 0 : upvotes.size();
        int downSum = downvotes == null ? 0 : downvotes.size();

        return upSum - downSum;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj == null || obj instanceof Votes) return false;

        Votes v = (Votes) obj;

        return voteSum() == v.voteSum();
    }
}
