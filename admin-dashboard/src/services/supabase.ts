import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.REACT_APP_SUPABASE_URL || '';
const supabaseAnonKey = process.env.REACT_APP_SUPABASE_ANON_KEY || '';

if (!supabaseUrl || !supabaseAnonKey) {
  console.warn('Supabase environment variables are not set. Using mock client.');
}

export const supabase = createClient(supabaseUrl, supabaseAnonKey);

// Auth functions
export const signUp = async (email: string, password: string, metadata?: any) => {
  return await supabase.auth.signUp({
    email,
    password,
    options: {
      data: metadata,
    },
  });
};

export const signIn = async (email: string, password: string) => {
  return await supabase.auth.signInWithPassword({
    email,
    password,
  });
};

export const signOut = async () => {
  return await supabase.auth.signOut();
};

export const getCurrentUser = async () => {
  const { data: { user } } = await supabase.auth.getUser();
  return user;
};

// User profile functions
export const getUserProfile = async (userId: string) => {
  const { data, error } = await supabase
    .from('user_profiles')
    .select('*')
    .eq('id', userId)
    .single();
  
  return { data, error };
};

export const updateUserProfile = async (userId: string, updates: any) => {
  const { data, error } = await supabase
    .from('user_profiles')
    .update(updates)
    .eq('id', userId)
    .select()
    .single();
  
  return { data, error };
};

// Project functions
export const getProjects = async (userId: string) => {
  const { data, error } = await supabase
    .from('projects')
    .select('*')
    .eq('user_id', userId)
    .order('created_at', { ascending: false });
  
  return { data, error };
};

export const getProject = async (projectId: string) => {
  const { data, error } = await supabase
    .from('projects')
    .select('*')
    .eq('id', projectId)
    .single();
  
  return { data, error };
};

export const createProject = async (projectData: any) => {
  const { data, error } = await supabase
    .from('projects')
    .insert([projectData])
    .select()
    .single();
  
  return { data, error };
};

export const updateProject = async (projectId: string, updates: any) => {
  const { data, error } = await supabase
    .from('projects')
    .update(updates)
    .eq('id', projectId)
    .select()
    .single();
  
  return { data, error };
};

export const deleteProject = async (projectId: string) => {
  const { error } = await supabase
    .from('projects')
    .delete()
    .eq('id', projectId);
  
  return { error };
};

// Sync job functions
export const getSyncJobs = async (projectId: string) => {
  const { data, error } = await supabase
    .from('sync_jobs')
    .select('*')
    .eq('project_id', projectId)
    .order('created_at', { ascending: false });
  
  return { data, error };
};

export const createSyncJob = async (jobData: any) => {
  const { data, error } = await supabase
    .from('sync_jobs')
    .insert([jobData])
    .select()
    .single();
  
  return { data, error };
};

// File upload functions
export const uploadFile = async (bucket: string, path: string, file: File) => {
  const { data, error } = await supabase.storage
    .from(bucket)
    .upload(path, file, {
      cacheControl: '3600',
      upsert: false,
    });
  
  return { data, error };
};

export const getFileUrl = async (bucket: string, path: string) => {
  const { data } = supabase.storage.from(bucket).getPublicUrl(path);
  return data.publicUrl;
};

export const deleteFile = async (bucket: string, path: string) => {
  const { error } = await supabase.storage.from(bucket).remove([path]);
  return { error };
};

// Real-time subscriptions
export const subscribeToProjectUpdates = (projectId: string, callback: (payload: any) => void) => {
  return supabase
    .channel(`project:${projectId}`)
    .on('postgres_changes', 
      { event: '*', schema: 'public', table: 'projects', filter: `id=eq.${projectId}` },
      callback
    )
    .subscribe();
};

export const subscribeToUserProjects = (userId: string, callback: (payload: any) => void) => {
  return supabase
    .channel(`user:${userId}:projects`)
    .on('postgres_changes',
      { event: '*', schema: 'public', table: 'projects', filter: `user_id=eq.${userId}` },
      callback
    )
    .subscribe();
};

export default supabase;