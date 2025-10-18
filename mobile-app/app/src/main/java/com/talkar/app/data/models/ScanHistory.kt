package com.talkar.app.data.models

import java.util.Date

/**
 * Scan History Entry
 * 
 * Represents a single scan session with detected image and avatar
 */
data class ScanHistoryEntry(
    val id: String,
    val imageId: String,
    val imageName: String,
    val imageUrl: String,
    val avatarId: String?,
    val avatarName: String?,
    val avatar3DModelUrl: String?,
    val script: String?,
    val audioUrl: String?,
    val videoUrl: String?,
    val visemeDataUrl: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long = 0, // How long the avatar was displayed (ms)
    val wasPlayed: Boolean = false // Whether the lip-sync was played
) {
    /**
     * Get formatted timestamp
     */
    fun getFormattedTime(): String {
        val date = Date(timestamp)
        val formatter = java.text.SimpleDateFormat("MMM dd, yyyy h:mm a", java.util.Locale.getDefault())
        return formatter.format(date)
    }
    
    /**
     * Get time ago string
     */
    fun getTimeAgo(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            diff < 604800_000 -> "${diff / 86400_000}d ago"
            else -> getFormattedTime()
        }
    }
    
    /**
     * Check if entry is from today
     */
    fun isToday(): Boolean {
        val today = Date()
        val entryDate = Date(timestamp)
        val todayCalendar = java.util.Calendar.getInstance().apply { time = today }
        val entryCalendar = java.util.Calendar.getInstance().apply { time = entryDate }
        
        return todayCalendar.get(java.util.Calendar.YEAR) == entryCalendar.get(java.util.Calendar.YEAR) &&
                todayCalendar.get(java.util.Calendar.DAY_OF_YEAR) == entryCalendar.get(java.util.Calendar.DAY_OF_YEAR)
    }
}

/**
 * Scan History Manager
 * 
 * Manages scan history with persistence
 */
class ScanHistoryManager(private val context: android.content.Context) {
    
    private val TAG = "ScanHistoryManager"
    private val PREFS_NAME = "scan_history"
    private val KEY_HISTORY = "history_entries"
    private val MAX_HISTORY_SIZE = 50
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    private val gson = com.google.gson.Gson()
    
    /**
     * Add scan to history
     */
    fun addScan(entry: ScanHistoryEntry) {
        val history = getHistory().toMutableList()
        
        // Remove duplicate if exists (same imageId within last 5 minutes)
        history.removeAll { 
            it.imageId == entry.imageId && 
            (System.currentTimeMillis() - it.timestamp) < 300_000 
        }
        
        // Add new entry at the beginning
        history.add(0, entry)
        
        // Limit size
        if (history.size > MAX_HISTORY_SIZE) {
            history.subList(MAX_HISTORY_SIZE, history.size).clear()
        }
        
        // Save to preferences
        saveHistory(history)
        
        android.util.Log.d(TAG, "Added scan to history: ${entry.imageName}")
    }
    
    /**
     * Get all history entries
     */
    fun getHistory(): List<ScanHistoryEntry> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<ScanHistoryEntry>>() {}.type
            gson.fromJson<List<ScanHistoryEntry>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to parse history", e)
            emptyList()
        }
    }
    
    /**
     * Get history grouped by date
     */
    fun getGroupedHistory(): Map<String, List<ScanHistoryEntry>> {
        val history = getHistory()
        
        return history.groupBy { entry ->
            when {
                entry.isToday() -> "Today"
                entry.timestamp > System.currentTimeMillis() - 86400_000 -> "Yesterday"
                entry.timestamp > System.currentTimeMillis() - 604800_000 -> "This Week"
                else -> "Older"
            }
        }
    }
    
    /**
     * Clear all history
     */
    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
        android.util.Log.d(TAG, "History cleared")
    }
    
    /**
     * Delete specific entry
     */
    fun deleteEntry(entryId: String) {
        val history = getHistory().toMutableList()
        history.removeAll { it.id == entryId }
        saveHistory(history)
        android.util.Log.d(TAG, "Deleted entry: $entryId")
    }
    
    /**
     * Update entry (e.g., mark as played)
     */
    fun updateEntry(entryId: String, update: (ScanHistoryEntry) -> ScanHistoryEntry) {
        val history = getHistory().toMutableList()
        val index = history.indexOfFirst { it.id == entryId }
        
        if (index != -1) {
            history[index] = update(history[index])
            saveHistory(history)
            android.util.Log.d(TAG, "Updated entry: $entryId")
        }
    }
    
    /**
     * Get statistics
     */
    fun getStatistics(): ScanStatistics {
        val history = getHistory()
        
        return ScanStatistics(
            totalScans = history.size,
            todayScans = history.count { it.isToday() },
            uniqueImages = history.map { it.imageId }.distinct().size,
            playedScans = history.count { it.wasPlayed },
            averageDuration = if (history.isNotEmpty()) 
                history.map { it.duration }.average().toLong() else 0L
        )
    }
    
    /**
     * Save history to preferences
     */
    private fun saveHistory(history: List<ScanHistoryEntry>) {
        val json = gson.toJson(history)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }
}

/**
 * Scan Statistics
 */
data class ScanStatistics(
    val totalScans: Int,
    val todayScans: Int,
    val uniqueImages: Int,
    val playedScans: Int,
    val averageDuration: Long
) {
    fun getPlayRate(): Float {
        return if (totalScans > 0) playedScans.toFloat() / totalScans else 0f
    }
    
    fun getFormattedAverageDuration(): String {
        val seconds = averageDuration / 1000
        return when {
            seconds < 60 -> "${seconds}s"
            else -> "${seconds / 60}m ${seconds % 60}s"
        }
    }
}
