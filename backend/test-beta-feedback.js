const axios = require('axios');

const API_URL = process.env.API_URL || 'http://localhost:4000/api/v1';

async function testBetaFeedbackEndpoint() {
  console.log('🧪 Testing Beta Feedback Endpoint...\n');

  try {
    // Test 1: Submit valid feedback
    console.log('Test 1: Submit valid beta feedback');
    const feedbackData = {
      user_id: 'test-user-123',
      poster_id: 'poster-abc-456',
      rating: 5,
      comment: 'Great AR experience! The avatar was very responsive.',
      timestamp: Date.now()
    };

    const response = await axios.post(`${API_URL}/beta-feedback`, feedbackData);
    
    if (response.data.success) {
      console.log('✅ Feedback submitted successfully');
      console.log(`   Feedback ID: ${response.data.feedbackId}`);
    } else {
      console.log('❌ Feedback submission failed');
    }

    // Test 2: Submit feedback without optional fields
    console.log('\nTest 2: Submit feedback without optional fields');
    const minimalFeedback = {
      user_id: null,
      poster_id: 'poster-xyz-789',
      rating: 3,
      comment: null,
      timestamp: Date.now()
    };

    const response2 = await axios.post(`${API_URL}/beta-feedback`, minimalFeedback);
    
    if (response2.data.success) {
      console.log('✅ Minimal feedback submitted successfully');
      console.log(`   Feedback ID: ${response2.data.feedbackId}`);
    }

    // Test 3: Submit invalid feedback (missing required fields)
    console.log('\nTest 3: Submit invalid feedback (missing required fields)');
    try {
      await axios.post(`${API_URL}/beta-feedback`, {
        user_id: 'test-user',
        // missing poster_id, rating, timestamp
      });
      console.log('❌ Should have failed validation');
    } catch (error) {
      if (error.response && error.response.status === 400) {
        console.log('✅ Validation working correctly');
        console.log(`   Error: ${error.response.data.message}`);
      } else {
        throw error;
      }
    }

    console.log('\n✅ All tests passed!');
  } catch (error) {
    console.error('❌ Test failed:', error.message);
    if (error.response) {
      console.error('   Response:', error.response.data);
    }
    process.exit(1);
  }
}

testBetaFeedbackEndpoint();
