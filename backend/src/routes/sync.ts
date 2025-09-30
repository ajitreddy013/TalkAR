import express from 'express';
import { generateSyncVideo, getSyncStatus } from '../services/syncService';
import { validateSyncRequest } from '../middleware/validation';

const router = express.Router();

// Generate sync video
router.post('/generate', validateSyncRequest, async (req, res, next) => {
  try {
    const { text, language, voiceId } = req.body;
    
    const result = await generateSyncVideo({
      text,
      language,
      voiceId
    });
    
    res.json(result);
  } catch (error) {
    next(error);
  }
});

// Get sync status
router.get('/status/:jobId', async (req, res, next) => {
  try {
    const { jobId } = req.params;
    
    const status = await getSyncStatus(jobId);
    
    res.json(status);
  } catch (error) {
    next(error);
  }
});

// Get available voices
router.get('/voices', async (req, res, next) => {
  try {
    const voices = [
      { id: 'voice-1', name: 'Male Voice 1', language: 'en', gender: 'male' },
      { id: 'voice-2', name: 'Female Voice 1', language: 'en', gender: 'female' },
      { id: 'voice-3', name: 'Male Voice 2', language: 'es', gender: 'male' },
      { id: 'voice-4', name: 'Female Voice 2', language: 'es', gender: 'female' },
      { id: 'voice-5', name: 'Male Voice 3', language: 'fr', gender: 'male' },
      { id: 'voice-6', name: 'Female Voice 3', language: 'fr', gender: 'female' }
    ];
    
    res.json(voices);
  } catch (error) {
    next(error);
  }
});

export default router;

