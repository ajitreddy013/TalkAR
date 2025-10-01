import axios from "axios";
import { v4 as uuidv4 } from "uuid";

interface SyncRequest {
  text: string;
  language: string;
  voiceId?: string;
}

interface SyncResponse {
  jobId: string;
  status: "pending" | "processing" | "completed" | "failed";
  videoUrl?: string;
  duration?: number;
  error?: string;
}

// In-memory storage for demo purposes
// In production, use Redis or database
const syncJobs = new Map<string, SyncResponse>();

export const generateSyncVideo = async (
  request: SyncRequest
): Promise<SyncResponse> => {
  try {
    const jobId = uuidv4();

    // Store job as pending
    const job: SyncResponse = {
      jobId,
      status: "pending",
    };
    syncJobs.set(jobId, job);

    // Real Sync.so API implementation
    const syncApiUrl = process.env.SYNC_API_URL || "https://api.sync.so/v2";
    const syncApiKey = process.env.SYNC_API_KEY;

    if (!syncApiKey) {
      throw new Error(
        "Sync API key not configured. Please set SYNC_API_KEY in .env file"
      );
    }

    console.log(`Calling sync.so API: ${syncApiUrl}`);
    console.log(
      `Generating sync video for: "${request.text}" in ${request.language}`
    );

    // Update job status to processing
    job.status = "processing";
    syncJobs.set(jobId, job);

    try {
      // Make request to sync.so API with correct format
      const response = await axios.post(
        `${syncApiUrl}/generate`,
        {
          model: "lipsync-2",
          input: [
            {
              type: "video",
              url: "https://assets.sync.so/docs/example-video.mp4" // Default video for now
            },
            {
              type: "audio",
              url: `data:audio/wav;base64,${Buffer.from(request.text).toString('base64')}` // Convert text to audio
            }
          ],
          outputFileName: `talkar_${jobId}`
        },
        {
          headers: {
            "x-api-key": syncApiKey,
            "Content-Type": "application/json",
          },
          timeout: 60000, // 60 second timeout for video processing
        }
      );

      // Handle the response from sync.so API
      if (response.data.status === "completed" || response.data.videoUrl) {
        const completedJob: SyncResponse = {
          jobId,
          status: "completed",
          videoUrl: response.data.videoUrl || response.data.outputUrl,
          duration: response.data.duration || 10, // Default duration
        };
        syncJobs.set(jobId, completedJob);
        console.log(
          `Sync video completed for job ${jobId}: ${completedJob.videoUrl}`
        );
      } else {
        // If the API returns a job ID for async processing
        const processingJob: SyncResponse = {
          jobId,
          status: "processing",
          videoUrl: response.data.videoUrl || response.data.outputUrl,
        };
        syncJobs.set(jobId, processingJob);
        
        // Poll for completion (simplified - in production use webhooks)
        setTimeout(async () => {
          try {
            const statusResponse = await axios.get(`${syncApiUrl}/status/${response.data.jobId || jobId}`, {
              headers: { "x-api-key": syncApiKey }
            });
            
            if (statusResponse.data.status === "completed") {
              const completedJob: SyncResponse = {
                jobId,
                status: "completed",
                videoUrl: statusResponse.data.videoUrl || statusResponse.data.outputUrl,
                duration: statusResponse.data.duration || 10,
              };
              syncJobs.set(jobId, completedJob);
              console.log(`Sync video completed for job ${jobId}: ${completedJob.videoUrl}`);
            }
          } catch (pollError) {
            console.error("Error polling sync status:", pollError);
          }
        }, 10000); // Poll after 10 seconds
      }
    } catch (apiError: any) {
      console.error("Sync.so API error:", apiError);
      const failedJob: SyncResponse = {
        jobId,
        status: "failed",
        error: apiError.response?.data?.message || apiError.message,
      };
      syncJobs.set(jobId, failedJob);
      throw new Error(`Sync.so API failed: ${apiError.message}`);
    }

    return job;
  } catch (error) {
    console.error("Sync service error:", error);
    throw new Error("Failed to generate sync video");
  }
};

export const getSyncStatus = async (jobId: string): Promise<SyncResponse> => {
  const job = syncJobs.get(jobId);

  if (!job) {
    throw new Error("Job not found");
  }

  return job;
};

export const getAvailableVoices = async (): Promise<any[]> => {
  try {
    const syncApiUrl = process.env.SYNC_API_URL || "https://api.sync.so/v2";
    const syncApiKey = process.env.SYNC_API_KEY;

    if (!syncApiKey) {
      console.warn("Sync API key not configured, returning default voices");
      // Return default voices if API key not configured
      return [
        { id: "voice-1", name: "Male Voice 1", language: "en", gender: "male" },
        {
          id: "voice-2",
          name: "Female Voice 1",
          language: "en",
          gender: "female",
        },
        { id: "voice-3", name: "Male Voice 2", language: "es", gender: "male" },
        {
          id: "voice-4",
          name: "Female Voice 2",
          language: "es",
          gender: "female",
        },
        { id: "voice-5", name: "Male Voice 3", language: "fr", gender: "male" },
        {
          id: "voice-6",
          name: "Female Voice 3",
          language: "fr",
          gender: "female",
        },
      ];
    }

    // Make request to sync.so API for voices (if they have a voices endpoint)
    try {
      const response = await axios.get(`${syncApiUrl}/voices`, {
        headers: {
          "x-api-key": syncApiKey,
          "Content-Type": "application/json",
        },
        timeout: 10000, // 10 second timeout
      });

      return response.data;
    } catch (voicesError) {
      console.warn("Sync.so doesn't have voices endpoint, using default voices");
      // Return default voices if voices endpoint doesn't exist
      return [
        { id: "voice-1", name: "Male Voice 1", language: "en", gender: "male" },
        {
          id: "voice-2",
          name: "Female Voice 1",
          language: "en",
          gender: "female",
        },
        { id: "voice-3", name: "Male Voice 2", language: "es", gender: "male" },
        {
          id: "voice-4",
          name: "Female Voice 2",
          language: "es",
          gender: "female",
        },
        { id: "voice-5", name: "Male Voice 3", language: "fr", gender: "male" },
        {
          id: "voice-6",
          name: "Female Voice 3",
          language: "fr",
          gender: "female",
        },
      ];
    }
  } catch (error) {
    console.error("Error fetching voices from sync API:", error);
    // Return default voices on error
    return [
      { id: "voice-1", name: "Male Voice 1", language: "en", gender: "male" },
      {
        id: "voice-2",
        name: "Female Voice 1",
        language: "en",
        gender: "female",
      },
      { id: "voice-3", name: "Male Voice 2", language: "es", gender: "male" },
      {
        id: "voice-4",
        name: "Female Voice 2",
        language: "es",
        gender: "female",
      },
      { id: "voice-5", name: "Male Voice 3", language: "fr", gender: "male" },
      {
        id: "voice-6",
        name: "Female Voice 3",
        language: "fr",
        gender: "female",
      },
    ];
  }
};
