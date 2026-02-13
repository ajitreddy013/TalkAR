# Continue.dev + Ollama Setup - Quick Fix Guide

## ❌ If YAML Config Isn't Working

Try configuring through the Continue.dev UI instead:

### Method 1: Use Continue.dev Settings UI (Recommended)

1. **Open Continue.dev sidebar** in VS Code
   - Press `Cmd + L`

2. **Click the gear icon** ⚙️ at the bottom of Continue sidebar

3. **In the settings that open:**
   - Look for "Models" section
   - Click **"+ Add Model"** or **"Add Chat Model"**

4. **Fill in the form:**
   - **Provider**: Select "Ollama" from dropdown
   - **Model**: Type `deepseek-coder:6.7b`
   - **API Base** (if shown): `http://localhost:11434`
   - Click **Save**

5. **Set as default:**
   - Make sure "DeepSeek Coder" is selected in the model dropdown

6. **Test it:**
   - Type a question in the chat
   - Press Enter

---

## Method 2: Use Ollama Directly (Bypass Continue.dev)

If Continue.dev still doesn't work, you can use Ollama directly:

### In Terminal:
```bash
ollama run deepseek-coder:6.7b
```

Then ask your coding questions directly!

### Example Session:
```bash
$ ollama run deepseek-coder:6.7b

>>> Explain ARCore image tracking in Kotlin

>>> How do I implement a camera preview in Jetpack Compose?

>>> Debug this code: [paste your code]

>>> /bye  # to exit
```

---

## Method 3: Use Aider (Better CLI Experience)

Install Aider for a better terminal-based AI coding assistant:

```bash
# Install
pip install aider-chat

# Use with Ollama
aider --model ollama/deepseek-coder:6.7b

# Or set as default
export AIDER_MODEL=ollama/deepseek-coder:6.7b
aider
```

Aider can:
- Edit files directly
- Make git commits
- Understand your entire codebase
- Run in your terminal

---

## Verify Ollama is Working

Test that Ollama responds:

```bash
curl http://localhost:11434/api/generate -d '{
  "model": "deepseek-coder:6.7b",
  "prompt": "Write a hello world in Kotlin",
  "stream": false
}'
```

If this works, Ollama is fine - the issue is just Continue.dev configuration.

---

## Alternative: Use Cursor or Other IDEs

If Continue.dev keeps having issues, consider:

1. **Cursor IDE** - Built-in AI with Ollama support
2. **Cody** - VS Code extension, supports Ollama
3. **Tabby** - Code completion, works with Ollama
4. **Direct Ollama** - Just use terminal

---

## Quick Test Right Now

Open a terminal and run:

```bash
ollama run deepseek-coder:6.7b "Explain what ARCore is in one sentence"
```

This should work immediately and prove your setup is fine!
