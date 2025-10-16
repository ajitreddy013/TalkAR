/**
 * Database Types for Supabase Integration
 * Auto-generated types for TalkAR database schema
 */

export interface Database {
  public: {
    Tables: {
      users: {
        Row: {
          id: string;
          email: string;
          username: string;
          created_at: string;
          updated_at: string;
          avatar_url?: string;
          is_active: boolean;
        };
        Insert: {
          id?: string;
          email: string;
          username: string;
          created_at?: string;
          updated_at?: string;
          avatar_url?: string;
          is_active?: boolean;
        };
        Update: {
          email?: string;
          username?: string;
          updated_at?: string;
          avatar_url?: string;
          is_active?: boolean;
        };
      };
      projects: {
        Row: {
          id: string;
          user_id: string;
          name: string;
          description?: string;
          created_at: string;
          updated_at: string;
          status: 'active' | 'archived' | 'deleted';
        };
        Insert: {
          id?: string;
          user_id: string;
          name: string;
          description?: string;
          created_at?: string;
          updated_at?: string;
          status?: 'active' | 'archived' | 'deleted';
        };
        Update: {
          name?: string;
          description?: string;
          updated_at?: string;
          status?: 'active' | 'archived' | 'deleted';
        };
      };
      sync_jobs: {
        Row: {
          id: string;
          user_id: string;
          project_id: string;
          script_id: string;
          video_id: string;
          status: 'pending' | 'processing' | 'completed' | 'failed';
          progress: number;
          created_at: string;
          updated_at: string;
          completed_at?: string;
          error_message?: string;
        };
        Insert: {
          id?: string;
          user_id: string;
          project_id: string;
          script_id: string;
          video_id: string;
          status?: 'pending' | 'processing' | 'completed' | 'failed';
          progress?: number;
          created_at?: string;
          updated_at?: string;
          completed_at?: string;
          error_message?: string;
        };
        Update: {
          status?: 'pending' | 'processing' | 'completed' | 'failed';
          progress?: number;
          updated_at?: string;
          completed_at?: string;
          error_message?: string;
        };
      };
      images: {
        Row: {
          id: string;
          user_id: string;
          project_id: string;
          name: string;
          url: string;
          s3_key: string;
          file_size: number;
          mime_type: string;
          created_at: string;
          updated_at: string;
          metadata?: Record<string, any>;
        };
        Insert: {
          id?: string;
          user_id: string;
          project_id: string;
          name: string;
          url: string;
          s3_key: string;
          file_size: number;
          mime_type: string;
          created_at?: string;
          updated_at?: string;
          metadata?: Record<string, any>;
        };
        Update: {
          name?: string;
          url?: string;
          s3_key?: string;
          file_size?: number;
          mime_type?: string;
          updated_at?: string;
          metadata?: Record<string, any>;
        };
      };
      scripts: {
        Row: {
          id: string;
          user_id: string;
          project_id: string;
          name: string;
          content: string;
          language: string;
          voice_id: string;
          created_at: string;
          updated_at: string;
          duration?: number;
        };
        Insert: {
          id?: string;
          user_id: string;
          project_id: string;
          name: string;
          content: string;
          language: string;
          voice_id: string;
          created_at?: string;
          updated_at?: string;
          duration?: number;
        };
        Update: {
          name?: string;
          content?: string;
          language?: string;
          voice_id?: string;
          updated_at?: string;
          duration?: number;
        };
      };
      analytics_events: {
        Row: {
          id: string;
          user_id?: string;
          event_type: string;
          event_data: Record<string, any>;
          created_at: string;
          session_id?: string;
          device_id?: string;
        };
        Insert: {
          id?: string;
          user_id?: string;
          event_type: string;
          event_data: Record<string, any>;
          created_at?: string;
          session_id?: string;
          device_id?: string;
        };
        Update: {
          event_data?: Record<string, any>;
        };
      };
    };
    Views: {
      [_ in never]: never;
    };
    Functions: {
      [_ in never]: never;
    };
    Enums: {
      [_ in never]: never;
    };
  };
}

// Export commonly used types
export type Tables<T extends keyof Database['public']['Tables']> = 
  Database['public']['Tables'][T]['Row'];

export type InsertTables<T extends keyof Database['public']['Tables']> = 
  Database['public']['Tables'][T]['Insert'];

export type UpdateTables<T extends keyof Database['public']['Tables']> = 
  Database['public']['Tables'][T]['Update'];

// Specific table types for convenience
export type User = Tables<'users'>;
export type Project = Tables<'projects'>;
export type SyncJob = Tables<'sync_jobs'>;
export type Image = Tables<'images'>;
export type Script = Tables<'scripts'>;
export type AnalyticsEvent = Tables<'analytics_events'>;