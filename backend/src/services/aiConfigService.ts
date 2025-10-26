import { AIConfig } from "../models/AIConfig";

export class AIConfigService {
  /**
   * Get AI configuration value by key
   */
  static async getConfig(key: string): Promise<string | null> {
    try {
      const config = await AIConfig.findOne({ where: { key } });
      return config ? config.value : null;
    } catch (error) {
      console.error(`Error getting AI config for key ${key}:`, error);
      return null;
    }
  }

  /**
   * Set AI configuration value by key
   */
  static async setConfig(key: string, value: string, description?: string): Promise<boolean> {
    try {
      const [config, created] = await AIConfig.findOrCreate({
        where: { key },
        defaults: { key, value, description }
      });

      if (!created) {
        await config.update({ value, description });
      }

      return true;
    } catch (error) {
      console.error(`Error setting AI config for key ${key}:`, error);
      return false;
    }
  }

  /**
   * Get all AI configurations
   */
  static async getAllConfigs(): Promise<{ [key: string]: string }> {
    try {
      const configs = await AIConfig.findAll();
      const result: { [key: string]: string } = {};
      
      configs.forEach(config => {
        result[config.key] = config.value;
      });

      return result;
    } catch (error) {
      console.error("Error getting all AI configs:", error);
      return {};
    }
  }

  /**
   * Get default tone
   */
  static async getDefaultTone(): Promise<string> {
    const tone = await this.getConfig("default_tone");
    return tone || "friendly";
  }

  /**
   * Set default tone
   */
  static async setDefaultTone(tone: string): Promise<boolean> {
    return await this.setConfig("default_tone", tone, "Default tone for AI-generated content");
  }

  /**
   * Get prompt template
   */
  static async getPromptTemplate(): Promise<string> {
    const template = await this.getConfig("prompt_template");
    return template || "Create a short, engaging script for a product advertisement. The product is {product}. Highlight its key features and benefits in a {tone} tone.";
  }

  /**
   * Set prompt template
   */
  static async setPromptTemplate(template: string): Promise<boolean> {
    return await this.setConfig("prompt_template", template, "Prompt template for AI-generated content");
  }

  /**
   * Get default language
   */
  static async getDefaultLanguage(): Promise<string> {
    const language = await this.getConfig("default_language");
    return language || "en";
  }

  /**
   * Set default language
   */
  static async setDefaultLanguage(language: string): Promise<boolean> {
    return await this.setConfig("default_language", language, "Default language for AI-generated content");
  }

  /**
   * Get default avatar ID
   */
  static async getDefaultAvatarId(): Promise<string | null> {
    return await this.getConfig("default_avatar_id");
  }

  /**
   * Set default avatar ID
   */
  static async setDefaultAvatarId(avatarId: string): Promise<boolean> {
    return await this.setConfig("default_avatar_id", avatarId, "Default avatar ID for AI-generated content");
  }
}