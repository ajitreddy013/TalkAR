# How to Render Your Architecture Diagrams

## 🚀 Quick Start - Easiest Methods

### Method 1: Mermaid Live Editor (Recommended - No Installation)

**Best for**: Quick conversion, presentations, sharing

1. **Go to**: https://mermaid.live
2. **Copy** any diagram code from `ARCHITECTURE_DIAGRAMS.md`
3. **Paste** into the left panel
4. **See** the diagram render automatically on the right
5. **Export** as:
   - PNG (for presentations)
   - SVG (for high-quality documents)
   - PDF (for printing)

**Step-by-step:**
```
1. Open https://mermaid.live in your browser
2. Delete the example code in the editor
3. Copy a diagram from ARCHITECTURE_DIAGRAMS.md (including the ```mermaid markers)
4. Paste it in the editor
5. Click "Actions" → "PNG" or "SVG" to download
```

---

### Method 2: GitHub/GitLab (Automatic Rendering)

**Best for**: Documentation, team sharing

**GitHub:**
1. Push `ARCHITECTURE_DIAGRAMS.md` to your GitHub repo
2. View the file on GitHub - diagrams render automatically!
3. No extra steps needed ✨

**GitLab:**
1. Push the file to your GitLab repo
2. Diagrams render automatically in markdown files
3. Works in wikis, issues, and merge requests

**Example:**
```bash
git add ARCHITECTURE_DIAGRAMS.md
git commit -m "Add architecture diagrams"
git push origin main
# Now view the file on GitHub - diagrams will be rendered!
```

---

### Method 3: VS Code (For Developers)

**Best for**: Editing and previewing while coding

**Install Extensions:**
1. Open VS Code
2. Go to Extensions (Ctrl+Shift+X or Cmd+Shift+X)
3. Search for "Mermaid"
4. Install one of these:
   - **"Markdown Preview Mermaid Support"** by Matt Bierner
   - **"Mermaid Markdown Syntax Highlighting"** by Bpruitt-goddard

**Usage:**
1. Open `ARCHITECTURE_DIAGRAMS.md` in VS Code
2. Press `Ctrl+Shift+V` (or `Cmd+Shift+V` on Mac)
3. See rendered diagrams in preview pane
4. Right-click preview → "Open Preview to the Side"

---

## 🎨 Online Tools (No Installation Required)

### 1. **Mermaid Live Editor** ⭐ RECOMMENDED
- **URL**: https://mermaid.live
- **Features**: 
  - Real-time preview
  - Export PNG, SVG, PDF
  - Share via URL
  - Edit themes/colors
- **Cost**: FREE

### 2. **Mermaid Chart**
- **URL**: https://www.mermaidchart.com
- **Features**:
  - Professional diagrams
  - Collaboration tools
  - Version control
  - Team workspaces
- **Cost**: Free tier available, paid plans for teams

### 3. **Draw.io / Diagrams.net**
- **URL**: https://app.diagrams.net
- **Features**:
  - Import Mermaid code
  - Additional editing tools
  - Multiple export formats
- **How to**:
  1. Go to https://app.diagrams.net
  2. Arrange → Insert → Advanced → Mermaid
  3. Paste your diagram code
  4. Click "Insert"

### 4. **Kroki**
- **URL**: https://kroki.io
- **Features**:
  - API for converting diagrams
  - Multiple diagram types
  - REST API access
- **Usage**: 
  ```
  https://kroki.io/mermaid/svg/<encoded-diagram>
  ```

---

## 💻 Desktop Applications

### 1. **Typora** (Markdown Editor)
- **URL**: https://typora.io
- **Features**:
  - Native Mermaid support
  - WYSIWYG markdown editor
  - Export to PDF, HTML, images
- **Cost**: $14.99 one-time purchase
- **Platforms**: Windows, macOS, Linux

### 2. **Obsidian** (Note-taking)
- **URL**: https://obsidian.md
- **Features**:
  - Built-in Mermaid support
  - Knowledge base tool
  - Plugin ecosystem
- **Cost**: FREE
- **Platforms**: Windows, macOS, Linux, iOS, Android

### 3. **Mark Text**
- **URL**: https://marktext.app
- **Features**:
  - Free and open-source
  - Mermaid diagram support
  - Real-time preview
- **Cost**: FREE
- **Platforms**: Windows, macOS, Linux

---

## 🌐 Documentation Platforms (Auto-Render)

### 1. **Notion**
- Add code block
- Set language to "mermaid"
- Paste diagram code
- It renders automatically!

### 2. **Confluence**
- Install "Mermaid Diagrams for Confluence" add-on
- Use Mermaid macro
- Paste diagram code

### 3. **GitBook**
- Native Mermaid support
- Just paste diagram code in markdown
- Renders automatically

### 4. **Docusaurus**
- Native support with plugin
- Add to markdown files
- Builds as static site

### 5. **MkDocs**
- Install `mkdocs-mermaid2-plugin`
- Add to docs
- Renders in HTML output

---

## 📱 Mobile Apps

### 1. **Mermaid Editor (Android)**
- Search "Mermaid Editor" on Google Play
- Edit and preview diagrams on mobile
- Export as images

### 2. **Obsidian Mobile** (iOS/Android)
- Full Mermaid support
- Sync with desktop
- View and edit diagrams

---

## 🔧 Command Line Tools

### 1. **Mermaid CLI**

**Install:**
```bash
npm install -g @mermaid-js/mermaid-cli
```

**Convert to PNG:**
```bash
mmdc -i ARCHITECTURE_DIAGRAMS.md -o output.png
```

**Convert to SVG:**
```bash
mmdc -i ARCHITECTURE_DIAGRAMS.md -o output.svg
```

**Convert to PDF:**
```bash
mmdc -i ARCHITECTURE_DIAGRAMS.md -o output.pdf
```

**Batch convert all diagrams:**
```bash
# Extract each diagram to separate file first, then:
mmdc -i diagram1.mmd -o diagram1.png
mmdc -i diagram2.mmd -o diagram2.svg
```

### 2. **Pandoc with Mermaid Filter**

**Install:**
```bash
npm install -g mermaid-filter
```

**Convert markdown to HTML with diagrams:**
```bash
pandoc ARCHITECTURE_DIAGRAMS.md -o output.html --filter mermaid-filter
```

**Convert to PDF:**
```bash
pandoc ARCHITECTURE_DIAGRAMS.md -o output.pdf --filter mermaid-filter
```

---

## 🎯 Best Tool for Each Use Case

| Use Case | Recommended Tool | Why |
|----------|-----------------|-----|
| **Quick Preview** | Mermaid Live Editor | Instant, no setup |
| **Team Collaboration** | GitHub/GitLab | Auto-renders, version control |
| **Presentations** | Mermaid Live → Export PNG | High quality exports |
| **Documentation** | Notion/Confluence | Integrated, searchable |
| **Development** | VS Code Extension | Edit while coding |
| **Offline Work** | Obsidian/Typora | Works without internet |
| **Batch Conversion** | Mermaid CLI | Automate multiple files |
| **Mobile Viewing** | GitHub Mobile App | View diagrams anywhere |

---

## 📋 Step-by-Step: Create Presentation-Ready Images

### Using Mermaid Live Editor:

1. **Open Mermaid Live**
   ```
   https://mermaid.live
   ```

2. **Copy First Diagram**
   - Open `ARCHITECTURE_DIAGRAMS.md`
   - Copy everything from "```mermaid" to the closing "```"
   - Include the mermaid markers!

3. **Paste in Editor**
   - Delete the example in Mermaid Live
   - Paste your diagram code

4. **Customize (Optional)**
   - Click "Configuration" to change theme
   - Options: default, dark, forest, neutral
   - Adjust colors and fonts

5. **Export**
   - Click "Actions" menu
   - Choose format:
     - **PNG** - For PowerPoint, Google Slides (300 DPI)
     - **SVG** - For scalable graphics, web
     - **PDF** - For printing, documentation

6. **Repeat for Each Diagram**
   - System Overview Architecture
   - Component Diagram
   - Data Flow Architecture
   - Mobile App Architecture
   - Backend API Architecture
   - Database Schema
   - Deployment Architecture
   - AR Experience Flow

7. **Organize**
   - Create folder: `diagrams/`
   - Save with descriptive names:
     - `01-system-overview.png`
     - `02-component-diagram.png`
     - `03-data-flow.png`
     - etc.

---

## 🖼️ Quick Script to Extract All Diagrams

I'll create a helper script for you:

**Save as `extract-diagrams.sh`:**

```bash
#!/bin/bash

