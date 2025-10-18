import { DataTypes, Model, Optional } from "sequelize";
import { sequelize } from "../config/database";

export interface AvatarAttributes {
  id: string;
  name: string;
  description?: string;
  avatarImageUrl: string; // 2D preview image
  avatarVideoUrl?: string; // Optional 2D video fallback
  avatar3DModelUrl?: string; // 3D model file path (GLB/GLTF)
  voiceId?: string; // Voice ID for TTS
  idleAnimationType?: string; // Type of idle animation (breathing, blinking, etc.)
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export interface AvatarCreationAttributes
  extends Optional<AvatarAttributes, "id" | "createdAt" | "updatedAt"> {}

export class Avatar
  extends Model<AvatarAttributes, AvatarCreationAttributes>
  implements AvatarAttributes
{
  public id!: string;
  public name!: string;
  public description?: string;
  public avatarImageUrl!: string;
  public avatarVideoUrl?: string;
  public avatar3DModelUrl?: string;
  public voiceId?: string;
  public idleAnimationType?: string;
  public isActive!: boolean;
  public readonly createdAt!: Date;
  public readonly updatedAt!: Date;
}

// Initialize Avatar model
Avatar.init(
  {
    id: {
      type: DataTypes.UUID,
      defaultValue: DataTypes.UUIDV4,
      primaryKey: true,
    },
    name: {
      type: DataTypes.STRING,
      allowNull: false,
    },
    description: {
      type: DataTypes.TEXT,
      allowNull: true,
    },
    avatarImageUrl: {
      type: DataTypes.STRING,
      allowNull: false,
    },
    avatarVideoUrl: {
      type: DataTypes.STRING,
      allowNull: true,
    },
    avatar3DModelUrl: {
      type: DataTypes.STRING,
      allowNull: true,
      comment: "Path to 3D model file (GLB/GLTF format)",
    },
    voiceId: {
      type: DataTypes.STRING,
      allowNull: true,
      comment: "Voice ID for text-to-speech",
    },
    idleAnimationType: {
      type: DataTypes.STRING,
      allowNull: true,
      defaultValue: "breathing",
      comment: "Type of idle animation (breathing, blinking, combined)",
    },
    isActive: {
      type: DataTypes.BOOLEAN,
      defaultValue: true,
    },
    createdAt: {
      type: DataTypes.DATE,
      allowNull: false,
    },
    updatedAt: {
      type: DataTypes.DATE,
      allowNull: false,
    },
  },
  {
    sequelize,
    tableName: "avatars",
    timestamps: true,
  }
);
