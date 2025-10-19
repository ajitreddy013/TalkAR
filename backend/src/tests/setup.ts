import { Sequelize, DataTypes } from "sequelize";
import { Image, Dialogue } from "../models/Image";

// Test database configuration
const testDb = new Sequelize({
  dialect: "sqlite",
  storage: ":memory:",
  logging: false,
});

// Initialize models for testing
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
    sequelize: testDb,
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
    orderIndex: {
      type: DataTypes.INTEGER,
      allowNull: false,
      defaultValue: 0,
    },
    chunkSize: {
      type: DataTypes.INTEGER,
      allowNull: false,
      defaultValue: 1,
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
    sequelize: testDb,
    tableName: "dialogues",
    timestamps: true,
  }
);

// Define associations
Image.hasMany(Dialogue, { foreignKey: "imageId", as: "dialogues" });
Dialogue.belongsTo(Image, { foreignKey: "imageId", as: "image" });

export { testDb, Image, Dialogue };
