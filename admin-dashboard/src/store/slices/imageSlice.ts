import { createSlice, createAsyncThunk, PayloadAction } from "@reduxjs/toolkit";
import { ImageService } from "../services/imageService";

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

interface ImageState {
  images: Image[];
  loading: boolean;
  error: string | null;
  selectedImage: Image | null;
}

const initialState: ImageState = {
  images: [],
  loading: false,
  error: null,
  selectedImage: null,
};

export const fetchImages = createAsyncThunk(
  "images/fetchImages",
  async (_, { rejectWithValue }) => {
    try {
      const response = await ImageService.getAllImages();
      return response.data;
    } catch (error: any) {
      return rejectWithValue(
        error.response?.data?.error || "Failed to fetch images"
      );
    }
  }
);

export const createImage = createAsyncThunk(
  "images/createImage",
  async (imageData: FormData, { rejectWithValue }) => {
    try {
      const response = await ImageService.createImage(imageData);
      return response.data;
    } catch (error: any) {
      return rejectWithValue(
        error.response?.data?.error || "Failed to create image"
      );
    }
  }
);

export const updateImage = createAsyncThunk(
  "images/updateImage",
  async (
    { id, data }: { id: string; data: Partial<Image> },
    { rejectWithValue }
  ) => {
    try {
      const response = await ImageService.updateImage(id, data);
      return response.data;
    } catch (error: any) {
      return rejectWithValue(
        error.response?.data?.error || "Failed to update image"
      );
    }
  }
);

export const deleteImage = createAsyncThunk(
  "images/deleteImage",
  async (id: string, { rejectWithValue }) => {
    try {
      await ImageService.deleteImage(id);
      return id;
    } catch (error: any) {
      return rejectWithValue(
        error.response?.data?.error || "Failed to delete image"
      );
    }
  }
);

const imageSlice = createSlice({
  name: "images",
  initialState,
  reducers: {
    setSelectedImage: (state, action: PayloadAction<Image | null>) => {
      state.selectedImage = action.payload;
    },
    clearError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchImages.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchImages.fulfilled, (state, action) => {
        state.loading = false;
        state.images = action.payload;
      })
      .addCase(fetchImages.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      .addCase(createImage.fulfilled, (state, action) => {
        state.images.unshift(action.payload);
      })
      .addCase(updateImage.fulfilled, (state, action) => {
        const index = state.images.findIndex(
          (img) => img.id === action.payload.id
        );
        if (index !== -1) {
          state.images[index] = action.payload;
        }
      })
      .addCase(deleteImage.fulfilled, (state, action) => {
        state.images = state.images.filter((img) => img.id !== action.payload);
      });
  },
});

export const { setSelectedImage, clearError } = imageSlice.actions;
export default imageSlice.reducer;
