# 3D Avatar Model Optimization Guidelines

## 📋 Performance Targets

**Target Device:** Samsung Galaxy A35 (8GB RAM, Exynos 1380)
**Minimum FPS:** 30 FPS (33.3ms per frame)
**Target FPS:** 60 FPS (16.6ms per frame)
**Maximum Models:** 1 active 3D avatar at a time

---

## 🎯 Model Specifications

### **Polygon Count Limits**

| Model Type  | Triangle Limit | Recommended        | Notes                        |
| ----------- | -------------- | ------------------ | ---------------------------- |
| Main Avatar | 50,000 tris    | 25,000-35,000 tris | Face should be 40% of budget |
| Accessories | 5,000 tris     | 2,000-3,000 tris   | Hair, glasses, etc.          |
| Total Scene | 50,000 tris    | <40,000 tris       | Leaves headroom for UI       |

**Optimization Tips:**

- Use LOD (Level of Detail) where possible
- Merge static meshes
- Remove hidden faces
- Use normal maps instead of geometry for details

---

### **Texture Specifications**

| Texture Type        | Max Size  | Format  | Compression | Max File Size |
| ------------------- | --------- | ------- | ----------- | ------------- |
| Diffuse/Albedo      | 1024x1024 | PNG/JPG | ETC2        | 512 KB        |
| Normal Map          | 1024x1024 | PNG     | ETC2        | 512 KB        |
| Metallic/Roughness  | 512x512   | PNG     | ETC2        | 256 KB        |
| Emissive            | 512x512   | PNG     | ETC2        | 128 KB        |
| **Total per Model** | -         | -       | -           | **≤1 MB**     |

**Texture Optimization:**

- **Use Texture Atlasing**: Combine multiple textures into one
- **Resize Textures**: Scale down to minimum required resolution
- **Compress Textures**: Use ETC2 compression for Android
- **Remove Alpha**: Use separate alpha channel if needed
- **Bake Lighting**: Pre-compute lighting into textures

**Recommended Tools:**

- **Texture Compression**: Android Asset Studio, Texture Packer
- **Image Optimization**: TinyPNG, ImageOptim
- **Texture Atlasing**: Texture Packer, Sprite Sheet Packer

---

### **Material Specifications**

**Shader Complexity:**

- **Max Shader Instructions**: 100-150 instructions
- **Avoid**: Real-time reflections, complex lighting
- **Use**: Baked lighting, simple PBR materials

**Material Settings:**

- Limit to 2-3 materials per model
- Use single-pass shaders
- Avoid transparency where possible
- Bake ambient occlusion

---

### **Animation Specifications**

| Animation Type       | Max Bones | Max Keyframes | FPS       |
| -------------------- | --------- | ------------- | --------- |
| Skeletal Animation   | 50 bones  | 120 frames    | 30 FPS    |
| Blend Shapes (Morph) | 20 shapes | -             | Real-time |
| Lip-Sync (Visemes)   | 12 shapes | -             | Real-time |

**Animation Optimization:**

- Use skeletal animation for body movements
- Use blend shapes for facial expressions
- Limit active animations to 2 simultaneous
- Compress animation data

---

## 🔧 GLB/GLTF Optimization Checklist

### **Before Export:**

1. ☐ Clean up mesh (remove doubles, fill holes)
2. ☐ Optimize topology (quad flow, edge loops)
3. ☐ UV unwrap efficiently (minimize seams)
4. ☐ Bake lighting and AO
5. ☐ Merge materials where possible
6. ☐ Remove hidden faces
7. ☐ Check polygon count (<50k tris)

### **During Export:**

1. ☐ Use GLB format (binary, smaller)
2. ☐ Enable Draco compression (50-90% reduction)
3. ☐ Embed textures
4. ☐ Export only necessary channels
5. ☐ Remove unused bones/animations
6. ☐ Set compression level: High

### **After Export:**

1. ☐ Validate with glTF Validator
2. ☐ Test in Sceneform Viewer
3. ☐ Check file size (≤2 MB ideal, ≤5 MB max)
4. ☐ Verify textures load correctly
5. ☐ Test animations play smoothly
6. ☐ Profile FPS on target device

---

## 📐 Model Creation Best Practices

### **Modeling:**

```
✅ DO:
- Model to scale (real-world units)
- Use quads for animation areas
- Keep topology clean and organized
- Use edge loops for deformation
- Group objects logically

❌ DON'T:
- Use n-gons (>4 sided faces)
- Create overlapping geometry
- Leave internal faces
- Use excessive subdivision
- Ignore scale/rotation/position
```

### **Texturing:**

```
✅ DO:
- Use power-of-2 texture sizes (512, 1024, 2048)
- Bake high-poly details to normal maps
- Use texture atlases
- Compress textures appropriately
- Test on mobile before finalizing

❌ DON'T:
- Use 4K textures
- Have multiple small textures
- Use uncompressed formats
- Ignore UV seams
- Use alpha blending unnecessarily
```

### **Rigging:**

