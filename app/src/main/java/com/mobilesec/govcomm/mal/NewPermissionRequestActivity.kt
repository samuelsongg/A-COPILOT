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
class NewPermissionRequestActivity: Activity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
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
