# 3D Avatar Models Directory

This directory contains 3D avatar models in GLB/GLTF format for AR rendering.

## Supported Formats

- **.glb** (Binary GLTF) - Recommended
- **.gltf** (Text GLTF with separate textures)

## Sample Avatar Models

### Where to Get Sample Avatars:

1. **ReadyPlayerMe** (https://readyplayer.me/)

   - Free celebrity-style avatars
   - Export as GLB format
   - Optimized for mobile

2. **Mixamo** (https://www.mixamo.com/)

   - Adobe's free 3D character library
   - Includes rigged characters with animations
   - Download as FBX and convert to GLB

3. **Sketchfab** (https://sketchfab.com/)

   - Free downloadable models
   - Filter by "Downloadable" and "GLB" format

4. **Poly Pizza** (https://poly.pizza/)
   - Free low-poly models
   - Good for mobile performance

## Recommended Avatars for TalkAR:

### Celebrity-Style Avatars (Placeholder Names):

1. `avatar_celebrity_male_1.glb` - Male celebrity style (e.g., SRK-style)
2. `avatar_celebrity_female_1.glb` - Female celebrity style
3. `avatar_generic_presenter.glb` - Generic professional presenter

## File Naming Convention:

```
avatar_<type>_<gender>_<id>.glb
```

Examples:

- `avatar_celebrity_male_1.glb`
- `avatar_sports_female_1.glb`
- `avatar_generic_neutral_1.glb`

## Model Requirements:

### Technical Specs:

- **Max Polygon Count**: 10,000 triangles (for mobile performance)
- **Texture Size**: 1024x1024 max
- **File Size**: < 5MB per model
- **Rig**: Humanoid rig for animations
- **Animations**: Include idle, breathing, blinking (optional)

### Optimization Tips:

1. Use Blender to reduce polygon count
2. Compress textures to minimize file size
3. Remove unnecessary bones/materials
4. Bake textures to reduce draw calls

## Converting FBX to GLB:

### Using Blender:

```bash
# Install Blender (https://www.blender.org/)
# Open FBX file
# File > Export > glTF 2.0 (.glb/.gltf)
# Select GLB format
# Export
```

### Using Online Converter:

- https://anyconv.com/fbx-to-glb-converter/
- https://products.aspose.app/3d/conversion/fbx-to-glb

## Testing Your Models:

### Online Viewers:

- https://gltf-viewer.donmccurdy.com/
- https://sandbox.babylonjs.com/

### Verify:

- Model loads without errors
- Textures are visible
- Animations play correctly
- File size is reasonable

## Current Models:

| File Name | Description | Use Case | Size |
| --------- | ----------- | -------- | ---- |
| TBD       | TBD         | TBD      | TBD  |

## Notes:

- Models should be in **T-pose** or **A-pose** for best compatibility
- All models must include proper UV mapping
- Animations should be embedded in the GLB file
- Test models in AR before deploying to production
