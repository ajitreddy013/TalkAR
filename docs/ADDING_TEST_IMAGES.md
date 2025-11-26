# Adding Test Images for Beta Testing

## Overview
For beta testing to work, you need to add product/poster images to the backend database. Testers will scan these images with the mobile app, which will trigger AR avatar generation and lip-sync.

## Method 1: Using Admin Dashboard (Recommended)

### Step 1: Start the Backend Server
```bash
cd backend
npm run dev
```

### Step 2: Start the Admin Dashboard
```bash
cd admin-dashboard
npm start
```

### Step 3: Access the Dashboard
Open your browser and navigate to:
```
http://localhost:3000
```

### Step 4: Add a New Image/Poster
1. Navigate to the **Images** section
2. Click **Add New Image**
3. Fill in the details:
   - **Name**: Product name (e.g., "Coca-Cola Bottle")
   - **Description**: Brief description
   - **Image URL**: URL to the product image
   - **Thumbnail URL**: (Optional) Smaller version
   - **Active**: Check to make it active

4. Click **Save**

### Step 5: Add Dialogue/Script for the Image
1. After creating the image, go to **Dialogues** section
2. Click **Add New Dialogue**
3. Fill in:
   - **Image**: Select the image you just created
   - **Text**: The script the avatar will speak (e.g., "Refresh yourself with ice-cold Coca-Cola!")
   - **Language**: English, Hindi, etc.
   - **Tone**: friendly, excited, professional, etc.
   - **Emotion**: neutral, happy, surprised, serious
   - **Is Default**: Check if this is the default dialogue
   - **Active**: Check to make it active

4. Click **Save**

---

## Method 2: Using API Directly

### Add Image via API
```bash
curl -X POST http://localhost:4000/api/v1/admin/images \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Coca-Cola Bottle",
    "description": "Refreshing cola drink",
    "imageUrl": "https://example.com/coca-cola.jpg",
    "isActive": true
  }'
```

### Add Dialogue via API
```bash
curl -X POST http://localhost:4000/api/v1/admin/dialogues \
  -H "Content-Type: application/json" \
  -d '{
    "imageId": "IMAGE_ID_FROM_PREVIOUS_STEP",
    "text": "Refresh yourself with ice-cold Coca-Cola!",
    "language": "English",
    "tone": "excited",
    "emotion": "happy",
    "isDefault": true,
    "isActive": true
  }'
```

---

## Method 3: Using Database Script (Bulk Import)

### Step 1: Update product-metadata.json
Edit `backend/data/product-metadata.json` and add your products:

```json
[
  {
    "image_id": "poster_06",
    "product_name": "Your Product Name",
    "category": "Category",
    "tone": "friendly",
    "language": "English",
    "image_url": "https://example.com/image.jpg",
    "brand": "Brand Name",
    "price": 99.99,
    "currency": "USD",
    "features": ["Feature 1", "Feature 2"],
    "description": "Product description"
  }
]
```

### Step 2: Run Population Script
```bash
cd backend
node populate-db-direct.js
```

---

## Preparing Physical Test Posters

For beta testing, you'll need **physical posters** that testers can scan with their phones:

### Option 1: Print Product Images
1. Download product images from the URLs you added
2. Print them on A4/Letter paper (color recommended)
3. Ensure good lighting when scanning

### Option 2: Display on Screen
1. Open the image URL on a laptop/tablet screen
2. Testers can scan the screen with their phone
3. Ensure screen brightness is high

### Option 3: Use QR Codes (Alternative)
If you don't have physical posters, you can:
1. Generate QR codes that link to product images
2. Print the QR codes
3. Modify the app to scan QR codes instead of images

---

## Testing the Flow

### Complete Beta Testing Flow:

1. **Backend Setup**
   - ✅ Backend server running
   - ✅ Database has images and dialogues
   - ✅ API endpoints working

2. **Tester Scans Poster**
   - Opens TalkAR Beta app
   - Points camera at poster/image
   - App uses ML Kit to recognize the image

3. **Image Recognition**
   - App sends image to backend
   - Backend matches image with database
   - Returns image ID and metadata

4. **Script Generation**
   - Backend generates dynamic script based on:
     - Product metadata
     - User preferences
     - Tone and emotion settings
   - Returns script text

5. **Lip-Sync Video Generation**
   - Backend generates lip-sync video
   - Uses avatar associated with the image
   - Syncs avatar lips with the script audio

6. **AR Display**
   - Mobile app displays avatar in AR
   - Avatar speaks the generated script
   - User can interact with voice commands

7. **Feedback Collection**
   - When AR session ends
   - Feedback modal appears (beta builds only)
   - User rates experience and adds comments
   - Feedback sent to backend

---

## Current Database Status

You currently have **5 test products** in the database:

1. **Sunrich Water Bottle** (English, excited tone)
2. **Amul Butter** (Hindi, friendly tone)
3. **Eco-Friendly Backpack** (English, enthusiastic tone)
4. **Professional Wireless Headphones** (English, professional tone)
5. **Nike Running Shoes** (English, energetic tone)

### To View Current Data:
```bash
cd backend
sqlite3 data/database.sqlite "SELECT name, imageUrl FROM images;"
```

---

## Recommendations for Beta Testing

### Minimum Setup:
- **3-5 different product posters** (you already have 5!)
- **Printed or displayed on screens**
- **Good lighting** for image recognition

### Ideal Setup:
- **10+ different products** across various categories
- **Multiple languages** (English, Hindi, etc.)
- **Different tones** (friendly, professional, excited)
- **Physical printed posters** for realistic testing

---

## Troubleshooting

### Image Not Recognized
- Ensure image URL is accessible
- Check image quality (high resolution recommended)
- Verify image is marked as `isActive: true`

### No Script Generated
- Check if dialogue exists for the image
- Verify dialogue is marked as `isActive: true`
- Check backend logs for errors

### No Avatar Appears
- Verify avatar is associated with the image
- Check `image_avatar_mappings` table
- Ensure avatar video URL is accessible

---

## Next Steps

1. **Add more images** via admin dashboard
2. **Print test posters** or prepare screens
3. **Distribute beta build** to testers
4. **Provide testers with posters** to scan
5. **Monitor feedback** in the database

For distribution instructions, see [BETA_DISTRIBUTION_GUIDE.md](./BETA_DISTRIBUTION_GUIDE.md)
