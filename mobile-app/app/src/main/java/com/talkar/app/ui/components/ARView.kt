package com.talkar.app.ui.components

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ar.core.AugmentedImage
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.SessionPausedException
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.TalkARApplication
import com.talkar.app.data.services.EnhancedARService
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Composable
fun ARView(
    onImageRecognized: (ImageRecognition) -> Unit,
    onAugmentedImageRecognized: (AugmentedImage) -> Unit = {},
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Use global AR service from Application singleton
    val arService = remember { TalkARApplication.instance.enhancedARService }
    
    // Initialize AR service when component is created
    LaunchedEffect(Unit) {
        val initialized = arService.initialize()
        Log.d("ARView", "Global AR service initialized: $initialized")
    }
    
    // Handle lifecycle events
    DisposableEffect(Unit) {
        arService.resumeTracking()
        onDispose {
            arService.pauseTracking()
        }
    }
    
    // Observe recognized images
    val recognizedImages by arService.recognizedImages.collectAsState()
    val isTracking by arService.isTracking.collectAsState()
    val error by arService.error.collectAsState()
    
    // Handle recognized images
    LaunchedEffect(recognizedImages) {
        recognizedImages.forEach { augmentedImage ->
            onAugmentedImageRecognized(augmentedImage)
            
            val imageRecognition = arService.getRecognizedImage(augmentedImage.name ?: "")
            imageRecognition?.let { 
                onImageRecognized(it) 
            }
        }
    }
    
    val delegatingRenderer = remember { DelegatingRenderer() }
    
    AndroidView(
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                setEGLContextClientVersion(2)
                setRenderer(delegatingRenderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                
                // Start checking for session to activate renderer
                post {
                    checkAndActivateRenderer(this, delegatingRenderer, arService, onError)
                }
            }
        },
        modifier = modifier
    )
    
    // Handle errors
    error?.let { errorMessage ->
        onError(errorMessage)
    }
}


private fun checkAndActivateRenderer(
    glSurfaceView: GLSurfaceView,
    delegatingRenderer: DelegatingRenderer,
    arService: EnhancedARService,
    onError: (String) -> Unit
) {
    val session = arService.getSession()
    if (session != null) {
        Log.d("ARView", "Session ready, activating CameraRenderer")
        val cameraRenderer = CameraRenderer(session, arService)
        glSurfaceView.queueEvent {
            delegatingRenderer.updateDelegate(cameraRenderer)
        }
    } else {
        // Retry after a short delay
        glSurfaceView.postDelayed({
            checkAndActivateRenderer(glSurfaceView, delegatingRenderer, arService, onError)
        }, 300) // Slightly faster retry
    }
}

/**
 * A renderer that delegates to another renderer. 
 * Allows switching renderers at runtime without re-creating the GL context.
 */
class DelegatingRenderer : GLSurfaceView.Renderer {
    private var delegate: GLSurfaceView.Renderer? = null
    private val lock = Any()
    
    // Cached state to replay to new delegates
    private var lastGl: GL10? = null
    private var lastConfig: EGLConfig? = null
    private var lastWidth: Int = -1
    private var lastHeight: Int = -1

