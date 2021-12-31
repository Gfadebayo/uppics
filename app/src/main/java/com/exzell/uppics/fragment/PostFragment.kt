package com.exzell.uppics.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.exzell.uppics.R
import com.exzell.uppics.databinding.FragmentPostBinding
import com.exzell.uppics.utils.getText
import com.exzell.uppics.utils.hideSoftKeyboard
import com.exzell.uppics.utils.setText
import com.exzell.uppics.viewmodel.HomeViewModel
import com.google.android.material.snackbar.Snackbar

class PostFragment: Fragment() {

    companion object{
        const val KEY_ID = "com.exzell.uppics.KEY_POST_ID"
    }

    private var mBinding: FragmentPostBinding? = null

    private lateinit var mViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mViewModel = ViewModelProvider(requireActivity(), ViewModelProvider.AndroidViewModelFactory(requireActivity().application))
                .get(HomeViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentPostBinding.inflate(inflater, container, false).run {
            mBinding = this
            root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val editPost = arguments?.let {
            if(it.containsKey(KEY_ID))
                mViewModel.getPosts().find { it.id == arguments!!.getLong(KEY_ID) }!!
            else null
        }

        mBinding!!.apply {

            Glide.with(root)
                    .load(editPost?.imageUrl ?: mViewModel.fileUri!!)
                    .override(400, 400)
                    .into(imagePic)
                    .request!!.apply {
                        if(!isRunning) begin()
                    }

            if(editPost != null){
                buttonPost.setText(R.string.update)
                textTitle.setText(editPost.title)
                textComment.setText(editPost.description)
            }

            buttonPost.setOnClickListener {
                it.hideSoftKeyboard()

                indicatorLoading.visibility = View.VISIBLE
                parentPost.visibility = View.GONE

                if(editPost == null){
                    mViewModel.uploadImageAndCreatePost(textTitle.getText(),
                            textComment.getText()){

                        if(it == HomeViewModel.SUCCESS){
                            Toast.makeText(requireContext(), "Post created successfully", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()

                        }else {
                            indicatorLoading.visibility = View.GONE
                            parentPost.visibility = View.VISIBLE

                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        }
                    }

                }else {
                    mViewModel.update(arguments!!.getLong(KEY_ID), textTitle.getText(),
                            textComment.getText()){

                        if(it == HomeViewModel.SUCCESS){
                            Toast.makeText(requireContext(), "Post updated successfully", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()

                        }else {
                            indicatorLoading.visibility = View.GONE
                            parentPost.visibility = View.VISIBLE

                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}