import axios from "axios";
import { v4 as uuidv4 } from "uuid";
import supabaseService, { SyncJob } from "./supabaseService";

interface SyncRequest {
  text: string;
  language: string;
  voiceId?: string;
  imageUrl?: string; // URL of the recognized image for AR overlay
  userId: string; // User ID for Supabase integration
}

interface SyncResponse {
  jobId: string;
  status: "pending" | "processing" | "completed" | "failed";
  videoUrl?: string;
  duration?: number;
  error?: string;
}

export const generateSyncVideo = async (
  request: SyncRequest,
): Promise<SyncResponse> => {
  try {
    const jobId = uuidv4();

    // Store job in Supabase
    const jobData = {
      project_id: jobId, // Use as project_id instead
      user_id: request.userId,
      text: request.text,
      language: request.language,
      voice_id: request.voiceId,
      image_url: request.imageUrl,
      status: "pending" as const,
    };

    const createdJob = await supabaseService.createSyncJob(jobData);
    if (!createdJob) {
      throw new Error("Failed to create sync job");
    }

    // Real Sync.so API implementation
    const syncApiUrl = process.env.SYNC_API_URL || "https://api.sync.so/v2";
    const syncApiKey = process.env.SYNC_API_KEY;

    if (!syncApiKey) {
      throw new Error(
        "Sync API key not configured. Please set SYNC_API_KEY in .env file",
      );
    }

    console.log(`Calling sync.so API: ${syncApiUrl}`);
    console.log(
      `Generating sync video for: "${request.text}" in ${request.language}`,
    );

    // Update job status to processing
    await supabaseService.updateSyncJob(jobId, { status: "processing" });

    try {
      // Ensure English language for proper lip-sync
      const language = request.language.toLowerCase() === "en" ? "en" : "en";
      const text = request.text;

      // For TalkAR, we need to:
      // 1. Use the recognized image as the video input
      // 2. Convert the script text to audio using TTS
      // 3. Generate lipsync video

      // First, we need to get the recognized image URL from the request
      const imageUrl =
        request.imageUrl || "https://assets.sync.so/docs/example-video.mp4";

      // For now, we'll use a simple approach:
      // - Use the recognized image as video input
      // - Convert text to audio (in production, use TTS service)
      // - Generate lipsync video

      // TODO: In production, you need to:
      // 1. Convert text to audio using TTS service
      // 2. Upload audio file to get a proper URL
      // 3. Use that audio URL in the sync request

      // For now, let's use a mock response for testing
      console.log(`Mock sync video generation for: "${request.text}"`);

      // Simulate API call delay
      await new Promise((resolve) => setTimeout(resolve, 2000));

      // Mock successful response
      const mockResponse = {
        data: {
          jobId: jobId,
          status: "completed",
          videoUrl: `https://assets.sync.so/docs/example-talking-head.mp4`,
          outputUrl: `https://assets.sync.so/docs/example-talking-head.mp4`,
          duration: 15,
        },
      };

      const response = mockResponse;

      // Handle the response from sync.so API
      if (response.data.status === "completed" || response.data.videoUrl) {
        const completedJob: SyncResponse = {
          jobId,
          status: "completed",
          videoUrl: response.data.videoUrl || response.data.outputUrl,
          duration: response.data.duration || 10, // Default duration
        };

        // Update job in Supabase
        await supabaseService.updateSyncJob(jobId, {
          status: "completed",
          video_url: completedJob.videoUrl,
          duration: completedJob.duration,
        });

        console.log(
          `Sync video completed for job ${jobId}: ${completedJob.videoUrl}`,
        );

        // Return the completed job with video URL
        return completedJob;
      } else {
        // If the API returns a job ID for async processing
        const processingJob: SyncResponse = {
          jobId,
          status: "processing",
          videoUrl: response.data.videoUrl || response.data.outputUrl,
        };

        // Update job in Supabase
        await supabaseService.updateSyncJob(jobId, {
          status: "processing",
          video_url: processingJob.videoUrl,
        });

        // Poll for completion (simplified - in production use webhooks)
        setTimeout(async () => {
          try {
            const statusResponse = await axios.get(
              `${syncApiUrl}/status/${response.data.jobId || jobId}`,
              {
                headers: { "x-api-key": syncApiKey },
              },
            );

            if (statusResponse.data.status === "completed") {
              const completedJobData: Partial<SyncJob> = {
                status: "completed" as const,
                video_url:
                  statusResponse.data.videoUrl || statusResponse.data.outputUrl,
                duration: statusResponse.data.duration || 10,
              };

              await supabaseService.updateSyncJob(jobId, completedJobData);
              console.log(
                `Sync video completed for job ${jobId}: ${completedJobData.video_url}`,
              );
            }
          } catch (pollError) {
            console.error("Error polling sync status:", pollError);
          }
        }, 10000); // Poll after 10 seconds

        // Return the processing job
        return processingJob;
      }
    } catch (apiError: any) {
      console.error("Sync.so API error:", apiError);
      const failedJob: SyncResponse = {
        jobId,
        status: "failed",
        error: apiError.response?.data?.message || apiError.message,
      };

      // Update job status in Supabase
      await supabaseService.updateSyncJob(jobId, {
        status: "failed",
        error: failedJob.error,
      });

      throw new Error(`Sync.so API failed: ${apiError.message}`);
    }
  } catch (error) {
    console.error("Sync service error:", error);
    throw new Error("Failed to generate sync video");
  }
};

export const getSyncStatus = async (jobId: string): Promise<SyncResponse> => {
  try {
    // Get job from Supabase
    const job = await supabaseService.getSyncJob(jobId);

    if (!job) {
      throw new Error("Job not found");
    }

    return {
      jobId: job.id,
      status: job.status,
      videoUrl: job.video_url,
      duration: job.duration,
      error: job.error,
    };
  } catch (error) {
    console.error("Error getting sync status from Supabase:", error);
    throw new Error("Failed to get sync status");
  }
};

export const getUserSyncJobs = async (
  userId: string,
): Promise<SyncResponse[]> => {
  try {
    const jobs = await supabaseService.getUserSyncJobs(userId);

    return jobs.map((job: SyncJob) => ({
      jobId: job.id,
      status: job.status,
      videoUrl: job.video_url,
      duration: job.duration,
      error: job.error,
    }));
  } catch (error) {
    console.error("Error getting user sync jobs from Supabase:", error);
    throw new Error("Failed to get user sync jobs");
  }
};

export const getTalkingHeadVideo = async (
  imageId: string,
  userId?: string,
): Promise<any> => {
  try {
    // For now, return a mock talking head video
    // In production, this would fetch from database or storage
    const mockTalkingHeadVideo = {
      imageId: imageId,
      videoUrl: "https://assets.sync.so/docs/example-talking-head.mp4", // Mock video URL
      duration: 15, // 15 seconds
      title: "Welcome to TalkAR",
      description: "This is a pre-saved talking head video for this image",
      language: "en",
      voiceId: "en-female-1",
      createdAt: new Date().toISOString(),
    };

    console.log(`Returning talking head video for image ${imageId}`);
    return mockTalkingHeadVideo;
  } catch (error) {
    console.error("Error getting talking head video:", error);
    throw new Error("Failed to get talking head video");
  }
};
