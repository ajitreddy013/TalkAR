import { api } from "./api";

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: {
    id: string;
    email: string;
    role: string;
    name: string;
  };
}

export const AuthService = {
  login: (credentials: LoginCredentials) =>
    api.post<AuthResponse>("/api/v1/auth/login", credentials),

  logout: () => api.post("/api/v1/auth/logout"),

  getCurrentUser: () => api.get("/api/v1/auth/me"),

  refreshToken: () => api.post("/api/v1/auth/refresh"),
};
