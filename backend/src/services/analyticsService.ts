import { v4 as uuidv4 } from 'uuid';
import { supabaseAdmin } from '../config/supabase';

// Analytics and logging service for Week 3
export interface ImageTriggerEvent {
  id: string;
  imageId: string;
  imageName: string;
  scriptId: string;
  scriptText: string;
  voiceId: string;
  timestamp: Date;
  sessionId: string;
  deviceId?: string;
  userAgent?: string;
  ipAddress?: string;
  recognitionConfidence?: number;
  processingTime?: number;
}

export interface AvatarPlayEvent {
  id: string;
  imageId: string;
  scriptId: string;
  videoId: string;
  startTime: Date;
  endTime?: Date;
  duration?: number;
  sessionId: string;
  deviceId?: string;
  status: 'started' | 'completed' | 'interrupted';
  videoQuality?: 'low' | 'medium' | 'high';
  bufferingEvents?: number;
  errorCount?: number;
}

export interface ScriptAnalytics {
  scriptId: string;
  usageCount: number;
  averageDuration: number;
  completionRate: number;
  userEngagement: number;
  lastUsed: Date;
  performance: {
    averageConfidence: number;
    recognitionAccuracy: number;
    processingSpeed: number;
  };
}

export interface ImageAnalytics {
  imageId: string;
  imageName: string;
  totalTriggers: number;
  uniqueSessions: number;
  averageConfidence: number;
  mostUsedScript: string;
  peakUsageTime: Date;
  usageByHour: number[]; // 24 hours
  usageByDay: number[]; // 7 days
}

export interface PerformanceMetrics {
  averageResponseTime: number;
  totalRequests: number;
  successfulRequests: number;
  failedRequests: number;
  averageVideoProcessingTime: number;
  mostTriggeredImages: Array<{ imageId: string; imageName: string; count: number }>;
  mostUsedVoices: Array<{ voiceId: string; count: number }>;
}

// In-memory storage for demo (in production, use database)
const imageTriggerEvents = new Map<string, ImageTriggerEvent>();
const avatarPlayEvents = new Map<string, AvatarPlayEvent>();
const performanceMetrics = {
  responseTimes: [] as number[],
  totalRequests: 0,
  successfulRequests: 0,
  failedRequests: 0,
  videoProcessingTimes: [] as number[],
};

export class AnalyticsService {
  /**
   * Persist analytics event to database (best effort, non-blocking)
   */
  static async persistAnalyticsEvent(
    eventType: string, 
    payload: ImageTriggerEvent | AvatarPlayEvent | Record<string, any>, 
    options?: { id?: string; sessionId?: string; deviceId?: string; userId?: string; }
  ): Promise<void> {
    try {
      const insertPayload = {
        id: options?.id,
        user_id: options?.userId,
        event_type: eventType,
        event_data: payload,
        session_id: options?.sessionId,
        device_id: options?.deviceId,
      };
      const { error } = await supabaseAdmin.from('analytics_events').insert(insertPayload);
      if (error) {
        console.warn('[ANALYTICS] Persist error:', error.message);
      }
    } catch (err: any) {
      console.warn('[ANALYTICS] Persist exception:', err?.message || err);
    }
  }
  /**
   * Log image trigger event
   */
  static logImageTrigger(data: {
    imageId: string;
    imageName: string;
    scriptId: string;
    scriptText: string;
    voiceId: string;
    sessionId?: string;
    deviceId?: string;
    userAgent?: string;
    ipAddress?: string;
    recognitionConfidence?: number;
    processingTime?: number;
  }): string {
    const eventId = uuidv4();
    const event: ImageTriggerEvent = {
      id: eventId,
      imageId: data.imageId,
      imageName: data.imageName,
      scriptId: data.scriptId,
      scriptText: data.scriptText,
      voiceId: data.voiceId,
      timestamp: new Date(),
      sessionId: data.sessionId || 'unknown',
      deviceId: data.deviceId,
      userAgent: data.userAgent,
      ipAddress: data.ipAddress,
      recognitionConfidence: data.recognitionConfidence,
      processingTime: data.processingTime,
    };

    imageTriggerEvents.set(eventId, event);
    performanceMetrics.totalRequests++;

    console.log(`[ANALYTICS] Image triggered: ${data.imageName} (${data.imageId}) - Script: "${data.scriptText.substring(0, 50)}..."`);
    
    if (data.recognitionConfidence) {
      console.log(`[ANALYTICS] Recognition confidence: ${(data.recognitionConfidence * 100).toFixed(1)}%`);
    }
    
    if (data.processingTime) {
      console.log(`[ANALYTICS] Processing time: ${data.processingTime}ms`);
    }

    // Best-effort persistence
    void AnalyticsService.persistAnalyticsEvent('image_trigger', {
      imageId: data.imageId,
      imageName: data.imageName,
      scriptId: data.scriptId,
      scriptText: data.scriptText,
      voiceId: data.voiceId,
      userAgent: data.userAgent,
      ipAddress: data.ipAddress,
      recognitionConfidence: data.recognitionConfidence,
      processingTime: data.processingTime,
      timestamp: event.timestamp.toISOString(),
    }, { id: eventId, sessionId: event.sessionId, deviceId: event.deviceId });

    return eventId;
  }