```
✅ DO:
- Use minimal bone count
- Name bones clearly
- Test weight painting
- Limit bone influences to 4 per vertex
- Use IK where helpful

❌ DON'T:
- Create complex bone hierarchies
- Use helper bones unnecessarily
- Ignore weight normalization
- Over-complicate rig
- Use physics bones on mobile
```

---

## 🛠️ Recommended Tools

### **3D Modeling:**

- **Blender** (Free, Open Source)
- **Maya** (Professional)
- **3ds Max** (Professional)

### **Texture Tools:**

- **Substance Painter** (Texture painting)
- **Photoshop** (Editing)
- **GIMP** (Free alternative)
- **TinyPNG** (Compression)

### **Export/Validation:**

- **Blender GLB Exporter** (Built-in)
- **glTF Validator** (Online validation)
- **Sceneform Tools** (Android-specific)

### **Compression:**

- **Draco** (Mesh compression)
- **gltf-pipeline** (Node.js tool)
- **Basis Universal** (Texture compression)

---

## 📊 Performance Budgets

### **Per-Frame Budget (30 FPS @ 33.3ms)**

| Component         | Budget      | Notes                            |
| ----------------- | ----------- | -------------------------------- |
| Rendering         | 16 ms       | Draw calls, shader execution     |
| Physics/Animation | 5 ms        | Skeletal animation, blend shapes |
| Scripting/Logic   | 5 ms        | Kotlin code execution            |
| AR Tracking       | 5 ms        | ARCore processing                |
| Other             | 2.3 ms      | UI, sound, etc.                  |
| **Total**         | **33.3 ms** | **30 FPS**                       |

### **Memory Budget**

| Resource        | Budget      | Notes                    |
| --------------- | ----------- | ------------------------ |
| 3D Model (Mesh) | 5-10 MB     | Vertex/index buffers     |
| Textures        | 10-20 MB    | Compressed in GPU memory |
| Animations      | 2-5 MB      | Keyframe data            |
| AR Session      | 50-100 MB   | ARCore overhead          |
| App Overhead    | 100-150 MB  | Kotlin runtime, UI       |
| **Total**       | **~200 MB** | Out of 8GB total         |

---

## 🎨 Example Workflow: Creating Optimized Avatar

### **Step 1: Modeling (Blender)**

```
1. Create base mesh (~10k tris)
2. Add details with normal maps
3. Optimize topology for animation
4. Final poly count: 25k-35k tris
```

### **Step 2: Texturing (Substance Painter)**

```
1. Bake high-poly details
2. Paint textures (1024x1024)
3. Export as PNG/JPG
4. Compress with TinyPNG
```

### **Step 3: Rigging (Blender)**

```
1. Create skeleton (40-50 bones)
2. Weight paint carefully
3. Add blend shapes for face (12 visemes)
4. Test animations
```

### **Step 4: Export (Blender GLB Exporter)**

```
Settings:
- Format: GLB (Binary)
- Compression: Draco (Level 10)
- Textures: Embedded
- Animations: Include All
- Limit: 50k tris, 1MB textures
```

### **Step 5: Validation**

```
1. Check with glTF Validator
2. Verify file size (<2 MB)
3. Test in Sceneform Viewer
4. Profile on Samsung A35
```

---

## 📱 Device-Specific Optimizations

### **Samsung Galaxy A35 (Target Device)**

**Specs:**

- CPU: Exynos 1380 (2x2.4GHz + 6x2.0GHz)
- GPU: Mali-G68 MP5
- RAM: 8GB
- Display: 6.6" FHD+ (2340x1080) 120Hz

**Optimizations:**

- Use ETC2 texture compression (Mali optimized)
- Limit draw calls to <50 per frame
- Use instancing for repeated objects
- Enable GPU acceleration
- Avoid overdraw (layered transparency)

**Thermal Management:**

- Monitor temperature every 30 seconds
- Reduce quality if temp >42°C
- Throttle animations if sustained load

---

## ✅ Validation Checklist

### **Before Deployment:**

- [ ] Polygon count <50,000 tris
- [ ] Texture size ≤1 MB per model
- [ ] GLB file size ≤2 MB (5 MB max)
- [ ] Textures are ETC2 compressed
- [ ] Baked lighting included
- [ ] Animations <120 keyframes
- [ ] Skeleton ≤50 bones
- [ ] Blend shapes ≤20 shapes
- [ ] Tested on Samsung A35
- [ ] FPS ≥30 (preferably 60)
- [ ] No thermal throttling
- [ ] Memory <200 MB overhead

### **Performance Metrics:**

- [ ] Average FPS: ≥30
- [ ] Frame time: ≤33.3ms
- [ ] GPU usage: <70%
- [ ] CPU usage: <50%
- [ ] Memory: <200 MB
- [ ] Battery drain: <5% per 10 min
- [ ] Device temp: <42°C

---

## 📚 Additional Resources

- **glTF Specification**: https://www.khronos.org/gltf/
- **Draco Compression**: https://google.github.io/draco/
- **Sceneform Documentation**: https://developers.google.com/ar/develop/sceneform
- **Android Performance**: https://developer.android.com/topic/performance

---

**Last Updated:** January 19, 2025  
**Version:** 1.0  
**Target:** Week 4 Phase 1 - Performance Optimization
