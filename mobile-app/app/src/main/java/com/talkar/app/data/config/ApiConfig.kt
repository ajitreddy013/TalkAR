package com.talkar.app.data.config

import android.util.Log

/**
 * Centralized API configuration for the TalkAR app
 *
 * This class provides a single source of truth for all API endpoints
 * and base URLs to prevent environment drift and inconsistencies.
 */
object ApiConfig {

    private const val TAG = "ApiConfig"

    // Base configuration
    // Prefer BuildConfig values (set per-build/variant) and fall back to these defaults
    private val PROTOCOL: String = try {
        com.talkar.app.BuildConfig.API_PROTOCOL
    } catch (e: Exception) {
        Log.w(TAG, "Failed to read BuildConfig.API_PROTOCOL, using default 'http'", e)
        "http"
    }

    private val HOST: String = try {
        com.talkar.app.BuildConfig.API_HOST
    } catch (e: Exception) {
        Log.w(TAG, "Failed to read BuildConfig.API_HOST, using default '10.17.5.127'", e)
        "10.17.5.127"
    }

    private val PORT: Int = try {
        com.talkar.app.BuildConfig.API_PORT
    } catch (e: Exception) {
        Log.w(TAG, "Failed to read BuildConfig.API_PORT, using default 3000", e)
        3000
    }

    private val API_VERSION: String = try {
        com.talkar.app.BuildConfig.API_VERSION
    } catch (e: Exception) {
        Log.w(TAG, "Failed to read BuildConfig.API_VERSION, using default 'v1'", e)
        "v1"
    }

    // Constructed URLs
    val BASE_URL = "$PROTOCOL://$HOST:$PORT"
    val API_BASE_URL = "$BASE_URL/api"
    val API_V1_URL = "$API_BASE_URL/$API_VERSION"

    // Specific endpoints
    val IMAGES_ENDPOINT = "$API_V1_URL/images"
    val AVATARS_ENDPOINT = "$API_V1_URL/avatars"
    val SYNC_ENDPOINT = "$API_V1_URL/sync"
    val LIPSYNC_ENDPOINT = "$API_V1_URL/lipsync"

    // Helper methods
    fun getFullImageUrl(imagePath: String): String {
        return if (imagePath.startsWith("/")) {
            "$BASE_URL$imagePath"
        } else {
            "$BASE_URL/$imagePath"
        }
    }

    fun getApiEndpoint(path: String): String {
        return if (path.startsWith("/")) {
            "$API_V1_URL$path"
        } else {
            "$API_V1_URL/$path"
        }
    }
}