  /**
   * Log avatar play start
   */
  static logAvatarPlayStart(data: {
    imageId: string;
    scriptId: string;
    videoId: string;
    sessionId?: string;
    deviceId?: string;
  }): string {
    const eventId = uuidv4();
    const event: AvatarPlayEvent = {
      id: eventId,
      imageId: data.imageId,
      scriptId: data.scriptId,
      videoId: data.videoId,
      startTime: new Date(),
      sessionId: data.sessionId || 'unknown',
      deviceId: data.deviceId,
      status: 'started',
    };

    avatarPlayEvents.set(eventId, event);

    console.log(`[ANALYTICS] Avatar play started: Video ${data.videoId} for image ${data.imageId}`);

    // Best-effort persistence
    void AnalyticsService.persistAnalyticsEvent('avatar_play_start', {
      imageId: data.imageId,
      scriptId: data.scriptId,
      videoId: data.videoId,
      startTime: event.startTime.toISOString(),
    }, { id: eventId, sessionId: event.sessionId, deviceId: event.deviceId });

    return eventId;
  }

  /**
   * Log avatar play end
   */
  static logAvatarPlayEnd(eventId: string, status: 'completed' | 'interrupted' = 'completed'): void {
    const event = avatarPlayEvents.get(eventId);
    if (!event) {
      console.warn(`[ANALYTICS] Avatar play event not found: ${eventId}`);
      return;
    }

    event.endTime = new Date();
    event.duration = event.endTime.getTime() - event.startTime.getTime();
    event.status = status;

    avatarPlayEvents.set(eventId, event);

    console.log(`[ANALYTICS] Avatar play ended: ${eventId} - Duration: ${event.duration}ms, Status: ${status}`);

    // Best-effort persistence
    void AnalyticsService.persistAnalyticsEvent('avatar_play_end', {
      imageId: event.imageId,
      scriptId: event.scriptId,
      videoId: event.videoId,
      startTime: event.startTime.toISOString(),
      endTime: event.endTime?.toISOString(),
      duration: event.duration,
      status: event.status,
    }, { id: eventId, sessionId: event.sessionId, deviceId: event.deviceId });
  }

  /**
   * Log performance metrics
   */
  static logResponseTime(responseTime: number, success: boolean = true): void {
    performanceMetrics.responseTimes.push(responseTime);
    if (success) {
      performanceMetrics.successfulRequests++;
    } else {
      performanceMetrics.failedRequests++;
    }

    // Keep only last 1000 response times for memory efficiency
    if (performanceMetrics.responseTimes.length > 1000) {
      performanceMetrics.responseTimes = performanceMetrics.responseTimes.slice(-1000);
    }

    // Best-effort persistence
    void AnalyticsService.persistAnalyticsEvent('response_time', {
      responseTime,
      success,
      timestamp: new Date().toISOString(),
    });
  }

  /**
   * Log video processing time
   */
  static logVideoProcessingTime(processingTime: number): void {
    performanceMetrics.videoProcessingTimes.push(processingTime);

    // Keep only last 1000 processing times
    if (performanceMetrics.videoProcessingTimes.length > 1000) {
      performanceMetrics.videoProcessingTimes = performanceMetrics.videoProcessingTimes.slice(-1000);
    }

    // Best-effort persistence
    void AnalyticsService.persistAnalyticsEvent('video_processing_time', {
      processingTime,
      timestamp: new Date().toISOString(),
    });
  }

