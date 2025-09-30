# TalkAR API Documentation

## Overview

The TalkAR API provides endpoints for managing images, dialogues, and generating lip-synced videos using the Sync API.

## Base URL

```
http://localhost:3000/api/v1
```

## Authentication

Most endpoints require authentication using JWT tokens. Include the token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

## Endpoints

### Images

#### GET /images

Get all active images with their dialogues.

**Response:**

```json
[
  {
    "id": "uuid",
    "name": "Poster Name",
    "description": "Description",
    "imageUrl": "https://...",
    "thumbnailUrl": "https://...",
    "isActive": true,
    "createdAt": "2023-01-01T00:00:00Z",
    "updatedAt": "2023-01-01T00:00:00Z",
    "dialogues": [
      {
        "id": "uuid",
        "text": "Hello, welcome to our store!",
        "language": "en",
        "voiceId": "voice-1",
        "isDefault": true
      }
    ]
  }
]
```

#### GET /images/:id

Get a specific image by ID.

#### POST /images

Create a new image.

**Request (multipart/form-data):**

- `image`: Image file
- `name`: Image name
- `description`: Optional description

#### PUT /images/:id

Update an image.

**Request:**

```json
{
  "name": "Updated Name",
  "description": "Updated Description",
  "isActive": true
}
```

#### DELETE /images/:id

Delete an image.

### Dialogues

#### POST /images/:id/dialogues

Add a dialogue to an image.

**Request:**

```json
{
  "text": "Hello, welcome!",
  "language": "en",
  "voiceId": "voice-1",
  "isDefault": true
}
```

#### PUT /images/:imageId/dialogues/:dialogueId

Update a dialogue.

#### DELETE /images/:imageId/dialogues/:dialogueId

Delete a dialogue.

### Sync API

#### POST /sync/generate

Generate a lip-synced video.

**Request:**

```json
{
  "text": "Hello, welcome to our store!",
  "language": "en",
  "voiceId": "voice-1"
}
```

**Response:**

```json
{
  "jobId": "uuid",
  "status": "pending",
  "videoUrl": "https://...",
  "duration": 15
}
```

#### GET /sync/status/:jobId

Get the status of a sync job.

#### GET /sync/voices

Get available voices.

**Response:**

```json
[
  {
    "id": "voice-1",
    "name": "Male Voice 1",
    "language": "en",
    "gender": "male"
  }
]
```

### Admin Endpoints

#### GET /admin/images

Get all images (including inactive) with pagination.

**Query Parameters:**

- `page`: Page number (default: 1)
- `limit`: Items per page (default: 10)
- `search`: Search term

#### GET /admin/analytics

Get analytics data.

**Response:**

```json
{
  "totalImages": 50,
  "activeImages": 45,
  "totalDialogues": 120,
  "languageStats": [
    {
      "language": "en",
      "count": 60
    }
  ]
}
```

## Error Responses

All endpoints return errors in the following format:

```json
{
  "error": "Error message"
}
```

Common HTTP status codes:

- `200`: Success
- `201`: Created
- `400`: Bad Request
- `401`: Unauthorized
- `403`: Forbidden
- `404`: Not Found
- `500`: Internal Server Error
