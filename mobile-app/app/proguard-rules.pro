# Jetpack Compose State Locks Fix
-dontwarn androidx.compose.runtime.snapshots.SnapshotStateList
-dontwarn androidx.compose.runtime.snapshots.SnapshotStateMap
-keep class androidx.compose.runtime.snapshots.** { *; }

# ML Kit / TensorFlow Lite
-keep class com.google.mlkit.** { *; }
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
