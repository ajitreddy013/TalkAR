import React, { useEffect } from "react";
import {
  Grid,
  Paper,
  Typography,
  Box,
  Card,
  CardContent,
  LinearProgress,
} from "@mui/material";
import {
  Image as ImageIcon,
  Chat,
  Visibility,
  TrendingUp,
} from "@mui/icons-material";
import { fetchImages } from "../store/slices/imageSlice";
import { useAppDispatch, useAppSelector } from "../store/hooks";

const Dashboard: React.FC = () => {
  const dispatch = useAppDispatch();
  const { images, loading } = useAppSelector((state) => state.images);

  useEffect(() => {
    dispatch(fetchImages());
  }, [dispatch]);

  const stats = [
    {
      title: "Total Images",
      value: images.length,
      icon: <ImageIcon />,
      color: "#1976d2",
    },
    {
      title: "Active Images",
      value: images.filter((img) => img.isActive).length,
      icon: <Visibility />,
      color: "#2e7d32",
    },
    {
      title: "Total Dialogues",
      value: images.reduce((acc, img) => acc + img.dialogues.length, 0),
      icon: <Chat />,
      color: "#ed6c02",
    },
    {
      title: "Languages",
      value: new Set(
        images.flatMap((img) => img.dialogues.map((d) => d.language))
      ).size,
      icon: <TrendingUp />,
      color: "#9c27b0",
    },
  ];

  if (loading) {
    return <LinearProgress />;
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Dashboard
      </Typography>

      <Grid container spacing={3}>
        {stats.map((stat, index) => (
          <Grid item xs={12} sm={6} md={3} key={index}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center" mb={2}>
                  <Box
                    sx={{
                      backgroundColor: stat.color,
                      color: "white",
                      borderRadius: "50%",
                      p: 1,
                      mr: 2,
                    }}
                  >
                    {stat.icon}
                  </Box>
                  <Typography variant="h6" component="div">
                    {stat.title}
                  </Typography>
                </Box>
                <Typography variant="h4" color="primary">
                  {stat.value}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}

        <Grid item xs={12}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Recent Images
            </Typography>
            {images.slice(0, 5).map((image) => (
              <Box
                key={image.id}
                sx={{
                  mb: 2,
                  p: 2,
                  border: "1px solid #e0e0e0",
                  borderRadius: 1,
                }}
              >
                <Typography variant="subtitle1">{image.name}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {image.dialogues.length} dialogues •{" "}
                  {image.isActive ? "Active" : "Inactive"}
                </Typography>
              </Box>
            ))}
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Dashboard;
