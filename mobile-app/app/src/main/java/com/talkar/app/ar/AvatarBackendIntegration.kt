package com.talkar.app.ar

import android.util.Log
import com.talkar.app.data.api.ApiClient
import com.talkar.app.data.api.ImageAvatarMappingWithDetails
import com.talkar.app.data.api.MapAvatarRequest
import com.talkar.app.data.api.UpdateMappingRequest
import com.talkar.app.data.models.Avatar
import com.talkar.app.data.models.AvatarModel3D
import com.talkar.app.data.models.IdleAnimation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Avatar Backend Integration
 * 
 * Bridges the backend avatar system with the AvatarManager.
 * Fetches avatar configurations from the backend and maps them to 3D models.
 */
class AvatarBackendIntegration(
    private val avatarManager: AvatarManager
) {
    
    private val TAG = "AvatarBackendIntegration"
    private val apiService = ApiClient.create()
    
    /**
     * Fetch all avatars from backend and register them with AvatarManager
     */
    suspend fun syncAvatarsFromBackend(): Result<List<Avatar>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching avatars from backend...")
            
            val response = apiService.getAvatars()
            
            if (response.isSuccessful) {
                val avatars = response.body() ?: emptyList()
                Log.d(TAG, "Fetched ${avatars.size} avatars from backend")
                
                // Convert backend avatars to AvatarModel3D and register
                avatars.forEach { backendAvatar ->
                    val avatarModel = convertToAvatarModel3D(backendAvatar)
                    avatarManager.registerAvatar(avatarModel)
                }
                
                Result.success(avatars)
            } else {
                val error = "Failed to fetch avatars: ${response.code()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing avatars from backend", e)
            Result.failure(e)
        }
    }
    
    /**
     * Fetch avatar for a specific image and register the mapping
     */
    suspend fun getAvatarForImage(imageId: String): Result<Avatar?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching avatar for image: $imageId")
            
            val response = apiService.getAvatarForImage(imageId)
            
            if (response.isSuccessful) {
                val avatar = response.body()
                
                if (avatar != null) {
                    // Convert and register avatar
                    val avatarModel = convertToAvatarModel3D(avatar)
                    avatarManager.registerAvatar(avatarModel)
                    
                    // Map image to avatar
                    avatarManager.mapImageToAvatar(imageId, avatar.id)
                    
                    Log.d(TAG, "Registered avatar ${avatar.name} for image $imageId")
                }
                
                Result.success(avatar)
            } else {
                Log.w(TAG, "No avatar found for image: $imageId")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching avatar for image: $imageId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Fetch complete image data with avatar and script mapping
     */
    suspend fun getCompleteImageData(imageId: String): Result<CompleteImageDataResponse> = 
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching complete image data for: $imageId")
                
                val response = apiService.getCompleteImageData(imageId)
                
                if (response.isSuccessful) {
                    val data = response.body() 
                        ?: return@withContext Result.failure(Exception("Empty response"))
                    
                    // Register avatar if present
                    data.avatar?.let { avatar ->
                        val avatarModel = convertToAvatarModel3D(avatar)
                        avatarManager.registerAvatar(avatarModel)
                        avatarManager.mapImageToAvatar(imageId, avatar.id)
                    }
                    
                    Log.d(TAG, "Complete data fetched for image: ${data.image.name}")
                    
                    Result.success(
                        CompleteImageDataResponse(
                            imageId = data.image.id,
                            imageName = data.image.name,
                            avatar = data.avatar,
                            script = data.mapping?.script,
                            audioUrl = data.mapping?.audioUrl,
                            videoUrl = data.mapping?.videoUrl,
                            visemeDataUrl = data.mapping?.visemeDataUrl
                        )
                    )
                } else {
                    val error = "Failed to fetch complete image data: ${response.code()}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching complete image data for: $imageId", e)
                Result.failure(e)
            }
        }
    
    /**
     * Fetch all image-avatar mappings from backend
     */
    suspend fun syncAllMappingsFromBackend(): Result<List<ImageAvatarMappingWithDetails>> = 
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Syncing all mappings from backend...")
                
                val response = apiService.getAllMappings()
                
                if (response.isSuccessful) {
                    val mappings = response.body() ?: emptyList()
                    Log.d(TAG, "Fetched ${mappings.size} mappings from backend")
                    
                    // Register all avatars and mappings
                    mappings.forEach { mapping ->
                        mapping.avatar?.let { avatar ->
                            val avatarModel = convertToAvatarModel3D(avatar)
                            avatarManager.registerAvatar(avatarModel)
                        }
                        avatarManager.mapImageToAvatar(mapping.imageId, mapping.avatarId)
                    }
                    
                    Result.success(mappings)
                } else {
                    val error = "Failed to sync mappings: ${response.code()}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing mappings from backend", e)
                Result.failure(e)
            }
        }
    
    /**
     * Update mapping with generated media URLs
     */
    suspend fun updateMappingWithMedia(
        mappingId: String,
        audioUrl: String?,
        videoUrl: String?,
        visemeDataUrl: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating mapping $mappingId with media URLs")
            
            val request = UpdateMappingRequest(
                audioUrl = audioUrl,
                videoUrl = videoUrl,
                visemeDataUrl = visemeDataUrl
            )
            
            val response = apiService.updateAvatarMapping(mappingId, request)
            
            if (response.isSuccessful) {
                val result = response.body()
                Log.d(TAG, "Mapping updated: ${result?.message}")
                Result.success(result?.message ?: "Success")
            } else {
                val error = "Failed to update mapping: ${response.code()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating mapping", e)
            Result.failure(e)
        }
    }
    
    /**
     * Map avatar to image with custom script
     */
    suspend fun mapAvatarToImage(
        avatarId: String,
        imageId: String,
        script: String?,
        audioUrl: String? = null,
        videoUrl: String? = null,
        visemeDataUrl: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Mapping avatar $avatarId to image $imageId")
            
            val request = MapAvatarRequest(
                script = script,
                audioUrl = audioUrl,
                videoUrl = videoUrl,
                visemeDataUrl = visemeDataUrl
            )
            
            val response = apiService.mapAvatarToImage(avatarId, imageId, request)
            
            if (response.isSuccessful) {
                val result = response.body()
                
                // Update local mapping
                avatarManager.mapImageToAvatar(imageId, avatarId)
                
                Log.d(TAG, "Avatar mapped: ${result?.message}")
                Result.success(result?.message ?: "Success")
            } else {
                val error = "Failed to map avatar: ${response.code()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error mapping avatar to image", e)
            Result.failure(e)
        }
    }
    
    /**
     * Convert backend Avatar to AvatarModel3D for AR rendering
     */
    private fun convertToAvatarModel3D(backendAvatar: Avatar): AvatarModel3D {
        // Parse idle animation type
        val idleAnimation = when (backendAvatar.idleAnimationType?.lowercase()) {
            "breathing" -> IdleAnimation.BREATHING
            "blinking" -> IdleAnimation.BLINKING
            "breathing_and_blinking", "combined" -> IdleAnimation.BREATHING_AND_BLINKING
            else -> IdleAnimation.BREATHING_AND_BLINKING
        }
        
        return AvatarModel3D(
            id = backendAvatar.id,
            name = backendAvatar.name,
            description = backendAvatar.description ?: "",
            modelUrl = backendAvatar.avatar3DModelUrl, // 3D model URL
            modelResourceId = null, // Not using local resources if backend URL exists
            scale = 1.0f, // Default scale, can be configured
            idleAnimation = idleAnimation,
            mappedImageIds = emptyList(), // Will be set via mapImageToAvatar
            voiceId = backendAvatar.voiceId
        )
    }
}

/**
 * Response data for complete image data
 */
data class CompleteImageDataResponse(
    val imageId: String,
    val imageName: String,
    val avatar: Avatar?,
    val script: String?,
    val audioUrl: String?,
    val videoUrl: String?,
    val visemeDataUrl: String?
)
