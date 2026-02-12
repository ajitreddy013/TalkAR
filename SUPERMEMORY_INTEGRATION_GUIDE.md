# Supermemory Integration Guide for TalkAR

> **Last Updated**: February 11, 2026  
> **Purpose**: Integrate Supermemory AI for long-term conversation memory and user personalization

---

## 🧠 What is Supermemory?

**Supermemory** is a long-term memory platform for AI applications that allows you to:
- Store and retrieve conversation history using natural language
- Build persistent user profiles with preferences and context
- Enable semantic search across all user interactions
- Provide personalized AI responses based on historical context

### Why Integrate Supermemory into TalkAR?

1. **Enhanced Personalization**: Remember user preferences across sessions
2. **Contextual Conversations**: Maintain conversation history for better AI responses
3. **User Profiles**: Build detailed profiles of user interests and behaviors
4. **Improved Engagement**: Provide more relevant and personalized content

---

## 🚀 Quick Start

### Step 1: Get Supermemory API Key

1. Sign up at: **https://console.supermemory.ai**
2. Create a new project
3. Copy your API key from the dashboard

### Step 2: Install Supermemory SDK

```bash
# For Backend (Node.js/TypeScript)
cd backend
npm install @supermemory/sdk

# For Admin Dashboard (React)
cd admin-dashboard
npm install @supermemory/sdk
```

### Step 3: Add Environment Variables

Add to `backend/.env`:
```env
# Supermemory Configuration
SUPERMEMORY_API_KEY=your_supermemory_api_key_here
SUPERMEMORY_BASE_URL=https://v2.api.supermemory.ai
```

---

## 📦 Backend Integration

### 1. Create Supermemory Service

Create `backend/src/services/supermemoryService.ts`:

```typescript
import axios from 'axios';

interface MemoryContext {
  userId: string;
  content: string;
  metadata?: Record<string, any>;
}

interface UserProfile {
  userId: string;
  preferences?: Record<string, any>;
  facts?: string[];
}

class SupermemoryService {
  private apiKey: string;
  private baseUrl: string;
  private client: any;

  constructor() {
    this.apiKey = process.env.SUPERMEMORY_API_KEY || '';
    this.baseUrl = process.env.SUPERMEMORY_BASE_URL || 'https://v2.api.supermemory.ai';
    
    if (!this.apiKey) {
      console.warn('Supermemory API key not configured');
    }
  }

  /**
   * Add a memory to Supermemory
   */
  async addMemory(context: MemoryContext): Promise<void> {
    try {
      await axios.post(
        `${this.baseUrl}/memories`,
        {
          userId: context.userId,
          content: context.content,
          metadata: context.metadata || {},
        },
        {
          headers: {
            'Authorization': `Bearer ${this.apiKey}`,
            'Content-Type': 'application/json',
          },
        }
      );
      console.log(`Memory added for user ${context.userId}`);
    } catch (error) {
      console.error('Error adding memory to Supermemory:', error);
      throw error;
    }
  }

  /**
   * Search memories for a user
   */
  async searchMemories(userId: string, query: string, limit: number = 5): Promise<any[]> {
    try {
      const response = await axios.post(
        `${this.baseUrl}/memories/search`,
        {
          userId,
          query,
          limit,
        },
        {
          headers: {
            'Authorization': `Bearer ${this.apiKey}`,
            'Content-Type': 'application/json',
          },
        }
      );
      return response.data.memories || [];
    } catch (error) {
      console.error('Error searching memories:', error);
      return [];
    }
  }

  /**
   * Get user profile
   */
  async getUserProfile(userId: string): Promise<UserProfile | null> {
    try {
      const response = await axios.get(
        `${this.baseUrl}/profiles/${userId}`,
        {
          headers: {
            'Authorization': `Bearer ${this.apiKey}`,
          },
        }
      );
      return response.data;
    } catch (error) {
      console.error('Error fetching user profile:', error);
      return null;
    }
  }

  /**
   * Update user profile
   */
  async updateUserProfile(userId: string, profile: Partial<UserProfile>): Promise<void> {
    try {
      await axios.put(
        `${this.baseUrl}/profiles/${userId}`,
        profile,
        {
          headers: {
            'Authorization': `Bearer ${this.apiKey}`,
            'Content-Type': 'application/json',
          },
        }
      );
      console.log(`Profile updated for user ${context.userId}`);
    } catch (error) {
      console.error('Error updating user profile:', error);
      throw error;
    }
  }

  /**
   * Add interaction to memory (TalkAR specific)
   */
  async recordInteraction(
    userId: string,
    posterId: string,
    script: string,
    feedback?: string
  ): Promise<void> {
    const content = `User scanned poster "${posterId}". Generated script: "${script}". ${
      feedback ? `Feedback: ${feedback}` : ''
    }`;
    
    await this.addMemory({
      userId,
      content,
      metadata: {
        type: 'ar_interaction',
        posterId,
        timestamp: new Date().toISOString(),
        hasFeedback: !!feedback,
      },
    });
  }

  /**
   * Get contextual information for AI generation
   */
  async getContextForGeneration(userId: string, posterId: string): Promise<string> {
    const memories = await this.searchMemories(
      userId,
      `poster ${posterId} interactions preferences`,
      3
    );
    
    if (memories.length === 0) {
      return '';
    }

    return `Previous context:\n${memories.map(m => m.content).join('\n')}`;
  }
}

export default new SupermemoryService();
```

