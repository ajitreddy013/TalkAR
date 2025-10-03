import axios from "axios";

const API_BASE_URL = process.env.REACT_APP_API_URL || "http://localhost:3001";

export interface MultiImageUpload {
  objectName: string;
  description: string;
  images: {
    type:
      | "front"
      | "left_angle"
      | "right_angle"
      | "bright"
      | "dim"
      | "close"
      | "far";
    file: File;
    description: string;
    required: boolean;
  }[];
}

export interface ImageSet {
  id: string;
  objectName: string;
  description: string;
  imageCount: number;
  createdAt: string;
  images: {
    id: string;
    name: string;
    imageType: string;
    imageUrl: string;
  }[];
}

export const MultiImageService = {
  // Upload multiple images for one object
  uploadMultiImages: async (imageSet: MultiImageUpload) => {
    const formData = new FormData();

    // Add basic info
    formData.append("objectName", imageSet.objectName);
    formData.append("description", imageSet.description);

    // Add image types array
    const imageTypes = imageSet.images.map((img) => ({
      type: img.type,
      description: img.description,
      required: img.required,
    }));
    formData.append("imageTypes", JSON.stringify(imageTypes));

    // Add all image files
    imageSet.images.forEach((img) => {
      formData.append("images", img.file);
    });

    const response = await axios.post(
      `${API_BASE_URL}/api/multi-images`,
      formData,
      {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      }
    );

    return response.data;
  },

  // Get all image sets
  getAllImageSets: async () => {
    const response = await axios.get(`${API_BASE_URL}/api/multi-images`);
    return response.data;
  },

  // Get specific image set
  getImageSet: async (id: string) => {
    const response = await axios.get(`${API_BASE_URL}/api/multi-images/${id}`);
    return response.data;
  },

  // Get all images for an object
  getObjectImages: async (id: string) => {
    const response = await axios.get(
      `${API_BASE_URL}/api/multi-images/${id}/images`
    );
    return response.data;
  },

  // Delete image set
  deleteImageSet: async (id: string) => {
    const response = await axios.delete(
      `${API_BASE_URL}/api/multi-images/${id}`
    );
    return response.data;
  },

  // Download images for mobile app
  downloadObjectImages: async (objectName: string) => {
    const response = await axios.get(
      `${API_BASE_URL}/api/multi-images/download/${objectName}`
    );
    return response.data;
  },
};
