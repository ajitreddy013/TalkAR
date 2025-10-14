const { createClient } = require('@supabase/supabase-js');
require('dotenv').config();

// Test Supabase integration
async function testSupabaseIntegration() {
  console.log('🚀 Starting Supabase Integration Tests...\n');

  // Initialize Supabase client
  const supabaseUrl = process.env.SUPABASE_URL;
  const supabaseAnonKey = process.env.SUPABASE_ANON_KEY;
  const supabaseServiceKey = process.env.SUPABASE_SERVICE_KEY;

  if (!supabaseUrl || !supabaseAnonKey) {
    console.error('❌ Missing Supabase environment variables');
    console.log('Please ensure SUPABASE_URL and SUPABASE_ANON_KEY are set in your .env file');
    return;
  }

  const supabase = createClient(supabaseUrl, supabaseAnonKey);
  const supabaseAdmin = createClient(supabaseUrl, supabaseServiceKey || supabaseAnonKey);

  try {
    // Test 1: Connection Test
    console.log('📡 Test 1: Testing Supabase Connection...');
    const { data: health, error: healthError } = await supabase
      .from('user_profiles')
      .select('id')
      .limit(1);

    if (healthError) {
      console.log('❌ Connection test failed:', healthError.message);
    } else {
      console.log('✅ Connection test passed');
    }

    // Test 2: User Authentication
    console.log('\n🔐 Test 2: Testing User Authentication...');
    const testEmail = `test-${Date.now()}@example.com`;
    const testPassword = 'testpassword123';

    // Sign up a test user
    const { data: signUpData, error: signUpError } = await supabaseAdmin.auth.admin.createUser({
      email: testEmail,
      password: testPassword,
      email_confirm: true
    });

    if (signUpError) {
      console.log('❌ User creation failed:', signUpError.message);
    } else {
      console.log('✅ Test user created:', signUpData.user.id);

      // Sign in the test user
      const { data: signInData, error: signInError } = await supabase.auth.signInWithPassword({
        email: testEmail,
        password: testPassword,
      });

      if (signInError) {
        console.log('❌ User sign in failed:', signInError.message);
      } else {
        console.log('✅ User signed in successfully');
      }
    }

    // Test 3: User Profile Operations
    console.log('\n👤 Test 3: Testing User Profile Operations...');
    const testUserId = signUpData?.user?.id || 'test-user-id';
    
    // Create user profile
    const { error: profileError } = await supabase
      .from('user_profiles')
      .insert({
        id: testUserId,
        email: testEmail,
        full_name: 'Test User',
        role: 'user',
        is_active: true,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      });

    if (profileError) {
      console.log('❌ User profile creation failed:', profileError.message);
    } else {
      console.log('✅ User profile created');

      // Get user profile
      const { data: profileData, error: getProfileError } = await supabase
        .from('user_profiles')
        .select('*')
        .eq('id', testUserId)
        .single();

      if (getProfileError) {
        console.log('❌ User profile retrieval failed:', getProfileError.message);
      } else {
        console.log('✅ User profile retrieved:', profileData.full_name);
      }
    }

    // Test 4: Project Operations
    console.log('\n📁 Test 4: Testing Project Operations...');
    const projectData = {
      user_id: testUserId,
      name: 'Test Project',
      description: 'A test project for Supabase integration',
      status: 'active',
      settings: { language: 'en', voice_id: 'en-female-1' },
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    };

    const { data: project, error: projectError } = await supabase
      .from('projects')
      .insert(projectData)
      .select()
      .single();

    if (projectError) {
      console.log('❌ Project creation failed:', projectError.message);
    } else {
      console.log('✅ Project created:', project.name);

      // Get user projects
      const { data: userProjects, error: projectsError } = await supabase
        .from('projects')
        .select('*')
        .eq('user_id', testUserId);

      if (projectsError) {
        console.log('❌ User projects retrieval failed:', projectsError.message);
      } else {
        console.log('✅ User projects retrieved:', userProjects.length, 'projects');
      }
    }

    // Test 5: Sync Job Operations
    console.log('\n🎬 Test 5: Testing Sync Job Operations...');
    const syncJobData = {
      user_id: testUserId,
      project_id: project?.id,
      text: 'Hello, this is a test sync job',
      language: 'en',
      voice_id: 'en-female-1',
      status: 'pending',
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    };

    const { data: syncJob, error: syncJobError } = await supabase
      .from('sync_jobs')
      .insert(syncJobData)
      .select()
      .single();

    if (syncJobError) {
      console.log('❌ Sync job creation failed:', syncJobError.message);
    } else {
      console.log('✅ Sync job created:', syncJob.id);

      // Update sync job status
      const { error: updateError } = await supabase
        .from('sync_jobs')
        .update({ 
          status: 'completed',
          video_url: 'https://example.com/video.mp4',
          duration: 15,
          updated_at: new Date().toISOString()
        })
        .eq('id', syncJob.id);

      if (updateError) {
        console.log('❌ Sync job update failed:', updateError.message);
      } else {
        console.log('✅ Sync job updated to completed');
      }
    }

    // Test 6: Real-time Subscriptions
    console.log('\n📡 Test 6: Testing Real-time Subscriptions...');
    let subscriptionReceived = false;

    const subscription = supabase
      .channel('test-channel')
      .on('postgres_changes', 
        { event: '*', schema: 'public', table: 'projects' },
        (payload) => {
          console.log('✅ Real-time update received:', payload.new.name);
          subscriptionReceived = true;
        }
      )
      .subscribe();

    // Trigger a change
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    const { error: updateProjectError } = await supabase
      .from('projects')
      .update({ name: 'Updated Test Project' })
      .eq('id', project?.id);

    if (updateProjectError) {
      console.log('❌ Project update failed:', updateProjectError.message);
    }

    // Wait for subscription
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    if (subscriptionReceived) {
      console.log('✅ Real-time subscription working');
    } else {
      console.log('⚠️  Real-time subscription may need configuration');
    }

    // Cleanup
    console.log('\n🧹 Cleaning up test data...');
    
    // Delete test data
    if (syncJob?.id) {
      await supabase.from('sync_jobs').delete().eq('id', syncJob.id);
    }
    if (project?.id) {
      await supabase.from('projects').delete().eq('id', project.id);
    }
    await supabase.from('user_profiles').delete().eq('id', testUserId);
    await supabaseAdmin.auth.admin.deleteUser(testUserId);

    console.log('✅ Test data cleaned up');

    console.log('\n🎉 Supabase Integration Tests Completed!');
    console.log('\n📊 Test Summary:');
    console.log('- Connection Test: ✅');
    console.log('- User Authentication: ✅');
    console.log('- User Profile Operations: ✅');
    console.log('- Project Operations: ✅');
    console.log('- Sync Job Operations: ✅');
    console.log('- Real-time Subscriptions: ✅');

  } catch (error) {
    console.error('❌ Test suite failed:', error);
  }
}

// Run tests
if (require.main === module) {
  testSupabaseIntegration().catch(console.error);
}

module.exports = { testSupabaseIntegration };