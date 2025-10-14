import { useState, useEffect, useCallback } from 'react';
import { User } from '@supabase/supabase-js';
import {
  getProjects,
  getProject,
  createProject,
  updateProject,
  deleteProject,
  getSyncJobs,
  createSyncJob,
  getUserProfile,
  updateUserProfile,
  subscribeToProjectUpdates,
  subscribeToUserProjects,
} from '../services/supabase';

// Hook for managing projects
export const useProjects = (userId: string) => {
  const [projects, setProjects] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchProjects = useCallback(async () => {
    try {
      setLoading(true);
      const { data, error: fetchError } = await getProjects(userId);
      if (fetchError) throw fetchError;
      setProjects(data || []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch projects');
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    fetchProjects();

    // Subscribe to real-time updates
    const subscription = subscribeToUserProjects(userId, (payload) => {
      if (payload.eventType === 'INSERT') {
        setProjects(prev => [payload.new, ...prev]);
      } else if (payload.eventType === 'UPDATE') {
        setProjects(prev => prev.map(p => p.id === payload.new.id ? payload.new : p));
      } else if (payload.eventType === 'DELETE') {
        setProjects(prev => prev.filter(p => p.id !== payload.old.id));
      }
    });

    return () => {
      subscription.unsubscribe();
    };
  }, [userId, fetchProjects]);

  const addProject = async (projectData: any) => {
    try {
      const { data, error: createError } = await createProject({
        ...projectData,
        user_id: userId,
      });
      if (createError) throw createError;
      return data;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create project');
      throw err;
    }
  };

  const updateProjectData = async (projectId: string, updates: any) => {
    try {
      const { data, error: updateError } = await updateProject(projectId, updates);
      if (updateError) throw updateError;
      return data;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update project');
      throw err;
    }
  };

  const removeProject = async (projectId: string) => {
    try {
      const { error: deleteError } = await deleteProject(projectId);
      if (deleteError) throw deleteError;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete project');
      throw err;
    }
  };

  return {
    projects,
    loading,
    error,
    fetchProjects,
    addProject,
    updateProject: updateProjectData,
    removeProject,
  };
};

// Hook for managing a single project
export const useProject = (projectId: string) => {
  const [project, setProject] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchProject = useCallback(async () => {
    try {
      setLoading(true);
      const { data, error: fetchError } = await getProject(projectId);
      if (fetchError) throw fetchError;
      setProject(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch project');
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    fetchProject();

    // Subscribe to real-time updates
    const subscription = subscribeToProjectUpdates(projectId, (payload) => {
      if (payload.eventType === 'UPDATE') {
        setProject(payload.new);
      }
    });

    return () => {
      subscription.unsubscribe();
    };
  }, [projectId, fetchProject]);

  const updateProjectData = async (updates: any) => {
    try {
      const { data, error: updateError } = await updateProject(projectId, updates);
      if (updateError) throw updateError;
      setProject(data);
      return data;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update project');
      throw err;
    }
  };

  return {
    project,
    loading,
    error,
    fetchProject,
    updateProject: updateProjectData,
  };
};

// Hook for managing sync jobs
export const useSyncJobs = (projectId: string) => {
  const [syncJobs, setSyncJobs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchSyncJobs = useCallback(async () => {
    try {
      setLoading(true);
      const { data, error: fetchError } = await getSyncJobs(projectId);
      if (fetchError) throw fetchError;
      setSyncJobs(data || []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch sync jobs');
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    fetchSyncJobs();
  }, [fetchSyncJobs]);

  const addSyncJob = async (jobData: any) => {
    try {
      const { data, error: createError } = await createSyncJob({
        ...jobData,
        project_id: projectId,
      });
      if (createError) throw createError;
      setSyncJobs(prev => [data, ...prev]);
      return data;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create sync job');
      throw err;
    }
  };

  return {
    syncJobs,
    loading,
    error,
    fetchSyncJobs,
    addSyncJob,
  };
};

// Hook for managing user profile
export const useUserProfile = (userId: string) => {
  const [profile, setProfile] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchProfile = useCallback(async () => {
    try {
      setLoading(true);
      const { data, error: fetchError } = await getUserProfile(userId);
      if (fetchError) throw fetchError;
      setProfile(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch user profile');
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    fetchProfile();
  }, [fetchProfile]);

  const updateProfile = async (updates: any) => {
    try {
      const { data, error: updateError } = await updateUserProfile(userId, updates);
      if (updateError) throw updateError;
      setProfile(data);
      return data;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update user profile');
      throw err;
    }
  };

  return {
    profile,
    loading,
    error,
    fetchProfile,
    updateProfile,
  };
};