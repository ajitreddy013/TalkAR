import express from 'express';
import { generateSyncVideo, getSyncStatus, getAvailableVoices } from '../services/syncService';
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
    const voices = await getAvailableVoices();
    res.json(voices);
  } catch (error) {
    next(error);
  }
});

export default router;

