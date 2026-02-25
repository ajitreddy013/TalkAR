package com.talkar.app.ar.video

import android.content.Context
import com.talkar.app.ar.video.backend.BackendVideoFetcherFactory
import com.talkar.app.ar.video.cache.VideoCacheImpl
import com.talkar.app.ar.video.rendering.LipRegionRendererImpl
import com.talkar.app.ar.video.rendering.RenderCoordinatorImpl

/**
 * Factory for creating TalkingPhotoController instances with all dependencies.
 */
object TalkingPhotoControllerFactory {
    
    /**
     * Creates a TalkingPhotoController with default implementations.
     * 
     * @param context Android context
     * @return Configured TalkingPhotoController instance
     */
    fun create(context: Context): TalkingPhotoController {
        val backendFetcher = BackendVideoFetcherFactory.create()
        val videoCache = VideoCacheImpl(context)
        val videoDecoder = ExoPlayerVideoDecoder(context)
        val lipRenderer = LipRegionRendererImpl()
        val renderCoordinator = RenderCoordinatorImpl()
        
        return TalkingPhotoControllerImpl(
            context = context,
            backendFetcher = backendFetcher,
            videoCache = videoCache,
            videoDecoder = videoDecoder,
            lipRenderer = lipRenderer,
            renderCoordinator = renderCoordinator
        )
    }
}
