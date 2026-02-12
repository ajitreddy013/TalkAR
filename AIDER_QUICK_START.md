# Aider Quick Start Guide

## 🎉 Aider is Installed!

Aider is a powerful AI coding assistant that runs in your terminal and can directly edit your code files.

---

## 🚀 Quick Start

### Start Aider with DeepSeek Coder:

```bash
aider --model ollama/deepseek-coder:6.7b
```

### Or set it as default (recommended):

```bash
# Add to your ~/.zshrc or ~/.bashrc
export AIDER_MODEL=ollama/deepseek-coder:6.7b

# Then just run:
aider
```

---

## 💡 How to Use Aider

### Basic Commands:

```bash
# Start in your project directory
cd /Users/ajitreddy/Engineering/Projets/TalkAR/TalkAR\ -
aider --model ollama/deepseek-coder:6.7b

# Now you can:
> Add error handling to backend/src/index.ts

> Fix the camera preview in mobile-app/app/src/.../CameraPreviewView.kt

> Explain how ARCore image tracking works

> /add backend/src/routes/images.ts  # Add file to context

> /help  # See all commands

> /exit  # Quit
```

### Aider Special Commands:

| Command | Description |
|---------|-------------|
| `/add <file>` | Add file to chat context |
| `/drop <file>` | Remove file from context |
| `/ls` | List files in context |
| `/git` | Run git command |
| `/commit` | Commit changes |
| `/undo` | Undo last change |
| `/help` | Show all commands |
| `/exit` | Quit aider |

---

## 🎯 Example Sessions

### Example 1: Fix a Bug

```bash
$ aider --model ollama/deepseek-coder:6.7b

> /add backend/src/routes/images.ts
> There's a bug where image uploads fail. Can you add better error handling?

# Aider will edit the file directly!
# Review changes, then:

> /commit
# Aider makes a git commit for you!
```

### Example 2: Add a Feature

```bash
$ aider --model ollama/deepseek-coder:6.7b

> /add mobile-app/app/src/main/java/com/talkar/app/ui/components/CameraPreviewView.kt
> Add a zoom feature to the camera preview

# Aider edits the file
# Test it, then commit:

> /commit
```

### Example 3: Ask Questions

```bash
$ aider --model ollama/deepseek-coder:6.7b

> Explain how ARCore image tracking works in Kotlin

> What's the best way to implement lip-sync in Android?

> How do I optimize video playback in ExoPlayer?
```

---

## ⚙️ Configuration

### Create ~/.aider.conf.yml:

```yaml
model: ollama/deepseek-coder:6.7b
auto-commits: false
pretty: true
stream: true
```

Then just run `aider` without flags!

---

## 🎨 Aider vs Continue.dev

| Feature | Aider | Continue.dev |
|---------|-------|--------------|
| **Setup** | ✅ Easy | ❌ Complex |
| **File Editing** | ✅ Direct | ⚠️ Suggestions |
| **Git Integration** | ✅ Built-in | ❌ None |
| **Terminal** | ✅ Native | ❌ VS Code only |
| **Ollama Support** | ✅ Perfect | ⚠️ Buggy |
| **Context** | ✅ Multi-file | ✅ Yes |

---

## 🔥 Pro Tips

### 1. Start in Project Root:
```bash
cd /Users/ajitreddy/Engineering/Projets/TalkAR/TalkAR\ -
aider --model ollama/deepseek-coder:6.7b
```

### 2. Add Multiple Files:
```bash
> /add backend/src/**/*.ts
> /add mobile-app/app/src/main/java/com/talkar/app/**/*.kt
```

### 3. Use with Git:
```bash
# Aider works best with git repos
> /git status
> /commit  # Auto-commits changes
```

### 4. Review Before Committing:
```bash
# After Aider makes changes:
> /diff  # See what changed
> /undo  # Undo if needed
> /commit  # Commit if good
```

---

## 🚀 Try It Now!

Open a terminal and run:

```bash
cd /Users/ajitreddy/Engineering/Projets/TalkAR/TalkAR\ -
aider --model ollama/deepseek-coder:6.7b
```

Then try:
```
> Explain the TalkAR project architecture
```

---

## 📚 Resources

- **Aider Docs**: https://aider.chat
- **GitHub**: https://github.com/paul-gauthier/aider
- **Commands**: Type `/help` in Aider

---

**Created**: February 11, 2026  
**For**: TalkAR Project  
**Model**: DeepSeek Coder 6.7B (Local/Free)
