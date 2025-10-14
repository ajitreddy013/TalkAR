import { supabase, supabaseAdmin } from '../config/supabase';
import { User, AuthError } from '@supabase/supabase-js';

export interface UserProfile {
  id: string;
  email: string;
  full_name?: string;
  avatar_url?: string;
  created_at: string;
  updated_at: string;
}

export interface Project {
  id: string;
  user_id: string;
  title: string;
  description?: string;
  status: 'draft' | 'processing' | 'completed' | 'failed';
  video_url?: string;
  audio_url?: string;
  avatar_url?: string;
  created_at: string;
  updated_at: string;
}

export interface SyncJob {
  id: string;
  project_id: string;
  status: 'pending' | 'processing' | 'completed' | 'failed';
  sync_data?: any;
  error_message?: string;
  created_at: string;
  updated_at: string;
}

class SupabaseService {
  // User management
  async createUser(email: string, password: string, metadata?: any): Promise<{ user: User | null; error: AuthError | null }> {
    const { data, error } = await supabaseAdmin.auth.signUp({
      email,
      password,
      options: {
        data: metadata,
      },
    });
    return { user: data?.user || null, error };
  }

  async getUserById(userId: string): Promise<User | null> {
    const { data: { user }, error } = await supabaseAdmin.auth.getUser(userId);
    if (error) {
      console.error('Error fetching user:', error);
      return null;
    }
    return user;
  }

  async updateUser(userId: string, updates: any): Promise<{ user: User | null; error: AuthError | null }> {
    const { data, error } = await supabaseAdmin.auth.updateUserById(userId, updates);
    return { user: data?.user || null, error };
  }

  // User Profile operations
  async createUserProfile(userData: Omit<UserProfile, 'id' | 'created_at' | 'updated_at'>): Promise<UserProfile | null> {
    const { data, error } = await supabase
      .from('user_profiles')
      .insert([userData])
      .select()
      .single();

    if (error) {
      console.error('Error creating user profile:', error);
      return null;
    }
    return data;
  }

  async getUserProfile(userId: string): Promise<UserProfile | null> {
    const { data, error } = await supabase
      .from('user_profiles')
      .select('*')
      .eq('id', userId)
      .single();

    if (error) {
      console.error('Error fetching user profile:', error);
      return null;
    }
    return data;
  }

  async updateUserProfile(userId: string, updates: Partial<UserProfile>): Promise<UserProfile | null> {
    const { data, error } = await supabase
      .from('user_profiles')
      .update(updates)
      .eq('id', userId)
      .select()
      .single();

    if (error) {
      console.error('Error updating user profile:', error);
      return null;
    }
    return data;
  }

  // Project operations
  async createProject(projectData: Omit<Project, 'id' | 'created_at' | 'updated_at'>): Promise<Project | null> {
    const { data, error } = await supabase
      .from('projects')
      .insert([projectData])
      .select()
      .single();

    if (error) {
      console.error('Error creating project:', error);
      return null;
    }
    return data;
  }

  async getProject(projectId: string): Promise<Project | null> {
    const { data, error } = await supabase
      .from('projects')
      .select('*')
      .eq('id', projectId)
      .single();

    if (error) {
      console.error('Error fetching project:', error);
      return null;
    }
    return data;
  }

  async getUserProjects(userId: string): Promise<Project[]> {
    const { data, error } = await supabase
      .from('projects')
      .select('*')
      .eq('user_id', userId)
      .order('created_at', { ascending: false });

    if (error) {
      console.error('Error fetching user projects:', error);
      return [];
    }
    return data || [];
  }

  async updateProject(projectId: string, updates: Partial<Project>): Promise<Project | null> {
    const { data, error } = await supabase
      .from('projects')
      .update(updates)
      .eq('id', projectId)
      .select()
      .single();

    if (error) {
      console.error('Error updating project:', error);
      return null;
    }
    return data;
  }

  async deleteProject(projectId: string): Promise<boolean> {
    const { error } = await supabase
      .from('projects')
      .delete()
      .eq('id', projectId);

    if (error) {
      console.error('Error deleting project:', error);
      return false;
    }
    return true;
  }

