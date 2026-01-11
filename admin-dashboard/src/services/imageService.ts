import { api } from "./api";

export interface Image {
  id: string;
  name: string;
  description?: string;
  imageUrl: string;
  thumbnailUrl?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  dialogues: Dialogue[];
}

export interface Dialogue {
  id: string;
  imageId: string;
  text: string;
  language: string;
  voiceId?: string;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export const ImageService = {
  getAllImages: () => api.get("/api/v1/images"),

  getImageById: (id: string) => api.get(`/api/v1/images/${id}`),

  // Create new image with multi-file support and script
  createImage: (imageData: FormData) => api.post("/api/v1/images", imageData),

  // Update image with multi-file support and metadata
  updateImage: (id: string, data: FormData | Partial<Image>) =>
    api.put(`/api/v1/images/${id}`, data),

  deleteImage: (id: string) => api.delete(`/api/v1/images/${id}`),

  addDialogue: (
    imageId: string,
    dialogueData: Omit<Dialogue, "id" | "imageId" | "createdAt" | "updatedAt">
  ) => api.post(`/api/v1/images/${imageId}/dialogues`, dialogueData),

  updateDialogue: (
    imageId: string,
    dialogueId: string,
    data: Partial<Dialogue>
  ) => api.put(`/api/v1/images/${imageId}/dialogues/${dialogueId}`, data),

  deleteDialogue: (imageId: string, dialogueId: string) =>
    api.delete(`/api/v1/images/${imageId}/dialogues/${dialogueId}`),
};
