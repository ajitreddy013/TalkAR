import React from "react";
import { Provider } from "react-redux";
import { configureStore } from "@reduxjs/toolkit";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import App from "../App";
import imagesReducer from "../store/slices/imageSlice";
import dialoguesReducer from "../store/slices/dialogueSlice";
import authReducer from "../store/slices/authSlice";

// Mock services used by thunks so no network is performed
jest.mock("../services/imageService", () => ({
  ImageService: {
    getAllImages: jest.fn().mockResolvedValue({ data: [] }),
    createImage: jest.fn().mockResolvedValue({
      data: {
        id: "1",
        name: "Test",
        imageUrl: "/uploads/x.jpg",
        isActive: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        dialogues: [],
      },
    }),
    updateImage: jest.fn().mockResolvedValue({
      data: {
        id: "1",
        name: "Updated",
        imageUrl: "/uploads/x.jpg",
        isActive: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        dialogues: [],
      },
    }),
    deleteImage: jest.fn().mockResolvedValue({}),
  },
}));

jest.mock("../services/multiImageService", () => ({
  MultiImageService: {
    getAllImageSets: jest.fn().mockResolvedValue({ imageSets: [] }),
  },
}));

function renderWithStore(ui: React.ReactElement) {
  const store = configureStore({
    reducer: {
      images: imagesReducer,
      dialogues: dialoguesReducer,
      auth: authReducer,
    },
    preloadedState: {
      images: { images: [], loading: false, error: null, selectedImage: null },
      dialogues: {
        dialogues: [],
        loading: false,
        error: null,
        selectedDialogue: null,
      },
      auth: {
        user: null,
        token: null,
        loading: false,
        error: null,
        isAuthenticated: false,
      },
    },
  });
  return render(<Provider store={store}>{ui}</Provider>);
}

describe("Admin Dashboard - TestSprite Frontend", () => {
  it("navigates from Dashboard to Images via button", async () => {
    renderWithStore(<App />);

    // Header
    expect(screen.getByText(/TalkAR Admin Dashboard/i)).toBeInTheDocument();

    // Click "Manage Images" to go to Images page
    const manageBtn = await screen.findByRole("button", {
      name: /Manage Images/i,
    });
    await userEvent.click(manageBtn);

    // Expect Images actions to be visible
    expect(
      await screen.findByRole("button", { name: /Upload Single Image/i })
    ).toBeInTheDocument();
  });

  it("opens and closes the Upload Single Image dialog", async () => {
    renderWithStore(<App />);

    // Go to Images page
    const manageBtn = await screen.findByRole("button", {
      name: /Manage Images/i,
    });
    await userEvent.click(manageBtn);

    // Open dialog
    const uploadBtn = await screen.findByRole("button", {
      name: /Upload Single Image/i,
    });
    await userEvent.click(uploadBtn);

    // Dialog visible
    expect(await screen.findByText(/Upload New Image/i)).toBeInTheDocument();

    // Close dialog
    const cancel = screen.getByRole("button", { name: /Cancel/i });
    await userEvent.click(cancel);

    // Ensure dialog closed (title not present)
    expect(screen.queryByText(/Upload New Image/i)).toBeNull();
  });

  it("navigates to Images via sidebar", async () => {
    renderWithStore(<App />);

    const imagesNav = screen.getByRole("button", { name: /Images/i });
    await userEvent.click(imagesNav);

    expect(
      await screen.findByRole("button", { name: /Upload Single Image/i })
    ).toBeInTheDocument();
  });
});
