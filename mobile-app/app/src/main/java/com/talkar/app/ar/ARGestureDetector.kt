package com.talkar.app.ar

import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat

/**
 * Gesture detector for AR interactions.
 * 
 * Handles:
 * - Long press on detected images to trigger video playback
 * - Tap gestures for future interactions
 * 
 * @param context Android context
 * @param onLongPress Callback when user long-presses on screen
 * @param onSingleTap Callback when user taps on screen
 */
class ARGestureDetector(
    context: Context,
    private val onLongPress: (x: Float, y: Float) -> Unit = { _, _ -> },
    private val onSingleTap: (x: Float, y: Float) -> Unit = { _, _ -> }
) {
    
    companion object {
        private const val TAG = "ARGestureDetector"
        
        // Minimum long press duration in milliseconds
        private const val LONG_PRESS_TIMEOUT = 500L
    }
    
    private val gestureDetector: GestureDetectorCompat
    
    init {
        val listener = object : GestureDetector.SimpleOnGestureListener() {
            
            override fun onLongPress(e: MotionEvent) {
                Log.d(TAG, "Long press detected at (${e.x}, ${e.y})")
                onLongPress(e.x, e.y)
            }
            
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                Log.d(TAG, "Single tap detected at (${e.x}, ${e.y})")
                onSingleTap(e.x, e.y)
                return true
            }
            
            override fun onDown(e: MotionEvent): Boolean {
                // Must return true to indicate we want to handle gestures
                return true
            }
        }
        
        gestureDetector = GestureDetectorCompat(context, listener)
    }
    
    /**
     * Processes a touch event.
     * Call this from your view's onTouchEvent method.
     * 
     * @param event The motion event to process
     * @return true if the event was handled
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }
}
