# TalkAR — Windows Setup Guide

This guide covers everything Windows teammates need to get the project running.

## Prerequisites

Install these in order:

| Tool | Version | Download |
|------|---------|----------|
| Git for Windows | Latest | https://git-scm.com/download/win |
| Docker Desktop | Latest | https://www.docker.com/products/docker-desktop/ |
| Node.js | 18+ | https://nodejs.org/ |
| Android Studio | Latest | https://developer.android.com/studio |

> **Docker Desktop on Windows** requires WSL2. The installer will guide you through enabling it. If prompted to install the Linux kernel update, do it.

---

## 1. Clone the Repository

Open **Git Bash** (installed with Git for Windows) and run:

```bash
git clone <repository-url>
cd "TalkAR -"
```

Use Git Bash (not PowerShell or Command Prompt) for all shell commands in this guide.

---

## 2. Environment Variables

```bash
# Root-level env (used by Docker Compose)
cp .env.example .env

# Backend env
cp backend/env.example backend/.env

# Admin dashboard env
echo "REACT_APP_API_URL=http://localhost:3000/api/v1" > admin-dashboard/.env
```

Open each `.env` file in your editor and fill in the required values. See `.env.example` for descriptions.

---

## 3. Start with Docker (Recommended)

This is the fastest way to get everything running — no manual database setup needed.

```bash
docker-compose up -d
```

Services will be available at:
- Backend API: http://localhost:3000
- Admin Dashboard: http://localhost:3001
- PostgreSQL: localhost:5432

To see logs:
```bash
docker-compose logs -f
```

To stop:
```bash
docker-compose down
```

---

## 4. Manual Setup (Without Docker)

If you prefer running services directly on Windows:

### Backend

Open Git Bash:

```bash
cd backend
npm install
npm run dev
```

For the wav2lip worker (separate terminal):

```bash
cd backend
npm run dev:wav2lip-worker
```

### Admin Dashboard

Open Git Bash:

```bash
cd admin-dashboard
npm install
npm start
```

Access at http://localhost:3001.

---

## 5. Mobile App (Android Studio)

1. Open Android Studio
2. Choose **Open** and select the `mobile-app/` folder
3. Let Gradle sync complete (first time takes a few minutes)
4. Connect a physical Android device (ARCore requires a real device, not an emulator for most features)
5. Run the app from Android Studio

---

## 6. Shell Scripts

A few helper scripts are `.sh` files. On Windows, run them inside **Git Bash**:

```bash
# Local backend with SQLite
bash backend/start-local.sh

# Compile Filament materials
bash mobile-app/compile_material.sh
```

These will **not** work in PowerShell or Command Prompt directly.

---

## 7. Common Windows Issues

### "Cannot find module" after npm install
Windows sometimes has path length issues. Enable long paths:
```
Settings > System > For developers > Enable Long Paths
```
Or run in PowerShell as Administrator:
```powershell
New-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem" -Name "LongPathsEnabled" -Value 1 -PropertyType DWORD -Force
```

### Docker containers fail to start
Make sure Docker Desktop is running and WSL2 is enabled. Check with:
```bash
docker info
```

### Port already in use
Check what's using the port:
```bash
netstat -ano | findstr :3000
```

### Line ending warnings from Git
The `.gitattributes` file handles this automatically. If you see warnings during clone, they're safe to ignore — the files are normalized correctly.

### `npm run dev` exits immediately on backend
Make sure you have a valid `backend/.env` file. The server requires `JWT_SECRET` and database config at minimum.

---

## 8. VS Code (Optional but Recommended)

Install the recommended extensions by opening the project and clicking **Install All** when prompted. The workspace config at `TalkAR.code-workspace` sets up the multi-root workspace correctly.

Useful extensions:
- ESLint
- Prettier
- Docker
- Kotlin
- Android (by Google)
