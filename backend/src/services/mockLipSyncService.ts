import { Avatar } from "../models/Avatar";
import { Image, Dialogue } from "../models/Image";

export interface LipSyncRequest {
  imageId: string;
  text: string;
  voiceId?: string;
  language?: string;
}

export interface LipSyncResponse {
  success: boolean;
  videoUrl?: string;
  status: string;
  message?: string;
  processingTime?: number;
}

export class MockLipSyncService {
  private static readonly MOCK_VIDEO_BASE_URL =
    "https://mock-lipsync-videos.com";
  private static readonly PROCESSING_TIME_MS = 2000; // 2 seconds mock processing

  /**
   * Generate mock lip-sync video URL for given text and avatar
   */
  static async generateLipSyncVideo(
    request: LipSyncRequest
  ): Promise<LipSyncResponse> {
    try {
      // Simulate processing time
      await new Promise((resolve) =>
        setTimeout(resolve, this.PROCESSING_TIME_MS)
      );

      // Get image and avatar data
      const image = await Image.findByPk(request.imageId, {
        include: [
          {
            model: Dialogue,
            as: "dialogues",
            where: { isActive: true },
          },
        ],
      });

      if (!image) {
        return {
          success: false,
          status: "error",
          message: "Image not found",
        };
      }

      // Generate mock video URL based on text hash
      const textHash = this.hashString(request.text);
      const videoUrl = `${this.MOCK_VIDEO_BASE_URL}/lipsync/${textHash}.mp4`;

      return {
        success: true,
        videoUrl,
        status: "completed",
        message: "Mock lip-sync video generated successfully",
        processingTime: this.PROCESSING_TIME_MS,
      };
    } catch (error) {
      console.error("Error generating mock lip-sync video:", error);
      return {
        success: false,
        status: "error",
        message: "Failed to generate lip-sync video",
      };
    }
  }

  /**
   * Get lip-sync video status (mock implementation)
   */
  static async getLipSyncStatus(videoId: string): Promise<LipSyncResponse> {
    try {
      // Mock status - always completed for demo
      return {
        success: true,
        videoUrl: `${this.MOCK_VIDEO_BASE_URL}/lipsync/${videoId}.mp4`,
        status: "completed",
        message: "Video is ready",
      };
    } catch (error) {
      console.error("Error getting lip-sync status:", error);
      return {
        success: false,
        status: "error",
        message: "Failed to get video status",
      };
    }
  }

  /**
   * Get available voices for lip-sync
   */
  static async getAvailableVoices(): Promise<
    { id: string; name: string; language: string; gender: string }[]
  > {
    return [
      {
        id: "voice_001",
        name: "Emma (Female)",
        language: "en-US",
        gender: "female",
      },
      {
        id: "voice_002",
        name: "James (Male)",
        language: "en-US",
        gender: "male",
      },
      {
        id: "voice_003",
        name: "Sophie (Female)",
        language: "en-GB",
        gender: "female",
      },
      {
        id: "voice_004",
        name: "David (Male)",
        language: "en-GB",
        gender: "male",
      },
      {
        id: "voice_005",
        name: "Maria (Female)",
        language: "es-ES",
        gender: "female",
      },
      {
        id: "voice_006",
        name: "Carlos (Male)",
        language: "es-ES",
        gender: "male",
      },
      {
        id: "voice_007",
        name: "Marie (Female)",
        language: "fr-FR",
        gender: "female",
      },
      {
        id: "voice_008",
        name: "Pierre (Male)",
        language: "fr-FR",
        gender: "male",
      },
    ];
  }

  /**
   * Generate talking head video with lip-sync
   */
  static async generateTalkingHeadVideo(
    imageId: string,
    text: string,
    voiceId: string = "voice_001"
  ): Promise<LipSyncResponse> {
    try {
      // Simulate processing time
      await new Promise((resolve) =>
        setTimeout(resolve, this.PROCESSING_TIME_MS)
      );

      // Generate mock talking head video URL
      const textHash = this.hashString(`${imageId}-${text}-${voiceId}`);
      const videoUrl = `${this.MOCK_VIDEO_BASE_URL}/talking-head/${textHash}.mp4`;

      return {
        success: true,
        videoUrl,
        status: "completed",
        message: "Mock talking head video generated successfully",
        processingTime: this.PROCESSING_TIME_MS,
      };
    } catch (error) {
      console.error("Error generating talking head video:", error);
      return {
        success: false,
        status: "error",
        message: "Failed to generate talking head video",
      };
    }
  }

  /**
   * Simple hash function for generating consistent IDs
   */
  private static hashString(str: string): string {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = (hash << 5) - hash + char;
      hash = hash & hash; // Convert to 32-bit integer
    }
    return Math.abs(hash).toString(36);
  }
}
