package com.exzell.uppics.fragment

import android.Manifest
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.exzell.uppics.R
import com.exzell.uppics.databinding.DialogProfileBinding
import com.exzell.uppics.utils.createTempFile
import com.exzell.uppics.utils.getText
import com.exzell.uppics.viewmodel.ProfileViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ProfileDialogFragment : BottomSheetDialogFragment() {

    companion object{
        fun getInstance(): ProfileDialogFragment{
            return ProfileDialogFragment()
        }
    }

    private var mBinding: DialogProfileBinding? = null

    private lateinit var mViewModel: ProfileViewModel

    private lateinit var mPictureContract: ActivityResultLauncher<Uri>

    private lateinit var mPermissionContract: ActivityResultLauncher<Array<String>>

    private lateinit var mFileContract: ActivityResultLauncher<String>

    private var mDismissListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mViewModel = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory())
                .get(ProfileViewModel::class.java)

        mPictureContract = registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if(it) {
                loadImage(mViewModel.picUri.toString())
            }
            else Toast.makeText(requireContext(), R.string.error_occured, Toast.LENGTH_SHORT).show()
        }

        mFileContract = registerForActivityResult(ActivityResultContracts.GetContent()){
            if(it != null) {
                mViewModel.picUri = it

                loadImage(it.toString())
            }
        }

        mPermissionContract = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){

            if(it.getOrDefault(Manifest.permission.CAMERA, false)) takePicture()
            else if(it.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false)) fetchFile()
        }
    }

    private fun takePicture() {
        val fileUri = requireContext().createTempFile()
        mViewModel.picUri = fileUri

        mPictureContract.launch(fileUri,
                ActivityOptionsCompat.makeScaleUpAnimation(mBinding!!.buttonCamera, 0, 0, 0, 0))
    }

    private fun fetchFile() {
        mFileContract.launch("image/*", ActivityOptionsCompat
                .makeScaleUpAnimation(mBinding!!.buttonFile, 0, 0, 0, 0))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return DialogProfileBinding.inflate(inflater, container, false).let {
            mBinding = it

            it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mBinding!!.apply {

            setDetails()

            buttonFile.setOnClickListener {
                mPermissionContract.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            }

            buttonCamera.setOnClickListener {
                mPermissionContract.launch(arrayOf(Manifest.permission.CAMERA))
            }

            buttonUpdate.setOnClickListener {
                indicatorLoading.visibility = View.VISIBLE
                parent.visibility = View.GONE

                mViewModel.updateUser(textName.getText(), textMail.getText(), ""){
                    indicatorLoading.visibility = View.GONE
                    parent.visibility = View.VISIBLE

                    if(it) setDetails()
                    else Toast.makeText(requireContext(), "Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setDetails(){
        mBinding!!.apply {
            mViewModel.getUserDetails {
                loadImage(it.photoUrl)

                textName.editText!!.setText(it.name)
                textMail.editText!!.setText(it.email)
            }
        }
    }

    private fun loadImage(url: String?){
        url?.let {
            Glide.with(mBinding!!.root)
                    .load(url)
                    .into(mBinding!!.imageProfile)
                    .request!!.apply {
                        if(!isRunning) begin()
                    }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        mDismissListener?.invoke()
        super.onDismiss(dialog)
    }

    fun setDismissListener(listener: () -> Unit) {
        mDismissListener = listener
    }
}