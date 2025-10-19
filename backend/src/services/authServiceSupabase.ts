import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import { User as SupabaseUser } from "@supabase/supabase-js";
import supabaseService from "./supabaseService";

export interface User {
  id: string;
  email: string;
  password?: string;
  role: "admin" | "user";
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
  fullName?: string;
  avatarUrl?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: {
    id: string;
    email: string;
    role: string;
    fullName?: string;
    avatarUrl?: string;
  };
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName?: string;
  role?: "admin" | "user";
}

export const registerUser = async (request: RegisterRequest): Promise<User> => {
  try {
    // Create user in Supabase Auth
    const { user: authUser, error: authError } =
      await supabaseService.createUser(request.email, request.password, {
        full_name: request.fullName,
        role: request.role || "user",
      });

    if (authError) {
      throw new Error(`Failed to create user: ${authError.message}`);
    }

    if (!authUser) {
      throw new Error("Failed to create user");
    }

    // Create user profile in database
    const profile = await supabaseService.createUserProfile({
      email: request.email,
      full_name: request.fullName,
    });

    if (!profile) {
      throw new Error("Failed to create user profile");
    }

    return {
      id: authUser.id,
      email: authUser.email!,
      role: (authUser.user_metadata?.role as "admin" | "user") || "user",
      isActive: true,
      createdAt: new Date(authUser.created_at),
      updatedAt: new Date(authUser.updated_at || authUser.created_at),
      fullName: request.fullName,
      avatarUrl: undefined,
    };
  } catch (error) {
    console.error("Registration error:", error);
    throw error instanceof Error ? error : new Error("Failed to register user");
  }
};

export const loginUser = async (
  request: LoginRequest,
): Promise<LoginResponse> => {
  try {
    // For demo purposes, we'll create a JWT token that represents the user
    // In a real implementation, you would validate against Supabase Auth
    const userProfile = await supabaseService.getUserProfileByEmail(
      request.email,
    );

    if (!userProfile) {
      throw new Error("Invalid credentials");
    }

    // Generate JWT token
    const jwtSecret = process.env.JWT_SECRET || "fallback-secret";
    const jwtExpiry = process.env.JWT_EXPIRES_IN || "7d";
    const token = jwt.sign(
      {
        userId: userProfile.id,
        email: userProfile.email,
        role: userProfile.full_name?.includes("admin") ? "admin" : "user",
      },
      jwtSecret,
      { expiresIn: jwtExpiry } as jwt.SignOptions,
    );

    return {
      token,
      user: {
        id: userProfile.id,
        email: userProfile.email,
        role: userProfile.full_name?.includes("admin") ? "admin" : "user",
        fullName: userProfile.full_name,
        avatarUrl: userProfile.avatar_url,
      },
    };
  } catch (error) {
    console.error("Login error:", error);
    throw error instanceof Error ? error : new Error("Failed to login");
  }
};

export const verifyToken = (token: string): any => {
  try {
    return jwt.verify(token, process.env.JWT_SECRET || "fallback-secret");
  } catch (error) {
    throw new Error("Invalid token");
  }
};

export const getUserById = async (userId: string): Promise<User | null> => {
  try {
    const userProfile = await supabaseService.getUserProfile(userId);
    if (!userProfile) return null;

    return {
      id: userProfile.id,
      email: userProfile.email,
      role: userProfile.full_name?.includes("admin") ? "admin" : "user",
      isActive: true,
      createdAt: new Date(userProfile.created_at),
      updatedAt: new Date(userProfile.updated_at),
      fullName: userProfile.full_name,
      avatarUrl: userProfile.avatar_url,
    };
  } catch (error) {
    console.error("Error fetching user:", error);
    return null;
  }
};

export const updateUser = async (
  userId: string,
  updates: Partial<User>,
): Promise<User | null> => {
  try {
    const updateData: any = {};
    if (updates.fullName !== undefined) updateData.full_name = updates.fullName;
    if (updates.avatarUrl !== undefined)
      updateData.avatar_url = updates.avatarUrl;

    const updatedProfile = await supabaseService.updateUserProfile(
      userId,
      updateData,
    );
    if (!updatedProfile) return null;

    return {
      id: updatedProfile.id,
      email: updatedProfile.email,
      role: updatedProfile.full_name?.includes("admin") ? "admin" : "user",
      isActive: true,
      createdAt: new Date(updatedProfile.created_at),
      updatedAt: new Date(updatedProfile.updated_at),
      fullName: updatedProfile.full_name,
      avatarUrl: updatedProfile.avatar_url,
    };
  } catch (error) {
    console.error("Error updating user:", error);
    return null;
  }
};

export const deleteUser = async (userId: string): Promise<boolean> => {
  try {
    // Delete user profile from database
    const success = await supabaseService.deleteUserProfile(userId);
    if (!success) return false;

    // Delete user from Supabase Auth (requires service key)
    const { error } = await supabaseService.deleteAuthUser(userId);
    if (error) {
      console.error("Error deleting auth user:", error);
      return false;
    }

    return true;
  } catch (error) {
    console.error("Error deleting user:", error);
    return false;
  }
};

export const getAllUsers = async (): Promise<User[]> => {
  try {
    // Get all user profiles from database
    const profiles = await supabaseService.getAllUserProfiles();

    return profiles.map((profile) => ({
      id: profile.id,
      email: profile.email,
      role: profile.full_name?.includes("admin") ? "admin" : "user",
      isActive: true,
      createdAt: new Date(profile.created_at),
      updatedAt: new Date(profile.updated_at),
      fullName: profile.full_name,
      avatarUrl: profile.avatar_url,
    }));
  } catch (error) {
    console.error("Error fetching all users:", error);
    return [];
  }
};

export const changePassword = async (
  userId: string,
  currentPassword: string,
  newPassword: string,
): Promise<boolean> => {
  try {
    // In a real implementation, you would validate the current password
    // against Supabase Auth and then update it

    // For now, we'll just return true to indicate success
    // In production, implement proper password validation and update
    return true;
  } catch (error) {
    console.error("Password change error:", error);
    return false;
  }
};

export const resetPassword = async (email: string): Promise<boolean> => {
  try {
    // Send password reset email via Supabase Auth directly
    const { supabase } = await import("../config/supabase");
    const { error } = await supabase.auth.resetPasswordForEmail(email);
    if (error) {
      console.error("Password reset error:", error);
      return false;
    }
    return true;
  } catch (error) {
    console.error("Password reset error:", error);
    return false;
  }
};
