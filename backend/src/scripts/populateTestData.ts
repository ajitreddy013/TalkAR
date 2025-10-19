import { sequelize } from "../config/database";
import { Image, Dialogue } from "../models/Image";
import { Avatar } from "../models/Avatar";
import { ImageAvatarMapping } from "../models/ImageAvatarMapping";

// Test data for images with scripts
const testImages = [
  {
    name: "Albert Einstein",
    description: "Famous physicist known for theory of relativity",
    imageUrl: "https://example.com/images/einstein.jpg",
    thumbnailUrl: "https://example.com/thumbnails/einstein_thumb.jpg",
    scripts: [
      {
        text: "Hello! I'm Albert Einstein. Did you know that E equals MC squared? This equation changed our understanding of the universe forever.",
        language: "en-US",
        voiceId: "voice_002",
        isDefault: true,
      },
      {
        text: "Imagination is more important than knowledge. Knowledge is limited, but imagination encircles the world.",
        language: "en-US",
        voiceId: "voice_002",
        isDefault: false,
      },
      {
        text: "The important thing is not to stop questioning. Curiosity has its own reason for existing.",
        language: "en-US",
        voiceId: "voice_002",
        isDefault: false,
      },
      {
        text: "Try not to become a person of success, but rather try to become a person of value.",
        language: "en-US",
        voiceId: "voice_002",
        isDefault: false,
      },
    ],
  },
  {
    name: "Marie Curie",
    description: "First woman to win a Nobel Prize, pioneer in radioactivity",
    imageUrl: "https://example.com/images/curie.jpg",
    thumbnailUrl: "https://example.com/thumbnails/curie_thumb.jpg",
    scripts: [
      {
        text: "Bonjour! I'm Marie Curie. I was the first woman to win a Nobel Prize and the first person to win Nobel Prizes in two different sciences.",
        language: "en-US",
        voiceId: "voice_001",
        isDefault: true,
      },
      {
        text: "Nothing in life is to be feared, it is only to be understood. Now is the time to understand more, so that we may fear less.",
        language: "en-US",
        voiceId: "voice_001",
        isDefault: false,
      },
      {
        text: "I was taught that the way of progress was neither swift nor easy. Science is about patience and perseverance.",
        language: "en-US",
        voiceId: "voice_001",
        isDefault: false,
      },
      {
        text: "You cannot hope to build a better world without improving the individuals. To that end, each of us must work for our own improvement.",
        language: "en-US",
        voiceId: "voice_001",
        isDefault: false,
      },
    ],
  },
  {
    name: "Leonardo da Vinci",
    description: "Renaissance polymath, artist, and inventor",
    imageUrl: "https://example.com/images/leonardo.jpg",
    thumbnailUrl: "https://example.com/thumbnails/leonardo_thumb.jpg",
    scripts: [
      {
        text: "Ciao! I'm Leonardo da Vinci. I painted the Mona Lisa and designed flying machines centuries before airplanes were invented.",
        language: "en-US",
        voiceId: "voice_002",
        isDefault: true,
      },
      {
        text: "Learning never exhausts the mind. The noblest pleasure is the joy of understanding.",
        language: "en-US",
        voiceId: "voice_002",
        isDefault: false,
      },
    ],
  },
  {
    name: "Frida Kahlo",
    description: "Mexican artist known for self-portraits and surrealist works",
    imageUrl: "https://example.com/images/frida.jpg",
    thumbnailUrl: "https://example.com/thumbnails/frida_thumb.jpg",
    scripts: [
      {
        text: "Hola! I'm Frida Kahlo. I painted my reality, not my dreams. My art is my way of expressing the pain and beauty of life.",
        language: "en-US",
        voiceId: "voice_001",
        isDefault: true,
      },
      {
        text: "I paint myself because I am so often alone and because I am the subject I know best.",
        language: "en-US",
        voiceId: "voice_001",
        isDefault: false,
      },
    ],
  },
  {
    name: "Nikola Tesla",
    description: "Inventor and electrical engineer, pioneer of AC power",
    imageUrl: "https://example.com/images/tesla.jpg",
    thumbnailUrl: "https://example.com/thumbnails/tesla_thumb.jpg",
    scripts: [
      {
        text: "Greetings! I'm Nikola Tesla. I invented alternating current and wireless transmission of energy. The future will prove that I was right.",
        language: "en-US",
        voiceId: "voice_002",
        isDefault: true,
      },
      {
        text: "The day science begins to study non-physical phenomena, it will make more progress in one decade than in all the previous centuries.",
        language: "en-US",
        voiceId: "voice_002",
        isDefault: false,
      },
    ],
  },
  {
    name: "Ada Lovelace",
    description: "First computer programmer, mathematician",
    imageUrl: "https://example.com/images/ada.jpg",
    thumbnailUrl: "https://example.com/thumbnails/ada_thumb.jpg",
    scripts: [
      {
        text: "Hello! I'm Ada Lovelace. I wrote the first computer program in 1843, long before computers existed. I saw the potential of machines to go beyond mere calculation.",
        language: "en-US",
        voiceId: "voice_001",
        isDefault: true,
      },
      {
        text: "The Analytical Engine has no pretensions to originate anything. It can do whatever we know how to order it to perform.",
        language: "en-US",
        voiceId: "voice_001",
        isDefault: false,
      },
    ],
  },
  {
    name: "Steve Jobs",
    description: "Co-founder of Apple, technology visionary",
    imageUrl: "https://example.com/images/jobs.jpg",
    thumbnailUrl: "https://example.com/thumbnails/jobs_thumb.jpg",
    scripts: [
      {
        text: "Hi there! I'm Steve Jobs. I believed that design is not just how it looks, but how it works. Innovation distinguishes between a leader and a follower.",
        language: "en-US",
        voiceId: "voice_002",
        isDefault: true,
      },
      {
        text: "Stay hungry, stay foolish. The people who are crazy enough to think they can change the world are the ones who do.",
        language: "en-US",
        voiceId: "voice_002",
        isDefault: false,
      },
    ],
  },
  {
    name: "Maya Angelou",
    description: "Poet, memoirist, and civil rights activist",
    imageUrl: "https://example.com/images/maya.jpg",
    thumbnailUrl: "https://example.com/thumbnails/maya_thumb.jpg",
    scripts: [
      {
        text: "Hello, beautiful soul! I'm Maya Angelou. I rise, I rise, I rise. My mission in life is not merely to survive, but to thrive.",
        language: "en-US",
        voiceId: "voice_001",
        isDefault: true,
      },
      {
        text: "Try to be a rainbow in someone's cloud. Nothing will work unless you do.",
        language: "en-US",
        voiceId: "voice_001",
        isDefault: false,
      },
    ],
  },
];

