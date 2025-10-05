package com.talkar.app.ui.components

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.view.Surface
import android.util.Log

// Centralized holder for camera resources so they can be closed reliably
class CameraHolder {
    @Volatile
    var cameraDevice: CameraDevice? = null
    @Volatile
    var captureSession: CameraCaptureSession? = null
    @Volatile
    var surface: Surface? = null

    fun close() {
        try {
            captureSession?.close()
        } catch (e: Exception) {
            Log.e("CameraHolder", "Error closing captureSession", e)
        }
        try {
            cameraDevice?.close()
        } catch (e: Exception) {
            Log.e("CameraHolder", "Error closing cameraDevice", e)
        }
        try {
            surface?.release()
        } catch (e: Exception) {
            Log.e("CameraHolder", "Error releasing surface", e)
        }
        captureSession = null
        cameraDevice = null
        surface = null
    }
}
