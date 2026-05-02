import { DataTypes, Model, Optional } from "sequelize";
import { sequelize } from "../config/database";

export type ArtifactStatus = "queued" | "processing" | "ready" | "failed";

export interface TalkingPhotoArtifactAttributes {
  id: string;
  imageId: string;
  dialogueId?: string | null;
  videoUrl?: string | null;
  lipLandmarks?: string | null;
  posterFaceBox?: string | null;
  status: ArtifactStatus;
  errorCode?: string | null;
  errorMessage?: string | null;
  confidence?: number | null;
  provider?: string | null;
  stageTimings?: string | null;
  lastProcessingDurationMs?: number | null;
  lastCorrelationId?: string | null;
  version: number;
  createdAt: Date;
  updatedAt: Date;
}

export interface TalkingPhotoArtifactCreationAttributes
  extends Optional<
    TalkingPhotoArtifactAttributes,
    | "id"
    | "dialogueId"
    | "videoUrl"
    | "lipLandmarks"
    | "posterFaceBox"
    | "errorCode"
    | "errorMessage"
    | "confidence"
    | "provider"
    | "stageTimings"
    | "lastProcessingDurationMs"
    | "lastCorrelationId"
    | "version"
    | "createdAt"
    | "updatedAt"
  > {}

export class TalkingPhotoArtifact
  extends Model<
    TalkingPhotoArtifactAttributes,
    TalkingPhotoArtifactCreationAttributes
  >
  implements TalkingPhotoArtifactAttributes
{
  public id!: string;
  public imageId!: string;
  public dialogueId?: string | null;
  public videoUrl?: string | null;
  public lipLandmarks?: string | null;
  public posterFaceBox?: string | null;
  public status!: ArtifactStatus;
  public errorCode?: string | null;
  public errorMessage?: string | null;
  public confidence?: number | null;
  public provider?: string | null;
  public stageTimings?: string | null;
  public lastProcessingDurationMs?: number | null;
  public lastCorrelationId?: string | null;
  public version!: number;
  public readonly createdAt!: Date;
  public readonly updatedAt!: Date;
}

TalkingPhotoArtifact.init(
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
    dialogueId: {
      type: DataTypes.UUID,
      allowNull: true,
    },
    videoUrl: {
      type: DataTypes.TEXT,
      allowNull: true,
    },
    lipLandmarks: {
      type: DataTypes.TEXT,
      allowNull: true,
    },
    posterFaceBox: {
      type: DataTypes.TEXT,
      allowNull: true,
    },
    status: {
      type: DataTypes.STRING(20),
      allowNull: false,
      defaultValue: "queued",
    },
    errorCode: {
      type: DataTypes.STRING(64),
      allowNull: true,
    },
    errorMessage: {
      type: DataTypes.TEXT,
      allowNull: true,
    },
    confidence: {
      type: DataTypes.FLOAT,
      allowNull: true,
    },
    provider: {
      type: DataTypes.STRING(64),
      allowNull: true,
    },
    stageTimings: {
      type: DataTypes.TEXT,
      allowNull: true,
    },
    lastProcessingDurationMs: {
      type: DataTypes.INTEGER,
      allowNull: true,
    },
    lastCorrelationId: {
      type: DataTypes.STRING(128),
      allowNull: true,
    },
    version: {
      type: DataTypes.INTEGER,
      allowNull: false,
      defaultValue: 1,
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
    tableName: "talking_photo_artifacts",
    timestamps: true,
  }
);
