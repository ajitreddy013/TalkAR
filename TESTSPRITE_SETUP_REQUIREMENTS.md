# TestSprite Setup Requirements

## System Requirements (Verified)

### Core Dependencies

- ✅ **Node.js**: v18.20.8 (minimum v18.0.0 required)
- ✅ **npm**: v10.8.2
- ✅ **OS**: macOS (zsh shell)

### TestSprite MCP Server

- ✅ **Location**: `/testsprite-mcp`
- ✅ **Status**: Installed and built successfully
- ✅ **Version**: 1.0.0
- ✅ **Dependencies**: All installed (139 packages, 0 vulnerabilities)

## Installation Commands

### 1. Install TestSprite MCP

```bash
cd testsprite-mcp
npm install
npm run build
```

### 2. Verify Installation

```bash
# Check if the built server exists
ls -la testsprite-mcp/dist/index.js

# Test the server (MCP Inspector mode)
npm --prefix testsprite-mcp test
```

### 3. Run TestSprite MCP Server

```bash
# Production mode
npm --prefix testsprite-mcp start

# Development mode (with auto-reload)
npm --prefix testsprite-mcp run dev
```

## Additional Tools Needed (For Full Testing Suite)

### Backend Testing

- [ ] **Jest**: For unit and API tests
- [ ] **Supertest**: For HTTP API testing
- [ ] **ts-jest**: TypeScript support for Jest

### Frontend E2E Testing

- [ ] **Playwright**: Browser automation (recommended)
  - Install: `npm install -D @playwright/test`
  - Install browsers: `npx playwright install`
- OR **Cypress**: Alternative E2E framework
  - Install: `npm install -D cypress`

### Mobile Testing

- [ ] **Android Studio**: For Android emulators
  - Download from: https://developer.android.com/studio
  - Configure AVD (Android Virtual Device)
- [ ] **Xcode**: For iOS simulators (macOS only)
  - Install from App Store
- [ ] **Detox** (React Native) OR **Appium** (Native apps)
  - Detox: `npm install -D detox detox-cli`
  - Appium: `npm install -g appium`

### CI/CD Requirements

- [ ] **GitHub Actions**: Already available in repo
- [ ] **Docker**: For containerized test environments
  - Check: `docker --version`

## Environment Variables Needed

Create a `.env.test` file in the project root:

```bash
# TestSprite Configuration
TESTSPRITE_API_KEY=demo-key  # or your actual API key

# Supabase Test Environment
SUPABASE_URL=your_test_supabase_url
SUPABASE_ANON_KEY=your_test_anon_key
SUPABASE_SERVICE_ROLE_KEY=your_test_service_key

# Backend Test Configuration
BACKEND_PORT=3001
BACKEND_URL=http://localhost:3001

# Frontend Test Configuration
FRONTEND_URL=http://localhost:3000

# Mobile Test Configuration (if applicable)
ANDROID_SDK_ROOT=/path/to/android/sdk
ANDROID_HOME=/path/to/android/sdk
```

## Browser Requirements (for E2E tests)

### Playwright (Recommended)

```bash
# Install all browsers
npx playwright install

# Or specific browsers
npx playwright install chromium
npx playwright install firefox
npx playwright install webkit
```

### Cypress (Alternative)

```bash
# Cypress automatically downloads Chrome-based browser
npx cypress install
```

## Verification Checklist

- [x] Node.js >= 18.0.0 installed
- [x] npm installed
- [x] TestSprite MCP installed
- [x] TestSprite MCP built successfully
- [ ] Playwright/Cypress installed (for E2E)
- [ ] Android Studio installed (for mobile)
- [ ] Environment variables configured
- [ ] Backend can start on localhost
- [ ] Frontend can start on localhost

## Next Steps

1. ✅ Install TestSprite CLI (COMPLETED)
2. ⏭️ Create TestSprite project config (`testsprite.yml`)
3. ⏭️ Add test suites for backend, frontend, and mobile
4. ⏭️ Run tests locally
5. ⏭️ Set up CI/CD pipeline

## Troubleshooting

### Node version too old

```bash
# Use nvm to install Node 18+
nvm install 18
nvm use 18
```

### TestSprite MCP build fails

```bash
# Clean and rebuild
cd testsprite-mcp
rm -rf node_modules dist
npm install
npm run build
```

### Permission denied errors

```bash
# Fix npm permissions (macOS/Linux)
sudo chown -R $(whoami) ~/.npm
```

## Resources

- TestSprite MCP README: `/testsprite-mcp/README.md`
- Backend Tests: `/backend/tests/`
- Admin Dashboard: `/admin-dashboard/`
- Mobile App: `/mobile-app/`
