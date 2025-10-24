import { DataTypes, Model, Optional } from "sequelize";
import { sequelize } from "../config/database";
import { Avatar } from "./Avatar";

export interface ImageAvatarMappingAttributes {
  id: string;
  imageId: string;
  avatarId: string;
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

// Define associations after model initialization
ImageAvatarMapping.belongsTo(Avatar, { foreignKey: "avatarId", as: "avatar" });
Avatar.hasMany(ImageAvatarMapping, { foreignKey: "avatarId", as: "mappings" });
