import { api } from "./api";

export interface ImageTriggerEvent {
  id: string;
  imageId: string;
  imageName: string;
  scriptId: string;
  scriptText: string;
  voiceId: string;
  timestamp: string;
  sessionId: string;
  deviceId?: string;
}

export interface AvatarPlayEvent {
  id: string;
  imageId: string;
  scriptId: string;
  videoId: string;
  startTime: string;
  endTime?: string;
  duration?: number;
  sessionId: string;
  deviceId?: string;
  status: 'started' | 'completed' | 'interrupted';
}

export interface AIPipelineEvent {
  id: string;
  jobId: string;
  eventType: 'script_generation' | 'audio_generation' | 'lipsync_generation' | 'ad_content_generation' | 'error';
  details: string;
  timestamp: string;
  duration?: number;
  status: 'started' | 'completed' | 'failed';
  productId?: string;
  productName?: string;
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

export interface AnalyticsData {
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
  aiPipelineEvents: {
    total: number;
    recent: AIPipelineEvent[];
    byType: Array<{ eventType: string; count: number }>;
    errors: number;
  };
  performance: PerformanceMetrics;
  timestamp: string;
}

export const AnalyticsService = {
  getAnalytics: () => api.get<{ success: boolean; analytics: AnalyticsData; timestamp: string }>("/api/v1/analytics"),
  
  getImageTriggers: () => api.get<{ success: boolean; imageTriggers: any; timestamp: string }>("/api/v1/analytics/image-triggers"),
  
  getAvatarPlays: () => api.get<{ success: boolean; avatarPlays: any; timestamp: string }>("/api/v1/analytics/avatar-plays"),
  
  getPerformance: () => api.get<{ success: boolean; performance: PerformanceMetrics; timestamp: string }>("/api/v1/analytics/performance"),
  
  getAIPipelineEvents: () => api.get<{ success: boolean; aiPipelineEvents: any; timestamp: string }>("/api/v1/analytics/ai-pipeline-events"),
};