// Test data for avatars
const testAvatars = [
  {
    name: "Emma (Female Voice)",
    description: "Professional female voice for educational content",
    avatarImageUrl: "https://example.com/avatars/emma.jpg",
    avatarVideoUrl: "https://example.com/avatar-videos/emma_intro.mp4",
    voiceId: "voice_001",
  },
  {
    name: "James (Male Voice)",
    description: "Professional male voice for educational content",
    avatarImageUrl: "https://example.com/avatars/james.jpg",
    avatarVideoUrl: "https://example.com/avatar-videos/james_intro.mp4",
    voiceId: "voice_002",
  },
  {
    name: "Sophie (British Female)",
    description: "British female voice for international content",
    avatarImageUrl: "https://example.com/avatars/sophie.jpg",
    avatarVideoUrl: "https://example.com/avatar-videos/sophie_intro.mp4",
    voiceId: "voice_003",
  },
  {
    name: "David (British Male)",
    description: "British male voice for international content",
    avatarImageUrl: "https://example.com/avatars/david.jpg",
    avatarVideoUrl: "https://example.com/avatar-videos/david_intro.mp4",
    voiceId: "voice_004",
  },
];

export async function populateTestData() {
  try {
    console.log("Starting to populate test data...");

    // Ensure database is synced and associations are defined
    const { defineAssociations } = await import("../models/associations");
    defineAssociations();
    await sequelize.sync({ alter: true });
    console.log("Database synchronized");

    // Clear existing data (handle case where tables don't exist yet)
    try {
      await ImageAvatarMapping.destroy({ where: {} });
    } catch (error) {
      console.log("ImageAvatarMapping table doesn't exist yet, skipping clear");
    }

    try {
      await Dialogue.destroy({ where: {} });
    } catch (error) {
      console.log("Dialogue table doesn't exist yet, skipping clear");
    }

    try {
      await Avatar.destroy({ where: {} });
    } catch (error) {
      console.log("Avatar table doesn't exist yet, skipping clear");
    }

    try {
      await Image.destroy({ where: {} });
    } catch (error) {
      console.log("Image table doesn't exist yet, skipping clear");
    }

    console.log("Cleared existing data");

    // Create avatars
    const createdAvatars = [];
    for (const avatarData of testAvatars) {
      const avatar = await Avatar.create({
        ...avatarData,
        isActive: true,
      });
      createdAvatars.push(avatar);
      console.log(`Created avatar: ${avatar.name}`);
    }

    // Create images with scripts
    const createdImages = [];
    for (const imageData of testImages) {
      const { scripts, ...imageInfo } = imageData;
      const image = await Image.create({
        ...imageInfo,
        isActive: true,
      });
      createdImages.push(image);

      // Create dialogues for this image
      for (let scriptIndex = 0; scriptIndex < scripts.length; scriptIndex++) {
        const script = scripts[scriptIndex];
        await Dialogue.create({
          imageId: image.id,
          text: script.text,
          language: script.language,
          voiceId: script.voiceId,
          isDefault: script.isDefault,
          orderIndex: scriptIndex,
          chunkSize: 1,
          isActive: true,
        });
      }

      console.log(
        `Created image: ${image.name} with ${scripts.length} scripts`,
      );
    }

    // Create avatar-image mappings
    for (let i = 0; i < createdImages.length; i++) {
      const image = createdImages[i];
      const avatar = createdAvatars[i % createdAvatars.length]; // Cycle through avatars

      await ImageAvatarMapping.create({
        imageId: image.id,
        avatarId: avatar.id,
        isActive: true,
      });

      console.log(`Mapped avatar ${avatar.name} to image ${image.name}`);
    }

    console.log("✅ Test data population completed successfully!");
    console.log(`Created ${createdImages.length} images with scripts`);
    console.log(`Created ${createdAvatars.length} avatars`);
    console.log(`Created ${createdImages.length} avatar-image mappings`);
  } catch (error) {
    console.error("Error populating test data:", error);
    throw error;
  }
}

// Run if called directly
if (require.main === module) {
  populateTestData()
    .then(() => {
      console.log("Test data population completed");
      process.exit(0);
    })
    .catch((error) => {
      console.error("Failed to populate test data:", error);
      process.exit(1);
    });
}
