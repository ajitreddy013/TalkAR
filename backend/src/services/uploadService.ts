import multer from 'multer';
import AWS from 'aws-sdk';
import { v4 as uuidv4 } from 'uuid';
import path from 'path';
import fs from 'fs';

// Configure AWS S3
const s3 = new AWS.S3({
  accessKeyId: process.env.AWS_ACCESS_KEY_ID,
  secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY,
  region: process.env.AWS_REGION || 'us-east-1'
});

// Create uploads directory if it doesn't exist
const uploadsDir = path.join(process.cwd(), 'uploads');
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}

// Configure multer for local disk storage (development) or memory storage (production with S3)
const storage = process.env.NODE_ENV === 'production' && process.env.AWS_S3_BUCKET 
  ? multer.memoryStorage()
  : multer.diskStorage({
      destination: (req, file, cb) => {
        cb(null, uploadsDir);
      },
      filename: (req, file, cb) => {
        const fileExtension = path.extname(file.originalname);
        const fileName = `${uuidv4()}${fileExtension}`;
        cb(null, fileName);
      }
    });

export const uploadImage = multer({
  storage,
  limits: {
    fileSize: parseInt(process.env.MAX_FILE_SIZE || '10485760') // 10MB default
  },
  fileFilter: (req, file, cb) => {
    const allowedTypes = (process.env.ALLOWED_FILE_TYPES || 'image/jpeg,image/png,image/gif,image/webp').split(',');
    
    if (allowedTypes.includes(file.mimetype)) {
      cb(null, true);
    } else {
      cb(new Error('Invalid file type. Only images are allowed.'));
    }
  }
});

export const uploadToS3 = async (file: Express.Multer.File, folder: string = 'images'): Promise<string> => {
  try {
    const fileExtension = path.extname(file.originalname);
    const fileName = `${folder}/${uuidv4()}${fileExtension}`;
    
    const uploadParams = {
      Bucket: process.env.AWS_S3_BUCKET || 'talkar-assets',
      Key: fileName,
      Body: file.buffer,
      ContentType: file.mimetype,
      ACL: 'public-read'
    };
    
    const result = await s3.upload(uploadParams).promise();
    return result.Location;
  } catch (error) {
    console.error('S3 upload error:', error);
    throw new Error('Failed to upload file to S3');
  }
};

export const deleteFromS3 = async (url: string): Promise<void> => {
  try {
    const key = url.split('/').slice(-2).join('/'); // Extract key from URL
    
    const deleteParams = {
      Bucket: process.env.AWS_S3_BUCKET || 'talkar-assets',
      Key: key
    };
    
    await s3.deleteObject(deleteParams).promise();
  } catch (error) {
    console.error('S3 delete error:', error);
    throw new Error('Failed to delete file from S3');
  }
};

export const generateThumbnail = async (imageUrl: string): Promise<string> => {
  // In production, implement thumbnail generation using Sharp or similar
  // For now, return the original URL
  return imageUrl;
};

