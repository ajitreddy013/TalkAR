import { DataTypes, Model, Optional } from "sequelize";
import { sequelize } from "../config/database";

// Define the attributes for the BetaFeedback model
interface BetaFeedbackAttributes {
  id: string;
  userId: string;
  posterId: string;
  rating: number;
  comment: string;
  timestamp: number;
}

// Define the creation attributes (id is optional as it will be auto-generated)
interface BetaFeedbackCreationAttributes extends Optional<BetaFeedbackAttributes, "id"> {}

// Define the BetaFeedback model class
class BetaFeedback extends Model<BetaFeedbackAttributes, BetaFeedbackCreationAttributes> 
  implements BetaFeedbackAttributes {
  public id!: string;
  public userId!: string;
  public posterId!: string;
  public rating!: number;
  public comment!: string;
  public timestamp!: number;

  // Timestamps
  public readonly createdAt!: Date;
  public readonly updatedAt!: Date;
}

// Initialize the BetaFeedback model
BetaFeedback.init(
  {
    id: {
      type: DataTypes.UUID,
      defaultValue: DataTypes.UUIDV4,
      primaryKey: true,
    },
    userId: {
      type: DataTypes.STRING,
      allowNull: true, // Can be anonymous
    },
    posterId: {
      type: DataTypes.STRING,
      allowNull: false,
    },
    rating: {
      type: DataTypes.INTEGER,
      allowNull: false,
      validate: {
        min: 1,
        max: 5
      }
    },
    comment: {
      type: DataTypes.TEXT,
      allowNull: true,
    },
    timestamp: {
      type: DataTypes.BIGINT,
      allowNull: false,
    },
  },
  {
    sequelize,
    modelName: "BetaFeedback",
    tableName: "beta_feedbacks",
    timestamps: true,
  }
);

export default BetaFeedback;
export { BetaFeedback };
