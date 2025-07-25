package com.martahp.batucadafina.utils

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class AudioPermissionManager (private val activity: ComponentActivity){

    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var onPermissionResult: ((Boolean) -> Unit)? = null

    fun setupPermissionLauncher(onResult: (Boolean) -> Unit) {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            isGranted: Boolean ->
            onPermissionResult?.invoke(isGranted) // Llama al callback cuando se obtiene el resultado
        }
        onPermissionResult = onResult // Guarda el callback para usarlo luego
    }

    fun askForPermission() {
        when {
            ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                onPermissionResult?.invoke(true) //Permiso ya concedido, notifica como concedido
            }
            activity.shouldShowRequestPermissionRationale(android.Manifest.permission.RECORD_AUDIO) -> {
                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
        }

}