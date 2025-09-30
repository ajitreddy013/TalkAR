import { generateSyncVideo, getSyncStatus, getAvailableVoices } from '../services/syncService';

// Mock axios
jest.mock('axios');
const mockedAxios = require('axios');

describe('SyncService', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Reset environment variables
    process.env.SYNC_API_URL = 'https://api.sync.com/v1';
    process.env.SYNC_API_KEY = 'test-api-key';
  });

  describe('generateSyncVideo', () => {
    it('should generate sync video successfully', async () => {
      const request = {
        text: 'Hello, welcome to our store!',
        language: 'en',
        voiceId: 'voice-1'
      };

      // Mock successful API response
      mockedAxios.post.mockResolvedValue({
        data: { jobId: 'test-job-id', status: 'processing' }
      });

      const result = await generateSyncVideo(request);

      expect(result).toBeDefined();
      expect(result.jobId).toBeDefined();
      expect(result.status).toBe('pending');
      expect(mockedAxios.post).toHaveBeenCalledWith(
        'https://api.sync.com/v1/generate',
        {
          text: request.text,
          language: request.language,
          voice_id: request.voiceId,
          job_id: result.jobId
        },
        {
          headers: {
            'Authorization': 'Bearer test-api-key',
            'Content-Type': 'application/json'
          }
        }
      );
    });

    it('should handle API errors gracefully', async () => {
      const request = {
        text: 'Hello, welcome to our store!',
        language: 'en',
        voiceId: 'voice-1'
      };

      // Mock API error
      mockedAxios.post.mockRejectedValue(new Error('API Error'));

      await expect(generateSyncVideo(request)).rejects.toThrow('Failed to generate sync video');
    });

    it('should throw error when API key is not configured', async () => {
      delete process.env.SYNC_API_KEY;

      const request = {
        text: 'Hello, welcome to our store!',
        language: 'en',
        voiceId: 'voice-1'
      };

      await expect(generateSyncVideo(request)).rejects.toThrow('Sync API key not configured');
    });

    it('should handle missing voiceId', async () => {
      const request = {
        text: 'Hello, welcome to our store!',
        language: 'en'
      };

      mockedAxios.post.mockResolvedValue({
        data: { jobId: 'test-job-id', status: 'processing' }
      });

      const result = await generateSyncVideo(request);

      expect(result).toBeDefined();
      expect(mockedAxios.post).toHaveBeenCalledWith(
        'https://api.sync.com/v1/generate',
        expect.objectContaining({
          voice_id: undefined
        }),
        expect.any(Object)
      );
    });
  });

  describe('getSyncStatus', () => {
    it('should return job status for existing job', async () => {
      const jobId = 'test-job-id';
      
      // First create a job
      const request = {
        text: 'Hello, welcome to our store!',
        language: 'en',
        voiceId: 'voice-1'
      };

      mockedAxios.post.mockResolvedValue({
        data: { jobId, status: 'processing' }
      });

      await generateSyncVideo(request);

      // Now get status
      const status = await getSyncStatus(jobId);

      expect(status).toBeDefined();
      expect(status.jobId).toBe(jobId);
      expect(status.status).toBe('pending');
    });

    it('should throw error for non-existing job', async () => {
      const jobId = 'non-existing-job-id';

      await expect(getSyncStatus(jobId)).rejects.toThrow('Job not found');
    });
  });

  describe('getAvailableVoices', () => {
    it('should return voices from API when configured', async () => {
      const mockVoices = [
        { id: 'voice-1', name: 'Male Voice 1', language: 'en', gender: 'male' },
        { id: 'voice-2', name: 'Female Voice 1', language: 'en', gender: 'female' }
      ];

      mockedAxios.get.mockResolvedValue({
        data: mockVoices
      });

      const voices = await getAvailableVoices();

      expect(voices).toEqual(mockVoices);
      expect(mockedAxios.get).toHaveBeenCalledWith(
        'https://api.sync.com/v1/voices',
        {
          headers: {
            'Authorization': 'Bearer test-api-key'
          }
        }
      );
    });

    it('should return default voices when API key is not configured', async () => {
      delete process.env.SYNC_API_KEY;

      const voices = await getAvailableVoices();

      expect(voices).toBeDefined();
      expect(voices.length).toBeGreaterThan(0);
      expect(voices[0]).toHaveProperty('id');
      expect(voices[0]).toHaveProperty('name');
      expect(voices[0]).toHaveProperty('language');
      expect(voices[0]).toHaveProperty('gender');
    });

    it('should return default voices on API error', async () => {
      mockedAxios.get.mockRejectedValue(new Error('API Error'));

      const voices = await getAvailableVoices();

      expect(voices).toBeDefined();
      expect(voices.length).toBeGreaterThan(0);
      expect(voices[0]).toHaveProperty('id');
      expect(voices[0]).toHaveProperty('name');
    });
  });

  describe('Integration with job processing', () => {
    it('should simulate job completion after timeout', async () => {
      jest.useFakeTimers();

      const request = {
        text: 'Hello, welcome to our store!',
        language: 'en',
        voiceId: 'voice-1'
      };

      mockedAxios.post.mockResolvedValue({
        data: { jobId: 'test-job-id', status: 'processing' }
      });

      const result = await generateSyncVideo(request);
      const jobId = result.jobId;

      // Fast-forward time to trigger job completion
      jest.advanceTimersByTime(5000);

      // Wait for async operations
      await new Promise(resolve => setImmediate(resolve));

      const status = await getSyncStatus(jobId);

      expect(status.status).toBe('completed');
      expect(status.videoUrl).toBeDefined();
      expect(status.duration).toBeDefined();

      jest.useRealTimers();
    });
  });
});
