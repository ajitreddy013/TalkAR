import { api, API_BASE_URL } from "./api";

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

    const response = await api.post(`/api/multi-images`, formData);

    return response.data;
  },

  // Get all image sets
  getAllImageSets: async () => {
    const response = await api.get(`/api/multi-images`);
    return response.data;
  },

  // Get specific image set
  getImageSet: async (id: string) => {
    const response = await api.get(`/api/multi-images/${id}`);
    return response.data;
  },

  // Get all images for an object
  getObjectImages: async (id: string) => {
    const response = await api.get(`/api/multi-images/${id}/images`);
    return response.data;
  },

  // Delete image set
  deleteImageSet: async (id: string) => {
    const response = await api.delete(`/api/multi-images/${id}`);
    return response.data;
  },

  // Download images for mobile app
  downloadObjectImages: async (objectName: string) => {
    const response = await api.get(`/api/multi-images/download/${objectName}`);
    return response.data;
  },
};
