import fs from "fs";
import path from "path";

// Define the structure of a user interaction entry
export interface UserInteraction {
  timestamp: string;
  poster_id: string;
  product_name: string;
  script: string;
  feedback: "like" | "dislike" | null;
}

const USER_CONTEXT_PATH = path.join(__dirname, "../../data/user_context.json");

/**
 * Store a new interaction in the user context
 * @param entry The interaction entry to store
 */
export function storeInteraction(entry: UserInteraction): void {
  try {
    // Read existing data
    const data = JSON.parse(fs.readFileSync(USER_CONTEXT_PATH, "utf8")) as UserInteraction[];
    
    // Add new entry
    data.push(entry);
    
    // Keep only the last 3 interactions
    if (data.length > 3) {
      data.shift();
    }
    
    // Write back to file
    fs.writeFileSync(USER_CONTEXT_PATH, JSON.stringify(data, null, 2));
  } catch (error) {
    console.error("Error storing interaction:", error);
  }
}

/**
 * Get recent interactions (last 3)
 * @returns Array of recent interactions
 */
export function getRecentInteractions(): UserInteraction[] {
  try {
    const data = JSON.parse(fs.readFileSync(USER_CONTEXT_PATH, "utf8")) as UserInteraction[];
    return data;
  } catch (error) {
    console.error("Error reading interactions:", error);
    return [];
  }
}

/**
 * Update feedback for a specific poster
 * @param poster_id The ID of the poster to update
 * @param feedback The feedback to set ("like" or "dislike")
 */
export function updateFeedback(poster_id: string, feedback: "like" | "dislike"): void {
  try {
    const data = JSON.parse(fs.readFileSync(USER_CONTEXT_PATH, "utf8")) as UserInteraction[];
    const index = data.findIndex(i => i.poster_id === poster_id);
    
    if (index !== -1) {
      data[index].feedback = feedback;
      fs.writeFileSync(USER_CONTEXT_PATH, JSON.stringify(data, null, 2));
    }
  } catch (error) {
    console.error("Error updating feedback:", error);
  }
}