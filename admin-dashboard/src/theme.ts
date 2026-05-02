import { createTheme } from "@mui/material/styles";

const theme = createTheme({
  palette: {
    mode: "light",
    primary: { main: "#1f4db8" },
    secondary: { main: "#0f766e" },
    background: { default: "#f4f6fb", paper: "#ffffff" },
    success: { main: "#2e7d32" },
    warning: { main: "#ed6c02" },
    error: { main: "#d32f2f" },
    text: { primary: "#111827", secondary: "#4b5563" },
    divider: "#e5e7eb",
  },
  shape: { borderRadius: 10 },
  typography: {
    fontFamily: "\"Plus Jakarta Sans\", \"Segoe UI\", \"Helvetica Neue\", Arial, sans-serif",
    h4: { fontWeight: 700, fontSize: "1.6rem" },
    h5: { fontWeight: 700, fontSize: "1.3rem" },
    h6: { fontWeight: 700, fontSize: "1.05rem" },
    body2: { color: "#4b5563" },
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: { boxShadow: "0 1px 2px rgba(0,0,0,0.06)", border: "1px solid #e5e7eb" },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: { border: "1px solid #e5e7eb" },
      },
    },
  },
});

export default theme;
