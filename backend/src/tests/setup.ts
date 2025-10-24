import { Sequelize } from "sequelize";
import { Image, Dialogue } from "../models/Image";

// Ensure setImmediate resolves under Jest fake timers by mapping to nextTick
// This helps tests that await setImmediate to flush async queues reliably
(global as any).setImmediate = (cb: (...args: any[]) => void, ...args: any[]) =>
  process.nextTick(cb, ...args);

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
    createdAt: {
      type: "DATE",
      allowNull: false,
    },
    updatedAt: {
      type: "DATE",
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
    isActive: {
      type: "BOOLEAN",
      defaultValue: true,
    },
    isDefault: {
      type: "BOOLEAN",
      defaultValue: false,
    },
    createdAt: {
      type: "DATE",
      allowNull: false,
    },
    updatedAt: {
      type: "DATE",
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

// Global test setup
beforeAll(async () => {
  await testDb.sync({ force: true });
});

// Reset database before each test
beforeEach(async () => {
  // Only clear dialogues between tests, not images
  // Images may be needed across tests and are cleared in beforeAll if needed
  await Dialogue.destroy({ where: {}, truncate: true, cascade: true });
});

// Clean up after all tests
afterAll(async () => {
  await testDb.close();
});

export { testDb, Image, Dialogue };
