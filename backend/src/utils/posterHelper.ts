import fs from 'fs';
import path from 'path';

interface PosterMetadata {
  image_id: string;
  product_name: string;
  category: string;
  tone: string;
  language: string;
  image_url: string;
  brand: string;
  price: number;
  currency: string;
  features: string[];
  description: string;
}

let postersCache: PosterMetadata[] | null = null;

/**
 * Load poster metadata from JSON file
 */
function loadPosters(): PosterMetadata[] {
  if (postersCache) {
    return postersCache;
  }

  try {
    const dataPath = path.join(__dirname, '../../data/product-metadata.json');
    const data = fs.readFileSync(dataPath, 'utf8');
    postersCache = JSON.parse(data);
    return postersCache!;
  } catch (error) {
    console.error('Error loading poster metadata:', error);
    return [];
  }
}

/**
 * Get poster metadata by image ID
 */
export function getPosterById(id: string): PosterMetadata | null {
  const posters = loadPosters();
  return posters.find(p => p.image_id === id) || null;
}

/**
 * Get all poster metadata
 */
export function getAllPosters(): PosterMetadata[] {
  return loadPosters();
}

/**
 * Get posters by category
 */
export function getPostersByCategory(category: string): PosterMetadata[] {
  const posters = loadPosters();
  return posters.filter(p => p.category.toLowerCase() === category.toLowerCase());
}

/**
 * Get posters by language
 */
export function getPostersByLanguage(language: string): PosterMetadata[] {
  const posters = loadPosters();
  return posters.filter(p => p.language.toLowerCase() === language.toLowerCase());
}

/**
 * Get posters by tone
 */
export function getPostersByTone(tone: string): PosterMetadata[] {
  const posters = loadPosters();
  return posters.filter(p => p.tone.toLowerCase() === tone.toLowerCase());
}

/**
 * Clear cache (useful for testing or when data changes)
 */
export function clearPostersCache(): void {
  postersCache = null;
}
