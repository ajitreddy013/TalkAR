import { DataTypes, Model, Optional } from 'sequelize';
import { sequelize } from '../config/database';

interface MetricAttributes {
  id: number;
  date: string; // YYYY-MM-DD
  scans: number;
  plays: number;
  avg_latency_ms: number;
  likes: number;
  dislikes: number;
}

interface MetricCreationAttributes extends Optional<MetricAttributes, 'id' | 'scans' | 'plays' | 'avg_latency_ms' | 'likes' | 'dislikes'> {}

class Metric extends Model<MetricAttributes, MetricCreationAttributes> implements MetricAttributes {
  public id!: number;
  public date!: string;
  public scans!: number;
  public plays!: number;
  public avg_latency_ms!: number;
  public likes!: number;
  public dislikes!: number;
}

Metric.init(
  {
    id: {
      type: DataTypes.INTEGER,
      autoIncrement: true,
      primaryKey: true,
    },
    date: {
      type: DataTypes.DATEONLY,
      allowNull: false,
    },
    scans: {
      type: DataTypes.INTEGER,
      defaultValue: 0,
    },
    plays: {
      type: DataTypes.INTEGER,
      defaultValue: 0,
    },
    avg_latency_ms: {
      type: DataTypes.FLOAT,
      defaultValue: 0,
    },
    likes: {
      type: DataTypes.INTEGER,
      defaultValue: 0,
    },
    dislikes: {
      type: DataTypes.INTEGER,
      defaultValue: 0,
    },
  },
  {
    sequelize,
    tableName: 'metrics',
    timestamps: false, // No created_at/updated_at needed for daily rollup
  }
);

export default Metric;
