package com.talkar.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.talkar.app.ui.components.StreamingAvatarView
import com.talkar.app.data.models.Avatar
import com.talkar.app.data.models.BackendImage
import com.talkar.app.data.models.AdContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StreamingAvatarViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testAvatar = Avatar(
        id = "test-avatar-1",
        name = "Test Avatar",
        description = "A test avatar for streaming",
        avatarImageUrl = "https://example.com/avatar.png",
        avatarVideoUrl = "https://example.com/video.mp4",
        voiceId = "test-voice",
        isActive = true
    )

    private val testImage = BackendImage(
        id = "test-image-1",
        name = "Test Image",
        description = "A test image for AR",
        imageUrl = "https://example.com/image.jpg",
        thumbnailUrl = "https://example.com/thumb.jpg",
        isActive = true
    )

    private val testAdContent = AdContent(
        script = "This is a test script",
        audioUrl = "https://example.com/audio.mp3",
        videoUrl = "https://example.com/video.mp4",
        productName = "Test Product"
    )

    @Test
    fun streamingAvatarView_showsLoadingState() {
        composeTestRule.setContent {
            StreamingAvatarView(
                isVisible = true,
                avatar = null,
                image = null,
                adContent = null,
                isAdContentLoading = true,
                adContentError = null,
                onAvatarTapped = {},
                isTracking = true
            )
        }

        composeTestRule.onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun streamingAvatarView_showsErrorState() {
        composeTestRule.setContent {
            StreamingAvatarView(
                isVisible = true,
                avatar = null,
                image = null,
                adContent = null,
                isAdContentLoading = false,
                adContentError = "Test error message",
                onAvatarTapped = {},
                isTracking = true
            )
        }

        composeTestRule.onNodeWithText("Error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test error message").assertIsDisplayed()
    }

    @Test
    fun streamingAvatarView_showsBufferingState() {
        composeTestRule.setContent {
            StreamingAvatarView(
                isVisible = true,
                avatar = testAvatar,
                image = testImage,
                adContent = testAdContent.copy(videoUrl = ""),
                isAdContentLoading = false,
                adContentError = null,
                onAvatarTapped = {},
                isTracking = true
            )
        }

        composeTestRule.onNodeWithText("Buffering...").assertIsDisplayed()
    }

    @Test
    fun streamingAvatarView_showsEmotionalAvatar_withAudio() {
        composeTestRule.setContent {
            StreamingAvatarView(
                isVisible = true,
                avatar = testAvatar,
                image = testImage,
                adContent = testAdContent.copy(videoUrl = ""),
                isAdContentLoading = false,
                adContentError = null,
                onAvatarTapped = {},
                isTracking = true
            )
        }

        composeTestRule.onNodeWithText("Test Avatar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Image").assertIsDisplayed()
    }

    @Test
    fun streamingAvatarView_showsVideo_whenAvailable() {
        composeTestRule.setContent {
            StreamingAvatarView(
                isVisible = true,
                avatar = testAvatar,
                image = testImage,
                adContent = testAdContent,
                isAdContentLoading = false,
                adContentError = null,
                onAvatarTapped = {},
                isTracking = true
            )
        }

        // After delay, should show video indicator
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Live").assertIsDisplayed()
    }
}