import { Sequelize } from "sequelize";
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
      type: "UUID",
      defaultValue: "UUIDV4",
      primaryKey: true,
    },
    name: {
      type: "STRING",
      allowNull: false,
    },
    description: {
      type: "TEXT",
      allowNull: true,
    },
    imageUrl: {
      type: "STRING",
      allowNull: false,
    },
    thumbnailUrl: {
      type: "STRING",
      allowNull: true,
    },
    isActive: {
      type: "BOOLEAN",
      defaultValue: true,
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
      type: "UUID",
      defaultValue: "UUIDV4",
      primaryKey: true,
    },
    imageId: {
      type: "UUID",
      allowNull: false,
    },
    text: {
      type: "TEXT",
      allowNull: false,
    },
    language: {
      type: "STRING(5)",
      allowNull: false,
    },
    voiceId: {
      type: "STRING",
      allowNull: true,
    },
    isDefault: {
      type: "BOOLEAN",
      defaultValue: false,
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
