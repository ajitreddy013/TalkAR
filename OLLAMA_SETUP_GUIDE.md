# Ollama Setup Guide - Free Local AI Models

> **Last Updated**: February 11, 2026  
> **Purpose**: Set up Ollama for free local AI model usage as an alternative to paid APIs

---

## 🎯 Why Ollama?

**Ollama** is a free, open-source tool that lets you run large language models locally on your Mac. Perfect for:

- ✅ **Zero API costs** - Run models completely free
- ✅ **Privacy** - All data stays on your machine
- ✅ **Offline capability** - Works without internet
- ✅ **Fast responses** - No network latency
- ✅ **Multiple models** - Choose from dozens of models

### Best Models for TalkAR Development

| Model | Size | RAM Required | Best For |
|-------|------|--------------|----------|
| **llama3.2** | 3B | 4GB | Fast responses, coding |
| **codellama** | 7B | 8GB | Code generation |
| **mistral** | 7B | 8GB | General purpose |
| **deepseek-coder** | 6.7B | 8GB | Advanced coding |
| **qwen2.5-coder** | 7B | 8GB | Code + reasoning |

---

## 🚀 Installation Steps

### Step 1: Install Ollama

```bash
# Install via Homebrew
brew install ollama

# Or download from website
# Visit: https://ollama.com/download
```

### Step 2: Start Ollama Service

```bash
# Start Ollama in the background
ollama serve
```

**Note**: Keep this terminal open, or run it as a background service.

### Step 3: Pull Your First Model

```bash
# Recommended: Llama 3.2 (3B) - Fast and efficient
ollama pull llama3.2

# Alternative: DeepSeek Coder (great for coding)
ollama pull deepseek-coder:6.7b

# Alternative: Qwen 2.5 Coder (excellent for code)
ollama pull qwen2.5-coder:7b
```

### Step 4: Test the Model

```bash
# Interactive chat
ollama run llama3.2

# Try a coding question
ollama run llama3.2 "Write a Python function to reverse a string"
```

---

## 🔧 Integration with TalkAR Backend

### 1. Install Ollama Node.js Client

```bash
cd backend
npm install ollama
```

### 2. Create Ollama Service

Create `backend/src/services/ollamaService.ts`:

```typescript
import { Ollama } from 'ollama';

class OllamaService {
  private ollama: Ollama;
  private model: string;

  constructor() {
    this.ollama = new Ollama({ host: 'http://localhost:11434' });
    this.model = process.env.OLLAMA_MODEL || 'llama3.2';
  }

  /**
   * Generate text using Ollama
   */
  async generateText(prompt: string, systemPrompt?: string): Promise<string> {
    try {
      const response = await this.ollama.generate({
        model: this.model,
        prompt: prompt,
        system: systemPrompt,
        stream: false,
      });

      return response.response;
    } catch (error) {
      console.error('Ollama generation error:', error);
      throw new Error('Failed to generate text with Ollama');
    }
  }

  /**
   * Generate product script using Ollama
   */
  async generateProductScript(productName: string): Promise<string> {
    const systemPrompt = `You are a creative advertising copywriter. Generate engaging, 
    concise product descriptions for AR experiences. Keep responses under 100 words.`;

    const prompt = `Create an engaging 30-second script for an AR talking head advertisement 
    about "${productName}". Make it enthusiastic and memorable.`;

    return await this.generateText(prompt, systemPrompt);
  }

  /**
   * Chat with streaming support
   */
  async chat(messages: Array<{ role: string; content: string }>) {
    try {
      const response = await this.ollama.chat({
        model: this.model,
        messages: messages,
        stream: false,
      });

      return response.message.content;
    } catch (error) {
      console.error('Ollama chat error:', error);
      throw new Error('Failed to chat with Ollama');
    }
  }

  /**
   * Check if Ollama is available
   */
  async isAvailable(): Promise<boolean> {
    try {
      await this.ollama.list();
      return true;
    } catch (error) {
      return false;
    }
  }

  /**
   * List available models
   */
  async listModels(): Promise<string[]> {
    try {
      const models = await this.ollama.list();
      return models.models.map(m => m.name);
    } catch (error) {
      console.error('Failed to list models:', error);
      return [];
    }
  }
}

export default new OllamaService();
```

### 3. Update AI Pipeline to Use Ollama

Update `backend/src/services/aiPipelineService.ts`:

```typescript
import ollamaService from './ollamaService';

class AIPipelineService {
  // ... existing code ...

  async generateScript(productName: string, context?: string): Promise<string> {
    const useOllama = process.env.AI_PROVIDER === 'ollama';

    if (useOllama) {
      console.log('Using Ollama for script generation');
      return await ollamaService.generateProductScript(productName);
    }

    // Fallback to OpenAI/Groq
    return await this.generateScriptWithOpenAI(productName, context);
  }
}
```

### 4. Update Environment Variables

Add to `backend/.env`:

```env
# AI Provider Configuration
AI_PROVIDER=ollama  # or 'openai' or 'groq'
OLLAMA_MODEL=llama3.2  # or 'deepseek-coder:6.7b' or 'qwen2.5-coder:7b'
OLLAMA_HOST=http://localhost:11434
```

---

## 💻 Using Ollama for CLI Coding Assistant

### Option 1: Use Ollama Directly

