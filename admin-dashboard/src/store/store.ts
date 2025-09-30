import { configureStore } from "@reduxjs/toolkit";
import imageReducer from "./slices/imageSlice";
import dialogueReducer from "./slices/dialogueSlice";
import authReducer from "./slices/authSlice";

export const store = configureStore({
  reducer: {
    images: imageReducer,
    dialogues: dialogueReducer,
    auth: authReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
