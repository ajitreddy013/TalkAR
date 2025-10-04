import { createSlice, createAsyncThunk, PayloadAction } from "@reduxjs/toolkit";
import { ImageService } from "../../services/imageService";
import { MultiImageService } from "../../services/multiImageService";

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
  // Multi-image specific fields
  imageType?: string;
  objectName?: string;
  isMultiImage?: boolean;
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
      console.log("Fetching images...");

      // Fetch both single images and multi-image sets
      const [singleImagesResponse, multiImagesResponse] = await Promise.all([
        ImageService.getAllImages(),
        MultiImageService.getAllImageSets(),
      ]);

      const singleImages = singleImagesResponse.data || [];
      const multiImageSets = multiImagesResponse.imageSets || [];

      // Convert multi-image sets to individual image entries
      const multiImages: Image[] = [];
      multiImageSets.forEach((set: any) => {
        set.images.forEach((img: any) => {
          multiImages.push({
            id: img.id,
            name: img.name,
            description: `${set.objectName} - ${img.imageType}`,
            imageUrl: img.imageUrl,
            thumbnailUrl: img.imageUrl,
            isActive: true,
            createdAt: set.createdAt,
            updatedAt: set.createdAt,
            dialogues: [],
            imageType: img.imageType,
            objectName: set.objectName,
            isMultiImage: true,
          });
        });
      });

      const allImages = [...singleImages, ...multiImages];
      console.log(
        "Images fetched successfully:",
        allImages.length,
        "total images"
      );
      console.log("Single images:", singleImages.length);
      console.log("Multi images:", multiImages.length);
      console.log("All images:", allImages);
      return allImages;
    } catch (error: any) {
      console.error("Failed to fetch images:", error);
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
      .addCase(createImage.rejected, (state, action) => {
        state.error = action.payload as string;
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
