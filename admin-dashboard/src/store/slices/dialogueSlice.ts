import { createSlice, createAsyncThunk, PayloadAction } from "@reduxjs/toolkit";
import { DialogueService } from "../../services/dialogueService";

export interface Dialogue {
  id: string;
  imageId: string;
  text: string;
  language: string;
  voiceId?: string;
  emotion?: string; // Add emotion field
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

interface DialogueState {
  dialogues: Dialogue[];
  loading: boolean;
  error: string | null;
  selectedDialogue: Dialogue | null;
}

const initialState: DialogueState = {
  dialogues: [],
  loading: false,
  error: null,
  selectedDialogue: null,
};

export const fetchDialogues = createAsyncThunk(
  "dialogues/fetchDialogues",
  async (imageId: string, { rejectWithValue }) => {
    try {
      const response = await DialogueService.getDialoguesByImageId(imageId);
      return response.data;
    } catch (error: any) {
      return rejectWithValue(
        error.response?.data?.error || "Failed to fetch dialogues"
      );
    }
  }
);

export const createDialogue = createAsyncThunk(
  "dialogues/createDialogue",
  async (
    dialogueData: Omit<Dialogue, "id" | "createdAt" | "updatedAt">,
    { rejectWithValue }
  ) => {
    try {
      const response = await DialogueService.createDialogue(dialogueData);
      return response.data;
    } catch (error: any) {
      return rejectWithValue(
        error.response?.data?.error || "Failed to create dialogue"
      );
    }
  }
);

export const updateDialogue = createAsyncThunk(
  "dialogues/updateDialogue",
  async (
    { id, data }: { id: string; data: Partial<Dialogue> },
    { rejectWithValue }
  ) => {
    try {
      const response = await DialogueService.updateDialogue(id, data);
      return response.data;
    } catch (error: any) {
      return rejectWithValue(
        error.response?.data?.error || "Failed to update dialogue"
      );
    }
  }
);

export const deleteDialogue = createAsyncThunk(
  "dialogues/deleteDialogue",
  async (id: string, { rejectWithValue }) => {
    try {
      await DialogueService.deleteDialogue(id);
      return id;
    } catch (error: any) {
      return rejectWithValue(
        error.response?.data?.error || "Failed to delete dialogue"
      );
    }
  }
);

const dialogueSlice = createSlice({
  name: "dialogues",
  initialState,
  reducers: {
    setSelectedDialogue: (state, action: PayloadAction<Dialogue | null>) => {
      state.selectedDialogue = action.payload;
    },
    clearError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchDialogues.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchDialogues.fulfilled, (state, action) => {
        state.loading = false;
        state.dialogues = action.payload;
      })
      .addCase(fetchDialogues.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      .addCase(createDialogue.fulfilled, (state, action) => {
        state.dialogues.push(action.payload);
      })
      .addCase(updateDialogue.fulfilled, (state, action) => {
        const index = state.dialogues.findIndex(
          (dialogue) => dialogue.id === action.payload.id
        );
        if (index !== -1) {
          state.dialogues[index] = action.payload;
        }
      })
      .addCase(deleteDialogue.fulfilled, (state, action) => {
        state.dialogues = state.dialogues.filter(
          (dialogue) => dialogue.id !== action.payload
        );
      });
  },
});

export const { setSelectedDialogue, clearError } = dialogueSlice.actions;
export default dialogueSlice.reducer;