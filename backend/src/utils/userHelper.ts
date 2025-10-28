import fs from 'fs';
import path from 'path';

interface UserPreferences {
  language: string;
  preferred_tone: string;
  user_id: string;
  preferences: {
    volume: number;
    playback_speed: number;
    auto_play: boolean;
    notifications: boolean;
  };
  last_updated: string;
}

let userPrefsCache: UserPreferences | null = null;

/**
 * Load user preferences from JSON file
 */
function loadUserPreferences(): UserPreferences {
  if (userPrefsCache) {
    return userPrefsCache;
  }

  try {
    const dataPath = path.join(__dirname, '../../data/user_preferences.json');
    const data = fs.readFileSync(dataPath, 'utf8');
    userPrefsCache = JSON.parse(data);
    return userPrefsCache!;
  } catch (error) {
    console.error('Error loading user preferences:', error);
    // Return default preferences if file doesn't exist
    return {
      language: "English",
      preferred_tone: "friendly",
      user_id: "default_user",
      preferences: {
        volume: 0.8,
        playback_speed: 1.0,
        auto_play: true,
        notifications: true
      },
      last_updated: new Date().toISOString()
    };
  }
}

/**
 * Get user preferences
 */
export function getUserPreferences(): UserPreferences {
  return loadUserPreferences();
}

/**
 * Update user preferences
 */
export function updateUserPreferences(newPrefs: Partial<UserPreferences>): UserPreferences {
  const currentPrefs = loadUserPreferences();
  const updatedPrefs = {
    ...currentPrefs,
    ...newPrefs,
    last_updated: new Date().toISOString()
  };

  try {
    const dataPath = path.join(__dirname, '../../data/user_preferences.json');
    fs.writeFileSync(dataPath, JSON.stringify(updatedPrefs, null, 2));
    userPrefsCache = updatedPrefs;
    return updatedPrefs;
  } catch (error) {
    console.error('Error updating user preferences:', error);
    throw new Error('Failed to update user preferences');
  }
}

/**
 * Get user's preferred language
 */
export function getUserLanguage(): string {
  return getUserPreferences().language;
}

/**
 * Get user's preferred tone
 */
export function getUserTone(): string {
  return getUserPreferences().preferred_tone;
}

/**
 * Clear cache (useful for testing)
 */
export function clearUserPrefsCache(): void {
  userPrefsCache = null;
}
