import { Image, Dialogue } from "../models/Image";

export interface ScriptChunk {
  id: string;
  text: string;
  language: string;
  voiceId?: string;
  orderIndex: number;
  chunkSize: number;
  estimatedDuration?: number;
  isDefault: boolean;
}

export interface ScriptResponse {
  success: boolean;
  script?: ScriptChunk;
  availableScripts: ScriptChunk[];
  totalChunks: number;
  message?: string;
  analytics?: {
    imageId: string;
    scriptId: string;
    timestamp: Date;
    chunkIndex: number;
  };
}

export interface ScriptAnalytics {
  imageId: string;
  scriptId: string;
  chunkIndex: number;
  timestamp: Date;
  userAgent?: string;
  ipAddress?: string;
}

export class ScriptService {
  /**
   * Get a dynamic script chunk for a detected image
   */
  static async getScriptForImage(
    imageId: string,
    chunkIndex?: number,
    userAgent?: string,
    ipAddress?: string
  ): Promise<ScriptResponse> {
    try {
      // Find the image with all its dialogues
      const image = await Image.findByPk(imageId, {
        include: [
          {
            model: Dialogue,
            as: "dialogues",
            where: { isActive: true },
            order: [["orderIndex", "ASC"]],
          },
        ],
      });

      if (!image) {
        return {
          success: false,
          availableScripts: [],
          totalChunks: 0,
          message: "Image not found",
        };
      }

      const dialogues = image.dialogues || [];
      if (dialogues.length === 0) {
        return {
          success: false,
          availableScripts: [],
          totalChunks: 0,
          message: "No scripts available for this image",
        };
      }

      // Determine which chunk to return
      let targetIndex = chunkIndex !== undefined ? chunkIndex : 0;
      if (targetIndex >= dialogues.length) {
        targetIndex = Math.floor(Math.random() * dialogues.length); // Wrap around or random
      }

      const selectedScript = dialogues[targetIndex];

      // Log analytics data
      const analytics: ScriptAnalytics = {
        imageId,
        scriptId: selectedScript.id,
        chunkIndex: targetIndex,
        timestamp: new Date(),
        userAgent,
        ipAddress,
      };

      // Convert to ScriptChunk format
      const scriptChunk: ScriptChunk = {
        id: selectedScript.id,
        text: selectedScript.text,
        language: selectedScript.language,
        voiceId: selectedScript.voiceId,
        orderIndex: selectedScript.orderIndex,
        chunkSize: selectedScript.chunkSize,
        estimatedDuration: selectedScript.estimatedDuration,
        isDefault: selectedScript.isDefault,
      };

      // Convert all available scripts
      const availableScripts: ScriptChunk[] = dialogues.map((dialogue) => ({
        id: dialogue.id,
        text: dialogue.text,
        language: dialogue.language,
        voiceId: dialogue.voiceId,
        orderIndex: dialogue.orderIndex,
        chunkSize: dialogue.chunkSize,
        estimatedDuration: dialogue.estimatedDuration,
        isDefault: dialogue.isDefault,
      }));

      return {
        success: true,
        script: scriptChunk,
        availableScripts,
        totalChunks: dialogues.length,
        analytics: {
          imageId,
          scriptId: selectedScript.id,
          timestamp: new Date(),
          chunkIndex: targetIndex,
        },
      };
    } catch (error) {
      console.error("Error getting script for image:", error);
      return {
        success: false,
        availableScripts: [],
        totalChunks: 0,
        message: "Failed to retrieve script for image",
      };
    }
  }

  /**
   * Get all scripts for an image (for admin/management)
   */
  static async getAllScriptsForImage(imageId: string): Promise<{
    success: boolean;
    scripts?: ScriptChunk[];
    totalChunks: number;
    message?: string;
  }> {
    try {
      const image = await Image.findByPk(imageId, {
        include: [
          {
            model: Dialogue,
            as: "dialogues",
            order: [["orderIndex", "ASC"]],
          },
        ],
      });

      if (!image) {
        return {
          success: false,
          totalChunks: 0,
          message: "Image not found",
        };
      }

      const dialogues = image.dialogues || [];
      const scripts: ScriptChunk[] = dialogues.map((dialogue) => ({
        id: dialogue.id,
        text: dialogue.text,
        language: dialogue.language,
        voiceId: dialogue.voiceId,
        orderIndex: dialogue.orderIndex,
        chunkSize: dialogue.chunkSize,
        estimatedDuration: dialogue.estimatedDuration,
        isDefault: dialogue.isDefault,
      }));

      return {
        success: true,
        scripts,
        totalChunks: scripts.length,
      };
    } catch (error) {
      console.error("Error getting all scripts for image:", error);
      return {
        success: false,
        totalChunks: 0,
        message: "Failed to retrieve scripts",
      };
    }
  }

  /**
   * Create or update script chunks for an image
   */
  static async createScriptChunks(
    imageId: string,
    scripts: Array<{
      text: string;
      language: string;
      voiceId?: string;
      orderIndex: number;
      isDefault?: boolean;
    }>
  ): Promise<{
    success: boolean;
    createdScripts?: ScriptChunk[];
    message?: string;
  }> {
    try {
      // Validate image exists
      const image = await Image.findByPk(imageId);
      if (!image) {
        return {
          success: false,
          message: "Image not found",
        };
      }

      // Calculate chunk sizes and estimated durations
      const processedScripts = scripts.map((script, index) => {
        const lines = script.text.split("\n").filter((line) => line.trim());
        const estimatedDuration = Math.ceil(script.text.length / 15); // Rough estimate: 15 chars per second

        return {
          imageId,
          text: script.text,
          language: script.language,
          voiceId: script.voiceId,
          orderIndex: script.orderIndex,
          chunkSize: lines.length,
          estimatedDuration,
          isDefault: script.isDefault || index === 0,
          isActive: true,
        };
      });

      // Create dialogues in database
      const createdDialogues = await Promise.all(
        processedScripts.map((script) => Dialogue.create(script))
      );

      // Convert to ScriptChunk format
      const createdScripts: ScriptChunk[] = createdDialogues.map((dialogue) => ({
        id: dialogue.id,
        text: dialogue.text,
        language: dialogue.language,
        voiceId: dialogue.voiceId,
        orderIndex: dialogue.orderIndex,
        chunkSize: dialogue.chunkSize,
        estimatedDuration: dialogue.estimatedDuration,
        isDefault: dialogue.isDefault,
      }));

      return {
        success: true,
        createdScripts,
        message: `Created ${createdScripts.length} script chunks successfully`,
      };
    } catch (error) {
      console.error("Error creating script chunks:", error);
      return {
        success: false,
        message: "Failed to create script chunks",
      };
    }
  }

  /**
   * Estimate speaking duration for text (helper function)
   */
  static estimateSpeakingDuration(text: string): number {
    // Average speaking rate: ~150 words per minute = 2.5 words per second
    // Average word length: ~5 characters
    // So roughly 12.5 characters per second, but we use 15 for natural pauses
    const words = text.trim().split(/\s+/).length;
    const characters = text.length;
    
    // Use character-based estimation for short texts, word-based for longer
    if (characters < 100) {
      return Math.ceil(characters / 15);
    } else {
      return Math.ceil(words / 2.5);
    }
  }
}