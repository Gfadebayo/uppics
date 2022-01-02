package com.exzell.uppics.fragment

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.marginBottom
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.exzell.uppics.R
import com.exzell.uppics.adapter.PostAdapter
import com.exzell.uppics.adapter.itemdecoration.PaddingDecoration
import com.exzell.uppics.databinding.FragmentHomeBinding
import com.exzell.uppics.model.Sort
import com.exzell.uppics.utils.checkPermission
import com.exzell.uppics.utils.createTempFile
import com.exzell.uppics.utils.getUri
import com.exzell.uppics.viewmodel.HomeViewModel
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import java.util.*

class HomeFragment : Fragment() {

    private var mBinding: FragmentHomeBinding? = null

    private lateinit var mViewModel: HomeViewModel

    private lateinit var mPictureContract: ActivityResultLauncher<Uri>

    private lateinit var mPermissionContract: ActivityResultLauncher<Array<String>>

    private lateinit var mFileContract: ActivityResultLauncher<String>

    private var canTakePicture = false

    private var canFetchFile = false

    private var mSort = Sort.UPVOTES

    private var isAscending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        canTakePicture = requireContext().checkPermission(Manifest.permission.CAMERA)

        mViewModel = ViewModelProvider(requireActivity(), ViewModelProvider.AndroidViewModelFactory(requireActivity().application))
                .get(HomeViewModel::class.java)

