import { DataTypes, Model, Optional } from "sequelize";
import { sequelize } from "../config/database";

export interface ImageAttributes {
  id: string;
  name: string;
  description?: string;
  imageUrl: string;
  thumbnailUrl?: string;
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export interface ImageCreationAttributes
  extends Optional<ImageAttributes, "id" | "createdAt" | "updatedAt"> {}

export class Image
  extends Model<ImageAttributes, ImageCreationAttributes>
  implements ImageAttributes
{
  public id!: string;
  public name!: string;
  public description?: string;
  public imageUrl!: string;
  public thumbnailUrl?: string;
  public isActive!: boolean;
  public readonly createdAt!: Date;
  public readonly updatedAt!: Date;

  // Associations
  public getDialogues!: () => Promise<Dialogue[]>;
  public addDialogue!: (dialogue: Dialogue) => Promise<void>;
  public removeDialogue!: (dialogue: Dialogue) => Promise<void>;
}

export interface DialogueAttributes {
  id: string;
  imageId: string;
  text: string;
  language: string;
  voiceId?: string;
  isActive: boolean;
  isDefault: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export interface DialogueCreationAttributes
  extends Optional<DialogueAttributes, "id" | "createdAt" | "updatedAt"> {}

export class Dialogue
  extends Model<DialogueAttributes, DialogueCreationAttributes>
  implements DialogueAttributes
{
  public id!: string;
  public imageId!: string;
  public text!: string;
  public language!: string;
  public voiceId?: string;
  public isActive!: boolean;
  public isDefault!: boolean;
  public readonly createdAt!: Date;
  public readonly updatedAt!: Date;
}

// Initialize models
Image.init(
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
    imageUrl: {
      type: DataTypes.STRING,
      allowNull: false,
    },
    thumbnailUrl: {
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
    tableName: "images",
    timestamps: true,
  }
);

Dialogue.init(
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
    text: {
      type: DataTypes.TEXT,
      allowNull: false,
    },
    language: {
      type: DataTypes.STRING(5),
      allowNull: false,
    },
    voiceId: {
      type: DataTypes.STRING,
      allowNull: true,
    },
    isActive: {
      type: DataTypes.BOOLEAN,
      defaultValue: true,
    },
    isDefault: {
      type: DataTypes.BOOLEAN,
      defaultValue: false,
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
    tableName: "dialogues",
    timestamps: true,
  }
);

// Define associations
Image.hasMany(Dialogue, { foreignKey: "imageId", as: "dialogues" });
Dialogue.belongsTo(Image, { foreignKey: "imageId", as: "image" });
