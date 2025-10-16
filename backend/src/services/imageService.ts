import { Image } from "../models/Image";

export const getAllImages = async () => {
  try {
    const images = await Image.findAll();
    return images;
  } catch (error) {
    throw new Error("Failed to get images");
  }
};