  // Sync Job operations
  async createSyncJob(jobData: Omit<SyncJob, 'id' | 'created_at' | 'updated_at'>): Promise<SyncJob | null> {
    const { data, error } = await supabase
      .from('sync_jobs')
      .insert([jobData])
      .select()
      .single();

    if (error) {
      console.error('Error creating sync job:', error);
      return null;
    }
    return data;
  }

  async getSyncJob(jobId: string): Promise<SyncJob | null> {
    const { data, error } = await supabase
      .from('sync_jobs')
      .select('*')
      .eq('id', jobId)
      .single();

    if (error) {
      console.error('Error fetching sync job:', error);
      return null;
    }
    return data;
  }

  async getProjectSyncJobs(projectId: string): Promise<SyncJob[]> {
    const { data, error } = await supabase
      .from('sync_jobs')
      .select('*')
      .eq('project_id', projectId)
      .order('created_at', { ascending: false });

    if (error) {
      console.error('Error fetching project sync jobs:', error);
      return [];
    }
    return data || [];
  }

  async updateSyncJob(jobId: string, updates: Partial<SyncJob>): Promise<SyncJob | null> {
    const { data, error } = await supabase
      .from('sync_jobs')
      .update(updates)
      .eq('id', jobId)
      .select()
      .single();

    if (error) {
      console.error('Error updating sync job:', error);
      return null;
    }
    return data;
  }

  // File upload operations
  async uploadFile(bucket: string, path: string, file: File | Buffer, contentType?: string): Promise<string | null> {
    const { data, error } = await supabase.storage
      .from(bucket)
      .upload(path, file, {
        contentType,
        upsert: true,
      });

    if (error) {
      console.error('Error uploading file:', error);
      return null;
    }
    return data?.path || null;
  }

  async getFileUrl(bucket: string, path: string): Promise<string | null> {
    const { data } = supabase.storage.from(bucket).getPublicUrl(path);
    return data?.publicUrl || null;
  }

  async deleteFile(bucket: string, path: string): Promise<boolean> {
    const { error } = await supabase.storage.from(bucket).remove([path]);

    if (error) {
      console.error('Error deleting file:', error);
      return false;
    }
    return true;
  }

  // Additional user operations
  async getUserProfileByEmail(email: string): Promise<UserProfile | null> {
    const { data, error } = await supabase
      .from('user_profiles')
      .select('*')
      .eq('email', email)
      .single();

    if (error) {
      console.error('Error fetching user profile by email:', error);
      return null;
    }
    return data;
  }

  async deleteUserProfile(userId: string): Promise<boolean> {
    const { error } = await supabase
      .from('user_profiles')
      .delete()
      .eq('id', userId);

    if (error) {
      console.error('Error deleting user profile:', error);
      return false;
    }
    return true;
  }

  async deleteAuthUser(userId: string): Promise<{ error: any }> {
    try {
      const { error } = await supabaseAdmin.auth.admin.deleteUser(userId);
      return { error };
    } catch (error) {
      console.error('Error deleting auth user:', error);
      return { error };
    }
  }

  async getAllUserProfiles(): Promise<UserProfile[]> {
    const { data, error } = await supabase
      .from('user_profiles')
      .select('*')
      .order('created_at', { ascending: false });

    if (error) {
      console.error('Error fetching all user profiles:', error);
      return [];
    }
    return data || [];
  }

  // Real-time subscriptions
  subscribeToProjectUpdates(projectId: string, callback: (payload: any) => void) {
    return supabase
      .channel(`project:${projectId}`)
      .on('postgres_changes', 
        { event: '*', schema: 'public', table: 'projects', filter: `id=eq.${projectId}` },
        callback
      )
      .subscribe();
  }

  subscribeToUserProjects(userId: string, callback: (payload: any) => void) {
    return supabase
      .channel(`user:${userId}:projects`)
      .on('postgres_changes',
        { event: '*', schema: 'public', table: 'projects', filter: `user_id=eq.${userId}` },
        callback
      )
      .subscribe();
  }
}

export default new SupabaseService();