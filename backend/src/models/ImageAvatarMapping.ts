import { DataTypes, Model, Optional } from "sequelize";
import { sequelize } from "../config/database";

export interface ImageAvatarMappingAttributes {
  id: string;
  imageId: string;
  avatarId: string;
  script?: string; // Custom script for this specific mapping
  audioUrl?: string; // Generated audio URL from Sync API
  videoUrl?: string; // Generated lip-sync video URL
  visemeDataUrl?: string; // URL to viseme/phoneme timing data JSON
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export interface ImageAvatarMappingCreationAttributes
  extends Optional<
    ImageAvatarMappingAttributes,
    "id" | "createdAt" | "updatedAt"
  > {}

export class ImageAvatarMapping
  extends Model<
    ImageAvatarMappingAttributes,
    ImageAvatarMappingCreationAttributes
  >
  implements ImageAvatarMappingAttributes
{
  public id!: string;
  public imageId!: string;
  public avatarId!: string;
  public script?: string;
  public audioUrl?: string;
  public videoUrl?: string;
  public visemeDataUrl?: string;
  public isActive!: boolean;
  public readonly createdAt!: Date;
  public readonly updatedAt!: Date;
}

// Initialize ImageAvatarMapping model
ImageAvatarMapping.init(
  {
    id: {
      type: DataTypes.UUID,
      defaultValue: DataTypes.UUIDV4,
      primaryKey: true,
    },
    imageId: {
      type: DataTypes.UUID,
      allowNull: false,
      references: {
        model: "images",
        key: "id",
      },
    },
    avatarId: {
      type: DataTypes.UUID,
      allowNull: false,
      references: {
        model: "avatars",
        key: "id",
      },
    },
    script: {
      type: DataTypes.TEXT,
      allowNull: true,
      comment: "Custom script text for this image-avatar combination",
    },
    audioUrl: {
      type: DataTypes.STRING,
      allowNull: true,
      comment: "Generated audio URL from Sync API",
    },
    videoUrl: {
      type: DataTypes.STRING,
      allowNull: true,
      comment: "Generated lip-sync video URL",
    },
    visemeDataUrl: {
      type: DataTypes.STRING,
      allowNull: true,
      comment: "URL to viseme/phoneme timing data (JSON)",
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
    tableName: "image_avatar_mappings",
    timestamps: true,
  }
);
