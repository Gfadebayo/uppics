package com.exzell.uppics.fragment

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.toSpannable
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.exzell.uppics.R
import com.exzell.uppics.databinding.FragmentLoginBinding
import com.exzell.uppics.utils.hideSoftKeyboard
import com.exzell.uppics.utils.watchField
import com.exzell.uppics.viewmodel.LoginViewModel
import com.google.android.material.snackbar.Snackbar

class LoginFragment : Fragment() {

    private var mBinding: FragmentLoginBinding? = null

    private var mViewModel: LoginViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mViewModel = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory())
                .get(LoginViewModel::class.java)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentLoginBinding.inflate(inflater, container, false).run {
            mBinding = this
            root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mViewModel!!.isSignedin()) findNavController().navigate(R.id.action_frag_login_to_frag_home)

        mBinding!!.apply {
            makeTextviewClickable(true)

            buttonSignIn.setOnClickListener {
                buttonSignIn.hideSoftKeyboard()

                val isSignup = buttonSignIn.text == requireContext().getString(R.string.sign_up)

                val email = textEmail.editText!!.text.toString()
                val pass = textPassword.editText!!.text.toString()


                if (!validateEmail(email) && !validatePassword(pass)) {
                    sign(email, pass, isSignup)
                }
            }

            buttonForgot.setOnClickListener {
                buttonForgot.hideSoftKeyboard()

                val email = textEmail.editText!!.text.toString()

                if (!validateEmail(email)) mViewModel!!.fixForgotPassword(email) {
                    val successText = requireContext().getString(R.string.email_sent_success)

                    Toast.makeText(requireContext(), if (it == LoginViewModel.MESSAGE_SUCCESS) successText
                    else it, Toast.LENGTH_LONG).show()
                }
            }

            textEmail.watchField {
                if (!validateEmail(it)) textEmail.isErrorEnabled = false
            }

            textPassword.watchField {
                if (!validatePassword(it)) textPassword.isErrorEnabled = false
            }
        }
    }

    /**
     * Returns true if theres an issue with the password
     */
    private fun validatePassword(password: String): Boolean {
        return when {
            password.isEmpty() -> {
                displayError(true)
                true
            }

            password.length < 8 -> {
                displayError(true, REASON_LENGTH)
                true
            }

            else -> false
        }
    }

    /**
     * Returns true if theres an issue with the email
     */
    private fun validateEmail(email: String): Boolean {
        val isNotValid = email.isEmpty()
        if (isNotValid) displayError(false)

        return isNotValid
    }

    private fun displayError(forPassword: Boolean, reason: String = REASON_EMPTY) {
        mBinding!!.apply {
            val reasonString = requireContext().getString(when (reason) {
                REASON_LENGTH -> R.string.password_length_error
                else -> R.string.field_empty
            })

            if (forPassword) {
                textPassword.error = reasonString
                textPassword.isErrorEnabled = true

            } else {
                textEmail.error = reasonString
                textEmail.isErrorEnabled = true
            }
        }
    }

    private fun sign(email: String, pass: String, isSignup: Boolean) {
        val snackbar = Snackbar.make(mBinding!!.root, if (isSignup) R.string.signing_up_wait
        else R.string.signing_in_wait, Snackbar.LENGTH_INDEFINITE)
        snackbar.show()

        val onComplete = { message: String ->
            snackbar.dismiss()

            if (message == LoginViewModel.MESSAGE_SUCCESS) findNavController().navigate(R.id.action_frag_login_to_frag_home)
            else Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        if (isSignup) mViewModel!!.signup(email, pass, onComplete)
        else mViewModel!!.signin(email, pass, onComplete)
    }

    private fun makeTextviewClickable(forSignUp: Boolean) {
        val sign = requireContext().getString(if (forSignUp) R.string.sign_up else R.string.sign_in)
        val signText = requireContext().getString(if (forSignUp) R.string.don_t_have_an_account_sign_up else R.string.already_have_an_account_sign_in)
        val signIndex = signText.indexOf(sign, ignoreCase = true)

        val span = signText.toSpannable().apply {
            setSpan(object : ClickableSpan() {
                override fun updateDrawState(ds: TextPaint) {
                    ds.setColor(Color.BLUE)
                }

                override fun onClick(p0: View) {
                    switchSign(forSignUp)
                }

            }, signIndex, signIndex + sign.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        mBinding!!.textSignUp.text = span
        if (mBinding!!.textSignUp.movementMethod != LinkMovementMethod.getInstance()) {
            mBinding!!.textSignUp.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun switchSign(toSignup: Boolean) {
        mBinding!!.apply {
            buttonSignIn.setText(if (toSignup) R.string.sign_up else R.string.sign_in)
            buttonForgot.visibility = if (!toSignup) View.VISIBLE else View.GONE

            makeTextviewClickable(!toSignup)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }

    companion object {
        const val REASON_EMPTY = "empty field"
        const val REASON_LENGTH = "length problem"
    }
}