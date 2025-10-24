import { DataTypes, Model, Optional } from "sequelize";
import { sequelize } from "../config/database";

export interface AvatarAttributes {
  id: string;
  name: string;
  description?: string;
  avatarImageUrl: string;
  avatarVideoUrl?: string;
  voiceId?: string;
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
  public voiceId?: string;
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
    voiceId: {
      type: DataTypes.STRING,
      allowNull: true,
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
