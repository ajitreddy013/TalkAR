import axios from "axios";
import { v4 as uuidv4 } from "uuid";
import { Dialogue, Image } from "../models/Image";
import { Avatar, AvatarAttributes } from "../models/Avatar";
import { ImageAvatarMapping } from "../models/ImageAvatarMapping";
import fs from "fs";
import path from "path";
import { PerformanceMetricsService } from "./performanceMetricsService";
import { AnalyticsService } from "./analyticsService";
import { getPosterById } from "../utils/posterHelper";
import { getUserPreferences } from "../utils/userHelper";
import { optimizedScriptService } from "./optimizedScriptService";
import http from "http";
import { SimpleCache } from "../utils/simpleCache";
import { storeInteraction } from "../utils/memoryHelper";
import { logInteraction } from "../utils/interactionLogger";

interface ScriptGenerationRequest {
  imageId: string;
  language: string;
  emotion?: string;
  productName?: string; // New field for product name
  userPreferences?: {
    language?: string;
    preferred_tone?: string;
  }; // New field for user preferences
}

// New interface for product metadata
interface ProductMetadata {
  image_id: string;
  product_name: string;
  category?: string;
  brand?: string;
  price?: number;
  currency?: string;
  features?: string[];
  description?: string;
  tone?: string;
  language?: string;
  target_audience?: string[];
  keywords?: string[];
}

interface ScriptGenerationResponse {
  text: string;
  language: string;
  emotion?: string;
}

interface AudioGenerationRequest {
  text: string;
  language: string;
  voiceId?: string;
  emotion?: string;
}

interface AudioGenerationResponse {
  audioUrl: string;
  duration: number;
}

interface LipSyncGenerationRequest {
  imageId: string;
  audioUrl: string;
  emotion?: string;
  avatar?: string; // Add avatar field
}

interface LipSyncGenerationResponse {
  videoUrl: string;
  duration: number;
  jobId?: string; // Add jobId field
}

interface AIPipelineJob {
  jobId: string;
  imageId: string;
  status: "pending" | "generating_script" | "converting_audio" | "creating_lipsync" | "completed" | "failed";
  script?: string;
  audioUrl?: string;
  videoUrl?: string;
  error?: string;
  createdAt: Date;
  updatedAt: Date;
}

// In-memory storage for jobs (in production, use Redis or database)
const aiPipelineJobs = new Map<string, AIPipelineJob>();

// In-memory cache for frequently requested content (in production, use Redis)
const contentCache = new Map<string, any>();
const CACHE_TTL = 5 * 60 * 1000; // 5 minutes

// Initialize LRU cache for scripts
const scriptCache = new SimpleCache<string, string>({
  max: 100,
  ttl: 1000 * 60 * 60, // 1 hour
});

// In-memory storage for product metadata
const productMetadataCache = new Map<string, ProductMetadata>();

// Retry configuration
const MAX_RETRIES = 3;
const RETRY_DELAY = 1000; // 1 second

export class AIPipelineService {
  /**
   * Generate complete AI pipeline: script → audio → lipsync
   */
  static async generateAIPipeline(imageId: string, language: string = "en", emotion: string = "neutral"): Promise<{ jobId: string }> {
    const jobId = uuidv4();
    
    // Initialize job
    const job: AIPipelineJob = {
      jobId,
      imageId,
      status: "pending",
      createdAt: new Date(),
      updatedAt: new Date()
    };
    
    aiPipelineJobs.set(jobId, job);
    
    // Start processing in background
    this.processAIPipeline(jobId, imageId, language, emotion);
    
    return { jobId };
  }

  /**
   * Generate complete ad content using dynamic script generation: poster → script → audio → lipsync video
   * Optimized with parallel processing and pre-generation for faster response times
   */
  static async generateAdContentFromPoster(imageId: string, userId?: string): Promise<{
    script: string;
    audioUrl: string;
    videoUrl: string;
    metadata: any;
  }> {
    const requestId = uuidv4();
    const startTime = Date.now();
    
    // Log AI pipeline event
    AnalyticsService.logAIPipelineEvent({
      jobId: requestId,
      eventType: 'ad_content_generation',
      details: `Starting dynamic ad content generation for poster: ${imageId}`,
      status: 'started',
      productName: imageId
    });
    
    // Start performance tracking
    PerformanceMetricsService.startTracking(requestId, imageId);
    
    try {
      // Step 1: Pre-generate placeholder audio while generating script
      const placeholderAudioPromise = this.generatePlaceholderAudio();
      
      // Step 2: Generate dynamic script using the new endpoint (parallel with placeholder audio)
      const scriptStartTime = Date.now();
      const scriptResponse = await this.generateDynamicScript(imageId, userId);
      const scriptDuration = Date.now() - scriptStartTime;
      PerformanceMetricsService.recordScriptGeneration(requestId, scriptDuration);
      
      const { script, language, tone, image_url, product_name } = scriptResponse;
      
      // Step 3: Generate real audio with streaming optimization (replace placeholder)
      const audioStartTime = Date.now();
      const audioResponse = await this.generateAudioStreaming({
        text: script,
        language: language.toLowerCase() === 'hindi' ? 'hi' : 'en',
        emotion: tone
      });
      const audioDuration = Date.now() - audioStartTime;
      const audioStartDelay = audioStartTime - startTime;
      PerformanceMetricsService.recordAudioStart(requestId, audioStartDelay);
      PerformanceMetricsService.recordAudioGeneration(requestId, audioDuration);
      
      // Step 4: Generate lip-sync video in parallel with audio generation
      const videoStartTime = Date.now();
      const lipSyncResponse = await this.generateLipSyncStreaming({
        imageId: imageId,
        audioUrl: audioResponse.audioUrl,
        emotion: tone,
        avatar: `${product_name.replace(/\s+/g, '_')}_avatar.png`
      });
      const videoDuration = Date.now() - videoStartTime;
      const totalDuration = Date.now() - startTime;
      PerformanceMetricsService.recordVideoCompletion(requestId, videoDuration, totalDuration);
      
      // Calculate total response time
      const totalResponseTime = Date.now() - startTime;
      
      // Store interaction in memory
      storeInteraction({
        timestamp: new Date().toISOString(),
        poster_id: imageId,
        product_name,
        script,
        feedback: null
      });
      
      // Log interaction with response time
      logInteraction({
        timestamp: new Date().toISOString(),
        poster_id: imageId,
        script,
        feedback: null,
        response_time: totalResponseTime
      });
      
      // Return the complete ad content with metadata
      return {
        script,
        audioUrl: audioResponse.audioUrl,
        videoUrl: lipSyncResponse.videoUrl,
        metadata: {
          image_id: imageId,
          product_name,
          language,
          tone,
          image_url,
          generated_at: new Date().toISOString(),
          user_id: userId || "anonymous"
        }
      };
    } catch (error) {
      console.error("Dynamic ad content generation error:", error);
      PerformanceMetricsService.recordFailure(requestId, error instanceof Error ? error.message : "Unknown error");
      
      // Log AI pipeline error
      AnalyticsService.logAIPipelineEvent({
        jobId: requestId,
        eventType: 'error',
        details: `Dynamic ad content generation failed for poster: ${imageId} - ${error instanceof Error ? error.message : "Unknown error"}`,
        status: 'failed',
        productName: imageId
      });
      
      throw error;
    }
  }

  /**
   * Generate complete ad content: product → script → audio → lipsync video
   */
  static async generateAdContent(productName: string): Promise<{
    script: string;
    audioUrl: string;
    videoUrl: string;
  }> {
    const requestId = uuidv4();
    const startTime = Date.now();
    
    // Log AI pipeline event
    AnalyticsService.logAIPipelineEvent({
      jobId: requestId,
      eventType: 'ad_content_generation',
      details: `Starting ad content generation for product: ${productName}`,
      status: 'started',
      productName: productName
    });
    
    // Start performance tracking
    PerformanceMetricsService.startTracking(requestId, productName);
    
    try {
      // Validate product name
      if (!productName || typeof productName !== 'string' || productName.trim().length === 0) {
        throw new Error("Invalid product name: must be a non-empty string");
      }

      if (productName.length > 100) {
        throw new Error("Invalid product name: must be less than 100 characters");
      }

      // Step 1: Generate script for the product
      const scriptStartTime = Date.now();
      const script = await this.generateProductScript(productName);
      const scriptDuration = Date.now() - scriptStartTime;
      PerformanceMetricsService.recordScriptGeneration(requestId, scriptDuration);
      
      // Step 2: Convert script to audio with streaming optimization
      const audioStartTime = Date.now();
      const audioResponse = await this.generateAudioStreaming({
        text: script,
        language: "en",
        emotion: "neutral"
      });
      const audioDuration = Date.now() - audioStartTime;
      const audioStartDelay = audioStartTime - startTime;
      PerformanceMetricsService.recordAudioStart(requestId, audioStartDelay);
      PerformanceMetricsService.recordAudioGeneration(requestId, audioDuration);
      
      // Step 3: Generate lip-sync video
      const videoStartTime = Date.now();
      const lipSyncResponse = await this.generateLipSyncStreaming({
        imageId: "default",
        audioUrl: audioResponse.audioUrl,
        emotion: "neutral",
        avatar: `${productName.replace(/\s+/g, '_')}_avatar.png`
      });
      const videoDuration = Date.now() - videoStartTime;
      const totalDuration = Date.now() - startTime;
      PerformanceMetricsService.recordVideoCompletion(requestId, videoDuration, totalDuration);
      
      // Return the complete ad content
      return {
        script,
        audioUrl: audioResponse.audioUrl,
        videoUrl: lipSyncResponse.videoUrl
      };
    } catch (error) {
      console.error("Ad content generation error:", error);
      PerformanceMetricsService.recordFailure(requestId, error instanceof Error ? error.message : "Unknown error");
      
      // Log AI pipeline error
      AnalyticsService.logAIPipelineEvent({
        jobId: requestId,
        eventType: 'error',
        details: `Ad content generation failed for product: ${productName} - ${error instanceof Error ? error.message : "Unknown error"}`,
        status: 'failed',
        productName: productName
      });
      
      // Re-throw with more context
      if (error instanceof Error) {
        throw new Error(`Failed to generate ad content for product "${productName}": ${error.message}`);
      } else {
        throw new Error(`Failed to generate ad content for product "${productName}": Unknown error`);
      }
    }
  }