        mPictureContract = registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) launchPostFragment()
            else Toast.makeText(requireContext(), R.string.error_occured, Toast.LENGTH_SHORT).show()
        }

        mPermissionContract = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            canTakePicture = it.getOrDefault(Manifest.permission.CAMERA, false)
            canFetchFile = it.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false)

            if (canTakePicture) takePicture()
            else if (canFetchFile) fetchFile()
        }

        mFileContract = registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                mViewModel.fileUri = it
                launchPostFragment()
            }
        }
    }

    private fun launchPostFragment(postId: Long? = null) {
        findNavController().navigate(R.id.action_frag_home_to_post,
                if (postId == null) null else Bundle(1).apply {
                    putLong(PostFragment.KEY_ID, postId)
                })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentHomeBinding.inflate(inflater, container, false).run {
            mBinding = this
            root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding!!.apply {
            recyclerPost.post {
                val fileFab = requireActivity().findViewById<View>(R.id.twin_fab)
                val fabTop = fileFab.measuredHeight + fileFab.marginBottom

                recyclerPost.addItemDecoration(PaddingDecoration(fabTop))
            }

            val adapter = PostAdapter(requireContext(), Collections.emptyList(), mViewModel.getCurrentUser())
            adapter.users = mViewModel.getAllUsers()
            recyclerPost.adapter = adapter

            adapter.onVoteClicked = { isUpvote, postId ->

                mViewModel.update(isUpvote, postId) {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                }
            }


            adapter.onMoreClicked = { postId, view ->
                createPopMenu(postId, view)
            }

            mViewModel.fetchAllPosts({
                indicatorLoading.visibility = View.GONE

                if (it.isEmpty()) {
                    textError.visibility = View.VISIBLE

                } else {

                    textError.visibility = View.GONE

                    adapter.rearrangeAndSubmitList(mSort, isAscending, it)
                }

            }, {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            })

            mViewModel.onUserChange = {
                adapter.users = it.toMutableList()
                adapter.notifyItemRangeChanged(0, adapter.itemCount, PostAdapter.PAYLOAD_USER)

                requireActivity().invalidateOptionsMenu()
            }

            recyclerPost.doOnLayout {
                requireActivity().findViewById<View>(R.id.fab_camera)?.setOnClickListener {
                    if (canTakePicture) takePicture()
                    else mPermissionContract.launch(arrayOf(Manifest.permission.CAMERA))
                }

                requireActivity().findViewById<View>(R.id.fab_gallery)?.setOnClickListener {
                    if (canFetchFile) fetchFile()
                    else mPermissionContract.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                }
            }
        }
    }

    private fun createPopMenu(postId: Long, view: View) {
        val post = mViewModel.getPosts().find { it.id == postId }!!

        PopupMenu(view.context, view).apply {
            inflate(R.menu.popup_more)

            if (mViewModel.getCurrentUser()?.id != post.uid) {
                menu.findItem(R.id.action_delete).isEnabled = false
                menu.findItem(R.id.action_edit).isEnabled = false
            }

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_save -> mViewModel.savePostImage(postId) { path ->
                        if (it == null) {
                            Toast.makeText(requireContext(), R.string.fail_save_image, Toast.LENGTH_SHORT).show()

                        } else {
                            Snackbar.make(mBinding!!.root, R.string.download_success, Snackbar.LENGTH_INDEFINITE).setAction(R.string.open) {
                                Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(requireContext().getUri(path!!), "image/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                                    startActivity(this)
                                }
                            }.show()
                        }
                    }

                    R.id.action_delete -> mViewModel.deletePost(postId)

                    R.id.action_edit -> launchPostFragment(postId)
                }

                true
            }

            show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)

        val userItem = menu.findItem(R.id.action_user)
        (userItem.actionView as ShapeableImageView)
                .setOnClickListener {
                    ProfileDialogFragment.getInstance().let {
                        it.show(childFragmentManager, null)
                        it.dialog?.setOnDismissListener {
                            loadMenuImage(userItem)
                        }
                    }
                }

        if (mSort == Sort.UPVOTES) menu.findItem(R.id.action_votes).isChecked = true
        else if (mSort == Sort.CREATED_TIME) menu.findItem(R.id.action_time).isChecked = true
        else if (mSort == Sort.TITLE) menu.findItem(R.id.action_title).isChecked = true

        menu.findItem(R.id.action_order).isChecked = isAscending
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        loadMenuImage(menu.findItem(R.id.action_user))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val adapter: PostAdapter = mBinding!!.recyclerPost.adapter as PostAdapter

        return when (item.itemId) {

            R.id.action_user -> {
                ProfileDialogFragment.getInstance().let {
                    it.show(childFragmentManager, null)
                    it.setDismissListener {
                        loadMenuImage(item)
                    }
                }
                true
            }
            R.id.action_votes -> {
                mSort = Sort.UPVOTES
                adapter.rearrangeAndSubmitList(mSort, isAscending, adapter.currentList)
                item.isChecked = true
                true
            }

            R.id.action_title -> {
                mSort = Sort.TITLE
                adapter.rearrangeAndSubmitList(mSort, isAscending, adapter.currentList)
                item.isChecked = true
                true
            }

            R.id.action_time -> {
                mSort = Sort.CREATED_TIME
                adapter.rearrangeAndSubmitList(mSort, isAscending, adapter.currentList)
                item.isChecked = true
                true
            }

            R.id.action_order -> {
                isAscending = !isAscending
                adapter.submitList(adapter.currentList.reversed())
                item.isChecked = isAscending
                true
            }

            R.id.action_logout -> {
                mViewModel.signout {
                    findNavController().navigate(R.id.action_frag_home_to_login)
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun fetchFile() {
        mViewModel.clearFileAfter = false
        val fab = requireActivity().findViewById<View>(R.id.fab_gallery)

        mFileContract.launch("image/*", ActivityOptionsCompat
                .makeScaleUpAnimation(fab, 0, 0, 0, 0))
    }

    private fun takePicture() {
        val fab = requireActivity().findViewById<View>(R.id.fab_camera)
        val fileUri = requireContext().createTempFile()
        mViewModel.fileUri = fileUri
        mViewModel.clearFileAfter = true

        mPictureContract.launch(fileUri,
                ActivityOptionsCompat.makeScaleUpAnimation(fab, 0, 0, 0, 0))
    }

    private fun loadMenuImage(item: MenuItem) {

        mViewModel.getUserPic()?.let {
            (item.actionView as ShapeableImageView).apply {
                Glide.with(this)
                        .load(mViewModel.getUserPic())
                        .circleCrop()
                        .into(this)
                        .request!!.let {
                            if (!it.isRunning) it.begin()
                        }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }
}