import { DataTypes, Model, Optional } from "sequelize";
import { sequelize } from "../config/database";

// Define the attributes for the Feedback model
interface FeedbackAttributes {
  id: string;
  adContentId: string;
  productName: string;
  isPositive: boolean;
  timestamp: number;
}

// Define the creation attributes (id is optional as it will be auto-generated)
interface FeedbackCreationAttributes extends Optional<FeedbackAttributes, "id"> {}

// Define the Feedback model class
class Feedback extends Model<FeedbackAttributes, FeedbackCreationAttributes> 
  implements FeedbackAttributes {
  public id!: string;
  public adContentId!: string;
  public productName!: string;
  public isPositive!: boolean;
  public timestamp!: number;

  // Timestamps
  public readonly createdAt!: Date;
  public readonly updatedAt!: Date;
}

// Initialize the Feedback model
Feedback.init(
  {
    id: {
      type: DataTypes.STRING,
      primaryKey: true,
    },
    adContentId: {
      type: DataTypes.STRING,
      allowNull: false,
    },
    productName: {
      type: DataTypes.STRING,
      allowNull: false,
    },
    isPositive: {
      type: DataTypes.BOOLEAN,
      allowNull: false,
    },
    timestamp: {
      type: DataTypes.BIGINT,
      allowNull: false,
    },
  },
  {
    sequelize,
    modelName: "Feedback",
    tableName: "feedbacks",
    timestamps: true,
  }
);

export default Feedback;
export { Feedback };