import axios from "axios";

export const API_BASE_URL =
  process.env.REACT_APP_API_URL || "http://localhost:3000";

// Derive the backend origin (e.g., http://localhost:3000) for building absolute asset URLs
export const API_ORIGIN = new URL(API_BASE_URL).origin;

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Don't set Content-Type for FormData - let the browser set it automatically
    if (config.data instanceof FormData) {
      delete config.headers["Content-Type"];
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle errors
api.interceptors.response.use(
  (response) => {
    console.log(
      "API Response:",
      response.config.url,
      response.status,
      response.data
    );
    return response;
  },
  (error) => {
    console.error(
      "API Error:",
      error.config?.url,
      error.response?.status,
      error.message
    );
    if (error.response?.status === 401) {
      localStorage.removeItem("token");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);
