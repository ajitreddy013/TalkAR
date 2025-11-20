import { DataTypes, Model, Optional } from 'sequelize';
import { sequelize } from '../config/database';

interface InteractionAttributes {
  id: number;
  user_id?: string;
  poster_id: string;
  script?: string;
  audio_url?: string;
  video_url?: string;
  feedback?: string;
  job_id?: string;
  status?: string;
  latency_ms?: number;
  created_at?: Date;
  updated_at?: Date;
}

interface InteractionCreationAttributes extends Optional<InteractionAttributes, 'id' | 'created_at' | 'updated_at'> {}

class Interaction extends Model<InteractionAttributes, InteractionCreationAttributes> implements InteractionAttributes {
  public id!: number;
  public user_id?: string;
  public poster_id!: string;
  public script?: string;
  public audio_url?: string;
  public video_url?: string;
  public feedback?: string;
  public job_id?: string;
  public status?: string;
  public latency_ms?: number;
  public readonly created_at!: Date;
  public readonly updated_at!: Date;
}

Interaction.init(
  {
    id: {
      type: DataTypes.INTEGER,
      autoIncrement: true,
      primaryKey: true,
    },
    user_id: {
      type: DataTypes.STRING,
      allowNull: true,
    },
    poster_id: {
      type: DataTypes.STRING,
      allowNull: false,
    },
    script: {
      type: DataTypes.TEXT,
      allowNull: true,
    },
    audio_url: {
      type: DataTypes.STRING,
      allowNull: true,
    },
    video_url: {
      type: DataTypes.STRING,
      allowNull: true,
    },
    feedback: {
      type: DataTypes.STRING, // 'like', 'dislike', NULL
      allowNull: true,
    },
    job_id: {
      type: DataTypes.STRING,
      allowNull: true,
    },
    status: {
      type: DataTypes.STRING, // 'started', 'audio_streaming', 'video_pending', 'completed', 'error'
      allowNull: true,
    },
    latency_ms: {
      type: DataTypes.INTEGER,
      allowNull: true,
    },
  },
  {
    sequelize,
    tableName: 'interactions',
    timestamps: true,
    createdAt: 'created_at',
    updatedAt: 'updated_at',
  }
);

export default Interaction;
