# Adding Images via Admin Dashboard - Quick Guide

## Step 1: Start the Servers

### Backend Server
```bash
cd backend
npm run dev
```
✅ Server should start on `http://localhost:4000`

### Admin Dashboard
Open a new terminal:
```bash
cd admin-dashboard
npm start
```
✅ Dashboard should open at `http://localhost:3000`

---

## Step 2: Access the Admin Dashboard

Open your browser and go to:
```
http://localhost:3000
```

---

## Step 3: Add a New Image

### Using Real Product Images

Here are some **real, publicly accessible product image URLs** you can use:

#### Option 1: Coca-Cola
```
Name: Coca-Cola Classic
Image URL: https://images.unsplash.com/photo-1554866585-cd94860890b7?w=800
Description: The iconic refreshing cola drink loved worldwide
```

#### Option 2: Nike Shoes
```
Name: Nike Air Max
Image URL: https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800
Description: Premium athletic footwear for peak performance
```

#### Option 3: Headphones
```
Name: Sony Headphones
Image URL: https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800
Description: Professional wireless headphones with noise cancellation
```

#### Option 4: Water Bottle
```
Name: Hydro Flask
Image URL: https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=800
Description: Insulated stainless steel water bottle
```

#### Option 5: Backpack
```
Name: Travel Backpack
Image URL: https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800
Description: Durable backpack for everyday adventures
```

---

## Step 4: Fill in the Form

In the admin dashboard:

1. Click **"Add New Image"** or navigate to Images section
2. Fill in the form:
   - **Name**: Product name (e.g., "Coca-Cola Classic")
   - **Description**: Brief description
   - **Image URL**: Copy one of the URLs above
   - **Thumbnail URL**: (Optional) Leave blank
   - **Active**: ✅ Check this box

3. Click **Save**

---

## Step 5: Add Dialogue for the Image

After creating the image:

1. Go to **Dialogues** section
2. Click **"Add New Dialogue"**
3. Fill in:
   - **Image**: Select the image you just created
   - **Text**: Script for the avatar (e.g., "Refresh yourself with ice-cold Coca-Cola!")
   - **Language**: English (or Hindi, Spanish, etc.)
   - **Tone**: excited, friendly, professional, etc.
   - **Emotion**: happy, neutral, surprised, serious
   - **Is Default**: ✅ Check this
   - **Active**: ✅ Check this

4. Click **Save**

---

## Step 6: Verify the Image

Check if the image was added:

```bash
cd backend
sqlite3 data/database.sqlite "SELECT name, imageUrl FROM images ORDER BY createdAt DESC LIMIT 5;"
```

---

## Example: Adding Coca-Cola

### Image Details:
```json
{
  "name": "Coca-Cola Classic",
  "description": "The iconic refreshing cola drink loved worldwide. Perfect for any occasion!",
  "imageUrl": "https://images.unsplash.com/photo-1554866585-cd94860890b7?w=800",
  "isActive": true
}
```

### Dialogue Details:
```json
{
  "text": "Quench your thirst with the refreshing taste of Coca-Cola! The perfect drink for any moment. Grab one today!",
  "language": "English",
  "tone": "excited",
  "emotion": "happy",
  "isDefault": true,
  "isActive": true
}
```

---

## Alternative: Add via API (if dashboard isn't working)

### Add Image:
```bash
curl -X POST http://localhost:4000/api/v1/admin/images \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Coca-Cola Classic",
    "description": "The iconic refreshing cola drink",
    "imageUrl": "https://images.unsplash.com/photo-1554866585-cd94860890b7?w=800",
    "isActive": true
  }'
```

### Add Dialogue (replace IMAGE_ID with the ID from above):
```bash
curl -X POST http://localhost:4000/api/v1/admin/dialogues \
  -H "Content-Type: application/json" \
  -d '{
    "imageId": "IMAGE_ID_HERE",
    "text": "Quench your thirst with refreshing Coca-Cola!",
    "language": "English",
    "tone": "excited",
    "emotion": "happy",
    "isDefault": true,
    "isActive": true
  }'
```

---

## Testing the New Image

### Print the Image:
1. Open the image URL in your browser
2. Right-click → Save image
3. Print it on paper (color recommended)

### Scan with Beta App:
1. Install TalkAR Beta on your phone
2. Open the app
3. Point camera at the printed image
4. AR avatar should appear and speak the dialogue!

---

## Recommended Test Images

For a good beta test, add **5-10 images** across different categories:

- 🥤 Beverages (Coca-Cola, Pepsi, etc.)
- 👟 Footwear (Nike, Adidas, etc.)
- 🎧 Electronics (Headphones, phones, etc.)
- 🎒 Accessories (Backpacks, watches, etc.)
- 🍫 Food items (Snacks, chocolates, etc.)

This gives testers variety and tests different tones/emotions!