  /**
   * Generate complete ad content with streaming optimization
   * Implements async/await + parallel API calls for reduced latency
   * Starts lipsync as soon as audio starts generating
   */
  static async generateAdContentStreaming(productName: string): Promise<{
    script: string;
    audioUrl: string;
    videoUrl: string;
  }> {
    const requestId = uuidv4();
    const startTime = Date.now();
    
    // Log AI pipeline event
    AnalyticsService.logAIPipelineEvent({
      jobId: requestId,
      eventType: 'ad_content_generation',
      details: `Starting streaming ad content generation for product: ${productName}`,
      status: 'started',
      productName: productName
    });
    
    // Start performance tracking
    PerformanceMetricsService.startTracking(requestId, productName);
    
    try {
      // Validate product name
      if (!productName || typeof productName !== 'string' || productName.trim().length === 0) {
        throw new Error("Invalid product name: must be a non-empty string");
      }

      if (productName.length > 100) {
        throw new Error("Invalid product name: must be less than 100 characters");
      }

      // Step 1: Pre-generate placeholder audio while generating script
      const placeholderAudioPromise = this.generatePlaceholderAudio();
      
      // Step 2: Generate script for the product (parallel with placeholder audio)
      const scriptStartTime = Date.now();
      
      // Log script generation start
      AnalyticsService.logAIPipelineEvent({
        jobId: requestId,
        eventType: 'script_generation',
        details: `Starting script generation for product: ${productName}`,
        status: 'started',
        productName: productName
      });
      
      const script = await this.generateProductScript(productName);
      const scriptDuration = Date.now() - scriptStartTime;
      PerformanceMetricsService.recordScriptGeneration(requestId, scriptDuration);
      
      // Log script generation completion
      AnalyticsService.logAIPipelineEvent({
        jobId: requestId,
        eventType: 'script_generation',
        details: `Script generation completed for product: ${productName}`,
        status: 'completed',
        productName: productName,
        duration: scriptDuration
      });
      
      // Step 3: Generate real audio with streaming optimization (replace placeholder)
      // Use Promise.all for parallel processing where possible
      const audioStartTime = Date.now();
      const [audioResponse, _] = await Promise.all([
        this.generateAudioStreaming({
          text: script,
          language: "en",
          emotion: "neutral"
        }),
        // Placeholder can be ignored as we now have the real audio
        placeholderAudioPromise
      ]);
      
      // Record audio start delay
      const audioStartDelay = audioStartTime - startTime;
      PerformanceMetricsService.recordAudioStart(requestId, audioStartDelay);
      const audioDuration = Date.now() - audioStartTime;
      PerformanceMetricsService.recordAudioGeneration(requestId, audioDuration);
      
      // Log audio generation start and completion
      AnalyticsService.logAIPipelineEvent({
        jobId: requestId,
        eventType: 'audio_generation',
        details: `Audio generation completed for product: ${productName}`,
        status: 'completed',
        productName: productName,
        duration: audioDuration
      });
      
      // Step 4: Generate lip-sync video as soon as audio is available
      // This enables streaming by starting lipsync immediately after audio generation completes
      const videoStartTime = Date.now();
      
      // Log lipsync generation start
      AnalyticsService.logAIPipelineEvent({
        jobId: requestId,
        eventType: 'lipsync_generation',
        details: `Starting lipsync generation for product: ${productName}`,
        status: 'started',
        productName: productName
      });
      
      const lipSyncResponse = await this.generateLipSyncStreaming({
        imageId: "default",
        audioUrl: audioResponse.audioUrl,
        emotion: "neutral",
        avatar: `${productName.replace(/\s+/g, '_')}_avatar.png`
      });
      const videoDuration = Date.now() - videoStartTime;
      const totalDuration = Date.now() - startTime;
      PerformanceMetricsService.recordVideoCompletion(requestId, videoDuration, totalDuration);
      
      // Log lipsync generation completion
      AnalyticsService.logAIPipelineEvent({
        jobId: requestId,
        eventType: 'lipsync_generation',
        details: `Lipsync generation completed for product: ${productName}`,
        status: 'completed',
        productName: productName,
        duration: videoDuration
      });
      
      // Return the complete ad content
      return {
        script,
        audioUrl: audioResponse.audioUrl,
        videoUrl: lipSyncResponse.videoUrl
      };
    } catch (error) {
      console.error("Ad content generation error:", error);
      PerformanceMetricsService.recordFailure(requestId, error instanceof Error ? error.message : "Unknown error");
      
      // Log AI pipeline error
      AnalyticsService.logAIPipelineEvent({
        jobId: requestId,
        eventType: 'error',
        details: `Ad content generation failed for product: ${productName} - ${error instanceof Error ? error.message : "Unknown error"}`,
        status: 'failed',
        productName: productName
      });
      
      // Re-throw with more context
      if (error instanceof Error) {
        throw new Error(`Failed to generate ad content for product "${productName}": ${error.message}`);
      } else {
        throw new Error(`Failed to generate ad content for product "${productName}": Unknown error`);
      }
    }
  }

  /**
   * Process the complete AI pipeline asynchronously
   */
  private static async processAIPipeline(jobId: string, imageId: string, language: string, emotion: string): Promise<void> {
    try {
      // Update job status
      this.updateJobStatus(jobId, "generating_script");
      
      // Step 1: Generate script with retry logic
      const scriptResponse = await this.retryOperation(
        () => this.generateScript({ imageId, language, emotion }),
        MAX_RETRIES,
        RETRY_DELAY
      );
      
      // Update job with script
      const job = aiPipelineJobs.get(jobId);
      if (!job) throw new Error("Job not found");
      
      job.script = scriptResponse.text;
      job.updatedAt = new Date();
      aiPipelineJobs.set(jobId, job);
      
      // Save script to file
      await this.saveScriptToFile(scriptResponse.text, imageId, language, emotion);
      
      // Update job status
      this.updateJobStatus(jobId, "converting_audio");
      
      // Step 2: Convert script to audio with retry logic
      const audioResponse = await this.retryOperation(
        () => this.generateAudioStreaming({
          text: scriptResponse.text,
          language: scriptResponse.language,
          emotion: scriptResponse.emotion
        }),
        MAX_RETRIES,
        RETRY_DELAY
      );
      
      // Update job with audio URL
      job.audioUrl = audioResponse.audioUrl;
      job.updatedAt = new Date();
      aiPipelineJobs.set(jobId, job);
      
      // Update job status
      this.updateJobStatus(jobId, "creating_lipsync");
      
      // Step 3: Create lip-sync video with retry logic
      const lipsyncResponse = await this.retryOperation(
        () => this.generateLipSyncStreaming({
          imageId,
          audioUrl: audioResponse.audioUrl,
          emotion: scriptResponse.emotion
        }),
        MAX_RETRIES,
        RETRY_DELAY
      );
      
      // Update job with video URL
      job.videoUrl = lipsyncResponse.videoUrl;
      job.updatedAt = new Date();
      aiPipelineJobs.set(jobId, job);
      
      // Complete job
      this.updateJobStatus(jobId, "completed");
    } catch (error) {
      console.error("AI Pipeline error:", error);
      const job = aiPipelineJobs.get(jobId);
      if (job) {
        job.status = "failed";
        job.error = error instanceof Error ? error.message : "Unknown error";
        job.updatedAt = new Date();
        aiPipelineJobs.set(jobId, job);
      }
    }
  }

  /**
   * Generate placeholder audio for faster initial response
   */
  static async generatePlaceholderAudio(): Promise<AudioGenerationResponse> {
    // Generate a short placeholder audio (1-2 seconds) for immediate feedback
    const placeholderText = "Welcome to TalkAR";
    return {
      audioUrl: "https://assets.sync.so/docs/placeholder-audio.mp3",
      duration: 2 // 2 seconds placeholder
    };
  }

  /**
   * Generate script based on image context
   */
  static async generateScript(request: ScriptGenerationRequest): Promise<ScriptGenerationResponse> {
    try {
      // Check cache first
      const cacheKey = `script:${request.imageId}:${request.language}:${request.emotion || 'neutral'}:${request.productName || 'default'}`;
      const cachedResult = this.getFromCache(cacheKey);
      if (cachedResult) {
        console.log("Returning cached script");
        return cachedResult;
      }

      // Load product metadata if available
      const productMetadata = await this.loadProductMetadata(request.imageId);
      
      // If we have product metadata, use it for enhanced script generation
      if (productMetadata) {
        console.log("Using product metadata for script generation");
        const result = await this.generateScriptFromMetadata(productMetadata, request.userPreferences);
        this.setInCache(cacheKey, result);
        return result;
      }

      // In development, use mock implementation
      if (process.env.NODE_ENV === "development" || (!process.env.OPENAI_API_KEY && !process.env.GROQCLOUD_API_KEY)) {
        console.log("Using mock script generation");
        const result = this.generateMockScript(request);
        this.setInCache(cacheKey, result);
        return result;
      }

      // Determine which AI provider to use
      const aiProvider = process.env.AI_PROVIDER || "openai"; // Default to OpenAI

      let result: ScriptGenerationResponse;
      if (aiProvider === "groq" && process.env.GROQCLOUD_API_KEY) {
        console.log("Calling GroqCloud API for script generation");
        result = await this.callGroqCloudAPI(request);
      } else {
        console.log("Calling OpenAI API for script generation");
        result = await this.callOpenAIAPI(request);
      }

      this.setInCache(cacheKey, result);
      return result;
    } catch (error) {
      console.error("Script generation error:", error);
      // Fallback to mock implementation if real API fails
      console.log("Falling back to mock script generation");
      return this.generateMockScript(request);
    }
  }

  /**
   * Generate script based on product metadata
   */
  private static async generateScriptFromMetadata(metadata: ProductMetadata, userPreferences?: { language?: string; preferred_tone?: string }): Promise<ScriptGenerationResponse> {
    try {
      // Create enhanced prompt using product metadata and user preferences
      const prompt = this.createMetadataBasedPrompt(metadata, userPreferences);
      
      // Create request object for existing AI functions
      const request: ScriptGenerationRequest = {
        imageId: metadata.image_id,
        language: metadata.language || userPreferences?.language || "en",
        emotion: metadata.tone || userPreferences?.preferred_tone || "neutral",
        productName: metadata.product_name
      };

      // In development, use mock implementation
      if (process.env.NODE_ENV === "development" || (!process.env.OPENAI_API_KEY && !process.env.GROQCLOUD_API_KEY)) {
        console.log("Using mock metadata-based script generation");
        const result = this.generateMockMetadataScript(metadata, userPreferences);
        return result;
      }

      // Determine which AI provider to use
      const aiProvider = process.env.AI_PROVIDER || "openai"; // Default to OpenAI

      let result: ScriptGenerationResponse;
      if (aiProvider === "groq" && process.env.GROQCLOUD_API_KEY) {
        console.log("Calling GroqCloud API for metadata-based script generation");
        result = await this.callGroqCloudAPI(request);
      } else {
        console.log("Calling OpenAI API for metadata-based script generation");
        result = await this.callOpenAIAPI(request);
      }

      return result;
    } catch (error) {
      console.error("Metadata-based script generation error:", error);
      // Fallback to mock implementation if real API fails
      console.log("Falling back to mock metadata-based script generation");
      return this.generateMockMetadataScript(metadata, userPreferences);
    }
  }

