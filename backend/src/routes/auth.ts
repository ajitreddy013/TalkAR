import express from "express";
import {
  registerUser,
  loginUser,
  changePassword,
  resetPassword,
  getAllUsers,
  updateUser,
  deleteUser,
} from "../services/authService";
import { authenticateAdmin, authenticateUser } from "../middleware/auth";
import { validateAuthRequest } from "../middleware/validation";

const router = express.Router();

// Register new user
router.post("/register", validateAuthRequest, async (req, res, next) => {
  try {
    const { email, password, role } = req.body;

    const user = await registerUser({ email, password, role });

    res.status(201).json({
      message: "User registered successfully",
      user: {
        id: user.id,
        email: user.email,
        role: user.role,
      },
    });
  } catch (error) {
    next(error);
    return;
  }
});

// Login user
router.post("/login", validateAuthRequest, async (req, res, next) => {
  try {
    const { email, password } = req.body;

    const result = await loginUser({ email, password });

    res.json(result);
  } catch (error) {
    next(error);
    return;
  }
});

// Change password
router.post("/change-password", authenticateUser, async (req, res, next) => {
  try {
    const { currentPassword, newPassword } = req.body;
    const userId = req.user?.userId;

    if (!userId) {
      return res.status(401).json({ error: "User not authenticated" });
    }

    await changePassword(userId, currentPassword, newPassword);

    return res.json({ message: "Password changed successfully" });
  } catch (error) {
    next(error);
    return;
  }
});

// Reset password
router.post("/reset-password", async (req, res, next) => {
  try {
    const { email } = req.body;

    await resetPassword(email);

    res.json({ message: "Password reset email sent" });
  } catch (error) {
    next(error);
    return;
  }
});

// Get all users (admin only)
router.get("/users", authenticateAdmin, async (req, res, next) => {
  try {
    const users = getAllUsers();

    res.json(
      users.map((user) => ({
        id: user.id,
        email: user.email,
        role: user.role,
        isActive: user.isActive,
        createdAt: user.createdAt,
        updatedAt: user.updatedAt,
      }))
    );
  } catch (error) {
    next(error);
    return;
  }
});

// Update user (admin only)
router.put("/users/:id", authenticateAdmin, async (req, res, next) => {
  try {
    const { id } = req.params;
    const updates = req.body;

    const updatedUser = updateUser(id, updates);

    if (!updatedUser) {
      return res.status(404).json({ error: "User not found" });
    }

    return res.json({
      id: updatedUser.id,
      email: updatedUser.email,
      role: updatedUser.role,
      isActive: updatedUser.isActive,
      updatedAt: updatedUser.updatedAt,
    });
  } catch (error) {
    next(error);
    return;
  }
});

// Delete user (admin only)
router.delete("/users/:id", authenticateAdmin, async (req, res, next) => {
  try {
    const { id } = req.params;

    const deleted = deleteUser(id);

    if (!deleted) {
      return res.status(404).json({ error: "User not found" });
    }

    return res.json({ message: "User deleted successfully" });
  } catch (error) {
    next(error);
    return;
  }
});

// Get current user profile
router.get("/profile", authenticateUser, async (req, res, next) => {
  try {
    const userId = req.user?.userId;

    if (!userId) {
      return res.status(401).json({ error: "User not authenticated" });
    }

    const user = getAllUsers().find((u) => u.id === userId);

    if (!user) {
      return res.status(404).json({ error: "User not found" });
    }

    return res.json({
      id: user.id,
      email: user.email,
      role: user.role,
      isActive: user.isActive,
      createdAt: user.createdAt,
      updatedAt: user.updatedAt,
    });
  } catch (error) {
    next(error);
    return;
  }
});

export default router;
