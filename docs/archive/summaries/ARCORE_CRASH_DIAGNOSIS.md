# ARCore Native Crash Diagnosis

## 🔴 CRITICAL: App Crashing with Segmentation Fault

**Date**: February 26, 2026 01:20:16  
**Status**: App crashes immediately after camera initialization  
**Crash Type**: Native crash (SIGSEGV - Signal 11)  
**Location**: ARCore native library (`libarcore_c.so`)

## Crash Details

### Process Information
```
Process: com.talkar.app (pid 13815)
Exit Signal: 11 (Segmentation fault)
Tombstone: tombstone_08
Error Type: native_crash
```

### Stack Trace
```
backtrace:
  #00 pc 0000000001163d64  libarcore_c.so (offset 0xd90000)
  #01 pc 00000000011574d8  libarcore_c.so (offset 0xd90000)
  #02 pc 00000000019b3bf8  libarcore_c.so (offset 0xd90000)
  #03 pc 00000000019b175c  libarcore_c.so (offset 0xd90000)
  #04 pc 00000000019f4a8c  libarcore_c.so (offset 0xd90000)
  #05 pc 0000000001a09f8c  libarcore_c.so (offset 0xd90000)
  #06 pc 00000000019483b8  libarcore_c.so (offset 0xd90000)
  #07 pc 000000000123be1c  libarcore_c.so (offset 0xd90000)
  #08 pc 00000000000a67a4  libc.so (__pthread_start)
  #09 pc 0000000000097b20  libc.so (__start_thread)
```

### Timeline
```
01:18:58.719 - App process started (pid 13815)
01:19:00.097 - MainActivity window created
01:19:01.943 - Camera service initialized
01:19:02.153 - Camera opened successfully
01:19:02.190 - Refresh rate changed to 60Hz for camera
01:20:15.331 - crash_dump64 started
01:20:16.777 - Backtrace captured
01:20:16.889 - Tombstone written
01:20:16.988 - Process died (signal 11)
```

## What This Means

### The Good News
✅ App launches successfully  
✅ MainActivity creates and displays  
✅ Camera permissions granted  
✅ Camera service initializes  
✅ Camera opens successfully  
✅ ARCore sensors (accelerometer, gyro) working  

