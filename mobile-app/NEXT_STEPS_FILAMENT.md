# Next Steps: Filament Material for 3D Video

## Current Status

✅ Audio playback working
❌ Video visuals not showing
🎯 Goal: Add 3D video texture rendering with Filament

## What We're Doing

Using Filament's material system to render video textures on 3D planes in AR.

## Files Created

1. ✅ `materials/video_material.mat` - Material definition (source)
2. ✅ `compile_material.sh` - Compilation script
3. ✅ `FILAMENT_MATERIAL_GUIDE.md` - Complete guide

## Step-by-Step Instructions

### Step 1: Compile Material (YOU DO THIS)

```bash
cd mobile-app
./compile_material.sh
```

**What it does:**
- Downloads Filament tools (if needed)
- Compiles `video_material.mat` to `video_material.filamat`
- Places compiled material in `app/src/main/assets/materials/`

**Expected output:**
```
🔨 Filament Material Compiler
==============================

📱 Platform: mac
✅ Filament already installed at /Users/ajitreddy/Downloads/filament

🔧 Compiling material...
   Source: materials/video_material.mat
   Output: app/src/main/assets/materials/video_material.filamat

✅ Material compiled successfully!
   File: app/src/main/assets/materials/video_material.filamat
   Size: 2.5K

🎉 Ready to use! Build and install the app now.
```

### Step 2: Update VideoPlaneNode (I DO THIS)

After you compile the material, I'll update `VideoPlaneNode.kt` to:
1. Load the compiled material from assets
2. Create material instance
3. Set video texture parameter
4. Apply to renderable
5. Render video on 3D plane

### Step 3: Build and Test

```bash
./gradlew app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## What You'll See

**Before (Current):**
- ✅ Image detection
- ✅ Video audio
- ❌ No video visuals

**After (With Material):**
- ✅ Image detection
- ✅ Video audio
- ✅ Video visuals on 3D plane
- ✅ Perfect tracking with AR image

## Troubleshooting

### Script fails to download

```bash
# Download manually
cd ~/Downloads
curl -L -O https://github.com/google/filament/releases/download/v1.52.0/filament-v1.52.0-mac.tgz
tar -xzf filament-v1.52.0-mac.tgz

# Then run script again
cd mobile-app
./compile_material.sh
```

### Permission denied

```bash
chmod +x compile_material.sh
./compile_material.sh
```

### matc not found

```bash
# Check if Filament was extracted
ls ~/Downloads/filament/bin/matc

# If not there, download manually (see above)
```

## Timeline

1. **Now:** You compile material (~2 minutes)
2. **Next:** I update VideoPlaneNode (~5 minutes)
3. **Then:** Build and test (~2 minutes)
4. **Total:** ~10 minutes to working 3D video

## Why This Approach?

**Pros:**
- ✅ True 3D rendering
- ✅ Perfect AR tracking
- ✅ Professional quality
- ✅ Proper Filament integration
- ✅ Supports external textures (video)

**Cons:**
- ⚠️ Requires material compilation (one-time)
- ⚠️ Need Filament tools

## Alternative (If Compilation Fails)

If you can't compile the material, we can:
1. Use WebView overlay (2D, but works immediately)
2. I can provide pre-compiled material (less ideal)

## Ready?

Run this command and share the output:

```bash
cd mobile-app
./compile_material.sh
```

Once successful, I'll immediately update the code to use the compiled material!

---

**Status:** Waiting for material compilation
**Next:** Update VideoPlaneNode.kt
**ETA:** 10 minutes to working 3D video
