import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';

interface SyncRequest {
  text: string;
  language: string;
  voiceId?: string;
}

interface SyncResponse {
  jobId: string;
  status: 'pending' | 'processing' | 'completed' | 'failed';
  videoUrl?: string;
  duration?: number;
  error?: string;
}

// In-memory storage for demo purposes
// In production, use Redis or database
const syncJobs = new Map<string, SyncResponse>();

export const generateSyncVideo = async (request: SyncRequest): Promise<SyncResponse> => {
  try {
    const jobId = uuidv4();
    
    // Store job as pending
    const job: SyncResponse = {
      jobId,
      status: 'pending'
    };
    syncJobs.set(jobId, job);
    
    // Simulate API call to Sync API
    const syncApiUrl = process.env.SYNC_API_URL || 'https://api.sync.com/v1';
    const syncApiKey = process.env.SYNC_API_KEY;
    
    if (!syncApiKey) {
      throw new Error('Sync API key not configured');
    }
    
    // Make request to Sync API
    const response = await axios.post(`${syncApiUrl}/generate`, {
      text: request.text,
      language: request.language,
      voice_id: request.voiceId,
      job_id: jobId
    }, {
      headers: {
        'Authorization': `Bearer ${syncApiKey}`,
        'Content-Type': 'application/json'
      }
    });
    
    // Update job status
    job.status = 'processing';
    syncJobs.set(jobId, job);
    
    // Simulate processing (in production, this would be handled by webhooks)
    setTimeout(() => {
      const completedJob: SyncResponse = {
        jobId,
        status: 'completed',
        videoUrl: `https://storage.talkar.com/videos/${jobId}.mp4`,
        duration: Math.floor(Math.random() * 30) + 10 // Random duration 10-40 seconds
      };
      syncJobs.set(jobId, completedJob);
    }, 5000); // Simulate 5-second processing
    
    return job;
  } catch (error) {
    console.error('Sync API error:', error);
    throw new Error('Failed to generate sync video');
  }
};

export const getSyncStatus = async (jobId: string): Promise<SyncResponse> => {
  const job = syncJobs.get(jobId);
  
  if (!job) {
    throw new Error('Job not found');
  }
  
  return job;
};

export const getAvailableVoices = async (): Promise<any[]> => {
  try {
    const syncApiUrl = process.env.SYNC_API_URL || 'https://api.sync.com/v1';
    const syncApiKey = process.env.SYNC_API_KEY;
    
    if (!syncApiKey) {
      // Return default voices if API key not configured
      return [
        { id: 'voice-1', name: 'Male Voice 1', language: 'en', gender: 'male' },
        { id: 'voice-2', name: 'Female Voice 1', language: 'en', gender: 'female' },
        { id: 'voice-3', name: 'Male Voice 2', language: 'es', gender: 'male' },
        { id: 'voice-4', name: 'Female Voice 2', language: 'es', gender: 'female' }
      ];
    }
    
    const response = await axios.get(`${syncApiUrl}/voices`, {
      headers: {
        'Authorization': `Bearer ${syncApiKey}`
      }
    });
    
    return response.data;
  } catch (error) {
    console.error('Error fetching voices:', error);
    // Return default voices on error
    return [
      { id: 'voice-1', name: 'Male Voice 1', language: 'en', gender: 'male' },
      { id: 'voice-2', name: 'Female Voice 1', language: 'en', gender: 'female' }
    ];
  }
};

