import {
  registerUser,
  loginUser,
  changePassword,
  resetPassword,
  getAllUsers,
  updateUser,
  deleteUser,
  User,
} from "../services/authService";

describe("AuthService", () => {
  beforeEach(() => {
    // Clear users before each test
    const users = getAllUsers();
    users.forEach((user) => deleteUser(user.id));
  });

  describe("registerUser", () => {
    it("should register a new user successfully", async () => {
      const userData = {
        email: "test@example.com",
        password: "password123",
        role: "user" as const,
      };

      const user = await registerUser(userData);

      expect(user).toBeDefined();
      expect(user.email).toBe(userData.email);
      expect(user.role).toBe(userData.role);
      expect(user.isActive).toBe(true);
      expect(user.password).not.toBe(userData.password); // Should be hashed
    });

    it("should throw error for duplicate email", async () => {
      const userData = {
        email: "test@example.com",
        password: "password123",
      };

      await registerUser(userData);

      await expect(registerUser(userData)).rejects.toThrow(
        "User already exists"
      );
    });

    it("should hash password securely", async () => {
      const userData = {
        email: "test@example.com",
        password: "password123",
      };

      const user = await registerUser(userData);

      expect(user.password).not.toBe(userData.password);
      expect(user.password.length).toBeGreaterThan(50); // bcrypt hash length
    });
  });

  describe("loginUser", () => {
    beforeEach(async () => {
      await registerUser({
        email: "test@example.com",
        password: "password123",
      });
    });

    it("should login with valid credentials", async () => {
      const loginData = {
        email: "test@example.com",
        password: "password123",
      };

      const result = await loginUser(loginData);

      expect(result.token).toBeDefined();
      expect(result.user.email).toBe(loginData.email);
      expect(result.user.id).toBeDefined();
    });

    it("should throw error for invalid email", async () => {
      const loginData = {
        email: "nonexistent@example.com",
        password: "password123",
      };

      await expect(loginUser(loginData)).rejects.toThrow("Invalid credentials");
    });

    it("should throw error for invalid password", async () => {
      const loginData = {
        email: "test@example.com",
        password: "wrongpassword",
      };

      await expect(loginUser(loginData)).rejects.toThrow("Invalid credentials");
    });

    it("should throw error for inactive user", async () => {
      const users = getAllUsers();
      const user = users.find((u) => u.email === "test@example.com");
      if (user) {
        updateUser(user.id, { isActive: false });
      }

      const loginData = {
        email: "test@example.com",
        password: "password123",
      };

      await expect(loginUser(loginData)).rejects.toThrow(
        "Account is deactivated"
      );
    });
  });

  describe("changePassword", () => {
    let userId: string;

    beforeEach(async () => {
      const user = await registerUser({
        email: "test@example.com",
        password: "password123",
      });
      userId = user.id;
    });

    it("should change password with valid current password", async () => {
      const result = await changePassword(
        userId,
        "password123",
        "newpassword123"
      );

      expect(result).toBe(true);

      // Verify new password works
      const loginResult = await loginUser({
        email: "test@example.com",
        password: "newpassword123",
      });
      expect(loginResult.token).toBeDefined();
    });

    it("should throw error for invalid current password", async () => {
      await expect(
        changePassword(userId, "wrongpassword", "newpassword123")
      ).rejects.toThrow("Current password is incorrect");
    });
  });

  describe("resetPassword", () => {
    beforeEach(async () => {
      await registerUser({
        email: "test@example.com",
        password: "password123",
      });
    });

    it("should return true for existing email", async () => {
      const result = await resetPassword("test@example.com");
      expect(result).toBe(true);
    });

    it("should return false for non-existing email", async () => {
      const result = await resetPassword("nonexistent@example.com");
      expect(result).toBe(false);
    });
  });

  describe("getAllUsers", () => {
    it("should return all users", async () => {
      await registerUser({
        email: "user1@example.com",
        password: "password123",
      });
      await registerUser({
        email: "user2@example.com",
        password: "password123",
      });

      const users = getAllUsers();
      expect(users.length).toBeGreaterThanOrEqual(2);
    });
  });

  describe("updateUser", () => {
    let userId: string;

    beforeEach(async () => {
      const user = await registerUser({
        email: "test@example.com",
        password: "password123",
      });
      userId = user.id;
    });

    it("should update user successfully", () => {
      const updates = { isActive: false };
      const updatedUser = updateUser(userId, updates);

      expect(updatedUser).toBeDefined();
      expect(updatedUser?.isActive).toBe(false);
    });

    it("should return null for non-existing user", () => {
      const updates = { isActive: false };
      const updatedUser = updateUser("non-existing-id", updates);

      expect(updatedUser).toBeNull();
    });
  });

  describe("deleteUser", () => {
    let userId: string;

    beforeEach(async () => {
      const user = await registerUser({
        email: "test@example.com",
        password: "password123",
      });
      userId = user.id;
    });

    it("should delete user successfully", () => {
      const result = deleteUser(userId);
      expect(result).toBe(true);

      const user = getAllUsers().find((u) => u.id === userId);
      expect(user).toBeUndefined();
    });

    it("should return false for non-existing user", () => {
      const result = deleteUser("non-existing-id");
      expect(result).toBe(false);
    });
  });
});
