import { getPosterById } from "../utils/posterHelper";
import { getUserPreferences } from "../utils/userHelper";
// OpenAI import removed
import axios from "axios";
import logger from "../utils/logger";

interface CacheEntry {
  data: any;
  timestamp: number;
  ttl: number;
}

interface OptimizedScriptRequest {
  image_id: string;
  user_id?: string;
  use_cache?: boolean;
}

interface OptimizedScriptResponse {
  script: string;
  language: string;
  tone: string;
  image_url: string;
  product_name: string;
  cached: boolean;
  generation_time: number;
}

import { config } from "../config";

export class OptimizedScriptService {
  // openai property removed
  private scriptCache: Map<string, CacheEntry>;
  private posterCache: Map<string, CacheEntry>;
  private userPrefsCache: CacheEntry | null;
  
  // Cache TTL settings (in milliseconds)
  private readonly SCRIPT_CACHE_TTL = 5 * 60 * 1000; // 5 minutes
  private readonly POSTER_CACHE_TTL = 30 * 60 * 1000; // 30 minutes
  private readonly USER_PREFS_CACHE_TTL = 10 * 60 * 1000; // 10 minutes
  
  // Performance tracking
  private performanceMetrics: {
    totalRequests: number;
    cacheHits: number;
    averageGenerationTime: number;
    parallelRequests: number;
  };

  constructor() {
    // OpenAI lazy init removed
    
    this.scriptCache = new Map();
    this.posterCache = new Map();
    this.userPrefsCache = null;
    
    console.log(`[OptimizedScriptService] Initialized with Provider: ${config.aiProvider}`);
    console.log(`[OptimizedScriptService] Groq Key Available: ${!!config.groqKey}`);
    
    this.performanceMetrics = {
      totalRequests: 0,
      cacheHits: 0,
      averageGenerationTime: 0,
      parallelRequests: 0
    };

    // Cleanup expired cache entries every 5 minutes
    setInterval(() => {
      this.cleanupExpiredCache();
    }, 5 * 60 * 1000);
  }

  /**
   * Generate script with caching and optimization
   */
  async generateOptimizedScript(request: OptimizedScriptRequest): Promise<OptimizedScriptResponse> {
    const startTime = Date.now();
    this.performanceMetrics.totalRequests++;

    try {
      // Check cache first if enabled
      if (request.use_cache !== false) {
        const cachedResult = this.getFromCache(request.image_id, request.user_id);
        if (cachedResult) {
          this.performanceMetrics.cacheHits++;
          return {
            ...cachedResult,
            cached: true,
            generation_time: Date.now() - startTime
          };
        }
      }

      // Generate script in parallel with metadata fetching
      const [scriptResult, posterData, userPrefs] = await Promise.all([
        this.generateScriptParallel(request),
        this.getPosterDataOptimized(request.image_id),
        this.getUserPreferencesOptimized()
      ]);

      const response: OptimizedScriptResponse = {
        script: scriptResult.script,
        language: scriptResult.language,
        tone: scriptResult.tone,
        image_url: posterData.image_url,
        product_name: posterData.product_name,
        cached: false,
        generation_time: Date.now() - startTime
      };

      // Cache the result
      this.setCache(request.image_id, request.user_id, response);

      // Update performance metrics
      this.updatePerformanceMetrics(response.generation_time);

      return response;

    } catch (error) {
      console.error('Optimized script generation error:', error);
      throw error;
    }
  }