```bash
# Start interactive session
ollama run deepseek-coder:6.7b

# Ask coding questions
> How do I implement ARCore image tracking in Kotlin?

> Write a TypeScript function to validate email addresses

> Debug this React component: [paste code]
```

### Option 2: Use Continue.dev (VS Code Extension)

**Continue.dev** is a free, open-source AI coding assistant that works with Ollama!

#### Installation:

1. Install VS Code extension: **Continue**
2. Configure to use Ollama:

Create `~/.continue/config.json`:

```json
{
  "models": [
    {
      "title": "Ollama - DeepSeek Coder",
      "provider": "ollama",
      "model": "deepseek-coder:6.7b",
      "apiBase": "http://localhost:11434"
    },
    {
      "title": "Ollama - Llama 3.2",
      "provider": "ollama",
      "model": "llama3.2",
      "apiBase": "http://localhost:11434"
    }
  ],
  "tabAutocompleteModel": {
    "title": "Ollama - DeepSeek Coder",
    "provider": "ollama",
    "model": "deepseek-coder:6.7b"
  }
}
```

#### Usage in VS Code:

- **Cmd+L**: Open chat sidebar
- **Cmd+I**: Inline code editing
- **Tab**: Code autocomplete
- **Cmd+Shift+R**: Refactor selection

### Option 3: Use Aider (Terminal-based AI Coding)

```bash
# Install Aider
pip install aider-chat

# Use with Ollama
aider --model ollama/deepseek-coder:6.7b

# Or set as default
export AIDER_MODEL=ollama/deepseek-coder:6.7b
aider
```

---

## 🎨 Best Practices

### 1. Model Selection

- **Quick tasks**: Use `llama3.2` (3B) - fastest
- **Coding**: Use `deepseek-coder:6.7b` or `qwen2.5-coder:7b`
- **Complex reasoning**: Use `mistral:7b` or larger models

### 2. Prompt Engineering

```typescript
// Good prompt structure
const systemPrompt = `You are an expert Android developer specializing in ARCore.
Provide concise, production-ready code examples.`;

const userPrompt = `Create a Kotlin function to detect ARCore image tracking events.
Include error handling and lifecycle management.`;
```

### 3. Performance Optimization

```bash
# Use quantized models for faster inference
ollama pull llama3.2:3b-instruct-q4_K_M

# Monitor resource usage
ollama ps

# Stop models when not in use
ollama stop llama3.2
```

---

## 📊 Comparison: Ollama vs Paid APIs

| Feature | Ollama (Free) | Claude API | OpenAI API |
|---------|---------------|------------|------------|
| **Cost** | $0 | ~$3-15/M tokens | ~$0.15-60/M tokens |
| **Privacy** | 100% local | Cloud-based | Cloud-based |
| **Speed** | Fast (local) | Medium | Medium |
| **Quality** | Good (7B models) | Excellent | Excellent |
| **Offline** | ✅ Yes | ❌ No | ❌ No |
| **Setup** | Easy | API key | API key |

---

## 🔄 Migration Plan

### Phase 1: Development (Use Ollama)
```env
AI_PROVIDER=ollama
OLLAMA_MODEL=deepseek-coder:6.7b
```

### Phase 2: Testing (Hybrid)
```typescript
// Use Ollama for development, OpenAI for production
const provider = process.env.NODE_ENV === 'production' ? 'openai' : 'ollama';
```

### Phase 3: Production (Choose based on needs)
- **High volume, cost-sensitive**: Keep Ollama on server
- **Best quality needed**: Use Claude/OpenAI
- **Hybrid**: Ollama for simple tasks, API for complex ones

---

## 🛠️ Troubleshooting

### Ollama not starting?
```bash
# Check if service is running
ps aux | grep ollama

# Restart service
pkill ollama
ollama serve
```

### Model too slow?
```bash
# Use smaller quantized model
ollama pull llama3.2:3b-instruct-q4_K_M

# Check system resources
top
```

### Out of memory?
```bash
# Use smaller model
ollama pull llama3.2:1b

# Or increase swap space (advanced)
```

---

## 📚 Recommended Models for TalkAR

### For Script Generation (Product Descriptions)
```bash
ollama pull llama3.2  # Best balance
ollama pull mistral   # More creative
```

### For Code Development
```bash
ollama pull deepseek-coder:6.7b  # Best for coding
ollama pull qwen2.5-coder:7b     # Great alternative
```

### For Chat/Conversation
```bash
ollama pull llama3.2  # Fast and good
ollama pull phi3      # Very fast, smaller
```

---

## 🚀 Next Steps

1. ✅ Install Ollama: `brew install ollama`
2. ✅ Start service: `ollama serve`
3. ✅ Pull model: `ollama pull deepseek-coder:6.7b`
4. ✅ Install Continue.dev in VS Code
5. ✅ Test integration with TalkAR backend
6. ✅ Update `.env` to use Ollama
7. ✅ Remove Claude Code CLI completely (if desired)

---

## 📖 Resources

- **Ollama Website**: https://ollama.com
- **Model Library**: https://ollama.com/library
- **Continue.dev**: https://continue.dev
- **Aider**: https://aider.chat
- **Ollama GitHub**: https://github.com/ollama/ollama

---

**Created**: February 11, 2026  
**For**: TalkAR Project  
**Purpose**: Free local AI alternative to paid APIs