### 2. Integrate with AI Pipeline

Update `backend/src/services/aiPipelineService.ts`:

```typescript
import supermemoryService from './supermemoryService';

// In your generateAdContent or similar method:
async generateAdContent(productName: string, userId?: string) {
  let contextPrompt = '';
  
  // Get user context from Supermemory
  if (userId) {
    contextPrompt = await supermemoryService.getContextForGeneration(userId, productName);
  }

  // Use contextPrompt in your AI generation
  const script = await this.generateScript(productName, contextPrompt);
  
  // Record the interaction
  if (userId) {
    await supermemoryService.recordInteraction(userId, productName, script);
  }

  return { script, audioUrl, videoUrl };
}
```

### 3. Create API Routes

Create `backend/src/routes/supermemory.ts`:

```typescript
import express from 'express';
import supermemoryService from '../services/supermemoryService';

const router = express.Router();

// Add memory
router.post('/memories', async (req, res) => {
  try {
    const { userId, content, metadata } = req.body;
    await supermemoryService.addMemory({ userId, content, metadata });
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: 'Failed to add memory' });
  }
});

// Search memories
router.post('/memories/search', async (req, res) => {
  try {
    const { userId, query, limit } = req.body;
    const memories = await supermemoryService.searchMemories(userId, query, limit);
    res.json({ memories });
  } catch (error) {
    res.status(500).json({ error: 'Failed to search memories' });
  }
});

// Get user profile
router.get('/profiles/:userId', async (req, res) => {
  try {
    const profile = await supermemoryService.getUserProfile(req.params.userId);
    res.json(profile);
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch profile' });
  }
});

export default router;
```

Register routes in `backend/src/index.ts`:

```typescript
import supermemoryRoutes from './routes/supermemory';

app.use('/api/v1/supermemory', supermemoryRoutes);
```

---

## 📱 Mobile App Integration

### 1. Update API Service

Update `mobile-app/app/src/main/java/com/talkar/app/data/api/TalkARApiService.kt`:

```kotlin
@POST("supermemory/memories")
suspend fun addMemory(
    @Body request: AddMemoryRequest
): Response<MemoryResponse>

@POST("supermemory/memories/search")
suspend fun searchMemories(
    @Body request: SearchMemoryRequest
): Response<MemoriesResponse>

@GET("supermemory/profiles/{userId}")
suspend fun getUserProfile(
    @Path("userId") userId: String
): Response<UserProfileResponse>
```

### 2. Create Data Models

Create `mobile-app/app/src/main/java/com/talkar/app/data/models/Memory.kt`:

```kotlin
data class AddMemoryRequest(
    val userId: String,
    val content: String,
    val metadata: Map<String, Any>? = null
)

data class SearchMemoryRequest(
    val userId: String,
    val query: String,
    val limit: Int = 5
)

data class MemoryResponse(
    val success: Boolean,
    val message: String? = null
)

data class MemoriesResponse(
    val memories: List<Memory>
)

data class Memory(
    val id: String,
    val content: String,
    val metadata: Map<String, Any>?,
    val createdAt: String
)

data class UserProfileResponse(
    val userId: String,
    val preferences: Map<String, Any>?,
    val facts: List<String>?
)
```

### 3. Update Repository

Update `mobile-app/app/src/main/java/com/talkar/app/data/repository/InteractionRepository.kt`:

```kotlin
suspend fun recordInteractionWithMemory(
    userId: String,
    posterId: String,
    script: String,
    feedback: String? = null
) {
    try {
        val content = "User scanned poster \"$posterId\". Generated script: \"$script\". ${
            feedback?.let { "Feedback: $it" } ?: ""
        }"
        
        apiService.addMemory(
            AddMemoryRequest(
                userId = userId,
                content = content,
                metadata = mapOf(
                    "type" to "ar_interaction",
                    "posterId" to posterId,
                    "timestamp" to System.currentTimeMillis(),
                    "hasFeedback" to (feedback != null)
                )
            )
        )
    } catch (e: Exception) {
        Log.e("InteractionRepository", "Failed to record memory", e)
    }
}
```

---

## 🎛️ Admin Dashboard Integration

### 1. Create Supermemory Service

Create `admin-dashboard/src/services/supermemoryService.ts`:

