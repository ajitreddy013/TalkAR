package com.talkar.app.ui.components

import android.graphics.Outline
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.talkar.app.R
import com.talkar.app.ar.video.models.LipCoordinates
import java.io.File
import kotlin.math.roundToInt

private const val TAG = "LipSyncOverlay"

/**
 * Overlays the lip region of a Wav2Lip video on the poster's mouth.
 *
 * Clipping strategy:
 *  - PlayerView uses TextureView mode (via XML). TextureView renders into the normal
 *    hardware-layer pipeline so clipToOutline is respected (SurfaceView bypasses it).
 *  - ViewOutlineProvider.setOval() clips to an oval matching the lip bounding box.
 *    The outline tracks the AR pose each frame via invalidateOutline().
 *  - DstIn / CompositingStrategy.Offscreen are NOT used: TextureView pixels are
 *    composited by the system AFTER Compose's off-screen pass, so DstIn never sees
 *    the video content. clipToOutline operates at the RenderNode level, before the
 *    compositor, which is why it works.
 *
 * posterScreenBounds: [left, top, right, bottom] fractions 0..1 of composable area
 * lipCoordinates: normalized lip UV within the poster frame
 * lipScreenBounds: optional directly-projected lip screen fractions (more accurate)
 * onVideoComplete: called once when playback reaches the end
 */
