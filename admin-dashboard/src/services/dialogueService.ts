import { api } from "./api";

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

export const DialogueService = {
  getDialoguesByImageId: (imageId: string) =>
    api.get(`/images/${imageId}/dialogues`),

  createDialogue: (
    dialogueData: Omit<Dialogue, "id" | "createdAt" | "updatedAt">
  ) => api.post(`/images/${dialogueData.imageId}/dialogues`, dialogueData),

  updateDialogue: (id: string, data: Partial<Dialogue>) =>
    api.put(`/dialogues/${id}`, data),

  deleteDialogue: (id: string) => api.delete(`/dialogues/${id}`),
};