```typescript
import axios from 'axios';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:3000/api/v1';

export const supermemoryService = {
  async getUserProfile(userId: string) {
    const response = await axios.get(`${API_URL}/supermemory/profiles/${userId}`);
    return response.data;
  },

  async searchUserMemories(userId: string, query: string) {
    const response = await axios.post(`${API_URL}/supermemory/memories/search`, {
      userId,
      query,
      limit: 10,
    });
    return response.data.memories;
  },

  async getUserInteractionHistory(userId: string) {
    const memories = await this.searchUserMemories(userId, 'ar_interaction');
    return memories.filter((m: any) => m.metadata?.type === 'ar_interaction');
  },
};
```

### 2. Create User Profile Component

Create `admin-dashboard/src/components/UserProfileView.tsx`:

```typescript
import React, { useEffect, useState } from 'react';
import { Card, CardContent, Typography, List, ListItem } from '@mui/material';
import { supermemoryService } from '../services/supermemoryService';

interface UserProfileViewProps {
  userId: string;
}

export const UserProfileView: React.FC<UserProfileViewProps> = ({ userId }) => {
  const [profile, setProfile] = useState<any>(null);
  const [memories, setMemories] = useState<any[]>([]);

  useEffect(() => {
    loadUserData();
  }, [userId]);

  const loadUserData = async () => {
    const [profileData, memoryData] = await Promise.all([
      supermemoryService.getUserProfile(userId),
      supermemoryService.getUserInteractionHistory(userId),
    ]);
    setProfile(profileData);
    setMemories(memoryData);
  };

  return (
    <Card>
      <CardContent>
        <Typography variant="h5">User Profile</Typography>
        <Typography>User ID: {userId}</Typography>
        
        <Typography variant="h6" sx={{ mt: 2 }}>Recent Interactions</Typography>
        <List>
          {memories.map((memory, index) => (
            <ListItem key={index}>
              <Typography variant="body2">{memory.content}</Typography>
            </ListItem>
          ))}
        </List>
      </CardContent>
    </Card>
  );
};
```

---

## 🔄 Use Cases in TalkAR

### 1. Personalized Content Generation
- Remember user's preferred language
- Recall previous poster interactions
- Adapt tone based on user preferences

### 2. Conversation Continuity
- Multi-turn conversations with context
- Reference previous interactions
- Build on past feedback

### 3. User Analytics
- Track user engagement patterns
- Identify favorite content types
- Measure retention and satisfaction

### 4. Smart Recommendations
- Suggest related posters based on history
- Recommend content based on preferences
- Personalize AR experiences

---

## 📊 Testing Supermemory Integration

### Test Script

Create `backend/test-supermemory.js`:

```javascript
const axios = require('axios');

const API_URL = 'http://localhost:3000/api/v1';
const TEST_USER_ID = 'test-user-123';

async function testSupermemory() {
  try {
    // 1. Add a memory
    console.log('Adding memory...');
    await axios.post(`${API_URL}/supermemory/memories`, {
      userId: TEST_USER_ID,
      content: 'User scanned Coca-Cola poster and loved the animation',
      metadata: { type: 'ar_interaction', posterId: 'coca-cola-001' },
    });
    console.log('✅ Memory added');

    // 2. Search memories
    console.log('\nSearching memories...');
    const searchResult = await axios.post(`${API_URL}/supermemory/memories/search`, {
      userId: TEST_USER_ID,
      query: 'Coca-Cola',
      limit: 5,
    });
    console.log('✅ Found memories:', searchResult.data.memories.length);

    // 3. Get user profile
    console.log('\nFetching user profile...');
    const profile = await axios.get(`${API_URL}/supermemory/profiles/${TEST_USER_ID}`);
    console.log('✅ User profile:', profile.data);

  } catch (error) {
    console.error('❌ Test failed:', error.message);
  }
}

testSupermemory();
```

Run test:
```bash
cd backend
node test-supermemory.js
```

---

## 🔒 Security Considerations

1. **API Key Protection**: Never commit API keys to version control
2. **User Privacy**: Ensure GDPR compliance for stored memories
3. **Data Encryption**: Use HTTPS for all API calls
4. **Access Control**: Implement user authentication before memory access

---

## 📈 Next Steps

1. ✅ Sign up for Supermemory account
2. ✅ Install SDK in backend
3. ✅ Create Supermemory service
4. ✅ Integrate with AI pipeline
5. ✅ Update mobile app to record interactions
6. ✅ Add user profile view in admin dashboard
7. ✅ Test end-to-end integration
8. ✅ Deploy to production

---

## 📚 Resources

- **Supermemory Docs**: https://docs.supermemory.ai
- **API Reference**: https://docs.supermemory.ai/api-reference
- **Console**: https://console.supermemory.ai
- **GitHub**: https://github.com/supermemoryai

---

**Created**: February 11, 2026  
**For**: TalkAR Project  
**Purpose**: Enable long-term memory and personalization
