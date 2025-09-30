import { DataTypes, Model, Optional } from "sequelize";
import { sequelize } from "../config/database";

export interface UserAttributes {
  id: string;
  email: string;
  password: string;
  role: "admin" | "user";
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export interface UserCreationAttributes
  extends Optional<UserAttributes, "id" | "createdAt" | "updatedAt"> {}

export class User
  extends Model<UserAttributes, UserCreationAttributes>
  implements UserAttributes
{
  public id!: string;
  public email!: string;
  public password!: string;
  public role!: "admin" | "user";
  public isActive!: boolean;
  public readonly createdAt!: Date;
  public readonly updatedAt!: Date;
}

// Initialize User model
User.init(
  {
    id: {
      type: DataTypes.UUID,
      defaultValue: DataTypes.UUIDV4,
      primaryKey: true,
    },
    email: {
      type: DataTypes.STRING,
      allowNull: false,
      unique: true,
    },
    password: {
      type: DataTypes.STRING,
      allowNull: false,
    },
    role: {
      type: DataTypes.ENUM("admin", "user"),
      allowNull: false,
      defaultValue: "user",
    },
    isActive: {
      type: DataTypes.BOOLEAN,
      allowNull: false,
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
    tableName: "users",
    timestamps: true,
  }
);