# Extract all Mermaid diagrams from markdown file
# Usage: ./extract-diagrams.sh ARCHITECTURE_DIAGRAMS.md

mkdir -p diagrams

# This extracts each mermaid block to a separate file
awk '/```mermaid/,/```/' "$1" | 
  awk 'BEGIN{n=0} /```mermaid/{n++; next} /```/{next} {print > "diagrams/diagram-"n".mmd"}'

echo "Extracted diagrams to diagrams/ folder"
echo "Now convert them with: mmdc -i diagrams/diagram-1.mmd -o diagrams/diagram-1.png"
```

**Make it executable:**
```bash
chmod +x extract-diagrams.sh
./extract-diagrams.sh ARCHITECTURE_DIAGRAMS.md
```

---

## 🎨 Customization Tips

### Change Theme in Mermaid Live:
```
Click "Configuration" → Select theme:
- default (blue/neutral)
- dark (dark background)
- forest (green theme)
- neutral (minimal)
```

### Add Custom Colors:
```mermaid
%%{init: {'theme':'base', 'themeVariables': { 'primaryColor':'#4CAF50', 'primaryTextColor':'#fff'}}}%%
graph TD
    A[Your Diagram]
```

### Adjust Font Size:
```
In Mermaid Live Config:
fontSize: 16
fontFamily: 'Arial'
```

---

## 🔗 Important Links

| Tool | URL | Purpose |
|------|-----|---------|
| Mermaid Live Editor | https://mermaid.live | Main conversion tool |
| Mermaid Documentation | https://mermaid.js.org | Syntax reference |
| Mermaid CLI GitHub | https://github.com/mermaid-js/mermaid-cli | Command line tool |
| VS Code Extension | Search "Mermaid" in VS Code | Editor integration |
| GitHub Rendering | Your repo URL | Auto-render in repo |

---

## 💡 Pro Tips

1. **For Presentations**: 
   - Export as SVG for infinite scaling
   - Use PNG at 300 DPI for crisp images

2. **For Documentation**:
   - Keep diagrams in markdown for easy updates
   - Use GitHub/GitLab for automatic rendering

3. **For Sharing**:
   - Mermaid Live has a "Share" button for URLs
   - Others can view without installing anything

4. **For Editing**:
   - Use VS Code with Mermaid extension
   - See live preview while you edit

5. **For Teams**:
   - Push to GitHub - everyone sees rendered diagrams
   - No need to share PNG files separately

---

## ❓ Troubleshooting

**Diagram not rendering?**
- Check syntax - every `graph` needs `TB`, `LR`, etc.
- Ensure no special characters in node IDs
- Verify closing ``` markers

**Export looks blurry?**
- Use SVG instead of PNG for presentations
- Increase DPI in Mermaid CLI: `--scale 2`

**Colors look wrong?**
- Change theme in Configuration panel
- Add custom theme variables

**Text too small?**
- Adjust fontSize in Configuration
- Use `--scale` flag in CLI

---

## 🚀 Recommended Workflow

1. **Edit** in VS Code with Mermaid extension
2. **Preview** live as you type
3. **Commit** to GitHub for team sharing
4. **Export** via Mermaid Live for presentations
5. **Embed** in Notion/Confluence for documentation

---

**Quick Start Command:**
```bash
# 1. View diagrams now (easiest):
# Open https://mermaid.live and paste any diagram

# 2. Install CLI for batch conversion:
npm install -g @mermaid-js/mermaid-cli

# 3. Convert single diagram:
mmdc -i input.mmd -o output.png

# 4. Or just push to GitHub and view there!
git add ARCHITECTURE_DIAGRAMS.md
git commit -m "Add diagrams"
git push
```

---

Need help with a specific tool? Let me know! 🎨
