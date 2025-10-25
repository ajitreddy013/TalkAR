import { DataTypes, Model, Optional } from "sequelize";
import { sequelize } from "../config/database";

export interface AIConfigAttributes {
  id: string;
  key: string;
  value: string;
  description?: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface AIConfigCreationAttributes
  extends Optional<AIConfigAttributes, "id" | "createdAt" | "updatedAt"> {}

export class AIConfig
  extends Model<AIConfigAttributes, AIConfigCreationAttributes>
  implements AIConfigAttributes
{
  public id!: string;
  public key!: string;
  public value!: string;
  public description?: string;
  public readonly createdAt!: Date;
  public readonly updatedAt!: Date;
}

// Initialize AIConfig model
AIConfig.init(
  {
    id: {
      type: DataTypes.UUID,
      defaultValue: DataTypes.UUIDV4,
      primaryKey: true,
    },
    key: {
      type: DataTypes.STRING,
      allowNull: false,
      unique: true,
    },
    value: {
      type: DataTypes.TEXT,
      allowNull: false,
    },
    description: {
      type: DataTypes.TEXT,
      allowNull: true,
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
    tableName: "ai_configs",
    timestamps: true,
  }
);