  /**
   * Create enhanced prompt based on product metadata
   */
  private static createMetadataBasedPrompt(metadata: ProductMetadata, userPreferences?: { language?: string; preferred_tone?: string }): string {
    // Build features string if available
    const featuresString = metadata.features && metadata.features.length > 0 
      ? metadata.features.map((feature, index) => `${index + 1}. ${feature}`).join("\n")
      : "High-quality product";

    // Use tone from metadata, user preferences, or default to "friendly"
    const tone = metadata.tone || userPreferences?.preferred_tone || "friendly";
    const language = metadata.language || userPreferences?.language || "English";

    // Build prompt with metadata
    return `Generate a 2-line voiceover for an advertisement about ${metadata.product_name}.
Category: ${metadata.category || "General"}
Brand: ${metadata.brand || "Premium"}
Tone: ${tone}
Language: ${language}

Product Description: ${metadata.description || "A premium product"}
Key Features:
${featuresString}

Price: ${metadata.currency || "USD"} ${metadata.price || "N/A"}

Create an engaging, concise advertisement script that highlights the product's value proposition and appeals to the target audience.
The tone should be ${tone} - ${this.getToneDescription(tone)}.`;
  }

  /**
   * Get description for a given tone
   */
  private static getToneDescription(tone: string): string {
    const toneDescriptions: Record<string, string> = {
      friendly: "warm, approachable, and welcoming",
      excited: "energetic, enthusiastic, and vibrant",
      professional: "formal, authoritative, and business-oriented",
      casual: "relaxed, informal, and conversational",
      enthusiastic: "passionate, eager, and optimistic",
      persuasive: "convincing, compelling, and influential"
    };

    return toneDescriptions[tone] || toneDescriptions["friendly"];
  }

  /**
   * Generate audio from text
   */
  static async generateAudio(request: AudioGenerationRequest): Promise<AudioGenerationResponse> {
    try {
      // Validate request parameters
      if (!request.text || request.text.length === 0) {
        throw new Error("Text is required for audio generation");
      }

      if (request.text.length > 5000) {
        throw new Error("Text too long for audio generation (max 5000 characters)");
      }

      if (request.language && request.language.length !== 2) {
        throw new Error("Invalid language code (should be 2 characters)");
      }

      // Validate emotion if provided
      const validEmotions = ["neutral", "happy", "surprised", "serious"];
      if (request.emotion && !validEmotions.includes(request.emotion)) {
        throw new Error(`Invalid emotion. Valid emotions: ${validEmotions.join(", ")}`);
      }

      // Check cache first
      const cacheKey = `audio:${request.text}:${request.language}:${request.emotion || 'neutral'}`;
      const cachedResult = this.getFromCache(cacheKey);
      if (cachedResult) {
        console.log("Returning cached audio");
        return cachedResult;
      }

      // In development, use mock implementation
      if (process.env.NODE_ENV === "development" || (!process.env.ELEVENLABS_API_KEY && !process.env.GOOGLE_CLOUD_TTS_API_KEY)) {
        console.log("Using mock audio generation");
        const result = this.generateMockAudio(request);
        this.setInCache(cacheKey, result);
        return result;
      }

      // Determine which TTS provider to use
      const ttsProvider = process.env.TTS_PROVIDER || "elevenlabs"; // Default to ElevenLabs

      let result: AudioGenerationResponse;
      if (ttsProvider === "google" && process.env.GOOGLE_CLOUD_TTS_API_KEY) {
        console.log("Calling Google Cloud TTS API for audio generation");
        result = await this.callGoogleCloudTTS(request);
      } else {
        console.log("Calling ElevenLabs API for audio generation");
        result = await this.callElevenLabsAPI(request);
      }

      this.setInCache(cacheKey, result);
      return result;
    } catch (error) {
      console.error("Audio generation error:", error);
      // Fallback to mock implementation if real API fails
      console.log("Falling back to mock audio generation");
      return this.generateMockAudio(request);
    }
  }

  /**
   * Generate audio with streaming optimization for faster start times
   * This method prioritizes getting the first part of audio quickly
   */
  static async generateAudioStreaming(request: AudioGenerationRequest): Promise<AudioGenerationResponse> {
    try {
      // Validate request parameters
      if (!request.text || request.text.length === 0) {
        throw new Error("Text is required for audio generation");
      }

      // For short texts, use regular generation
      if (request.text.length < 100) {
        return await this.generateAudio(request);
      }

      // For longer texts, prioritize getting audio quickly by using optimized settings
      const optimizedRequest = {
        ...request,
        // Use faster, less quality-intensive settings for initial audio
        emotion: request.emotion || "neutral"
      };

      // Check cache first
      const cacheKey = `audio:${request.text}:${request.language}:${request.emotion || 'neutral'}`;
      const cachedResult = this.getFromCache(cacheKey);
      if (cachedResult) {
        console.log("Returning cached audio for streaming");
        return cachedResult;
      }

      // In development, use mock implementation
      if (process.env.NODE_ENV === "development" || (!process.env.ELEVENLABS_API_KEY && !process.env.GOOGLE_CLOUD_TTS_API_KEY)) {
        console.log("Using mock audio generation for streaming");
        const result = this.generateMockAudio(request);
        this.setInCache(cacheKey, result);
        return result;
      }

      // Determine which TTS provider to use
      const ttsProvider = process.env.TTS_PROVIDER || "elevenlabs"; // Default to ElevenLabs

      let result: AudioGenerationResponse;
      if (ttsProvider === "google" && process.env.GOOGLE_CLOUD_TTS_API_KEY) {
        console.log("Calling Google Cloud TTS API for streaming audio generation");
        result = await this.callGoogleCloudTTS(request);
      } else {
        console.log("Calling ElevenLabs API for streaming audio generation");
        result = await this.callElevenLabsAPI(request);
      }

      this.setInCache(cacheKey, result);
      return result;
    } catch (error) {
      console.error("Streaming audio generation error:", error);
      // Fallback to regular audio generation
      console.log("Falling back to regular audio generation");
      return this.generateAudio(request);
    }
  }

  /**
   * Generate lip-sync video
   */
  static async generateLipSync(request: LipSyncGenerationRequest): Promise<LipSyncGenerationResponse> {
    try {
      // Check cache first
      const cacheKey = `lipsync:${request.imageId}:${request.audioUrl}:${request.emotion || 'neutral'}`;
      const cachedResult = this.getFromCache(cacheKey);
      if (cachedResult) {
        console.log("Returning cached lip-sync video");
        return cachedResult;
      }

      // In development, use mock implementation
      if (process.env.NODE_ENV === "development" || !process.env.SYNC_API_KEY) {
        console.log("Using mock lip-sync generation");
        const result = this.generateMockLipSync(request);
        this.setInCache(cacheKey, result);
        return result;
      }

      // In production, call Sync.so API
      console.log("Calling Sync.so API for lip-sync generation");
      const result = await this.callSyncAPI(request);
      this.setInCache(cacheKey, result);
      return result;
    } catch (error) {
      console.error("Lip-sync generation error:", error);
      // Fallback to mock implementation if real API fails
      console.log("Falling back to mock lip-sync generation");
      return this.generateMockLipSync(request);
    }
  }

  /**
   * Generate lip-sync with streaming optimization for faster render times
   * This method can start processing with partial audio data
   */
  static async generateLipSyncStreaming(request: LipSyncGenerationRequest): Promise<LipSyncGenerationResponse> {
    try {
      // Check cache first
      const cacheKey = `lipsync:${request.imageId}:${request.audioUrl}:${request.emotion || 'neutral'}`;
      const cachedResult = this.getFromCache(cacheKey);
      if (cachedResult) {
        console.log("Returning cached lip-sync video for streaming");
        return cachedResult;
      }

      // In development, use mock implementation
      if (process.env.NODE_ENV === "development" || !process.env.SYNC_API_KEY) {
        console.log("Using mock lip-sync generation for streaming");
        const result = this.generateMockLipSync(request);
        this.setInCache(cacheKey, result);
        return result;
      }

      // In production, call Sync.so API with optimized settings
      console.log("Calling Sync.so API for streaming lip-sync generation");
      const result = await this.callSyncAPI(request);
      this.setInCache(cacheKey, result);
      return result;
    } catch (error) {
      console.error("Streaming lip-sync generation error:", error);
      // Fallback to regular lip-sync generation
      console.log("Falling back to regular lip-sync generation");
      return this.generateLipSync(request);
    }
  }

  /**
   * Get job status
   */
  static async getJobStatus(jobId: string): Promise<AIPipelineJob | null> {
    return aiPipelineJobs.get(jobId) || null;
  }

  /**
   * Update job status
   */
  private static updateJobStatus(jobId: string, status: AIPipelineJob["status"]): void {
    const job = aiPipelineJobs.get(jobId);
    if (job) {
      job.status = status;
      job.updatedAt = new Date();
      aiPipelineJobs.set(jobId, job);
    }
  }