@Composable
fun LipSyncVideoOverlay(
    videoPath: String?,
    posterScreenBounds: FloatArray?,
    lipCoordinates: LipCoordinates?,
    lipScreenBounds: FloatArray? = null,
    onVideoComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (videoPath.isNullOrBlank()) return
    if (posterScreenBounds == null || posterScreenBounds.size < 4) return
    if (lipCoordinates == null) return

    val context = LocalContext.current

    val player = remember(videoPath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(videoPath))))
            repeatMode = ExoPlayer.REPEAT_MODE_OFF
            volume = 1f
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    Log.d(TAG, "video ended — notifying")
                    onVideoComplete()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val cW = constraints.maxWidth.toFloat()
        val cH = constraints.maxHeight.toFloat()
        if (cW < 1f || cH < 1f) return@BoxWithConstraints
        Log.d(TAG, "composable=${cW.toInt()}x${cH.toInt()} posterBounds=[${posterScreenBounds[0]},${posterScreenBounds[1]},${posterScreenBounds[2]},${posterScreenBounds[3]}]")

        val pL = posterScreenBounds[0]
        val pT = posterScreenBounds[1]
        val pR = posterScreenBounds[2]
        val pB = posterScreenBounds[3]

        val fracW = pR - pL
        val fracH = pB - pT
        if (fracW <= 0f || fracH <= 0f || fracW > 3f || fracH > 3f) return@BoxWithConstraints

        val posterWpx = (fracW * cW).roundToInt().coerceIn(1, (cW * 3f).roundToInt())
        val posterHpx = (fracH * cH).roundToInt().coerceIn(1, (cH * 3f).roundToInt())
        val posterX   = (pL * cW).roundToInt()
        val posterY   = (pT * cH).roundToInt()

        // Clip rectangle in PlayerView-local coordinates using linear UV→pixel mapping.
        // The video fills the PlayerView linearly (resize_mode="fill"), so linear UV
        // is the only mapping that places the clip over the correct video content.
        val lipL = (lipCoordinates.lipX                               * posterWpx).roundToInt()
        val lipT = (lipCoordinates.lipY                               * posterHpx).roundToInt()
        val lipR = ((lipCoordinates.lipX + lipCoordinates.lipWidth)   * posterWpx).roundToInt()
        val lipB = ((lipCoordinates.lipY + lipCoordinates.lipHeight)  * posterHpx).roundToInt()

        if ((lipR - lipL) < 4 || (lipB - lipT) < 4) return@BoxWithConstraints

        // Perspective-align: shift the PlayerView so that the UV-mapped video lip CENTER
        // lands exactly on the ARCore-projected physical lip CENTER (lipScreenBounds).
        // When the poster is tilted, ARCore perspective gives the correct on-screen lip
        // position, but the video fills linearly — this offset bridges the gap.
        val pvX: Int
        val pvY: Int
        if (lipScreenBounds != null && lipScreenBounds.size >= 4) {
            val directCX = (lipScreenBounds[0] + lipScreenBounds[2]) / 2f * cW
            val directCY = (lipScreenBounds[1] + lipScreenBounds[3]) / 2f * cH
            val uvCX     = (lipL + lipR) / 2f   // PlayerView-local
            val uvCY     = (lipT + lipB) / 2f
            pvX = (directCX - uvCX).roundToInt()
            pvY = (directCY - uvCY).roundToInt()
        } else {
            pvX = posterX
            pvY = posterY
        }

        Log.d(TAG, "pv=${posterWpx}x${posterHpx}@($pvX,$pvY) lip=($lipL,$lipT,$lipR,$lipB)")

        // DEBUG: green = poster bbox, yellow = actual clip (UV linear, on video lip),
        //        cyan = ARCore projected lip center (perspective reference).
        if (com.talkar.app.BuildConfig.DEBUG) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    color = Color.Green,
                    topLeft = Offset(posterX.toFloat(), posterY.toFloat()),
                    size = Size(posterWpx.toFloat(), posterHpx.toFloat()),
                    style = Stroke(width = 6f)
                )
                val absLipLeft = (pvX + lipL).toFloat()
                val absLipTop  = (pvY + lipT).toFloat()
                drawRect(
                    color = Color.Yellow,
                    topLeft = Offset(absLipLeft, absLipTop),
                    size = Size((lipR - lipL).toFloat(), (lipB - lipT).toFloat()),
                    style = Stroke(width = 6f)
                )
                val cx = absLipLeft + (lipR - lipL) / 2f
                val cy = absLipTop  + (lipB - lipT) / 2f
                drawLine(Color.Red, Offset(cx - 30f, cy), Offset(cx + 30f, cy), strokeWidth = 4f)
                drawLine(Color.Red, Offset(cx, cy - 30f), Offset(cx, cy + 30f), strokeWidth = 4f)
                if (lipScreenBounds != null && lipScreenBounds.size >= 4) {
                    drawRect(
                        color = Color.Cyan,
                        topLeft = Offset(lipScreenBounds[0] * cW, lipScreenBounds[1] * cH),
                        size = Size((lipScreenBounds[2] - lipScreenBounds[0]) * cW,
                                    (lipScreenBounds[3] - lipScreenBounds[1]) * cH),
                        style = Stroke(width = 4f)
                    )
                }
            }
        }

        AndroidView(
            modifier = Modifier.layout { measurable, _ ->
                val placeable = measurable.measure(Constraints.fixed(posterWpx, posterHpx))
                layout(cW.roundToInt(), cH.roundToInt()) {
                    placeable.place(pvX, pvY)
                }
            },
            factory = { ctx ->
                val pv = LayoutInflater.from(ctx)
                    .inflate(R.layout.lip_player_view, null) as PlayerView
                pv.tag = Rect(lipL, lipT, lipR, lipB)
                pv.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        val r = view.tag as? Rect ?: return
                        outline.setOval(r.left, r.top, r.right, r.bottom)
                    }
                }
                pv.clipToOutline = true
                // Hide until the first video frame is decoded — TextureView shows black
                // before any frame arrives, which would flash a black oval over the poster.
                pv.alpha = 0f
                player.addListener(object : Player.Listener {
                    override fun onRenderedFirstFrame() {
                        pv.animate().alpha(1f).setDuration(80).start()
                        player.removeListener(this)
                    }
                })
                pv.player = player
                pv
            },
            update = { pv ->
                pv.player = player
                (pv.tag as? Rect)?.set(lipL, lipT, lipR, lipB)
                pv.invalidateOutline()
            }
        )
    }
}
