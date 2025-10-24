import bcrypt from "bcryptjs";
const jwt = require("jsonwebtoken");
import { v4 as uuidv4 } from "uuid";

export interface User {
  id: string;
  email: string;
  password: string;
  role: "admin" | "user";
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
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
  };
}

export interface RegisterRequest {
  email: string;
  password: string;
  role?: "admin" | "user";
}

// In-memory storage for demo purposes
// In production, use database
const users = new Map<string, User>();

// Create default admin user if none exists
const createDefaultAdmin = () => {
  const adminExists = Array.from(users.values()).some(
    (user) => user.role === "admin"
  );

  if (!adminExists) {
    const adminId = uuidv4();
    const hashedPassword = bcrypt.hashSync("admin123", 10);

    const adminUser: User = {
      id: adminId,
      email: "admin@talkar.com",
      password: hashedPassword,
      role: "admin",
      isActive: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    };

    users.set(adminId, adminUser);
    console.log("Default admin user created: admin@talkar.com / admin123");
  }
};

// Initialize default admin
createDefaultAdmin();

export const registerUser = async (request: RegisterRequest): Promise<User> => {
  try {
    // Check if user already exists
    const existingUser = Array.from(users.values()).find(
      (user) => user.email === request.email
    );
    if (existingUser) {
      throw new Error("User already exists");
    }

    // Hash password
    const hashedPassword = await bcrypt.hash(request.password, 10);

    // Create user
    const userId = uuidv4();
    const user: User = {
      id: userId,
      email: request.email,
      password: hashedPassword,
      role: request.role || "user",
      isActive: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    };

    users.set(userId, user);

    return user;
  } catch (error) {
    console.error("Registration error:", error);
    throw new Error("Failed to register user");
  }
};

export const loginUser = async (
  request: LoginRequest
): Promise<LoginResponse> => {
  try {
    // Find user by email
    const user = Array.from(users.values()).find(
      (u) => u.email === request.email
    );

    if (!user) {
      throw new Error("Invalid credentials");
    }

    if (!user.isActive) {
      throw new Error("Account is deactivated");
    }

    // Verify password
    const isValidPassword = await bcrypt.compare(
      request.password,
      user.password
    );
    if (!isValidPassword) {
      throw new Error("Invalid credentials");
    }

    // Generate JWT token
    const token = jwt.sign(
      {
        userId: user.id,
        email: user.email,
        role: user.role,
      },
      process.env.JWT_SECRET || "fallback-secret",
      { expiresIn: process.env.JWT_EXPIRES_IN || "7d" }
    );

    return {
      token,
      user: {
        id: user.id,
        email: user.email,
        role: user.role,
      },
    };
  } catch (error) {
    console.error("Login error:", error);
    throw new Error("Failed to login");
  }
};

export const verifyToken = (token: string): any => {
  try {
    return jwt.verify(token, process.env.JWT_SECRET || "fallback-secret");
  } catch (error) {
    throw new Error("Invalid token");
  }
};

export const getUserById = (userId: string): User | undefined => {
  return users.get(userId);
};

export const updateUser = (
  userId: string,
  updates: Partial<User>
): User | null => {
  const user = users.get(userId);
  if (!user) return null;

  const updatedUser = { ...user, ...updates, updatedAt: new Date() };
  users.set(userId, updatedUser);

  return updatedUser;
};

export const deleteUser = (userId: string): boolean => {
  return users.delete(userId);
};

export const getAllUsers = (): User[] => {
  return Array.from(users.values());
};

export const changePassword = async (
  userId: string,
  currentPassword: string,
  newPassword: string
): Promise<boolean> => {
  try {
    const user = users.get(userId);
    if (!user) return false;

    // Verify current password
    const isValidPassword = await bcrypt.compare(
      currentPassword,
      user.password
    );
    if (!isValidPassword) {
      throw new Error("Current password is incorrect");
    }

    // Hash new password
    const hashedPassword = await bcrypt.hash(newPassword, 10);

    // Update user
    const updatedUser = {
      ...user,
      password: hashedPassword,
      updatedAt: new Date(),
    };
    users.set(userId, updatedUser);

    return true;
  } catch (error) {
    console.error("Change password error:", error);
    throw new Error("Failed to change password");
  }
};

export const resetPassword = async (email: string): Promise<boolean> => {
  try {
    const user = Array.from(users.values()).find((u) => u.email === email);
    if (!user) return false;

    // In production, send reset email
    // For now, just log the reset request
    console.log(`Password reset requested for: ${email}`);

    return true;
  } catch (error) {
    console.error("Reset password error:", error);
    throw new Error("Failed to reset password");
  }
};
