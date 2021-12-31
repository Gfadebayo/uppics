package com.exzell.uppics.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.ConfigurationCompat
import androidx.documentfile.provider.DocumentFile
import com.exzell.uppics.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun Context.createTempFile(): Uri{

    val locale: Locale = ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0]
    val format = SimpleDateFormat("dd/MM/yyyy,HH:mm:s", locale).format(Date())

    val tempFile = File.createTempFile("UPPICS", ".png", getExternalFilesDir("images"))

    return FileProvider.getUriForFile(this, BuildConfig.AUTHORITY, tempFile)
}

fun Context.getUri(filePath: String): Uri{
    val file = File(filePath)

    return FileProvider.getUriForFile(this, BuildConfig.AUTHORITY, file)
}

fun Context.deleteFile(uri: Uri): Boolean{
    return DocumentFile.fromSingleUri(this, uri)?.delete() == true
}

fun Context.checkPermission(permission: String): Boolean{
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}