    fun updateDelegate(newDelegate: GLSurfaceView.Renderer) {
        synchronized(lock) {
            delegate = newDelegate
            
            // Replay state if we have it
            lastGl?.let { gl ->
                lastConfig?.let { config ->
                    newDelegate.onSurfaceCreated(gl, config)
                }
                if (lastWidth != -1 && lastHeight != -1) {
                    newDelegate.onSurfaceChanged(gl, lastWidth, lastHeight)
                }
            }
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        synchronized(lock) {
            lastGl = gl
            lastConfig = config
            delegate?.onSurfaceCreated(gl, config)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        synchronized(lock) {
            lastWidth = width
            lastHeight = height
            delegate?.onSurfaceChanged(gl, width, height)
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        synchronized(lock) {
            delegate?.onDrawFrame(gl)
        }
    }
}

/**
 * Core ARCore camera renderer.
 */
class CameraRenderer(
    private val session: com.google.ar.core.Session,
    private val arService: EnhancedARService
) : GLSurfaceView.Renderer {
    
    private var cameraTextureId = -1
    private var shaderProgram = 0
    private var positionAttrib = 0
    private var texCoordAttrib = 0
    private var textureUniform = 0
    
    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer
    private val inputUvBuffer: FloatBuffer
    private val outputUvBuffer: FloatBuffer
    private val transformedTexCoords = FloatArray(8)
    
    init {
        // Quad vertices for full screen rendering
        val vertices = floatArrayOf(
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
        )
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer.position(0)
        
        // Base texture coordinates
        val texCoords = floatArrayOf(
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
        )
        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(texCoords)
        texCoordBuffer.position(0)
        
        inputUvBuffer = ByteBuffer.allocateDirect(8 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(texCoords)
        inputUvBuffer.position(0)
        
        outputUvBuffer = ByteBuffer.allocateDirect(8 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    }

    private val vertexShaderCode = """
        attribute vec4 a_Position;
        attribute vec2 a_TexCoord;
        varying vec2 v_TexCoord;
        void main() {
            gl_Position = a_Position;
            v_TexCoord = a_TexCoord;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 v_TexCoord;
        uniform samplerExternalOES u_Texture;
        void main() {
            gl_FragColor = texture2D(u_Texture, v_TexCoord);
        }
    """.trimIndent()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        // Generate texture for camera
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        cameraTextureId = textures[0]
        
        // Bind and configure texture
        GLES20.glBindTexture(android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
        GLES20.glTexParameteri(android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        
        // Configure ARCore
        session.setCameraTextureName(cameraTextureId)
        
        // Shaders
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        shaderProgram = GLES20.glCreateProgram().apply {
            GLES20.glAttachShader(this, vertexShader)
            GLES20.glAttachShader(this, fragmentShader)
            GLES20.glLinkProgram(this)
        }
        
        positionAttrib = GLES20.glGetAttribLocation(shaderProgram, "a_Position")
        texCoordAttrib = GLES20.glGetAttribLocation(shaderProgram, "a_TexCoord")
        textureUniform = GLES20.glGetUniformLocation(shaderProgram, "u_Texture")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        // Set display geometry for ARCore
        arService.setDisplayGeometry(Surface.ROTATION_0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        
        try {
            val frame = session.update()
            
            // Draw background
            drawCameraBackground(frame)
            
            // Process image recognition
            if (frame.camera.trackingState == TrackingState.TRACKING) {
                arService.processFrame(frame)
            }
            
        } catch (e: Exception) {
            if (e !is SessionPausedException && e !is CameraNotAvailableException) {
                Log.e("CameraRenderer", "Draw frame error", e)
            }
        }
    }

    private fun drawCameraBackground(frame: Frame) {
        if (cameraTextureId == -1 || shaderProgram == 0) return
        
        GLES20.glUseProgram(shaderProgram)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
        GLES20.glUniform1i(textureUniform, 0)
        
        inputUvBuffer.position(0)
        outputUvBuffer.position(0)
        frame.transformDisplayUvCoords(inputUvBuffer, outputUvBuffer)
        outputUvBuffer.position(0)
        outputUvBuffer.get(transformedTexCoords)
        
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(positionAttrib, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionAttrib)
        
        texCoordBuffer.position(0)
        texCoordBuffer.put(transformedTexCoords)
        texCoordBuffer.position(0)
        GLES20.glVertexAttribPointer(texCoordAttrib, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
        GLES20.glEnableVertexAttribArray(texCoordAttrib)
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        
        GLES20.glDisableVertexAttribArray(positionAttrib)
        GLES20.glDisableVertexAttribArray(texCoordAttrib)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}
