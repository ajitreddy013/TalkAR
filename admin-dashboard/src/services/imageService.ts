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
  getAllImages: () => api.get("/images"),

  getImageById: (id: string) => api.get(`/images/${id}`),

  // Let the browser set the correct multipart boundary automatically
  createImage: (imageData: FormData) => api.post("/images", imageData),

  updateImage: (id: string, data: Partial<Image>) =>
    api.put(`/images/${id}`, data),

  deleteImage: (id: string) => api.delete(`/images/${id}`),

  addDialogue: (
    imageId: string,
    dialogueData: Omit<Dialogue, "id" | "imageId" | "createdAt" | "updatedAt">
  ) => api.post(`/images/${imageId}/dialogues`, dialogueData),

  updateDialogue: (
    imageId: string,
    dialogueId: string,
    data: Partial<Dialogue>
  ) => api.put(`/images/${imageId}/dialogues/${dialogueId}`, data),

  deleteDialogue: (imageId: string, dialogueId: string) =>
    api.delete(`/images/${imageId}/dialogues/${dialogueId}`),
};