  /**
   * Generate script with parallel processing
   */
  private async generateScriptParallel(request: OptimizedScriptRequest): Promise<{
    script: string;
    language: string;
    tone: string;
  }> {
    const poster = await this.getPosterDataOptimized(request.image_id);
    const userPrefs = await this.getUserPreferencesOptimized();

    const language = poster.language || userPrefs.language;
    const tone = poster.tone || userPrefs.preferred_tone;

    // Create optimized prompt
    const prompt = this.createOptimizedPrompt(poster, language, tone);

    // Generate script based on provider
    try {
      console.log(`[Script Generation] Using provider: ${config.aiProvider}`);
      
      if (config.aiProvider === "groq") {
        if (!config.groqKey || config.groqKey === 'your-groqcloud-api-key') {
           console.warn("[Script Generation] Groq provider selected but key is missing/default. Falling back to OpenAI.");
           // Fall through to OpenAI logic only if key missing
        } else {
           return await this.callGroqAPI(prompt, language, tone);
        }
      } 
      
      
        if (config.aiProvider === "ollama") {
        return await this.callOllamaAPI(prompt, language, tone);
      } 
      
      // Default to Groq
      if (!config.groqKey || config.groqKey === 'your-groqcloud-api-key') {
          throw new Error("Groq API Key is missing. OpenAI fallback has been removed.");
      } else {
          return await this.callGroqAPI(prompt, language, tone);
      }
    } catch (error: any) {
      const errorMessage = error.response?.data || error.message;
      logger.error(`${config.aiProvider} generation failed:`, { error: errorMessage, prompt });
      
      // Fallback script
      const fallbackScript = `Welcome to ${poster.product_name}! Experience the future of ${poster.category} with our amazing features.`;
      return { 
        script: fallbackScript, 
        language, 
        tone 
      };
    }
  }

  /**
   * Call Groq API for script generation
   */
  private async callGroqAPI(prompt: string, language: string, tone: string): Promise<{script: string, language: string, tone: string}> {
    const groqApiUrl = "https://api.groq.com/openai/v1/chat/completions";
    
    const response = await axios.post(
      groqApiUrl,
      {
        model: "llama-3.1-8b-instant",
        messages: [
          { role: "system", content: "You are an AI advertisement script writer. Generate 1-2 punchy lines." },
          { role: "user", content: prompt }
        ],
        max_tokens: 100,
        temperature: 0.7
      },
      {
        headers: {
          "Authorization": `Bearer ${config.groqKey}`,
          "Content-Type": "application/json"
        },
        timeout: 10000
      }
    );

    const script = response.data.choices[0].message.content.trim();
    return { script, language, tone };
  }

  /**
   * Call local Ollama API for script generation
   */
  private async callOllamaAPI(prompt: string, language: string, tone: string): Promise<{script: string, language: string, tone: string}> {
    const ollamaHost = process.env.OLLAMA_HOST || "http://localhost:11434";
    const ollamaModel = process.env.OLLAMA_MODEL || "llama3.2";
    
    const response = await axios.post(`${ollamaHost}/api/generate`, {
      model: ollamaModel,
      prompt: `Generate a short AR ad script based on this: ${prompt}. Only return the script text.`,
      stream: false,
      options: {
        temperature: 0.7,
        num_predict: 100
      }
    });

    const script = response.data.response.trim();
    return { script, language, tone };
  }

  /**
   * Create optimized prompt for faster generation
   */
  private createOptimizedPrompt(poster: any, language: string, tone: string): string {
    // Use a more concise prompt for faster generation
    return `Create 1 concise, punchy ad line for ${poster.product_name}.
Tone: ${tone}, Language: ${language}.
Limit to 15 words. ONLY return the script text, no introduction or quotes.`;
  }

  /**
   * Get poster data with caching
   */
  private async getPosterDataOptimized(imageId: string): Promise<any> {
    const cacheKey = `poster_${imageId}`;
    const cached = this.posterCache.get(cacheKey);
    
    if (cached && this.isCacheValid(cached)) {
      return cached.data;
    }

    const poster = getPosterById(imageId);
    if (!poster) {
      throw new Error(`Poster with image_id '${imageId}' not found`);
    }

    this.posterCache.set(cacheKey, {
      data: poster,
      timestamp: Date.now(),
      ttl: this.POSTER_CACHE_TTL
    });

    return poster;
  }

  /**
   * Get user preferences with caching
   */
  private async getUserPreferencesOptimized(): Promise<any> {
    if (this.userPrefsCache && this.isCacheValid(this.userPrefsCache)) {
      return this.userPrefsCache.data;
    }

    const userPrefs = getUserPreferences();
    this.userPrefsCache = {
      data: userPrefs,
      timestamp: Date.now(),
      ttl: this.USER_PREFS_CACHE_TTL
    };

    return userPrefs;
  }

  /**
   * Cache management
   */
  private getFromCache(imageId: string, userId?: string): OptimizedScriptResponse | null {
    const cacheKey = this.getCacheKey(imageId, userId);
    const cached = this.scriptCache.get(cacheKey);
    
    if (cached && this.isCacheValid(cached)) {
      return cached.data;
    }
    
    return null;
  }

