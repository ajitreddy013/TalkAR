-- Migration: Add 3D Avatar support for TalkAR
-- Date: 2025-10-19
-- Description: Extends avatar schema for 3D models, scripts, and audio/video URLs

-- Create avatars table (if not exists)
CREATE TABLE IF NOT EXISTS avatars (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    avatar_image_url VARCHAR(500) NOT NULL,
    avatar_video_url VARCHAR(500),
    voice_id VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Add 3D model support columns to avatars table
ALTER TABLE avatars
ADD COLUMN IF NOT EXISTS avatar_model_url VARCHAR(500),
ADD COLUMN IF NOT EXISTS avatar_model_type VARCHAR(50) DEFAULT 'glb',
ADD COLUMN IF NOT EXISTS avatar_scale DECIMAL(5,3) DEFAULT 1.0,
ADD COLUMN IF NOT EXISTS avatar_position_x DECIMAL(5,3) DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS avatar_position_y DECIMAL(5,3) DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS avatar_position_z DECIMAL(5,3) DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS avatar_rotation_x DECIMAL(6,2) DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS avatar_rotation_y DECIMAL(6,2) DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS avatar_rotation_z DECIMAL(6,2) DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS idle_animation VARCHAR(50) DEFAULT 'BREATHING_AND_BLINKING',
ADD COLUMN IF NOT EXISTS avatar_type VARCHAR(50) DEFAULT 'GENERIC',
ADD COLUMN IF NOT EXISTS gender VARCHAR(20) DEFAULT 'NEUTRAL';

-- Create image_avatar_mappings table (if not exists)
CREATE TABLE IF NOT EXISTS image_avatar_mappings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    image_id UUID NOT NULL REFERENCES images(id) ON DELETE CASCADE,
    avatar_id UUID NOT NULL REFERENCES avatars(id) ON DELETE CASCADE,
    script_text TEXT,
    audio_url VARCHAR(500),
    video_url VARCHAR(500),
    viseme_data JSONB,
    priority INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(image_id, avatar_id)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_avatars_is_active ON avatars(is_active);
CREATE INDEX IF NOT EXISTS idx_avatars_avatar_type ON avatars(avatar_type);
CREATE INDEX IF NOT EXISTS idx_image_avatar_mappings_image_id ON image_avatar_mappings(image_id);
CREATE INDEX IF NOT EXISTS idx_image_avatar_mappings_avatar_id ON image_avatar_mappings(avatar_id);
CREATE INDEX IF NOT EXISTS idx_image_avatar_mappings_is_active ON image_avatar_mappings(is_active);
CREATE INDEX IF NOT EXISTS idx_image_avatar_mappings_priority ON image_avatar_mappings(priority);

-- Create updated_at trigger for avatars
CREATE OR REPLACE FUNCTION update_avatars_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS trigger_avatars_updated_at ON avatars;
CREATE TRIGGER trigger_avatars_updated_at 
    BEFORE UPDATE ON avatars
    FOR EACH ROW
    EXECUTE FUNCTION update_avatars_updated_at();

-- Create updated_at trigger for image_avatar_mappings
CREATE OR REPLACE FUNCTION update_image_avatar_mappings_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS trigger_image_avatar_mappings_updated_at ON image_avatar_mappings;
CREATE TRIGGER trigger_image_avatar_mappings_updated_at 
    BEFORE UPDATE ON image_avatar_mappings
    FOR EACH ROW
    EXECUTE FUNCTION update_image_avatar_mappings_updated_at();

-- Insert sample avatars
INSERT INTO avatars (id, name, description, avatar_image_url, avatar_model_url, avatar_scale, avatar_type, gender, voice_id)
VALUES
    (
        '550e8400-e29b-41d4-a716-446655440001',
        'Generic Male Presenter',
        'Professional male presenter avatar for general use',
        'https://storage.example.com/avatars/generic_male.png',
        'https://storage.example.com/models/generic_male.glb',
        0.3,
        'GENERIC',
        'MALE',
        'voice-male-1'
    ),
    (
        '550e8400-e29b-41d4-a716-446655440002',
        'Generic Female Presenter',
        'Professional female presenter avatar for general use',
        'https://storage.example.com/avatars/generic_female.png',
        'https://storage.example.com/models/generic_female.glb',
        0.3,
        'GENERIC',
        'FEMALE',
        'voice-female-1'
    ),
    (
        '550e8400-e29b-41d4-a716-446655440003',
        'Celebrity Male Avatar - SRK Style',
        'Celebrity-style male avatar inspired by Bollywood',
        'https://storage.example.com/avatars/celebrity_male_srk.png',
        'https://storage.example.com/models/celebrity_male_srk.glb',
        0.3,
        'CELEBRITY',
        'MALE',
        'voice-male-celebrity-1'
    )
ON CONFLICT (id) DO NOTHING;

-- Example: Map Shah Rukh Khan poster to SRK avatar
-- INSERT INTO image_avatar_mappings (image_id, avatar_id, script_text, priority)
-- SELECT 
--     i.id as image_id,
--     '550e8400-e29b-41d4-a716-446655440003' as avatar_id,
--     'Welcome to TalkAR — experience magic in motion.' as script_text,
--     1 as priority
-- FROM images i
-- WHERE i.name ILIKE '%Shah Rukh Khan%'
-- ON CONFLICT (image_id, avatar_id) DO NOTHING;

-- Add comments for documentation
COMMENT ON TABLE avatars IS '3D avatar models with properties for AR rendering';
COMMENT ON TABLE image_avatar_mappings IS 'Maps detected images to specific 3D avatars with scripts and generated media';
COMMENT ON COLUMN avatars.avatar_model_url IS 'URL to GLB/GLTF 3D model file';
COMMENT ON COLUMN avatars.avatar_scale IS 'Default scale factor for AR rendering (0.1 - 2.0)';
COMMENT ON COLUMN avatars.idle_animation IS 'Default idle animation type: NONE, BREATHING, BLINKING, BREATHING_AND_BLINKING, CUSTOM';
COMMENT ON COLUMN image_avatar_mappings.script_text IS 'Dialogue text for this image-avatar combination';
COMMENT ON COLUMN image_avatar_mappings.audio_url IS 'Generated TTS audio URL from Sync API';
COMMENT ON COLUMN image_avatar_mappings.video_url IS 'Generated lip-sync video URL from Sync API';
COMMENT ON COLUMN image_avatar_mappings.viseme_data IS 'Phoneme timing data for lip-sync animation (JSON format)';
COMMENT ON COLUMN image_avatar_mappings.priority IS 'Higher priority mappings are used first (for multiple avatars per image)';
