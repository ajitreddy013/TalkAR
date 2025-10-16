import { Request, Response } from "express";
import * as imageService from "../services/imageService";

export const getAllImages = async (req: Request, res: Response) => {
  try {
    const images = await imageService.getAllImages();
    res.status(200).json(images);
  } catch (error) {
    res.status(500).json({ message: "Failed to get images" });
  }
};