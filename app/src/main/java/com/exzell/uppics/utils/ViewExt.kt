package com.exzell.uppics.utils

import android.R.attr
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView


/**
 * Watches the field to detect errors the User might make and display the error
 * @param condition the condition that should be satisfied, returning a null means the condition
 * is satisfied and no error should be displayed while non null means the opposite
 */
fun TextInputLayout.watchField(condition: (String) -> Unit){
    this.editText!!.addTextChangedListener(object: TextWatcher{
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            condition.invoke(s!!.toString())
//            if(error == null){
//                isErrorEnabled = false
//            }
        }

        override fun afterTextChanged(p0: Editable?) {}
    })

    setOnFocusChangeListener { view, hasFocus ->
    //it has lost focus and doesn't satisfy the condition so error will be shown
        if(!hasFocus) condition.invoke(editText!!.text.toString())
//
//        if(!hasFocus && error != null){
//            this.error = error
//            isErrorEnabled = true
//
//        }else if(!hasFocus && error == null){
//            isErrorEnabled = false
//        }
    }
}

fun TextInputLayout.getText(): String{
    return editText!!.text.toString()
}

fun TextInputLayout.setText(text: String){
    editText!!.setText(text)
}

fun View.hideSoftKeyboard(){
    ViewCompat.getWindowInsetsController(this)
            ?.hide(WindowInsetsCompat.Type.ime())
}

fun MaterialTextView.shrink(lines: Int){
    post {
        layout?.let {
            val lineCount: Int = it.lineCount
            if (it.getEllipsisCount(lineCount - 1) > 0) {
                setOnClickListener {
                    if (maxLines < Int.MAX_VALUE) {
                        maxLines = Int.MAX_VALUE
                        ellipsize = null
                    } else {
                        maxLines = lines
                        ellipsize = TextUtils.TruncateAt.END
                    }
                }
            }
        }
    }
}