  /**
   * Get analytics data
   */
  static getAnalytics(): {
    imageTriggers: {
      total: number;
      recent: ImageTriggerEvent[];
      byImage: Array<{ imageId: string; imageName: string; count: number }>;
      byVoice: Array<{ voiceId: string; count: number }>;
    };
    avatarPlays: {
      total: number;
      completed: number;
      interrupted: number;
      averageDuration: number;
      recent: AvatarPlayEvent[];
    };
    performance: PerformanceMetrics;
  } {
    const now = new Date();
    const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000);

    // Recent events (last hour)
    const recentImageTriggers = Array.from(imageTriggerEvents.values())
      .filter(event => event.timestamp > oneHourAgo)
      .sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime())
      .slice(0, 50);

    const recentAvatarPlays = Array.from(avatarPlayEvents.values())
      .filter(event => event.startTime > oneHourAgo)
      .sort((a, b) => b.startTime.getTime() - a.startTime.getTime())
      .slice(0, 50);

    // Count by image
    const imageCounts = new Map<string, { imageId: string; imageName: string; count: number }>();
    imageTriggerEvents.forEach(event => {
      const key = event.imageId;
      const existing = imageCounts.get(key) || { imageId: event.imageId, imageName: event.imageName, count: 0 };
      existing.count++;
      imageCounts.set(key, existing);
    });

    // Count by voice
    const voiceCounts = new Map<string, { voiceId: string; count: number }>();
    imageTriggerEvents.forEach(event => {
      const key = event.voiceId;
      const existing = voiceCounts.get(key) || { voiceId: event.voiceId, count: 0 };
      existing.count++;
      voiceCounts.set(key, existing);
    });

    // Avatar play statistics
    const completedPlays = Array.from(avatarPlayEvents.values()).filter(event => event.status === 'completed');
    const interruptedPlays = Array.from(avatarPlayEvents.values()).filter(event => event.status === 'interrupted');
    const averageDuration = completedPlays.length > 0
      ? completedPlays.reduce((sum, event) => sum + (event.duration || 0), 0) / completedPlays.length
      : 0;

    // Performance metrics
    const averageResponseTime = performanceMetrics.responseTimes.length > 0
      ? performanceMetrics.responseTimes.reduce((sum, time) => sum + time, 0) / performanceMetrics.responseTimes.length
      : 0;

    const averageVideoProcessingTime = performanceMetrics.videoProcessingTimes.length > 0
      ? performanceMetrics.videoProcessingTimes.reduce((sum, time) => sum + time, 0) / performanceMetrics.videoProcessingTimes.length
      : 0;

    return {
      imageTriggers: {
        total: imageTriggerEvents.size,
        recent: recentImageTriggers,
        byImage: Array.from(imageCounts.values()).sort((a, b) => b.count - a.count).slice(0, 10),
        byVoice: Array.from(voiceCounts.values()).sort((a, b) => b.count - a.count).slice(0, 10),
      },
      avatarPlays: {
        total: avatarPlayEvents.size,
        completed: completedPlays.length,
        interrupted: interruptedPlays.length,
        averageDuration: Math.round(averageDuration),
        recent: recentAvatarPlays,
      },
      performance: {
        averageResponseTime: Math.round(averageResponseTime),
        totalRequests: performanceMetrics.totalRequests,
        successfulRequests: performanceMetrics.successfulRequests,
        failedRequests: performanceMetrics.failedRequests,
        averageVideoProcessingTime: Math.round(averageVideoProcessingTime),
        mostTriggeredImages: Array.from(imageCounts.values()).sort((a, b) => b.count - a.count).slice(0, 5),
        mostUsedVoices: Array.from(voiceCounts.values()).sort((a, b) => b.count - a.count).slice(0, 5),
      },
    };
  }

  /**
   * Clean up old events (keep last 24 hours)
   */
  static cleanupOldEvents(): number {
    const now = new Date();
    const oneDayAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000);

    let cleanedCount = 0;

    // Clean image trigger events
    for (const [id, event] of imageTriggerEvents.entries()) {
      if (event.timestamp < oneDayAgo) {
        imageTriggerEvents.delete(id);
        cleanedCount++;
      }
    }

    // Clean avatar play events
    for (const [id, event] of avatarPlayEvents.entries()) {
      if (event.startTime < oneDayAgo) {
        avatarPlayEvents.delete(id);
        cleanedCount++;
      }
    }

    if (cleanedCount > 0) {
      console.log(`[ANALYTICS] Cleaned up ${cleanedCount} old events`);
    }

    return cleanedCount;
  }
}

// Cleanup old events every 6 hours
setInterval(() => {
  AnalyticsService.cleanupOldEvents();
}, 6 * 60 * 60 * 1000);