  private setCache(imageId: string, userId: string | undefined, data: OptimizedScriptResponse): void {
    const cacheKey = this.getCacheKey(imageId, userId);
    this.scriptCache.set(cacheKey, {
      data,
      timestamp: Date.now(),
      ttl: this.SCRIPT_CACHE_TTL
    });
  }

  private getCacheKey(imageId: string, userId?: string): string {
    return `script_${imageId}_${userId || 'anonymous'}`;
  }

  private isCacheValid(entry: CacheEntry): boolean {
    return Date.now() - entry.timestamp < entry.ttl;
  }

  /**
   * Cleanup expired cache entries
   */
  private cleanupExpiredCache(): void {
    const now = Date.now();
    
    // Clean script cache
    for (const [key, entry] of this.scriptCache.entries()) {
      if (now - entry.timestamp >= entry.ttl) {
        this.scriptCache.delete(key);
      }
    }
    
    // Clean poster cache
    for (const [key, entry] of this.posterCache.entries()) {
      if (now - entry.timestamp >= entry.ttl) {
        this.posterCache.delete(key);
      }
    }
    
    // Clean user preferences cache
    if (this.userPrefsCache && now - this.userPrefsCache.timestamp >= this.userPrefsCache.ttl) {
      this.userPrefsCache = null;
    }
  }

  /**
   * Performance metrics
   */
  private updatePerformanceMetrics(generationTime: number): void {
    const totalTime = this.performanceMetrics.averageGenerationTime * (this.performanceMetrics.totalRequests - 1) + generationTime;
    this.performanceMetrics.averageGenerationTime = totalTime / this.performanceMetrics.totalRequests;
  }

  /**
   * Get performance metrics
   */
  getPerformanceMetrics(): any {
    const cacheHitRate = this.performanceMetrics.totalRequests > 0 
      ? (this.performanceMetrics.cacheHits / this.performanceMetrics.totalRequests) * 100 
      : 0;

    return {
      ...this.performanceMetrics,
      cacheHitRate: cacheHitRate.toFixed(2) + '%',
      cacheSize: {
        scripts: this.scriptCache.size,
        posters: this.posterCache.size,
        userPrefs: this.userPrefsCache ? 1 : 0
      }
    };
  }

  /**
   * Clear all caches
   */
  clearCache(): void {
    this.scriptCache.clear();
    this.posterCache.clear();
    this.userPrefsCache = null;
    console.log('All caches cleared');
  }

  /**
   * Preload popular posters into cache
   */
  async preloadPopularPosters(posterIds: string[]): Promise<void> {
    console.log(`Preloading ${posterIds.length} popular posters...`);
    
    const preloadPromises = posterIds.map(async (imageId) => {
      try {
        await this.getPosterDataOptimized(imageId);
      } catch (error) {
        console.warn(`Failed to preload poster ${imageId}:`, error);
      }
    });

    await Promise.all(preloadPromises);
    console.log('Popular posters preloaded successfully');
  }

  /**
   * Batch generate scripts for multiple posters
   */
  async batchGenerateScripts(requests: OptimizedScriptRequest[]): Promise<OptimizedScriptResponse[]> {
    console.log(`Batch generating ${requests.length} scripts...`);
    
    // Process in parallel with concurrency limit
    const concurrencyLimit = 5;
    const results: OptimizedScriptResponse[] = [];
    
    for (let i = 0; i < requests.length; i += concurrencyLimit) {
      const batch = requests.slice(i, i + concurrencyLimit);
      const batchPromises = batch.map(request => this.generateOptimizedScript(request));
      
      try {
        const batchResults = await Promise.all(batchPromises);
        results.push(...batchResults);
      } catch (error) {
        console.error(`Batch ${i}-${i + concurrencyLimit} failed:`, error);
        // Add error results for failed requests
        batch.forEach(() => {
          results.push({
            script: '',
            language: '',
            tone: '',
            image_url: '',
            product_name: '',
            cached: false,
            generation_time: 0
          });
        });
      }
    }
    
    console.log(`Batch generation completed: ${results.length} scripts`);
    return results;
  }
}

// Export singleton instance
export const optimizedScriptService = new OptimizedScriptService();
