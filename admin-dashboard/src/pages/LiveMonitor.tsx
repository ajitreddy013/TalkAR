import React, { useEffect, useState } from 'react';
import { Box, Typography, Paper, List, ListItem, ListItemText, Chip } from '@mui/material';
import { io, Socket } from 'socket.io-client';
import { API_ORIGIN } from '../services/api';

interface InteractionEvent {
  id: string;
  type: string;
  poster_id?: string;
  status?: string;
  latency_ms?: number;
  timestamp: number;
  video_url?: string;
}

const LiveMonitor: React.FC = () => {
  const [events, setEvents] = useState<InteractionEvent[]>([]);
  const [socket, setSocket] = useState<Socket | null>(null);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    const newSocket = io(API_ORIGIN);
    setSocket(newSocket);

    newSocket.on('connect', () => {
      console.log('Connected to WebSocket');
      setConnected(true);
    });

    newSocket.on('disconnect', () => {
      console.log('Disconnected from WebSocket');
      setConnected(false);
    });

    newSocket.on('new_interaction', (event: InteractionEvent) => {
      setEvents((prev) => [event, ...prev].slice(0, 50));
    });

    newSocket.on('interaction_update', (event: InteractionEvent) => {
      setEvents((prev) => {
        // Update existing event if found, or add new if relevant
        // Note: interaction_update might not have all fields, so we merge
        const index = prev.findIndex((e) => e.id === event.id);
        if (index !== -1) {
          const newEvents = [...prev];
          newEvents[index] = { ...newEvents[index], ...event };
          return newEvents;
        }
        // If not found, we might want to add it, but usually new_interaction comes first.
        // But if we joined late, we might see updates.
        return [event, ...prev].slice(0, 50);
      });
    });

    return () => {
      newSocket.disconnect();
    };
  }, []);

  return (
    <Box>
      <Box display="flex" alignItems="center" mb={3}>
        <Typography variant="h4" sx={{ mr: 2 }}>Live Monitor</Typography>
        <Chip 
          label={connected ? "Connected" : "Disconnected"} 
          color={connected ? "success" : "error"} 
          variant="outlined" 
        />
      </Box>
      
      <Paper sx={{ p: 2, maxHeight: '80vh', overflow: 'auto' }}>
        <List>
          {events.map((event, index) => (
            <ListItem key={`${event.id}-${index}`} divider>
              <ListItemText
                primary={
                  <Box display="flex" alignItems="center">
                    <Typography variant="subtitle1" sx={{ mr: 2 }}>
                      {event.type === 'scan' ? 'Poster Scan' : 'Interaction'}
                    </Typography>
                    <Chip 
                      label={event.status || 'unknown'} 
                      color={event.status === 'completed' ? 'success' : event.status === 'error' ? 'error' : 'warning'} 
                      size="small" 
                    />
                  </Box>
                }
                secondary={
                  <Box component="span" display="block">
                    <Typography component="span" variant="body2" color="text.primary">
                      {new Date(event.timestamp || Date.now()).toLocaleTimeString()}
                    </Typography>
                    <br />
                    {event.id && `ID: ${event.id.substring(0, 8)}... `}
                    {event.poster_id && `Poster: ${event.poster_id} `}
                    {event.latency_ms && `Latency: ${event.latency_ms}ms`}
                  </Box>
                }
              />
            </ListItem>
          ))}
          {events.length === 0 && (
            <Typography variant="body1" color="text.secondary" align="center" sx={{ py: 4 }}>
              Waiting for real-time events...
            </Typography>
          )}
        </List>
      </Paper>
    </Box>
  );
};

export default LiveMonitor;
