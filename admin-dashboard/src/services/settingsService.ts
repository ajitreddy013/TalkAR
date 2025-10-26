import { api } from "./api";

export interface SettingsData {
  // Audio settings
  ambientAudioEnabled: boolean;
  ambientAudioVolume: number;
  ambientAudioType: string;
  audioFadeDuration: number;
  
  // Visual settings
  dynamicLightingEnabled: boolean;
  shadowIntensity: number;
  
  // Avatar settings
  avatarScale: number;
  avatarPositionX: number;
  avatarPositionY: number;
  avatarPositionZ: number;
  
  // Performance settings
  maxFrameRate: number;
  qualitySetting: 'low' | 'medium' | 'high' | 'ultra';
  
  // UI settings
  showDebugInfo: boolean;
  enableGestures: boolean;
}

export const SettingsService = {
  // Get current settings
  getSettings: () => api.get<{ success: boolean; settings: SettingsData }>("/api/v1/settings"),
  
  // Update settings
  updateSettings: (settings: SettingsData) => 
    api.post<{ success: boolean; message: string }>("/api/v1/settings", settings),
  
  // Reset to default settings
  resetSettings: () => api.post<{ success: boolean; message: string }>("/api/v1/settings/reset"),
};