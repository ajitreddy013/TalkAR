import React, { useEffect } from "react";
import {
  Box,
  Typography,
  Grid,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
} from "@mui/material";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from "recharts";
import { useDispatch, useSelector } from "react-redux";
import { RootState } from "../store/store";
import { fetchImages } from "../store/slices/imageSlice";

const Analytics: React.FC = () => {
  const dispatch = useDispatch();
  const { images } = useSelector((state: RootState) => state.images);

  useEffect(() => {
    dispatch(fetchImages());
  }, [dispatch]);

  // Calculate analytics data
  const totalImages = images.length;
  const activeImages = images.filter((img) => img.isActive).length;
  const totalDialogues = images.reduce(
    (acc, img) => acc + img.dialogues.length,
    0
  );

  // Language distribution
  const languageStats = images
    .flatMap((img) => img.dialogues)
    .reduce((acc, dialogue) => {
      acc[dialogue.language] = (acc[dialogue.language] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);

  const languageData = Object.entries(languageStats).map(
    ([language, count]) => ({
      language: language.toUpperCase(),
      count,
    })
  );

  const COLORS = ["#0088FE", "#00C49F", "#FFBB28", "#FF8042", "#8884D8"];

  // Recent activity
  const recentImages = images
    .sort(
      (a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
    .slice(0, 5);

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Analytics
      </Typography>

      <Grid container spacing={3}>
        {/* Overview Cards */}
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Total Images
              </Typography>
              <Typography variant="h4">{totalImages}</Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Active Images
              </Typography>
              <Typography variant="h4">{activeImages}</Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Total Dialogues
              </Typography>
              <Typography variant="h4">{totalDialogues}</Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Languages
              </Typography>
              <Typography variant="h4">
                {Object.keys(languageStats).length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Language Distribution Chart */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Language Distribution
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={languageData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="language" />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="count" fill="#8884d8" />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>

        {/* Language Pie Chart */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Language Breakdown
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={languageData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ language, count }) => `${language}: ${count}`}
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="count"
                  >
                    {languageData.map((entry, index) => (
                      <Cell
                        key={`cell-${index}`}
                        fill={COLORS[index % COLORS.length]}
                      />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>

        {/* Recent Images Table */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Recent Images
              </Typography>
              <TableContainer component={Paper}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Name</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Dialogues</TableCell>
                      <TableCell>Languages</TableCell>
                      <TableCell>Created</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {recentImages.map((image) => (
                      <TableRow key={image.id}>
                        <TableCell>{image.name}</TableCell>
                        <TableCell>
                          <Chip
                            label={image.isActive ? "Active" : "Inactive"}
                            color={image.isActive ? "success" : "default"}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>{image.dialogues.length}</TableCell>
                        <TableCell>
                          {Array.from(
                            new Set(image.dialogues.map((d) => d.language))
                          ).join(", ")}
                        </TableCell>
                        <TableCell>
                          {new Date(image.createdAt).toLocaleDateString()}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Analytics;