  /**
   * Retry operation with exponential backoff
   */
  private static async retryOperation<T>(
    operation: () => Promise<T>,
    maxRetries: number,
    baseDelay: number
  ): Promise<T> {
    let lastError: Error | null = null;
    
    for (let i = 0; i <= maxRetries; i++) {
      try {
        return await operation();
      } catch (error) {
        lastError = error as Error;
        
        if (i === maxRetries) {
          // Max retries reached, throw the error
          throw lastError;
        }
        
        // Calculate delay with exponential backoff
        const delay = baseDelay * Math.pow(2, i);
        console.log(`Operation failed, retrying in ${delay}ms... (attempt ${i + 1}/${maxRetries + 1})`);
        
        // Wait before retrying
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }
    
    throw lastError || new Error("Retry operation failed");
  }

  /**
   * Get item from cache
   */
  private static getFromCache(key: string): any {
    const cachedItem = contentCache.get(key);
    if (!cachedItem) {
      return null;
    }

    const { value, expiry } = cachedItem;
    if (Date.now() > expiry) {
      // Cache expired, remove it
      contentCache.delete(key);
      return null;
    }

    return value;
  }

  /**
   * Set item in cache
   */
  private static setInCache(key: string, value: any): void {
    const expiry = Date.now() + CACHE_TTL;
    contentCache.set(key, { value, expiry });
  }

  /**
   * Call OpenAI API for script generation (enhanced for GPT-4o-mini)
   */
  private static async callOpenAIAPI(request: ScriptGenerationRequest): Promise<ScriptGenerationResponse> {
    try {
      const openaiApiKey = process.env.OPENAI_API_KEY;
      const openaiApiUrl = "https://api.openai.com/v1/chat/completions";
      
      if (!openaiApiKey) {
        throw new Error("OpenAI API key not configured");
      }

      // Validate request parameters
      if (request.productName && request.productName.length > 100) {
        throw new Error("Product name too long (max 100 characters)");
      }

      if (request.language && request.language.length !== 2) {
        throw new Error("Invalid language code (should be 2 characters)");
      }

      // Create a prompt based on the request type
      let prompt: string;
      if (request.productName) {
        // Product description prompt
        prompt = `Describe the product "${request.productName}" in 2 engaging lines for an advertisement.`;
      } else {
        // Museum guide prompt (existing)
        prompt = `Generate a short, engaging script for an interactive museum guide. 
        The guide should welcome visitors and provide interesting information about the exhibit.
        Language: ${request.language}
        Emotion: ${request.emotion || "neutral"}
        Keep it concise and conversational.`;
      }

      // Validate emotion if provided
      const validEmotions = ["neutral", "happy", "surprised", "serious"];
      if (request.emotion && !validEmotions.includes(request.emotion)) {
        throw new Error(`Invalid emotion. Valid emotions: ${validEmotions.join(", ")}`);
      }

      // Use GPT-4o-mini model
      const response = await axios.post(
        openaiApiUrl,
        {
          model: "gpt-4o-mini",
          messages: [
            {
              role: "system",
              content: request.productName 
                ? "You are a creative marketing assistant specializing in product descriptions. Keep it short and engaging, max 15 words." 
                : "You are an interactive museum guide creating engaging content for visitors. Keep it short and engaging, max 15 words."
            },
            {
              role: "user",
              content: prompt
            }
          ],
          max_tokens: request.productName ? 50 : 75, // Reduced for faster generation
          temperature: 0.7
        },
        {
          headers: {
            "Authorization": `Bearer ${openaiApiKey}`,
            "Content-Type": "application/json"
          },
          timeout: 10000 // 10 second timeout
        }
      );

      const text = response.data.choices[0].message.content.trim();
      
      // Validate response
      if (!text || text.length === 0) {
        throw new Error("Empty response from OpenAI API");
      }

      if (text.length > 500) {
        throw new Error("Response too long from OpenAI API");
      }

      return {
        text,
        language: request.language,
        emotion: request.emotion
      };
    } catch (error: any) {
      console.error("OpenAI API error:", error.response?.data || error.message);
      
      // Handle specific error cases
      if (error.response?.status === 401) {
        throw new Error("Invalid OpenAI API key");
      } else if (error.response?.status === 429) {
        throw new Error("OpenAI API rate limit exceeded");
      } else if (error.response?.status === 400) {
        throw new Error(`OpenAI API bad request: ${error.response.data?.error?.message || 'Invalid request'}`);
      } else if (error.response?.status >= 500) {
        throw new Error("OpenAI API service unavailable");
      } else if (error.code === 'ECONNABORTED') {
        throw new Error("OpenAI API request timeout");
      } else if (error.code === 'ENOTFOUND') {
        throw new Error("OpenAI API service unreachable");
      }
      
      throw new Error(`Failed to generate script with OpenAI API: ${error.message}`);
    }
  }

  /**
   * Call ElevenLabs API for audio generation
   */
  private static async callElevenLabsAPI(request: AudioGenerationRequest): Promise<AudioGenerationResponse> {
    try {
      const elevenLabsApiKey = process.env.ELEVENLABS_API_KEY;
      const elevenLabsApiUrl = "https://api.elevenlabs.io/v1/text-to-speech";
      
      if (!elevenLabsApiKey) {
        throw new Error("ElevenLabs API key not configured");
      }

      // Validate request parameters
      if (!request.text || request.text.length === 0) {
        throw new Error("Text is required for audio generation");
      }

      if (request.text.length > 5000) {
        throw new Error("Text too long for audio generation (max 5000 characters)");
      }

      if (request.language && request.language.length !== 2) {
        throw new Error("Invalid language code (should be 2 characters)");
      }

      // Validate emotion if provided
      const validEmotions = ["neutral", "happy", "surprised", "serious"];
      if (request.emotion && !validEmotions.includes(request.emotion)) {
        throw new Error(`Invalid emotion. Valid emotions: ${validEmotions.join(", ")}`);
      }

      // Select voice based on language and emotion
      const voiceId = this.selectVoiceId(request.language, request.emotion);
      
      // Get HTTP agent from app for keep-alive
      const agent = (global as any).app?.get('httpAgent') as http.Agent || new http.Agent({ keepAlive: true });
      
      const response = await axios.post(
        `${elevenLabsApiUrl}/${voiceId}`,
        {
          text: request.text,
          model_id: "eleven_monolingual_v1",
          voice_settings: {
            stability: 0.5,
            similarity_boost: 0.5
          }
        },
        {
          headers: {
            "xi-api-key": elevenLabsApiKey,
            "Content-Type": "application/json"
          },
          responseType: "arraybuffer",
          timeout: 10000, // Reduced timeout for faster failure
          httpAgent: agent
        }
      );

      // Validate response
      if (!response.data) {
        throw new Error("Empty response from ElevenLabs API");
      }

      // Save audio to file
      const audioBuffer = response.data;
      const audioFilename = await this.saveAudioToFile(audioBuffer, request.text, request.language, request.emotion);
      const audioUrl = `http://localhost:3000/audio/${audioFilename}`;
      
      // Calculate approximate duration based on text length
      const duration = Math.max(5, Math.min(30, request.text.length / 15));

      return {
        audioUrl,
        duration
      };
    } catch (error: any) {
      console.error("ElevenLabs API error:", error.response?.data || error.message);
      
      // Handle specific error cases
      if (error.response?.status === 401) {
        throw new Error("Invalid ElevenLabs API key");
      } else if (error.response?.status === 429) {
        throw new Error("ElevenLabs API rate limit exceeded");
      } else if (error.response?.status === 400) {
        throw new Error(`ElevenLabs API bad request: ${error.response.data?.error?.message || 'Invalid request'}`);
      } else if (error.response?.status >= 500) {
        throw new Error("ElevenLabs API service unavailable");
      } else if (error.code === 'ECONNABORTED') {
        throw new Error("ElevenLabs API request timeout");
      } else if (error.code === 'ENOTFOUND') {
        throw new Error("ElevenLabs API service unreachable");
      }
      
      throw new Error(`Failed to generate audio with ElevenLabs API: ${error.message}`);
    }
  }

  /**
   * Call Google Cloud TTS API for audio generation
   */
  private static async callGoogleCloudTTS(request: AudioGenerationRequest): Promise<AudioGenerationResponse> {
    try {
      const googleCloudTTSApiKey = process.env.GOOGLE_CLOUD_TTS_API_KEY;
      const googleCloudTTSApiUrl = "https://texttospeech.googleapis.com/v1/text:synthesize";
      
      if (!googleCloudTTSApiKey) {
        throw new Error("Google Cloud TTS API key not configured");
      }

      // Validate request parameters
      if (!request.text || request.text.length === 0) {
        throw new Error("Text is required for audio generation");
      }

      if (request.text.length > 5000) {
        throw new Error("Text too long for audio generation (max 5000 characters)");
      }

      if (request.language && request.language.length !== 2) {
        throw new Error("Invalid language code (should be 2 characters)");
      }

      // Validate emotion if provided
      const validEmotions = ["neutral", "happy", "surprised", "serious"];
      if (request.emotion && !validEmotions.includes(request.emotion)) {
        throw new Error(`Invalid emotion. Valid emotions: ${validEmotions.join(", ")}`);
      }

      // Select voice based on language and emotion
      const voiceId = this.selectVoiceId(request.language, request.emotion);
      
      const response = await axios.post(
        `${googleCloudTTSApiUrl}?key=${googleCloudTTSApiKey}`,
        {
          input: {
            text: request.text
          },
          voice: {
            languageCode: request.language,
            ssmlGender: "NEUTRAL",
            name: voiceId
          },
          audioConfig: {
            audioEncoding: "MP3"
          }
        },
        {
          headers: {
            "Content-Type": "application/json"
          },
          responseType: "arraybuffer",
          timeout: 10000 // Reduced timeout for faster failure
        }
      );

      // Validate response
      if (!response.data) {
        throw new Error("Empty response from Google Cloud TTS API");
      }

      // Save audio to file
      const audioBuffer = response.data;
      const audioFilename = await this.saveAudioToFile(audioBuffer, request.text, request.language, request.emotion);
      const audioUrl = `http://localhost:3000/audio/${audioFilename}`;
      
      // Calculate approximate duration based on text length
      const duration = Math.max(5, Math.min(30, request.text.length / 15));

      return {
        audioUrl,
        duration
      };
    } catch (error: any) {
      console.error("Google Cloud TTS API error:", error.response?.data || error.message);
      
      // Handle specific error cases
      if (error.response?.status === 401) {
        throw new Error("Invalid Google Cloud TTS API key");
      } else if (error.response?.status === 429) {
        throw new Error("Google Cloud TTS API rate limit exceeded");
      } else if (error.response?.status === 400) {
        throw new Error(`Google Cloud TTS API bad request: ${error.response.data?.error?.message || 'Invalid request'}`);
      } else if (error.response?.status >= 500) {
        throw new Error("Google Cloud TTS API service unavailable");
      } else if (error.code === 'ECONNABORTED') {
        throw new Error("Google Cloud TTS API request timeout");
      } else if (error.code === 'ENOTFOUND') {
        throw new Error("Google Cloud TTS API service unreachable");
      }
      
      throw new Error(`Failed to generate audio with Google Cloud TTS API: ${error.message}`);
    }
  }

  /**
   * Call GroqCloud API for script generation
   */
  private static async callGroqCloudAPI(request: ScriptGenerationRequest): Promise<ScriptGenerationResponse> {
    try {
      const groqCloudApiKey = process.env.GROQCLOUD_API_KEY;
      const groqCloudApiUrl = "https://api.groqcloud.com/v1/generate";
      
      if (!groqCloudApiKey) {
        throw new Error("GroqCloud API key not configured");
      }

      // Validate request parameters
      if (request.productName && request.productName.length > 100) {
        throw new Error("Product name too long (max 100 characters)");
      }

      if (request.language && request.language.length !== 2) {
        throw new Error("Invalid language code (should be 2 characters)");
      }

      // Create a prompt based on the request type
      let prompt: string;
      if (request.productName) {
        // Product description prompt
        prompt = `Describe the product "${request.productName}" in 2 engaging lines for an advertisement.`;
      } else {
        // Museum guide prompt (existing)
        prompt = `Generate a short, engaging script for an interactive museum guide. 
        The guide should welcome visitors and provide interesting information about the exhibit.
        Language: ${request.language}
        Emotion: ${request.emotion || "neutral"}
        Keep it concise and conversational.`;
      }

      // Validate emotion if provided
      const validEmotions = ["neutral", "happy", "surprised", "serious"];
      if (request.emotion && !validEmotions.includes(request.emotion)) {
        throw new Error(`Invalid emotion. Valid emotions: ${validEmotions.join(", ")}`);
      }

      const response = await axios.post(
        groqCloudApiUrl,
        {
          prompt,
          max_tokens: request.productName ? 100 : 150,
          temperature: 0.7
        },
        {
          headers: {
            "Authorization": `Bearer ${groqCloudApiKey}`,
            "Content-Type": "application/json"
          },
          timeout: 10000 // 10 second timeout
        }
      );

      const text = response.data.choices[0].text.trim();
      
      // Validate response
      if (!text || text.length === 0) {
        throw new Error("Empty response from GroqCloud API");
      }

      if (text.length > 500) {
        throw new Error("Response too long from GroqCloud API");
      }

      return {
        text,
        language: request.language,
        emotion: request.emotion
      };
    } catch (error: any) {
      console.error("GroqCloud API error:", error.response?.data || error.message);
      
      // Handle specific error cases
      if (error.response?.status === 401) {
        throw new Error("Invalid GroqCloud API key");
      } else if (error.response?.status === 429) {
        throw new Error("GroqCloud API rate limit exceeded");
      } else if (error.response?.status === 400) {
        throw new Error(`GroqCloud API bad request: ${error.response.data?.error?.message || 'Invalid request'}`);
      } else if (error.response?.status >= 500) {
        throw new Error("GroqCloud API service unavailable");
      } else if (error.code === 'ECONNABORTED') {
        throw new Error("GroqCloud API request timeout");
      } else if (error.code === 'ENOTFOUND') {
        throw new Error("GroqCloud API service unreachable");
      }
      
      throw new Error(`Failed to generate script with GroqCloud API: ${error.message}`);
    }
  }



  /**
   * Call Sync.so API for lip-sync generation
   */
  private static async callSyncAPI(request: LipSyncGenerationRequest): Promise<LipSyncGenerationResponse> {
    try {
      const syncApiKey = process.env.SYNC_API_KEY;
      const syncApiUrl = process.env.SYNC_API_URL || "https://api.sync.so/v2";
      
      if (!syncApiKey) {
        throw new Error("Sync API key not configured");
      }

      // Validate request parameters
      if (!request.audioUrl) {
        throw new Error("Audio URL is required for lip-sync generation");
      }

      // Prepare request body matching Sync.so API requirements
      const requestBody: any = {
        audio_url: request.audioUrl
      };

      // Add avatar if provided, otherwise use imageId
      let avatarUrl: string;
      if (request.avatar) {
        requestBody.avatar = request.avatar;
        avatarUrl = request.avatar;
      } else {
        // Get image URL from database or use default
        avatarUrl = `https://talkar-image-storage.com/${request.imageId}.jpg`;
        requestBody.avatar = avatarUrl;
      }

      // Add emotion if provided
      if (request.emotion) {
        requestBody.emotion = request.emotion;
      }

      console.log("Calling Sync.so API with request:", requestBody);

      // Get HTTP agent from app for keep-alive
      const agent = (global as any).app?.get('httpAgent') as http.Agent || new http.Agent({ keepAlive: true });
      
      const response = await axios.post(
        `${syncApiUrl}/generate`,
        requestBody,
        {
          headers: {
            "x-api-key": syncApiKey,
            "Content-Type": "application/json"
          },
          timeout: 20000, // Reduced timeout for faster failure
          httpAgent: agent
        }
      );

      // Handle job ID and polling if needed
      if (response.data.jobId) {
        // This is an async job, need to poll for completion
        const jobId = response.data.jobId;
        console.log(`Sync.so job started with ID: ${jobId}`);
        
        // Poll for completion (in a real implementation, this would be done asynchronously)
        // For now, we'll return the job ID and let the client poll
        return {
          videoUrl: response.data.videoUrl || response.data.outputUrl || "",
          duration: response.data.duration || 15,
          jobId: jobId
        };
      } else {
        // Job completed immediately
        const videoUrl = response.data.videoUrl || response.data.outputUrl;
        const duration = response.data.duration || 15;

        // Save video URL to database
        await this.saveVideoUrlToDatabase(request.imageId, videoUrl, avatarUrl, request.emotion);

        return {
          videoUrl,
          duration
        };
      }
    } catch (error: any) {
      console.error("Sync.so API error:", error.response?.data || error.message);
      
      // Handle specific error cases
      if (error.response?.status === 401) {
        throw new Error("Invalid Sync API key");
      } else if (error.response?.status === 429) {
        throw new Error("Sync API rate limit exceeded");
      } else if (error.response?.status === 400) {
        throw new Error(`Sync API bad request: ${error.response.data?.message || 'Invalid request'}`);
      } else if (error.response?.status >= 500) {
        throw new Error("Sync API service unavailable");
      } else if (error.code === 'ECONNABORTED') {
        throw new Error("Sync API request timeout");
      } else if (error.code === 'ENOTFOUND') {
        throw new Error("Sync API service unreachable");
      }
      
      throw new Error(`Failed to generate lip-sync video with Sync.so API: ${error.message}`);
    }
  }

  /**
   * Poll Sync.so API for job completion
   */
  static async pollSyncJob(jobId: string): Promise<LipSyncGenerationResponse> {
    try {
      const syncApiKey = process.env.SYNC_API_KEY;
      const syncApiUrl = process.env.SYNC_API_URL || "https://api.sync.so/v2";
      
      if (!syncApiKey) {
        throw new Error("Sync API key not configured");
      }

      // Poll for job completion
      const maxAttempts = 30; // Maximum polling attempts
      const pollInterval = 2000; // 2 seconds between polls

      for (let attempt = 1; attempt <= maxAttempts; attempt++) {
        console.log(`Polling Sync.so job ${jobId}, attempt ${attempt}/${maxAttempts}`);
        
        try {
          // Get HTTP agent from app for keep-alive
          const agent = (global as any).app?.get('httpAgent') as http.Agent || new http.Agent({ keepAlive: true });
          
          const response = await axios.get(
            `${syncApiUrl}/jobs/${jobId}`,
            {
              headers: {
                "x-api-key": syncApiKey,
                "Content-Type": "application/json"
              },
              timeout: 30000, // 30 second timeout
              httpAgent: agent
            }
          );

          const jobStatus = response.data.status?.toLowerCase();
          
          if (jobStatus === "completed") {
            const videoUrl = response.data.videoUrl || response.data.outputUrl;
            const duration = response.data.duration || 15;
            
            console.log(`Sync.so job ${jobId} completed successfully`);
            return {
              videoUrl,
              duration
            };
          } else if (jobStatus === "failed") {
            throw new Error(`Sync.so job failed: ${response.data.error || 'Unknown error'}`);
          } else if (jobStatus === "processing" || jobStatus === "queued") {
            // Job still processing, wait and retry
            if (attempt < maxAttempts) {
              await new Promise(resolve => setTimeout(resolve, pollInterval));
              continue;
            } else {
              throw new Error(`Sync.so job timed out after ${maxAttempts} attempts`);
            }
          } else {
            throw new Error(`Unknown job status: ${jobStatus}`);
          }
        } catch (pollError: any) {
          if (attempt === maxAttempts) {
            throw new Error(`Failed to poll Sync.so job: ${pollError.message}`);
          }
          // Wait and retry
          await new Promise(resolve => setTimeout(resolve, pollInterval));
        }
      }
      
      throw new Error(`Sync.so job polling timed out`);
    } catch (error) {
      console.error("Sync.so job polling error:", error);
      throw error;
    }
  }

  /**
   * Get Sync.so job status
   */
  static async getSyncJobStatus(jobId: string): Promise<any> {
    try {
      const syncApiKey = process.env.SYNC_API_KEY;
      const syncApiUrl = process.env.SYNC_API_URL || "https://api.sync.so/v2";
      
      if (!syncApiKey) {
        throw new Error("Sync API key not configured");
      }

      // Get HTTP agent from app for keep-alive
      const agent = (global as any).app?.get('httpAgent') as http.Agent || new http.Agent({ keepAlive: true });
      
      const response = await axios.get(
        `${syncApiUrl}/jobs/${jobId}`,
        {
          headers: {
            "x-api-key": syncApiKey,
            "Content-Type": "application/json"
          },
          timeout: 30000, // 30 second timeout
          httpAgent: agent
        }
      );

      return response.data;
    } catch (error: any) {
      console.error("Sync.so job status error:", error.response?.data || error.message);
      throw new Error(`Failed to get Sync.so job status: ${error.message}`);
    }
  }

  /**
   * Stream audio using ElevenLabs streaming API
   */
  static async streamAudio(text: string, response: any): Promise<void> {
    try {
      const elevenLabsApiKey = process.env.ELEVENLABS_API_KEY;
      const elevenLabsApiUrl = "https://api.elevenlabs.io/v1/text-to-speech";
      
      if (!elevenLabsApiKey) {
        throw new Error("ElevenLabs API key not configured");
      }

      // Validate request parameters
      if (!text || text.length === 0) {
        throw new Error("Text is required for audio streaming");
      }

      if (text.length > 5000) {
        throw new Error("Text too long for audio streaming (max 5000 characters)");
      }

      const voiceId = "21m00Tcm4TlvDq8ikWAM"; // Default voice ID
      
      // Get HTTP agent from app for keep-alive
      const agent = (global as any).app?.get('httpAgent') as http.Agent || new http.Agent({ keepAlive: true });
      
      const streamResponse = await axios({
        method: "post",
        url: `${elevenLabsApiUrl}/${voiceId}/stream`,
        data: { 
          text: text,
          model_id: "eleven_monolingual_v1",
          voice_settings: {
            stability: 0.5,
            similarity_boost: 0.5
          }
        },
        headers: { 
          "xi-api-key": elevenLabsApiKey,
          "Content-Type": "application/json"
        },
        responseType: "stream",
        timeout: 10000, // Reduced timeout for faster failure
        httpAgent: agent
      });
      
      streamResponse.data.pipe(response);
    } catch (error: any) {
      console.error("Audio streaming error:", error.response?.data || error.message);
      
      // Handle specific error cases
      if (error.response?.status === 401) {
        throw new Error("Invalid ElevenLabs API key");
      } else if (error.response?.status === 429) {
        throw new Error("ElevenLabs API rate limit exceeded");
      } else if (error.response?.status === 400) {
        throw new Error(`ElevenLabs API bad request: ${error.response.data?.message || 'Invalid request'}`);
      } else if (error.response?.status >= 500) {
        throw new Error("ElevenLabs API service unavailable");
      } else if (error.code === 'ECONNABORTED') {
        throw new Error("ElevenLabs API request timeout");
      } else if (error.code === 'ENOTFOUND') {
        throw new Error("ElevenLabs API service unreachable");
      }
      
      throw new Error(`Failed to stream audio with ElevenLabs API: ${error.message}`);
    }
  }

  /**
   * Generate lip-sync video asynchronously
   */
  static async generateLipSyncAsync(request: LipSyncGenerationRequest): Promise<LipSyncGenerationResponse> {
    try {
      const syncApiKey = process.env.SYNC_API_KEY;
      const syncApiUrl = process.env.SYNC_API_URL || "https://api.sync.so/v2";
      
      if (!syncApiKey) {
        throw new Error("Sync API key not configured");
      }

      // Validate request parameters
      if (!request.audioUrl) {
        throw new Error("Audio URL is required for lip-sync generation");
      }

      // Prepare request body matching Sync.so API requirements
      const requestBody: any = {
        audio_url: request.audioUrl
      };

      // Add avatar if provided, otherwise use imageId
      let avatarUrl: string;
      if (request.avatar) {
        requestBody.avatar = request.avatar;
        avatarUrl = request.avatar;
      } else {
        // Get image URL from database or use default
        avatarUrl = `https://talkar-image-storage.com/${request.imageId}.jpg`;
        requestBody.avatar = avatarUrl;
      }

      // Add emotion if provided
      if (request.emotion) {
        requestBody.emotion = request.emotion;
      }

      console.log("Calling Sync.so API with request:", requestBody);

      // Get HTTP agent from app for keep-alive
      const agent = (global as any).app?.get('httpAgent') as http.Agent || new http.Agent({ keepAlive: true });
      
      const response = await axios.post(
        `${syncApiUrl}/generate`,
        requestBody,
        {
          headers: {
            "x-api-key": syncApiKey,
            "Content-Type": "application/json"
          },
          timeout: 20000, // Reduced timeout for faster failure
          httpAgent: agent
        }
      );

      // Handle job ID and polling if needed
      if (response.data.jobId) {
        // This is an async job, need to poll for completion
        const jobId = response.data.jobId;
        console.log(`Sync.so job started with ID: ${jobId}`);
        
        // Poll for completion (in a real implementation, this would be done asynchronously)
        // For now, we'll return the job ID and let the client poll
        return {
          videoUrl: response.data.videoUrl || response.data.outputUrl || "",
          duration: response.data.duration || 15,
          jobId: jobId
        };
      } else {
        // Job completed immediately
        const videoUrl = response.data.videoUrl || response.data.outputUrl;
        const duration = response.data.duration || 15;

        // Save video URL to database
        await this.saveVideoUrlToDatabase(request.imageId, videoUrl, avatarUrl, request.emotion);

        return {
          videoUrl,
          duration
        };
      }
    } catch (error: any) {
      console.error("Sync.so API error:", error.response?.data || error.message);
      
      // Handle specific error cases
      if (error.response?.status === 401) {
        throw new Error("Invalid Sync API key");
      } else if (error.response?.status === 429) {
        throw new Error("Sync API rate limit exceeded");
      } else if (error.response?.status === 400) {
        throw new Error(`Sync API bad request: ${error.response.data?.message || 'Invalid request'}`);
      } else if (error.response?.status >= 500) {
        throw new Error("Sync API service unavailable");
      } else if (error.code === 'ECONNABORTED') {
        throw new Error("Sync API request timeout");
      } else if (error.code === 'ENOTFOUND') {
        throw new Error("Sync API service unreachable");
      }
      
      throw new Error(`Failed to generate lip-sync video with Sync.so API: ${error.message}`);
    }
  }

  /**
   * Get lip-sync job status
   */
  static async getLipSyncJobStatus(jobId: string): Promise<any> {
    try {
      const syncApiKey = process.env.SYNC_API_KEY;
      const syncApiUrl = process.env.SYNC_API_URL || "https://api.sync.so/v2";
      
      if (!syncApiKey) {
        throw new Error("Sync API key not configured");
      }

      // Get HTTP agent from app for keep-alive
      const agent = (global as any).app?.get('httpAgent') as http.Agent || new http.Agent({ keepAlive: true });
      
      const response = await axios.get(
        `${syncApiUrl}/jobs/${jobId}`,
        {
          headers: {
            "x-api-key": syncApiKey,
            "Content-Type": "application/json"
          },
          timeout: 30000, // 30 second timeout
          httpAgent: agent
        }
      );

      return response.data;
    } catch (error: any) {
      console.error("Sync.so job status error:", error.response?.data || error.message);
      throw new Error(`Failed to get Sync.so job status: ${error.message}`);
    }
  }

  /**
   * Get Google Cloud TTS voice configuration based on language and emotion
   */
  private static getGoogleCloudVoiceConfig(language: string, emotion?: string): { 
    voice: { languageCode: string; name: string; ssmlGender: "SSML_VOICE_GENDER_UNSPECIFIED" | "MALE" | "FEMALE" | "NEUTRAL" },
    speakingRate: number,
    pitch: number
  } {
    // Default voice configuration
    const defaultConfig = {
      voice: {
        languageCode: "en-US",
        name: "en-US-Standard-C",
        ssmlGender: "FEMALE" as const
      },
      speakingRate: 1.0,
      pitch: 0.0
    };

    // Language mappings
    const languageMap: Record<string, { languageCode: string; name: string; ssmlGender: "SSML_VOICE_GENDER_UNSPECIFIED" | "MALE" | "FEMALE" | "NEUTRAL" }> = {
      "en": { languageCode: "en-US", name: "en-US-Standard-C", ssmlGender: "FEMALE" },
      "es": { languageCode: "es-ES", name: "es-ES-Standard-A", ssmlGender: "FEMALE" },
      "fr": { languageCode: "fr-FR", name: "fr-FR-Standard-A", ssmlGender: "FEMALE" }
    };

    // Emotion mappings (affect speaking rate and pitch)
    const emotionMap: Record<string, { speakingRate: number; pitch: number }> = {
      "happy": { speakingRate: 1.1, pitch: 2.0 },
      "serious": { speakingRate: 0.9, pitch: -2.0 },
      "surprised": { speakingRate: 1.2, pitch: 3.0 },
      "neutral": { speakingRate: 1.0, pitch: 0.0 }
    };

    // Get language config
    const langConfig = languageMap[language] || defaultConfig.voice;
    
    // Get emotion config
    const emoConfig = emotionMap[emotion || "neutral"] || emotionMap["neutral"];

    return {
      voice: langConfig,
      speakingRate: emoConfig.speakingRate,
      pitch: emoConfig.pitch
    };
  }

  /**
   * Select appropriate voice ID based on language and emotion
   */
  private static selectVoiceId(language: string, emotion?: string): string {
    // This is a simplified mapping - in a real implementation, you would have
    // a more comprehensive mapping of voices to languages and emotions
    const voiceMap: Record<string, Record<string, string>> = {
      en: {
        neutral: "21m00Tcm4TlvDq8ikWAM",
        happy: "21m00Tcm4TlvDq8ikWAM",
        surprised: "21m00Tcm4TlvDq8ikWAM",
        serious: "21m00Tcm4TlvDq8ikWAM"
      },
      es: {
        neutral: "21m00Tcm4TlvDq8ikWAM",
        happy: "21m00Tcm4TlvDq8ikWAM",
        surprised: "21m00Tcm4TlvDq8ikWAM",
        serious: "21m00Tcm4TlvDq8ikWAM"
      },
      fr: {
        neutral: "21m00Tcm4TlvDq8ikWAM",
        happy: "21m00Tcm4TlvDq8ikWAM",
        surprised: "21m00Tcm4TlvDq8ikWAM",
        serious: "21m00Tcm4TlvDq8ikWAM"
      }
    };

    return voiceMap[language]?.[emotion || "neutral"] || "21m00Tcm4TlvDq8ikWAM";
  }

  /**
   * Save script to file in /scripts/ directory
   */
  private static async saveScriptToFile(script: string, imageId: string, language: string, emotion?: string): Promise<void> {
    try {
      // Validate inputs
      if (!script || script.length === 0) {
        throw new Error("Cannot save empty script to file");
      }

      if (!imageId || imageId.length === 0) {
        throw new Error("Image ID is required to save script");
      }

      if (language && language.length !== 2) {
        throw new Error("Invalid language code for file naming");
      }

      // Create scripts directory if it doesn't exist
      const scriptsDir = path.join(__dirname, "..", "..", "scripts");
      if (!fs.existsSync(scriptsDir)) {
        fs.mkdirSync(scriptsDir, { recursive: true });
      }

      // Create filename based on parameters
      const timestamp = new Date().toISOString().replace(/[:.]/g, "-");
      const emotionSuffix = emotion ? `-${emotion}` : "";
      const filename = `script-${imageId}-${language}${emotionSuffix}-${timestamp}.txt`;
      const filepath = path.join(scriptsDir, filename);

      // Validate file path
      if (filepath.length > 255) {
        throw new Error("File path too long");
      }

      // Write script to file
      fs.writeFileSync(filepath, script, "utf8");
      console.log(`Script saved to ${filepath}`);
    } catch (error) {
      console.error("Error saving script to file:", error);
      // Don't throw error as this is non-critical
    }
  }

  /**
   * Save audio buffer to file in /audio/ directory
   */
  private static async saveAudioToFile(audioBuffer: Buffer, text: string, language: string, emotion?: string): Promise<string> {
    try {
      // Create audio directory if it doesn't exist
      const audioDir = path.join(__dirname, "..", "..", "audio");
      if (!fs.existsSync(audioDir)) {
        fs.mkdirSync(audioDir, { recursive: true });
      }

      // Create filename based on text hash and parameters
      const textHash = this.hashString(text);
      const timestamp = new Date().toISOString().replace(/[:.]/g, "-");
      const emotionSuffix = emotion ? `-${emotion}` : "";
      const filename = `audio-${textHash}-${language}${emotionSuffix}-${timestamp}.mp3`;
      const filepath = path.join(audioDir, filename);

      // Validate file path
      if (filepath.length > 255) {
        throw new Error("File path too long");
      }

      // Write audio buffer to file
      fs.writeFileSync(filepath, audioBuffer);
      console.log(`Audio saved to ${filepath}`);
      
      return filename;
    } catch (error: any) {
      console.error("Error saving audio to file:", error);
      throw new Error(`Failed to save audio file: ${error.message}`);
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

  /**
   * Generate dynamic script using optimized service with caching
   */
  static async generateDynamicScript(imageId: string, userId?: string): Promise<{
    script: string;
    language: string;
    tone: string;
    image_url: string;
    product_name: string;
  }> {
    const startTime = Date.now();
    
    try {
      // Use optimized script service with caching
      const result = await optimizedScriptService.generateOptimizedScript({
        image_id: imageId,
        user_id: userId,
        use_cache: true
      });

      // Calculate response time
      const responseTime = Date.now() - startTime;
      
      // Log the generation request for analytics
      console.log(`[DYNAMIC_SCRIPT] Generated script for ${imageId}: ${result.script.substring(0, 50)}... (${result.cached ? 'cached' : 'generated'} in ${result.generation_time}ms)`);
      
      // Log interaction with response time
      logInteraction({
        timestamp: new Date().toISOString(),
        poster_id: imageId,
        script: result.script,
        feedback: null,
        response_time: responseTime
      });

      return {
        script: result.script,
        language: result.language,
        tone: result.tone,
        image_url: result.image_url,
        product_name: result.product_name
      };

    } catch (error: any) {
      console.error("Dynamic script generation error:", error);
      throw new Error(`Failed to generate dynamic script: ${error.message}`);
    }
  }

  /**
   * Generate script for product description (as specified in requirements)
   */
  static async generateProductScript(productName: string): Promise<string> {
    try {
      // Check cache first
      const cacheKey = `product-script:${productName}`;
      const cachedResult = this.getFromCache(cacheKey);
      if (cachedResult) {
        console.log("Returning cached product script");
        return cachedResult.text;
      }

      // Create request object
      const request: ScriptGenerationRequest = {
        imageId: "product-" + productName.toLowerCase().replace(/\s+/g, "-"),
        language: "en",
        productName: productName
      };

      // In development, use mock implementation
      if (process.env.NODE_ENV === "development" || (!process.env.OPENAI_API_KEY && !process.env.GROQCLOUD_API_KEY)) {
        console.log("Using mock product script generation");
        const result = this.generateMockProductScript(productName);
        this.setInCache(cacheKey, { text: result });
        await this.saveScriptToFile(result, request.imageId, "en");
        return result;
      }

      // Determine which AI provider to use
      const aiProvider = process.env.AI_PROVIDER || "openai"; // Default to OpenAI

      let result: ScriptGenerationResponse;
      if (aiProvider === "groq" && process.env.GROQCLOUD_API_KEY) {
        console.log("Calling GroqCloud API for product script generation");
        result = await this.callGroqCloudAPI(request);
      } else {
        console.log("Calling OpenAI API for product script generation");
        result = await this.callOpenAIAPI(request);
      }

      this.setInCache(cacheKey, result);
      await this.saveScriptToFile(result.text, request.imageId, "en");
      return result.text;
    } catch (error) {
      console.error("Product script generation error:", error);
      // Fallback to mock implementation if real API fails
      console.log("Falling back to mock product script generation");
      const mockResult = this.generateMockProductScript(productName);
      await this.saveScriptToFile(mockResult, "product-" + productName.toLowerCase().replace(/\s+/g, "-"), "en");
      return mockResult;
    }
  }

  /**
   * Mock product script generation
   */
  private static generateMockProductScript(productName: string): string {
    const mockScripts: Record<string, string> = {
      "iPhone": "Experience the future in your hands with the revolutionary iPhone. Cutting-edge technology meets elegant design.",
      "MacBook": "Unleash your creativity with the powerful MacBook. Sleek, fast, and ready for anything you throw at it.",
      "Tesla": "Drive into the future with Tesla's electric vehicles. Innovation, sustainability, and performance combined.",
      "Nike Shoes": "Step up your game with Nike's latest athletic shoes. Engineered for peak performance and unmatched comfort.",
      "Samsung TV": "Transform your home entertainment with Samsung's crystal-clear 4K TV. Every picture comes to life.",
      "Coffee Maker": "Brew the perfect cup every time with our advanced coffee maker. Rich flavor, effortless operation.",
      "Smart Watch": "Stay connected and healthy with our feature-packed smartwatch. Your life, simplified and optimized.",
      "Bluetooth Speaker": "Fill your space with rich, room-filling sound. Wireless freedom with premium audio quality."
    };

    // Return a mock script based on the product name or a generic one
    return mockScripts[productName] || `Discover the amazing ${productName}. Quality and innovation in every detail.`;
  }

  /**
   * Mock script generation
   */
  private static generateMockScript(request: ScriptGenerationRequest): ScriptGenerationResponse {
    // Simulate API delay
    const delay = Math.random() * 1000 + 500;
    
    // Generate mock script based on language and emotion/tone
    const scripts: Record<string, Record<string, string>> = {
      en: {
        neutral: "Welcome to our exhibition. I'm here to guide you through this amazing collection.",
        happy: "Welcome! I'm so excited to show you around our wonderful exhibition today!",
        surprised: "Oh my! Look at this incredible piece. Isn't it absolutely fascinating?",
        serious: "Please pay attention to this important historical artifact and its significance.",
        friendly: "Hello there! I'm delighted to be your guide through this fascinating exhibition.",
        excited: "Wow! Get ready for an incredible journey through our amazing collection.",
        professional: "Welcome to our distinguished exhibition. I'm here to provide you with expert insights.",
        casual: "Hey there! Come check out these cool exhibits I've got to show you.",
        enthusiastic: "You're going to love what we have in store for you today - it's absolutely incredible!",
        persuasive: "Don't miss this opportunity to explore our exceptional collection - it's truly remarkable!"
      },
      es: {
        neutral: "Bienvenido a nuestra exposición. Estoy aquí para guiarlo a través de esta increíble colección.",
        happy: "¡Bienvenido! ¡Estoy muy emocionado de mostrarle nuestra maravillosa exposición hoy!",
        surprised: "¡Oh cielos! Mire esta pieza increíble. ¿No es absolutamente fascinante?",
        serious: "Por favor, preste atención a este importante artefacto histórico y su significado.",
        friendly: "¡Hola! Me complace ser su guía a través de esta fascinante exposición.",
        excited: "¡Guau! Prepárese para un viaje increíble a través de nuestra asombrosa colección.",
        professional: "Bienvenido a nuestra distinguida exposición. Estoy aquí para proporcionarle conocimientos expertos.",
        casual: "¡Oye! Ven a ver estos geniales exhibiciones que tengo para mostrarte.",
        enthusiastic: "¡Te encantará lo que tenemos preparado para ti hoy - es absolutamente increíble!",
        persuasive: "No pierdas esta oportunidad de explorar nuestra colección excepcional - es verdaderamente notable."
      },
      fr: {
        neutral: "Bienvenue à notre exposition. Je suis ici pour vous guider à travers cette collection incroyable.",
        happy: "Bienvenue ! Je suis tellement excité de vous montrer notre merveilleuse exposition aujourd'hui !",
        surprised: "Oh mon Dieu ! Regardez cette pièce incroyable. N'est-ce pas absolument fascinant ?",
        serious: "Veuillez prêter attention à cet important artefact historique et sa signification.",
        friendly: "Bonjour ! Je suis ravi de vous guider à travers cette fascinante exposition.",
        excited: "Waouh ! Préparez-vous pour un voyage incroyable à travers notre collection étonnante.",
        professional: "Bienvenue dans notre exposition distinguée. Je suis ici pour vous fournir des connaissances expertes.",
        casual: "Hé ! Viens voir ces expositions cool que j'ai à te montrer.",
        enthusiastic: "Vous allez adorer ce que nous avons en stock pour vous aujourd'hui - c'est absolument incroyable !",
        persuasive: "Ne manquez pas cette opportunité d'explorer notre collection exceptionnelle - elle est vraiment remarquable !"
      }
    };

    const language = request.language || "en";
    // Use emotion if available, otherwise fall back to tone or default to neutral
    const emotionOrTone = request.emotion || "neutral";
    
    const text = scripts[language]?.[emotionOrTone] || scripts["en"]["neutral"];

    return {
      text,
      language,
      emotion: request.emotion
    };
  }

  /**
   * Mock audio generation
   */
  private static generateMockAudio(request: AudioGenerationRequest): AudioGenerationResponse {
    try {
      // Simulate API delay - reduced for performance optimization
      const delay = Math.random() * 300 + 100; // Reduced from 500-1500ms to 100-400ms
      
      // Calculate approximate duration based on text length
      const duration = Math.max(5, Math.min(30, request.text.length / 15));
      
      // Generate mock audio content (MP3 format)
      const audioContent = this.generateMockAudioContent(request.text);
      
      // Save mock audio to file
      const audioFilename = this.saveMockAudioToFile(audioContent, request.text, request.language, request.emotion);
      const audioUrl = `http://localhost:3000/audio/${audioFilename}`;

      return {
        audioUrl,
        duration
      };
    } catch (error) {
      console.error("Mock audio generation error:", error);
      // Fallback to simple mock URL
      const audioId = uuidv4().substring(0, 8);
      const audioUrl = `https://mock-audio-service.com/audio/${audioId}.mp3`;
      const duration = Math.max(5, Math.min(30, request.text.length / 15));
      
      return {
        audioUrl,
        duration
      };
    }
  }

  /**
   * Generate mock audio content
   */
  private static generateMockAudioContent(text: string): Buffer {
    // Create a simple mock MP3-like buffer
    // In a real implementation, this would be actual audio data
    const content = `Mock audio for: ${text.substring(0, 50)}...`;
    return Buffer.from(content, 'utf8');
  }

  /**
   * Save mock audio to file
   */
  private static saveMockAudioToFile(audioBuffer: Buffer, text: string, language: string, emotion?: string): string {
    try {
      // Create audio directory if it doesn't exist
      const audioDir = path.join(__dirname, "..", "..", "audio");
      if (!fs.existsSync(audioDir)) {
        fs.mkdirSync(audioDir, { recursive: true });
      }

      // Create filename based on text hash and parameters
      const textHash = this.hashString(text);
      const timestamp = new Date().toISOString().replace(/[:.]/g, "-");
      const emotionSuffix = emotion ? `-${emotion}` : "";
      const filename = `mock-audio-${textHash}-${language}${emotionSuffix}-${timestamp}.mp3`;
      const filepath = path.join(audioDir, filename);

      // Write audio buffer to file
      fs.writeFileSync(filepath, audioBuffer);
      console.log(`Mock audio saved to ${filepath}`);
      
      return filename;
    } catch (error) {
      console.error("Error saving mock audio to file:", error);
      throw error;
    }
  }

  /**
   * Mock lip-sync generation
   */
  private static generateMockLipSync(request: LipSyncGenerationRequest): LipSyncGenerationResponse {
    // Simulate API delay - further reduced for performance optimization
    const delay = Math.random() * 500 + 200; // Further reduced from 500-1500ms to 200-700ms
    
    // Generate mock video URL
    const videoId = uuidv4().substring(0, 8);
    const emotionSuffix = request.emotion ? `-${request.emotion}` : "";
    const videoUrl = `https://mock-lipsync-service.com/videos/${videoId}${emotionSuffix}.mp4`;
    
    // Mock duration
    const duration = 15; // 15 seconds

    return {
      videoUrl,
      duration
    };
  }



  /**
   * Load product metadata from JSON file or database
   */
  private static async loadProductMetadata(imageId: string): Promise<ProductMetadata | null> {
    try {
      // Check cache first
      if (productMetadataCache.has(imageId)) {
        return productMetadataCache.get(imageId) || null;
      }

      // Try to load from JSON file
      const metadataFilePath = path.join(__dirname, "..", "..", "data", "product-metadata.json");
      if (fs.existsSync(metadataFilePath)) {
        const metadataContent = fs.readFileSync(metadataFilePath, "utf8");
        const allMetadata: ProductMetadata[] = JSON.parse(metadataContent);
        
        // Find metadata for the specific image ID
        const metadata = allMetadata.find(item => item.image_id === imageId) || null;
        
        if (metadata) {
          // Cache the metadata
          productMetadataCache.set(imageId, metadata);
          return metadata;
        }
      }

      // If not found in file, try to load from database
      // For now, we'll return null if not found
      return null;
    } catch (error) {
      console.error("Error loading product metadata:", error);
      return null;
    }
  }

  /**
   * Mock metadata-based script generation
   */
  private static generateMockMetadataScript(metadata: ProductMetadata, userPreferences?: { language?: string; preferred_tone?: string }): ScriptGenerationResponse {
    // Generate mock script based on metadata and user preferences
    const tone = metadata.tone || userPreferences?.preferred_tone || "friendly";
    const language = metadata.language || userPreferences?.language || "en";
    
    // Create mock scripts based on tone and product
    const scripts: Record<string, string> = {
      friendly: `Hi there! Check out the amazing ${metadata.product_name} - it's perfect for you!`,
      excited: `Wow! Get ready for the incredible ${metadata.product_name} - you won't believe how awesome it is!`,
      professional: `Introducing the premium ${metadata.product_name}, engineered for discerning professionals who demand excellence.`,
      casual: `Hey, you should really check out this cool ${metadata.product_name} - it's pretty awesome!`,
      enthusiastic: `You're going to love the fantastic ${metadata.product_name} - it's everything you've been looking for!`,
      persuasive: `Don't miss out on the exceptional ${metadata.product_name} - transform your experience today!`
    };

    // Return a mock script based on the tone or a generic one
    const text = scripts[tone] || scripts["friendly"];

    return {
      text,
      language,
      emotion: tone
    };
  }



  /**
   * Save generated video URL to database
   */
  private static async saveVideoUrlToDatabase(imageId: string, videoUrl: string, avatarUrl: string, emotion?: string): Promise<void> {
    try {
      console.log(`Saving video URL to database for image ${imageId}: ${videoUrl}`);
      
      // Find or create avatar record
      let avatar = await Avatar.findOne({ where: { avatarImageUrl: avatarUrl } });
      
      if (!avatar) {
        // Create new avatar record
        const avatarName = `Avatar for ${imageId}${emotion ? ` (${emotion})` : ''}`;
        avatar = await Avatar.create({
          name: avatarName,
          description: `Auto-generated avatar for image ${imageId}`,
          avatarImageUrl: avatarUrl,
          avatarVideoUrl: videoUrl,
          voiceId: emotion || "neutral",
          isActive: true
        });
        console.log(`Created new avatar record with ID: ${avatar.id}`);
      } else {
        // Update existing avatar with new video URL
        avatar.avatarVideoUrl = videoUrl;
        avatar.isActive = true;
        await avatar.save();
        console.log(`Updated existing avatar record with ID: ${avatar.id}`);
      }

      // Create or update image-avatar mapping
      let mapping = await ImageAvatarMapping.findOne({ 
        where: { 
          imageId: imageId,
          avatarId: avatar.id
        } 
      });
      
      if (!mapping) {
        mapping = await ImageAvatarMapping.create({
          imageId: imageId,
          avatarId: avatar.id,
          isActive: true
        });
        console.log(`Created new image-avatar mapping with ID: ${mapping.id}`);
      } else {
        mapping.isActive = true;
        await mapping.save();
        console.log(`Updated existing image-avatar mapping with ID: ${mapping.id}`);
      }

      console.log(`Successfully saved video URL to database for image ${imageId}`);
    } catch (error) {
      console.error("Error saving video URL to database:", error);
      // Don't throw error as this is non-critical
    }
  }

  /**
   * Process conversational context queries
   */
  static async processConversationalQuery(request: { 
    query: string; 
    imageId?: string; 
    context?: any 
  }): Promise<{ 
    response: string; 
    audioUrl?: string; 
    emotion?: string 
  }> {
    try {
      // In development, use mock implementation
      if (process.env.NODE_ENV === "development" || 
          (!process.env.OPENAI_API_KEY && !process.env.GROQCLOUD_API_KEY)) {
        console.log("Using mock conversational response");
        return this.generateMockConversationalResponse(request);
      }

      // Determine which AI provider to use
      const aiProvider = process.env.AI_PROVIDER || "openai"; // Default to OpenAI

      let result: { response: string; audioUrl?: string; emotion?: string };
      if (aiProvider === "groq" && process.env.GROQCLOUD_API_KEY) {
        console.log("Calling GroqCloud API for conversational query");
        result = await this.callGroqCloudForConversation(request);
      } else {
        console.log("Calling OpenAI API for conversational query");
        result = await this.callOpenAIForConversation(request);
      }

      return result;
    } catch (error) {
      console.error("Conversational query error:", error);
      // Fallback to mock implementation if real API fails
      console.log("Falling back to mock conversational response");
      return this.generateMockConversationalResponse(request);
    }
  }

  /**
   * Mock conversational response generation
   */
  private static generateMockConversationalResponse(request: { 
    query: string; 
    imageId?: string; 
    context?: any 
  }): { response: string; audioUrl?: string; emotion?: string } {
    const query = request.query.toLowerCase();
    
    // Generate mock responses based on query content
    let response = "";
    let emotion = "neutral";
    
    if (query.includes("hello") || query.includes("hi")) {
      response = "Hello there! I'm your TalkAR assistant. How can I help you today?";
      emotion = "happy";
    } else if (query.includes("what is this") || query.includes("what's this")) {
      if (request.imageId) {
        response = "This appears to be an interesting object. I can tell you more about it if you'd like!";
        emotion = "friendly";
      } else {
        response = "I'm not sure what you're referring to. Could you point your camera at something?";
        emotion = "neutral";
      }
    } else if (query.includes("how does this work")) {
      response = "TalkAR uses augmented reality to bring images to life. Simply point your camera at an object, and I'll provide information about it!";
      emotion = "friendly";
    } else if (query.includes("thank")) {
      response = "You're welcome! Is there anything else I can help you with?";
      emotion = "happy";
    } else {
      response = "That's an interesting question. I'm still learning about the world around us. Could you ask me something else?";
      emotion = "neutral";
    }
    
    return {
      response,
      emotion
    };
  }

  /**
   * Call OpenAI API for conversational queries
   */
  private static async callOpenAIForConversation(request: { 
    query: string; 
    imageId?: string; 
    context?: any 
  }): Promise<{ response: string; audioUrl?: string; emotion?: string }> {
    try {
      const openaiApiKey = process.env.OPENAI_API_KEY;
      const openaiApiUrl = "https://api.openai.com/v1/chat/completions";
      
      if (!openaiApiKey) {
        throw new Error("OpenAI API key not configured");
      }

      // Create a prompt based on the query and context
      let prompt = `You are a helpful AR assistant in the TalkAR application. Respond to the user's query in a friendly and informative way.`;
      
      if (request.imageId) {
        prompt += ` The user is currently viewing an image with ID: ${request.imageId}.`;
      }
      
      if (request.context) {
        prompt += ` Context: ${JSON.stringify(request.context)}`;
      }
      
      prompt += `\n\nUser query: ${request.query}`;

      // Use GPT-4o-mini model
      const response = await axios.post(
        openaiApiUrl,
        {
          model: "gpt-4o-mini",
          messages: [
            {
              role: "system",
              content: prompt
            },
            {
              role: "user",
              content: request.query
            }
          ],
          max_tokens: 150,
          temperature: 0.7
        },
        {
          headers: {
            "Authorization": `Bearer ${openaiApiKey}`,
            "Content-Type": "application/json"
          },
          timeout: 10000 // 10 second timeout
        }
      );

      const text = response.data.choices[0].message.content.trim();
      
      // Validate response
      if (!text || text.length === 0) {
        throw new Error("Empty response from OpenAI API");
      }

      return {
        response: text,
        emotion: "neutral"
      };
    } catch (error: any) {
      console.error("OpenAI conversational API error:", error.response?.data || error.message);
      
      // Handle specific error cases
      if (error.response?.status === 401) {
        throw new Error("Invalid OpenAI API key");
      } else if (error.response?.status === 429) {
        throw new Error("OpenAI API rate limit exceeded");
      } else if (error.response?.status === 400) {
        throw new Error(`OpenAI API bad request: ${error.response.data?.error?.message || 'Invalid request'}`);
      } else if (error.response?.status >= 500) {
        throw new Error("OpenAI API service unavailable");
      } else if (error.code === 'ECONNABORTED') {
        throw new Error("OpenAI API request timeout");
      } else if (error.code === 'ENOTFOUND') {
        throw new Error("OpenAI API service unreachable");
      }
      
      throw new Error(`Failed to generate conversational response with OpenAI API: ${error.message}`);
    }
  }

  /**
   * Call GroqCloud API for conversational queries
   */
  private static async callGroqCloudForConversation(request: { 
    query: string; 
    imageId?: string; 
    context?: any 
  }): Promise<{ response: string; audioUrl?: string; emotion?: string }> {
    try {
      const groqApiKey = process.env.GROQCLOUD_API_KEY;
      const groqApiUrl = "https://api.groq.com/openai/v1/chat/completions";
      
      if (!groqApiKey) {
        throw new Error("GroqCloud API key not configured");
      }

      // Create a prompt based on the query and context
      let prompt = `You are a helpful AR assistant in the TalkAR application. Respond to the user's query in a friendly and informative way.`;
      
      if (request.imageId) {
        prompt += ` The user is currently viewing an image with ID: ${request.imageId}.`;
      }
      
      if (request.context) {
        prompt += ` Context: ${JSON.stringify(request.context)}`;
      }
      
      prompt += `\n\nUser query: ${request.query}`;

      // Use LLaMA 3.2 Vision model (or appropriate Groq model)
      const response = await axios.post(
        groqApiUrl,
        {
          model: "llama3-groq-70b-8192-tool-use-preview", // or "llama-3.2-90b-vision-preview"
          messages: [
            {
              role: "system",
              content: prompt
            },
            {
              role: "user",
              content: request.query
            }
          ],
          max_tokens: 150,
          temperature: 0.7
        },
        {
          headers: {
            "Authorization": `Bearer ${groqApiKey}`,
            "Content-Type": "application/json"
          },
          timeout: 10000 // 10 second timeout
        }
      );

      const text = response.data.choices[0].message.content.trim();
      
      // Validate response
      if (!text || text.length === 0) {
        throw new Error("Empty response from GroqCloud API");
      }

      return {
        response: text,
        emotion: "neutral"
      };
    } catch (error: any) {
      console.error("GroqCloud conversational API error:", error.response?.data || error.message);
      
      // Handle specific error cases
      if (error.response?.status === 401) {
        throw new Error("Invalid GroqCloud API key");
      } else if (error.response?.status === 429) {
        throw new Error("GroqCloud API rate limit exceeded");
      } else if (error.response?.status === 400) {
        throw new Error(`GroqCloud API bad request: ${error.response.data?.error?.message || 'Invalid request'}`);
      } else if (error.response?.status >= 500) {
        throw new Error("GroqCloud API service unavailable");
      } else if (error.code === 'ECONNABORTED') {
        throw new Error("GroqCloud API request timeout");
      } else if (error.code === 'ENOTFOUND') {
        throw new Error("GroqCloud API service unreachable");
      }
      
      throw new Error(`Failed to generate conversational response with GroqCloud API: ${error.message}`);
    }
  }
}