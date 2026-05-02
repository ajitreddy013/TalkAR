import { Image, Dialogue } from "./Image";
import { Avatar } from "./Avatar";
import { ImageAvatarMapping } from "./ImageAvatarMapping";
import { AIConfig } from "./AIConfig";
import Feedback from "./Feedback";
import Settings from "./Settings";
import Interaction from "./Interaction";
import Metric from "./Metric";
import { TalkingPhotoArtifact } from "./TalkingPhotoArtifact";
import { PosterPreprocessResult } from "./PosterPreprocessResult";

// Define all model associations
export function defineAssociations() {
  // AIConfig model doesn't need associations
  // Image associations are already defined in Image.ts

  // Avatar associations
  Avatar.hasMany(ImageAvatarMapping, {
    foreignKey: "avatarId",
    as: "imageMappings",
  });
  ImageAvatarMapping.belongsTo(Avatar, {
    foreignKey: "avatarId",
    as: "associatedAvatar",
  });

  // Image-Avatar mapping associations
  Image.hasMany(ImageAvatarMapping, {
    foreignKey: "imageId",
    as: "avatarMappings",
    onDelete: "CASCADE",
  });
  ImageAvatarMapping.belongsTo(Image, { foreignKey: "imageId", as: "associatedImage" });

  // Talking photo artifact (one active artifact per image)
  Image.hasOne(TalkingPhotoArtifact, {
    foreignKey: "imageId",
    as: "talkingPhotoArtifact",
    onDelete: "CASCADE",
  });
  TalkingPhotoArtifact.belongsTo(Image, {
    foreignKey: "imageId",
    as: "image",
  });

  Image.hasOne(PosterPreprocessResult, {
    foreignKey: "imageId",
    as: "preprocessResult",
    onDelete: "CASCADE",
  });
  PosterPreprocessResult.belongsTo(Image, {
    foreignKey: "imageId",
    as: "image",
  });

  // Avatar-Dialogue associations (for voice mapping)
  // Note: This is a loose association based on voiceId string matching
  // Avatar.hasMany(Dialogue, { foreignKey: "voiceId", as: "dialogues" });
  // Dialogue.belongsTo(Avatar, { foreignKey: "voiceId", as: "avatar" });
  
  // Feedback and Settings models don't need associations
}