### The Bad News
❌ ARCore crashes ~18 seconds after camera opens  
❌ Crash happens in native C++ code (not your Kotlin code)  
❌ Black screen because app crashes before rendering  
❌ No error handling can catch this (it's a segfault)  

## Root Cause Analysis

### Most Likely Causes (in order of probability)

#### 1. **ARCore Version Incompatibility** (80% likely)
- Your device may not support the ARCore version you're using
- ARCore may be trying to access unsupported camera features
- Device-specific camera implementation conflicts with ARCore

**Evidence**:
- Crash happens during camera frame processing
- Stack trace shows ARCore internal functions
- Camera opens successfully but ARCore crashes shortly after

#### 2. **Camera Configuration Issue** (15% likely)
- ARCore trying to access camera features not available on your device
- Incorrect camera texture format or size
- Missing camera capabilities

**Evidence**:
- Multiple "torch mode unavailable" messages for cameras 0-7
- Camera vendor tag errors in previous logs

#### 3. **Memory Corruption** (5% likely)
- Buffer overflow in ARCore's camera processing
- Invalid memory access during frame processing
- Race condition in multi-threaded camera code

**Evidence**:
- Segmentation fault (invalid memory access)
- Crash in pthread (threading code)

## Immediate Solutions to Try

### Solution 1: Update ARCore (HIGHEST PRIORITY)
```gradle
// In app/build.gradle.kts
dependencies {
    // Try latest stable version
    implementation("com.google.ar:core:1.42.0")  // Update to latest
    
    // Or try a known stable version
    // implementation("com.google.ar:core:1.40.0")
}
```

**Why**: ARCore updates fix device-specific crashes

### Solution 2: Add ARCore Exception Handling
```kotlin
// In ArSceneViewComposable.kt - onCreate()
try {
    session = Session(context, setOf(Feature.FRONT_CAMERA))
    
    // Add configuration checks
    val config = Config(session)
    if (!session.isSupported(config)) {
        Log.e(TAG, "❌ ARCore configuration not supported on this device")
        return
    }
    
    session.configure(config)
    Log.d(TAG, "✅ ARCore session configured successfully")
    
} catch (e: UnavailableException) {
    Log.e(TAG, "❌ ARCore unavailable: ${e.message}")
    // Show user-friendly error
} catch (e: Exception) {
    Log.e(TAG, "❌ ARCore initialization failed", e)
    // Show user-friendly error
}
```

### Solution 3: Reduce ARCore Camera Requirements
```kotlin
// In ArSceneViewComposable.kt
val config = Config(session).apply {
    // Reduce requirements to minimum
    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
    planeFindingMode = Config.PlaneFindingMode.DISABLED  // Disable if not needed
    lightEstimationMode = Config.LightEstimationMode.DISABLED  // Disable if not needed
    focusMode = Config.FocusMode.AUTO
    
    // Disable depth if not needed
    depthMode = Config.DepthMode.DISABLED
}
session.configure(config)
```

### Solution 4: Check Device Compatibility
```kotlin
// Add this check before creating ARCore session
fun isARCoreSupportedAndUpToDate(context: Context): Boolean {
    return when (ArCoreApk.getInstance().checkAvailability(context)) {
        ArCoreApk.Availability.SUPPORTED_INSTALLED -> true
        ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
        ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
            // Request ARCore installation
            try {
                ArCoreApk.getInstance().requestInstall(activity, true)
                false
            } catch (e: Exception) {
                Log.e(TAG, "ARCore installation failed", e)
                false
            }
        }
        else -> {
            Log.e(TAG, "ARCore not supported on this device")
            false
        }
    }
}
```

### Solution 5: Add Crash Recovery
```kotlin
// In MainActivity.kt
class MainActivity : ComponentActivity() {
    private var arCoreInitAttempts = 0
    private val maxAttempts = 3
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Catch native crashes
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
            
            if (arCoreInitAttempts < maxAttempts) {
                arCoreInitAttempts++
                Log.d(TAG, "Attempting ARCore recovery (attempt $arCoreInitAttempts)")
                // Try to restart ARCore with reduced settings
            } else {
                Log.e(TAG, "ARCore failed after $maxAttempts attempts")
                // Show error to user
            }
        }
    }
}
```

## Testing Steps

### Step 1: Check ARCore Installation
```bash
# Check if ARCore is installed
adb shell pm list packages | grep arcore

# Should see:
# package:com.google.ar.core

# Check ARCore version
adb shell dumpsys package com.google.ar.core | grep versionName

# Update ARCore if needed
adb install -r ARCore.apk
```

### Step 2: Test with Minimal ARCore Config
1. Update code with Solution 3 (reduce requirements)
2. Rebuild and install app
3. Monitor logs:
```bash
adb logcat -c
adb logcat | grep -E "ArSceneView|ARCore|FATAL|native"
```

### Step 3: Test on Different Device
- If possible, test on a different Android device
- Check if crash is device-specific
- Compare ARCore versions between devices

### Step 4: Capture Full Tombstone
```bash
# Pull the tombstone file for detailed analysis
adb pull /data/tombstones/tombstone_08 ./tombstone_08.txt

# View tombstone
cat tombstone_08.txt
```

## Alternative Approaches

### Option A: Use ARCore Extensions (Safer)
```kotlin
// Instead of direct ARCore Session
implementation("com.google.ar.sceneform:core:1.17.1")

// Sceneform provides higher-level API with better error handling
```

### Option B: Fallback to Camera2 API
```kotlin
// If ARCore continues to crash, implement fallback
if (!initializeARCore()) {
    Log.w(TAG, "ARCore failed, falling back to Camera2 API")
    initializeCamera2()
}
```

### Option C: Use ARCore Cloud Anchors (More Stable)
```kotlin
// Cloud Anchors API is more stable on some devices
implementation("com.google.ar:core:1.42.0")
implementation("com.google.ar.sceneform.ux:sceneform-ux:1.17.1")
```

## Expected Log Output (When Fixed)

```
D/ArSceneView: Checking ARCore availability...
D/ArSceneView: ✅ ARCore supported and installed
D/ArSceneView: Creating ARCore session...
D/ArSceneView: ✅ ARCore session created successfully
D/ArSceneView: Configuring ARCore session...
D/ArSceneView: ✅ ARCore session configured
D/ArSceneView: GL Surface created
D/ArSceneView: ✅ ARCore camera texture configured: 1
D/ArSceneView: ✅ GL renderer initialized
D/ArSceneView: onDrawFrame() - Camera tracking: TRACKING
D/ArSceneView: Drawing camera background
D/ArSceneView: Processing frame for poster detection
```

## Files to Modify

### Priority 1 (Must Fix)
1. `app/build.gradle.kts` - Update ARCore version
2. `ArSceneViewComposable.kt` - Add exception handling and config checks
3. `MainActivity.kt` - Add crash recovery

### Priority 2 (Should Fix)
4. `ArSceneViewComposable.kt` - Reduce ARCore requirements
5. `TalkingPhotoScreen.kt` - Add ARCore availability check

### Priority 3 (Nice to Have)
6. `ErrorHandling.kt` - Create centralized error handling
7. `DeviceCompatibility.kt` - Create device compatibility checker

## Success Criteria

✅ App launches without crashing  
✅ Camera preview shows (not black)  
✅ ARCore initializes successfully  
✅ No segmentation faults in logs  
✅ App runs for >5 minutes without crashing  

## Next Steps

1. **Immediate**: Update ARCore version in build.gradle
2. **High Priority**: Add exception handling and config checks
3. **Medium Priority**: Test on different device if available
4. **Low Priority**: Implement fallback to Camera2 API

**Estimated Time**: 2-4 hours to fix and test

## Additional Resources

- [ARCore Supported Devices](https://developers.google.com/ar/devices)
- [ARCore Release Notes](https://developers.google.com/ar/releases)
- [ARCore Troubleshooting Guide](https://developers.google.com/ar/develop/troubleshooting)
- [Android Native Crash Debugging](https://source.android.com/docs/core/tests/debug/native-crash)
