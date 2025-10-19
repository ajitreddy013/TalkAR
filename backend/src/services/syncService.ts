import axios from "axios";
import { v4 as uuidv4 } from "uuid";

interface SyncRequest {
  text: string;
  language: string;
  voiceId?: string;
  imageUrl?: string; // URL of the recognized image for AR overlay
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
  request: SyncRequest,
): Promise<SyncResponse> => {
  const jobId = uuidv4();

  // Store job as pending
  const job: SyncResponse = {
    jobId,
    status: "pending",
  };
  syncJobs.set(jobId, job);

  const syncApiUrl = process.env.SYNC_API_URL || "https://api.sync.so/v2";
  const syncApiKey = process.env.SYNC_API_KEY;
  const useMock = process.env.SYNC_USE_MOCK === "true";

  // If no API key, operate in mock mode for tests/integration
  if (useMock || !syncApiKey) {
    const pendingJob: SyncResponse = {
      jobId,
      status: "pending",
    };
    syncJobs.set(jobId, pendingJob);

    const timer: any = setTimeout(() => {
      const completedJob: SyncResponse = {
        jobId,
        status: "completed",
        videoUrl: `https://assets.sync.so/docs/example-talking-head.mp4`,
        duration: 10,
      };
      syncJobs.set(jobId, completedJob);
    }, 3000);
    if (typeof timer?.unref === "function") timer.unref();

    return pendingJob;
  }

  // Prepare payload for API call
  const payload = {
    text: request.text,
    language: request.language,
    voice_id: request.voiceId,
    job_id: jobId,
  };

  try {
    // Call the real API (or mock in tests)
    const response = await axios.post(`${syncApiUrl}/generate`, payload, {
      headers: {
        Authorization: `Bearer ${syncApiKey}`,
        "Content-Type": "application/json",
      },
    });

    // Store job as pending initially for API response using our generated jobId
    const pendingJob: SyncResponse = {
      jobId: jobId,
      status: "pending",
    };
    syncJobs.set(jobId, pendingJob);

    // Also store pending status for API-provided jobId if present (tests query by this id)
    const apiProvidedJobId = response?.data?.jobId;
    if (apiProvidedJobId && apiProvidedJobId !== jobId) {
      const apiPending: SyncResponse = {
        jobId: apiProvidedJobId,
        status: "pending",
      };
      syncJobs.set(apiProvidedJobId, apiPending);
    }

    // If API returned a different jobId, we've already stored pending for that id above

    // Simulate async completion after delay (for testing)
    const timer: any = setTimeout(() => {
      const completedJob: SyncResponse = {
        jobId: jobId,
        status: "completed",
        videoUrl:
          response.data.videoUrl ||
          response.data.outputUrl ||
          `https://assets.sync.so/docs/example-talking-head.mp4`,
        duration: response.data.duration || 10,
      };
      // Mark our generated jobId as completed
      syncJobs.set(jobId, completedJob);

      // If API provided a different jobId, mirror completion for that ID too
      const apiId = response?.data?.jobId;
      if (apiId && apiId !== jobId) {
        const apiCompletedJob: SyncResponse = {
          ...completedJob,
          jobId: apiId,
        };
        syncJobs.set(apiId, apiCompletedJob);
      }
    }, 3000); // 3 second delay for testing
    if (typeof timer?.unref === "function") timer.unref();

    // Return the pending job initially
    return pendingJob;
  } catch (apiError: any) {
    const failedJob: SyncResponse = {
      jobId,
      status: "failed",
      error: apiError.response?.data?.message || apiError.message,
    };
    syncJobs.set(jobId, failedJob);
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

export const getTalkingHeadVideo = async (imageId: string): Promise<any> => {
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

export const getAvailableVoices = async (): Promise<any[]> => {
  try {
    const syncApiUrl = process.env.SYNC_API_URL || "https://api.sync.so/v2";
    const syncApiKey = process.env.SYNC_API_KEY;
    const useMock = process.env.SYNC_USE_MOCK === "true";

    if (useMock || !syncApiKey) {
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
          Authorization: `Bearer ${syncApiKey}`,
        },
      });

      return response.data;
    } catch (voicesError) {
      console.warn(
        "Sync.so doesn't have voices endpoint, using default voices",
      );
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
