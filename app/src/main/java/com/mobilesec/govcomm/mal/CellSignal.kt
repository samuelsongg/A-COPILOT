package com.mobilesec.govcomm.mal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.regex.Pattern

class CellSignal(private val context: Context) {

    companion object {
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1001
    }

    fun cellSignal(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Log.d("CellSignal", "Permission not granted. Please request ACCESS_FINE_LOCATION permission.")
            return false // Can't determine cell signal without permissions.
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //Log.d("CellSignal", "Running on Android 10 or newer")
                return requestCellSignal() // Only request cell signal on Android 10 or newer.
            } else {
                //Log.d("CellSignal", "Running on an older version of Android")
                // Handle the case for older versions differently if needed.
                return false // Or any other appropriate response for older versions.
            }
        }
    }


    private fun requestCellSignal(): Boolean {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val cellInfoList = telephonyManager.allCellInfo
            for (info in cellInfoList) {
                val isConnected = extractCellConnectionStatus(info.toString())
                //Log.d("CellSignal", "CellInfo: $info, Connection Status: $isConnected")
                if (isConnected) {
                    return true // Return true immediately if any cell is connected
                }
            }
        }
        return false // Return false if no cells are connected or if we couldn't check the cell info
    }

    private fun extractCellConnectionStatus(cellInfoString: String): Boolean {
        val pattern = Pattern.compile("mCellConnectionStatus=(\\d)")
        val matcher = pattern.matcher(cellInfoString)

        if (matcher.find()) {
            return matcher.group(1) == "1"
        }
        return false
    }
}
