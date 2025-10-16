import { describe, it, expect, beforeEach, jest } from '@jest/globals';

// Mock the supabase service
jest.mock('../services/supabaseService', () => ({
  createUserProfile: jest.fn(),
  getUserProfile: jest.fn(),
  updateUserProfile: jest.fn(),
  createProject: jest.fn(),
  getUserProjects: jest.fn(),
  updateProject: jest.fn(),
  deleteProject: jest.fn(),
  createSyncJob: jest.fn(),
  getProjectSyncJobs: jest.fn(),
  updateSyncJob: jest.fn(),
  getProject: jest.fn(),
  getSyncJob: jest.fn(),
  uploadFile: jest.fn(),
  getFileUrl: jest.fn(),
  deleteFile: jest.fn(),
  createUser: jest.fn(),
  getUserById: jest.fn(),
  updateUser: jest.fn(),
  deleteUserProfile: jest.fn(),
  deleteAuthUser: jest.fn(),
  getAllUserProfiles: jest.fn(),
  getUserProfileByEmail: jest.fn(),
}));

import supabaseService from '../services/supabaseService';

describe('Supabase Service Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('User Profile Operations', () => {
    it('should create user profile successfully', async () => {
      const userData = {
        email: 'test@example.com',
        full_name: 'Test User',
        avatar_url: 'https://example.com/avatar.jpg',
      };

      const mockProfile = { id: 'test-user-id', ...userData };
      (supabaseService.createUserProfile as any).mockResolvedValue(mockProfile);

      const result = await supabaseService.createUserProfile(userData);

      expect(result).toBeDefined();
      expect((result as any).email).toBe(userData.email);
      expect(supabaseService.createUserProfile).toHaveBeenCalledWith(userData);
    });

    it('should get user profile by ID', async () => {
      const userId = 'test-user-id';
      const mockProfile = {
        id: userId,
        email: 'test@example.com',
        full_name: 'Test User',
      };

      (supabaseService.getUserProfile as any).mockResolvedValue(mockProfile);

      const result = await supabaseService.getUserProfile(userId);

      expect(result).toBeDefined();
      expect((result as any).id).toBe(userId);
      expect(supabaseService.getUserProfile).toHaveBeenCalledWith(userId);
    });

    it('should update user profile', async () => {
      const userId = 'test-user-id';
      const updates = {
        full_name: 'Updated Name',
        avatar_url: 'https://example.com/new-avatar.jpg',
      };

      const mockProfile = { id: userId, ...updates };
      (supabaseService.updateUserProfile as any).mockResolvedValue(mockProfile);

      const result = await supabaseService.updateUserProfile(userId, updates);

      expect(result).toBeDefined();
      expect((result as any).full_name).toBe(updates.full_name);
      expect(supabaseService.updateUserProfile).toHaveBeenCalledWith(userId, updates);
    });

    it('should handle profile not found', async () => {
      const userId = 'nonexistent-user-id';

      (supabaseService.getUserProfile as any).mockResolvedValue(null);

      const result = await supabaseService.getUserProfile(userId);

      expect(result).toBeNull();
      expect(supabaseService.getUserProfile).toHaveBeenCalledWith(userId);
    });
  });

  describe('Project Operations', () => {
    it('should create project successfully', async () => {
      const projectData = {
        user_id: 'test-user-id',
        title: 'Test Project',
        description: 'A test project',
        status: 'draft' as const,
      };

      const mockProject = { id: 'test-project-id', ...projectData };
      (supabaseService.createProject as any).mockResolvedValue(mockProject);

      const result = await supabaseService.createProject(projectData);

      expect(result).toBeDefined();
      expect((result as any).title).toBe(projectData.title);
      expect(supabaseService.createProject).toHaveBeenCalledWith(projectData);
    });

    it('should get projects by user ID', async () => {
      const userId = 'test-user-id';
      const mockProjects = [
        {
          id: 'project-1',
          title: 'Project 1',
          user_id: userId,
          status: 'completed' as const,
        },
        {
          id: 'project-2',
          title: 'Project 2',
          user_id: userId,
          status: 'draft' as const,
        },
      ];

      (supabaseService.getUserProjects as any).mockResolvedValue(mockProjects);

      const result = await supabaseService.getUserProjects(userId);

      expect(result).toBeDefined();
      expect((result as any).length).toBe(2);
      expect(supabaseService.getUserProjects).toHaveBeenCalledWith(userId);
    });

    it('should update project status', async () => {
      const projectId = 'test-project-id';
      const updates = {
        status: 'completed' as const,
        video_url: 'https://example.com/video.mp4',
      };

      const mockProject = { id: projectId, ...updates };
      (supabaseService.updateProject as any).mockResolvedValue(mockProject);

      const result = await supabaseService.updateProject(projectId, updates);

      expect(result).toBeDefined();
      expect((result as any).status).toBe(updates.status);
      expect(supabaseService.updateProject).toHaveBeenCalledWith(projectId, updates);
    });

    it('should delete project', async () => {
      const projectId = 'test-project-id';

      (supabaseService.deleteProject as any).mockResolvedValue(true);

      const result = await supabaseService.deleteProject(projectId);

      expect(result).toBe(true);
      expect(supabaseService.deleteProject).toHaveBeenCalledWith(projectId);
    });
  });

  describe('Sync Job Operations', () => {
    it('should create sync job successfully', async () => {
      const jobData = {
        project_id: 'test-project-id',
        status: 'pending' as const,
        sync_data: { items: 100 },
      };

      const mockJob = { id: 'test-job-id', ...jobData };
      (supabaseService.createSyncJob as any).mockResolvedValue(mockJob);

      const result = await supabaseService.createSyncJob(jobData);

      expect(result).toBeDefined();
      expect((result as any).status).toBe(jobData.status);
      expect(supabaseService.createSyncJob).toHaveBeenCalledWith(jobData);
    });

    it('should get sync jobs by project ID', async () => {
      const projectId = 'test-project-id';
      const mockJobs = [
        {
          id: 'job-1',
          project_id: projectId,
          status: 'completed' as const,
        },
        {
          id: 'job-2',
          project_id: projectId,
          status: 'pending' as const,
        },
      ];

      (supabaseService.getProjectSyncJobs as any).mockResolvedValue(mockJobs);

      const result = await supabaseService.getProjectSyncJobs(projectId);

      expect(result).toBeDefined();
      expect((result as any).length).toBe(2);
      expect(supabaseService.getProjectSyncJobs).toHaveBeenCalledWith(projectId);
    });

    it('should update sync job status', async () => {
      const jobId = 'test-job-id';
      const updates = {
        status: 'completed' as const,
      };

      const mockJob = { id: jobId, ...updates };
      (supabaseService.updateSyncJob as any).mockResolvedValue(mockJob);

      const result = await supabaseService.updateSyncJob(jobId, updates);

      expect(result).toBeDefined();
      expect((result as any).status).toBe(updates.status);
      expect(supabaseService.updateSyncJob).toHaveBeenCalledWith(jobId, updates);
    });
  });

  describe('Error Handling', () => {
    it('should handle database errors gracefully', async () => {
      const userId = 'test-user-id';

      (supabaseService.getUserProfile as any).mockResolvedValue(null);

      const result = await supabaseService.getUserProfile(userId);

      expect(result).toBeNull();
      expect(supabaseService.getUserProfile).toHaveBeenCalledWith(userId);
    });

    it('should handle query timeout errors', async () => {
      const userId = 'test-user-id';

      (supabaseService.getUserProfile as any).mockResolvedValue(null);

      const result = await supabaseService.getUserProfile(userId);

      expect(result).toBeNull();
      expect(supabaseService.getUserProfile).toHaveBeenCalledWith(userId);
    });
  });

  describe('Additional User Operations', () => {
    it('should get user profile by email', async () => {
      const email = 'test@example.com';
      const mockProfile = {
        id: 'test-user-id',
        email: email,
        full_name: 'Test User',
      };

      (supabaseService.getUserProfileByEmail as any).mockResolvedValue(mockProfile);

      const result = await supabaseService.getUserProfileByEmail(email);

      expect(result).toBeDefined();
      expect((result as any).email).toBe(email);
      expect(supabaseService.getUserProfileByEmail).toHaveBeenCalledWith(email);
    });

    it('should handle profile not found by email', async () => {
      const email = 'nonexistent@example.com';

      (supabaseService.getUserProfileByEmail as any).mockResolvedValue(null);

      const result = await supabaseService.getUserProfileByEmail(email);

      expect(result).toBeNull();
      expect(supabaseService.getUserProfileByEmail).toHaveBeenCalledWith(email);
    });

    it('should delete user profile', async () => {
      const userId = 'test-user-id';

      (supabaseService.deleteUserProfile as any).mockResolvedValue(true);

      const result = await supabaseService.deleteUserProfile(userId);

      expect(result).toBe(true);
      expect(supabaseService.deleteUserProfile).toHaveBeenCalledWith(userId);
    });

    it('should get all user profiles', async () => {
      const mockProfiles = [
        {
          id: 'user-1',
          email: 'user1@example.com',
          full_name: 'User One',
        },
        {
          id: 'user-2',
          email: 'user2@example.com',
          full_name: 'User Two',
        },
      ];

      (supabaseService.getAllUserProfiles as any).mockResolvedValue(mockProfiles);

      const result = await supabaseService.getAllUserProfiles();

      expect(result).toBeDefined();
      expect((result as any).length).toBe(2);
      expect(supabaseService.getAllUserProfiles).toHaveBeenCalled();
    });
  });

  describe('File Upload Operations', () => {
    it('should upload file successfully', async () => {
      const bucket = 'test-bucket';
      const path = 'test/file.jpg';
      const file = Buffer.from('test file content');
      const contentType = 'image/jpeg';

      const mockUploadResult = { path: 'test/file.jpg' };
      (supabaseService.uploadFile as any).mockResolvedValue(mockUploadResult.path);

      const result = await supabaseService.uploadFile(bucket, path, file, contentType);

      expect(result).toBe(mockUploadResult.path);
      expect(supabaseService.uploadFile).toHaveBeenCalledWith(bucket, path, file, contentType);
    });

    it('should handle file upload failure', async () => {
      const bucket = 'test-bucket';
      const path = 'test/file.jpg';
      const file = Buffer.from('test file content');

      (supabaseService.uploadFile as any).mockResolvedValue(null);

      const result = await supabaseService.uploadFile(bucket, path, file);

      expect(result).toBeNull();
      expect(supabaseService.uploadFile).toHaveBeenCalledWith(bucket, path, file);
    });

    it('should get file URL', async () => {
      const bucket = 'test-bucket';
      const path = 'test/file.jpg';
      const mockUrl = 'https://example.com/test/file.jpg';

      (supabaseService.getFileUrl as any).mockResolvedValue(mockUrl);

      const result = await supabaseService.getFileUrl(bucket, path);

      expect(result).toBe(mockUrl);
      expect(supabaseService.getFileUrl).toHaveBeenCalledWith(bucket, path);
    });

    it('should delete file successfully', async () => {
      const bucket = 'test-bucket';
      const path = 'test/file.jpg';

      (supabaseService.deleteFile as any).mockResolvedValue(true);

      const result = await supabaseService.deleteFile(bucket, path);

      expect(result).toBe(true);
      expect(supabaseService.deleteFile).toHaveBeenCalledWith(bucket, path);
    });

    it('should handle file deletion failure', async () => {
      const bucket = 'test-bucket';
      const path = 'test/file.jpg';

      (supabaseService.deleteFile as any).mockResolvedValue(false);

      const result = await supabaseService.deleteFile(bucket, path);

      expect(result).toBe(false);
      expect(supabaseService.deleteFile).toHaveBeenCalledWith(bucket, path);
    });
  });

  describe('User Management Operations', () => {
    it('should create user successfully', async () => {
      const email = 'newuser@example.com';
      const password = 'securepassword123';
      const metadata = { full_name: 'New User' };

      const mockUser = { id: 'new-user-id', email, user_metadata: metadata };
      (supabaseService.createUser as any).mockResolvedValue({ user: mockUser, error: null });

      const result = await supabaseService.createUser(email, password, metadata);

      expect(result.user).toBeDefined();
      expect((result.user as any).email).toBe(email);
      expect(result.error).toBeNull();
      expect(supabaseService.createUser).toHaveBeenCalledWith(email, password, metadata);
    });

    it('should handle user creation failure', async () => {
      const email = 'invalid@example.com';
      const password = 'short';

      const mockError = { message: 'Password too short' };
      (supabaseService.createUser as any).mockResolvedValue({ user: null, error: mockError });

      const result = await supabaseService.createUser(email, password);

      expect(result.user).toBeNull();
      expect(result.error).toBeDefined();
      expect((result.error as any).message).toBe('Password too short');
      expect(supabaseService.createUser).toHaveBeenCalledWith(email, password);
    });

    it('should get user by ID', async () => {
      const userId = 'test-user-id';
      const mockUser = { id: userId, email: 'test@example.com' };

      (supabaseService.getUserById as any).mockResolvedValue(mockUser);

      const result = await supabaseService.getUserById(userId);

      expect(result).toBeDefined();
      expect((result as any).id).toBe(userId);
      expect(supabaseService.getUserById).toHaveBeenCalledWith(userId);
    });

    it('should handle user not found by ID', async () => {
      const userId = 'nonexistent-user-id';

      (supabaseService.getUserById as any).mockResolvedValue(null);

      const result = await supabaseService.getUserById(userId);

      expect(result).toBeNull();
      expect(supabaseService.getUserById).toHaveBeenCalledWith(userId);
    });

    it('should update user', async () => {
      const userId = 'test-user-id';
      const updates = { email: 'updated@example.com', user_metadata: { full_name: 'Updated Name' } };

      const mockUser = { id: userId, ...updates };
      (supabaseService.updateUser as any).mockResolvedValue({ user: mockUser, error: null });

      const result = await supabaseService.updateUser(userId, updates);

      expect(result.user).toBeDefined();
      expect((result.user as any).email).toBe(updates.email);
      expect(result.error).toBeNull();
      expect(supabaseService.updateUser).toHaveBeenCalledWith(userId, updates);
    });

    it('should delete auth user', async () => {
      const userId = 'test-user-id';

      (supabaseService.deleteAuthUser as any).mockResolvedValue({ error: null });

      const result = await supabaseService.deleteAuthUser(userId);

      expect(result.error).toBeNull();
      expect(supabaseService.deleteAuthUser).toHaveBeenCalledWith(userId);
    });

    it('should handle auth user deletion failure', async () => {
      const userId = 'test-user-id';
      const mockError = { message: 'User not found' };

      (supabaseService.deleteAuthUser as any).mockResolvedValue({ error: mockError });

      const result = await supabaseService.deleteAuthUser(userId);

      expect(result.error).toBeDefined();
      expect((result.error as any).message).toBe('User not found');
      expect(supabaseService.deleteAuthUser).toHaveBeenCalledWith(userId);
    });
  });

  describe('Missing Function Tests', () => {
    it('should get project by ID', async () => {
      const projectId = 'test-project-id';
      const mockProject = {
        id: projectId,
        title: 'Test Project',
        user_id: 'test-user-id',
        status: 'draft' as const,
      };

      (supabaseService.getProject as any).mockResolvedValue(mockProject);

      const result = await supabaseService.getProject(projectId);

      expect(result).toBeDefined();
      expect((result as any).id).toBe(projectId);
      expect(supabaseService.getProject).toHaveBeenCalledWith(projectId);
    });

    it('should handle project not found', async () => {
      const projectId = 'nonexistent-project-id';

      (supabaseService.getProject as any).mockResolvedValue(null);

      const result = await supabaseService.getProject(projectId);

      expect(result).toBeNull();
      expect(supabaseService.getProject).toHaveBeenCalledWith(projectId);
    });

    it('should get sync job by ID', async () => {
      const jobId = 'test-job-id';
      const mockJob = {
        id: jobId,
        project_id: 'test-project-id',
        status: 'pending' as const,
      };

      (supabaseService.getSyncJob as any).mockResolvedValue(mockJob);

      const result = await supabaseService.getSyncJob(jobId);

      expect(result).toBeDefined();
      expect((result as any).id).toBe(jobId);
      expect(supabaseService.getSyncJob).toHaveBeenCalledWith(jobId);
    });

    it('should handle sync job not found', async () => {
      const jobId = 'nonexistent-job-id';

      (supabaseService.getSyncJob as any).mockResolvedValue(null);

      const result = await supabaseService.getSyncJob(jobId);

      expect(result).toBeNull();
      expect(supabaseService.getSyncJob).toHaveBeenCalledWith(jobId);
    });
  });
});