# Filament Material Compilation Guide

## Overview

To display video textures in 3D AR, we need to compile a Filament material that supports external textures (for video playback).

## Files

- **Source:** `materials/video_material.mat` - Material definition
- **Output:** `app/src/main/assets/materials/video_material.filamat` - Compiled binary
- **Code:** `VideoPlaneNode.kt` - Will load and use the compiled material

## Step 1: Download Filament Tools

### macOS

```bash
cd ~/Downloads
curl -L -O https://github.com/google/filament/releases/download/v1.52.0/filament-v1.52.0-mac.tgz
tar -xzf filament-v1.52.0-mac.tgz
```

### Linux

```bash
cd ~/Downloads
curl -L -O https://github.com/google/filament/releases/download/v1.52.0/filament-v1.52.0-linux.tgz
tar -xzf filament-v1.52.0-linux.tgz
```

### Windows

```bash
# Download from: https://github.com/google/filament/releases/download/v1.52.0/filament-v1.52.0-windows.tgz
# Extract to C:\filament
```

## Step 2: Compile Material

### macOS/Linux

```bash
cd mobile-app

# Create output directory
mkdir -p app/src/main/assets/materials

# Compile material
~/Downloads/filament/bin/matc \
  -a opengl \
  -p mobile \
  -o app/src/main/assets/materials/video_material.filamat \
  materials/video_material.mat
```

### Windows

```bash
cd mobile-app
mkdir app\src\main\assets\materials
C:\filament\bin\matc.exe -a opengl -p mobile -o app\src\main\assets\materials\video_material.filamat materials\video_material.mat
```

## Step 3: Verify Compilation

```bash
# Check if file was created
ls -lh app/src/main/assets/materials/video_material.filamat

# Should see something like:
# -rw-r--r--  1 user  staff   2.5K  video_material.filamat
```

## Step 4: Update VideoPlaneNode.kt

I'll update the code to load and use the compiled material.

## Material Explanation

### Material Definition

```material
material {
    name : VideoMaterial,
    shadingModel : unlit,  // No lighting calculations needed
    
    parameters : [
        {
            type : samplerExternal,  // External texture (for video)
            name : videoTexture
        }
    ],
    
    requires : [
        uv0  // UV coordinates for texture mapping
    ],
    
    fragment {
        void material(inout MaterialInputs material) {
            prepareMaterial(material);
            vec2 uv = getUV0();
            // Sample video texture and set as base color
            material.baseColor = texture(materialParams_videoTexture, uv);
        }
    }
}
```

### Key Points

1. **unlit** - No lighting, just display the texture as-is
2. **samplerExternal** - Required for video textures (Android SurfaceTexture)
3. **uv0** - UV coordinates from vertex buffer
4. **texture()** - Samples the video texture at UV coordinates

## Troubleshooting

### matc: command not found

```bash
# Add to PATH temporarily
export PATH="$HOME/Downloads/filament/bin:$PATH"

# Or use full path
~/Downloads/filament/bin/matc --help
```

### Compilation Errors

```bash
# Check syntax
~/Downloads/filament/bin/matc --help

# Validate material
~/Downloads/filament/bin/matc -a opengl -p mobile materials/video_material.mat
```

### File Not Found

```bash
# Ensure directories exist
mkdir -p app/src/main/assets/materials

# Check material source exists
cat materials/video_material.mat
```

## Alternative: Pre-compiled Material

If you can't compile the material, I can provide a pre-compiled `.filamat` file, but it's better to compile it yourself to ensure compatibility with your Filament version (1.52.0).

## Next Steps

After compiling:

1. ✅ Material compiled to `.filamat`
2. ⏳ Update VideoPlaneNode.kt to load material
3. ⏳ Apply material to renderable
4. ⏳ Set video texture parameter
5. ⏳ Test video rendering

## Quick Start Script

Save this as `compile_material.sh`:

```bash
#!/bin/bash

# Download Filament if not exists
if [ ! -d "$HOME/Downloads/filament" ]; then
    echo "Downloading Filament..."
    cd ~/Downloads
    curl -L -O https://github.com/google/filament/releases/download/v1.52.0/filament-v1.52.0-mac.tgz
    tar -xzf filament-v1.52.0-mac.tgz
fi

# Compile material
echo "Compiling material..."
cd "$(dirname "$0")"
mkdir -p app/src/main/assets/materials

~/Downloads/filament/bin/matc \
  -a opengl \
  -p mobile \
  -o app/src/main/assets/materials/video_material.filamat \
  materials/video_material.mat

if [ $? -eq 0 ]; then
    echo "✅ Material compiled successfully!"
    ls -lh app/src/main/assets/materials/video_material.filamat
else
    echo "❌ Compilation failed"
    exit 1
fi
```

Make executable and run:

```bash
chmod +x compile_material.sh
./compile_material.sh
```

## Resources

- [Filament Releases](https://github.com/google/filament/releases)
- [Material Guide](https://google.github.io/filament/Materials.html)
- [matc Documentation](https://google.github.io/filament/matc.html)
- [External Textures](https://google.github.io/filament/Materials.html#materialdefinitions/materialproperties/externalsampler)

---

**Ready?** Run the compilation command and let me know when the `.filamat` file is created!
