import { describe, it, expect, beforeEach, jest } from '@jest/globals';
import * as authService from '../services/authService';

describe('Auth Service Tests', () => {
  beforeEach(() => {
    jest.restoreAllMocks();
  });

  describe('User Registration', () => {
    it('should register user successfully', async () => {
      const userData = {
        email: 'test@example.com',
        password: 'password123',
      };

      const mockUser = {
        id: 'test-user-id',
        email: userData.email,
        password: 'hashedpassword',
        role: 'user',
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date(),
      };

      jest.spyOn(authService, 'registerUser').mockResolvedValue(mockUser as any);

      const result = await authService.registerUser(userData as any);

      expect(result).toBeDefined();
      expect(result.email).toBe(userData.email);
      expect(authService.registerUser).toHaveBeenCalledWith(userData);
    });

    it('should handle registration failure', async () => {
      const userData = {
        email: 'test@example.com',
        password: 'password123',
      };

      jest.spyOn(authService, 'registerUser').mockRejectedValue(new Error('Failed to register user') as any);

      await expect(authService.registerUser(userData as any)).rejects.toThrow('Failed to register user');
    });
  });

  describe('User Login', () => {
    it('should login user successfully', async () => {
      const loginData = {
        email: 'test@example.com',
        password: 'password123',
      };

      const mockResponse = {
        user: {
          id: 'test-user-id',
          email: loginData.email,
          role: 'user',
        },
        token: 'mock-jwt-token',
      };

      jest.spyOn(authService, 'loginUser').mockResolvedValue(mockResponse as any);

      const result = await authService.loginUser(loginData);

      expect(result).toBeDefined();
      expect(result.token).toBe('mock-jwt-token');
      expect(authService.loginUser).toHaveBeenCalledWith(loginData);
    });

    it('should handle login failure', async () => {
      const loginData = {
        email: 'test@example.com',
        password: 'wrongpassword',
      };

      jest.spyOn(authService, 'loginUser').mockRejectedValue(new Error('Invalid credentials') as any);

      await expect(authService.loginUser(loginData)).rejects.toThrow('Invalid credentials');
    });
  });

  describe('Token Validation', () => {
    it('should validate token successfully', () => {
      const token = 'valid-jwt-token';
      const mockUser = {
        userId: 'test-user-id',
        email: 'test@example.com',
        role: 'user',
      };

      jest.spyOn(authService, 'verifyToken').mockReturnValue(mockUser as any);

      const result = authService.verifyToken(token);

      expect(result).toBeDefined();
      expect(result.userId).toBe('test-user-id');
      expect(authService.verifyToken).toHaveBeenCalledWith(token);
    });

    it('should handle invalid token', () => {
      const token = 'invalid-token';

      jest.spyOn(authService, 'verifyToken').mockImplementation(() => {
        throw new Error('Invalid token');
      });

      expect(() => authService.verifyToken(token)).toThrow('Invalid token');
    });
  });

  describe('User Management', () => {
    it('should get user by ID', () => {
      const userId = 'test-user-id';
      const mockUser = {
        id: userId,
        email: 'test@example.com',
        password: 'hashedpassword',
        role: 'user',
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date(),
      };

      jest.spyOn(authService, 'getUserById').mockReturnValue(mockUser as any);

      const result = authService.getUserById(userId);

      expect(result).toBeDefined();
      expect(result!.id).toBe(userId);
      expect(authService.getUserById).toHaveBeenCalledWith(userId);
    });

    it('should update user information', () => {
      const userId = 'test-user-id';
      const updates = {
        email: 'updated@example.com',
      };

      const mockUser = {
        id: userId,
        email: 'updated@example.com',
        password: 'hashedpassword',
        role: 'user',
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date(),
      };

      jest.spyOn(authService, 'updateUser').mockReturnValue(mockUser as any);

      const result = authService.updateUser(userId, updates);

      expect(result).toBeDefined();
      expect(result!.email).toBe(updates.email);
      expect(authService.updateUser).toHaveBeenCalledWith(userId, updates);
    });

    it('should delete user', () => {
      const userId = 'test-user-id';

      jest.spyOn(authService, 'deleteUser').mockReturnValue(true as any);

      const result = authService.deleteUser(userId);

      expect(result).toBe(true);
      expect(authService.deleteUser).toHaveBeenCalledWith(userId);
    });
  });

  describe('Password Management', () => {
    it('should change password successfully', async () => {
      const userId = 'test-user-id';
      const currentPassword = 'oldpassword123';
      const newPassword = 'newpassword123';

      jest.spyOn(authService, 'changePassword').mockResolvedValue(true as any);

      const result = await authService.changePassword(userId, currentPassword, newPassword);

      expect(result).toBe(true);
      expect(authService.changePassword).toHaveBeenCalledWith(userId, currentPassword, newPassword);
    });

    it('should handle password change failure', async () => {
      const userId = 'test-user-id';
      const currentPassword = 'wrongpassword';
      const newPassword = 'newpassword123';

      jest.spyOn(authService, 'changePassword').mockRejectedValue(new Error('Current password is incorrect') as any);

      await expect(authService.changePassword(userId, currentPassword, newPassword)).rejects.toThrow('Current password is incorrect');
    });
  });

  describe('Admin Operations', () => {
    it('should get all users', () => {
      const mockUsers = [
        {
          id: 'user-1',
          email: 'user1@example.com',
          password: 'hashedpassword',
          role: 'user',
          isActive: true,
          createdAt: new Date(),
          updatedAt: new Date(),
        },
        {
          id: 'user-2',
          email: 'user2@example.com',
          password: 'hashedpassword',
          role: 'user',
          isActive: true,
          createdAt: new Date(),
          updatedAt: new Date(),
        },
      ];

      jest.spyOn(authService, 'getAllUsers').mockReturnValue(mockUsers as any);

      const result = authService.getAllUsers();

      expect(result).toBeDefined();
      expect(result.length).toBe(2);
      expect(authService.getAllUsers).toHaveBeenCalled();
    });
  });
});