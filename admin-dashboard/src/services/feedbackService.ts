import { api } from "./api";

export interface Feedback {
  id: string;
  adContentId: string;
  productName: string;
  isPositive: boolean;
  timestamp: number;
  createdAt: string;
  updatedAt: string;
}

export interface FeedbackStats {
  total: number;
  positive: number;
  negative: number;
  positivePercentage: number;
  negativePercentage: number;
  byProduct: Array<{
    productName: string;
    totalCount: number;
    positiveCount: number;
    negativeCount: number;
    positivePercentage: number;
  }>;
}

export const FeedbackService = {
  // Get all feedback
  getFeedback: () => api.get<{ success: boolean; feedbacks: Feedback[]; count: number }>("/api/v1/feedback"),
  
  // Get feedback statistics
  getFeedbackStats: () => api.get<{ success: boolean; stats: FeedbackStats }>("/api/v1/feedback/stats"),
  
  // Get recent feedback
  getRecentFeedback: (limit?: number) => 
    api.get<{ success: boolean; feedbacks: Feedback[] }>("/api/v1/feedback/recent" + (limit ? `?limit=${limit}` : "")),
};