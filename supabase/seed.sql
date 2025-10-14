-- Seed data for testing
INSERT INTO user_profiles (id, email, full_name, avatar_url) VALUES
    ('550e8400-e29b-41d4-a716-446655440000', 'test@talkar.com', 'Test User', 'https://example.com/avatar.jpg');

INSERT INTO projects (id, user_id, title, description, status, video_url, audio_url, avatar_url) VALUES
    ('660e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440000', 'Test Project 1', 'First test project', 'completed', 'https://example.com/video1.mp4', 'https://example.com/audio1.mp3', 'https://example.com/avatar1.jpg'),
    ('660e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440000', 'Test Project 2', 'Second test project', 'processing', null, null, 'https://example.com/avatar2.jpg');

INSERT INTO sync_jobs (id, project_id, status, sync_data) VALUES
    ('770e8400-e29b-41d4-a716-446655440001', '660e8400-e29b-41d4-a716-446655440001', 'completed', '{"duration": 120, "fps": 30}'),
    ('770e8400-e29b-41d4-a716-446655440002', '660e8400-e29b-41d4-a716-446655440002', 'processing', '{"duration": 90, "fps": 25}');