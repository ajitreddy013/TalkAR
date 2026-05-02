import { DataTypes, Model, Optional } from "sequelize";
import { sequelize } from "../config/database";

export type PreprocessStatus = "pending" | "ready" | "failed";

export interface PosterPreprocessResultAttributes {
  id: string;
  imageId: string;
  status: PreprocessStatus;
  provider: string;
  faceDetected: boolean;
  confidence: number | null;
  eligibleForTalkingPhoto: boolean;
  faceBox: string | null;
  lipRoi: string | null;
  errorCode: string | null;
  errorMessage: string | null;
  processedAt: Date | null;
  createdAt: Date;
  updatedAt: Date;
}

export interface PosterPreprocessResultCreationAttributes
  extends Optional<
    PosterPreprocessResultAttributes,
    | "id"
    | "status"
    | "provider"
    | "faceDetected"
    | "confidence"
    | "eligibleForTalkingPhoto"
    | "faceBox"
    | "lipRoi"
    | "errorCode"
    | "errorMessage"
    | "processedAt"
    | "createdAt"
    | "updatedAt"
  > {}

export class PosterPreprocessResult
  extends Model<
    PosterPreprocessResultAttributes,
    PosterPreprocessResultCreationAttributes
  >
  implements PosterPreprocessResultAttributes
{
  public id!: string;
  public imageId!: string;
  public status!: PreprocessStatus;
  public provider!: string;
  public faceDetected!: boolean;
  public confidence!: number | null;
  public eligibleForTalkingPhoto!: boolean;
  public faceBox!: string | null;
  public lipRoi!: string | null;
  public errorCode!: string | null;
  public errorMessage!: string | null;
  public processedAt!: Date | null;
  public readonly createdAt!: Date;
  public readonly updatedAt!: Date;
}

PosterPreprocessResult.init(
  {
    id: {
      type: DataTypes.UUID,
      defaultValue: DataTypes.UUIDV4,
      primaryKey: true,
    },
    imageId: {
      type: DataTypes.UUID,
      allowNull: false,
      unique: true,
      references: {
        model: "images",
        key: "id",
      },
    },
    status: {
      type: DataTypes.STRING(20),
      allowNull: false,
      defaultValue: "pending",
    },
    provider: {
      type: DataTypes.STRING(64),
      allowNull: false,
      defaultValue: "none",
    },
    faceDetected: {
      type: DataTypes.BOOLEAN,
      allowNull: false,
      defaultValue: false,
    },
    confidence: {
      type: DataTypes.FLOAT,
      allowNull: true,
      defaultValue: null,
    },
    eligibleForTalkingPhoto: {
      type: DataTypes.BOOLEAN,
      allowNull: false,
      defaultValue: false,
    },
    faceBox: {
      type: DataTypes.TEXT,
      allowNull: true,
      defaultValue: null,
    },
    lipRoi: {
      type: DataTypes.TEXT,
      allowNull: true,
      defaultValue: null,
    },
    errorCode: {
      type: DataTypes.STRING(64),
      allowNull: true,
      defaultValue: null,
    },
    errorMessage: {
      type: DataTypes.TEXT,
      allowNull: true,
      defaultValue: null,
    },
    processedAt: {
      type: DataTypes.DATE,
      allowNull: true,
      defaultValue: null,
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
    tableName: "poster_preprocess_results",
    timestamps: true,
  }
);
